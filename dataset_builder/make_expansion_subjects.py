#!/usr/bin/env python3
"""
make_expansion_subjects.py - turn resolved_tags.json into a subjects file.

Three of the resolved tags are not app releases and are pinned by hand:

  Etar-Calendar  the repository still carries the AOSP Calendar history, so its
                 highest-numbered tag is android-sdk-adt_r20 from 2012.
  KeePassDroid   its newest tags are GitHub `untagged-<sha>` placeholders.
  CPU-Info       tags are prefixed per platform; the wear- tag is not the phone
                 app.

Everything else takes the highest stable tag the remote advertises.
"""

import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))

# ref pinned by hand -> reason recorded in the subject as `note`
PINS = {
    "Etar-Calendar": ("v1.0.57", "repo retains AOSP tags; adt_r20 is not Etar"),
    "KeePassDroid": ("v2.6.9.2", "newer tags are untagged-<sha> placeholders"),
    "CPU-Info": ("android-6.6.2", "wear- tags are the Wear OS app"),
}

# Projects whose build era predates JDK 17. The builder still walks its JDK
# ladder, this only sets where it starts.
JDK_OVERRIDES = {
    "AlarmKlock": "11", "AnyMemo": "11", "KeePassDroid": "11",
    "Vanilla-Music": "11", "CEToolbox": "11", "Aard2": "11",
    "Currency": "11", "Diary": "11", "QuickDic": "11", "Lexica": "11",
    "DiskUsage": "11", "openScale": "11", "FastNFitness": "11",
    "PrivacyFriendlyNotes": "11", "Android-Chess": "11",
}


def sid(ref):
    """Directory-safe id, mirroring the ids already in instrumented_apps/."""
    return re.sub(r"[^A-Za-z0-9._]", "", ref)[:28] or "0"


def main():
    resolved = json.load(open(os.path.join(HERE, "resolved_tags.json")))
    existing = {d.lower() for d in os.listdir(
        os.path.join(os.path.dirname(HERE), "instrumented_apps"))}

    subjects, skipped = [], []
    for r in resolved:
        name = r["name"]
        if name.lower() in existing:
            skipped.append(name)
            continue
        ref, note = PINS.get(name, (r.get("ref"), None))
        if not ref:
            skipped.append(name)
            continue
        s = {
            "name": name,
            "id": sid(ref),
            "group": "expansion-50",
            "repo": r["repo"],
            "ref": ref,
            "jdk": JDK_OVERRIDES.get(name, "17"),
            "instrument": True,
            "themis_dir": None,
            "themis_apk": None,
        }
        if note:
            s["note"] = note
        subjects.append(s)

    out = os.path.join(HERE, "expansion_subjects.json")
    json.dump(subjects, open(out, "w"), indent=1)
    print("%d subjects -> %s" % (len(subjects), out))
    if skipped:
        print("skipped (already in dataset or unresolved): %s" % ", ".join(skipped))


if __name__ == "__main__":
    main()
