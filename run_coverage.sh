#!/bin/bash
#
# run_coverage.sh - run TimeMachine against dataset apps with a hard wall-clock
#                   cap, then generate the Jacoco XML + HTML coverage reports.
#
# Why a wrapper is needed:
#   TimeMachine's --time flag is not a wall-clock limit. The engine only checks
#   its deadline *between* fuzz cycles, and one cycle takes 18-28 min on a host
#   without KVM. So --time 1m and --time 15m both run ~35 min. This script sets
#   a small --time (so the engine's own writer threads flush early) and enforces
#   the real limit with `timeout`.
#
# Usage:
#   ./run_coverage.sh                     # all ready apps, 15 min each
#   ./run_coverage.sh amaze geohash       # only apps matching those substrings
#   CAP_MINUTES=20 ./run_coverage.sh      # different cap
#
set -uo pipefail

# ---------------------------------------------------------------- configuration
CAP_MINUTES="${CAP_MINUTES:-15}"        # hard wall-clock cap per app
FUZZ_BUDGET="${FUZZ_BUDGET:-1m}"        # TimeMachine --time (see note above)
AVD="${AVD:-avd0}"
SDK="${SDK:-/home/ubuntu/android-sdk}"
PROJECT="${PROJECT:-/home/ubuntu/TimeMachine}"
RESULTS="${RESULTS:-$PROJECT/results}"
APPS_DIR="$PROJECT/instrumented_apps"
JACOCO="$PROJECT/fuzzingandroid/libs/jacococli.jar"
SNAPSHOT_DIR="$HOME/.android/avd/${AVD}.avd/snapshots"

# Dataset: "<app-dir>|<apk filename>"  (apk must have an entry in class_files.json)
DATASET=(
  "AmazeFileManager|AmazeFileManager-3.4.2-#1837-rebuilt.apk"
  "geohashdroid|geohashdroid-0.9.4-#73-rebuilt.apk"
  "MaterialFBook|MaterialFBook4.0.2-debug-#224.apk"
)

# ---------------------------------------------------------------- environment
export ANDROID_SDK_ROOT="$SDK"
export ANDROID_HOME="$SDK"
export PATH="$SDK/platform-tools:$SDK/emulator:$SDK/build-tools/25.0.3:$SDK/cmdline-tools/latest/bin:$PATH"
export EMU_EXTRA_ARGS="${EMU_EXTRA_ARGS:--accel off -gpu guest}"  # no KVM on this host
export BOOT_WAIT_TRIES="${BOOT_WAIT_TRIES:-180}"                  # slow software boot
export TM_SKIP_CACHE_BOOT="${TM_SKIP_CACHE_BOOT:-1}"              # saves ~4 min/run
export TM_HTML_REPORT="${TM_HTML_REPORT:-0}"                       # HTML built once at end

log() { echo "[$(date +%H:%M:%S)] $*"; }

cleanup_emulator() {
  pkill -f 'main.py --avd'   2>/dev/null
  pkill -f 'executor.py'     2>/dev/null
  pkill -f run_timemachine.sh 2>/dev/null
  sleep 2
  adb -s emulator-5554 emu kill >/dev/null 2>&1
  sleep 5
  pkill -f qemu-system 2>/dev/null
  sleep 2
}

# Free the 1.3 GB-per-snapshot pile; nothing in TimeMachine prunes these.
clear_snapshots() {
  [ -d "$SNAPSHOT_DIR" ] || return 0
  find "$SNAPSHOT_DIR" -mindepth 1 -maxdepth 1 -not -name default_boot \
       -exec rm -rf {} + 2>/dev/null
}

# Build the --classfiles/--sourcefiles argument list straight from class_files.json
jacoco_args() {
  local app_dir="$1" apk="$2"
  python3 - "$app_dir" "$apk" <<'PY'
import json, os, sys
app_dir, apk = sys.argv[1], sys.argv[2]
cfg = json.load(open(os.path.join(app_dir, 'class_files.json')))
info = cfg[apk]
args = []
for p in info.get('classfiles', []):
    args += ['--classfiles', os.path.join(app_dir, p)]
for p in info.get('sourcefiles', []):
    args += ['--sourcefiles', os.path.join(app_dir, p)]
print('\0'.join(args), end='')
PY
}

# ---------------------------------------------------------------- main loop
mkdir -p "$RESULTS"
SELECTED=("$@")
summary=()

for entry in "${DATASET[@]}"; do
  app="${entry%%|*}"
  apk="${entry##*|}"
  app_dir="$APPS_DIR/$app"
  apk_path="$app_dir/$apk"

  # optional filtering by substring
  if [ ${#SELECTED[@]} -gt 0 ]; then
    match=0
    for want in "${SELECTED[@]}"; do
      shopt -s nocasematch
      [[ "$app" == *"$want"* ]] && match=1
      shopt -u nocasematch
    done
    [ $match -eq 1 ] || continue
  fi

  echo
  echo "================================================================"
  log "APP: $app  (cap ${CAP_MINUTES}m)"
  echo "================================================================"

  if [ ! -f "$apk_path" ]; then
    log "SKIP: apk not found: $apk_path"
    summary+=("$app|SKIPPED|apk missing")
    continue
  fi

  cleanup_emulator
  clear_snapshots

  before=$(ls -1d "$RESULTS"/*.timemachine.result.* 2>/dev/null | wc -l)
  start=$(date +%s)

  # --- the run, hard-capped -------------------------------------------------
  timeout --foreground -k 15 "${CAP_MINUTES}m" \
    python2.7 main.py --avd "$AVD" --apk "$apk_path" \
      --time "$FUZZ_BUDGET" -o "$RESULTS" > "/tmp/tm_${app}.log" 2>&1
  rc=$?
  elapsed=$(( $(date +%s) - start ))
  [ $rc -eq 124 ] && log "hit the ${CAP_MINUTES}m cap (expected)" \
                  || log "exited on its own (rc=$rc)"

  cleanup_emulator

  # --- locate the run directory this invocation created ---------------------
  run_dir=$(ls -1dt "$RESULTS"/"$apk".timemachine.result.* 2>/dev/null | head -1)
  if [ -z "$run_dir" ] || [ ! -d "$run_dir/ec_files" ]; then
    log "FAIL: no run directory produced"
    summary+=("$app|FAILED|no output after ${elapsed}s")
    continue
  fi

  ec_count=$(ls -1 "$run_dir/ec_files"/*.ec 2>/dev/null | wc -l)
  if [ "$ec_count" -eq 0 ]; then
    log "FAIL: no coverage (.ec) collected in ${elapsed}s - cap too short"
    summary+=("$app|NO COVERAGE|${elapsed}s, 0 ec files")
    continue
  fi

  # --- final report: XML + annotated HTML ----------------------------------
  log "generating report from $ec_count ec files ..."
  mapfile -d '' -t JARGS < <(jacoco_args "$app_dir" "$apk")
  mismatch=$(java -jar "$JACOCO" report "$run_dir/ec_files"/*.ec "${JARGS[@]}" \
      --xml "$run_dir/coverage.xml" \
      --html "$run_dir/coverage_html" \
      --name "TimeMachine coverage - $app" 2>&1 | grep -c 'does not match')

  pct=$(python3 - "$run_dir/coverage.xml" <<'PY'
import sys, xml.dom.minidom as m
try:
    d = m.parse(sys.argv[1])
    for c in d.getElementsByTagName('counter'):
        if c.parentNode.tagName == 'report' and c.getAttribute('type') == 'LINE':
            mi, co = int(c.getAttribute('missed')), int(c.getAttribute('covered'))
            print('%.2f%% (%d/%d lines)' % (100.0*co/(mi+co), co, mi+co))
except Exception:
    print('unreadable')
PY
)

  pages=$(find "$run_dir/coverage_html" -name '*.java.html' 2>/dev/null | wc -l)
  log "coverage: $pct   html pages: $pages   mismatches: $mismatch"
  [ "$mismatch" -gt 0 ] && log "WARNING: $mismatch classes did not match - class files are from a different build than the apk"

  # short browsable symlink
  ln -sfn "$(basename "$run_dir")/coverage_html" "$RESULTS/report-$app"

  summary+=("$app|$pct|${elapsed}s, $ec_count ec, $pages pages, mismatch=$mismatch")
done

clear_snapshots

# ---------------------------------------------------------------- summary
echo
echo "================================================================"
echo " SUMMARY"
echo "================================================================"
printf '%-22s %-26s %s\n' "APP" "COVERAGE" "DETAIL"
for row in "${summary[@]}"; do
  IFS='|' read -r a b c <<< "$row"
  printf '%-22s %-26s %s\n' "$a" "$b" "$c"
done
echo
echo "Reports:  $RESULTS/report-<app>/index.html"
echo "Serve:    python3 -m http.server 8000 --bind 127.0.0.1  (from $RESULTS)"
