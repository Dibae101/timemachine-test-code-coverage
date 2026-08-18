# Dataset

26 Android apps, each packaged so TimeMachine can measure code coverage on it.

## Answering the confusion first: what an APK needs

A complete dataset is **four things in one folder**. All four are committed
directly in this repository:

```
instrumented_apps/geohashdroid/
├── geohashdroid-0.9.4-#73-rebuilt.apk     1. the APK, instrumented
├── geohashdroid-#73/                      2. compiled classes  3. sources
├── class_files.json                       4. the map from APK to 2 and 3
└── upstream/                              OPTIONAL - not needed to run
```

`class_files.json` is the piece that ties it together:

```json
{
  "geohashdroid-0.9.4-#73-rebuilt.apk": {
    "classfiles":  ["geohashdroid-#73/app/build/intermediates/javac/debug/classes/"],
    "sourcefiles": ["geohashdroid-#73/app/src/main/java/"]
  }
}
```

`jacococli` needs all three: the `.ec` from the running APK, the **compiled
classes** to map coverage probes onto, and the **sources** to render annotated
HTML. Miss the classes and you get an empty report.

### `upstream/` is optional, and that is the whole answer

13 of the 26 apps have an `upstream/` submodule; 13 do not. **It makes no
difference to whether the dataset works.** Every app has its own copy of the
compiled classes and sources committed directly, so:

```bash
git clone <repo>                      # all 26 datasets work immediately
git clone --recurse-submodules <repo> # also fetches 13 instrumented sources
```

The submodule points at a fork in [github.com/bugfixops](https://github.com/bugfixops)
holding the instrumented source. It is there for provenance and for rebuilding an
app from scratch. It is **not** an input to coverage measurement.

Why only 13 have it: those went through `dataset_builder/fork_and_link.py` in an
earlier round. The 13 newer apps were built from upstream tags directly and never
forked. This is an inconsistency in bookkeeping, not in the datasets.

Why the copied tree cannot be replaced by the submodule: a source fork contains
no build output. A fresh clone would give sources with no compiled classes, and
`jacococli` would have nothing to map probes onto - the exact empty-report failure
this dataset exists to avoid.

## What we changed in each app

Every app is modified in exactly four ways before building. `dataset_builder/instrument.py`
applies them; the Themis-derived apps already carried them.

| # | change | why |
|---|---|---|
| 1 | apply the `jacoco` plugin to the app module | enables coverage instrumentation |
| 2 | set `testCoverageEnabled` / `enableAndroidTestCoverage` on the `debug` build type | makes AGP instrument the classes that go into the APK |
| 3 | add `JacocoInstrument/` (3 Java files) under the app's package | reads execution data out of the running agent by reflection |
| 4 | register a receiver for `edu.gatech.m3.emma.COLLECT_COVERAGE` in the manifest | lets `adb shell am broadcast` make the app dump `coverage.ec` |

Two build-time changes are applied but **not** committed into the app source:

* `dataset_builder/repair-repos.init.gradle` replaces dead repositories (jcenter
  shut down), disables code-quality gates that reject the harness files, and turns
  off R8 for debug builds, since shrinking rewrites classes and breaks the
  APK-to-classes mapping.
* CI workflow files are deleted from the forks only, because pushing them needs a
  token scope this pipeline does not use.

## The 26 datasets

`coverage` is a floor from a 90-second, 40-event monkey burst - proof the dataset
works, not a test result. Do not compare apps on it: a large app touched briefly
scores low, a small one scores high.

| app | coverage | APK | smoke test | fork |
|---|--:|---|---|:--:|
| Binary-Eye | 47.17% | `Binary-Eye-#1.63.12-rebuilt.apk` | [results](results/smoke/Binary-Eye-_1.63.12-rebuilt_) | |
| MaterialFBook | 19.24% | `MaterialFBook4.0.2-debug-#224.apk` | [results](results/smoke/MaterialFBook4.0.2-debug-_224_) | yes |
| Kiwix | 19.19% | `Kiwix-#3.11.1-rebuilt.apk` | [results](results/smoke/Kiwix-_3.11.1-rebuilt_) | |
| geohashdroid | 17.78% | `geohashdroid-0.9.4-#73-rebuilt.apk` | [results](results/smoke/geohashdroid-0.9.4-_73-rebuilt_) | yes |
| SkyTube | 16.41% | `SkyTube-#v2.999-rebuilt.apk` | [results](results/smoke/SkyTube-_v2.999-rebuilt_) | |
| StreetComplete | 15.20% | `StreetComplete-#v53.2-rebuilt.apk` | [results](results/smoke/StreetComplete-_v53.2-rebuilt_) | yes |
| FirefoxLite | 13.72% | `FirefoxLite-2.1.20-#5085-rebuilt.apk` | [results](results/smoke/FirefoxLite-2.1.20-_5085-rebuilt_) | yes |
| AmazeFileManager | 13.08% | `AmazeFileManager-3.4.2-#1837-rebuilt.apk` | [results](results/smoke/AmazeFileManager-3.4.2-_1837-rebuilt_) | yes |
| Omni-Notes | 10.58% | `Omni-Notes-6.1.0-#745-rebuilt.apk` | [results](results/smoke/Omni-Notes-6.1.0-_745-rebuilt_) | yes |
| Orgzly-Revived | 10.50% | `Orgzly-Revived-#v1.8.27beta.2-rebuilt.apk` | [results](results/smoke/Orgzly-Revived-_v1.8.27beta.2-rebuilt_) | |
| LibreTorrent | 9.37% | `LibreTorrent-#3.5.2-rebuilt.apk` | [results](results/smoke/LibreTorrent-_3.5.2-rebuilt_) | |
| nextcloud | 8.63% | `nextcloud-#4792-rebuilt.apk` | [results](results/smoke/nextcloud-_4792-rebuilt_) | yes |
| Open-Food-Facts | 8.43% | `Open-Food-Facts-#v3.10.2-rebuilt.apk` | [results](results/smoke/Open-Food-Facts-_v3.10.2-rebuilt_) | |
| Wallabag | 7.82% | `Wallabag-#2.5.3-rebuilt.apk` | [results](results/smoke/Wallabag-_2.5.3-rebuilt_) | |
| newpipe | 7.57% | `newpipe-#v0.25.1-rebuilt.apk` | [results](results/smoke/newpipe-_v0.25.1-rebuilt_) | yes |
| commons | 7.57% | `commons-2.7.1-#1581-rebuilt.apk` | [results](results/smoke/commons-2.7.1-_1581-rebuilt_) | yes |
| ownCloud | 6.56% | `ownCloud-#v4.4.0-rebuilt.apk` | [results](results/smoke/ownCloud-_v4.4.0-rebuilt_) | |
| Breezy-Weather | 6.22% | `Breezy-Weather-#v5.2.8-rebuilt.apk` | [results](results/smoke/Breezy-Weather-_v5.2.8-rebuilt_) | |
| AnkiDroid | 4.11% | `AnkiDroid-debug-2.6beta6-#4200-rebuilt.apk` | [results](results/smoke/AnkiDroid-debug-2.6beta6-_4200-rebuilt_) | yes |
| Wikipedia | 3.41% | `Wikipedia-#beta2.7.50447beta2023062-rebuilt.apk` | [results](results/smoke/Wikipedia-_beta2.7.50447beta2023062-rebuilt_) | yes |
| Infinity-For-Reddit | 3.16% | `Infinity-For-Reddit-#v7.3.4-rebuilt.apk` | [results](results/smoke/Infinity-For-Reddit-_v7.3.4-rebuilt_) | |
| Twire | 2.67% | `Twire-#v2.10.7-rebuilt.apk` | [results](results/smoke/Twire-_v2.10.7-rebuilt_) | yes |
| openlauncher | 2.35% | `openlauncher-0.3.1-#67-rebuilt.apk` | [results](results/smoke/openlauncher-0.3.1-_67-rebuilt_) | yes |
| Kore | 1.72% | `Kore-#v3.1.0-rebuilt.apk` | [results](results/smoke/Kore-_v3.1.0-rebuilt_) | |
| Jellyfin-Android | 1.25% | `Jellyfin-Android-#v2.6.2-rebuilt.apk` | [results](results/smoke/Jellyfin-Android-_v2.6.2-rebuilt_) | |
| Fedilab | 0.52% | `Fedilab-#3.28.0-rebuilt.apk` | [results](results/smoke/Fedilab-_3.28.0-rebuilt_) | |

Every APK is named `-rebuilt` except MaterialFBook. `-rebuilt` means the APK was
built here from the source in the same folder, so its classes are byte-for-byte
the ones in `class_files.json`. That is what makes a report come out with zero
mismatched classes. MaterialFBook uses its published APK because its instrumented
source branch was lost with a deleted GitHub account, and it happens to match.

## Where each app came from

| app | source repo | ref |
|---|---|---|
| AmazeFileManager | tingsu/AmazeFileManager | `instrumented-version-3.2.1` |
| AnkiDroid | tingsu/Anki-Android | `instrumented-version-2.6beta6` |
| Binary-Eye | markusfisch/BinaryEye | `1.63.12` |
| Breezy-Weather | breezy-weather/breezy-weather | `v5.2.8` |
| Fedilab | stom79/Fedilab | `3.28.0` |
| FirefoxLite | tingsu/FirefoxLite | `instrumented-version-2.1.20-#5085` |
| Infinity-For-Reddit | Docile-Alligator/Infinity-For-Reddit | `v7.3.4` |
| Jellyfin-Android | jellyfin/jellyfin-android | `v2.6.2` |
| Kiwix | kiwix/kiwix-android | `3.11.1` |
| Kore | xbmc/Kore | `v3.1.0` |
| LibreTorrent | proninyaroslav/libretorrent | `3.5.2` |
| MaterialFBook | ZeeRooo/MaterialFBook | `v4.0.2` |
| Omni-Notes | tingsu/Omni-Notes | `instrumented-version-6.1.0` |
| Open-Food-Facts | openfoodfacts/openfoodfacts-androidapp | `v3.10.2` |
| Orgzly-Revived | orgzly-revived/orgzly-android-revived | `v1.8.27-beta.2` |
| SkyTube | SkyTubeTeam/SkyTube | `v2.999` |
| StreetComplete | streetcomplete/StreetComplete | `v53.2` |
| Twire | twireapp/Twire | `v2.10.7` |
| Wallabag | wallabag/android-app | `2.5.3` |
| Wikipedia | wikimedia/apps-android-wikipedia | `beta/2.7.50447-beta-2023-06-28` |
| commons | tingsu/apps-android-commons | `instrumented-version-2.7.1` |
| geohashdroid | tingsu/geohashdroid | `instrumented-version-0.9.4` |
| newpipe | TeamNewPipe/NewPipe | `v0.25.1` |
| nextcloud | skull591/android | `buggy-4792` |
| openlauncher | tingsu/openlauncher | `instrument-version-0.3.1` |
| ownCloud | owncloud/android | `v4.4.0` |

Refs named `instrumented-version-*` or `buggy-*` are Themis benchmark branches
that already carried the harness. The rest are upstream release tags that were
instrumented here.

## What is in a smoke test folder

```
results/smoke/<apk-slug>/
├── coverage.ec         raw execution data pulled off the device
├── coverage.xml        the JaCoCo report
├── coverage_html/      annotated source - open index.html
├── smoke.log           the adb transcript for this app
└── probe_report.txt    per-class probe counts
```

`results/smoke/smoke_summary.csv` is the verdict table for all 26.
`PASS` means: installed, launched, produced execution data, and the report
generated with **zero** mismatched classes.

## Running it

Git LFS must be installed **before** cloning or the APKs arrive as text pointers.
Emulator must be API 25, `google_apis`, x86. See [HANDOVER.md](HANDOVER.md).

```bash
./smoke_test_dataset.sh              # all 26
./smoke_test_dataset.sh Kiwix Twire  # by name
```

## Not included

Build and install but produce no coverage data, a device-side problem:
**Muzei**, **Vinyl-Music-Player**.

Do not build: **GPSLogger** (R8 fails), **Trackbook**, **Transistor**,
**Ultrasonic** (its own jacoco config clashes with the plugin).

`dataset_builder/research_subjects.json` still describes them, so
`dataset_builder/supervise.sh` can retry one without touching the rest.
