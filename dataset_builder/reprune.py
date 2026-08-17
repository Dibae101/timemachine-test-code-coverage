#!/usr/bin/env python3
"""
reprune.py - re-apply pruning to datasets that were built before the pruner
walked build/ directories recursively.

Reads each app's class_files.json, treats the declared classfiles/sourcefiles
(plus their class-output containers) as the keep set, and deletes everything else
under any build/ directory. Validates afterwards that every declared path still
resolves, and refuses to touch an app whose declarations are already broken.

    python3 reprune.py            # report only
    python3 reprune.py --apply    # actually delete
"""

import argparse
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
APPS = os.path.join(os.path.dirname(HERE), "instrumented_apps")

sys.path.insert(0, HERE)
from build_dataset import dir_size_mb, prune_tree  # noqa: E402


def keep_set(app_dir, entries):
    keep = []
    for info in entries.values():
        for key in ("classfiles", "sourcefiles"):
            for rel in info.get(key, []):
                keep.append(os.path.normpath(os.path.join(app_dir, rel)))
    containers = []
    for d in keep:
        for marker in ("intermediates/javac", "intermediates/classes",
                       "tmp/kotlin-classes"):
            idx = d.replace(os.sep, "/").find(marker)
            if idx >= 0:
                containers.append(d[:idx + len(marker)])
    return keep + containers


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args()

    total_before = total_after = 0
    for app in sorted(os.listdir(APPS)):
        app_dir = os.path.join(APPS, app)
        cfg = os.path.join(app_dir, "class_files.json")
        if not os.path.isdir(app_dir) or not os.path.isfile(cfg):
            continue
        try:
            entries = json.load(open(cfg))
        except (OSError, ValueError):
            print("%-30s skipped (unreadable class_files.json)" % app)
            continue

        keep = keep_set(app_dir, entries)
        missing = [k for k in keep if not os.path.isdir(k)]
        if missing:
            print("%-30s skipped (%d declared paths already missing)"
                  % (app, len(missing)))
            continue

        before = dir_size_mb(app_dir)
        total_before += before
        if not args.apply:
            print("%-30s %8.1f MB  (dry run)" % (app, before))
            total_after += before
            continue

        for entry in os.listdir(app_dir):
            tree = os.path.join(app_dir, entry)
            if os.path.isdir(tree):
                prune_tree(tree, keep)

        still_missing = [k for k in keep if not os.path.isdir(k)]
        after = dir_size_mb(app_dir)
        total_after += after
        flag = "" if not still_missing else "  BROKEN: %d paths lost" % len(still_missing)
        print("%-30s %8.1f -> %8.1f MB%s" % (app, before, after, flag))

    print()
    print("total: %.1f -> %.1f MB" % (total_before, total_after))


if __name__ == "__main__":
    main()
