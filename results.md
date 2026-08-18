# Datasets and results

26 Android apps. Each has a Jacoco coverage dataset and a smoke test proving it
works end to end: the APK installs on an API 25 emulator, the app runs, execution
data is dumped on broadcast, and `jacococli` turns it into an annotated HTML
report with zero mismatched classes.

## The 26 datasets

`coverage` is a smoke-test floor from a 40-event monkey burst, not a real testing
session. `scope` is how many of the app's compiled classes the report covers -
read the caveat under [Coverage scope](#coverage-scope) before comparing numbers.

| app | coverage | scope | pages | package | source | ref |
|---|--:|--:|--:|---|---|---|
| Binary-Eye | 47.17% | 4/370 | 2 | `de.markusfisch.android.binaryeye.debug` | markusfisch/BinaryEye | `bf5dc96a` |
| MaterialFBook | 19.24% | 55/55 | 28 | `me.zeeroooo.materialfb` | ZeeRooo/MaterialFBook | `v4.0.2` |
| Kiwix | 19.19% | 77/1402 | 2 | `org.kiwix.kiwixmobile` | kiwix/kiwix-android | `9c4ae35c` |
| geohashdroid | 17.78% | 114/438 | 53 | `net.exclaimindustries.geohashdroid` | tingsu/geohashdroid | `instrume` |
| SkyTube | 16.41% | 346/1352 | 179 | `free.rm.skytube.extra` | SkyTubeTeam/SkyTube | `072e2291` |
| StreetComplete | 15.20% | 3782/3782 | 2 | `de.westnordost.streetcomplete.debug` | streetcomplete/StreetComplete | `3a7dc5b6` |
| FirefoxLite | 13.72% | 3490/4176 | 400 | `org.mozilla.rocket.debug.ubuntu` | tingsu/FirefoxLite | `instrume` |
| AmazeFileManager | 13.08% | 507/1200 | 253 | `com.amaze.filemanager.debug` | tingsu/AmazeFileManager | `instrume` |
| Omni-Notes | 10.58% | 237/1015 | 152 | `it.feio.android.omninotes` | tingsu/Omni-Notes | `instrume` |
| Orgzly-Revived | 10.50% | 1213/2426 | 56 | `com.orgzlyrevived` | orgzly-revived/orgzly-android-revived | `f0cccf20` |
| LibreTorrent | 9.37% | 688/2752 | 258 | `org.proninyaroslav.libretorrent.debug` | proninyaroslav/libretorrent | `d93e92ea` |
| nextcloud | 8.63% | 1403/3370 | 328 | `com.nextcloud.client` | skull591/android | `buggy-47` |
| Open-Food-Facts | 8.43% | 591/14148 | 33 | `org.openpetfoodfacts.scanner.debug` | openfoodfacts/openfoodfacts-androidapp | `b811f0a3` |
| Wallabag | 7.82% | 296/592 | 159 | `fr.gaulupeau.apps.InThePoche.debug` | wallabag/android-app | `277ddaa3` |
| commons | 7.57% | 468/936 | 130 | `fr.free.nrw.commons.debug` | tingsu/apps-android-commons | `instrume` |
| newpipe | 7.57% | 1031/1031 | 302 | `org.schabi.newpipe.debug.HEAD` | TeamNewPipe/NewPipe | `v0.25.1` |
| ownCloud | 6.56% | 1296/1296 | 65 | `com.owncloud.android.debug` | owncloud/android | `3913582b` |
| Breezy-Weather | 6.22% | 3245/14224 | 2 | `org.breezyweather.debug` | breezy-weather/breezy-weather | `7fcc13ce` |
| AnkiDroid | 4.11% | 676/676 | 243 | `com.ichi2.anki` | tingsu/Anki-Android | `instrume` |
| Wikipedia | 3.41% | 3053/11279 | 6 | `org.wikipedia.dev` | wikimedia/apps-android-wikipedia | `beta/2.7` |
| Infinity-For-Reddit | 3.16% | 2080/4160 | 563 | `ml.docilealligator.infinityforreddit.debug` | Docile-Alligator/Infinity-For-Reddit | `b703463f` |
| Twire | 2.67% | 324/324 | 117 | `com.perflyst.twire.debug` | twireapp/Twire | `43572429` |
| openlauncher | 2.35% | 250/741 | 50 | `com.benny.openlauncher` | tingsu/openlauncher | `instrume` |
| Kore | 1.72% | 759/1518 | 208 | `org.xbmc.kore` | xbmc/Kore | `5f711140` |
| Jellyfin-Android | 1.25% | 386/1486 | 6 | `org.jellyfin.mobile.debug` | jellyfin/jellyfin-android | `8908d18f` |
| Fedilab | 0.52% | 1238/5197 | 544 | `app.fedilab.android.debug` | stom79/Fedilab | `c44330d2` |

## Coverage scope

**14 of the 26 declare fewer than half their compiled classes**, so their
percentage is measured over part of the app rather than all of it. The extreme
cases are Binary-Eye (4 of 370 classes), Open-Food-Facts (591 of 14,148) and
Kiwix (77 of 1,402).

The reason is deliberate but has a cost. A class directory is only declared if
adding it keeps the report free of mismatches. JaCoCo excludes any class whose
bytecode differs from what the execution data was recorded against, so a set
holding another variant's build of a class produces a wrong number silently. For
Kotlin-heavy apps the Kotlin output often cannot be reconciled with the APK this
way, so it is left out. The result is a report that is correct about what it
measures, over a smaller denominator.

Part of the gap is also double counting: an app with three product flavours
compiles the same classes three times, so the `tree` figure overstates the number
of distinct classes. Both effects are present and are not separated here.

Treat these percentages as proof the pipeline works, not as the app's true
coverage.

## What each dataset contains

```
instrumented_apps/<App>/
    class_files.json     what jacococli reads: class and source directories
    <App>-#<version>/    project tree, pruned to compiled classes + sources
    <name>-rebuilt.apk   the APK those classes belong to
    upstream/            submodule -> github.com/bugfixops/<repo>  (15 of 26)
```

Everything a coverage report needs is committed, so a plain clone works with no
rebuild. Results per app are in `results/smoke/<apk-slug>/`:

```
coverage.ec        raw execution data pulled from the device
coverage.xml       JaCoCo XML report
coverage_html/     annotated source, open index.html
smoke.log          adb transcript
probe_report.txt   per-class probe counts
```

## Where they came from

**Themis and related benchmarks** (13 apps). APKs published at
`github.com/the-themis-benchmarks/home`; sources from the per-bug
`instrumented-version-*` branches, which already carry the Jacoco harness. Each
was rebuilt locally so the classes match the APK.

**llm-gui-testing-research** (13 apps: Binary-Eye, Breezy-Weather, Fedilab,
Infinity-For-Reddit, Jellyfin-Android, Kiwix, Kore, LibreTorrent,
Open-Food-Facts, Orgzly-Revived, SkyTube, Wallabag, ownCloud). From
`dataset/apps.tsv` in `Dibae101/llm-gui-testing-research`, which records the
exact `source_repo`, `source_ref` and `source_commit` behind each tested APK.
Those exact commits are why these built reliably; guessing tags had been the main
cause of earlier failures.

## How they were built

`dataset_builder/build_dataset.py` per app: clone at the recorded ref, inject the
Jacoco harness if absent, build a debug APK, collect compiled classes and
sources, write `class_files.json`, prune the tree.

The harness (`JacocoIntegration/JacocoInstrument`) applies the `jacoco` plugin,
enables coverage for `debug`, and registers a receiver for
`edu.gatech.m3.emma.COLLECT_COVERAGE` so a running app dumps `coverage.ec`.
18 of 26 were instrumented this way; the Themis branches came pre-instrumented.

Toolchain: API 25 `google_apis` x86 emulator, build-tools 28.0.3, JaCoCo CLI
0.8.13, JDK 8/11/17/21 chosen per project.

## What had to be changed

None of these projects builds unmodified today. Each row was a real failure.

| change | why |
|---|---|
| jcenter replaced with google + mavenCentral + huaweicloud + jitpack | jcenter is shut down and `anko-commons`, `okhttp-digest`, `jacoco-android` were never republished to Central |
| canonical repos ordered ahead of the mirror | Gradle fetches an artifact from whichever repo gave its metadata; huaweicloud has some plugin `.pom` but not the `.jar` |
| aliyun mirror tried, then dropped | unpredictable 502/404 on `maven-metadata.xml`, which aborts dynamic version resolution |
| repositories injected in `settingsEvaluated` | modern projects set `repositoriesMode = FAIL_ON_PROJECT_REPOS`; `beforeSettings` runs too early and was overridden |
| quality gates disabled | checkstyle/ktlint/detekt/lint reject the injected harness |
| R8 disabled for debug | it rewrites classes, breaking the APK-to-classes mapping, and failed outright on the harness |
| `local.properties` written after clone | gitignored, so absent; some projects fail configuration without it |
| per-flavour build fallback | `assembleDebug` builds every flavour and one bad flavour fails everything; AmazeFileManager overflows the 64K dex limit on `play` |
| JDK 8/11/17/21 ladder gated by Gradle version | Gradle 4-5 cannot run on JDK 17, Gradle 8 cannot run on JDK 8, some 2024 projects want a JDK 21 toolchain |
| coverage flag per AGP version | AGP 8 removed `testCoverageEnabled` in favour of `enableAndroidTestCoverage` |
| comment-aware brace matching | braces inside commented-out code were counted, and the coverage flag was once written into the middle of a comment |
| module detection fallback and sample-module ranking | version-catalog aliases and buildSrc convention plugins hide the plugin id; Muzei's `example-unsplash` sample was picked over the real app |
| JaCoCo CLI 0.8.6 -> 0.8.13 | 0.8.6 cannot analyse Java 17 bytecode (`Unsupported class file major version 61`) |
| class dirs chosen by zero-mismatch report | a report that merely runs is not correct; requiring zero mismatches took Kiwix 15.52% -> 19.19% and Open-Food-Facts 3.55% -> 8.43% |
| generated `R` classes dropped | no meaningful coverage, and they collide across modules and flavours |

## Running them

`HANDOVER.md` has the detail. In short: `git lfs install` before cloning (APKs are
LFS objects), use an **API 25 `google_apis` x86** emulator, then:

```bash
./smoke_test_dataset.sh                    # all 26
./smoke_test_dataset.sh geohash            # one, by substring
python3 dataset_builder/make_manifest.py   # validate without an emulator
```

## Known limits

* Percentages are smoke-test floors, and 14 apps measure a subset of their classes.
* Only tested on API 25, x86, without KVM. Nothing verified on arm64.
* 11 of 26 have no `upstream` submodule: the research-derived apps were built from
  their upstream repos and have not been forked into `bugfixops`.
* Six apps were attempted and dropped. GPSLogger, Trackbook, Transistor and
  Ultrasonic do not build; Muzei and Vinyl-Music-Player install but produce no
  execution data. `dataset_builder/research_subjects.json` still describes them.
* `HANDOVER.md` still describes an earlier 8-app state and needs updating.

