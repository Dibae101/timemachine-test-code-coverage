# Datasets and results

26 Android apps. Each one has a Jacoco coverage dataset and a smoke test that
proves it works: the APK installs on an API 25 emulator, the app runs, coverage
data is dumped on broadcast, and `jacococli` produces an HTML report with zero
mismatched classes.

## The 26 datasets

| app | coverage | pages | package |
|---|--:|--:|---|
| Binary-Eye | 47.17% | 2 | `de.markusfisch.android.binaryeye.debug` |
| MaterialFBook | 19.24% | 28 | `me.zeeroooo.materialfb` |
| Kiwix | 19.19% | 2 | `org.kiwix.kiwixmobile` |
| geohashdroid | 17.78% | 53 | `net.exclaimindustries.geohashdroid` |
| SkyTube | 16.41% | 179 | `free.rm.skytube.extra` |
| StreetComplete | 15.20% | 2 | `de.westnordost.streetcomplete.debug` |
| FirefoxLite | 13.72% | 400 | `org.mozilla.rocket.debug.ubuntu` |
| AmazeFileManager | 13.08% | 253 | `com.amaze.filemanager.debug` |
| Omni-Notes | 10.58% | 152 | `it.feio.android.omninotes` |
| Orgzly-Revived | 10.50% | 56 | `com.orgzlyrevived` |
| LibreTorrent | 9.37% | 258 | `org.proninyaroslav.libretorrent.debug` |
| nextcloud | 8.63% | 328 | `com.nextcloud.client` |
| Open-Food-Facts | 8.43% | 33 | `org.openpetfoodfacts.scanner.debug` |
| Wallabag | 7.82% | 159 | `fr.gaulupeau.apps.InThePoche.debug` |
| newpipe | 7.57% | 302 | `org.schabi.newpipe.debug.HEAD` |
| commons | 7.57% | 130 | `fr.free.nrw.commons.debug` |
| ownCloud | 6.56% | 65 | `com.owncloud.android.debug` |
| Breezy-Weather | 6.22% | 2 | `org.breezyweather.debug` |
| AnkiDroid | 4.11% | 243 | `com.ichi2.anki` |
| Wikipedia | 3.41% | 6 | `org.wikipedia.dev` |
| Infinity-For-Reddit | 3.16% | 563 | `ml.docilealligator.infinityforreddit.debug` |
| Twire | 2.67% | 117 | `com.perflyst.twire.debug` |
| openlauncher | 2.35% | 50 | `com.benny.openlauncher` |
| Kore | 1.72% | 208 | `org.xbmc.kore` |
| Jellyfin-Android | 1.25% | 6 | `org.jellyfin.mobile.debug` |
| Fedilab | 0.52% | 544 | `app.fedilab.android.debug` |

**The coverage numbers are floors, not results.** They come from a 40-event
monkey burst that lasts about 90 seconds. A real TimeMachine run reaches much
higher. Their only purpose here is to prove the dataset works.

Do not compare apps against each other on this number either. A big app touched
briefly scores low; a small one scores high. Binary-Eye leads at 47% because the
run happened to reach 4 of its 370 classes and covered most lines in them.

## Where each file is

```
instrumented_apps/<App>/
    class_files.json        what jacococli reads
    <App>-#<ref>/           project tree: compiled classes + sources
    <apk>                   the APK those classes belong to
    upstream/               submodule -> github.com/bugfixops/<repo>

results/smoke/<apk>/
    coverage.ec             raw execution data
    coverage.xml            report
    coverage_html/          annotated source, open index.html
    smoke.log               adb transcript
```

`instrumented_apps/MANIFEST.tsv` lists every entry with its package, SHA-256 and
source. `results/smoke/smoke_summary.csv` holds the verdict table.

## Running them

Needs Git LFS installed **before** cloning, or the APKs arrive as text pointers.
Needs an API 25 emulator, `google_apis`, x86. Full instructions in
[HANDOVER.md](HANDOVER.md).

```bash
./smoke_test_dataset.sh                 # all of them
./smoke_test_dataset.sh Kiwix Twire     # by name
```

## What is not included

Two apps build and install but produce no coverage data at all, which is a
device-side problem: **Muzei** and **Vinyl-Music-Player**.

Four apps do not build: **GPSLogger** (R8 fails), **Trackbook**, **Transistor**,
and **Ultrasonic** (its own jacoco config clashes with the plugin).

`dataset_builder/research_subjects.json` still describes all of them, so
`supervise.sh` can retry any one without redoing the rest.
