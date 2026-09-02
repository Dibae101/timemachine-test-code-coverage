#!/bin/bash
#
# smoke_test_dataset.sh - fast per-app validation of the TimeMachine dataset.
#
# Answers one question per app, cheaply: "does this app launch, does the Jacoco
# agent produce coverage, and do the class files match the apk?"
#
# It deliberately does NOT run TimeMachine. A full run costs ~35 min/app because
# of the emulator boot and the fuzz-cycle length. This boots ONE emulator, then
# per app does: install -> launch -> short event burst -> COLLECT_COVERAGE
# broadcast -> pull .ec -> jacococli report. About 60-90s per app.
#
# Artifacts land in results/smoke/<app>/ :
#     coverage.ec      raw execution data
#     coverage.xml     jacoco report
#     coverage_html/   annotated source report (when sources are declared)
#     smoke.log        adb transcript for that app
# plus results/smoke/smoke_summary.csv with the verdict table.
#
# Usage:
#   ./smoke_test_dataset.sh                  # every apk present in the dataset
#   ./smoke_test_dataset.sh amaze geohash    # filter by substring
#   EVENTS=80 ./smoke_test_dataset.sh        # exercise the app harder
#
set -uo pipefail

SDK="${SDK:-/home/ubuntu/android-sdk}"
# Default to the clone this script lives in. Hardcoding an absolute path meant
# every checkout that was not at /home/ubuntu/TimeMachine silently looked for its
# apps in a directory that did not exist.
PROJECT="${PROJECT:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)}"
APPS_DIR="$PROJECT/instrumented_apps"
OUT="${OUT:-$PROJECT/results/smoke}"
JACOCO="${JACOCO:-$PROJECT/fuzzingandroid/libs/jacococli-0.8.13.jar}"
AVD="${AVD:-avd0}"
SERIAL="${SERIAL:-emulator-5554}"
EVENTS="${EVENTS:-40}"            # monkey events to exercise the app
SETTLE="${SETTLE:-8}"             # seconds to let the app initialise
INSTALL_TIMEOUT="${INSTALL_TIMEOUT:-600}"   # per install attempt, seconds
BROADCAST_TIMEOUT="${BROADCAST_TIMEOUT:-900}" # COLLECT_COVERAGE dump, seconds
EMU_ARGS="${EMU_ARGS:--accel off -gpu guest}"
# build-tools 28.0.3 is not the only version that can dump a badging record, and
# pinning it made the script unusable on an SDK that has any other one.
AAPT="${AAPT:-$SDK/build-tools/28.0.3/aapt}"
if [ ! -x "$AAPT" ]; then
  AAPT=$(ls -1 "$SDK"/build-tools/*/aapt 2>/dev/null | sort -V | tail -1)
  [ -n "${AAPT:-}" ] || AAPT=$(command -v aapt || true)
fi
ADB="${ADB:-$SDK/platform-tools/adb}"
[ -x "$ADB" ] || ADB=$(command -v adb || true)
export ANDROID_SDK_ROOT="$SDK" ANDROID_HOME="$SDK"
export PATH="$SDK/platform-tools:$SDK/emulator:$PATH"

log()  { echo "[$(date +%H:%M:%S)] $*"; }
alog() { echo "[$(date +%H:%M:%S)] $*" >> "$APP_LOG"; }

mkdir -p "$OUT"
SUMMARY="$OUT/smoke_summary.csv"
HEADER="app,apk,package,installed,launched,ec_bytes,classes_found,line_coverage_pct,mismatched_classes,html_pages,verdict,seconds"

# RESUME (default on): never delete previous results, and skip apks that already
# have a completed verdict. Failures are retried, since they are usually caused
# by a busy host rather than the app. Set RESUME=0 to re-test everything.
RESUME="${RESUME:-1}"
[ -f "$SUMMARY" ] || echo "$HEADER" > "$SUMMARY"

already_done() {
  local apk="$1"
  [ "$RESUME" = "1" ] || return 1
  [ -f "$SUMMARY" ] || return 1
  # completed = has a verdict that is not a retryable failure
  awk -F, -v want="$apk" '
    $2==want && $11!="" && $11!="INSTALL FAILED" && $11!="NO EC FILE" && $11!="BAD APK" { found=1 }
    END { exit(found?0:1) }
  ' "$SUMMARY"
}

# ------------------------------------------------------------------ emulator
# `pm install` runs dex2oat, which dominates install time under software
# emulation (8m38s measured while the host was busy). For a smoke test we only
# need the app to run and report coverage, not to run fast, so skip AOT
# compilation: installs drop to ~2min. The app runs interpreted; jacoco probes
# are unaffected.
tune_for_speed() {
  timeout 30 $ADB -s "$SERIAL" root >/dev/null 2>&1
  sleep 3
  timeout 30 $ADB -s "$SERIAL" wait-for-device >/dev/null 2>&1
  timeout 30 $ADB -s "$SERIAL" shell setprop pm.dexopt.install verify-none >/dev/null 2>&1
  log "dexopt filter: $(timeout 30 $ADB -s "$SERIAL" shell getprop pm.dexopt.install 2>/dev/null | tr -d '\r')"
}

boot_emulator() {
  if $ADB devices | grep -q "$SERIAL"; then
    log "emulator already running, reusing it"
    tune_for_speed
    return 0
  fi
  log "booting emulator $AVD (software emulation, expect ~5 min) ..."
  nohup "$SDK/emulator/emulator" -avd "$AVD" -port 5554 -no-window -no-audio \
        -no-boot-anim $EMU_ARGS > /tmp/smoke_emulator.log 2>&1 &
  for i in $(seq 1 90); do
    sleep 10
    local bc
    bc=$(timeout 25 $ADB -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
    if [ "$bc" = "1" ]; then
      log "emulator booted after $((i*10))s"
      tune_for_speed
      return 0
    fi
  done
  log "FATAL: emulator failed to boot"
  return 1
}

# ------------------------------------------------------------- dataset discovery
# emits "<app>|<apk filename>" for every apk that physically exists
discover() {
  python3 - "$APPS_DIR" <<'PY'
import json, os, sys
root = sys.argv[1]
for app in sorted(os.listdir(root)):
    cfg = os.path.join(root, app, 'class_files.json')
    if not os.path.isfile(cfg):
        continue
    try:
        entries = json.load(open(cfg))
    except Exception:
        continue
    for apk in sorted(entries):
        if os.path.isfile(os.path.join(root, app, apk)):
            print('%s|%s' % (app, apk))
PY
}

jacoco_args() {
  python3 - "$1" "$2" <<'PY'
import json, os, sys
app_dir, apk = sys.argv[1], sys.argv[2]
info = json.load(open(os.path.join(app_dir, 'class_files.json')))[apk]
args = []
for p in info.get('classfiles', []):
    full = os.path.join(app_dir, p)
    if os.path.isdir(full):
        args += ['--classfiles', full]
for p in info.get('sourcefiles', []):
    full = os.path.join(app_dir, p)
    if os.path.isdir(full):
        args += ['--sourcefiles', full]
print('\0'.join(args), end='')
PY
}

count_classes() {
  python3 - "$1" "$2" <<'PY'
import json, os, sys
app_dir, apk = sys.argv[1], sys.argv[2]
info = json.load(open(os.path.join(app_dir, 'class_files.json')))[apk]
n = 0
for p in info.get('classfiles', []):
    full = os.path.join(app_dir, p)
    for _, _, fs in os.walk(full):
        n += len([f for f in fs if f.endswith('.class')])
print(n)
PY
}

# DRY_RUN checks everything that does not need a device: which apps are
# discovered, that each APK is a real file rather than an LFS pointer, that the
# declared class and source directories resolve, and what jacococli would be
# called with. Worth running before committing an emulator host to a full pass,
# and it is the only part of this script that can run on a host with no emulator
# available at all (there is no linux-aarch64 emulator build, for instance).
if [ "${DRY_RUN:-0}" = "1" ]; then
  echo "DRY RUN - no emulator, no install, no coverage"
  echo "PROJECT=$PROJECT"
  echo "APPS_DIR=$APPS_DIR"
  echo "AAPT=${AAPT:-<none>}"
  echo
  n=0; bad=0; toonew=0
  while IFS='|' read -r app apk; do
    [ -n "$app" ] || continue
    if [ ${#} -gt 0 ]; then
      match=0
      for f in "$@"; do
        case "${app,,}/${apk,,}" in *"${f,,}"*) match=1 ;; esac
      done
      [ "$match" = 1 ] || continue
    fi
    n=$((n+1))
    app_dir="$APPS_DIR/$app"
    apk_path="$app_dir/$apk"
    size=$(stat -c %s "$apk_path" 2>/dev/null || echo 0)
    classes=$(count_classes "$app_dir" "$apk")
    mapfile -d '' -t JARGS < <(jacoco_args "$app_dir" "$apk")
    ncls=0; nsrc=0
    for ((i=0; i<${#JARGS[@]}; i+=2)); do
      case "${JARGS[i]}" in
        --classfiles) ncls=$((ncls+1)) ;;
        --sourcefiles) nsrc=$((nsrc+1)) ;;
      esac
    done
    minsdk=$(timeout 120 "$AAPT" dump badging "$apk_path" 2>/dev/null \
             | sed -n "s/.*sdkVersion:'\([0-9]*\)'.*/\1/p" | head -1)
    status=ok
    [ "$size" -lt 1024 ] && status="APK IS AN LFS POINTER"
    [ "$classes" -gt 0 ] || status="NO CLASS FILES"
    [ "$ncls" -gt 0 ] || status="NO CLASSFILES DIRS RESOLVE"
    [ "$status" = ok ] || bad=$((bad+1))
    if [ -n "$minsdk" ] && [ "$minsdk" -gt "${API:-25}" ]; then
      status="$status (minSdk $minsdk > API ${API:-25}: will not install)"
      toonew=$((toonew+1))
    fi
    printf '%-26s %-44s %9s B  minSdk=%-4s classes=%-6s dirs=%-3s src=%-3s %s\n' \
      "$app" "$apk" "$size" "${minsdk:-?}" "$classes" "$ncls" "$nsrc" "$status"
  done < <(discover)
  echo
  echo "$n apk entries discovered, $bad with dataset problems"
  echo "$toonew need an emulator newer than API ${API:-25}"
  echo "results would be written to: $OUT/<apk-slug>/"
  exit $(( bad > 0 ? 1 : 0 ))
fi

boot_emulator || exit 1

FILTER=("$@")
rows=0

# Read the whole dataset list up front. It must NOT be streamed into the loop:
# `adb shell` reads stdin and would swallow the remaining entries, so the loop
# would silently process only the first app.
mapfile -t ENTRIES < <(discover)
skipped=0
done_count=$(( $(wc -l < "$SUMMARY") - 1 ))
log "discovered ${#ENTRIES[@]} apks in the dataset; $done_count already recorded (RESUME=$RESUME)"

for entry in "${ENTRIES[@]}"; do
  app="${entry%%|*}"
  apk="${entry##*|}"
  [ -z "${app:-}" ] && continue

  if [ ${#FILTER[@]} -gt 0 ]; then
    keep=0
    for want in "${FILTER[@]}"; do
      shopt -s nocasematch; [[ "$app" == *"$want"* ]] && keep=1; shopt -u nocasematch
    done
    [ $keep -eq 1 ] || continue
  fi

  if already_done "$apk"; then
    prev=$(awk -F, -v want="$apk" '$2==want {v=$11; p=$8} END {print v" "p}' "$SUMMARY")
    log "SKIP (already validated): $apk  [$prev]"
    skipped=$((skipped+1))
    continue
  fi

  app_dir="$APPS_DIR/$app"
  apk_path="$app_dir/$apk"
  slug=$(echo "${apk%.apk}" | tr -c 'A-Za-z0-9._-' '_')
  dest="$OUT/$slug"
  mkdir -p "$dest"
  APP_LOG="$dest/smoke.log"
  : > "$APP_LOG"

  echo
  echo "----------------------------------------------------------------"
  log "SMOKE: $app / $apk"
  t0=$(date +%s)

  pkg=$(timeout 120 "$AAPT" dump badging "$apk_path" 2>/dev/null \
        | awk -F"'" '/^package: name=/{print $2; exit}')
  classes=$(count_classes "$app_dir" "$apk")
  alog "package=$pkg classes_declared_found=$classes"

  if [ -z "$pkg" ]; then
    log "  FAIL: cannot read package name from apk"
    echo "$app,$apk,,no,no,0,$classes,,,0,BAD APK,$(( $(date +%s)-t0 ))" >> "$SUMMARY"
    continue
  fi
  log "  package: $pkg   class files found: $classes"

  # ---- install -------------------------------------------------------------
  # Installs of the larger rebuilt apks (Wikipedia, FirefoxLite, nextcloud) can
  # take several minutes under software emulation, and the emulator's package
  # manager occasionally wedges. Generous timeout plus one retry after an adb
  # reconnect, which recovers the common case.
  installed=no
  timeout 90 $ADB -s "$SERIAL" uninstall "$pkg" >>"$APP_LOG" 2>&1
  for attempt in 1 2; do
    alog "install attempt $attempt"
    if timeout "$INSTALL_TIMEOUT" $ADB -s "$SERIAL" install -g "$apk_path" >>"$APP_LOG" 2>&1; then
      installed=yes
      log "  installed (attempt $attempt)"
      break
    fi
    log "  install attempt $attempt failed/timed out, reconnecting adb ..."
    $ADB reconnect >>"$APP_LOG" 2>&1
    sleep 10
    timeout 60 $ADB -s "$SERIAL" wait-for-device >>"$APP_LOG" 2>&1
    timeout 90 $ADB -s "$SERIAL" uninstall "$pkg" >>"$APP_LOG" 2>&1
  done
  if [ "$installed" != yes ]; then
    log "  FAIL: install failed (see $APP_LOG)"
    echo "$app,$apk,$pkg,no,no,0,$classes,,,0,INSTALL FAILED,$(( $(date +%s)-t0 ))" >> "$SUMMARY"
    continue
  fi

  # ---- launch + exercise ---------------------------------------------------
  out=$(timeout 180 $ADB -s "$SERIAL" shell monkey -p "$pkg" 1 2>&1 | tr -d '\r')
  alog "launch: $out"
  if echo "$out" | grep -q 'Events injected'; then
    launched=yes; log "  launched"
  else
    launched=no;  log "  WARN: launch may have failed"
  fi
  sleep "$SETTLE"
  timeout 300 $ADB -s "$SERIAL" shell monkey -p "$pkg" --throttle 200 \
      --ignore-crashes --ignore-timeouts --ignore-security-exceptions \
      "$EVENTS" >>"$APP_LOG" 2>&1
  log "  exercised with $EVENTS events"

  # ---- collect coverage ----------------------------------------------------
  # `am broadcast` blocks until the receiver finishes writing coverage.ec. On a
  # loaded host that can take many minutes; if the timeout fires first the
  # receiver is killed mid-dump and no .ec is ever produced. Two attempts, and
  # we confirm "Broadcast completed" rather than assuming success.
  rm -f "$dest/coverage.ec"
  ec_bytes=0
  for battempt in 1 2; do
    # The Jacoco receiver can only dump from a LIVE process: once monkey exits
    # the app is often gone, the broadcast then returns "completed" without any
    # receiver running, and no .ec is written. Relaunch and confirm the process
    # exists before each broadcast.
    timeout 180 $ADB -s "$SERIAL" shell monkey -p "$pkg" 1 >>"$APP_LOG" 2>&1
    sleep 12
    alive=$(timeout 60 $ADB -s "$SERIAL" shell ps 2>/dev/null | grep -c "$pkg")
    alog "process alive before broadcast: $alive"
    if [ "${alive:-0}" -eq 0 ]; then
      log "  app not running, relaunching once more"
      timeout 180 $ADB -s "$SERIAL" shell monkey -p "$pkg" 1 >>"$APP_LOG" 2>&1
      sleep 15
    fi
    alog "broadcast attempt $battempt (timeout ${BROADCAST_TIMEOUT}s)"
    bout=$(timeout "$BROADCAST_TIMEOUT" $ADB -s "$SERIAL" shell am broadcast \
             -a edu.gatech.m3.emma.COLLECT_COVERAGE 2>&1 | tr -d '\r')
    alog "broadcast said: $bout"
    if ! echo "$bout" | grep -q 'Broadcast completed'; then
      log "  broadcast attempt $battempt did not complete (receiver killed by timeout?)"
      sleep 15
      continue
    fi
    timeout 90 $ADB -s "$SERIAL" shell "mv /data/data/$pkg/files/coverage.ec /sdcard/smoke.ec" >>"$APP_LOG" 2>&1
    timeout 180 $ADB -s "$SERIAL" pull /sdcard/smoke.ec "$dest/coverage.ec" >>"$APP_LOG" 2>&1
    timeout 30 $ADB -s "$SERIAL" shell rm -f /sdcard/smoke.ec >>"$APP_LOG" 2>&1
    [ -f "$dest/coverage.ec" ] && ec_bytes=$(stat -c%s "$dest/coverage.ec")
    [ "$ec_bytes" -gt 0 ] && break
    log "  no .ec yet after attempt $battempt, retrying broadcast"
    sleep 15
  done
  log "  coverage.ec: $ec_bytes bytes"

  # ---- report -------------------------------------------------------------
  pct=""; mismatch=""; pages=0; verdict=""
  if [ "$ec_bytes" -gt 0 ] && [ "$classes" -gt 0 ]; then
    mapfile -d '' -t JARGS < <(jacoco_args "$app_dir" "$apk")
    rep=$(java -jar "$JACOCO" report "$dest/coverage.ec" "${JARGS[@]}" \
            --xml "$dest/coverage.xml" --html "$dest/coverage_html" \
            --name "SMOKE $app" 2>&1)
    echo "$rep" >> "$APP_LOG"
    mismatch=$(echo "$rep" | grep -c 'does not match')
    # Count Kotlin pages too. Matching only *.java.html reported 2 pages for
    # StreetComplete, which actually has 964 *.kt.html, and likewise understated
    # every other Kotlin app. The reports were correct; the column was not.
    pages=$(find "$dest/coverage_html" \( -name '*.java.html' -o -name '*.kt.html' \) \
              2>/dev/null | wc -l)
    pct=$(python3 - "$dest/coverage.xml" <<'PY'
import sys, xml.dom.minidom as m
try:
    d = m.parse(sys.argv[1])
    for c in d.getElementsByTagName('counter'):
        if c.parentNode.tagName == 'report' and c.getAttribute('type') == 'LINE':
            mi, co = int(c.getAttribute('missed')), int(c.getAttribute('covered'))
            print('%.2f' % (100.0*co/(mi+co)))
            break
except Exception:
    print('')
PY
)
    log "  coverage: ${pct:-?}%   mismatched classes: $mismatch   html pages: $pages"
    if [ -z "$pct" ] || [ "$pct" = "0.00" ]; then
      verdict="NO COVERAGE"
    elif [ "$mismatch" -gt 0 ]; then
      verdict="MISMATCH ($mismatch classes)"
    else
      verdict="PASS"
    fi
  elif [ "$ec_bytes" -gt 0 ]; then
    verdict="AGENT OK / NEEDS BUILD"
    # No class files, so jacoco cannot emit XML/HTML (a .ec has no line info).
    # probe_report.py still extracts per-class probe hit counts from the .ec,
    # so every app gets a quantified coverage artifact.
    log "  no class files -> probe-level report only (see probe_report.txt)"
  else
    verdict="NO EC FILE"
    log "  FAIL: no coverage data produced"
  fi

  log "  VERDICT: $verdict"
  echo "$app,$apk,$pkg,$installed,$launched,$ec_bytes,$classes,${pct},${mismatch},$pages,$verdict,$(( $(date +%s)-t0 ))" >> "$SUMMARY"
  rows=$((rows+1))

  timeout 60 $ADB -s "$SERIAL" uninstall "$pkg" >>"$APP_LOG" 2>&1
done

# Probe-level report for every app, including those with no class files.
echo
log "generating probe-level reports from collected .ec files ..."
python3 "$PROJECT/probe_report.py" "$OUT" 2>&1 | sed 's/^/  /'

echo
echo "================================================================"
echo " SMOKE TEST SUMMARY   ($rows tested this pass, $skipped skipped as already done)"
echo "================================================================"
python3 - "$SUMMARY" <<'PY'
import collections, csv, sys
# Retried apks append a second row; keep only the most recent per apk.
uniq = collections.OrderedDict()
for r in csv.DictReader(open(sys.argv[1])):
    uniq[r['apk']] = r
rows = list(uniq.values())
w = '%-26s %-34s %-9s %-8s %-6s %s'
print(w % ('APP', 'APK', 'EC BYTES', 'COVER%', 'MISM', 'VERDICT'))
print('-'*112)
for r in rows:
    print(w % (r['app'][:26], r['apk'][:34], r['ec_bytes'],
               r['line_coverage_pct'] or '-', r['mismatched_classes'] or '-', r['verdict']))
ok = [r for r in rows if r['verdict'] == 'PASS']
print()
print('USABLE FOR COVERAGE (%d):' % len(ok))
for r in ok:
    print('   %s / %s  -> %s%%' % (r['app'], r['apk'], r['line_coverage_pct']))
bad = [r for r in rows if r['verdict'] != 'PASS']
if bad:
    print()
    print('NOT USABLE (%d) - candidates to discard:' % len(bad))
    for r in bad:
        print('   %-26s %-34s %s' % (r['app'], r['apk'][:34], r['verdict']))
PY
echo
echo "Artifacts: $OUT/<apk>/{coverage.ec,coverage.xml,coverage_html/,smoke.log}"
echo "Summary:   $SUMMARY"
echo
echo "Emulator left running for reuse. Stop it with:  adb -s $SERIAL emu kill"
