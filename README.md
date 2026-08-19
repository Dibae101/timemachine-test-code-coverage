# TimeMachine coverage datasets

A collection of **26 Android apps packaged so a GUI testing tool can measure code
coverage on them**, plus a smoke test that proves each one works.

This is a fork of [DroidTest/TimeMachine](https://github.com/DroidTest/TimeMachine)
with the dataset work added. The original tool and paper are unchanged; see
[the upstream README](https://github.com/DroidTest/TimeMachine) for what
TimeMachine does.

* **[dataset.md](dataset.md)** - what a dataset contains, what was changed in each
  app, the list of 26, and a link to each app's smoke test
* **[HANDOVER.md](HANDOVER.md)** - how to run this on another machine
* **[dataset_builder/README.md](dataset_builder/README.md)** - how the datasets are
  built and every upstream breakage that had to be worked around

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

## The 26 apps

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

That is a plumbing check. The percentages it produces are a floor - what you get
from a minute and a half of random tapping. A real TimeMachine run explores for an
hour and reaches far higher. Do not quote these as coverage results, and do not
compare apps against each other on them: a large app touched briefly scores low, a
small one scores high. Binary-Eye leads at 47% because the burst happened to reach
4 of its 370 classes and cover most lines inside them.

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

44 apps were attempted. 26 work. Six build but do not report usable coverage, and
the rest fail to build; all are named with their reason in
[dataset.md](dataset.md#not-included).

## What a dataset contains

```
instrumented_apps/geohashdroid/
├── geohashdroid-0.9.4-#73-rebuilt.apk    the APK, JaCoCo compiled in
├── geohashdroid-#73/                     compiled classes + sources
├── class_files.json                      maps the APK to the above
└── upstream/                             optional submodule -> a fork of the source
```

`upstream/` is provenance only. It is **not** needed to measure coverage, and only
13 of the 26 apps have one. A plain `git clone` runs all 26.

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

Kept deliberately: the full smoke test output for all 26 apps, including raw
execution data and annotated HTML, so the coverage claims can be checked rather
than taken on trust.

## Running it

Two prerequisites catch people out:

* **Install Git LFS before cloning.** The APKs are LFS objects; without it you get
  130-byte text pointers and every install fails.
* **Use an API 25 emulator**, `google_apis`, x86. That is what all 26 were tested
  on. Newer images change permission and storage behaviour and several apps will
  not start.

```bash
git lfs install
git clone https://github.com/Dibae101/timemachine-test-code-coverage.git
cd timemachine-test-code-coverage

# check the APKs are real files, not pointers
find instrumented_apps -name '*.apk' -size -1k     # must print nothing

# validate every dataset without an emulator
python3 dataset_builder/make_manifest.py

# smoke test
./smoke_test_dataset.sh                  # all 26
./smoke_test_dataset.sh Kiwix Twire      # by name
```

A real coverage run, per app:

```bash
cd fuzzingandroid
python2.7 main.py --avd avd0 \
  --apk ../instrumented_apps/geohashdroid/geohashdroid-0.9.4-#73-rebuilt.apk \
  --time 1h -o ../results/timemachine --no-headless
```

`--time` is not a wall-clock limit; the engine only checks its deadline between
fuzz cycles. `run_coverage.sh` wraps it with a hard timeout. Full instructions in
[HANDOVER.md](HANDOVER.md).

## Where this was built

An AWS `t2.xlarge` in `us-east-1`: 4 vCPU, 15 GB RAM, 50 GB disk, **no KVM**, so
the emulator runs in software. That shapes the numbers throughout: the emulator
takes about 5 minutes to boot, a smoke test is 60-150 seconds per app, and one
TimeMachine fuzz cycle takes 18-28 minutes. Disk was the binding constraint - the
Gradle cache grows past 20 GB during a build pass and had to be trimmed between
passes.

Repository: 99,265 tracked files, 50 APKs in LFS, 2.2 GB of datasets, 384 MB of
smoke test output.

## Licence and third-party content

TimeMachine is upstream's, under its original licence. Each app under
`instrumented_apps/` keeps its own upstream licence; the only changes are the four
instrumentation edits listed in [dataset.md](dataset.md#what-we-changed-in-each-app).

The app trees are verbatim upstream source, which means they include files those
projects publish themselves - `google-services.json`, debug keystores, test PEM
fixtures, a Maps API key in one manifest. These are byte-identical to their public
upstream repositories and none of them belong to this project. Automated secret
scanners may still flag them.
