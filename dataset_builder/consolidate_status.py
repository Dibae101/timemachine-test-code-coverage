#!/usr/bin/env python3
"""
consolidate_status.py - fold shard status files back into the parent status file.

Sharding gives every builder its own status file, which is what keeps two
builders from reverting each other. Re-sharding then needs the results collected
back into the parent file, otherwise the next pass rebuilds work that already
succeeded.

Entries can be dropped by name at the same time, which is how a subject that
built but produced a bad entry gets re-queued.

Usage
    python3 consolidate_status.py expansion_subjects [--drop Feeder ClockYou]
"""

import argparse
import csv
import glob
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
FIELDS = ["name", "id", "group", "state", "apk", "class_dirs", "class_files",
          "source_dirs", "size_mb", "seconds", "detail"]


def read(path):
    if not os.path.isfile(path):
        return {}
    with open(path) as fh:
        return {(r["name"], r["id"]): r for r in csv.DictReader(fh)}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("stem", help="subject set stem, e.g. expansion_subjects")
    ap.add_argument("--drop", nargs="*", default=[],
                    help="subject names to remove so they are rebuilt")
    ap.add_argument("--purge", action="store_true",
                    help="also delete the dropped subjects' app directories")
    args = ap.parse_args()

    parent = os.path.join(HERE, "status-%s.csv" % args.stem)
    rows = read(parent)
    shard_files = sorted(glob.glob(os.path.join(HERE, "status-%s-shard*.csv" % args.stem)))
    for sf in shard_files:
        for key, row in read(sf).items():
            prev = rows.get(key)
            # a later ok must not be overwritten by an earlier failure
            if prev and prev.get("state") in ("ok", "apk_only") \
                    and row.get("state") not in ("ok", "apk_only"):
                continue
            rows[key] = row

    dropped = []
    for name in args.drop:
        for key in [k for k in rows if k[0] == name]:
            dropped.append(key)
            rows.pop(key)
        if args.purge:
            d = os.path.join(os.path.dirname(HERE), "instrumented_apps", name)
            if os.path.isdir(d):
                shutil.rmtree(d, ignore_errors=True)

    with open(parent, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=FIELDS)
        w.writeheader()
        for key in sorted(rows):
            w.writerow({k: rows[key].get(k, "") for k in FIELDS})

    states = {}
    for r in rows.values():
        states[r.get("state", "")] = states.get(r.get("state", ""), 0) + 1
    print("merged %d shard files into %s" % (len(shard_files), os.path.basename(parent)))
    print("rows: %d  %s" % (len(rows), states))
    if dropped:
        print("dropped for rebuild: %s" % ", ".join(n for n, _ in dropped))

    # shard status files have been folded in; remove them so a later pass cannot
    # resurrect a stale row
    for sf in shard_files:
        os.remove(sf)
    for shard in glob.glob(os.path.join(HERE, "%s-shard*.json" % args.stem)):
        os.remove(shard)
    return 0


if __name__ == "__main__":
    sys.exit(main())
