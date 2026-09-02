#!/usr/bin/env python3
"""
resolve_tags.py - ask each candidate repository which tags it actually has.

Guessing a tag name is the single most common way a subject fails: the clone
aborts with "Remote branch <ref> not found", the build is never attempted, and
the log says nothing about the real version scheme. `git ls-remote --tags` costs
one network round trip and removes the guess entirely.

Prints a JSON list of {name, repo, ref, tag_date} ready to be pasted into a
subjects file, and reports the repositories that expose no usable tag.

Usage
    python3 resolve_tags.py candidates.json > resolved.json
"""

import json
import re
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor

# Tags that are not app releases: nightly/CI markers and per-module tags.
NOISE = re.compile(
    r"(nightly|snapshot|^latest$|^continuous$|weekly|^ci-|-ci$|^build-\d+$)", re.I)


def tags(repo):
    """Every annotated/lightweight tag name a remote advertises, newest last."""
    try:
        out = subprocess.run(
            ["git", "ls-remote", "--tags", "--refs", repo],
            capture_output=True, text=True, timeout=180)
    except subprocess.TimeoutExpired:
        return None, "timeout"
    if out.returncode != 0:
        return None, (out.stderr.strip().splitlines() or ["ls-remote failed"])[-1][:120]
    names = []
    for line in out.stdout.splitlines():
        parts = line.split("refs/tags/")
        if len(parts) == 2:
            names.append(parts[1].strip())
    return names, None


def version_key(tag):
    """Sort key from the numeric components of a tag.

    Purely lexical sorting puts 3.9 above 3.10 and picks a stale release, which
    is how a "latest tag" heuristic silently selects a two-year-old version.
    """
    nums = [int(n) for n in re.findall(r"\d+", tag)[:5]]
    while len(nums) < 5:
        nums.append(0)
    pre = 1 if re.search(r"(alpha|beta|rc|dev|pre|snapshot|m\d)", tag, re.I) else 2
    return (pre, nums)


def pick(names, prefer_stable=True):
    """Highest-versioned tag, preferring stable over pre-release."""
    usable = [t for t in names if not NOISE.search(t) and re.search(r"\d", t)]
    if not usable:
        return None
    if prefer_stable:
        stable = [t for t in usable if not re.search(
            r"(alpha|beta|rc|dev|pre|snapshot)", t, re.I)]
        if stable:
            usable = stable
    return max(usable, key=version_key)


def resolve(cand):
    names, err = tags(cand["repo"])
    if err:
        return {**cand, "ref": None, "error": err, "n_tags": 0}
    if not names:
        return {**cand, "ref": None, "error": "no tags", "n_tags": 0}
    ref = cand.get("ref") if cand.get("pin") else pick(names)
    if ref and ref not in names:
        ref = pick(names)
    return {**cand, "ref": ref, "n_tags": len(names),
            "sample": sorted(names, key=version_key)[-4:]}


def main():
    candidates = json.load(open(sys.argv[1]))
    with ThreadPoolExecutor(max_workers=10) as pool:
        results = list(pool.map(resolve, candidates))
    ok = [r for r in results if r.get("ref")]
    bad = [r for r in results if not r.get("ref")]
    json.dump(results, open("resolved_tags.json", "w"), indent=1)
    for r in ok:
        print("OK    %-26s %-44s %s" % (r["name"], r["ref"], r.get("sample")))
    for r in bad:
        print("BAD   %-26s %s" % (r["name"], r.get("error")))
    print("\n%d resolved, %d unusable -> resolved_tags.json" % (len(ok), len(bad)))


if __name__ == "__main__":
    main()
