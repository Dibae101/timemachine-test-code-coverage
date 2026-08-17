#!/usr/bin/env python3
"""
resolve_modern.py - pick a buildable git ref for each upstream app.

The 26 apps in the hybriddroid-jacoco and 22-dataset groups are plain upstream
projects and the published report records no version for them. HEAD is a poor
default: current releases increasingly need JDK 21 and this host has only JDK 8
and 17. So each repo is inspected for tags with their creation dates, and the
newest tag at or before a cutoff (default 2023-06-30) is chosen - that era is
AGP 7.x / Gradle 7.x, the newest combination JDK 17 builds cleanly.

Tag discovery uses git, not the GitHub API: the API allows only 60 unauthenticated
requests an hour, which is nowhere near enough for 26 repos. A blobless bare
clone carries the full ref graph with dates for a few MB and is deleted straight
after.

Output: modern_subjects.json, consumable by build_dataset.py.
"""

import json
import os
import re
import shutil
import subprocess
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
CUTOFF = os.environ.get("TAG_CUTOFF", "2023-06-30")

# dataset name -> candidate upstream repos, tried in order
REPOS = [
    ("hybriddroid", "AntennaPod", ["https://github.com/AntennaPod/AntennaPod"]),
    ("hybriddroid", "feeder", ["https://github.com/spacecowboy/Feeder"]),
    ("hybriddroid", "myExpenses", ["https://github.com/mtotschnig/MyExpenses"]),
    ("hybriddroid", "newpipe", ["https://github.com/TeamNewPipe/NewPipe"]),
    ("hybriddroid", "OmniNotes", ["https://github.com/federicoiosue/Omni-Notes"]),
    ("hybriddroid", "owntracks", ["https://github.com/owntracks/android"]),
    ("hybriddroid", "RedReader", ["https://github.com/QuantumBadger/RedReader"]),
    ("hybriddroid", "SimpleAlarm", [
        "https://github.com/SimpleMobileTools/Simple-Alarm-Clock",
        "https://github.com/FossifyOrg/Clock"]),
    ("hybriddroid", "Wikipedia", ["https://github.com/wikimedia/apps-android-wikipedia"]),
    ("hybriddroid", "chess", [
        "https://github.com/albertoruibal/carballo",
        "https://github.com/gvlfm78/BukkitOldCombatMechanics"]),
    ("22-dataset", "GPSLogger", ["https://github.com/mendhak/gpslogger"]),
    ("22-dataset", "Infinity-For-Reddit", ["https://github.com/Docile-Alligator/Infinity-For-Reddit"]),
    ("22-dataset", "Kiwix", ["https://github.com/kiwix/kiwix-android"]),
    ("22-dataset", "Kore", ["https://github.com/xbmc/Kore"]),
    ("22-dataset", "Money-Manager-Ex", ["https://github.com/moneymanagerex/android-money-manager-ex"]),
    ("22-dataset", "Open-Food-Facts", ["https://github.com/openfoodfacts/openfoodfacts-androidapp"]),
    ("22-dataset", "Orgzly-Revived", ["https://github.com/orgzly-revived/orgzly-android-revived"]),
    ("22-dataset", "ownCloud", ["https://github.com/owncloud/android"]),
    ("22-dataset", "StreetComplete", ["https://github.com/streetcomplete/StreetComplete"]),
    ("22-dataset", "Trackbook", ["https://github.com/y20k/trackbook"]),
    ("22-dataset", "Transistor", ["https://github.com/y20k/transistor"]),
    ("22-dataset", "Twire", ["https://github.com/twireapp/Twire"]),
    ("22-dataset", "Ultrasonic", ["https://github.com/ultrasonic/ultrasonic"]),
    ("22-dataset", "Vinyl-Music-Player", ["https://github.com/AdrienPoupa/VinylMusicPlayer"]),
    ("22-dataset", "Wallabag", ["https://github.com/wallabag/android"]),
    ("22-dataset", "Fedilab", [
        "https://github.com/stom79/Fedilab",
        "https://github.com/stom79/mastalab"]),
]

VERSIONISH = re.compile(r"\d+\.\d+")


def alive(repo):
    r = subprocess.run(["git", "ls-remote", "--heads", repo],
                       capture_output=True, text=True, timeout=120)
    return r.returncode == 0


def dated_tags(repo):
    """[(date, tag)] from a blobless bare clone; empty list on failure."""
    tmp = tempfile.mkdtemp(prefix="refs-")
    bare = os.path.join(tmp, "r.git")
    try:
        r = subprocess.run(
            ["git", "clone", "--bare", "--filter=blob:none", "--quiet", repo, bare],
            capture_output=True, text=True, timeout=900)
        if r.returncode != 0:
            return []
        r = subprocess.run(
            ["git", "-C", bare, "for-each-ref", "--sort=creatordate",
             "--format=%(creatordate:short)\t%(refname:short)", "refs/tags"],
            capture_output=True, text=True, timeout=300)
        out = []
        for line in r.stdout.splitlines():
            if "\t" in line:
                date, tag = line.split("\t", 1)
                out.append((date.strip(), tag.strip()))
        return out
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def pick(tags):
    """Newest version-looking tag on or before CUTOFF."""
    versioned = [(d, t) for d, t in tags if VERSIONISH.search(t)]
    pool = versioned or tags
    eligible = [x for x in pool if x[0] <= CUTOFF]
    if eligible:
        return eligible[-1]
    return pool[-1] if pool else (None, None)


def main():
    out = []
    for group, name, candidates in REPOS:
        repo = next((c for c in candidates if alive(c)), None)
        if not repo:
            print("%-22s NO REACHABLE REPO (%s)" % (name, candidates[0]), flush=True)
            continue
        tags = dated_tags(repo)
        date, tag = pick(tags)
        print("%-22s %-12s %-22s %s" % (name, date or "HEAD", tag or "HEAD",
                                        repo.replace("https://github.com/", "")),
              flush=True)
        out.append({
            "name": name,
            "id": re.sub(r"[^\w.]", "", (tag or "head"))[:24],
            "group": group,
            "repo": repo,
            "ref": tag,
            "jdk": "17",
            "instrument": True,
            "themis_dir": None,
            "themis_apk": None,
            "tag_date": date,
        })
    path = os.path.join(HERE, "modern_subjects.json")
    with open(path, "w") as fh:
        json.dump(out, fh, indent=1)
    print("\nwrote %d subjects -> %s" % (len(out), path))


if __name__ == "__main__":
    main()
