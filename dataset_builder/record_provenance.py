#!/usr/bin/env python3
"""
record_provenance.py - pin every dataset entry to an immutable commit.

Each entry records the repository and the ref it was built from, but a ref is a
name, not a commit. Tags move, branches certainly move, and the builder deletes a
checkout's .git immediately after cloning, so the commit that produced an entry
was never written down anywhere.

This resolves every ref to a SHA with `git ls-remote` and writes
instrumented_apps/PROVENANCE.tsv.

One honest limitation: this resolves the ref as it stands *today*. For an entry
built earlier it is evidence, not proof - if a tag was moved in between, the SHA
recorded here is not the one that was built. build_dataset.py now captures the SHA
at clone time, before .git is removed, so entries built from here on are pinned
exactly.

Usage
    python3 record_provenance.py
    python3 record_provenance.py Markor Kiwix      # substring filters
"""

import csv
import glob
import json
import os
import re
import subprocess
import sys
import time
from concurrent.futures import ThreadPoolExecutor

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(HERE)
APPS = os.path.join(PROJECT, "instrumented_apps")
OUT = os.path.join(APPS, "PROVENANCE.tsv")

FIELDS = ["app", "apk", "repo", "ref", "ref_type", "commit", "source",
          "resolved_at"]


def subject_index():
    """{(name, id): (repo, ref, subject_file)} across every subject set."""
    idx = {}
    for path in sorted(glob.glob(os.path.join(HERE, "*subjects*.json"))):
        if "shard" in os.path.basename(path):
            continue
        try:
            entries = json.load(open(path))
        except (OSError, ValueError):
            continue
        for s in entries:
            if not s.get("repo"):
                continue
            idx[(s["name"], str(s.get("id") or "0"))] = (
                s["repo"], s.get("ref"), os.path.basename(path))
    return idx


def tree_id(app_dir, app):
    for entry in sorted(os.listdir(app_dir)):
        if entry.startswith(app + "-#") and os.path.isdir(os.path.join(app_dir, entry)):
            return entry[len(app) + 2:]
    return ""


def resolve(repo, ref):
    """(ref_type, sha) for a ref on a remote, without cloning it."""
    if not repo or not ref:
        return "", ""
    try:
        r = subprocess.run(["git", "ls-remote", repo,
                            "refs/tags/" + ref, "refs/heads/" + ref],
                           capture_output=True, text=True, timeout=180)
    except subprocess.TimeoutExpired:
        return "timeout", ""
    if r.returncode != 0:
        return "unreachable", ""
    tag = branch = ""
    for line in r.stdout.splitlines():
        parts = line.split()
        if len(parts) != 2:
            continue
        sha, name = parts
        if name == "refs/tags/" + ref:
            tag = sha
        elif name == "refs/heads/" + ref:
            branch = sha
    if tag:
        return "tag", tag
    if branch:
        return "branch", branch
    return "not found", ""


def main():
    filters = [a.lower() for a in sys.argv[1:]]
    idx = subject_index()
    manifest = os.path.join(APPS, "MANIFEST.tsv")
    if not os.path.isfile(manifest):
        sys.exit("no MANIFEST.tsv; run make_manifest.py first")

    jobs = []
    for r in csv.DictReader(open(manifest), delimiter="\t"):
        app, apk = r["app"], r["apk"]
        if filters and not any(f in app.lower() for f in filters):
            continue
        app_dir = os.path.join(APPS, app)
        tid = tree_id(app_dir, app)
        repo, ref, src = idx.get((app, tid), (None, None, None))
        if repo is None:
            # fall back to any subject entry for this app
            for (name, _id), (rp, rf, sf) in idx.items():
                if name == app:
                    repo, ref, src = rp, rf, sf
                    break
        jobs.append({"app": app, "apk": apk, "repo": repo or "", "ref": ref or "",
                     "source": src or "", "tree_id": tid})

    def work(j):
        j["ref_type"], j["commit"] = resolve(j["repo"], j["ref"])
        j["resolved_at"] = time.strftime("%Y-%m-%d")
        return j

    with ThreadPoolExecutor(max_workers=8) as pool:
        rows = list(pool.map(work, jobs))

    with open(OUT, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=FIELDS, delimiter="\t",
                           extrasaction="ignore")
        w.writeheader()
        for r in sorted(rows, key=lambda x: x["app"]):
            w.writerow(r)

    pinned = [r for r in rows if r["commit"]]
    print("entries: %d   pinned to a commit: %d" % (len(rows), len(pinned)))
    print("provenance: %s" % OUT)
    by_type = {}
    for r in rows:
        by_type[r["ref_type"]] = by_type.get(r["ref_type"], 0) + 1
    print("ref types: %s" % by_type)
    unresolved = [r for r in rows if not r["commit"]]
    if unresolved:
        print("\nnot resolvable (%d):" % len(unresolved))
        for r in unresolved:
            print("   %-24s ref=%-34s %s" % (r["app"], r["ref"] or "<none>",
                                             r["ref_type"] or "no subject entry"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
