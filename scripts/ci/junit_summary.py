#!/usr/bin/env python3
"""Summarise JUnit XML results and fail if tests failed or too few tests ran."""

from __future__ import annotations

import argparse
import glob
import sys
from dataclasses import dataclass, field

import ci_common
import safe_xml

_DETAILS = """\
Writes totals and every failed test (class, name, first line of the failure) to the job summary,
so the reason is visible without downloading the report artifact.

Exit code 1 when a test failed or errored, a test suite reports errors without test cases, a
result file can't be read, or fewer than --min-tests tests ran. --require-tests-per-file also
fails on result files without test cases (an Android test APK that didn't install). Some runners,
e.g. AGP 9's connected-test runner, don't fail the build for these, so this is a real gate.

Usage: junit_summary.py "<title>" <glob> [<glob> ...] [--min-tests N] [--min-files N] [--require-tests-per-file]
"""

MAX_ANNOTATIONS = 20
MAX_SUMMARY_ROWS = 100


@dataclass
class Failure:
    module: str
    classname: str
    name: str
    message: str


@dataclass
class Results:
    files: int = 0
    total: int = 0
    skipped: int = 0
    failures: list[Failure] = field(default_factory=list)
    unreadable: list[str] = field(default_factory=list)
    empty_files: list[str] = field(default_factory=list)


def _first_line(text: str) -> str:
    lines = text.strip().splitlines()
    return lines[0][:300] if lines else ""


def _suite_failures(suite: safe_xml.Element, module: str) -> tuple[int, int, list[Failure]]:
    total = skipped = 0
    failures: list[Failure] = []
    for case in suite.findall("testcase"):
        total += 1
        if case.find("skipped") is not None:
            skipped += 1
        problem = case.find("failure") or case.find("error")
        if problem is not None:
            message = _first_line(problem.get("message") or problem.text)
            failures.append(Failure(module, case.get("classname", "?"), case.get("name", "?"), message))
    declared = sum(int(suite.get(k, "0") or 0) for k in ("failures", "errors"))
    if declared > len(failures):
        failures.append(Failure(module, suite.get("name", "?"), "(suite)",
                                f"suite reports {declared} failure(s)/error(s) without matching test cases"))
    return total, skipped, failures


def collect(paths: list[str]) -> Results:
    """Read every JUnit XML file and tally tests, skips and failures."""
    results = Results()
    for path in paths:
        results.files += 1
        module = path.split("/build/")[0]
        try:
            root = safe_xml.parse(path)
        except (OSError, safe_xml.UnsafeXmlError) as e:
            results.unreadable.append(f"{path}: {e}")
            continue
        suites = [root] if root.tag == "testsuite" else root.findall("testsuite")
        file_total = 0
        for suite in suites:
            total, skipped, failures = _suite_failures(suite, module)
            file_total += total
            results.total += total
            results.skipped += skipped
            results.failures.extend(failures)
        if file_total == 0:
            results.empty_files.append(path)
    return results


def problems(results: Results, min_tests: int, require_per_file: bool, min_files: int = 0) -> list[str]:
    """Return the reasons the gate fails (empty list = pass)."""
    found = []
    if results.files < min_files:
        found.append(f"found {results.files} result file(s), expected at least {min_files} (one per tested module)")
    if results.failures:
        found.append(f"{len(results.failures)} failed test(s)")
    found.extend(f"unreadable result file {u}" for u in results.unreadable)
    if results.total < min_tests:
        found.append(f"only {results.total} test(s) ran, expected at least {min_tests}")
    if require_per_file:
        found.extend(f"no test cases in {p} (did the test APK install and run?)" for p in results.empty_files)
    return found


def render_summary(title: str, results: Results, reasons: list[str]) -> str:
    icon = "❌" if reasons else "✅"
    out = [f"### {icon} {title}: {results.total} tests, {len(results.failures)} failed, "
           f"{results.skipped} skipped ({results.files} result files)\n\n"]
    for reason in reasons:
        out.append(f"- {ci_common.md_cell(reason)}\n")
    if results.failures:
        out.append("\n| Module | Test | Failure |\n|---|---|---|\n")
        for f in results.failures[:MAX_SUMMARY_ROWS]:
            out.append(f"| `{f.module}` | `{f.classname.split('.')[-1]}` > {ci_common.md_cell(f.name)} | "
                       f"{ci_common.md_cell(f.message)} |\n")
        out.append("\nFull reports are in the test report artifact.\n")
    out.append("\n")
    return "".join(out)


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(
        description=f"{__doc__}\n\n{_DETAILS}", formatter_class=argparse.RawDescriptionHelpFormatter
    )
    ap.add_argument("title")
    ap.add_argument("patterns", nargs="+", help="glob(s) of JUnit XML files; ** is recursive")
    ap.add_argument("--min-tests", type=int, default=0)
    ap.add_argument("--require-tests-per-file", action="store_true")
    ap.add_argument("--min-files", type=int, default=0, help="minimum number of result files (e.g. one per module)")
    args = ap.parse_args(argv)

    paths = sorted({p for pattern in args.patterns for p in glob.glob(pattern, recursive=True)})
    results = collect(paths)
    reasons = problems(results, args.min_tests, args.require_tests_per_file, args.min_files)

    print(f"{args.title}: {results.total} tests, {len(results.failures)} failed, "
          f"{results.skipped} skipped ({results.files} result files)")
    for i, f in enumerate(results.failures):
        print(f"FAILED {f.module} {f.classname} > {f.name}: {f.message}")
        if i < MAX_ANNOTATIONS:
            ci_common.annotate("error", f"Test failed: {f.classname.split('.')[-1]}", f"{f.name}: {f.message}")
    for reason in reasons:
        print(f"ERROR: {reason}")
    if reasons and not results.failures:
        ci_common.annotate("error", args.title, "; ".join(reasons))
    ci_common.append_summary(render_summary(args.title, results, reasons))
    return 1 if reasons else 0


if __name__ == "__main__":
    sys.exit(main())
