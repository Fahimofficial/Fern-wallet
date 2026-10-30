#!/usr/bin/env python3
"""
Fail if Android lint reported Error or Fatal issues, listing each with its location.

app/build.gradle.kts sets lint abortOnError=false so that one report covers every module;
this script is the gate. Issues already in app/lint-baseline.xml are not in the report.
Exit code 0 = no blocking issues, 1 = blocking issues or an unreadable/missing report.

Usage: lint_gate.py build/reports/lint/lint.xml
"""
from __future__ import annotations

import os
import sys
from dataclasses import dataclass

import ci_common
import safe_xml

BLOCKING = {"Error", "Fatal"}
MAX_SUMMARY_ROWS = 100


@dataclass
class Issue:
    id: str
    severity: str
    category: str
    file: str
    line: str
    message: str


def load_issues(path: str, repo_root: str) -> list[Issue]:
    """Read lint.xml; raises OSError or safe_xml.UnsafeXmlError if it can't be read."""
    root = safe_xml.parse(path)
    prefix = repo_root.rstrip("/") + "/"
    issues = []
    for issue in root.iter("issue"):
        location = issue.find("location")
        file = (location.get("file", "?") if location is not None else "?").replace(prefix, "")
        line = location.get("line", "") if location is not None else ""
        issues.append(Issue(issue.get("id", "?"), issue.get("severity", "?"), issue.get("category", "?"),
                            file, line, (issue.get("message") or "").replace("\n", " ")))
    return issues


def blocking_issues(issues: list[Issue]) -> list[Issue]:
    return [i for i in issues if i.severity in BLOCKING]


def severity_counts(issues: list[Issue]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for issue in issues:
        counts[issue.severity] = counts.get(issue.severity, 0) + 1
    return counts


def render_summary(blocking: list[Issue]) -> str:
    if not blocking:
        return "### Android lint: ✅ no Error/Fatal issues\n\nFull report: `static-analysis-reports` artifact.\n\n"
    rows = "".join(f"- `{i.id}` ({i.category}) `{i.file}:{i.line}`: {ci_common.md_cell(i.message)}\n"
                   for i in blocking[:MAX_SUMMARY_ROWS])
    return (f"### Android lint: ❌ {len(blocking)} blocking issue(s)\n\n{rows}"
            "\nFull report: `static-analysis-reports` artifact (lint.html).\n\n")


def fail(message: str) -> int:
    print(f"ERROR: {message}")
    ci_common.annotate("error", "Android lint", message)
    ci_common.append_summary(f"### Android lint: ❌ {message}\n\n")
    return 1


def main(argv: list[str] | None = None) -> int:
    argv = sys.argv[1:] if argv is None else argv
    path = argv[0] if argv else "build/reports/lint/lint.xml"
    if not os.path.exists(path):
        return fail(f"{path} not found; did lintRelease run?")
    try:
        issues = load_issues(path, os.getcwd())
    except (OSError, safe_xml.UnsafeXmlError) as e:
        return fail(f"cannot read {path}: {e}")

    blocking = blocking_issues(issues)
    print("Android lint severities: " + ", ".join(f"{k}={v}" for k, v in sorted(severity_counts(issues).items())))
    for i in blocking:
        print(f"{i.file}:{i.line}: [{i.id}] ({i.category}) {i.message}")
        ci_common.annotate("error", f"Lint {i.id}", i.message, i.file, i.line)
    ci_common.append_summary(render_summary(blocking))
    if blocking:
        print(f"{len(blocking)} blocking lint issue(s). Fix them; baseline only pre-existing legacy issues "
              f"(scripts/lintBaseline.sh). Security issues must be fixed, not baselined.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
