#!/usr/bin/env python3
"""
diagnose.py - summarise why subjects failed, grouped by cause.

Reads status.csv plus the per-subject build logs and prints a tally of distinct
failure reasons. Each pass of build_dataset.py usually reveals one systemic
cause affecting many subjects (a dead repository, a quality gate, a missing
SDK platform), so grouping is far more useful than reading logs one by one.

    python3 diagnose.py            # tally + per-subject reason
    python3 diagnose.py --verbose  # include the raw gradle message
"""

import argparse
import collections
import csv
import glob
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
LOGS = os.path.join(HERE, "logs")
STATUS_GLOB = os.path.join(HERE, "status-*.csv")

# ordered: first match wins, so specific patterns go above generic ones
PATTERNS = [
    ("missing dependency (not on any mirror)", r"Could not find ([\w.\-:]+)"),
    ("missing SDK platform / build-tools", r"(Failed to install the following|not accepted the license|SDK location not found|Could not determine java version|Installed Build Tools revision)"),
    ("dex 64K limit", r"DexIndexOverflowException"),
    ("quality gate", r"(Checkstyle rule violations|ktlint|detekt|SpotBugs|Lint found)"),
    ("kotlin/java version mismatch", r"(Unsupported class file major version|Unsupported Java|invalid source release|Kotlin could not|no longer supported|requires Java|Android Gradle plugin requires Java)"),
    ("compile error in app code", r"error: (cannot find symbol|package .* does not exist|incompatible types)"),
    ("manifest merge", r"Manifest merger failed"),
    ("out of memory", r"(OutOfMemoryError|Java heap space|GC overhead)"),
    ("network / resolution", r"(Connection reset|Read timed out|502 Bad Gateway|Could not GET|Could not HEAD|peer not authenticated)"),
    ("gradle/agp incompatibility", r"(Could not find method|Unsupported method|No such property|Minimum supported Gradle version|plugin \[id)"),
    ("build timeout", r"\*\*\* TIMEOUT \*\*\*"),
    ("stack overflow in gradle", r"StackOverflowError"),
]


def reason_for(name, sid):
    path = os.path.join(LOGS, "%s-%s.log" % (name, sid))
    if not os.path.isfile(path):
        return "no log", ""
    try:
        text = open(path, encoding="utf-8", errors="replace").read()
    except OSError:
        return "log unreadable", ""
    # the last failure block is the one that matters after retries
    tail = text[-400000:]
    for label, pattern in PATTERNS:
        m = re.search(pattern, tail)
        if m:
            return label, m.group(0)[:150]
    m = re.search(r"What went wrong:\s*\n(.{0,180})", tail, re.S)
    return "other", (m.group(1).strip().replace("\n", " ") if m else "")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--verbose", action="store_true")
    args = ap.parse_args()

    files = sorted(glob.glob(STATUS_GLOB))
    if not files:
        print("no status-*.csv yet")
        return
    rows = []
    for f in files:
        rows.extend(csv.DictReader(open(f)))
    ok = [r for r in rows if r["state"] in ("ok", "apk_only")]
    bad = [r for r in rows if r["state"] not in ("ok", "apk_only")]

    print("usable: %d    failing: %d    total: %d" % (len(ok), len(bad), len(rows)))
    print()

    tally = collections.Counter()
    detail = []
    for r in bad:
        label, snippet = reason_for(r["name"], r["id"])
        if r["state"] in ("instrument_failed", "error", "clone_failed", "no_products"):
            label = r["state"] + ": " + (r.get("detail") or label)[:70]
        tally[label] += 1
        detail.append((label, r["name"], r["id"], snippet))

    print("FAILURE CAUSES")
    for label, n in tally.most_common():
        print("  %-58s %d" % (label[:58], n))
    print()
    print("PER SUBJECT")
    for label, name, sid, snippet in sorted(detail):
        print("  %-26s %-10s %s" % (name[:26], sid[:10], label[:60]))
        if args.verbose and snippet:
            print("        %s" % snippet[:150])
    print()
    print("USABLE SUBJECTS")
    for r in sorted(ok, key=lambda r: r["name"]):
        print("  %-28s %-8s %-6s classes=%-6s src=%-3s %s MB"
              % (r["name"][:28], r["id"][:8], r["state"], r.get("class_files") or "-",
                 r.get("source_dirs") or "-", r.get("size_mb") or "-"))


if __name__ == "__main__":
    main()
