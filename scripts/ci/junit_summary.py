#!/usr/bin/env python3
"""Summarise JUnit XML results (Gradle test tasks) into the GitHub job summary.

Prints totals, and for every failed test the class, name and first line of the failure,
so the reason is visible without downloading the report artifact. Never fails the job by
itself; the Gradle task already did. Standard library only.

Usage: junit_summary.py "<title>" <glob> [<glob> ...]
"""
from __future__ import annotations

import glob
import os
import sys
import xml.etree.ElementTree as ET

MAX_ANNOTATIONS = 20


def main() -> int:
    if len(sys.argv) < 3:
        print(__doc__)
        return 0
    title, patterns = sys.argv[1], sys.argv[2:]
    files = sorted({f for p in patterns for f in glob.glob(p, recursive=True)})
    total = failed = skipped = 0
    failures = []
    for path in files:
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        suites = [root] if root.tag == "testsuite" else root.findall("testsuite")
        for suite in suites:
            for case in suite.findall("testcase"):
                total += 1
                if case.find("skipped") is not None:
                    skipped += 1
                problem = case.find("failure")
                if problem is None:
                    problem = case.find("error")
                if problem is not None:
                    failed += 1
                    msg = (problem.get("message") or problem.text or "").strip().splitlines()
                    module = path.split("/build/")[0]
                    failures.append((module, case.get("classname", "?"), case.get("name", "?"),
                                     msg[0][:300] if msg else ""))

    print(f"{title}: {total} tests, {failed} failed, {skipped} skipped ({len(files)} result files)")
    for i, (module, cls, name, msg) in enumerate(failures):
        print(f"FAILED {module} {cls} > {name}: {msg}")
        if os.environ.get("GITHUB_ACTIONS") == "true" and i < MAX_ANNOTATIONS:
            print(f"::error title=Test failed: {cls.split('.')[-1]}::{name}: {msg}")

    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as s:
            icon = "❌" if failed else ("⚠️" if total == 0 else "✅")
            s.write(f"### {icon} {title}: {total} tests, {failed} failed, {skipped} skipped\n\n")
            if failures:
                s.write("| Module | Test | Failure |\n|---|---|---|\n")
                for module, cls, name, msg in failures[:100]:
                    safe = msg.replace("|", "\\|")
                    s.write(f"| `{module}` | `{cls.split('.')[-1]}` > {name} | {safe} |\n")
                s.write("\nFull reports are in the `test-reports` artifact.\n")
            s.write("\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
