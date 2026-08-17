#!/bin/bash
#
# supervise.sh - run the dataset builder repeatedly until enough subjects work.
#
# Each pass retries every subject that is not yet usable. That matters because a
# single fix to repair-repos.init.gradle (a repository, a disabled quality gate)
# typically unblocks several subjects at once, and a later pass picks them all
# up without any manual bookkeeping.
#
# After every pass it prints the failure tally from diagnose.py, so the morning
# review has the causes grouped rather than 40 separate logs.
#
# Usage:
#   ./supervise.sh                 # up to 4 passes, stop at 40 usable
#   PASSES=6 TARGET=44 ./supervise.sh
#
set -uo pipefail

cd "$(dirname "$0")"

PASSES="${PASSES:-4}"
TARGET="${TARGET:-40}"
MIN_FREE_GB="${MIN_FREE_GB:-6}"

usable() {
  python3 - <<'PY'
import csv, os
p = 'status.csv'
if not os.path.isfile(p):
    print(0); raise SystemExit
rows = list(csv.DictReader(open(p)))
print(sum(1 for r in rows if r['state'] in ('ok', 'apk_only')))
PY
}

free_gb() { df --output=avail -BG /home/ubuntu | tail -1 | tr -dc '0-9'; }

for pass in $(seq 1 "$PASSES"); do
  echo
  echo "================================================================"
  echo " PASS $pass/$PASSES   usable so far: $(usable)/$TARGET   free: $(free_gb) GB"
  echo "================================================================"

  if [ "$(free_gb)" -lt "$MIN_FREE_GB" ]; then
    echo "STOP: only $(free_gb) GB free, need $MIN_FREE_GB"
    break
  fi

  python3 -u build_dataset.py                                  2>&1 | tail -60
  python3 -u build_dataset.py --subjects modern_subjects.json   2>&1 | tail -80

  echo
  echo "---- diagnosis after pass $pass ----"
  python3 diagnose.py 2>&1 | head -70

  n=$(usable)
  echo "usable after pass $pass: $n"
  if [ "$n" -ge "$TARGET" ]; then
    echo "TARGET REACHED ($n >= $TARGET)"
    break
  fi
done

echo
echo "================================================================"
echo " FINAL"
echo "================================================================"
python3 diagnose.py
