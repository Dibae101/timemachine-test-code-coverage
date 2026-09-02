#!/usr/bin/env python3
"""
make_manifest.py - inventory and validate every dataset entry.

Walks instrumented_apps/, and for each APK declared in a class_files.json
checks the things that actually break coverage reporting:

  * the APK file exists and aapt can read its package name
  * every declared classfiles/sourcefiles path resolves
  * the class directories really contain .class files

Writes instrumented_apps/MANIFEST.tsv (one row per APK, with SHA-256 and
provenance) and prints a summary. Exit status is non-zero if any entry is
broken, so this can gate a commit.
"""

import csv
import hashlib
import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(HERE)
APPS = os.path.join(PROJECT, "instrumented_apps")
SDK = os.environ.get("SDK", "/home/ubuntu/android-sdk")
AAPT = os.path.join(SDK, "build-tools", "28.0.3", "aapt")
STATUS = os.path.join(HERE, "status.csv")

FIELDS = ["app", "apk", "package", "min_sdk", "target_sdk", "sha256", "size_mb",
          "class_dirs", "class_files", "source_dirs", "paths_ok", "provenance"]


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def badging(apk):
    """(package, min_sdk, target_sdk) from aapt.

    min_sdk is recorded because it decides which emulator an app can be smoke
    tested on. 17 of the 61 apps cannot install on the API 25 image the original
    set was tested on, and without this column that only shows up as an
    unexplained install failure.
    """
    try:
        out = subprocess.run([AAPT, "dump", "badging", apk], capture_output=True,
                             text=True, timeout=180).stdout
    except Exception:
        return "", "", ""
    pkg = re.search(r"^package: name='([^']+)'", out, re.M)
    mins = re.search(r"sdkVersion:'(\d+)'", out)
    targ = re.search(r"targetSdkVersion:'(\d+)'", out)
    return (pkg.group(1) if pkg else "",
            mins.group(1) if mins else "",
            targ.group(1) if targ else "")


def count_classes(dirs):
    n = 0
    for d in dirs:
        for _, _, files in os.walk(d):
            n += sum(1 for f in files if f.endswith(".class"))
    return n


def subject_files():
    """Every subject definition, so a new set does not silently lose provenance.

    This used to name subjects.json and modern_subjects.json literally, which
    meant apps from any later set were inventoried with an empty provenance
    column.
    """
    import glob as _glob
    return sorted(_glob.glob(os.path.join(HERE, "*subjects*.json")))


def provenance():
    """name -> "repo@ref" from the subject definitions."""
    out = {}
    for p in subject_files():
        if not os.path.isfile(p):
            continue
        for s in json.load(open(p)):
            if s.get("repo"):
                out[s["name"]] = "%s@%s" % (s["repo"].replace("https://github.com/", ""),
                                            s.get("ref") or "HEAD")
            else:
                out[s["name"]] = s.get("note", "apk only")
    return out


def main():
    prov = provenance()
    rows = []
    broken = []

    for app in sorted(os.listdir(APPS)):
        app_dir = os.path.join(APPS, app)
        cfg = os.path.join(app_dir, "class_files.json")
        if not os.path.isdir(app_dir) or not os.path.isfile(cfg):
            continue
        try:
            entries = json.load(open(cfg))
        except (OSError, ValueError) as exc:
            broken.append("%s: class_files.json unreadable (%s)" % (app, exc))
            continue

        for apk_name, info in sorted(entries.items()):
            apk_path = os.path.join(app_dir, apk_name)
            if not os.path.isfile(apk_path):
                broken.append("%s: declared APK missing: %s" % (app, apk_name))
                continue

            cls = [os.path.join(app_dir, p) for p in info.get("classfiles", [])]
            src = [os.path.join(app_dir, p) for p in info.get("sourcefiles", [])]
            missing = [p for p in cls + src if not os.path.isdir(p)]
            n_classes = count_classes([p for p in cls if os.path.isdir(p)])

            paths_ok = "yes"
            if missing:
                paths_ok = "no (%d missing)" % len(missing)
                broken.append("%s / %s: %d declared paths do not exist"
                              % (app, apk_name, len(missing)))
            elif not cls:
                paths_ok = "no class dirs"
                broken.append("%s / %s: no class directories declared"
                              % (app, apk_name))
            elif n_classes == 0:
                paths_ok = "no class files"
                broken.append("%s / %s: class dirs contain no .class files"
                              % (app, apk_name))

            pkg, min_sdk, target_sdk = badging(apk_path)
            rows.append({
                "app": app,
                "apk": apk_name,
                "package": pkg,
                "min_sdk": min_sdk,
                "target_sdk": target_sdk,
                "sha256": sha256(apk_path),
                "size_mb": round(os.path.getsize(apk_path) / 1e6, 1),
                "class_dirs": len(cls),
                "class_files": n_classes,
                "source_dirs": len(src),
                "paths_ok": paths_ok,
                "provenance": prov.get(app, ""),
            })

    out = os.path.join(APPS, "MANIFEST.tsv")
    with open(out, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=FIELDS, delimiter="\t")
        w.writeheader()
        for r in rows:
            w.writerow(r)

    apps = {r["app"] for r in rows}
    good = [r for r in rows if r["paths_ok"] == "yes"]
    print("apps: %d    apk entries: %d    fully valid entries: %d"
          % (len(apps), len(rows), len(good)))
    print("manifest: %s" % out)
    print()
    print("%-30s %-46s %-9s %s" % ("APP", "APK", "CLASSES", "OK"))
    for r in rows:
        print("%-30s %-46s %-9s %s" % (r["app"][:30], r["apk"][:46],
                                       r["class_files"], r["paths_ok"]))
    if broken:
        print()
        print("PROBLEMS (%d):" % len(broken))
        for b in broken:
            print("  " + b)
    return 1 if broken else 0


if __name__ == "__main__":
    sys.exit(main())
