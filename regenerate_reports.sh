#!/usr/bin/env bash
#
# regenerate_reports.sh - rebuild coverage.xml and coverage_html/ from committed
# execution data.
#
# A results directory holds two kinds of file. coverage.ec is measured on a
# device and cannot be reproduced; the XML and HTML are derived from it plus the
# dataset's class files, and for 61 apps they come to 628 MB. Only the .ec is
# committed, and this script rebuilds the rest.
#
# Usage:
#   ./regenerate_reports.sh                                  # results/smoke-redroid-api31
#   ./regenerate_reports.sh results/smoke                     # any results tree
#   ./regenerate_reports.sh results/smoke Kiwix Twire         # filter by substring
#
set -uo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)" || exit 1

OUT="${1:-results/smoke-redroid-api31}"
shift || true
JACOCO="${JACOCO:-fuzzingandroid/libs/jacococli-0.8.13.jar}"
APPS_DIR=instrumented_apps

[ -d "$OUT" ] || { echo "no such results directory: $OUT"; exit 1; }
SUMMARY="$OUT/smoke_summary.csv"
[ -f "$SUMMARY" ] || { echo "no smoke_summary.csv in $OUT"; exit 1; }

# app,apk pairs from the summary, most recent row per apk
mapfile -t PAIRS < <(python3 - "$SUMMARY" <<'PY'
import collections, csv, sys
uniq = collections.OrderedDict()
for r in csv.DictReader(open(sys.argv[1])):
    uniq[r["apk"]] = r["app"]
for apk, app in uniq.items():
    print("%s\t%s" % (app, apk))
PY
)

built=0; skipped=0; failed=0
for pair in "${PAIRS[@]}"; do
  app="${pair%%$'\t'*}"; apk="${pair##*$'\t'}"
  if [ $# -gt 0 ]; then
    match=0
    for f in "$@"; do case "${app,,}" in *"${f,,}"*) match=1 ;; esac; done
    [ "$match" = 1 ] || continue
  fi

  slug=$(echo "${apk%.apk}" | tr -c 'A-Za-z0-9._-' '_')
  dest="$OUT/$slug"
  ec="$dest/coverage.ec"
  if [ ! -s "$ec" ]; then
    skipped=$((skipped+1)); continue
  fi
  # An unfetched Git LFS pointer is a ~130 byte text file, not execution data.
  if [ "$(stat -c %s "$ec")" -lt 1024 ] && head -c 40 "$ec" | grep -q 'git-lfs'; then
    echo "  $app: coverage.ec is an unfetched LFS pointer; run 'git lfs pull'"
    failed=$((failed+1)); continue
  fi

  mapfile -d '' -t JARGS < <(python3 - "$APPS_DIR/$app" "$apk" <<'PY'
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
)
  if [ "${#JARGS[@]}" -eq 0 ]; then
    echo "  $app: no class directories resolve; is the dataset checked out?"
    failed=$((failed+1)); continue
  fi

  rm -rf "$dest/coverage_html"
  if java -jar "$JACOCO" report "$ec" "${JARGS[@]}" \
       --xml "$dest/coverage.xml" --html "$dest/coverage_html" \
       --name "$app $apk" > "$dest/jacococli-output.txt" 2>&1; then
    pages=$(find "$dest/coverage_html" \( -name '*.java.html' -o -name '*.kt.html' \) \
              2>/dev/null | wc -l)
    mism=$(grep -c 'does not match' "$dest/jacococli-output.txt")
    printf '  %-26s pages=%-5s mismatched=%s\n' "$app" "$pages" "$mism"
    built=$((built+1))
  else
    echo "  $app: jacococli failed, see $dest/jacococli-output.txt"
    failed=$((failed+1))
  fi
done

echo
echo "regenerated $built, skipped $skipped (no execution data), failed $failed"
