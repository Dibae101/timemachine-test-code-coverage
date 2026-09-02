#!/usr/bin/env python3
"""
build_dataset.py - build TimeMachine coverage datasets in the layout already
used by instrumented_apps/.

For each subject it clones the pre-instrumented source branch, builds a debug
APK with Jacoco, then keeps only what a coverage report needs:

    instrumented_apps/<App>/
        <App>-#<id>/            pruned project tree (classes + java/kotlin sources)
        <apk>                   the APK the class files belong to
        class_files.json        {apk: {classfiles: [...], sourcefiles: [...]}}

Why the tree is pruned: a full gradle build tree is 80 MB - 1.5 GB, and only the
compiled classes and the sources are needed by `jacococli report`. Everything
else (caches, merged resources, native libs, .git) is deleted after the build.

Design notes
------------
* Old AGP (2.x-4.x) needs JDK 8; anything newer runs on JDK 17.
* jcenter is dead, so every build gets repair-repos.init.gradle, which rewrites
  dead bintray repositories to google/mavenCentral/aliyun/jitpack.
* State lives in status.csv, so the script can be re-run and picks up where it
  stopped. Subjects already marked ok are skipped unless --force is given.

Usage
    python3 build_dataset.py                    # all pending subjects
    python3 build_dataset.py --only firefox     # substring filter
    python3 build_dataset.py --subjects modern_subjects.json
"""

import argparse
import csv
import glob
import re
import json
import os
import shutil
import subprocess
import sys
import time

from instrument import instrument_project

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(HERE)
APPS_DIR = os.path.join(PROJECT, "instrumented_apps")
INIT_SCRIPT = os.path.join(HERE, "repair-repos.init.gradle")
WORK = os.path.join(HERE, "work")
LOGS = os.path.join(HERE, "logs")
# One status file per subject set. A single shared status.csv is unsafe: two
# builders running concurrently each hold their own snapshot and rewrite the
# whole file on every subject, so the second one silently reverts the first
# one's results. The datasets on disk were unaffected, the bookkeeping was not.
STATUS = os.path.join(HERE, "status.csv")


def set_status_path(subjects_file):
    global STATUS
    stem = os.path.splitext(os.path.basename(subjects_file))[0]
    STATUS = os.path.join(HERE, "status-%s.csv" % stem)

SDK = os.environ.get("SDK", "/home/ubuntu/android-sdk")


def _jvm_arch():
    """Debian JVM directories are suffixed with the dpkg architecture.

    Hardcoding -amd64 makes every JAVA_HOME wrong on an aarch64 host, and gradle
    then silently runs on whatever `java` is first on PATH. Resolve the suffix
    from what is actually installed instead.
    """
    import platform
    machine = platform.machine()
    if machine in ("aarch64", "arm64"):
        return "arm64"
    if machine in ("x86_64", "amd64"):
        return "amd64"
    return machine


JVM_ARCH = _jvm_arch()


def _jdk_home(version):
    """First existing JVM directory for a major version, else a plain guess."""
    candidates = [
        "/usr/lib/jvm/java-%s-openjdk-%s" % (version, JVM_ARCH),
        "/usr/lib/jvm/java-1.%s.0-openjdk-%s" % (version, JVM_ARCH),
        "/usr/lib/jvm/java-%s-openjdk" % version,
    ]
    for c in candidates:
        if os.path.isdir(c):
            return c
    return candidates[0]


JDK8 = _jdk_home("8")
JDK17 = _jdk_home("17")
AAPT = os.environ.get("AAPT") or os.path.join(SDK, "build-tools", "28.0.3", "aapt")

# Google publishes aapt2, aidl and zipalign only for linux-x86_64, so on an
# aarch64 host the copies AGP fetches from Maven cannot execute at all
# ("cannot run program ... Exec format error").
#
# Two ways out, and the second is the one used here:
#
#   1. Point android.aapt2FromMavenOverride at a native aarch64 aapt2. Debian
#      ships one, but it is aapt2 2.19-debian and it cannot read the resource
#      tables of android-35 or android-36 ("RES_TABLE_TYPE_TYPE entry offsets
#      overlap actual entry data", then a segfault on 36), which rules out every
#      current app.
#   2. Register qemu-user for x86_64 (binfmt_misc) and give it an x86_64 glibc
#      sysroot through QEMU_LD_PREFIX. Then the official binaries run as-is,
#      AGP's own version-matched aapt2 is used, and aidl/zipalign work too.
#
# Set AAPT2 explicitly only to force option 1.
AAPT2 = os.environ.get("AAPT2", "")
QEMU_LD_PREFIX = os.environ.get("QEMU_LD_PREFIX", "/opt/x86_64-sysroot")

BUILD_TIMEOUT = int(os.environ.get("BUILD_TIMEOUT", "3600"))
CLONE_TIMEOUT = int(os.environ.get("CLONE_TIMEOUT", "1800"))

STATUS_FIELDS = [
    "name", "id", "group", "state", "apk", "class_dirs", "class_files",
    "source_dirs", "size_mb", "seconds", "detail",
]


def log(msg):
    print("[%s] %s" % (time.strftime("%H:%M:%S"), msg), flush=True)


# --------------------------------------------------------------------- status
def load_status():
    rows = {}
    if os.path.isfile(STATUS):
        with open(STATUS) as fh:
            for r in csv.DictReader(fh):
                rows[(r["name"], r["id"])] = r
    return rows


def save_status(rows):
    with open(STATUS, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=STATUS_FIELDS)
        w.writeheader()
        for key in sorted(rows):
            row = {k: rows[key].get(k, "") for k in STATUS_FIELDS}
            w.writerow(row)


# ---------------------------------------------------------------------- shell
def run(cmd, cwd=None, env=None, timeout=None, logfile=None):
    """Run a command, streaming combined output to logfile. Returns exit code."""
    full_env = dict(os.environ)
    if env:
        full_env.update(env)
    with open(logfile, "ab") if logfile else open(os.devnull, "wb") as out:
        out.write(("\n$ " + " ".join(cmd) + "\n").encode())
        out.flush()
        try:
            p = subprocess.Popen(cmd, cwd=cwd, env=full_env, stdout=out,
                                 stderr=subprocess.STDOUT)
            return p.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            p.kill()
            out.write(b"\n*** TIMEOUT ***\n")
            return 124


# ------------------------------------------------------------ jacoco wiring
JACOCO_SRC = os.path.join(PROJECT, "JacocoIntegration", "JacocoInstrument")
COLLECT_ACTION = "edu.gatech.m3.emma.COLLECT_COVERAGE"


def already_instrumented(tree):
    """True when the branch already carries the Themis Jacoco harness."""
    for root, dirs, files in os.walk(tree):
        if ".git" in dirs:
            dirs.remove(".git")
        if "JacocoInstrument" in dirs:
            return True
        if "AndroidManifest.xml" in files:
            try:
                with open(os.path.join(root, "AndroidManifest.xml"),
                          encoding="utf-8", errors="replace") as fh:
                    if COLLECT_ACTION in fh.read():
                        return True
            except OSError:
                pass
    return False


# ------------------------------------------------------------ build products
def find_apks(tree):
    hits = []
    for root, dirs, files in os.walk(tree):
        if os.sep + "outputs" + os.sep + "apk" not in root:
            continue
        for f in files:
            if f.endswith(".apk"):
                hits.append(os.path.join(root, f))
    return hits


def pick_apk(apks):
    """Prefer a debug APK, then the largest (universal over per-abi splits)."""
    debug = [a for a in apks if "debug" in a.lower()]
    pool = debug or apks
    pool = [a for a in pool if "unsigned" not in os.path.basename(a).lower()] or pool
    return max(pool, key=lambda p: os.path.getsize(p)) if pool else None


def flavour_token(task_detail):
    """Extract the product flavour from a task name like app:assembleFdroidDebug."""
    if not task_detail:
        return None
    m = re.search(r"assemble([A-Z]\w*?)Debug\b", task_detail)
    if not m:
        return None
    return m.group(1).lower() or None


def has_class_file(path):
    for _, _, files in os.walk(path):
        if any(f.endswith(".class") for f in files):
            return True
    return False


# Directories that hold classes belonging to tests rather than to the app.
TEST_OUTPUT_MARKERS = ("unittest", "androidtest", "unit_test", "android_test",
                       "testdebug", "testfixtures")


def class_internal_name(path):
    """The internal name (com/foo/Bar) recorded inside a .class file.

    Used to locate the root of a class output directory. Matching on known
    output paths instead does not survive contact with new tool versions: AGP's
    built-in Kotlin support writes to
    intermediates/built_in_kotlinc/<variant>/compile<Variant>Kotlin/classes,
    which no earlier glob covered, so Feeder declared 4 classes instead of 1849
    and pruning then deleted the 1845 that were never declared.
    """
    import struct
    try:
        with open(path, "rb") as fh:
            blob = fh.read()
    except OSError:
        return None
    if len(blob) < 10 or blob[:4] != b"\xca\xfe\xba\xbe":
        return None
    count = struct.unpack_from(">H", blob, 8)[0]
    offsets = {}
    pos = 10
    i = 1
    try:
        while i < count:
            tag = blob[pos]
            offsets[i] = pos
            if tag == 1:                       # Utf8
                pos += 3 + struct.unpack_from(">H", blob, pos + 1)[0]
            elif tag in (7, 8, 16, 19, 20):    # Class/String/MethodType/Module
                pos += 3
            elif tag == 15:                    # MethodHandle
                pos += 4
            elif tag in (5, 6):                # Long/Double take two slots
                pos += 9
                i += 1
            else:                              # 3,4,9,10,11,12,17,18
                pos += 5
            i += 1
        access_pos = pos + 2                   # skip access_flags
        this_class = struct.unpack_from(">H", blob, access_pos)[0]
        name_index = struct.unpack_from(">H", blob, offsets[this_class] + 1)[0]
        length = struct.unpack_from(">H", blob, offsets[name_index] + 1)[0]
        start = offsets[name_index] + 3
        return blob[start:start + length].decode("utf-8", "replace")
    except (IndexError, KeyError, struct.error):
        return None


def class_root_of(directory):
    """The output root a directory of .class files belongs to.

    Given .../classes/com/foo/Bar.class the root is .../classes. Derived from the
    class's own package rather than from the directory layout, so it holds for
    any AGP or Kotlin plugin version.
    """
    sample = None
    for f in sorted(os.listdir(directory)):
        if f.endswith(".class"):
            sample = os.path.join(directory, f)
            break
    if not sample:
        return None
    internal = class_internal_name(sample)
    if internal is None:
        return None
    package = internal.rsplit("/", 1)[0] if "/" in internal else ""
    root = directory
    if package:
        for _ in package.split("/"):
            root = os.path.dirname(root)
    return os.path.normpath(root)


def is_instrumented_class(path):
    """True when a .class file has already been through JaCoCo's instrumenter.

    Offline-instrumented classes carry a synthetic $jacocoData field and a
    $jacocoInit method. Reporting against them is not merely wrong, it aborts:
    jacococli raises "Error while analyzing <class>" and produces no report at
    all, because a class may not be instrumented twice.
    """
    try:
        with open(path, "rb") as fh:
            blob = fh.read()
    except OSError:
        return False
    return b"$jacocoData" in blob or b"$jacocoInit" in blob


def _sample_class_files(path, limit=5):
    out = []
    for root, _, files in os.walk(path):
        for f in files:
            if f.endswith(".class"):
                out.append(os.path.join(root, f))
                if len(out) >= limit:
                    return out
    return out


def holds_instrumented_output(path):
    """True when a directory holds JaCoCo-instrumented copies of the classes."""
    samples = _sample_class_files(path)
    return bool(samples) and all(is_instrumented_class(p) for p in samples)


def find_class_dirs(tree):
    """Compiled application classes, whatever AGP generation produced them.

    AmazeFileManager 3.2.1 (AGP 2.3.3) writes build/intermediates/classes/
    <flavour>/<buildType>, which has no directory literally named "classes" at
    the leaf, so matching on the leaf name alone finds nothing. Globbing the
    known layouts and then dropping ancestors handles every case, including
    Kotlin modules.

    Turning coverage on adds a second copy of every class: AGP writes the
    instrumented ones to intermediates/classes/<variant>/jacoco<Variant>/. Those
    are excluded here - they are what goes into the APK, but a report has to be
    built from the original bytecode.

    Test output is excluded too: it is compiled against the app but is not part
    of the shipped APK, and counting it would dilute the coverage denominator.
    """
    roots = set()
    for build_root, dirs, _ in os.walk(tree):
        if ".git" in dirs:
            dirs.remove(".git")
        if os.path.basename(build_root) != "build":
            continue
        for root, subdirs, files in os.walk(build_root):
            if not any(f.endswith(".class") for f in files):
                continue
            derived = class_root_of(root)
            if derived:
                roots.add(derived)

    clean = []
    for d in sorted(roots):
        rel = os.path.relpath(d, tree).replace(os.sep, "/").lower()
        segments = rel.split("/")
        if any(s.startswith("jacoco") for s in segments):
            continue
        if any(marker in rel for marker in TEST_OUTPUT_MARKERS):
            continue
        if not has_class_file(d):
            continue
        if holds_instrumented_output(d):
            continue
        clean.append(d)

    # Drop any root nested inside another so a class is declared once only.
    return [d for d in clean
            if not any(o != d and d.startswith(o + os.sep) for o in clean)]


def apk_dex_class_count(apk_path):
    """Classes defined in an APK's dex files, from the dex headers.

    A cheap independent measure of how much code an APK contains, used to catch
    an entry that declares far too few classes. Feeder built fine and declared 4
    classes out of 1849 because its Kotlin output sat in a directory no glob
    matched; nothing downstream noticed, and pruning then deleted the rest.

    class_defs_size is a uint at offset 0x60 of the dex header.
    """
    import struct
    import zipfile
    total = 0
    try:
        with zipfile.ZipFile(apk_path) as zf:
            for name in zf.namelist():
                if not name.endswith(".dex"):
                    continue
                head = zf.open(name).read(112)
                if len(head) < 112 or head[:4] != b"dex\n":
                    continue
                total += struct.unpack_from("<I", head, 0x60)[0]
    except (OSError, zipfile.BadZipFile, struct.error):
        return 0
    return total


def apk_variant(apk_path):
    """Variant token for an APK, from .../outputs/apk/<flavour>/<type>/x.apk.

    A project with product flavours builds every one of them under
    assembleDebug, so the class directories found afterwards cover all flavours
    while the APK is a single one. Declaring them all makes the same class
    appear several times over and stops matching the APK under test. The APK's
    own path says which variant it is, so that is what gets declared.
    """
    parts = apk_path.replace(os.sep, "/").split("/outputs/apk/")
    if len(parts) < 2:
        return None
    segs = [s for s in parts[1].split("/")[:-1] if s]
    if len(segs) < 2:
        return None  # no flavour dimension, just a build type
    flavour, build_type = segs[0], segs[-1]
    return flavour + build_type[:1].upper() + build_type[1:]


def filter_dirs_to_variant(dirs, variant):
    """Keep only class directories belonging to one variant."""
    if not variant:
        return dirs
    low = variant.lower()
    hit = [d for d in dirs if low in d.replace(os.sep, "/").lower()]
    return hit or dirs


def count_classes(dirs):
    n = 0
    for d in dirs:
        for _, _, files in os.walk(d):
            n += sum(1 for f in files if f.endswith(".class"))
    return n


def find_source_dirs(tree):
    out = []
    for root, dirs, files in os.walk(tree):
        if ".git" in dirs:
            dirs.remove(".git")
        if os.sep + "build" + os.sep in root + os.sep:
            continue
        base = os.path.basename(root)
        if base in ("java", "kotlin") and os.sep + "src" + os.sep in root + os.sep:
            out.append(root)
            dirs[:] = []
    return sorted(out)


# ------------------------------------------------------------------- pruning
def prune_tree(tree, keep_dirs):
    """Delete everything not needed for a coverage report.

    keep_dirs are absolute paths that must survive; their parent chain survives
    with them. Also kept: any src/ tree (sources for annotated HTML).
    """
    keep = {os.path.normpath(d) for d in keep_dirs}
    removed = 0

    def is_protected(path):
        p = os.path.normpath(path)
        for k in keep:
            if p == k or k.startswith(p + os.sep) or p.startswith(k + os.sep):
                return True
        return False

    # Walk every build/ directory recursively and drop anything not protected.
    #
    # Pruning only the immediate children of build/ is not enough: the whole
    # `intermediates` directory is an ancestor of the class output and so has to
    # be kept, but its siblings inside it (merged_res, dex, transforms,
    # merged_manifests, ...) are not needed and are where the bulk of the space
    # goes. Open-Food-Facts stayed at 1.9 GB until this walked all the way down.
    build_dirs = []
    for root, dirs, _ in os.walk(tree):
        if ".git" in dirs:
            dirs.remove(".git")
        for d in dirs:
            if d == "build":
                build_dirs.append(os.path.join(root, d))

    for bdir in build_dirs:
        for root, dirs, files in os.walk(bdir, topdown=True):
            for d in list(dirs):
                full = os.path.join(root, d)
                if not is_protected(full):
                    shutil.rmtree(full, ignore_errors=True)
                    removed += 1
                    dirs.remove(d)
            for f in files:
                full = os.path.join(root, f)
                if not is_protected(full):
                    try:
                        os.remove(full)
                    except OSError:
                        pass
    for junk in (".git", ".gradle", "build/kotlin", ".idea"):
        p = os.path.join(tree, junk)
        if os.path.isdir(p):
            shutil.rmtree(p, ignore_errors=True)
            removed += 1
    return removed


def dir_size_mb(path):
    total = 0
    for root, _, files in os.walk(path):
        for f in files:
            try:
                total += os.path.getsize(os.path.join(root, f))
            except OSError:
                pass
    return round(total / 1e6, 1)


# ------------------------------------------------------------ class_files.json
def write_class_files_json(app_dir, apk_name, class_dirs, source_dirs):
    # merge with whatever this app already declares, so re-runs and multi-apk
    # apps accumulate entries instead of overwriting each other
    path = os.path.join(app_dir, "class_files.json")
    data = {}
    if os.path.isfile(path):
        try:
            with open(path) as fh:
                data = json.load(fh)
        except (OSError, ValueError):
            data = {}

    def rel(p):
        return os.path.relpath(p, app_dir) + os.sep

    data[apk_name] = {
        "classfiles": [rel(d) for d in class_dirs],
        "sourcefiles": [rel(d) for d in source_dirs],
    }
    with open(path, "w") as fh:
        json.dump(data, fh, indent=2)
    return path


# ------------------------------------------------------------------- gradle
def wrapper_gradle_major(tree):
    """Major Gradle version from the wrapper properties, or None."""
    p = os.path.join(tree, "gradle", "wrapper", "gradle-wrapper.properties")
    if not os.path.isfile(p):
        return None
    m = re.search(r"gradle-(\d+)\.", open(p, encoding="utf-8",
                                          errors="replace").read())
    return int(m.group(1)) if m else None


JDKS = {"8": JDK8, "11": _jdk_home("11"), "17": JDK17, "21": _jdk_home("21")}


def gradle_extra_args():
    """Flags every gradle invocation needs on this host."""
    args = []
    if AAPT2 and os.path.isfile(AAPT2):
        args.append("-Pandroid.aapt2FromMavenOverride=%s" % AAPT2)
    return args


def gradle_env(jdk):
    java_home = JDKS.get(str(jdk), JDK17)
    env = {
        "JAVA_HOME": java_home,
        "ANDROID_SDK_ROOT": SDK,
        "ANDROID_HOME": SDK,
        "GRADLE_OPTS": "-Xmx3g -Dorg.gradle.daemon=false -Dfile.encoding=UTF-8",
        "TERM": "dumb",
    }
    # Where qemu-user finds the x86_64 loader and libc for the SDK's native
    # tools. Harmless on an x86_64 host, required on aarch64.
    if os.path.isdir(QEMU_LD_PREFIX):
        env["QEMU_LD_PREFIX"] = QEMU_LD_PREFIX
    return env


def discover_debug_tasks(tree, jdk, logfile):
    """List per-flavour debug assemble tasks.

    `assembleDebug` builds every product flavour, so one bad flavour fails the
    whole build - AmazeFileManager 3.2.1 overflows the 64K dex limit on its
    `play` flavour while other flavours are fine. Building a single flavour
    avoids that, and it is also what the published APKs are (e.g.
    WordPress-vanilla-debug, FirefoxLite focusWebkit).
    """
    wrapper = os.path.join(tree, "gradlew")
    if not os.path.isfile(wrapper):
        return []
    out = os.path.join(LOGS, "tasks-%s.txt" % os.path.basename(tree))
    rc = run([wrapper, "--no-daemon", "-I", INIT_SCRIPT] + gradle_extra_args()
             + ["tasks", "--all", "-q"],
             cwd=tree, env=gradle_env(jdk), timeout=900, logfile=out)
    names = []
    if os.path.isfile(out):
        with open(out, encoding="utf-8", errors="replace") as fh:
            for line in fh:
                line = line.strip()
                m = re.match(r"^([\w:]*assemble[A-Z]\w*Debug)\b", line)
                if m:
                    task = m.group(1)
                    if task.lower() != "assembledebug" and task not in names:
                        names.append(task)
    # prefer open-source / plain flavours over store-specific ones
    def rank(t):
        low = t.lower()
        for i, pref in enumerate(("fdroid", "foss", "vanilla", "free", "oss",
                                  "generic", "default", "standard")):
            if pref in low:
                return i
        return 50
    names.sort(key=lambda t: (rank(t), len(t)))
    with open(logfile, "a") as fh:
        fh.write("discovered debug tasks (rc=%d): %s\n" % (rc, names[:12]))
    return names[:6]


def standalone_gradle(version="5.4.1"):
    """A gradle launcher from the wrapper cache, for projects shipping no wrapper.

    Scarlet-Notes has no gradlew, so its build needs a distribution supplied
    from outside the checkout.
    """
    base = os.path.expanduser("~/.gradle/wrapper/dists/gradle-%s-all" % version)
    if os.path.isdir(base):
        for entry in os.listdir(base):
            cand = os.path.join(base, entry, "gradle-%s" % version, "bin", "gradle")
            if os.path.isfile(cand):
                os.chmod(cand, 0o755)
                return cand
    return None


def gradle_build(tree, jdk, logfile, tasks=None):
    wrapper = os.path.join(tree, "gradlew")
    if os.path.isfile(wrapper):
        os.chmod(wrapper, 0o755)
    else:
        wrapper = standalone_gradle()
        if not wrapper:
            return 127, "no gradle wrapper and no cached distribution"
        with open(logfile, "a") as fh:
            fh.write("no gradlew in checkout; using %s\n" % wrapper)
    env = gradle_env(jdk)
    for task_set in (tasks or [["assembleDebug"]]):
        cmd = [wrapper, "--no-daemon", "--stacktrace",
               "-I", INIT_SCRIPT] + gradle_extra_args() + task_set
        rc = run(cmd, cwd=tree, env=env, timeout=BUILD_TIMEOUT, logfile=logfile)
        if rc == 0:
            return 0, " ".join(task_set)
    return rc, "build failed"


# --------------------------------------------------------------- themis apk
THEMIS_RAW = "https://raw.githubusercontent.com/the-themis-benchmarks/home/master"


def fetch_themis_apk(subject, app_dir, logfile):
    """Download the exact APK the published runs used, when there is one."""
    if not subject.get("themis_dir") or not subject.get("themis_apk"):
        return None
    import urllib.parse
    import urllib.request
    name = subject["themis_apk"]
    dest = os.path.join(app_dir, name)
    if os.path.isfile(dest) and os.path.getsize(dest) > 0:
        return dest
    url = "%s/%s/%s" % (THEMIS_RAW, urllib.parse.quote(subject["themis_dir"]),
                        urllib.parse.quote(name))
    try:
        with urllib.request.urlopen(url, timeout=300) as r, open(dest, "wb") as fh:
            shutil.copyfileobj(r, fh)
        return dest
    except Exception as exc:  # noqa: BLE001 - report and continue
        with open(logfile, "a") as fh:
            fh.write("themis apk download failed: %s\n" % exc)
        if os.path.exists(dest):
            os.remove(dest)
        return None


# ------------------------------------------------------------------ subject
def build_subject(subject, status_rows):
    name = subject["name"]
    sid = subject.get("id") or "0"
    key = (name, sid)
    t0 = time.time()
    os.makedirs(LOGS, exist_ok=True)
    logfile = os.path.join(LOGS, "%s-%s.log" % (name, sid))
    open(logfile, "w").close()

    app_dir = os.path.join(APPS_DIR, name)
    os.makedirs(app_dir, exist_ok=True)
    tree = os.path.join(app_dir, "%s-#%s" % (name, sid))

    row = {"name": name, "id": sid, "group": subject.get("group", ""),
           "state": "", "apk": "", "class_dirs": "0", "class_files": "0",
           "source_dirs": "0", "size_mb": "", "seconds": "", "detail": ""}

    # the APK used by the published runs, when Themis publishes one
    themis_apk = fetch_themis_apk(subject, app_dir, logfile)

    # No upstream branch is not necessarily fatal: APhotoManager and
    # MaterialFBook lost their instrumented branches when a GitHub account was
    # deleted, but the original dataset already ships their source trees, so
    # they can still be built locally. Only fall back to apk-only when there is
    # no tree either.
    if not subject.get("repo") and not os.path.isdir(tree):
        row["state"] = "apk_only"
        row["apk"] = os.path.basename(themis_apk) if themis_apk else ""
        row["detail"] = subject.get("note", "no source branch, no local tree")
        row["seconds"] = str(int(time.time() - t0))
        status_rows[key] = row
        save_status(status_rows)
        log("  %s: apk only (%s)" % (name, row["detail"]))
        return row

    # ---- clone
    if not os.path.isdir(tree):
        cmd = ["git", "clone", "--depth", "1"]
        if subject.get("ref"):
            cmd += ["--branch", subject["ref"]]
        cmd += [subject["repo"], tree]
        rc = run(cmd, timeout=CLONE_TIMEOUT, logfile=logfile)
        if rc == 0:
            # Drop the checkout's own git metadata straight away. If a tree
            # still contains .git when the dataset is committed, git records a
            # gitlink (a submodule reference) instead of the files, so the
            # entry appears on GitHub as an unclickable submodule with no
            # contents. Pruning removes .git too, but only after a successful
            # build, which leaves every failed subject exposed to this.
            for dot in (os.path.join(tree, ".git"),):
                if os.path.isdir(dot):
                    shutil.rmtree(dot, ignore_errors=True)
                elif os.path.isfile(dot):
                    os.remove(dot)
        if rc != 0:
            shutil.rmtree(tree, ignore_errors=True)
            row.update(state="clone_failed", detail="git clone rc=%d" % rc,
                       seconds=str(int(time.time() - t0)))
            status_rows[key] = row
            save_status(status_rows)
            log("  %s: CLONE FAILED" % name)
            return row
    log("  %s: cloned %s" % (name, subject["ref"]))

    # Some projects read local.properties during configuration and fail outright
    # when it is absent, since it is gitignored and never checked in. Muzei fails
    # evaluating :legacy-standalone with a FileNotFoundException for it.
    local_props = os.path.join(tree, "local.properties")
    if not os.path.isfile(local_props):
        with open(local_props, "w") as fh:
            fh.write("sdk.dir=%s\n" % SDK)

    # ---- instrument (upstream checkouts carry no harness; Themis branches do)
    if not already_instrumented(tree):
        if subject.get("instrument"):
            try:
                steps = instrument_project(tree, logfile)
                log("  %s: instrumented (module=%s agp=%s pkg=%s)"
                    % (name, steps["module"], steps["agp_major"], steps["package"]))
            except Exception as exc:  # noqa: BLE001 - record and move on
                row.update(state="instrument_failed", detail=str(exc)[:180],
                           seconds=str(int(time.time() - t0)))
                status_rows[key] = row
                save_status(status_rows)
                log("  %s: INSTRUMENT FAILED: %s" % (name, exc))
                return row
        else:
            with open(logfile, "a") as fh:
                fh.write("WARNING: no Jacoco harness found in this branch\n")
            log("  %s: WARNING no jacoco harness detected in branch" % name)

    # ---- build: whole-project debug first, then one flavour at a time
    jdk = subject.get("jdk", "8")
    tasks = subject.get("tasks") or [["assembleDebug"]]
    rc, detail = gradle_build(tree, jdk, logfile, tasks)
    if rc != 0 and not subject.get("tasks"):
        log("  %s: assembleDebug failed, trying single flavours" % name)
        flavours = discover_debug_tasks(tree, jdk, logfile)
        if flavours:
            rc, detail = gradle_build(tree, jdk, logfile, [[t] for t in flavours])
    if rc != 0:
        # The era guess for the JDK is sometimes wrong, so retry with the other
        # one - but only when that JDK can actually run this Gradle. Gradle 4.x
        # and 5.x cannot start a daemon on JDK 17 at all ("unrecognized jvm
        # option MaxPermSize", "Could not determine java version from 17.0.19"),
        # and an unconditional retry both wasted a build and buried the real
        # error at the end of the log.
        gv = wrapper_gradle_major(tree)
        # JDK 11 matters on its own: a Gradle 7 project with an older Kotlin
        # plugin rejects JDK 17 ("Unsupported class file major version") while
        # being too new for JDK 8. Six subjects failed on exactly that.
        # JDK 21 is in the ladder because some 2024-2025 projects request a 21
        # toolchain outright: Wallabag failed with "Cannot find a Java
        # installation ... languageVersion=21" until it was installed.
        ladder = [j for j in ("17", "21", "11", "8") if j != str(jdk)]
        for other in ladder:
            # Gradle 8 with AGP 8 needs JDK 17: retrying it on 8 or 11 cannot
            # succeed, wastes a build each, and buries the real error at the end
            # of the log, which is how GPSLogger's true failure was masked.
            runnable = (
                (other == "8" and (gv is None or gv <= 6)) or
                (other == "11" and (gv is None or gv <= 7)) or
                (other == "17" and (gv is None or gv >= 6)) or
                (other == "21" and (gv is None or gv >= 8))
            )
            # A JAVA_HOME that does not exist is worse than no retry: gradle
            # falls back to whatever `java` is on PATH, so the "retry on JDK 8"
            # actually re-runs the identical build and the log claims a JDK that
            # was never used.
            if runnable and not os.path.isdir(JDKS.get(other, "")):
                log("  %s: skipping JDK %s (not installed)" % (name, other))
                continue
            if not runnable:
                log("  %s: skipping JDK %s (gradle %s cannot run on it)"
                    % (name, other, gv))
                continue
            log("  %s: retrying with JDK %s (gradle %s)" % (name, other, gv))
            rc2, detail2 = gradle_build(tree, other, logfile, tasks)
            if rc2 == 0:
                rc, detail = rc2, detail2 + " (jdk%s)" % other
                break
    if rc != 0:
        row.update(state="build_failed", detail=detail,
                   seconds=str(int(time.time() - t0)))
        status_rows[key] = row
        save_status(status_rows)
        log("  %s: BUILD FAILED (%s)" % (name, detail))
        return row
    log("  %s: build ok (%s)" % (name, detail))

    # ---- collect products
    apks = find_apks(tree)
    class_dirs = find_class_dirs(tree)
    # When a single flavour was built, keep only that flavour's classes and APK.
    # Javac may well have succeeded for a flavour whose dexing failed (this is
    # what happens to AmazeFileManager's `play` flavour), and declaring both
    # would report the same classes twice.
    flavour = flavour_token(detail)
    if flavour:
        matching = [d for d in class_dirs if flavour in d.lower()]
        if matching:
            class_dirs = matching
        matching_apks = [a for a in apks if flavour in a.lower()]
        if matching_apks:
            apks = matching_apks
    apk = pick_apk(apks)
    # Even when assembleDebug succeeded for every flavour, only one APK is
    # declared, so narrow the classes to that APK's variant.
    if apk:
        variant = apk_variant(apk)
        if variant:
            narrowed = filter_dirs_to_variant(class_dirs, variant)
            if narrowed and narrowed != class_dirs:
                log("  %s: narrowing classes to variant %s (%d of %d dirs)"
                    % (name, variant, len(narrowed), len(class_dirs)))
                class_dirs = narrowed
    source_dirs = find_source_dirs(tree)
    n_classes = count_classes(class_dirs)

    if not apk or not class_dirs:
        row.update(state="no_products",
                   detail="apks=%d class_dirs=%d" % (len(apks), len(class_dirs)),
                   seconds=str(int(time.time() - t0)))
        status_rows[key] = row
        save_status(status_rows)
        log("  %s: NO PRODUCTS (apks=%d class_dirs=%d)" % (name, len(apks), len(class_dirs)))
        return row

    # Refuse an entry that declares far less code than the APK contains. Pruning
    # is destructive, so a wrong set of class directories has to be caught here
    # rather than at report time.
    dex_classes = apk_dex_class_count(apk)
    floor = max(10, int(dex_classes * 0.01))
    if dex_classes and n_classes < floor:
        row.update(state="too_few_classes",
                   class_dirs=str(len(class_dirs)), class_files=str(n_classes),
                   detail="declared %d classes, APK dex defines %d"
                          % (n_classes, dex_classes),
                   seconds=str(int(time.time() - t0)))
        status_rows[key] = row
        save_status(status_rows)
        log("  %s: TOO FEW CLASSES (%d declared vs %d in dex)"
            % (name, n_classes, dex_classes))
        return row

    # Name the locally built APK after the published one so a dataset that
    # already ships "<x>.apk" gains "<x>-rebuilt.apk" rather than a second,
    # differently-named copy of the same thing.
    if themis_apk:
        stem = os.path.basename(themis_apk)[: -len(".apk")]
        built_name = "%s-rebuilt.apk" % stem
    else:
        built_name = "%s-#%s-rebuilt.apk" % (name, sid)
    shutil.copy2(apk, os.path.join(app_dir, built_name))

    write_class_files_json(app_dir, built_name, class_dirs, source_dirs)
    # the published APK shares this build's classes only if Themis built it from
    # the same branch; declare it too so the dataset covers both.
    if themis_apk:
        write_class_files_json(app_dir, os.path.basename(themis_apk),
                               class_dirs, source_dirs)

    # Protect the whole class-output container, not just the leaves that were
    # selected. commons showed why: an early pass selected only its Kotlin
    # output, pruning deleted the javac output, and the entry could not be
    # repaired without a full rebuild. Keeping the containers costs a little
    # disk and makes re-deriving class_files.json possible at any time.
    containers = []
    for d in class_dirs:
        for marker in ("intermediates/javac", "intermediates/classes",
                       "tmp/kotlin-classes"):
            idx = d.replace(os.sep, "/").find(marker)
            if idx >= 0:
                containers.append(d[:idx + len(marker)])
    prune_tree(tree, class_dirs + source_dirs + containers)
    size = dir_size_mb(app_dir)

    row.update(state="ok", apk=built_name, class_dirs=str(len(class_dirs)),
               class_files=str(n_classes), source_dirs=str(len(source_dirs)),
               size_mb=str(size), seconds=str(int(time.time() - t0)),
               detail=detail)
    status_rows[key] = row
    save_status(status_rows)
    log("  %s: OK  %d class files, %d source roots, %s MB"
        % (name, n_classes, len(source_dirs), size))
    return row


# --------------------------------------------------------------------- main
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--subjects", default=os.path.join(HERE, "subjects.json"))
    ap.add_argument("--only", default=None, help="substring filter on name")
    ap.add_argument("--force", action="store_true", help="rebuild subjects marked ok")
    args = ap.parse_args()

    set_status_path(args.subjects)
    with open(args.subjects) as fh:
        subjects = json.load(fh)
    if args.only:
        needle = args.only.lower()
        subjects = [s for s in subjects if needle in s["name"].lower()]

    os.makedirs(WORK, exist_ok=True)
    os.makedirs(LOGS, exist_ok=True)
    status_rows = load_status()

    log("%d subjects queued from %s" % (len(subjects), os.path.basename(args.subjects)))
    done = 0
    for i, subject in enumerate(subjects, 1):
        key = (subject["name"], subject.get("id") or "0")
        prev = status_rows.get(key)
        if prev and prev["state"] in ("ok", "apk_only") and not args.force:
            log("(%d/%d) %s: already %s, skipping" % (i, len(subjects), subject["name"], prev["state"]))
            done += 1
            continue
        log("(%d/%d) === %s #%s ===" % (i, len(subjects), subject["name"], subject.get("id")))
        try:
            row = build_subject(subject, status_rows)
        except Exception as exc:  # noqa: BLE001 - one bad subject must not end the run
            key = (subject["name"], subject.get("id") or "0")
            row = {"name": subject["name"], "id": subject.get("id") or "0",
                   "group": subject.get("group", ""), "state": "error",
                   "detail": ("%s: %s" % (type(exc).__name__, exc))[:180]}
            status_rows[key] = row
            save_status(status_rows)
            log("  %s: ERROR %s" % (subject["name"], exc))
        if row["state"] in ("ok", "apk_only"):
            done += 1
        free = shutil.disk_usage("/home/ubuntu").free / 1e9
        log("  disk free: %.1f GB" % free)
        if free < 4:
            log("STOPPING: less than 4 GB free")
            break

    log("finished: %d/%d subjects usable" % (done, len(subjects)))
    log("status: %s" % STATUS)


if __name__ == "__main__":
    main()
