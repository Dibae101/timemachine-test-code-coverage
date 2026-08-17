# Dataset builder

Builds TimeMachine coverage datasets in the same layout as `instrumented_apps/`,
for the 44 unique apps that appear in the published 60-minute run report
(`reproduction/results/run-60m` and `_check-22`).

## What a dataset entry is

```
instrumented_apps/<App>/
    <App>-#<id>/          project tree, pruned to compiled classes + sources
    <apk>                 the APK those classes belong to
    <apk>-rebuilt.apk     locally built APK (guaranteed to match the classes)
    class_files.json      {apk: {classfiles: [...], sourcefiles: [...]}}
```

`class_files.json` is what `jacococli report` consumes: `--classfiles` for every
`classfiles` entry, `--sourcefiles` for every `sourcefiles` entry. Paths are
relative to the app directory.

Both an upstream-published APK and a locally rebuilt one are declared where
possible. The rebuilt APK is the safe one to test with, because its classes are
byte-for-byte the ones in `class_files.json`; a published APK built elsewhere can
report `does not match` for some classes.

## Where the 44 apps come from

| group | apps | source |
|---|--:|---|
| `themis-jacoco` | 17 | Themis benchmark: prebuilt APK from `the-themis-benchmarks/home`, sources from the per-bug `instrumented-version-*` branch |
| `ddroid-jacoco` | 1 | Scarlet-Notes, same style of instrumented branch |
| `hybriddroid-jacoco` | 10 | plain upstream projects, instrumented here |
| `22-dataset` | 16 | plain upstream projects, instrumented here |

The Themis branches already carry the Jacoco harness. The other 26 do not, so
`instrument.py` injects it.

## Running it

```bash
# one pass over the Themis/DDroid subjects
python3 build_dataset.py

# one pass over the upstream subjects
python3 build_dataset.py --subjects modern_subjects.json

# repeat passes until 40 subjects are usable, with a failure tally after each
./supervise.sh

# rebuild state from what is on disk (after a crash, or to re-check)
python3 reconcile.py

# inventory + validation, writes instrumented_apps/MANIFEST.tsv
python3 make_manifest.py

# group the remaining failures by cause
python3 diagnose.py
```

State lives in `status-subjects.csv` and `status-modern_subjects.csv`; anything
not yet `ok` is retried on the next pass. Per-subject build logs are in `logs/`.

## Things that had to be solved

These are the reasons a 2017-2023 Android project does not build unmodified in
2026. All of them are handled in `repair-repos.init.gradle` or the build driver.

* **jcenter is gone.** Every Themis branch declares it. Dead bintray
  repositories are stripped and replaced. A stand-in is genuinely required, not
  just `mavenCentral()`: `anko-commons:0.10.8`, `okhttp-digest:1.18` and
  `jacoco-android:0.1.3` were never republished to Central. `repo.huaweicloud.com`
  serves them.
* **Mirror ordering matters.** Gradle fetches an artifact from whichever
  repository supplied its metadata. huaweicloud has the `.pom` but not the `.jar`
  for some Gradle plugins (spotbugs, detekt), so canonical repositories are
  listed first and the stand-in last.
* **An aliyun mirror was tried and dropped.** It serves the artifacts but returns
  502/404 unpredictably for `maven-metadata.xml`, and a failed metadata fetch
  aborts resolution of a dynamic version such as
  `androidx.room:room-migration:[2.2.2]` instead of falling through.
* **`assembleDebug` builds every product flavour.** One bad flavour fails the
  whole build: AmazeFileManager 3.2.1 overflows the 64K dex limit on its `play`
  flavour. The driver falls back to single-flavour tasks and prefers
  fdroid/foss/vanilla variants, which is also what the published APKs are.
* **Quality gates fail on the injected harness.** NewPipe's `runCheckstyle`
  rejects the harness files. Checkstyle/ktlint/detekt/spotbugs/pmd/lint tasks are
  disabled; none of them affect whether an instrumented APK is produced.
* **The coverage flag was renamed.** AGP 8 removed `testCoverageEnabled`;
  `instrument.py` emits `enableAndroidTestCoverage` for AGP 8, both names for
  AGP 7, the original for older.
* **Version catalogs hide the plugin id.** Modern projects apply
  `alias(libs.plugins.android.application)`, so searching for the literal
  `com.android.application` finds no application module.
* **Class output moved between AGP generations.** AGP 2.x writes
  `build/intermediates/classes/<flavour>/<type>` with no directory named
  `classes` at the leaf; AGP 3.0-3.1 and 3.2+ each differ again, and Kotlin
  output lives under `build/tmp/kotlin-classes`. All layouts are globbed, then
  ancestors are dropped so a single variant is declared rather than every
  flavour at once.
* **compileSdk 31-34** were missing from this host's SDK and had to be installed;
  without them every recent upstream app fails.
* **Pruning has to be conservative.** Keeping only the selected leaf directories
  once deleted commons' javac output, leaving an entry that could not be repaired
  without a full rebuild. The whole class-output container is kept now.
* **One status file cannot be shared by two concurrent builders.** Each holds a
  snapshot and rewrites the file per subject, so the second silently reverts the
  first. State is per subject set, and `reconcile.py` can rebuild it from disk.

## Storage

Each finished subject is pruned to compiled classes, sources, and the APKs -
roughly 50-250 MB rather than the 80 MB-1.5 GB a full Gradle build tree
occupies. `build_dataset.py` stops on its own if free space drops below 4 GB.

## Publishing

`.gitattributes` routes `*.apk` and `*.ec` through Git LFS: GitHub rejects any
file over 100 MB, and the dataset carries tens of APKs. Compiled `.class` files
stay as ordinary git objects, since they are small individually and number in the
tens of thousands, which is the case LFS handles worst.

Note before pushing: LFS storage on the free GitHub tier is 1 GB. The APK set is
several hundred MB, so check the account's LFS quota, or keep APKs out of the
repository and publish them alongside a manifest of SHA-256 sums, which is what
the research repo already does.
