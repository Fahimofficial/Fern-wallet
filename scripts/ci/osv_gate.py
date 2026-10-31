#!/usr/bin/env python3
"""Apply the vulnerability severity policy to an osv-scanner JSON report.

Policy (see docs/SECURITY.md):
  Critical (CVSS >= 9.0) and High (>= 7.0) -> blocking
  unknown severity                         -> blocking until triaged (fix it, or add a
                                              justified, expiring entry to osv-scanner.toml)
  Medium (>= 4.0), Low                     -> reported as warnings, not blocking

Accepted risks belong in osv-scanner.toml ([[IgnoredVulns]] with reason and ignoreUntil);
osv-scanner removes them from the report before this script runs.

Exit code 0 = no blocking findings, 1 = blocking findings, 2 = bad input.
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from dataclasses import dataclass

import ci_common

ORDER = ["Critical", "High", "Unknown", "Medium", "Low", "Informational"]
BLOCKING = {"Critical", "High", "Unknown"}


class ReportError(ValueError):
    """The report doesn't have the osv-scanner JSON structure."""


@dataclass
class Finding:
    level: str
    score: str
    package: str
    ecosystem: str
    ids: str
    source: str

    def describe(self) -> str:
        return f"{self.level} ({self.score}) {self.ecosystem} {self.package} [{self.ids}] in {self.source}"


def level(score: str | float | None) -> str:
    """Map a CVSS base score to the policy's severity level."""
    try:
        value = float(score) if score not in (None, "") else None
    except (TypeError, ValueError):
        value = None
    if value is None:
        return "Unknown"
    thresholds = [(9.0, "Critical"), (7.0, "High"), (4.0, "Medium")]
    for minimum, name in thresholds:
        if value >= minimum:
            return name
    return "Low" if value > 0 else "Informational"


def _list(value, what: str) -> list:
    if value is None:
        return []
    if not isinstance(value, list):
        raise ReportError(f"'{what}' must be a list")
    return value


def findings(report: dict) -> list[Finding]:
    """Flatten osv-scanner results into one finding per vulnerability group, worst first."""
    if not isinstance(report, dict):
        raise ReportError("report must be a JSON object")
    rows = []
    for result in _list(report.get("results"), "results"):
        source = os.path.basename((result.get("source") or {}).get("path", "?"))
        for pkg in _list(result.get("packages"), "packages"):
            p = pkg.get("package") or {}
            for group in _list(pkg.get("groups"), "groups"):
                ids = group.get("aliases") or group.get("ids") or []
                rows.append(Finding(level(group.get("max_severity")), str(group.get("max_severity") or "n/a"),
                                    f"{p.get('name')}@{p.get('version')}", p.get("ecosystem", "?"),
                                    ", ".join(sorted(ids)[:4]), source))
    rows.sort(key=lambda f: ORDER.index(f.level))
    return rows


def blocking(rows: list[Finding], advisory: bool) -> list[Finding]:
    return [] if advisory else [r for r in rows if r.level in BLOCKING]


def render_summary(title: str, rows: list[Finding], blocked: list[Finding]) -> str:
    out = [f"### {title}: {'❌ blocking findings' if blocked else '✅ no blocking findings'}\n\n"]
    if not rows:
        out.append("No known vulnerabilities.\n\n")
        return "".join(out)
    out.append("| Severity | CVSS | Package | Advisories | Source |\n|---|---|---|---|---|\n")
    out.extend(f"| {r.level} | {r.score} | `{r.package}` | {r.ids} | {r.source} |\n" for r in rows)
    out.append("\nFix: upgrade the dependency (Dependabot may already have a PR). If it does not "
               "apply to Fern Wallet, add an expiring entry with a reason to `osv-scanner.toml`.\n\n")
    return "".join(out)


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("report", help="osv-scanner --format json output")
    ap.add_argument("--advisory", action="store_true",
                    help="report only, never fail (for tooling that does not ship, e.g. Gemfile.lock)")
    ap.add_argument("--title", default="Dependency vulnerabilities")
    args = ap.parse_args(argv)
    try:
        with open(args.report, encoding="utf-8") as f:
            rows = findings(json.load(f))
    except (OSError, ValueError) as e:  # ValueError covers JSON errors and ReportError
        print(f"error: cannot use {args.report}: {e}", file=sys.stderr)
        return 2

    blocked = blocking(rows, args.advisory)
    for r in rows:
        is_blocking = r in blocked
        print(("BLOCKING " if is_blocking else "warning  ") + r.describe())
        ci_common.annotate("error" if is_blocking else "warning", "Vulnerable dependency", r.describe())
    if not rows:
        print("No known vulnerabilities in the scanned dependency set.")
    ci_common.append_summary(render_summary(args.title, rows, blocked))
    return 1 if blocked else 0


if __name__ == "__main__":
    sys.exit(main())
