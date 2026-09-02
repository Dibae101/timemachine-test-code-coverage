#!/usr/bin/env bash
#
# package_datasets.sh - bundle each dataset into a self-contained zip.
#
# One zip per app, holding everything a coverage run needs: the instrumented APK,
# the app's source tree, the compiled classes those sources produced, and the
# class_files.json that maps the APK to both. Plus a parent zip of all of them and
# a SHA256SUMS file.
#
# The zips are deliberately NOT committed. They are ~2.5 GB in total and individual
# ones reach 210 MB, over GitHub's 100 MB per-file limit, and the repository already
# contains every byte of the same content - a plain `git clone` is the complete
# bundle. Publish these as GitHub **release assets** instead, which allow up to
# 2 GB each:
#
#   gh release create v1.0 dist/*.zip --title "59 JaCoCo-ready Android datasets"
#
# Usage:
#   ./package_datasets.sh                # every dataset in MANIFEST.tsv
#   ./package_datasets.sh Markor Twire   # by name
#   ./package_datasets.sh --no-parent    # skip the combined archive
#
set -uo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)" || exit 1

command -v zip >/dev/null || { echo "zip is not installed: sudo apt-get install zip"; exit 1; }

APPS=instrumented_apps
DIST="${DIST:-dist}"
MANIFEST="$APPS/MANIFEST.tsv"
[ -f "$MANIFEST" ] || { echo "no $MANIFEST; run dataset_builder/make_manifest.py"; exit 1; }

PARENT=1
FILTERS=()
for a in "$@"; do
  case "$a" in
    --no-parent) PARENT=0 ;;
    *) FILTERS+=("$a") ;;
  esac
done

mkdir -p "$DIST"
mapfile -t APPLIST < <(awk -F'\t' 'NR>1 && $11=="yes" {print $1}' "$MANIFEST" | sort -u)
echo "${#APPLIST[@]} datasets in the manifest"

built=0
for app in "${APPLIST[@]}"; do
  if [ "${#FILTERS[@]}" -gt 0 ]; then
    keep=0
    for f in "${FILTERS[@]}"; do case "${app,,}" in *"${f,,}"*) keep=1 ;; esac; done
    [ "$keep" = 1 ] || continue
  fi

  out="$DIST/$app.zip"
  rm -f "$out"

  # A short note inside each zip, so a downloaded archive explains itself.
  note="$APPS/$app/DATASET.txt"
  {
    echo "$app - JaCoCo-ready Android coverage dataset"
    echo
    awk -F'\t' -v a="$app" 'NR==1{for(i=1;i<=NF;i++)h[i]=$i} $1==a{for(i=1;i<=NF;i++) printf "%-14s %s\n", h[i]":", $i}' "$MANIFEST"
    echo
    echo "Layout"
    echo "  <apk>                 the instrumented APK, built from the source in this archive"
    echo "  <app>-#<id>/          source tree plus the compiled classes that APK contains"
    echo "  class_files.json      maps the APK to its classfiles and sourcefiles directories"
    echo
    echo "Report coverage with:"
    echo "  java -jar jacococli.jar report <coverage.ec> \\"
    echo "    --classfiles <each classfiles path> --sourcefiles <each sourcefiles path> \\"
    echo "    --html out_html --xml out.xml"
    echo
    echo "Full instructions: https://github.com/Dibae101/timemachine-test-code-coverage"
  } > "$note"

  # upstream/ is a provenance submodule, empty in a plain clone and not needed
  ( cd "$APPS" && zip -q -r -9 "../$out" "$app" -x "$app/upstream/*" )
  rm -f "$note"

  sz=$(stat -c %s "$out")
  awk -v n="$app" -v s="$sz" 'BEGIN{printf "  %-26s %7.1f MB\n", n, s/1e6}'
  built=$((built+1))
done

if [ "$PARENT" = 1 ] && [ "${#FILTERS[@]}" -eq 0 ]; then
  echo "building the combined archive ..."
  rm -f "$DIST/all-datasets.zip"
  # store, not deflate: the members are already compressed
  ( cd "$DIST" && zip -q -0 all-datasets.zip ./*.zip -x all-datasets.zip )
  awk -v s="$(stat -c %s "$DIST/all-datasets.zip")" \
      'BEGIN{printf "  %-26s %7.1f MB\n", "all-datasets.zip", s/1e6}'
fi

( cd "$DIST" && sha256sum ./*.zip > SHA256SUMS )
total=$(du -sb "$DIST" | cut -f1)
echo
awk -v b="$built" -v t="$total" 'BEGIN{printf "%d zips in '"$DIST"'/, %.2f GB total\n", b, t/1e9}'
echo "checksums: $DIST/SHA256SUMS"
echo
echo "These are not committed. Publish as release assets, which allow 2 GB each:"
echo "  gh release create v1.0 $DIST/*.zip --title '59 JaCoCo-ready Android datasets'"
