# TimeMachine coverage datasets

A collection of **26 Android apps** packaged so a GUI testing tool can measure
Jacoco code coverage on them, plus the harness that proves each one works.

This is not a fork of TimeMachine with new features. It is a dataset collection:
finding apps that can report coverage, building them, and shipping the pieces a
coverage report needs. [TimeMachine](https://github.com/DroidTest/TimeMachine)
itself is included as the tool these datasets are built for.

- **[dataset.md](dataset.md)** - what a dataset contains, the list of 26, per-app
  sources, links to every smoke test result
- **[HANDOVER.md](HANDOVER.md)** - running this on another machine
- **[dataset_builder/README.md](dataset_builder/README.md)** - the build pipeline
  and every upstream breakage it works around

## What problem this solves

Measuring code coverage on an Android app needs three things that must agree with
each other:

1. an APK with the Jacoco agent compiled in,
2. the **compiled classes** from that exact build, to map coverage probes onto,
3. the **sources**, to render an annotated report.

Get any of them from a different build and the report silently comes out empty or
excludes classes as "does not match". Most published benchmarks ship only the APK,
which is why the original dataset here could not produce line coverage for 5 of
its 8 apps. Each dataset in this repository carries all three, verified together.

## The 26 apps

| app | coverage | | app | coverage | | app | coverage |
|---|--:|---|---|--:|---|---|--:|
| Binary-Eye | 47.17% | | LibreTorrent | 9.37% | | Wikipedia | 3.41% |
| MaterialFBook | 19.24% | | nextcloud | 8.63% | | Infinity-For-Reddit | 3.16% |
| Kiwix | 19.19% | | Open-Food-Facts | 8.43% | | Twire | 2.67% |
| geohashdroid | 17.78% | | Wallabag | 7.82% | | openlauncher | 2.35% |
| SkyTube | 16.41% | | newpipe | 7.57% | | Kore | 1.72% |
| StreetComplete | 15.20% | | commons | 7.57% | | Jellyfin-Android | 1.25% |
| FirefoxLite | 13.72% | | ownCloud | 6.56% | | Fedilab | 0.52% |
| AmazeFileManager | 13.08% | | Breezy-Weather | 6.22% | | | |
| Omni-Notes | 10.58% | | AnkiDroid | 4.11% | | | |
| Orgzly-Revived | 10.50% | | | | | | |

**These percentages are not test results.** See
[the smoke test](#the-smoke-test-is-not-a-coverage-run) below.

## Where the apps came from

Three sources, all open source:

| source | apps | how |
|---|--:|---|
| [Themis benchmark](https://github.com/the-themis-benchmarks/home) branches | 8 | already instrumented on a per-bug branch; built from that branch |
| upstream release tags and commits | 18 | cloned at an exact ref, then instrumented here |

The 8 Themis-derived apps are AmazeFileManager, AnkiDroid, FirefoxLite,
Omni-Notes, commons, geohashdroid, nextcloud and openlauncher. MaterialFBook is a
special case among the other 18: its instrumented branch was lost with a deleted
GitHub account, so its tree comes from the original dataset in this repository
while its fork points at the upstream tag.

44 apps were attempted in total. 26 produce a clean report. The rest either fail
to build, or install and run but emit no coverage data - all listed with reasons in
[dataset.md](dataset.md#not-included).

Candidate apps came from the Themis benchmark and from a set of 50 apps already
known to report coverage, which supplied the exact `source_commit` each tested APK
was built from. Exact refs mattered: guessing version tags was the single largest
cause of build failures.

## What a dataset contains

```
instrumented_apps/geohashdroid/
├── geohashdroid-0.9.4-#73-rebuilt.apk    the APK, Jacoco agent compiled in
├── geohashdroid-#73/                     compiled classes + sources
├── class_files.json                      maps the APK to those two
└── upstream/                             optional submodule, provenance only
```

Four changes are made to every app before building: the `jacoco` plugin, coverage
enabled on the `debug` build type, three harness classes under the app's package,
and a receiver for `edu.gatech.m3.emma.COLLECT_COVERAGE` so `adb` can make the
running app dump its data. Full detail in
[dataset.md](dataset.md#what-we-changed-in-each-app).

## What was removed before publishing

The build tree is pruned to what a coverage report actually needs. A full Gradle
build tree is 80 MB to 1.5 GB per app; pruning took the whole collection from
6.0 GB to 2.2 GB.

| removed | why |
|---|---|
| Gradle caches, merged resources, dex output, native libs | regenerable, and not read by a coverage report |
| each checkout's own `.git` | otherwise git records a submodule reference instead of the files, and the contents never upload |
| upstream `.gitignore` files | they list `build/`, which is exactly where the compiled classes live - they were silently excluding the payload |
| `google-services.json`, `*.keystore`, `*.jks` | third-party credentials that ship with upstream sources; verified to sit outside every declared classfiles/sourcefiles path, so nothing needs them |
| CI workflow definitions | only in the forks, not here; pushing them needs a token scope this pipeline does not use |

Kept: compiled classes, Java/Kotlin sources, the APK, `class_files.json`, and the
smoke test evidence.

## The smoke test is not a coverage run

`smoke_test_dataset.sh` answers one question per app: *does this dataset work?*
It installs the APK, launches it, fires a **40-event monkey burst of about 90
seconds**, broadcasts the coverage dump, pulls the `.ec` file and builds a report.

That is all. It is not a testing session, and the percentages it produces are a
floor that proves the plumbing works. A real TimeMachine run reaches far higher.

Do not compare apps against each other on these numbers either. A large app
touched briefly scores low; a small one scores high. Binary-Eye leads at 47%
because the burst happened to reach 4 of its 370 classes and cover most lines in
them.

A dataset counts as working only when the report generates with **zero mismatched
classes** - meaning the APK and the declared classes are provably the same build.

## Running it

Needs Git LFS **before** cloning, or the APKs arrive as 130-byte text pointers:

```bash
git lfs install
git clone https://github.com/Dibae101/timemachine-test-code-coverage.git
cd timemachine-test-code-coverage
find instrumented_apps -name '*.apk' -size -1k   # must print nothing
```

Emulator must be **API 25, `google_apis`, x86** - every app was verified on it,
and `adb root` (needed to pull coverage) is unavailable on Play Store images:

```bash
sdkmanager "system-images;android-25;google_apis;x86"
avdmanager create avd -n avd0 -k "system-images;android-25;google_apis;x86" \
    -d pixel_2_xl -c 1000M -f
nohup emulator -avd avd0 -port 5554 -no-window -no-audio -no-boot-anim \
      -accel off -gpu guest > /tmp/emulator.log 2>&1 &
adb wait-for-device && adb root
```

Then:

```bash
./smoke_test_dataset.sh                 # all 26, about 90 s each
./smoke_test_dataset.sh Kiwix Twire     # by name
python3 dataset_builder/make_manifest.py  # validate without an emulator
```

Results land in `results/smoke/<apk>/` as `coverage.ec`, `coverage.xml`,
`coverage_html/` and `smoke.log`, with the verdict table in
`results/smoke/smoke_summary.csv`.

A real run against one app:

```bash
cd fuzzingandroid
python2.7 main.py --avd avd0 \
  --apk ../instrumented_apps/geohashdroid/geohashdroid-0.9.4-#73-rebuilt.apk \
  --time 1h -o ../results/timemachine --no-headless
```

## Where this was built

A single AWS instance: **t2.xlarge, 4 vCPU, 15 GB RAM, 48 GB
disk, us-east-1**, Ubuntu, **no KVM**. The emulator therefore runs in software
emulation, which is why a smoke test takes 90 seconds per app rather than a few
seconds, and why the first emulator boot takes about 5 minutes.

Disk was the binding constraint throughout: 48 GB total, with Gradle caches
reaching 24 GB during builds and needing repeated trimming. Anyone reproducing the
build side should expect to want more.

JDK 8, 11, 17 and 21 are all needed - different apps refuse different versions -
along with Android SDK platforms 25 through 34 and JaCoCo 0.8.13 (0.8.6, which
ships with TimeMachine, cannot parse Java 17 bytecode).

## Licence

Each app keeps its own upstream licence; see its directory. The tooling in
`dataset_builder/` follows this repository's [LICENSE](LICENSE), and TimeMachine
remains under its original terms.
