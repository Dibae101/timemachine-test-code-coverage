#!/usr/bin/env python3
"""
prune_to_tested.py - reduce the repository to datasets proven to work.

Keeps only what the smoke run verified: an app directory survives when at least
one of its APK entries produced a clean coverage report (verdict PASS), and
within a surviving app only the PASS entries are kept. Everything else goes:

  * app directories that are source-only checkouts (no class_files.json)
  * app directories whose entries produce execution data but no report
  * APK entries that report a class mismatch, which happens where an
    upstream-published APK is paired with locally compiled classes
  * the smoke results belonging to any removed entry

Submodules are de-registered properly, so .gitmodules keeps only the apps that
remain.

Everything removed stays recoverable from git history; this rewrites the working
tree, not the past.

    python3 prune_to_tested.py --dry-run
    python3 prune_to_tested.py --apply
"""

import argparse
import csv
import json
import os
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(HERE)
APPS = os.path.join(PROJECT, "instrumented_apps")
SMOKE = os.path.join(PROJECT, "results", "smoke")
SUMMARY = os.path.join(SMOKE, "smoke_summary.csv")


def git(*args, check=False):
    r = subprocess.run(["git"] + list(args), cwd=PROJECT, capture_output=True,
                       text=True, timeout=1800)
    if check and r.returncode != 0:
        raise RuntimeError("git %s: %s" % (" ".join(args), r.stderr[-300:]))
    return r


def latest_verdicts():
    rows = list(csv.DictReader(open(SUMMARY)))
    last = {}
    for r in rows:
        last[(r["app"], r["apk"])] = r
    return last


def submodule_paths():
    out = set()
    if not os.path.isfile(os.path.join(PROJECT, ".gitmodules")):
        return out
    for line in open(os.path.join(PROJECT, ".gitmodules")):
        line = line.strip()
        if line.startswith("path ="):
            out.add(line.split("=", 1)[1].strip())
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    apply = args.apply and not args.dry_run

    last = latest_verdicts()
    keep_entries = {(a, k) for (a, k), r in last.items() if r["verdict"] == "PASS"}
    keep_apps = {a for a, _ in keep_entries}

    all_dirs = sorted(d for d in os.listdir(APPS)
                      if os.path.isdir(os.path.join(APPS, d)))
    drop_dirs = [d for d in all_dirs if d not in keep_apps]
    subs = submodule_paths()

    print("keeping %d apps: %s" % (len(keep_apps), ", ".join(sorted(keep_apps))))
    print()

    # ---- whole app directories -------------------------------------------
    for d in drop_dirs:
        rel = os.path.join("instrumented_apps", d)
        sub = os.path.join(rel, "upstream")
        print("drop app  %s%s" % (d, "  (+submodule)" if sub in subs else ""))
        if not apply:
            continue
        if sub in subs:
            git("submodule", "deinit", "-f", "--", sub)
            git("rm", "-q", "-rf", "--", sub)
            shutil.rmtree(os.path.join(PROJECT, ".git", "modules",
                                       *sub.split("/")), ignore_errors=True)
        git("rm", "-q", "-rf", "--cached", "--ignore-unmatch", "--", rel)
        shutil.rmtree(os.path.join(PROJECT, rel), ignore_errors=True)

    # ---- non-passing entries inside surviving apps ------------------------
    print()
    for app in sorted(keep_apps):
        app_dir = os.path.join(APPS, app)
        cfg = os.path.join(app_dir, "class_files.json")
        entries = json.load(open(cfg)) if os.path.isfile(cfg) else {}
        keep = {k: v for k, v in entries.items() if (app, k) in keep_entries}
        drop = [k for k in entries if k not in keep]
        # apks present but never declared or never passing
        for f in sorted(os.listdir(app_dir)):
            if f.endswith(".apk") and (app, f) not in keep_entries and f not in drop:
                drop.append(f)
        if not drop:
            print("keep app  %-28s all %d entr%s pass"
                  % (app, len(keep), "y" if len(keep) == 1 else "ies"))
            continue
        print("keep app  %-28s keep %d, drop %s"
              % (app, len(keep), ", ".join(drop)))
        if not apply:
            continue
        for f in drop:
            p = os.path.join(app_dir, f)
            if os.path.isfile(p):
                git("rm", "-q", "-f", "--ignore-unmatch", "--",
                    os.path.join("instrumented_apps", app, f))
                if os.path.exists(p):
                    os.remove(p)
        json.dump(keep, open(cfg, "w"), indent=2)

    # ---- smoke results ---------------------------------------------------
    print()
    kept_slugs = {k[:-4].replace("#", "_") + "_" for (_, k) in keep_entries}
    sdirs = sorted(d for d in os.listdir(SMOKE)
                   if os.path.isdir(os.path.join(SMOKE, d)))
    for d in sdirs:
        if d in kept_slugs:
            continue
        print("drop smoke %s" % d)
        if apply:
            rel = os.path.join("results", "smoke", d)
            git("rm", "-q", "-rf", "--cached", "--ignore-unmatch", "--", rel)
            shutil.rmtree(os.path.join(PROJECT, rel), ignore_errors=True)

    # ---- rewrite the summary tables to match ----------------------------
    if apply:
        rows = list(csv.DictReader(open(SUMMARY)))
        fields = rows[0].keys()
        kept = [r for r in rows if (r["app"], r["apk"]) in keep_entries]
        with open(SUMMARY, "w", newline="") as fh:
            w = csv.DictWriter(fh, fieldnames=list(fields))
            w.writeheader()
            w.writerows(kept)
        print()
        print("smoke_summary.csv: %d rows -> %d" % (len(rows), len(kept)))

        probe = os.path.join(SMOKE, "probe_summary_all.csv")
        if os.path.isfile(probe):
            prows = list(csv.DictReader(open(probe)))
            pkept = [r for r in prows if r["result_dir"] in kept_slugs]
            with open(probe, "w", newline="") as fh:
                w = csv.DictWriter(fh, fieldnames=list(prows[0].keys()))
                w.writeheader()
                w.writerows(pkept)
            print("probe_summary_all.csv: %d rows -> %d" % (len(prows), len(pkept)))

    print()
    print("APPLIED" if apply else "DRY RUN - nothing changed; pass --apply")
    return 0


if __name__ == "__main__":
    sys.exit(main())
