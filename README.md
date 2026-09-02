# Android code-coverage datasets

**59 Android apps packaged so a GUI testing tool can measure line coverage on them,
each with a coverage run measured on a real Android device.**

Every app ships four things that agree with each other, which is the whole point:

```
instrumented_apps/geohashdroid/
├── geohashdroid-0.9.4-#73-rebuilt.apk   the APK, with the JaCoCo agent compiled in
├── geohashdroid-#73/                    its sources + the compiled classes from that build
├── class_files.json                     maps the APK to those two
└── DATASET.txt                          (inside the zip only) what this entry is
```

Get the classes from a different build than the APK and coverage silently reads
near-zero: JaCoCo drops every class whose bytecode does not match. Making those four
things agree, for apps spanning 2017 to 2025 and AGP 2.3 to AGP 9, is most of the
work here.

A fork of [DroidTest/TimeMachine](https://github.com/DroidTest/TimeMachine) with the
dataset work added. The tool itself is unchanged.

---

## 1. What is in here

| | |
|---|--:|
| apps | 59 |
| compiled classes | 46,106 |
| APKs | 59, 1.7 GB |
| pinned to a source commit | 59 (51 tags, 8 branches) |
| installs on API 25 | 42 — the other 17 need API 26-31 |

* `instrumented_apps/` — the datasets, one folder per app
* `instrumented_apps/MANIFEST.tsv` — package, minSdk, SHA-256, source commit, class and source counts
* `instrumented_apps/PROVENANCE.tsv` — repo, ref and commit SHA per entry
* `results/smoke_test/` — the coverage runs, one folder per app
* `dataset_builder/` — the pipeline that produced all of it

## 2. How the datasets were created

Per app, `dataset_builder/build_dataset.py`:

1. **Resolve the version.** `resolve_tags.py` asks the repo which tags exist
   (`git ls-remote`) and picks the newest stable one. Guessing tag names was the
   single biggest cause of failures.
2. **Clone that exact tag** and record the commit SHA.
3. **Inject the JaCoCo harness** if the branch has none — the coverage plugin, the
   debug coverage flag, three harness classes, and a broadcast receiver that dumps
   coverage on demand (`instrument.py`).
4. **Build a debug APK.** Old projects need old JDKs and dead repositories rewritten;
   `repair-repos.init.gradle` handles that.
5. **Find the compiled classes that went into that APK.** Read a sample `.class`
   file's own package from its bytecode to locate the output root, then narrow to the
   APK's build variant and drop duplicates. Hardcoded paths do not survive new
   tool versions.
6. **Prune and record.** Delete everything a report does not need, write
   `class_files.json`.

**The APK is always built here, from the source shipped beside it** — that is what
`-rebuilt` means. It is never an upstream release binary paired with a guessed
source tree. (One exception: MaterialFBook ships its published APK, its instrumented
branch having been lost with a deleted account.)

Not every app survives this. 105 were attempted, 59 produced a complete dataset that
reports cleanly. The rest are recorded with their failure reason in
`dataset_builder/status-*.csv`.

## 3. How they were tested

One pass per app, about 25 seconds each:

**install → launch → 40 monkey events → broadcast the coverage dump → pull
`coverage.ec` → build the report**

An app passes when a report generates with **zero mismatched classes**. That is the
check that matters: it proves the shipped classes really are the ones inside the APK.

## 4. Results

All 59 in `results/smoke_test/`:

| | |
|---|--:|
| installed and launched | 59 / 59 |
| reported with zero mismatched classes | 59 / 59 |
| line coverage | 0.51% (Fedilab) – 31.66% (AlarmClock), median 9.40% |

```
results/smoke_test/Twire-_v2.10.7-rebuilt_/
├── coverage.ec         execution data pulled off the device
├── coverage.xml        JaCoCo report
├── coverage_html/      annotated, colour-coded source
├── probe_report.txt    per-class probe detail
└── probe_summary.csv
results/smoke_test/smoke_summary.csv    the table for all 59
```

**Those percentages are not a benchmark.** They are a floor from 40 random taps —
proof the plumbing works. A large app touched briefly scores low, a small one scores
high, so do not compare apps on them. A real TimeMachine run explores for an hour and
goes far higher.

Every dataset maps to exactly one result and back: 59 datasets, 59 result folders, no
orphans. For each one, the class ids recorded in its `coverage.ec` resolve against its
own declared class directories with no mismatch.

Two apps, Kiwix and Open-Food-Facts, were **dropped** rather than shipped imperfect.
ObjectBox and Hilt rewrite some of their classes after compilation and write no copy
to disk, so the bytecode that ran exists only inside the APK and a few classes could
never be matched.

---

## 5. Running the tests

Two ways. Docker is easier and faster, and it is what produced the committed results.

First, get the code:

```bash
git lfs install                      # REQUIRED before cloning: APKs are LFS objects
git clone https://github.com/Dibae101/timemachine-test-code-coverage.git
cd timemachine-test-code-coverage

find instrumented_apps -name '*.apk' -size -1k    # must print nothing
```

If that prints filenames, LFS did not fetch: run `git lfs pull`.

### Option A — Docker, with redroid (recommended)

[redroid](https://github.com/remote-android/redroid-doc) runs Android as a container
against the host kernel. No KVM needed, works on arm64 and x86_64, boots in ~10
seconds.

```bash
# once: host kernel modules
sudo apt-get install -y linux-modules-extra-$(uname -r)
sudo modprobe binder_linux devices="binder,hwbinder,vndbinder"

# start Android 12 (API 31) — covers all 59 apps
docker run -itd --name redroid31 --privileged \
    -v ~/redroid-data:/data -p 5555:5555 \
    redroid/redroid:12.0.0-latest \
    androidboot.use_memfd=1 ro.secure=0

adb connect localhost:5555
adb -s localhost:5555 wait-for-device

# run it
PROJECT="$PWD" SDK=/path/to/android-sdk ADB=$(command -v adb) \
  SERIAL=localhost:5555 ./smoke_test_dataset.sh
```

Four details that will otherwise cost you an afternoon:

* `androidboot.use_memfd=1` — current kernels have no `ashmem`, redroid needs one.
* `ro.secure=0` — gives a root `adb shell`. Without it `coverage.ec` cannot be read
  out of app-private storage.
* Use the **non-`_64only`** image; several apps ship 32-bit native libraries.
* Use a **native** `adb`. On arm64 the Android SDK's `adb` is x86_64 and fails with
  `Could not open '/lib64/ld-linux-x86-64.so.2'`.

### Option B — the Android SDK emulator

Needs KVM on Linux, and there is no `linux_aarch64` emulator build, so this is
x86_64 only.

```bash
sdkmanager "system-images;android-31;google_apis;x86_64"
avdmanager create avd -n avd0 -k "system-images;android-31;google_apis;x86_64" -f

emulator -avd avd0 -no-window -no-audio -no-boot-anim &
adb wait-for-device && adb root

PROJECT="$PWD" SDK=/path/to/android-sdk ./smoke_test_dataset.sh
```

Use `google_apis`, **not** `google_apis_playstore`: `adb root` is unavailable on Play
images and the coverage pull needs it.

### Useful flags

```bash
./smoke_test_dataset.sh Twire Markor     # only these apps
DRY_RUN=1 ./smoke_test_dataset.sh        # check the dataset, no device needed
DRY_RUN=1 API=25 ./smoke_test_dataset.sh # which apps an API 25 image can install
EVENTS=200 ./smoke_test_dataset.sh       # exercise each app harder
RESUME=0 ./smoke_test_dataset.sh         # re-test apps that already have a verdict
```

`DRY_RUN` reports 59 discovered, 0 dataset problems, and flags apps whose `minSdk` is
above the image you name. Run it first.

### Which Android version

An **API 31** image runs all 59. On API 25, 17 apps refuse to install: 13 need API 26,
then Calculator-You (27), uhabits (28), Feeder (29), FastNFitness (31). `min_sdk` is a
column in `MANIFEST.tsv`.

Anything past API 25 also needs the harness fixes already in
`smoke_test_dataset.sh`: the coverage broadcast must name the receiver component
(implicit broadcasts to manifest receivers stopped being delivered at API 26 and fail
*silently*), and the launcher activity must be started explicitly, or the dump
contains only the harness classes.

### A full TimeMachine run

```bash
cd fuzzingandroid
python2.7 main.py --avd avd0 \
  --apk ../instrumented_apps/geohashdroid/geohashdroid-0.9.4-#73-rebuilt.apk \
  --time 1h -o ../results/timemachine --no-headless
```

`--time` is not a wall clock: the engine only checks its deadline between fuzz cycles,
and a cycle takes 18-28 minutes without KVM, so `--time 1m` and `--time 15m` both run
about 35 minutes. `run_coverage.sh` wraps it with a hard timeout.

---

## 6. Downloading a single dataset

```bash
./package_datasets.sh            # dist/<App>.zip for all 59 + all-datasets.zip
./package_datasets.sh Markor     # just one
```

Each zip is self-contained: APK, sources, compiled classes, `class_files.json`, and a
`DATASET.txt` describing the entry. `dist/SHA256SUMS` covers them all.

The zips are **not committed**: they total ~2.5 GB, individual ones reach 210 MB
(GitHub rejects files over 100 MB outside LFS), and the repository already holds every
byte — `git clone` *is* the complete bundle. To publish them, use release assets,
which allow 2 GB each:

```bash
gh release create v1.0 dist/*.zip --title "59 JaCoCo-ready Android datasets"
```

## 7. Rebuilding things

```bash
python3 dataset_builder/make_manifest.py       # validate every declared path
python3 dataset_builder/record_provenance.py   # re-pin refs to commit SHAs
./regenerate_reports.sh                        # rebuild XML/HTML from committed .ec
python3 dataset_builder/build_dataset.py --subjects expansion_subjects.json
```

`dataset_builder/README.md` documents every upstream breakage that had to be worked
around. Two worth knowing before you build anything:

* **Building Kiwix installs a pre-commit hook into this repository** and every later
  commit then fails with `./gradlew: not found`. Delete `.git/hooks/pre-commit`.
* **Always stage with `dataset_builder/publish.py`**, never `git add instrumented_apps`.
  It stages only apps that validate, and skips half-built trees.

## Licence

TimeMachine is upstream's, under its original licence. Each app under
`instrumented_apps/` keeps its own upstream licence; the only changes are the
instrumentation edits listed in [dataset.md](dataset.md#what-we-changed-in-each-app).

The app trees are verbatim upstream source, so they include files those projects
publish themselves — `google-services.json`, debug keystores, test fixtures. These are
byte-identical to their public repositories and none belong to this project. Secret
scanners may still flag them.
