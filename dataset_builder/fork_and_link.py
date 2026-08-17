#!/usr/bin/env python3
"""
fork_and_link.py - fork each subject's source repo into a GitHub organization,
push the instrumented sources to it, and register it as a submodule.

Why the submodule sits beside the dataset tree, not on top of it
---------------------------------------------------------------
A dataset entry works because class_files.json points at compiled classes that
are committed in this repository. If the tree that holds them were replaced by a
submodule, a fresh clone would get sources but no compiled classes, and
`jacococli report` would have nothing to map probes onto - the exact
`classes_declared_found=0` failure this dataset was built to fix.

So each app gets:

    instrumented_apps/<App>/<App>-#<id>/   tracked content: classes + sources
    instrumented_apps/<App>/upstream       submodule -> <org>/<repo> @ instrumented commit

The submodule carries the instrumentation modifications, so the app builds and
reports coverage from a plain checkout. The tracked tree keeps the dataset usable
without rebuilding anything.

Auth: uses the token the IDE's git credential helper provides (GIT_ASKPASS).
Requires `repo` scope and permission to create repositories in the org.

    python3 fork_and_link.py --list                 # what would happen
    python3 fork_and_link.py --only geohashdroid    # one app, end to end
    python3 fork_and_link.py                        # every eligible app
"""

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(HERE)
APPS = os.path.join(PROJECT, "instrumented_apps")
ORG = os.environ.get("ORG", "bugfixops")
BRANCH = os.environ.get("INSTRUMENTED_BRANCH", "timemachine-instrumented")
STATE = os.path.join(HERE, "forks.json")

sys.path.insert(0, HERE)
from instrument import instrument_project, InstrumentError  # noqa: E402


def token():
    """Token for the GitHub API and for pushing to the forks.

    Read from the environment or from the project's .env. The IDE's GIT_ASKPASS
    helper is deliberately not used: it talks to the editor over an IPC socket,
    so it works for an interactive `git push` but blocks forever in a detached
    background run, which is how this script has to execute.
    """
    for key in ("GIT_TOKEN", "GITHUB_TOKEN"):
        val = os.environ.get(key)
        if val:
            return val.strip()

    envfile = os.path.join(PROJECT, ".env")
    if os.path.isfile(envfile):
        for line in open(envfile, encoding="utf-8", errors="replace"):
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, val = line.partition("=")
            if key.strip() in ("GIT_TOKEN", "GITHUB_TOKEN"):
                return val.strip().strip("'\"")

    raise SystemExit("no GIT_TOKEN in environment or .env")


def api(path, tok, method="GET", body=None, accept_status=(200, 201, 202)):
    url = path if path.startswith("http") else "https://api.github.com" + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers={
        "Authorization": "Bearer " + tok,
        "Accept": "application/vnd.github+json",
        "User-Agent": "tm-dataset-builder",
    })
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            payload = r.read()
            return r.status, (json.loads(payload) if payload else {})
    except urllib.error.HTTPError as e:
        payload = e.read()
        try:
            detail = json.loads(payload)
        except ValueError:
            detail = {"message": payload[:200].decode("utf-8", "replace")}
        return e.code, detail


def subjects():
    """Subjects that have both a source repo and a published dataset."""
    out = []
    for fname in ("subjects.json", "modern_subjects.json"):
        p = os.path.join(HERE, fname)
        if os.path.isfile(p):
            out.extend(json.load(open(p)))
    return out


def valid_apps():
    import csv
    path = os.path.join(APPS, "MANIFEST.tsv")
    if not os.path.isfile(path):
        return set()
    good, bad = set(), set()
    for r in csv.DictReader(open(path), delimiter="\t"):
        (good if r["paths_ok"] == "yes" else bad).add(r["app"])
    return good - bad


def load_state():
    return json.load(open(STATE)) if os.path.isfile(STATE) else {}


def save_state(state):
    json.dump(state, open(STATE, "w"), indent=1, sort_keys=True)


def fork_repo(upstream, tok, org=ORG):
    """Fork owner/repo into the org; returns (full_name, clone_url)."""
    owner_repo = upstream.replace("https://github.com/", "").rstrip("/")

    # Resolve renames first. Forking a repository that has moved answers
    # 307 Moved Permanently (AdrienPoupa/VinylMusicPlayer did), so ask for the
    # repo metadata, which follows the redirect, and fork its current name.
    status, info = api("/repos/" + owner_repo, tok)
    if status == 200 and info.get("full_name") and \
            info["full_name"].lower() != owner_repo.lower():
        owner_repo = info["full_name"]

    name = owner_repo.split("/")[1]
    target = "%s/%s" % (org, name)

    status, existing = api("/repos/" + target, tok)
    if status == 200:
        return target, existing["clone_url"], "already present"

    status, body = api("/repos/%s/forks" % owner_repo, tok, method="POST",
                       body={"organization": org, "default_branch_only": True})
    if status not in (200, 201, 202):
        return None, None, "fork failed: %s %s" % (status, body.get("message"))

    # forking is asynchronous
    for _ in range(30):
        time.sleep(5)
        status, info = api("/repos/" + target, tok)
        if status == 200:
            return target, info["clone_url"], "forked"
    return None, None, "fork did not appear in time"


def push_instrumented(full_name, clone_url, subject, tok, logf):
    """Clone the fork, apply instrumentation, push it on a branch.

    Returns (commit_sha, note).
    """
    tmp = tempfile.mkdtemp(prefix="fork-", dir=os.environ.get("FORK_TMP", "/tmp"))
    work = os.path.join(tmp, "repo")
    authed = clone_url.replace("https://", "https://x-access-token:%s@" % tok)
    try:
        ref = subject.get("ref")
        cmd = ["git", "clone", "--depth", "1"]
        if ref:
            cmd += ["--branch", ref]
        cmd += [authed, work]
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=3600)
        if r.returncode != 0:
            # the instrumented branch lives in the upstream we forked from; a
            # default-branch-only fork will not have it, so fetch it explicitly
            r = subprocess.run(["git", "clone", "--depth", "1", authed, work],
                               capture_output=True, text=True, timeout=3600)
            if r.returncode != 0:
                return None, "clone failed: %s" % r.stderr[-160:]
            if ref:
                up = subject["repo"]
                subprocess.run(["git", "-C", work, "remote", "add", "upstream", up],
                               capture_output=True, text=True, timeout=120)
                f = subprocess.run(["git", "-C", work, "fetch", "--depth", "1",
                                    "upstream", ref],
                                   capture_output=True, text=True, timeout=3600)
                if f.returncode != 0:
                    return None, "fetch of %s failed" % ref
                subprocess.run(["git", "-C", work, "checkout", "-q", "FETCH_HEAD"],
                               capture_output=True, text=True, timeout=300)

        # apply the harness unless the branch already carries it
        note = "already instrumented"
        try:
            steps = instrument_project(work, logf)
            # the Themis branches already carry the harness, in which case every
            # step is a no-op; say so rather than claiming work was done
            applied = [k for k in ("plugin", "coverage_flag", "harness", "receiver")
                       if steps.get(k)]
            note = ("module=%s agp=%s applied=%s"
                    % (steps["module"], steps["agp_major"],
                       ",".join(applied) if applied else "nothing (already instrumented)"))
        except InstrumentError as exc:
            note = "instrument skipped: %s" % exc

        subprocess.run(["git", "-C", work, "checkout", "-q", "-B", BRANCH],
                       capture_output=True, text=True, timeout=300)

        # Drop CI definitions. A token without the `workflow` scope cannot push
        # a branch that creates or updates any .github/workflows file, and five
        # subjects were rejected on exactly that. The forks exist to build an
        # instrumented APK locally, so their CI is of no use here.
        wf = os.path.join(work, ".github", "workflows")
        if os.path.isdir(wf):
            shutil.rmtree(wf, ignore_errors=True)

        subprocess.run(["git", "-C", work, "add", "-A"],
                       capture_output=True, text=True, timeout=1800)
        subprocess.run(["git", "-C", work,
                        "-c", "user.name=dataset-builder",
                        "-c", "user.email=dataset-builder@localhost",
                        "commit", "-q", "-m",
                        "Instrument for TimeMachine coverage collection\n\n"
                        "Adds the Jacoco plugin, enables coverage for the debug\n"
                        "build type, and registers a receiver for\n"
                        "edu.gatech.m3.emma.COLLECT_COVERAGE so a running app can\n"
                        "dump execution data on demand.\n\n"
                        "CI workflow definitions are removed: they are not used\n"
                        "for local instrumented builds, and pushing them needs a\n"
                        "token scope this pipeline does not require."],
                       capture_output=True, text=True, timeout=1800)
        p = subprocess.run(["git", "-C", work, "push", "-f", "origin", BRANCH],
                           capture_output=True, text=True, timeout=3600)
        if p.returncode != 0:
            return None, "push failed: %s" % p.stderr[-160:]
        sha = subprocess.run(["git", "-C", work, "rev-parse", "HEAD"],
                             capture_output=True, text=True,
                             timeout=120).stdout.strip()
        return sha, note
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def add_submodule(app, full_name, sha):
    """Register the fork as a submodule at instrumented_apps/<App>/upstream."""
    rel = os.path.join("instrumented_apps", app, "upstream")
    full = os.path.join(PROJECT, rel)
    url = "https://github.com/%s.git" % full_name
    if os.path.exists(full):
        subprocess.run(["git", "-C", PROJECT, "submodule", "deinit", "-f", rel],
                       capture_output=True, text=True, timeout=300)
        subprocess.run(["git", "-C", PROJECT, "rm", "-rf", "--cached", rel],
                       capture_output=True, text=True, timeout=300)
        shutil.rmtree(full, ignore_errors=True)
    r = subprocess.run(["git", "-C", PROJECT, "submodule", "add", "-f",
                        "-b", BRANCH, "--", url, rel],
                       capture_output=True, text=True, timeout=3600)
    if r.returncode != 0:
        return False, r.stderr[-200:]
    if sha:
        subprocess.run(["git", "-C", full, "fetch", "--depth", "1", "origin", sha],
                       capture_output=True, text=True, timeout=1800)
        subprocess.run(["git", "-C", full, "checkout", "-q", sha],
                       capture_output=True, text=True, timeout=300)
    subprocess.run(["git", "-C", PROJECT, "add", ".gitmodules", rel],
                   capture_output=True, text=True, timeout=300)
    return True, "linked"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", default=None)
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--org", default=ORG)
    args = ap.parse_args()

    good = valid_apps()
    subs = [s for s in subjects() if s.get("repo")]
    if args.only:
        subs = [s for s in subs if args.only.lower() in s["name"].lower()]
    eligible = [s for s in subs if s["name"] in good]
    skipped = [s["name"] for s in subs if s["name"] not in good]

    print("org: %s     branch: %s" % (args.org, BRANCH))
    print("eligible (published dataset + source repo): %d" % len(eligible))
    for s in eligible:
        print("   %-30s %s@%s" % (s["name"],
                                  s["repo"].replace("https://github.com/", ""),
                                  s.get("ref") or "HEAD"))
    if skipped:
        print("skipped (no published dataset yet): %d" % len(skipped))
    if args.list:
        return 0

    tok = token()
    state = load_state()
    logf = os.path.join(HERE, "logs", "forks.log")
    os.makedirs(os.path.dirname(logf), exist_ok=True)

    for i, s in enumerate(eligible, 1):
        app = s["name"]
        print()
        print("(%d/%d) %s" % (i, len(eligible), app))
        entry = state.get(app, {})
        if entry.get("linked"):
            print("   already linked -> %s" % entry.get("fork"))
            continue

        full_name, clone_url, note = fork_repo(s["repo"], tok, args.org)
        print("   fork: %s (%s)" % (full_name or "FAILED", note))
        if not full_name:
            state[app] = {"error": note}
            save_state(state)
            continue

        sha, pnote = push_instrumented(full_name, clone_url, s, tok, logf)
        print("   push: %s (%s)" % (sha[:10] if sha else "FAILED", pnote))
        if not sha:
            state[app] = {"fork": full_name, "error": pnote}
            save_state(state)
            continue

        ok, lnote = add_submodule(app, full_name, sha)
        print("   link: %s" % lnote)
        state[app] = {"fork": full_name, "sha": sha, "linked": bool(ok),
                      "note": pnote}
        save_state(state)

    print()
    print("state: %s" % STATE)
    print("commit the result with:  git commit -m 'Link instrumented forks as submodules'")
    return 0


if __name__ == "__main__":
    sys.exit(main())
