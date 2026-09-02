#!/usr/bin/env python3
"""
shard_subjects.py - split a subject set into N files that can build in parallel.

build_dataset.py walks its subjects one at a time, and a single Android build is
mostly waiting on one gradle worker, so a 60-subject pass leaves most of a
16-core host idle. Sharding cuts the wall clock roughly by the number of shards.

Each shard gets its own file, and because build_dataset.py derives its status
path from the subjects filename, each shard also gets its own status file. That
matters: two builders sharing one status file each hold a snapshot and rewrite
the whole thing per subject, so the second silently reverts the first.

Subjects already marked ok/apk_only in the parent status file are dropped, so
re-sharding after a partial run does not rebuild what already succeeded.

Usage
    python3 shard_subjects.py expansion_subjects.json 4
"""

import csv
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))


def done_subjects(subjects_file):
    stem = os.path.splitext(os.path.basename(subjects_file))[0]
    status = os.path.join(HERE, "status-%s.csv" % stem)
    done = set()
    if os.path.isfile(status):
        with open(status) as fh:
            for r in csv.DictReader(fh):
                if r.get("state") in ("ok", "apk_only"):
                    done.add((r["name"], r["id"]))
    return done


def main():
    subjects_file = os.path.join(HERE, sys.argv[1])
    n = int(sys.argv[2]) if len(sys.argv) > 2 else 4
    subjects = json.load(open(subjects_file))
    done = done_subjects(subjects_file)

    pending = [s for s in subjects
               if (s["name"], s.get("id") or "0") not in done]
    stem = os.path.splitext(os.path.basename(subjects_file))[0]

    # Deal round-robin rather than in blocks: the list is not ordered by build
    # cost, and contiguous blocks put all the heavyweight projects together.
    shards = [[] for _ in range(n)]
    for i, s in enumerate(pending):
        shards[i % n].append(s)

    written = []
    for i, shard in enumerate(shards, 1):
        if not shard:
            continue
        path = os.path.join(HERE, "%s-shard%d.json" % (stem, i))
        json.dump(shard, open(path, "w"), indent=1)
        written.append((path, len(shard)))

    print("%d subjects, %d already done, %d pending"
          % (len(subjects), len(done), len(pending)))
    for path, count in written:
        print("  %-46s %d subjects" % (os.path.basename(path), count))


if __name__ == "__main__":
    main()
