# TimeMachine coverage datasets

A collection of **59 Android apps packaged so a GUI testing tool can measure code
coverage on them**, each with a matching coverage run measured on a device.

Every app in `instrumented_apps/` has exactly one result directory in
`results/smoke/`, and every one of them reports with **zero mismatched classes**.

This is a fork of [DroidTest/TimeMachine](https://github.com/DroidTest/TimeMachine)
with the dataset work added. The original tool and paper are unchanged; see
[the upstream README](https://github.com/DroidTest/TimeMachine) for what
TimeMachine does.

* **[dataset.md](dataset.md)** - what a dataset contains, what was changed in each
  app, the app list, and a link to each app's smoke test
* **[dataset_builder/README.md](dataset_builder/README.md)** - how the datasets are
  built and every upstream breakage that had to be worked around

## The results

`results/smoke/` is the only results folder. It holds one directory per app, named
after that app's APK, produced by installing the APK on redroid Android 12 (API 31),
exercising it with a 40-event monkey burst, dumping a real `.ec` and building a
report.

| | |
|---|--:|
| apps | 59 |
| installed and launched | 59 |
| reporting with zero mismatched classes | 59 |
| line coverage | 0.51% - 31.66%, median 9.40% |
| device execution data | 5.4 MB |

```
results/smoke/Twire-_v2.10.7-rebuilt_/
├── coverage.ec         execution data pulled off the device
├── coverage.xml        jacoco report
├── coverage_html/      annotated, colour-coded source
├── probe_report.txt    jacococli execinfo
└── probe_summary.csv   per-class probe hits
results/smoke/smoke_summary.csv   the verdict table for all 59
```

The correspondence is checked, not assumed: 59 datasets, 59 summary rows, 59 result
directories, no orphans either way, and for every entry the class ids recorded in
its `coverage.ec` resolve against its own declared class directories with no
name-only mismatches.

### Two apps were removed rather than shipped imperfect

Kiwix and Open-Food-Facts each reported a handful of mismatched classes that could
not be fixed by declaring a different directory. Both had classes rewritten by a
bytecode transformer that writes no post-transform copy to disk - ObjectBox entity
cursors for Kiwix, Hilt entry points (`SplashActivity`, `WelcomeActivity`,
`OFFApplication`) for Open-Food-Facts - so the bytecode that ran exists only inside
the APK's dex and no directory can reproduce its class ids. Rather than ship two
entries that silently under-count, they are out. Their build recipes remain in
`dataset_builder/` if someone wants to solve it.

### Running the device test without an emulator: redroid

[redroid](https://github.com/remote-android/redroid-doc) runs Android as a Docker
container against the host kernel, so it needs no KVM and it publishes arm64
images. That is what makes a device run possible on an aarch64 host where the SDK
emulator does not even exist as a binary. It is also far quicker than the software
-emulated AVD: about 25 seconds per app against 60-150, and a ~10 second boot
rather than five minutes.

```bash
# host kernel modules (once)
sudo apt-get install -y linux-modules-extra-$(uname -r)
sudo modprobe binder_linux devices="binder,hwbinder,vndbinder"

docker run -itd --name redroid31 --privileged \
    -v ~/redroid-data/api31:/data -p 5555:5555 \
    redroid/redroid:12.0.0-latest \
    androidboot.use_memfd=1 ro.secure=0

adb connect localhost:5555
PROJECT="$PWD" SDK=/path/to/sdk ADB=/usr/bin/adb SERIAL=localhost:5555 \
    ./smoke_test_dataset.sh
```

Three details matter:

* `androidboot.use_memfd=1` - `ashmem_linux` does not exist on current kernels, and
  redroid needs one or the other.
* `ro.secure=0` - gives a root `adb shell`, without which `coverage.ec` cannot be
  read out of app-private storage.
* Use a **native** `adb`. The SDK's is x86_64 and dies under binfmt with
  `Could not open '/lib64/ld-linux-x86-64.so.2'` unless `QEMU_LD_PREFIX` is set.
* Use the **non-`_64only`** image. Several apps ship 32-bit native libraries.

Two changes to the harness were needed to make the coverage dump work on a modern
API level at all, and they apply to any emulator newer than API 25:

* **The broadcast must name the receiver.** An implicit broadcast to a
  manifest-declared receiver is not delivered on API 26 and above. It returns
  `Broadcast completed: result=0` with no receiver having run and no `.ec` written,
  which looks exactly like a broken dataset. Setting the package is not sufficient;
  the component has to be named, and it is read back from `dumpsys package`.
* **The app must really be running.** `monkey -p <pkg> 1` can exit without starting
  an activity. The dump then contains only the two harness classes - 224 bytes for
  geohashdroid instead of 1600 - because none of the app's own code ever ran. The
  launcher activity is resolved and started directly now.

### Which emulator an app needs

`MANIFEST.tsv` records `min_sdk` per APK, because that decides where an app can be
smoke tested:

* **42 of 59** install on the API 25 image the original set was tested on.
* **17 need something newer** - 13 at API 26, then Calculator-You (27), uhabits
  (28), Feeder (29) and FastNFitness (31).

An API 31 image covers all 59, which is what the committed run used. The original 26
were pinned to API 25 because newer
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
unresolved paths. It reports 59 discovered, 0 dataset problems, and 17 too new for
API 25.

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
All were rebuilt from the exact refs in `dataset_builder/research_subjects.json`
and now carry their classes; Kiwix and Open-Food-Facts were later dropped for the
unrelated mismatch reason above.

The guard against a repeat is `python3 dataset_builder/make_manifest.py`, which
fails when a declared path does not resolve or holds no `.class` files. Worth
running after cloning. The stronger check is that every entry here has a
`coverage.ec` in `results/smoke/` whose class ids resolve against its own declared
directories, which cannot be true of an entry with no classes.

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

59 apps, one APK each, all 59 entries validating in
[`instrumented_apps/MANIFEST.tsv`](instrumented_apps/MANIFEST.tsv) with per-entry
package, minSdk, SHA-256, source commit, and class and source counts. Per-app
coverage is in
[`results/smoke/smoke_summary.csv`](results/smoke/smoke_summary.csv).

The set spans file managers, RSS and Reddit clients, media players, note takers,
calendars, launchers, trackers, calculators and games, built with AGP 2.3 through
AGP 9 and mixing Java, Kotlin and Compose. That spread is deliberate: the older
projects exercise the AGP 2.x/3.x class-output layouts, the newest exercise
Compose and AGP's built-in Kotlin compilation.

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

105 apps are defined across the subject sets. 59 produce a complete dataset that
reports cleanly against a real device run. The rest fail to build or to instrument, and are named with their state in
the `status-*.csv` files under `dataset_builder/`.

## Where each APK came from

**Every APK is built here, from the source tree shipped beside it.** That is what
the `-rebuilt` suffix means, and it is the whole point: the compiled classes in the
entry are byte-for-byte the ones inside that APK, which is what makes a report come
out with no mismatched classes. The APK is *not* an upstream release binary paired
with a guessed-at source tree.

So the answer to "is the code the latest and the APK a different release?" is no:
the checkout is pinned to a release tag, not to `HEAD`, and the APK is compiled
from that checkout.

One exception. **MaterialFBook** ships its published APK, because its instrumented
source branch was lost with a deleted GitHub account. Its classes happen to match.

`instrumented_apps/PROVENANCE.tsv` pins each entry to a commit:

| refs | count | note |
|---|--:|---|
| tag | 53 | immutable in practice |
| branch | 8 | **mutable** - the Themis `instrumented-version-*` branches, plus nextcloud's `buggy-4792` |

`MANIFEST.tsv` carries the same SHA in its `source_commit` column.

Two honest caveats:

* The commit was **not** recorded when these entries were built. The builder
  deleted each checkout's `.git` immediately after cloning, so only the ref name
  survived, and `record_provenance.py` resolved those refs afterwards. For a tag
  that has not moved this is exact; for the 8 mutable branches it is today's tip,
  which may not be what was built. `build_dataset.py` now captures
  `git rev-parse HEAD` before `.git` is removed and appends it to
  `SOURCE_COMMIT.txt`, so entries built from here on are pinned exactly.
* Verified where possible. 46 of the 51 tag-resolved entries have an APK
  `versionName` matching their tag. The other five are explained, not wrong:
  Open-Food-Facts and OpenCalc declare a `versionName` upstream never bumped
  (3.9.0 at tag `v3.10.2`, 3.2.0 at `v3.2.1`), MyExpenses uses a marketing version
  against a revision tag, and Kore and Jellyfin derive their version from
  `git describe`. The last two are worth knowing about: with the checkout's `.git`
  removed, Kore's build resolved to *this* repository and stamped its commit hash
  as the app's `versionName`. `commons` is the useful cross-check in the other
  direction - its `versionName` embeds `~36cdb86`, which matches the SHA resolved
  for its branch.

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
| Apps that never produced a clean report | not usable datasets; Kiwix and Open-Food-Facts were dropped for this reason after real coverage exposed a few mismatched classes each |
| Duplicate APKs per app | each app appears exactly once, with the APK whose classes are declared |
| Nested `.git` directories | otherwise git records a submodule reference and uploads none of the contents |
| CI workflow files, in the forks only | pushing them needs a token scope this pipeline does not use |

Kept deliberately: the full smoke test output for all 59 apps, including raw
execution data and annotated HTML, so the coverage claims can be checked rather
than taken on trust. An earlier synthetic validation set was removed once every app
had a real device run; `regenerate_reports.sh` rebuilds the derived XML and HTML
from the committed `.ec` if the tree ever needs slimming.

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

# smoke test (needs a working emulator)
./smoke_test_dataset.sh                  # every app in the dataset
./smoke_test_dataset.sh Twire Markor    # by name
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

The expansion was built on a 16-core **aarch64** host with 61 GB RAM and no
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

Repository: 59 APKs in LFS, ~95,000 compiled class files, and one results folder. The APK set alone is about 1.9 GB, which is over the 1 GB free GitHub LFS
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
