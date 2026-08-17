#!/usr/bin/env python3
"""
keep_unique_ready.py - reduce the repository to one ready dataset per app.

Keeps exactly one APK entry for each app that produced a clean coverage report,
and the single smoke result belonging to it. Everything else is removed:

  * apps whose report excludes classes as mismatched, or produces none at all
  * duplicate APK entries inside a surviving app, so no app appears twice
  * smoke results for anything removed
  * submodule registrations for removed apps

Where an app has both a published and a locally rebuilt APK, the rebuilt one is
kept: its classes are byte-for-byte the ones declared in class_files.json, so it
cannot drift. MaterialFBook is the exception, having only a published APK.

Removals affect the working tree only; git history keeps everything.

    python3 keep_unique_ready.py --dry-run
    python3 keep_unique_ready.py --apply
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


def git(*args):
    return subprocess.run(["git"] + list(args), cwd=PROJECT,
                          capture_output=True, text=True, timeout=1800)


def choose():
    """app -> the single passing APK entry to keep."""
    rows = list(csv.DictReader(open(SUMMARY)))
    best = {}
    for r in rows:
        k = (r["app"], r["apk"])
        score = (r["verdict"] == "PASS", float(r["line_coverage_pct"] or 0))
        cur = best.get(k)
        if cur is None or score > (cur["verdict"] == "PASS",
                                   float(cur["line_coverage_pct"] or 0)):
            best[k] = r

    chosen = {}
    for r in best.values():
        if r["verdict"] != "PASS":
            continue
        app = r["app"]
        score = ("rebuilt" in r["apk"], float(r["line_coverage_pct"] or 0))
        cur = chosen.get(app)
        if cur is None or score > ("rebuilt" in cur["apk"],
                                   float(cur["line_coverage_pct"] or 0)):
            chosen[app] = r
    return chosen, list(best.values())


def submodule_paths():
    out = set()
    p = os.path.join(PROJECT, ".gitmodules")
    if os.path.isfile(p):
        for line in open(p):
            if line.strip().startswith("path ="):
                out.add(line.split("=", 1)[1].strip())
    return out


def drop_submodule(rel_sub):
    git("submodule", "deinit", "-f", "--", rel_sub)
    git("rm", "-q", "-rf", "--", rel_sub)
    shutil.rmtree(os.path.join(PROJECT, ".git", "modules", *rel_sub.split("/")),
                  ignore_errors=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    apply = args.apply and not args.dry_run

    chosen, all_rows = choose()
    subs = submodule_paths()

    print("keeping %d apps, one APK each" % len(chosen))
    for app, r in sorted(chosen.items()):
        print("   %-28s %-44s %6s%%" % (app, r["apk"], r["line_coverage_pct"]))

    # ---- whole apps -------------------------------------------------------
    print()
    for app in sorted(d for d in os.listdir(APPS)
                      if os.path.isdir(os.path.join(APPS, d))):
        if app in chosen:
            continue
        rel = "instrumented_apps/" + app
        sub = rel + "/upstream"
        print("drop app   %s%s" % (app, "  (+submodule)" if sub in subs else ""))
        if apply:
            if sub in subs:
                drop_submodule(sub)
            git("rm", "-q", "-rf", "--cached", "--ignore-unmatch", "--", rel)
            shutil.rmtree(os.path.join(PROJECT, rel), ignore_errors=True)

    # ---- duplicate APKs within kept apps ---------------------------------
    print()
    for app, r in sorted(chosen.items()):
        app_dir = os.path.join(APPS, app)
        keep_apk = r["apk"]
        extra = [f for f in sorted(os.listdir(app_dir))
                 if f.endswith(".apk") and f != keep_apk]
        if not extra:
            continue
        print("drop apk   %-28s %s" % (app, ", ".join(extra)))
        if not apply:
            continue
        for f in extra:
            git("rm", "-q", "-f", "--ignore-unmatch", "--",
                "instrumented_apps/%s/%s" % (app, f))
            p = os.path.join(app_dir, f)
            if os.path.exists(p):
                os.remove(p)
        cfg = os.path.join(app_dir, "class_files.json")
        if os.path.isfile(cfg):
            data = json.load(open(cfg))
            data = {k: v for k, v in data.items() if k == keep_apk}
            json.dump(data, open(cfg, "w"), indent=2)

    # ---- smoke results ---------------------------------------------------
    print()
    keep_slugs = {r["apk"][:-4].replace("#", "_") + "_" for r in chosen.values()}
    for d in sorted(x for x in os.listdir(SMOKE)
                    if os.path.isdir(os.path.join(SMOKE, x))):
        if d in keep_slugs:
            continue
        print("drop smoke %s" % d)
        if apply:
            rel = "results/smoke/" + d
            git("rm", "-q", "-rf", "--cached", "--ignore-unmatch", "--", rel)
            shutil.rmtree(os.path.join(PROJECT, rel), ignore_errors=True)

    # ---- summary tables --------------------------------------------------
    if apply:
        keep_pairs = {(r["app"], r["apk"]) for r in chosen.values()}
        rows = list(csv.DictReader(open(SUMMARY)))
        kept = []
        seen = set()
        for r in rows:
            k = (r["app"], r["apk"])
            if k in keep_pairs and k not in seen and r["verdict"] == "PASS":
                kept.append(r)
                seen.add(k)
        with open(SUMMARY, "w", newline="") as fh:
            w = csv.DictWriter(fh, fieldnames=list(rows[0].keys()))
            w.writeheader()
            w.writerows(kept)
        print()
        print("smoke_summary.csv: %d rows -> %d" % (len(rows), len(kept)))

        probe = os.path.join(SMOKE, "probe_summary_all.csv")
        if os.path.isfile(probe):
            prows = list(csv.DictReader(open(probe)))
            pk = [r for r in prows if r["result_dir"] in keep_slugs]
            with open(probe, "w", newline="") as fh:
                w = csv.DictWriter(fh, fieldnames=list(prows[0].keys()))
                w.writeheader()
                w.writerows(pk)
            print("probe_summary_all.csv: %d rows -> %d" % (len(prows), len(pk)))

    print()
    print("APPLIED" if apply else "DRY RUN - pass --apply")
    return 0


if __name__ == "__main__":
    sys.exit(main())
