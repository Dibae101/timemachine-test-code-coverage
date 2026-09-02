#!/usr/bin/env bash
# publish_batches.sh - stage, commit and push the dataset a few apps at a time.
#
# One push carrying all 61 apps is ~2 GB, and the APKs alone are over the 1 GB
# free GitHub LFS allowance. Batching means a rejected push costs one batch
# instead of the whole set, and the first batch reveals a quota problem early.
#
# Stops at the first failed push so the cause can be looked at rather than
# repeated 10 times.
#
# Usage: ./publish_batches.sh [apps-per-batch]

set -u
cd "$(dirname "$0")/.." || exit 1

SIZE="${1:-6}"
MANIFEST=instrumented_apps/MANIFEST.tsv
[ -f "$MANIFEST" ] || { echo "no MANIFEST.tsv; run make_manifest.py"; exit 1; }

mapfile -t APPS < <(awk -F'\t' 'NR>1 && $9=="yes" {print $1}' "$MANIFEST" | sort -u)
echo "${#APPS[@]} valid apps, batches of $SIZE"

total=${#APPS[@]}
i=0
batch=0
while [ "$i" -lt "$total" ]; do
    batch=$((batch + 1))
    slice=("${APPS[@]:i:SIZE}")
    i=$((i + SIZE))

    echo
    echo "=== batch $batch: ${slice[*]} ==="

    # Kiwix's build installs a pre-commit hook into this repository, which then
    # fails every commit with "./gradlew: not found".
    rm -f .git/hooks/pre-commit

    python3 dataset_builder/publish.py --apps "${slice[@]}" >/dev/null || {
        echo "publish.py failed on batch $batch"; exit 1; }

    if git diff --cached --quiet; then
        echo "nothing new to commit"
        continue
    fi

    files=$(git diff --cached --name-only | wc -l)
    git commit -q -m "Add coverage datasets: ${slice[*]}" \
        -m "$files files. Each app carries its instrumented APK, the compiled
classes from that same build, the sources, and class_files.json mapping the APK to
both. All entries validate in MANIFEST.tsv and produce a JaCoCo report under
results/report-validation/." || { echo "commit failed on batch $batch"; exit 1; }

    echo "pushing batch $batch ($files files)..."
    if ! git push origin main; then
        echo
        echo "PUSH FAILED on batch $batch. The commit is local; nothing is lost."
        echo "If this is an LFS quota rejection, the options are to raise the"
        echo "quota or to stop tracking APKs and publish them against the"
        echo "SHA-256 sums already in MANIFEST.tsv."
        exit 1
    fi
done

echo
echo "all batches pushed"
