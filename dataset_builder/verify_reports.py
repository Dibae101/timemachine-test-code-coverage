#!/usr/bin/env python3
"""
verify_reports.py - prove that every dataset entry can produce a JaCoCo report.

What this checks, per APK declared in a class_files.json:

  1. the APK exists and is a real file, not a Git LFS pointer
  2. every declared classfiles/sourcefiles directory resolves
  3. jacococli can analyze all declared classes without aborting
  4. the report contains counted lines, and the HTML carries the per-line
     green/red/yellow markup and one source page per source file

Step 3 is the one that catches the failure this was written for: if a declared
directory holds JaCoCo-instrumented classes, jacococli does not warn and carry
on, it raises "Error while analyzing <class>" and writes nothing.

Coverage data
-------------
The dataset's real .ec files are collected from a device. This host has no
emulator (aarch64, no /dev/kvm), so execution data is synthesized by
tools/SynthExec against the same class files, keyed by the class ids the reporter
computes. That exercises id matching, probe-count agreement, source resolution
and the HTML markup.

It does not measure anything. The percentage a synthetic report shows is a
property of --fraction, not of any test run, and is written to a separate
directory from the device-collected smoke results so the two cannot be confused.

Usage
    python3 verify_reports.py                # every app
    python3 verify_reports.py Fossify Markor # substring filters
"""

import csv
import glob
import json
import os
import re
import shutil
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(HERE)
APPS = os.path.join(PROJECT, "instrumented_apps")
OUT = os.path.join(PROJECT, "results", "report-validation")
TOOLS = os.path.join(HERE, "tools")
JACOCOCLI = os.path.join(PROJECT, "fuzzingandroid", "libs", "jacococli-0.8.13.jar")
JAVA = os.environ.get("JAVA", "/usr/lib/jvm/java-17-openjdk-arm64/bin/java")
FRACTION = os.environ.get("FRACTION", "0.55")

# Same columns as results/smoke_test/smoke_summary.csv, in the same order, so both
# result sets can be read by one consumer. Two columns are added rather than
# reused:
#
#   coverage_source  "synthetic" here, "device" in smoke_summary.csv. This is the
#                    column that stops the two being conflated.
#   line_total       smoke does not record its denominator, which is what made it
#                    impossible to see that Binary-Eye's 47% was 25 lines of 53.
#
# installed/launched are recorded as "n/a": nothing is installed or launched here.
FIELDS = ["app", "apk", "package", "installed", "launched", "ec_bytes",
          "classes_found", "line_coverage_pct", "mismatched_classes",
          "html_pages", "verdict", "seconds",
          "coverage_source", "line_total", "line_covered", "class_dirs",
          "source_dirs", "exec_classes", "highlight_spans", "analyze_error",
          "detail"]


def smoke_slug(apk_name):
    """The directory name smoke_test_dataset.sh would use for this APK.

    It is `echo "${apk%.apk}" | tr -c 'A-Za-z0-9._-' '_'`, and the trailing
    underscore in the existing directories is real: echo's newline is translated
    too. Reproduced exactly so the two result trees line up.
    """
    stem = apk_name[:-4] if apk_name.endswith(".apk") else apk_name
    return "".join(c if re.match(r"[A-Za-z0-9._-]", c) else "_" for c in stem) + "_"


def synth_classpath():
    jars = [os.path.join(TOOLS, "lib", j)
            for j in sorted(os.listdir(os.path.join(TOOLS, "lib")))
            if j.endswith(".jar")]
    return os.pathsep.join(jars + [os.path.join(TOOLS, "classes")])


def is_lfs_pointer(path):
    """A checkout without git-lfs leaves 130-byte text stubs in place of APKs."""
    try:
        if os.path.getsize(path) > 1024:
            return False
        with open(path, "rb") as fh:
            return fh.read(40).startswith(b"version https://git-lfs")
    except OSError:
        return False


def count_classes(dirs):
    n = 0
    for d in dirs:
        for _, _, files in os.walk(d):
            n += sum(1 for f in files if f.endswith(".class"))
    return n


def xml_line_counter(xml_path):
    """(covered, missed) from the report-level LINE counter."""
    try:
        text = open(xml_path, encoding="utf-8", errors="replace").read()
    except OSError:
        return None
    # the last report-level counters are the totals; take the final LINE counter
    hits = re.findall(r'<counter type="LINE" missed="(\d+)" covered="(\d+)"/>', text)
    if not hits:
        return None
    missed, covered = int(hits[-1][0]), int(hits[-1][1])
    return covered, missed


def html_stats(html_dir):
    """(source pages, highlighted lines) in a generated HTML report.

    JaCoCo marks each source line with class fc/pc/nc (full, partial, no
    coverage); counting them is what shows the report is actually colour coded
    rather than a bare class listing.
    """
    pages, spans = 0, 0
    for root, _, files in os.walk(html_dir):
        for f in files:
            if not f.endswith(".java.html") and not f.endswith(".kt.html"):
                continue
            pages += 1
            if spans < 1:
                try:
                    body = open(os.path.join(root, f), encoding="utf-8",
                                errors="replace").read()
                except OSError:
                    continue
                spans += len(re.findall(r'class="(?:fc|pc|nc)[\s"]', body))
    if pages and not spans:
        # count across all pages only if the first sample had none
        for root, _, files in os.walk(html_dir):
            for f in files:
                if f.endswith(".java.html") or f.endswith(".kt.html"):
                    body = open(os.path.join(root, f), encoding="utf-8",
                                errors="replace").read()
                    spans += len(re.findall(r'class="(?:fc|pc|nc)[\s"]', body))
    return pages, spans


def package_of(apk_path):
    """Package name via aapt, matching what the smoke test records."""
    aapt = os.environ.get("AAPT")
    if not aapt or not os.path.isfile(aapt):
        sdk = os.environ.get("SDK", "/home/ubuntu/android-sdk")
        found = sorted(glob.glob(os.path.join(sdk, "build-tools", "*", "aapt")))
        aapt = found[-1] if found else shutil.which("aapt")
    if not aapt:
        return ""
    try:
        out = subprocess.run([aapt, "dump", "badging", apk_path],
                             capture_output=True, text=True, timeout=180).stdout
        m = re.search(r"^package: name='([^']+)'", out, re.M)
        return m.group(1) if m else ""
    except Exception:  # noqa: BLE001 - a missing package name is not fatal here
        return ""


def verify_apk(app, app_dir, apk_name, info, cp):
    t0 = time.time()
    dest = os.path.join(OUT, smoke_slug(apk_name))
    shutil.rmtree(dest, ignore_errors=True)
    os.makedirs(dest, exist_ok=True)

    row = {k: "" for k in FIELDS}
    row.update(app=app, apk=apk_name, verdict="FAIL", coverage_source="synthetic",
               installed="n/a", launched="n/a")

    apk_path = os.path.join(app_dir, apk_name)
    if not os.path.isfile(apk_path):
        row["detail"] = "declared APK missing"
        row["seconds"] = int(time.time() - t0)
        return row
    if is_lfs_pointer(apk_path):
        row["detail"] = "APK is an unfetched Git LFS pointer"
        row["seconds"] = int(time.time() - t0)
        return row
    row["package"] = package_of(apk_path)

    cls = [os.path.join(app_dir, p) for p in info.get("classfiles", [])]
    src = [os.path.join(app_dir, p) for p in info.get("sourcefiles", [])]
    row["class_dirs"] = len(cls)
    row["source_dirs"] = len(src)
    missing = [p for p in cls + src if not os.path.isdir(p)]
    if missing:
        row["detail"] = "%d declared paths do not exist" % len(missing)
        return row
    if not cls:
        row["detail"] = "no class directories declared"
        return row

    row["classes_found"] = count_classes(cls)
    if not row["classes_found"]:
        row["detail"] = "declared class dirs hold no .class files"
        row["seconds"] = int(time.time() - t0)
        return row

    # ---- synthesize execution data over exactly these classes
    #
    # Deliberately NOT named coverage.ec. The smoke tree's coverage.ec is pulled
    # off a device; giving this file that name is all it would take for the two to
    # be mixed up later.
    exec_path = os.path.join(dest, "synthetic.exec")
    p = subprocess.run([JAVA, "-cp", cp, "SynthExec", exec_path, FRACTION] + cls,
                       capture_output=True, text=True, timeout=1800)  # noqa: E501
    if p.returncode != 0:
        row["detail"] = ("SynthExec failed: " + (p.stderr.strip().splitlines() or [""])[-1])[:160]
        row["seconds"] = int(time.time() - t0)
        return row
    m = re.search(r"written=(\d+)", p.stdout)
    row["exec_classes"] = int(m.group(1)) if m else 0
    row["ec_bytes"] = os.path.getsize(exec_path)

    # ---- report
    xml_path = os.path.join(dest, "coverage.xml")
    html_dir = os.path.join(dest, "coverage_html")
    cmd = [JAVA, "-jar", JACOCOCLI, "report", exec_path]
    for c in cls:
        cmd += ["--classfiles", c]
    for s in src:
        cmd += ["--sourcefiles", s]
    cmd += ["--xml", xml_path, "--html", html_dir, "--name", "%s %s" % (app, apk_name)]
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=3600)
    combined = (r.stdout or "") + (r.stderr or "")
    # not .log: the repository ignores *.log, and this output is the evidence
    open(os.path.join(dest, "jacococli-output.txt"), "w").write(
        " ".join(cmd) + "\n" + combined)

    row["mismatched_classes"] = combined.count("does not match")

    if "Error while analyzing" in combined:
        bad = re.search(r"Error while analyzing ([^\s]+)", combined)
        row["analyze_error"] = bad.group(1)[-70:] if bad else "yes"
        row["detail"] = "jacococli aborted analyzing a class"
        row["verdict"] = "ANALYZE ERROR"
        row["seconds"] = int(time.time() - t0)
        return row
    if r.returncode != 0 or not os.path.isfile(xml_path):
        row["detail"] = ("jacococli rc=%s %s" % (r.returncode, combined.strip()[-120:]))
        row["seconds"] = int(time.time() - t0)
        return row

    counts = xml_line_counter(xml_path)
    if not counts:
        row["detail"] = "report has no LINE counter"
        row["seconds"] = int(time.time() - t0)
        return row
    covered, missed = counts
    total = covered + missed
    row["line_total"] = total
    row["line_covered"] = covered
    row["line_coverage_pct"] = "%.2f" % (100.0 * covered / total) if total else "0.00"

    pages, spans = html_stats(html_dir)
    row["html_pages"] = pages
    row["highlight_spans"] = spans
    row["seconds"] = int(time.time() - t0)

    row["detail"] = "mismatch warnings: %d" % row["mismatched_classes"]
    if total == 0:
        row["detail"] = "no lines counted"
    elif covered == 0:
        row["detail"] = "no covered lines; class ids did not match"
    elif pages == 0:
        row["detail"] = "no annotated source pages; sources unresolved"
    elif spans == 0:
        row["detail"] = "source pages carry no coverage markup"
    else:
        row["verdict"] = "PASS"
    return row


def main():
    filters = [a.lower() for a in sys.argv[1:]]
    os.makedirs(OUT, exist_ok=True)
    cp = synth_classpath()

    rows = []
    for app in sorted(os.listdir(APPS)):
        app_dir = os.path.join(APPS, app)
        cfg = os.path.join(app_dir, "class_files.json")
        if not os.path.isdir(app_dir) or not os.path.isfile(cfg):
            continue
        if filters and not any(f in app.lower() for f in filters):
            continue
        try:
            entries = json.load(open(cfg))
        except (OSError, ValueError) as exc:
            print("%-28s class_files.json unreadable: %s" % (app, exc))
            continue
        for apk_name, info in sorted(entries.items()):
            row = verify_apk(app, app_dir, apk_name, info, cp)
            rows.append(row)
            print("%-14s %-26s %-40s lines=%-7s cov=%-7s pages=%-5s %s"
                  % (row["verdict"], app[:26], apk_name[:40], row["line_total"],
                     row["line_coverage_pct"], row["html_pages"],
                     row["detail"][:52]))

    # Same filename shape as smoke_summary.csv, in the same layout, so one
    # consumer can read both. validation_summary.csv is the current name;
    # report_validation.csv is written too so existing references keep working.
    csv_path = os.path.join(OUT, "validation_summary.csv")
    for path in (csv_path, os.path.join(OUT, "report_validation.csv")):
        with open(path, "w", newline="") as fh:
            w = csv.DictWriter(fh, fieldnames=FIELDS)
            w.writeheader()
            for r in rows:
                w.writerow(r)

    # Same probe-level artefacts the smoke test produces, so a result directory
    # here has the same file set as one under results/smoke_test/.
    probe = os.path.join(PROJECT, "probe_report.py")
    if os.path.isfile(probe):
        print()
        subprocess.run([sys.executable, probe, OUT], timeout=7200)

    ok = [r for r in rows if r["verdict"] == "PASS"]
    apps_ok = {r["app"] for r in ok}
    print("\n%d/%d apk entries pass, covering %d apps"
          % (len(ok), len(rows), len(apps_ok)))
    print("summary: %s" % csv_path)
    return 0 if len(ok) == len(rows) else 1


if __name__ == "__main__":
    sys.exit(main())
