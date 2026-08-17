#!/usr/bin/env python3
"""
fix_reports.py - repair the entries that produced execution data but no report.

Two distinct faults were behind every one of them:

1. The bundled jacococli 0.8.6 cannot analyse Java 17 bytecode and aborts with
   "Unsupported class file major version 61". Fixed by using JaCoCo 0.8.13.

2. Some entries declared more than one build variant's output at once, so JaCoCo
   refused to continue: "Can't add different class with same name". The usual
   culprits are generated resource classes (R, R$string, ...) which appear in
   every module and every flavour with different content.

The repair is per app:

  * enumerate candidate class directories
  * drop generated R classes, which carry no meaningful coverage and are the
    main source of name collisions
  * ask `jacococli classinfo` to analyse the result, which surfaces both faults
    without needing an emulator
  * if a collision remains, drop whole directories until the set analyses cleanly,
    keeping the one that contributes the most classes
  * rewrite class_files.json with the set that works

    python3 fix_reports.py --dry-run
    python3 fix_reports.py --apply
"""

import argparse
import glob
import json
import os
import re
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(HERE)
APPS = os.path.join(PROJECT, "instrumented_apps")
JACOCO = os.path.join(PROJECT, "fuzzingandroid", "libs", "jacococli-0.8.13.jar")

sys.path.insert(0, HERE)
from build_dataset import CLASS_DIR_GLOBS, find_source_dirs, has_class_file  # noqa: E402

R_CLASS = re.compile(r"(^|/)R(\$[\w$]+)?\.class$")


def report_ok(dirs, exec_file, srcs=()):
    """Try a real report. This is the only check that detects duplicates.

    `classinfo` walks classes independently and never builds the single coverage
    bundle that `report` does, so it accepts a directory set that `report` then
    rejects with "Can't add different class with same name". Validating with the
    actual report command against real execution data is the only reliable test.
    """
    if not dirs:
        return False, 0.0, "no class directories"
    out = os.path.join("/tmp", "fixrep_probe.xml")
    cmd = ["java", "-jar", JACOCO, "report", exec_file]
    for d in dirs:
        cmd += ["--classfiles", d]
    for s in srcs:
        cmd += ["--sourcefiles", s]
    cmd += ["--xml", out]
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=1800)
    if r.returncode != 0:
        err = (r.stderr or r.stdout).strip().splitlines()
        msg = next((l for l in err if "Can't add" in l or "Caused by" in l),
                   err[-1] if err else "unknown")
        return False, 0.0, msg[:150]
    pct = 0.0
    try:
        import xml.dom.minidom as m
        d = m.parse(out)
        for c in d.getElementsByTagName("counter"):
            if c.parentNode.tagName == "report" and c.getAttribute("type") == "LINE":
                mi = int(c.getAttribute("missed"))
                co = int(c.getAttribute("covered"))
                pct = 100.0 * co / (mi + co) if (mi + co) else 0.0
                break
    except Exception:  # noqa: BLE001
        pass
    finally:
        if os.path.exists(out):
            os.remove(out)
    return True, pct, ""


def classinfo(dirs):
    """Analyse class directories. Returns (ok, class_count, error)."""
    if not dirs:
        return False, 0, "no class directories"
    # classinfo takes class locations as positional arguments; --classfiles is
    # a `report` option only and makes classinfo reject the whole command.
    # Output is one line per class, which doubles as the class count.
    cmd = ["java", "-jar", JACOCO, "classinfo"] + list(dirs)
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=1800)
    if r.returncode != 0:
        err = (r.stderr or r.stdout).strip().splitlines()
        msg = next((l for l in err if "Caused by" in l or "Exception" in l),
                   err[-1] if err else "unknown")
        return False, 0, msg[:160]
    return True, len(r.stdout.splitlines()), ""


def candidate_dirs(tree):
    found = []
    for pattern in CLASS_DIR_GLOBS:
        for path in glob.glob(os.path.join(tree, pattern), recursive=True):
            if os.path.isdir(path) and has_class_file(path):
                found.append(os.path.normpath(path))
    found = sorted(set(found))
    return [d for d in found
            if not any(o != d and o.startswith(d + os.sep) for o in found)]


def count_classes(dirs):
    n = 0
    for d in dirs:
        for _, _, fs in os.walk(d):
            n += sum(1 for f in fs if f.endswith(".class"))
    return n


def strip_r_classes(dirs, apply):
    """Remove generated resource classes. Returns how many were removed."""
    removed = 0
    for d in dirs:
        for root, _, files in os.walk(d):
            for f in files:
                p = os.path.join(root, f)
                if f.endswith(".class") and R_CLASS.search(p.replace(os.sep, "/")):
                    removed += 1
                    if apply:
                        try:
                            os.remove(p)
                        except OSError:
                            pass
    return removed


def build_variant(app):
    """Flavour the APK was built from, taken from the recorded gradle task.

    status-*.csv keeps the task that succeeded, e.g. app:assembleDevDebug or
    app:assembleObfFdroidDebug. The flavour part identifies which variant's
    class output belongs with the APK.
    """
    for path in glob.glob(os.path.join(HERE, "status-*.csv")):
        try:
            import csv as _csv
            for row in _csv.DictReader(open(path)):
                if row.get("name") != app:
                    continue
                m = re.search(r"assemble([A-Z]\w*?)Debug\b", row.get("detail") or "")
                if m:
                    return m.group(1).lower()
        except OSError:
            continue
    return None


def find_exec(app):
    """A coverage.ec collected for this app by the smoke harness."""
    pattern = os.path.join(PROJECT, "results", "smoke", "*", "coverage.ec")
    hits = []
    for p in glob.glob(pattern):
        slug = os.path.basename(os.path.dirname(p)).lower()
        if slug.startswith(app.lower()[:10].replace("-", "")) or \
                app.lower().replace("-", "") in slug.replace("-", "").replace("_", ""):
            if os.path.getsize(p) > 0:
                hits.append(p)
    return max(hits, key=os.path.getsize) if hits else None


def resolve(tree, app, apply):
    """Find a class-directory set that a real report accepts."""
    dirs = candidate_dirs(tree)
    if not dirs:
        return [], "no class directories found"

    removed = strip_r_classes(dirs, apply)
    dirs = [d for d in dirs if has_class_file(d)]
    srcs = find_source_dirs(tree)

    exec_file = find_exec(app)
    if not exec_file:
        ok, n, err = classinfo(dirs)
        return (dirs if ok else []), ("%d classes, no .ec to verify against" % n
                                      if ok else "unresolved: %s" % err)

    ok, pct, err = report_ok(dirs, exec_file, srcs)
    if ok:
        return dirs, "%d dirs, %d R dropped, report %.2f%%" % (len(dirs), removed, pct)

    # Duplicates across build variants. Add directories one at a time, keeping
    # only those that do not break the report, which converges on a single
    # variant plus any module that does not collide with it.
    #
    # Order matters more than size: prefer the variant the APK was actually
    # built from. Picking a different flavour's classes still produces a report,
    # but every class that differs is reported as "does not match" and silently
    # excluded - Wikipedia lost 167 classes that way, having been built from
    # devDebug while betaDebug output was declared.
    variant = build_variant(app)

    def rank(d):
        low = d.lower()
        return (0 if variant and variant in low else 1, -count_classes(d))

    ranked = sorted(dirs, key=rank)
    keep = []
    for d in ranked:
        cand = keep + [d]
        good, _, _ = report_ok(cand, exec_file, srcs)
        if good:
            keep = cand
    if not keep:
        return [], "unresolved: %s" % err
    ok, pct, err = report_ok(keep, exec_file, srcs)
    return keep, ("%d of %d dirs, %d R dropped, report %.2f%%"
                  % (len(keep), len(dirs), removed, pct))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--only", default=None)
    args = ap.parse_args()
    apply = args.apply and not args.dry_run

    apps = sorted(d for d in os.listdir(APPS)
                  if os.path.isdir(os.path.join(APPS, d)))
    if args.only:
        apps = [a for a in apps if args.only.lower() in a.lower()]

    print("jacoco: %s" % os.path.basename(JACOCO))
    print("%-30s %-8s %s" % ("APP", "STATUS", "DETAIL"))
    fixed = broken = 0
    for app in apps:
        app_dir = os.path.join(APPS, app)
        trees = [os.path.join(app_dir, d) for d in os.listdir(app_dir)
                 if os.path.isdir(os.path.join(app_dir, d)) and d != "upstream"]
        if not trees:
            print("%-30s %-8s %s" % (app, "skip", "no project tree"))
            continue
        tree = trees[0]

        dirs, note = resolve(tree, app, apply)
        if not dirs:
            print("%-30s %-8s %s" % (app, "BROKEN", note))
            broken += 1
            continue

        srcs = find_source_dirs(tree)
        print("%-30s %-8s %s" % (app, "ok", note))
        fixed += 1
        if not apply:
            continue

        cfg = os.path.join(app_dir, "class_files.json")
        existing = json.load(open(cfg)) if os.path.isfile(cfg) else {}
        apks = [f for f in sorted(os.listdir(app_dir)) if f.endswith(".apk")]
        data = {}
        for apk in apks:
            data[apk] = {
                "classfiles": [os.path.relpath(d, app_dir) + os.sep for d in dirs],
                "sourcefiles": [os.path.relpath(s, app_dir) + os.sep for s in srcs],
            }
        json.dump(data, open(cfg, "w"), indent=2)

    print()
    print("analysable: %d   unresolved: %d" % (fixed, broken))
    print("APPLIED" if apply else "DRY RUN - pass --apply")
    return 0


if __name__ == "__main__":
    sys.exit(main())
