#!/usr/bin/env python3
"""
instrument.py - inject the TimeMachine/Themis Jacoco harness into an arbitrary
Gradle Android project.

The Themis branches already carry this harness. Apps taken straight from
upstream do not, so this module reproduces what
JacocoIntegration/auto_integrate.py does, with three differences that matter for
modern projects:

* Kotlin DSL (build.gradle.kts) as well as Groovy.
* AGP 7/8 renamed the coverage flag: testCoverageEnabled -> enableAndroidTestCoverage.
  Both are emitted where the AGP version calls for it.
* namespace (AGP 7+) is used to place the harness package when the manifest
  carries no package attribute.

What gets injected
    1. the jacoco plugin on the application module
    2. android coverage flag inside buildTypes { debug { } }
    3. FinishListener / JacocoInstrumentation / SMSInstrumentedReceiver under
       <module>/src/main/java/<pkg>/JacocoInstrument/
    4. a receiver for edu.gatech.m3.emma.COLLECT_COVERAGE in the manifest
"""

import os
import re
import shutil

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(HERE)
HARNESS = os.path.join(PROJECT, "JacocoIntegration", "JacocoInstrument")
COLLECT_ACTION = "edu.gatech.m3.emma.COLLECT_COVERAGE"


class InstrumentError(RuntimeError):
    pass


# ------------------------------------------------------------------ discovery
def find_app_module(tree):
    """Return the directory of the module that applies com.android.application."""
    candidates = []
    for root, dirs, files in os.walk(tree):
        dirs[:] = [d for d in dirs if d not in (".git", "build", ".gradle")]
        for fname in ("build.gradle", "build.gradle.kts"):
            if fname not in files:
                continue
            path = os.path.join(root, fname)
            try:
                text = open(path, encoding="utf-8", errors="replace").read()
            except OSError:
                continue
            # Matches the plain plugin id and the version-catalog aliases modern
            # projects use, e.g. alias(libs.plugins.android.application) in
            # Fossify Clock, which a literal "com.android.application" search
            # never finds.
            if re.search(r"(com\.android\.application|plugins\.android\.application"
                         r"|androidApplication|android-application)", text):
                depth = root[len(tree):].count(os.sep)
                rel = root[len(tree):].lower()
                # Sample and demo modules also apply the application plugin.
                # Muzei has example-unsplash, which was picked over the real app
                # and produced an APK for the sample instead.
                is_sample = any(w in rel for w in
                                ("example", "sample", "demo", "playground",
                                 "benchmark", "androidtest"))
                has_launcher = False
                manifest = os.path.join(root, "src", "main", "AndroidManifest.xml")
                if os.path.isfile(manifest):
                    try:
                        has_launcher = "android.intent.category.LAUNCHER" in open(
                            manifest, encoding="utf-8", errors="replace").read()
                    except OSError:
                        pass
                score = (1 if is_sample else 0,
                         0 if has_launcher else 1,
                         0 if os.path.basename(root) == "app" else 1,
                         depth)
                candidates.append((score, root, path))
    if not candidates:
        # Some projects never name the plugin in a way that can be grepped:
        # Jellyfin applies alias(libs.plugins.android.app), and Kiwix uses a
        # bare `android` accessor supplied by its own buildSrc convention
        # plugin. Fall back to a module that looks like an application: it has a
        # build file and a manifest declaring <application>.
        for root, dirs, files in os.walk(tree):
            dirs[:] = [d for d in dirs if d not in (".git", "build", ".gradle")]
            build = next((f for f in ("build.gradle", "build.gradle.kts")
                          if f in files), None)
            if not build:
                continue
            manifest = os.path.join(root, "src", "main", "AndroidManifest.xml")
            if not os.path.isfile(manifest):
                continue
            try:
                text = open(manifest, encoding="utf-8", errors="replace").read()
            except OSError:
                continue
            if "<application" not in text:
                continue
            # a launcher intent filter marks the application, not a library
            if "android.intent.category.LAUNCHER" in text or \
                    os.path.basename(root) == "app":
                return root, os.path.join(root, build)
        raise InstrumentError("no module applies com.android.application")
    candidates.sort()
    return candidates[0][1], candidates[0][2]


def agp_major(tree):
    """Best-effort AGP major version, used to pick the coverage flag name."""
    texts = []
    for name in ("build.gradle", "build.gradle.kts", "gradle/libs.versions.toml",
                 "buildSrc/src/main/java/Dependencies.kt", "gradle.properties"):
        p = os.path.join(tree, name)
        if os.path.isfile(p):
            texts.append(open(p, encoding="utf-8", errors="replace").read())
    joined = "\n".join(texts)
    m = re.search(r"com\.android\.tools\.build:gradle:(\d+)\.", joined)
    if m:
        return int(m.group(1))
    m = re.search(r"agp\s*=\s*[\"'](\d+)\.", joined)
    if m:
        return int(m.group(1))
    # wrapper version is a decent proxy: gradle 7+ implies AGP 7+
    wrap = os.path.join(tree, "gradle", "wrapper", "gradle-wrapper.properties")
    if os.path.isfile(wrap):
        m = re.search(r"gradle-(\d+)\.", open(wrap, encoding="utf-8",
                                              errors="replace").read())
        if m:
            g = int(m.group(1))
            return 7 if g >= 7 else 3
    return 3


def find_manifest(module_dir):
    """The module's main manifest.

    src/main is the convention but not a rule: MyExpenses keeps its manifest
    under a custom sourceSet, so fall back to the shallowest manifest in the
    module that declares an <application>.
    """
    p = os.path.join(module_dir, "src", "main", "AndroidManifest.xml")
    if os.path.isfile(p):
        return p
    candidates = []
    for root, dirs, files in os.walk(module_dir):
        dirs[:] = [d for d in dirs if d not in ("build", ".git", "androidTest",
                                                "test", "debug")]
        if "AndroidManifest.xml" in files:
            path = os.path.join(root, "AndroidManifest.xml")
            try:
                if "<application" in open(path, encoding="utf-8",
                                          errors="replace").read():
                    candidates.append((root[len(module_dir):].count(os.sep), path))
            except OSError:
                pass
    if candidates:
        candidates.sort()
        return candidates[0][1]
    raise InstrumentError("no AndroidManifest.xml with <application> in %s"
                          % os.path.basename(module_dir))


def harness_package(module_dir, gradle_file):
    """Package to host the harness: manifest package, else gradle namespace."""
    manifest = find_manifest(module_dir)
    text = open(manifest, encoding="utf-8", errors="replace").read()
    m = re.search(r'package\s*=\s*"([\w.]+)"', text)
    if m:
        return m.group(1)
    g = open(gradle_file, encoding="utf-8", errors="replace").read()
    m = re.search(r'namespace\s*=?\s*[\'"]([\w.]+)[\'"]', g)
    if m:
        return m.group(1)
    m = re.search(r'applicationId\s*=?\s*[\'"]([\w.]+)[\'"]', g)
    if m:
        return m.group(1)
    raise InstrumentError("cannot determine package/namespace")


# -------------------------------------------------------------- brace helpers
def find_block(text, header_regex):
    """Locate a `header {` block; return (open_brace_idx, close_brace_idx)."""
    m = re.search(header_regex, text)
    if not m:
        return None
    i = text.find("{", m.end() - 1)
    if i < 0:
        return None
    depth = 0
    for j in range(i, len(text)):
        if text[j] == "{":
            depth += 1
        elif text[j] == "}":
            depth -= 1
            if depth == 0:
                return i, j
    return None


# --------------------------------------------------------------- gradle edits
def add_plugin(gradle_file):
    text = open(gradle_file, encoding="utf-8", errors="replace").read()
    if re.search(r"""(apply\s+plugin:\s*['"]jacoco['"]|^\s*jacoco\s*$|id\(?\s*['"]jacoco['"])""",
                 text, re.M):
        return False
    is_kts = gradle_file.endswith(".kts")
    block = find_block(text, r"\bplugins\s*\{")
    if block:
        i, j = block
        line = '    id("jacoco")\n' if is_kts else "    id 'jacoco'\n"
        text = text[:j] + line + text[j:]
    else:
        line = 'apply(plugin = "jacoco")\n' if is_kts else "apply plugin: 'jacoco'\n"
        text = line + text
    open(gradle_file, "w", encoding="utf-8").write(text)
    return True


def enable_coverage(gradle_file, agp):
    """Turn on Jacoco class instrumentation for the debug build type."""
    text = open(gradle_file, encoding="utf-8", errors="replace").read()
    is_kts = gradle_file.endswith(".kts")
    # AGP 8 removed testCoverageEnabled outright, so emitting it there fails the
    # build with "Could not find method". AGP 7 accepts both names, older AGP
    # only knows the original.
    if agp >= 8:
        lines = ["enableAndroidTestCoverage = true\n" if is_kts
                 else "enableAndroidTestCoverage true\n"]
    elif agp == 7:
        lines = [
            "isTestCoverageEnabled = true\n" if is_kts else "testCoverageEnabled true\n",
            "enableAndroidTestCoverage = true\n" if is_kts else "enableAndroidTestCoverage true\n",
        ]
    else:
        lines = ["testCoverageEnabled true\n"]
    if "TestCoverageEnabled" in text or "enableAndroidTestCoverage" in text:
        return False

    android = find_block(text, r"\bandroid\s*\{")
    if not android:
        raise InstrumentError("no android { } block in %s" % os.path.basename(gradle_file))
    a_open, a_close = android

    types = find_block(text[a_open:a_close], r"\bbuildTypes\s*\{")
    if types:
        t_open = a_open + types[0]
        t_close = a_open + types[1]
        debug = find_block(text[t_open:t_close], r"(?:\bdebug\b|getByName\(\"debug\"\)|create\(\"debug\"\))\s*\{")
        if debug:
            d_close = t_open + debug[1]
            ins = "".join("            " + ln for ln in lines)
            text = text[:d_close] + ins + text[d_close:]
        else:
            body = ("        debug {\n" + "".join("            " + ln for ln in lines)
                    + "        }\n") if not is_kts else (
                   '        getByName("debug") {\n'
                   + "".join("            " + ln for ln in lines) + "        }\n")
            text = text[:t_close] + body + text[t_close:]
    else:
        if is_kts:
            body = ('    buildTypes {\n        getByName("debug") {\n'
                    + "".join("            " + ln for ln in lines)
                    + "        }\n    }\n")
        else:
            body = ("    buildTypes {\n        debug {\n"
                    + "".join("            " + ln for ln in lines)
                    + "        }\n    }\n")
        text = text[:a_close] + body + text[a_close:]
    open(gradle_file, "w", encoding="utf-8").write(text)
    return True


# -------------------------------------------------------------- harness files
def copy_harness(module_dir, package):
    dest = os.path.join(module_dir, "src", "main", "java",
                        *package.split("."), "JacocoInstrument")
    if os.path.isdir(dest) and os.listdir(dest):
        return False
    os.makedirs(dest, exist_ok=True)
    for fname in ("FinishListener.java", "JacocoInstrumentation.java",
                  "SMSInstrumentedReceiver.java"):
        src = os.path.join(HARNESS, fname)
        body = open(src, encoding="utf-8").read()
        header = "package %s.JacocoInstrument;\n\n" % package
        open(os.path.join(dest, fname), "w", encoding="utf-8").write(header + body)
    return True


def register_receiver(module_dir):
    manifest = find_manifest(module_dir)
    text = open(manifest, encoding="utf-8", errors="replace").read()
    if COLLECT_ACTION in text:
        return False
    snippet = (
        '        <receiver android:name=".JacocoInstrument.SMSInstrumentedReceiver"\n'
        '            android:exported="true">\n'
        "            <intent-filter>\n"
        '                <action android:name="%s" />\n'
        "            </intent-filter>\n"
        "        </receiver>\n" % COLLECT_ACTION
    )
    if "</application>" not in text:
        raise InstrumentError("manifest has no </application>")
    text = text.replace("</application>", snippet + "    </application>", 1)
    open(manifest, "w", encoding="utf-8").write(text)
    return True


# --------------------------------------------------------------------- driver
def instrument_project(tree, logfile=None):
    """Apply the whole harness to a checkout. Returns a summary dict."""
    module_dir, gradle_file = find_app_module(tree)
    agp = agp_major(tree)
    package = harness_package(module_dir, gradle_file)
    steps = {
        "module": os.path.relpath(module_dir, tree),
        "agp_major": agp,
        "package": package,
        "plugin": add_plugin(gradle_file),
        "coverage_flag": enable_coverage(gradle_file, agp),
        "harness": copy_harness(module_dir, package),
        "receiver": register_receiver(module_dir),
    }
    if logfile:
        with open(logfile, "a") as fh:
            fh.write("instrumented: %s\n" % steps)
    return steps


if __name__ == "__main__":
    import json
    import sys
    print(json.dumps(instrument_project(sys.argv[1]), indent=2))
