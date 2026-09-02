# TimeMachine coverage datasets

A collection of **61 Android apps packaged so a GUI testing tool can measure code
coverage on them**, with a check per app that a report actually generates.

This is a fork of [DroidTest/TimeMachine](https://github.com/DroidTest/TimeMachine)
with the dataset work added. The original tool and paper are unchanged; see
[the upstream README](https://github.com/DroidTest/TimeMachine) for what
TimeMachine does.

* **[dataset.md](dataset.md)** - what a dataset contains, what was changed in each
  app, the app list, and a link to each app's smoke test
* **[dataset_builder/README.md](dataset_builder/README.md)** - how the datasets are
  built and every upstream breakage that had to be worked around

## Verification status, per app

Two different things have been checked, and they are not equivalent:

| level | apps | what it proves |
|---|--:|---|
| Device smoke test | 26 | the APK installs on an API 25 emulator, runs, dumps a real `.ec`, and reports with zero mismatched classes |
| Report validation | 61 | every declared class directory parses, sources resolve, and `jacococli` produces colour-coded HTML |

`results/smoke/` holds the device runs. `results/report-validation/` holds the
report checks, and its execution data is **synthesized, not measured** - see
[the note below](#report-validation-is-not-a-coverage-measurement).

Both trees use the **same layout**, so one consumer can read either:

```
results/<smoke|report-validation>/<apk-slug>/
├── coverage.ec        device execution data     (smoke only)
├── synthetic.exec     synthesized               (report-validation only)
├── coverage.xml       jacoco XML report
├── coverage_html/     annotated, colour-coded HTML
├── probe_report.txt   jacococli execinfo
└── probe_summary.csv  per-class probe hits
results/<...>/smoke_summary.csv | validation_summary.csv
```

Directory names are identical for the 26 apps present in both, and the summary
CSVs share their first twelve columns. The execution-data filename differs on
purpose, and `validation_summary.csv` carries a `coverage_source` column
(`device` vs `synthetic`) so the two cannot be conflated.

### Which emulator an app needs

`MANIFEST.tsv` records `min_sdk` per APK, because that decides where an app can be
smoke tested:

* **44 of 61** install on the API 25 image the original set was tested on.
* **17 need something newer** - 13 at API 26, then Calculator-You (27), uhabits
  (28), Feeder (29) and FastNFitness (31).

An API 31 image covers all 61. The original 26 were pinned to API 25 because newer
images change permission and storage behaviour and several of them stop launching,
so two passes are more reliable than one: API 25 for the 44, a newer image for the
rest.

Check before booting anything:

```bash
DRY_RUN=1 ./smoke_test_dataset.sh            # against API 25
DRY_RUN=1 API=31 ./smoke_test_dataset.sh     # against API 31
```

`DRY_RUN` needs no emulator. It lists every discovered APK with its `min_sdk`,
declared class count, resolved class/source directories, and flags LFS pointers or
unresolved paths. It reports 61 discovered, 0 dataset problems, 17 too new for API
25.

The 35 apps added most recently have not been run on a device, because the host
they were built on cannot run one (aarch64, no `/dev/kvm`). Their datasets are
structurally complete and reportable; whether each app installs and launches on
API 25 is still open. Several declare `minSdk` above 25 and will need a newer
emulator image.

## 13 of the original 26 were published without their classes

Worth knowing if you cloned this repository before this was fixed. Thirteen apps -
Binary-Eye, Breezy-Weather, Fedilab, Infinity-For-Reddit, Jellyfin-Android, Kiwix,
Kore, LibreTorrent, Open-Food-Facts, Orgzly-Revived, SkyTube, Wallabag and
ownCloud - shipped their sources, their APK and their `class_files.json`, but not
one compiled `.class` file. `MANIFEST.tsv` reported them as valid because it had
been generated on the machine that still had the build output on disk.

A coverage report needs the compiled classes, so those thirteen produced nothing.
All have been rebuilt from the exact refs in
`dataset_builder/research_subjects.json` and now carry their classes.
`dataset_builder/verify_reports.py` is what catches this class of problem, and it
is worth running after cloning.

## Why this exists

Measuring code coverage on an Android app needs four things together, and they
have to agree with each other:

1. an APK with the JaCoCo agent compiled in,
2. the **compiled classes** from that same build,
3. the **sources** for those classes,
4. a mapping from the APK to 2 and 3.

Get the classes from a different build than the APK and the report silently comes
out near-empty: JaCoCo excludes every class whose bytecode does not match. Most of
the effort here went into making those four things agree, for apps ranging from
2017 to 2025.

## The apps

61 apps, one APK each, all 61 entries validating in
[`instrumented_apps/MANIFEST.tsv`](instrumented_apps/MANIFEST.tsv) with per-entry
package, SHA-256, class and source counts. Per-app report figures are in
[`results/report-validation/report_validation.csv`](results/report-validation/report_validation.csv).

The set spans file managers, RSS and Reddit clients, media players, note takers,
calendars, launchers, trackers, calculators and games, built with AGP 2.3 through
AGP 9 and mixing Java, Kotlin and Compose. That spread is deliberate: the older
projects exercise the AGP 2.x/3.x class-output layouts, the newest exercise
Compose and AGP's built-in Kotlin compilation.

### Device-measured coverage, original 26 apps

| app | coverage | app | coverage | app | coverage |
|---|--:|---|--:|---|--:|
| Binary-Eye | 47.17% | LibreTorrent | 9.37% | Wikipedia | 3.41% |
| MaterialFBook | 19.24% | nextcloud | 8.63% | Infinity-For-Reddit | 3.16% |
| Kiwix | 19.19% | Open-Food-Facts | 8.43% | Twire | 2.67% |
| geohashdroid | 17.78% | Wallabag | 7.82% | openlauncher | 2.35% |
| SkyTube | 16.41% | newpipe | 7.57% | Kore | 1.72% |
| StreetComplete | 15.20% | commons | 7.57% | Jellyfin-Android | 1.25% |
| FirefoxLite | 13.72% | ownCloud | 6.56% | Fedilab | 0.52% |
| AmazeFileManager | 13.08% | Breezy-Weather | 6.22% | | |
| Omni-Notes | 10.58% | AnkiDroid | 4.11% | | |
| Orgzly-Revived | 10.50% | | | | |

**Those percentages are not results.** Read the next section before using them.

## The smoke test is not a coverage run

The smoke test exists to answer one question per app: *does this dataset work?*

It installs the APK, launches it, fires **40 monkey events over about 90 seconds**,
broadcasts the coverage dump, pulls the `.ec` file, and builds a report. An app
passes when the report generates with **zero mismatched classes**.

Two caveats on the numbers in the committed smoke output:

* **The `html_pages` column understated every Kotlin app.** It counted only
  `*.java.html`, so StreetComplete was recorded as 2 pages when its report
  actually contains 964 `*.kt.html`. Kiwix (322) and Breezy-Weather (424) were
  affected the same way. The reports were always correct; the column was not.
  Fixed, but the committed CSV still carries the old figures.
* **Three apps were measured over a fraction of their code.** Binary-Eye's
  headline 47.17% is 25 covered lines out of a 53-line denominator, because only
  4 of its classes were declared at the time. Kiwix analysed 1,339 lines against
  the 16,425 it has now, and Open-Food-Facts 9,861 against 24,198. Those three
  percentages are not app-level coverage, which is why the class directories were
  rebuilt.

That is a plumbing check. The percentages it produces are a floor - what you get
from a minute and a half of random tapping. A real TimeMachine run explores for an
hour and reaches far higher. Do not quote these as coverage results, and do not
compare apps against each other on them: a large app touched briefly scores low, a
small one scores high. Binary-Eye leads at 47% because the burst happened to reach
4 of its 370 classes and cover most lines inside them.

## Report validation is not a coverage measurement

`results/report-validation/` answers a narrower question than the smoke test, for
all 61 apps: *given this entry, does `jacococli` produce a report?*

It exists because the host these were built on is aarch64 with no `/dev/kvm`, so no
emulator can run and no real `.ec` can be collected there. Instead
`dataset_builder/tools/SynthExec.java` writes execution data over the declared
class files, keyed by the same class ids the reporter computes (CRC64 of the class
bytes) and sized by the same probe counts. That exercises the parts that actually
break in practice: id matching, probe-count agreement, source resolution, and the
green/yellow/red line markup.

**The percentages in those reports are meaningless.** They follow the `--fraction`
argument, not any test run, which is why they all sit near 60%. The verdict column
is the output that matters. The directory is kept separate from `results/smoke/`
precisely so synthesized and measured data cannot be mistaken for one another.

```bash
python3 dataset_builder/verify_reports.py            # all apps
python3 dataset_builder/verify_reports.py Markor     # by name
```

What it catches, all of which occurred while building this set:

* a declared directory holding JaCoCo-**instrumented** classes; `jacococli` aborts
  on those rather than warning, and writes no report at all
* the same class declared twice, from two product flavours or from a Hilt/ASM
  post-processing copy - also fatal, not a warning
* kapt stub trees, whose empty method bodies crash the analyzer
* an entry whose class directories were never published, which is exactly the
  13-app problem described above

## How the datasets were found and built

Apps came from three places:

* the **Themis** benchmark, which publishes instrumented branches per bug,
* an **LLM GUI testing study** whose app table records the exact source commit
  each tested APK was built from,
* **upstream release tags** for apps in neither.

For each app the builder clones the source at a known ref, injects the JaCoCo
harness if it is not already there, builds a debug APK, then derives the class and
source directories from that same build. `dataset_builder/` holds the pipeline:

```
build_dataset.py      clone, instrument, build, collect, prune
instrument.py         inject the harness (AGP 2-8, Groovy and Kotlin DSL)
fix_reports.py        pick class directories that report with zero mismatches
make_manifest.py      inventory and validate every entry
supervise.sh          repeat passes until enough subjects work
```

Old Android projects do not build unmodified in 2026 - jcenter shut down, plugin
APIs changed, JDK requirements moved. `dataset_builder/README.md` documents every
breakage encountered and the fix.

Refs are resolved from the remotes rather than guessed. `resolve_tags.py` runs
`git ls-remote --tags` per repository and sorts numerically, so 3.10 outranks 3.9;
guessing tag names was previously the single largest cause of failed subjects.
Three repositories still need a hand-pinned ref, recorded with the reason in
`make_expansion_subjects.py`: Etar still carries AOSP Calendar history, so its
highest-numbered tag is from 2012; KeePassDroid's newest tags are GitHub
`untagged-<sha>` placeholders; and CPU-Info's highest tag is its Wear OS build.

105 apps are defined across the subject sets. 61 produce a complete, reportable
dataset. The rest fail to build or to instrument, and are named with their state in
the `status-*.csv` files under `dataset_builder/`.

## What a dataset contains

```
instrumented_apps/geohashdroid/
├── geohashdroid-0.9.4-#73-rebuilt.apk    the APK, JaCoCo compiled in
├── geohashdroid-#73/                     compiled classes + sources
├── class_files.json                      maps the APK to the above
└── upstream/                             optional submodule -> a fork of the source
```

`upstream/` is provenance only. It is **not** needed to measure coverage, and only
13 apps have one. A plain `git clone` runs all of them.

## What was removed before publishing

The working tree during development is far larger than what is here:

| removed | why |
|---|---|
| Most of each Gradle build tree | only compiled classes and sources are needed; this took the dataset from 6.0 GB to 1.2 GB |
| Generated `R` / `R$*` classes | no meaningful coverage, and they collide across modules and flavours |
| 18 apps that never produced a clean report | not usable datasets |
| Duplicate APKs per app | each app appears exactly once, with the APK whose classes are declared |
| Nested `.git` directories | otherwise git records a submodule reference and uploads none of the contents |
| CI workflow files, in the forks only | pushing them needs a token scope this pipeline does not use |

Kept deliberately: the full smoke test output for the 26 device-tested apps,
including raw execution data and annotated HTML, so the coverage claims can be
checked rather than taken on trust. The synthesized `.exec` inputs under
`results/report-validation/` are **not** kept - they are regenerable in seconds and
keeping them invites confusion with the measured `.ec` files.

## Running it

Two prerequisites catch people out:

* **Install Git LFS before cloning.** The APKs are LFS objects; without it you get
  130-byte text pointers and every install fails.
* **Use an API 25 emulator**, `google_apis`, x86, for the 26 device-tested apps.
  Newer images change permission and storage behaviour and several of those apps
  will not start. The 35 newer apps were never device-tested and some declare
  `minSdk` above 25, so they need a newer image; check `minSdk` before assuming an
  install failure is a dataset problem.

Create the emulator these were tested on:

```bash
sdkmanager "system-images;android-25;google_apis;x86"
avdmanager create avd -n avd0 -k "system-images;android-25;google_apis;x86" \
    -d pixel_2_xl -c 1000M -f

# drop -accel off -gpu guest if the host has KVM
nohup emulator -avd avd0 -port 5554 -no-window -no-audio -no-boot-anim \
      -accel off -gpu guest > /tmp/emulator.log 2>&1 &
adb wait-for-device && adb root
```

Use a `google_apis` image, not `google_apis_playstore`: `adb root` is unavailable
on Play images and the coverage pull needs it. On API 29+ the scoped-storage and
permission-review changes stop several of these apps launching at all.

Then:

```bash
git lfs install
git clone https://github.com/Dibae101/timemachine-test-code-coverage.git
cd timemachine-test-code-coverage

# check the APKs are real files, not pointers
find instrumented_apps -name '*.apk' -size -1k     # must print nothing

# inventory and validate every declared path, without an emulator
python3 dataset_builder/make_manifest.py

# stronger: prove each entry actually produces a report
python3 dataset_builder/verify_reports.py

# smoke test (needs a working emulator)
./smoke_test_dataset.sh                  # every discoverable app
./smoke_test_dataset.sh Kiwix Twire      # by name
```

`smoke_test_dataset.sh` and `run_coverage.sh` default `PROJECT` to
`/home/ubuntu/TimeMachine`. Unless the clone is at that path, set it:
`PROJECT="$PWD" SDK=/path/to/android-sdk ./smoke_test_dataset.sh`.

A real coverage run, per app:

```bash
cd fuzzingandroid
python2.7 main.py --avd avd0 \
  --apk ../instrumented_apps/geohashdroid/geohashdroid-0.9.4-#73-rebuilt.apk \
  --time 1h -o ../results/timemachine --no-headless
```

`--time` is not a wall-clock limit; the engine only checks its deadline between
fuzz cycles, and one cycle takes 18-28 minutes on a host without KVM, so
`--time 1m` and `--time 15m` both run about 35 minutes. `run_coverage.sh` wraps it
with a hard `timeout` and generates the report afterwards.

## Where this was built

The original 26 were built and smoke tested on an AWS `t2.xlarge` in `us-east-1`:
4 vCPU, 15 GB RAM, 50 GB disk, **no KVM**, so the emulator ran in software. That
shapes the device numbers throughout: the emulator takes about 5 minutes to boot, a
smoke test is 60-150 seconds per app, and one TimeMachine fuzz cycle takes 18-28
minutes.

The expansion to 61 was built on a 16-core **aarch64** host with 61 GB RAM and no
`/dev/kvm`. Two things follow from that, and both are worth knowing before
reproducing this:

* **No emulator at all.** The Android emulator needs KVM on Linux, so nothing on
  that host could install an APK. This is why the newer apps have report validation
  rather than device smoke results.
* **The SDK's native tools are x86_64-only.** Google publishes `aapt2`, `aidl` and
  `zipalign` for `linux` (x86_64) alone. Debian ships an aarch64 `aapt2`, but it is
  2.19-debian and cannot read the resource tables of android-35 or android-36 - it
  errors on 35 and segfaults on 36 - so it is not a usable substitute for current
  apps. The working approach is `qemu-user-static` with an x86_64 glibc sysroot
  exported as `QEMU_LD_PREFIX`, which runs the official binaries unmodified and
  keeps AGP's own version-matched `aapt2`:

  ```bash
  sudo apt-get install -y qemu-user-static binfmt-support
  # extract amd64 libc6, libstdc++6, libgcc-s1, zlib1g into /opt/x86_64-sysroot
  export QEMU_LD_PREFIX=/opt/x86_64-sysroot
  ```

Builds are sharded across parallel workers, since one Android build mostly waits on
a single Gradle worker and a 60-subject serial pass leaves most of the host idle:

```bash
python3 dataset_builder/shard_subjects.py expansion_subjects.json 4
# run one build_dataset.py per shard, then fold the results back
python3 dataset_builder/consolidate_status.py expansion_subjects
```

Each shard gets its own status file on purpose. Two builders sharing one status
file each hold a snapshot and rewrite the whole thing per subject, so the second
silently reverts the first.

One trap: **building Kiwix installs a pre-commit hook into this repository.** Cloned
trees have their `.git` removed, so the task resolves to the parent repository, the
hook lands in `.git/hooks/pre-commit`, and every later commit fails with
`./gradlew: not found`. Delete it if commits suddenly start failing.

Repository: 61 APKs in LFS, 86,612 compiled class files, 384 MB of smoke test
output. The APK set alone is about 1.9 GB, which is over the 1 GB free GitHub LFS
allowance; check the account's quota before pushing, or keep APKs out of git and
publish them against the SHA-256 sums already recorded in `MANIFEST.tsv`.

## Licence and third-party content

TimeMachine is upstream's, under its original licence. Each app under
`instrumented_apps/` keeps its own upstream licence; the only changes are the four
instrumentation edits listed in [dataset.md](dataset.md#what-we-changed-in-each-app).

The app trees are verbatim upstream source, which means they include files those
projects publish themselves - `google-services.json`, debug keystores, test PEM
fixtures, a Maps API key in one manifest. These are byte-identical to their public
upstream repositories and none of them belong to this project. Automated secret
scanners may still flag them.
