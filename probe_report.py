#!/usr/bin/env python3
"""
probe_report.py - produce a coverage artifact for apps that have no .class files.

Jacoco cannot build an XML/HTML report without class files: a .ec file holds only
a class id, a class name and a probe hit array, with no line-number information.
What it CAN do is report probe-level execution, via `jacococli execinfo`.

For every smoke-test result directory this writes:
    probe_report.txt    raw `jacococli execinfo` output
    probe_summary.csv   per-class: class_id, class_name, probes_hit, probes_total, pct

IMPORTANT interpretation note:
    probe coverage here is measured only over classes that were actually LOADED
    at runtime, because unloaded classes never appear in a .ec file at all. Line
    coverage from a full report counts every class in the app, loaded or not.
    So this percentage is systematically higher and is NOT comparable to the
    line_coverage_pct of the apps that have class files. Use it as proof that
    coverage data exists, and as a measure of how much loaded code was executed.

Usage:
    python3 probe_report.py [results/smoke_test]
"""
import csv
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
# Resolved relative to this file, not to one developer's home directory. The
# absolute path meant any other checkout ran `java -jar` against a jar that was
# not there and reported every app as an execinfo failure.
JACOCO = os.environ.get(
    'JACOCO', os.path.join(HERE, 'fuzzingandroid', 'libs', 'jacococli.jar'))
JAVA = os.environ.get('JAVA', 'java')

# Execution data is coverage.ec when pulled from a device, and synthetic.exec
# when written by dataset_builder/verify_reports.py on a host with no emulator.
# Both are valid input here; the filename is what distinguishes them.
EXEC_NAMES = ('coverage.ec', 'synthetic.exec')

ROW = re.compile(r'^([0-9a-f]{16})\s+(\d+)\s+of\s+(\d+)\s+(\S+)')


def analyse(ec_path):
    """Return (rows, hits, probes) parsed from jacococli execinfo."""
    try:
        out = subprocess.run([JAVA, '-jar', JACOCO, 'execinfo', ec_path],
                             capture_output=True, text=True, timeout=600).stdout
    except Exception as exc:
        return None, 0, 0, 'execinfo failed: %s' % exc

    rows, hits, probes = [], 0, 0
    for line in out.splitlines():
        m = ROW.match(line.strip())
        if not m:
            continue
        cid, h, p, name = m.group(1), int(m.group(2)), int(m.group(3)), m.group(4)
        rows.append([cid, name, h, p, round(100.0 * h / p, 2) if p else 0.0])
        hits += h
        probes += p
    return rows, hits, probes, out


def main():
    base = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, 'results', 'smoke')
    if not os.path.isdir(base):
        sys.exit('no such directory: %s' % base)
    results = []

    for name in sorted(os.listdir(base)):
        d = os.path.join(base, name)
        if not os.path.isdir(d):
            continue
        ec = next((os.path.join(d, n) for n in EXEC_NAMES
                   if os.path.isfile(os.path.join(d, n))), None)
        if not ec or os.path.getsize(ec) == 0:
            continue

        rows, hits, probes, raw = analyse(ec)
        if rows is None:
            print('  %-40s ERROR %s' % (name, raw))
            continue

        with open(os.path.join(d, 'probe_report.txt'), 'w') as fh:
            fh.write(raw)
        with open(os.path.join(d, 'probe_summary.csv'), 'w', newline='') as fh:
            w = csv.writer(fh)
            w.writerow(['class_id', 'class_name', 'probes_hit', 'probes_total', 'pct'])
            w.writerows(sorted(rows, key=lambda r: -r[2]))

        pct = 100.0 * hits / probes if probes else 0.0
        has_xml = os.path.isfile(os.path.join(d, 'coverage.xml'))
        results.append([name, len(rows), hits, probes, round(pct, 2), has_xml])
        print('  %-40s classes=%-5s probes=%s/%s (%.2f%%)  %s' % (
            name[:40], len(rows), hits, probes, pct,
            'has full report' if has_xml else 'PROBE REPORT ONLY (no class files)'))

    out_csv = os.path.join(base, 'probe_summary_all.csv')
    with open(out_csv, 'w', newline='') as fh:
        w = csv.writer(fh)
        w.writerow(['result_dir', 'classes_loaded', 'probes_hit', 'probes_total',
                    'probe_pct_of_loaded_classes', 'has_full_line_coverage_report'])
        w.writerows(results)
    print('\nwrote %s (%d apps)' % (out_csv, len(results)))


if __name__ == '__main__':
    main()
