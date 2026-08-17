#!/usr/bin/env python3
"""
publish.py - stage exactly the app directories that are complete datasets.

Never run `git add instrumented_apps` directly. Two things go wrong:

1. A subject that has been cloned but not yet built still contains its own
   `.git`, and git then records a **gitlink** rather than the files. On GitHub
   the directory renders as a submodule you cannot open, and none of its
   contents are uploaded.
2. Failed and in-progress subjects carry full unpruned build trees, which are
   large and are not usable datasets.

This script stages only apps whose entries validate in MANIFEST.tsv, strips any
stray `.git` inside them first, and reports what it skipped and why.

    python3 publish.py                 # stage, then show what to commit
    python3 publish.py --commit -m MSG # stage and commit
"""

import argparse
import csv
import os
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(HERE)
APPS_REL = "instrumented_apps"
APPS = os.path.join(PROJECT, APPS_REL)
MANIFEST = os.path.join(APPS, "MANIFEST.tsv")


def git(*args, check=True):
    r = subprocess.run(["git"] + list(args), cwd=PROJECT, capture_output=True,
                       text=True, timeout=3600)
    if check and r.returncode != 0:
        raise RuntimeError("git %s failed: %s" % (" ".join(args), r.stderr[-400:]))
    return r


def strip_nested_git(path):
    """Remove any .git inside a dataset directory, so git stores files."""
    removed = 0
    for root, dirs, files in os.walk(path):
        if ".git" in dirs:
            shutil.rmtree(os.path.join(root, ".git"), ignore_errors=True)
            dirs.remove(".git")
            removed += 1
        if ".git" in files:
            try:
                os.remove(os.path.join(root, ".git"))
                removed += 1
            except OSError:
                pass
    return removed


def strip_nested_gitignore(path):
    """Remove .gitignore files that came with an upstream checkout.

    Every Android project ignores `build/`, which is exactly where the compiled
    classes live. Left in place, git silently excludes the entire point of the
    dataset: Wikipedia published 11,279 class files as zero. These files have no
    purpose inside a dataset directory.
    """
    removed = 0
    for root, dirs, files in os.walk(path):
        if ".git" in dirs:
            dirs.remove(".git")
        if ".gitignore" in files:
            try:
                os.remove(os.path.join(root, ".gitignore"))
                removed += 1
            except OSError:
                pass
    return removed


def valid_apps():
    if not os.path.isfile(MANIFEST):
        raise SystemExit("no MANIFEST.tsv - run make_manifest.py first")
    rows = list(csv.DictReader(open(MANIFEST), delimiter="\t"))
    good, bad = set(), set()
    for r in rows:
        (good if r["paths_ok"] == "yes" else bad).add(r["app"])
    return sorted(good - bad), sorted(bad), rows


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--commit", action="store_true")
    ap.add_argument("-m", "--message", default="Add validated coverage datasets")
    args = ap.parse_args()

    good, bad, rows = valid_apps()
    on_disk = sorted(d for d in os.listdir(APPS)
                     if os.path.isdir(os.path.join(APPS, d)))
    incomplete = [a for a in on_disk if a not in good]

    # any gitlink already recorded for an incomplete subject has to come out of
    # the index, otherwise it stays on the remote as a phantom submodule
    tracked = git("ls-tree", "-r", "HEAD").stdout.splitlines()
    gitlinks = [ln.split("\t", 1)[1] for ln in tracked
                if ln.split(" ", 1)[0] == "160000"]
    removed_links = 0
    for path in gitlinks:
        git("rm", "--cached", "-q", "--", path, check=False)
        removed_links += 1

    stripped = ignores = 0
    paths = []
    for app in good:
        d = os.path.join(APPS, app)
        stripped += strip_nested_git(d)
        ignores += strip_nested_gitignore(d)
        paths.append(os.path.join(APPS_REL, app))

    if os.path.isfile(MANIFEST):
        paths.append(os.path.join(APPS_REL, "MANIFEST.tsv"))

    # -f because build output is ignored by convention everywhere; here it is
    # the payload, not a build artefact to skip.
    git("add", "-f", "--", *paths)

    print("staged %d complete datasets:" % len(good))
    for app in good:
        # an app usually has two entries (published APK and rebuilt APK) that
        # share one set of class directories, so report the per-entry figure
        # rather than the sum
        counts = [int(r["class_files"]) for r in rows if r["app"] == app]
        print("   %-30s %d class files" % (app, max(counts) if counts else 0))
    if removed_links:
        print()
        print("un-staged %d gitlink(s) left by a previous `git add`" % removed_links)
    if stripped:
        print("stripped %d nested .git director%s"
              % (stripped, "y" if stripped == 1 else "ies"))
    if ignores:
        print("removed %d upstream .gitignore file(s) that excluded build output"
              % ignores)

    staged_classes = sum(
        1 for ln in git("diff", "--cached", "--name-only").stdout.splitlines()
        if ln.endswith(".class"))
    print()
    print("staged .class files: %d" % staged_classes)
    if incomplete:
        print()
        print("not staged - build not complete (%d):" % len(incomplete))
        for app in incomplete:
            print("   " + app)

    if args.commit:
        r = git("commit", "-q", "-m", args.message, check=False)
        if r.returncode == 0:
            print()
            print("committed. push with:  git push origin main")
        else:
            print()
            print("nothing to commit" if "nothing to commit" in (r.stdout + r.stderr)
                  else r.stderr[-300:])
    else:
        print()
        print("review with:  git status --short | head")
        print("then:         git commit -m '...' && git push origin main")
    return 0


if __name__ == "__main__":
    sys.exit(main())
