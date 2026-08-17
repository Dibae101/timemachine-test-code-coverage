#!/usr/bin/env python3
"""
reconcile.py - rebuild builder state from what is actually on disk.

The dataset itself is the source of truth: an app is usable when its
class_files.json declares an APK that exists alongside class directories that
really contain .class files. This script regenerates MANIFEST.tsv, then marks
each subject ok/failed in the per-subject-set status files to match.

Needed because a shared status.csv was clobbered by two concurrent builders, and
useful in general: a status file can always be reconstructed after a crash.
"""

import csv
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(HERE)
APPS = os.path.join(PROJECT, "instrumented_apps")
FIELDS = ["name", "id", "group", "state", "apk", "class_dirs", "class_files",
          "source_dirs", "size_mb", "seconds", "detail"]


def valid_entries():
    """app -> (best apk, class_files) for apps with a working declaration."""
    subprocess.run([sys.executable, os.path.join(HERE, "make_manifest.py")],
                   capture_output=True, text=True, timeout=3600)
    path = os.path.join(APPS, "MANIFEST.tsv")
    best = {}
    if not os.path.isfile(path):
        return best
    with open(path) as fh:
        for r in csv.DictReader(fh, delimiter="\t"):
            if r["paths_ok"] != "yes":
                continue
            try:
                n = int(r["class_files"])
            except ValueError:
                continue
            if n <= 0:
                continue
            cur = best.get(r["app"])
            if not cur or n > cur[1]:
                best[r["app"]] = (r["apk"], n, r["class_dirs"], r["source_dirs"],
                                  r["size_mb"])
    return best


def main():
    good = valid_entries()
    print("apps with a valid dataset entry: %d" % len(good))

    for fname in ("subjects.json", "modern_subjects.json"):
        subs = json.load(open(os.path.join(HERE, fname)))
        stem = os.path.splitext(fname)[0]
        status_path = os.path.join(HERE, "status-%s.csv" % stem)
        old = {}
        if os.path.isfile(status_path):
            for r in csv.DictReader(open(status_path)):
                old[(r["name"], r["id"])] = r

        rows = []
        for s in subs:
            key = (s["name"], s.get("id") or "0")
            prev = old.get(key, {})
            row = {f: prev.get(f, "") for f in FIELDS}
            row.update(name=key[0], id=key[1], group=s.get("group", ""))
            if key[0] in good:
                apk, n, cd, sd, size = good[key[0]]
                row.update(state="ok", apk=apk, class_files=str(n),
                           class_dirs=cd, source_dirs=sd, size_mb=size,
                           detail=prev.get("detail") or "reconciled from disk")
            elif not s.get("repo"):
                row.update(state="apk_only",
                           detail=s.get("note", "no source branch"))
            elif not row["state"] or row["state"] == "ok":
                row.update(state=prev.get("state") or "pending",
                           detail=prev.get("detail", ""))
            rows.append(row)

        with open(status_path, "w", newline="") as fh:
            w = csv.DictWriter(fh, fieldnames=FIELDS)
            w.writeheader()
            w.writerows(rows)
        ok = sum(1 for r in rows if r["state"] in ("ok", "apk_only"))
        print("%-28s %d subjects, %d usable" % (os.path.basename(status_path),
                                                len(rows), ok))


if __name__ == "__main__":
    main()
