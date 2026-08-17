# Handover: running the coverage datasets on another machine

Everything here was verified on the machine that produced the datasets. Figures
quoted are measured, not estimated.

## Short answer

Yes, it moves to another computer. Two things will bite you if you skip them:

1. **Install Git LFS before cloning.** The APKs are LFS objects. Without LFS you
   get 130-byte text pointers instead of APKs, and every install fails.
2. **Use an Android API 25 emulator.** That is what all ten entries were tested
   on. Newer images are not a drop-in substitute; see
   [Emulator](#emulator-what-to-use-and-why).

## What you are getting

8 apps, 10 APK entries. Every entry installs, runs, dumps JaCoCo execution data,
and produces an annotated HTML coverage report. These are measured results from
the run in `results/smoke/`:

| app | APK | line coverage | HTML pages | seconds |
|---|---|--:|--:|--:|
| MaterialFBook | `MaterialFBook4.0.2-debug-#224.apk` | 19.24% | 28 | 98 |
| geohashdroid | `geohashdroid-0.9.4-#73-rebuilt.apk` | 17.78% | 53 | 84 |
| geohashdroid | `geohashdroid-0.9.4-#73.apk` | 17.78% | 53 | 79 |
| FirefoxLite | `FirefoxLite-2.1.20-#5085-rebuilt.apk` | 13.72% | 400 | 97 |
| AmazeFileManager | `AmazeFileManager-3.4.2-#1837-rebuilt.apk` | 13.08% | 253 | 91 |
| Omni-Notes | `Omni-Notes-6.1.0-#745-rebuilt.apk` | 10.58% | 152 | 69 |
| nextcloud | `nextcloud-#4792-rebuilt.apk` | 8.63% | 328 | 82 |
| Twire | `Twire-#v2.10.7-rebuilt.apk` | 2.67% | 117 | 74 |
| openlauncher | `openlauncher-0.3.1-#67-rebuilt.apk` | 2.35% | 50 | 60 |
| openlauncher | `openlauncher-0.3.1-#67.apk` | 2.35% | 50 | 56 |

Those coverage numbers come from a 40-event monkey burst, not a real testing
session. They are a smoke-test floor: proof the plumbing works end to end. A real
TimeMachine run reaches considerably higher.

## How a dataset is laid out

```
instrumented_apps/<App>/
    class_files.json                 what jacococli needs, paths relative to here
    <App>-#<id>/                     project tree: compiled classes + sources
    <name>.apk                       the APK those classes belong to
    <name>-rebuilt.apk               locally built APK (safest to test with)
    upstream/                        submodule -> github.com/bugfixops/<repo>
```

`class_files.json` maps each APK to the class and source directories for it:

```json
{
  "geohashdroid-0.9.4-#73-rebuilt.apk": {
    "classfiles":  ["geohashdroid-#73/app/build/intermediates/javac/debug/classes/"],
    "sourcefiles": ["geohashdroid-#73/app/src/main/java/"]
  }
}
```

The compiled classes are committed directly in this repository, so a plain clone
is enough to run coverage. `upstream/` is a submodule holding the instrumented
source in the `bugfixops` org; you only need it to rebuild an app from scratch.

`instrumented_apps/MANIFEST.tsv` lists every entry with its package name,
SHA-256, class count and provenance.

## Prerequisites

| what | version | why this one |
|---|---|---|
| Git LFS | any | APKs are LFS objects |
| Android SDK platform-tools | any recent | `adb` |
| Android SDK build-tools | 28.0.3 | `aapt` for reading package names |
| Android emulator + system image | **android-25, google_apis, x86** | what the entries were tested on |
| JDK | 8, 11 or 17 | any of them runs `jacococli.jar` 0.8.6 |
| Python | 2.7 with `enum` and `uiautomator` | TimeMachine's own runner |
| Python | 3 | the dataset tooling in `dataset_builder/` |

The bundled `jacococli.jar` is JaCoCo 0.8.6 (`0.8.6.201911080117`) and was checked
under JDK 8, 11 and 17 - it runs on all three. Its limitation is different: it
cannot *analyse* class files compiled to Java 17 bytecode, which is what removed
the AGP 8 subjects from this set. None of the ten entries here is affected.

## Clone

```bash
git lfs install                       # once per machine, BEFORE cloning
git clone https://github.com/Dibae101/timemachine-test-code-coverage.git
cd timemachine-test-code-coverage
```

Verify the APKs are real files rather than pointers:

```bash
find instrumented_apps -name '*.apk' -size -1k        # must print nothing
```

If it prints filenames, LFS was not active. Fix with:

```bash
git lfs install && git lfs pull
```

Optional, only if you intend to rebuild from source:

```bash
git submodule update --init --recursive
```

## Emulator: what to use, and why

The datasets were tested on exactly this:

```
system image  system-images/android-25/google_apis/x86
device        pixel_2_xl
abi           x86
API at run    25
```

Create it:

```bash
sdkmanager "system-images;android-25;google_apis;x86"
avdmanager create avd -n avd0 -k "system-images;android-25;google_apis;x86" \
    -d pixel_2_xl -c 1000M -f
```

Launch it. Drop `-accel off -gpu guest` if the host has KVM, which makes boot and
execution several times faster:

```bash
nohup emulator -avd avd0 -port 5554 -no-window -no-audio -no-boot-anim \
      -accel off -gpu guest > /tmp/emulator.log 2>&1 &
adb wait-for-device
adb root
```

**Why API 25 and not something newer.** Every APK here has `minSdk` between 14
and 21, so API 25 installs all of them, and API 25 is what TimeMachine itself
targets (its snapshot and restore logic is exercised against that image). Two
things go wrong on newer images:

* API 29+ enforces scoped storage and stricter runtime permission flows. An app
  written against `targetSdk` 25-28 hits permission-review screens that block
  first launch. This is not hypothetical - in the published 60-minute runs, A
  Photo Manager and ODK Collect were dropped from the results because a
  permission-review screen stopped them starting at all.
* `adb root` is unavailable on Google Play images. Use a `google_apis` image, not
  `google_apis_playstore`.

If you must use a newer API, treat coverage numbers as not comparable with the
table above, and re-run the smoke test first to see what still launches.

x86 images need an x86_64 host. On Apple Silicon or another arm64 host, use
`system-images;android-25;google_apis;arm64-v8a` if available for your SDK, and
expect to re-verify: nothing here was tested on arm64.

## Smoke test: confirm the datasets work on your machine

Do this before any real testing. It boots one emulator and, per APK, installs,
launches, fires 40 monkey events, broadcasts the coverage dump, pulls the `.ec`
and builds a report. Roughly 60-100 seconds per entry, plus a one-off emulator
boot of about 5 minutes without KVM.

```bash
export SDK=$HOME/Android/Sdk          # wherever your SDK lives
./smoke_test_dataset.sh               # all 10 entries
./smoke_test_dataset.sh geohash amaze # substring filter
EVENTS=200 ./smoke_test_dataset.sh    # exercise the apps harder
```

Artifacts land in `results/smoke/<apk>/`:

```
coverage.ec        raw execution data
coverage.xml       JaCoCo report
coverage_html/     annotated source, open index.html
smoke.log          the adb transcript for that entry
probe_report.txt   per-class probe counts, useful when no report is produced
```

and a verdict table in `results/smoke/smoke_summary.csv`. Expect `PASS` on all
ten. Anything else means your environment differs from the reference; read that
entry's `smoke.log` first.

## Real TimeMachine run

```bash
cd fuzzingandroid
python2.7 main.py --avd avd0 \
  --apk ../instrumented_apps/geohashdroid/geohashdroid-0.9.4-#73-rebuilt.apk \
  --time 1h -o ../results/timemachine --no-headless
```

Be aware that `--time` is not a wall-clock limit. The engine only checks its
deadline between fuzz cycles, and one cycle takes 18-28 minutes on a host without
KVM, so `--time 1m` and `--time 15m` both run about 35 minutes. `run_coverage.sh`
wraps this with a hard `timeout` and generates the report afterwards:

```bash
CAP_MINUTES=20 ./run_coverage.sh geohashdroid
```

## Generating a coverage report by hand

```bash
APP=instrumented_apps/geohashdroid
java -jar fuzzingandroid/libs/jacococli.jar report coverage.ec \
  --classfiles  $APP/geohashdroid-#73/app/build/intermediates/javac/debug/classes/ \
  --sourcefiles $APP/geohashdroid-#73/app/src/main/java/ \
  --html coverage_html --xml coverage.xml
```

Read the paths out of that app's `class_files.json` rather than typing them; they
differ per app and per AGP generation. That exact command was run against the
recorded `coverage.ec` while writing this document and reproduced 17.78% over
938/5275 lines with 53 HTML pages, matching the table above.

**Always pair an APK with its own classes.** Where an app ships two APKs, prefer
the `-rebuilt.apk`: its classes are byte-for-byte the ones declared. Mixing an
APK with classes from a different build makes JaCoCo report `does not match` and
silently exclude those classes, which is how a report comes out at 0%. Only
geohashdroid and openlauncher have published and rebuilt APKs that agree class
for class, which is why both of theirs are kept.

## Checking the datasets without an emulator

```bash
python3 dataset_builder/make_manifest.py
```

Confirms every APK is present and readable by `aapt`, every declared path
resolves, and every class directory is non-empty. Exit status is non-zero if
anything is broken, so it works as a pre-run gate. Expect:

```
apps: 8    apk entries: 10    fully valid entries: 10
```

## Rebuilding an app from source

Only needed if you want to change the instrumentation. Each `upstream/` submodule
is a fork on branch `timemachine-instrumented` carrying the Jacoco plugin,
coverage enabled for `debug`, the harness classes, and a receiver for
`edu.gatech.m3.emma.COLLECT_COVERAGE`.

These projects date from 2017-2023 and none builds unmodified today, mainly
because jcenter was shut down. Each fork ships the fix:

```bash
cd instrumented_apps/geohashdroid/upstream
./gradlew --no-daemon -I timemachine/repair-repos.init.gradle assembleDebug
```

`timemachine/BUILD.md` in each fork covers which JDK matches which Gradle version
and what to do when a single product flavour fails. `dataset_builder/README.md`
documents every upstream breakage encountered and how it is handled.

## Known limits

* **10 entries, not 40.** 44 apps were attempted. 18 produced complete datasets;
  10 of those failed at report generation and were removed, leaving these 10
  verified entries. `dataset_builder/subjects.json` and `modern_subjects.json`
  still describe all 44, and `dataset_builder/supervise.sh` can retry the rest.
* **Two known reporting bugs blocked the 10 that were dropped**, and neither
  needs a rebuild to fix: the bundled `jacococli.jar` (0.8.6) cannot parse Java
  17 bytecode, reporting `Unsupported class file major version 61`, which stops
  the AGP 8 subjects such as Wikipedia; and some entries declared two build
  variants at once, so JaCoCo rejected duplicate class names (AnkiDroid,
  commons). Upgrading the CLI jar and narrowing class directories to one variant
  should recover most of them.
* **Coverage figures here are smoke-test floors** from 40 monkey events, not
  results from a real testing session.
* **Nothing was tested on arm64 or on API levels other than 25.**
