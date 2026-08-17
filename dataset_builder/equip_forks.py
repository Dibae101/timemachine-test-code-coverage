#!/usr/bin/env python3
"""
equip_forks.py - give each fork what it needs to actually build in 2026.

Instrumenting a checkout is not enough to make it testable. These projects date
from 2017-2023 and none of them build unmodified today, mostly because jcenter
was shut down. Each fork therefore also gets:

    timemachine/repair-repos.init.gradle   repository + quality-gate fixes
    timemachine/BUILD.md                   how to build and collect coverage

Files are written through the GitHub contents API rather than by cloning, since
several of these repositories are large and only two small files are changing.

    python3 equip_forks.py --dry-run
    python3 equip_forks.py
"""

import argparse
import base64
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from fork_and_link import api, load_state, token, BRANCH  # noqa: E402

INIT_SRC = os.path.join(HERE, "repair-repos.init.gradle")

BUILD_MD = """# Building this app for coverage collection

This fork carries the modifications TimeMachine needs to measure code coverage:

* the `jacoco` plugin on the application module
* coverage enabled for the `debug` build type, so classes are instrumented
* `JacocoInstrument/` harness classes
* a receiver for `edu.gatech.m3.emma.COLLECT_COVERAGE`, which makes a running
  app dump execution data to `files/coverage.ec` on demand

CI workflow definitions were removed; they are not used for local builds.

## Build

These projects predate the jcenter shutdown, so they do not resolve
dependencies unmodified any more. Build with the supplied init script:

```bash
./gradlew --no-daemon -I timemachine/repair-repos.init.gradle assembleDebug
```

The init script replaces dead bintray repositories, adds a jcenter stand-in for
artifacts never republished to Maven Central, and disables code-quality gates
(checkstyle, ktlint, detekt, spotbugs, lint), which reject the harness files.

Pick the JDK that matches the project's Gradle version: Gradle 4.x-5.x needs
JDK 8, Gradle 6.x-7.x is happiest on JDK 11 or 17, Gradle 8.x needs JDK 17.

```bash
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 \\
  ./gradlew --no-daemon -I timemachine/repair-repos.init.gradle assembleDebug
```

If `assembleDebug` fails on one product flavour, build a single flavour instead
(`assembleFdroidDebug`, `assembleVanillaDebug`, ...). One bad flavour otherwise
fails the whole build - AmazeFileManager 3.2.1 overflows the 64K dex limit on
its `play` flavour while the others are fine.

## Collect coverage

```bash
adb install -g app/build/outputs/apk/**/debug/*.apk
adb shell monkey -p <package> 1                 # or drive it with a test tool
adb shell am broadcast -a edu.gatech.m3.emma.COLLECT_COVERAGE
adb shell "cat /data/data/<package>/files/coverage.ec" > coverage.ec
```

Turn the execution data into a report with the compiled classes and sources:

```bash
java -jar jacococli.jar report coverage.ec \\
  --classfiles app/build/intermediates/javac/debug/**/classes \\
  --sourcefiles app/src/main/java \\
  --html coverage_html
```

`--classfiles` must point at the classes from *this* build. Classes from a
different build report as "does not match" and are excluded, which is what makes
a coverage report come out empty.
"""


def put_file(repo, path, content, message, tok, branch=BRANCH, dry=False):
    """Create or update a file on a branch. Returns (ok, note)."""
    status, existing = api("/repos/%s/contents/%s?ref=%s" % (repo, path, branch), tok)
    sha = existing.get("sha") if status == 200 else None
    if status == 200:
        current = base64.b64decode(existing.get("content", "")).decode(
            "utf-8", "replace")
        if current == content:
            return True, "unchanged"
    if dry:
        return True, "would %s" % ("update" if sha else "create")

    body = {
        "message": message,
        "content": base64.b64encode(content.encode()).decode(),
        "branch": branch,
    }
    if sha:
        body["sha"] = sha
    status, resp = api("/repos/%s/contents/%s" % (repo, path), tok,
                       method="PUT", body=body)
    if status in (200, 201):
        return True, "updated" if sha else "created"
    return False, "%s %s" % (status, resp.get("message"))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    init_content = open(INIT_SRC, encoding="utf-8").read()
    tok = token()
    state = load_state()
    forks = {app: v["fork"] for app, v in state.items()
             if v.get("linked") and v.get("fork")}

    print("equipping %d forks on branch %s%s"
          % (len(forks), BRANCH, "  (dry run)" if args.dry_run else ""))
    ok_count = 0
    for app, repo in sorted(forks.items()):
        r1, n1 = put_file(repo, "timemachine/repair-repos.init.gradle",
                          init_content,
                          "Add repository fixes needed to build in 2026", tok,
                          dry=args.dry_run)
        r2, n2 = put_file(repo, "timemachine/BUILD.md", BUILD_MD,
                          "Document building and collecting coverage", tok,
                          dry=args.dry_run)
        good = r1 and r2
        ok_count += 1 if good else 0
        print("  %-28s %-38s init=%s build_md=%s" % (app, repo, n1, n2))

    print()
    print("%d/%d forks equipped" % (ok_count, len(forks)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
