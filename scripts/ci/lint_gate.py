#!/usr/bin/env python3
"""Fail if Android lint reported Error or Fatal issues, listing each with its location.

app/build.gradle.kts sets lint abortOnError=false so that one report covers every module;
this script is the gate. Issues already in app/lint-baseline.xml are not in the report.
Standard library only.

Usage: lint_gate.py build/reports/lint/lint.xml
"""
from __future__ import annotations

import os
import sys
import xml.etree.ElementTree as ET

BLOCKING = {"Error", "Fatal"}


def main() -> int:
    path = sys.argv[1] if len(sys.argv) > 1 else "build/reports/lint/lint.xml"
    if not os.path.exists(path):
        print(f"::error title=Android lint::{path} not found; did lintRelease run?")
        return 1
    root = ET.parse(path).getroot()
    repo = os.getcwd().rstrip("/") + "/"
    blocking = []
    counts: dict[str, int] = {}
    for issue in root.iter("issue"):
        sev = issue.get("severity", "?")
        counts[sev] = counts.get(sev, 0) + 1
        if sev not in BLOCKING:
            continue
        loc = issue.find("location")
        file = (loc.get("file", "?") if loc is not None else "?").replace(repo, "")
        line = loc.get("line", "") if loc is not None else ""
        blocking.append((issue.get("id"), issue.get("category"), file, line,
                         (issue.get("message") or "").replace("\n", " ")))

    print("Android lint severities: " + ", ".join(f"{k}={v}" for k, v in sorted(counts.items())))
    for issue_id, category, file, line, msg in blocking:
        print(f"{file}:{line}: [{issue_id}] ({category}) {msg}")
        if os.environ.get("GITHUB_ACTIONS") == "true":
            line_part = f",line={line}" if line else ""
            print(f"::error file={file}{line_part},title=Lint {issue_id}::{msg}")

    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as s:
            s.write(f"### Android lint: {'❌ ' + str(len(blocking)) + ' blocking issue(s)' if blocking else '✅ no Error/Fatal issues'}\n\n")
            for issue_id, category, file, line, msg in blocking[:100]:
                s.write(f"- `{issue_id}` ({category}) `{file}:{line}`: {msg}\n")
            s.write("\nFull report: `static-analysis-reports` artifact (lint.html).\n\n")
    if blocking:
        print(f"{len(blocking)} blocking lint issue(s). Fix them; baseline only pre-existing legacy issues "
              f"(scripts/lintBaseline.sh). Security issues must be fixed, not baselined.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
