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
JDK8 = "/usr/lib/jvm/java-8-openjdk-amd64"
JDK17 = "/usr/lib/jvm/java-17-openjdk-amd64"
AAPT = os.path.join(SDK, "build-tools", "28.0.3", "aapt")

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


# Where each AGP generation puts compiled application classes. Ordered from most
# to least specific; deeper matches win so a single variant is declared rather
# than every flavour at once.
CLASS_DIR_GLOBS = [
    "**/build/intermediates/javac/*/*/classes",   # AGP 3.2+
    "**/build/intermediates/javac/*/classes",     # AGP 3.0-3.1
    "**/build/intermediates/classes/*/*",         # AGP 2.x: classes/<flavour>/<type>
    "**/build/intermediates/classes/*",           # AGP 2.x: classes/<type>
    "**/build/tmp/kotlin-classes/*",              # Kotlin output
]


def find_class_dirs(tree):
    """Compiled application classes, whatever AGP generation produced them.

    AmazeFileManager 3.2.1 (AGP 2.3.3) writes build/intermediates/classes/
    <flavour>/<buildType>, which has no directory literally named "classes" at
    the leaf, so matching on the leaf name alone finds nothing. Globbing the
    known layouts and then dropping ancestors handles every case, including
    Kotlin modules.
    """
    found = []
    for pattern in CLASS_DIR_GLOBS:
        for path in glob.glob(os.path.join(tree, pattern), recursive=True):
            if os.path.isdir(path) and has_class_file(path):
                found.append(os.path.normpath(path))
    found = sorted(set(found))
    # keep the deepest match on each branch
    keep = [d for d in found
            if not any(other != d and other.startswith(d + os.sep) for other in found)]
    return keep


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

    # inside every build/ directory, drop non-protected children
    for root, dirs, files in os.walk(tree, topdown=True):
        if os.path.basename(root) == "build":
            for d in list(dirs):
                full = os.path.join(root, d)
                if not is_protected(full):
                    shutil.rmtree(full, ignore_errors=True)
                    removed += 1
                    dirs.remove(d)
            for f in files:
                try:
                    os.remove(os.path.join(root, f))
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
def gradle_env(jdk):
    java_home = JDK8 if str(jdk) == "8" else JDK17
    return {
        "JAVA_HOME": java_home,
        "ANDROID_SDK_ROOT": SDK,
        "ANDROID_HOME": SDK,
        "GRADLE_OPTS": "-Xmx3g -Dorg.gradle.daemon=false -Dfile.encoding=UTF-8",
        "TERM": "dumb",
    }


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
    rc = run([wrapper, "--no-daemon", "-I", INIT_SCRIPT, "tasks", "--all", "-q"],
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
               "-I", INIT_SCRIPT] + task_set
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

    if not subject.get("repo"):
        row["state"] = "apk_only"
        row["apk"] = os.path.basename(themis_apk) if themis_apk else ""
        row["detail"] = subject.get("note", "no source branch")
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
        if rc != 0:
            shutil.rmtree(tree, ignore_errors=True)
            row.update(state="clone_failed", detail="git clone rc=%d" % rc,
                       seconds=str(int(time.time() - t0)))
            status_rows[key] = row
            save_status(status_rows)
            log("  %s: CLONE FAILED" % name)
            return row
    log("  %s: cloned %s" % (name, subject["ref"]))

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
        # The era guess for the JDK is sometimes wrong in both directions: a
        # 2023 tag can still need JDK 8 toolchains, and an older project can
        # refuse JDK 8. Retry once with the other JDK before giving up.
        other = "17" if str(jdk) == "8" else "8"
        log("  %s: retrying with JDK %s" % (name, other))
        rc, detail = gradle_build(tree, other, logfile, tasks)
        if rc == 0:
            detail += " (jdk%s)" % other
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
