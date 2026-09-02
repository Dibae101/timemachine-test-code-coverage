#!/usr/bin/env python3
"""
repair_declarations.py - pick the class directories that match a recorded run.

When jacococli reports "Execution data for class X does not match", the declared
directory holds different bytes for X than the ones that ran. Coverage for that
class is dropped silently. Two things cause it in this dataset:

  * the wrong product flavour is declared - Markor ships its flavorAtest APK but
    had flavorDefault classes declared, and two classes differ between them
  * output taken from before a bytecode-rewriting plugin, typically Hilt, which
    rewrites @AndroidEntryPoint classes after javac has run

The .ec file settles it without guessing: it records the CRC64 id of every class
that actually ran. This walks the candidate directories in each app's build tree,
scores them with tools/MatchClasses, and rewrites class_files.json to the best
scoring set. An app whose correct directory was pruned away is reported as needing
a rebuild instead.

Usage
    python3 repair_declarations.py results/smoke_test
    python3 repair_declarations.py results/smoke_test Markor Aegis
"""

import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(HERE)
APPS = os.path.join(PROJECT, "instrumented_apps")
TOOLS = os.path.join(HERE, "tools")
JAVA = os.environ.get("JAVA", "/usr/lib/jvm/java-17-openjdk-arm64/bin/java")

sys.path.insert(0, HERE)
import build_dataset as bd  # noqa: E402  - reuses the discovery rules


def classpath():
    jars = [os.path.join(TOOLS, "lib", j)
            for j in sorted(os.listdir(os.path.join(TOOLS, "lib")))
            if j.endswith(".jar")]
    return os.pathsep.join(jars + [os.path.join(TOOLS, "classes")])


def smoke_slug(apk):
    stem = apk[:-4] if apk.endswith(".apk") else apk
    return "".join(c if re.match(r"[A-Za-z0-9._-]", c) else "_" for c in stem) + "_"


def candidate_roots(tree):
    """Every plausible class root, including ones find_class_dirs excludes.

    The excluded ones matter here: for a Hilt app the transformClassesWithAsm
    output is the bytecode that actually ran, so it has to be scored rather than
    filtered out.
    """
    roots = set()
    for build_root, dirs, _ in os.walk(tree):
        if ".git" in dirs:
            dirs.remove(".git")
        if os.path.basename(build_root) != "build":
            continue
        for root, _, files in os.walk(build_root):
            if not any(f.endswith(".class") for f in files):
                continue
            derived = bd.class_root_of(root)
            if not derived:
                continue
            rel = os.path.relpath(derived, tree).replace(os.sep, "/").lower()
            # instrumented copies can never match: their bytes carry the probes
            if any(s.startswith("jacoco") for s in rel.split("/")):
                continue
            if bd.holds_instrumented_output(derived):
                continue
            # Test output is deliberately NOT filtered here. It scores zero
            # matches against a run of the app and is dropped on evidence, and
            # filtering by name is what hid Markor's flavorAtestDebug.
            roots.add(derived)
    return sorted(roots)


def pick_roots(sc, cp):
    """Largest agreeing set of directories: most matched classes, no mismatches.

    Ordering by id_match rather than by directory kind is what matters. Droid-ify's
    transformDebugClassesWithAsm output holds 1695 classes with 479 matches and no
    mismatch, while its javac output holds 252 with 59; a rule that prefers "primary"
    output picks the small one and throws away most of the app.
    """
    ranked = sorted(((hit, n, d) for d, (n, hit, bad) in sc.items()
                     if bad == 0 and hit > 0), reverse=True)
    picked, taken = [], set()
    for _, _, d in ranked:
        names = bd.class_names_in(d)
        if names & taken:
            continue
        picked.append(d)
        taken |= names
    return sorted(picked), len(taken)


def score(ec, roots, cp):
    """{root: (n_classes, id_match, name_only_mismatch)} from MatchClasses."""
    if not roots:
        return {}
    out = subprocess.run([JAVA, "-cp", cp, "MatchClasses", ec] + roots,
                         capture_output=True, text=True, timeout=3600)
    scores = {}
    for line in out.stdout.splitlines():
        m = re.match(r"\s*classes=(\d+)\s+id_match=(\d+)\s+name_only_mismatch=(\d+)\s+(.+)$",
                     line)
        if m:
            scores[m.group(4).strip()] = (int(m.group(1)), int(m.group(2)),
                                          int(m.group(3)))
    if not scores and out.stderr:
        print("      MatchClasses: %s" % out.stderr.strip().splitlines()[-1][:120])
    return scores


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    results = os.path.join(PROJECT, sys.argv[1]) if not os.path.isabs(sys.argv[1]) \
        else sys.argv[1]
    filters = [a.lower() for a in sys.argv[2:]]
    cp = classpath()

    repaired, need_rebuild, clean = [], [], []
    for app in sorted(os.listdir(APPS)):
        app_dir = os.path.join(APPS, app)
        cfg = os.path.join(app_dir, "class_files.json")
        if not os.path.isfile(cfg):
            continue
        if filters and not any(f in app.lower() for f in filters):
            continue
        entries = json.load(open(cfg))
        changed = False
        for apk, info in sorted(entries.items()):
            ec = os.path.join(results, smoke_slug(apk), "coverage.ec")
            if not os.path.isfile(ec) or os.path.getsize(ec) < 1024:
                continue
            trees = [os.path.join(app_dir, d) for d in os.listdir(app_dir)
                     if os.path.isdir(os.path.join(app_dir, d)) and d.startswith(app + "-#")]
            if not trees:
                continue
            tree = trees[0]

            declared = [os.path.join(app_dir, p) for p in info["classfiles"]]
            cands = candidate_roots(tree)
            for d in declared:
                if os.path.isdir(d) and d not in cands:
                    cands.append(d)
            sc = score(ec, cands, cp)
            if not sc:
                continue

            declared_bad = sum(sc.get(d, (0, 0, 0))[2] for d in declared)
            if declared_bad == 0:
                clean.append(app)
                continue

            print("  %s / %s: %d mismatching class(es) declared" % (app, apk, declared_bad))
            for d, (n, hit, bad) in sorted(sc.items(), key=lambda x: (x[1][2], -x[1][1])):
                mark = "<- declared" if d in declared else ""
                print("      classes=%-6d id_match=%-6d mismatch=%-4d %s %s"
                      % (n, hit, bad, os.path.relpath(d, app_dir), mark))

            picked, covered = pick_roots(sc, cp)
            declared_total = sum(sc.get(d, (0, 0, 0))[0] for d in declared
                                 if os.path.isdir(d))
            if not picked:
                need_rebuild.append((app, apk, declared_bad))
                print("      -> no directory on disk matches; needs a rebuild")
                continue
            # Trading most of the app's classes for a clean report is not a repair.
            # Aegis can reach zero mismatches by declaring only its 46 Hilt
            # component classes and dropping 498 from javac, which is worse than
            # the 3 mismatches it started with.
            if declared_total and covered < 0.8 * declared_total:
                need_rebuild.append((app, apk, declared_bad))
                print("      -> best clean set covers %d of %d classes; "
                      "needs a rebuild instead" % (covered, declared_total))
                continue
            if sorted(picked) == sorted(d for d in declared if os.path.isdir(d)):
                need_rebuild.append((app, apk, declared_bad))
                continue
            info["classfiles"] = [os.path.relpath(d, app_dir) + os.sep for d in picked]
            changed = True
            repaired.append((app, apk, declared_bad, len(picked)))
            print("      -> declaring %d director(ies): %s"
                  % (len(picked), ", ".join(os.path.relpath(d, app_dir) for d in picked)))

        if changed:
            json.dump(entries, open(cfg, "w"), indent=2)

    print()
    print("repaired by re-declaration: %d" % len(repaired))
    for app, apk, bad, n in repaired:
        print("   %-24s was %d mismatching -> %d dir(s)" % (app, bad, n))
    print("still need a rebuild: %d" % len(need_rebuild))
    for app, apk, bad in need_rebuild:
        print("   %-24s %d mismatching class(es)" % (app, bad))
    return 0


if __name__ == "__main__":
    sys.exit(main())
