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


def level(score: str | None) -> str:
    try:
        s = float(score) if score not in (None, "") else None
    except ValueError:
        s = None
    if s is None:
        return "Unknown"
    if s >= 9.0:
        return "Critical"
    if s >= 7.0:
        return "High"
    if s >= 4.0:
        return "Medium"
    if s > 0:
        return "Low"
    return "Informational"


BLOCKING = {"Critical", "High", "Unknown"}


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("report", help="osv-scanner --format json output")
    ap.add_argument("--advisory", action="store_true",
                    help="report only, never fail (for tooling that does not ship, e.g. Gemfile.lock)")
    ap.add_argument("--title", default="Dependency vulnerabilities")
    args = ap.parse_args()
    try:
        with open(args.report, encoding="utf-8") as f:
            report = json.load(f)
    except (OSError, ValueError) as e:
        print(f"error: cannot read {args.report}: {e}", file=sys.stderr)
        return 2

    rows = []
    scanned = 0
    for result in report.get("results", []):
        source = os.path.basename(result.get("source", {}).get("path", "?"))
        for pkg in result.get("packages", []):
            p = pkg.get("package", {})
            for group in pkg.get("groups", []):
                ids = group.get("aliases") or group.get("ids") or []
                rows.append({
                    "level": level(group.get("max_severity")),
                    "score": group.get("max_severity") or "n/a",
                    "package": f"{p.get('name')}@{p.get('version')}",
                    "ecosystem": p.get("ecosystem", "?"),
                    "ids": ", ".join(sorted(ids)[:4]),
                    "source": source,
                })
    for result in report.get("results", []):
        scanned += len(result.get("packages", []))

    order = ["Critical", "High", "Unknown", "Medium", "Low", "Informational"]
    rows.sort(key=lambda r: order.index(r["level"]))
    blocking = [] if args.advisory else [r for r in rows if r["level"] in BLOCKING]

    in_actions = os.environ.get("GITHUB_ACTIONS") == "true"
    for r in rows:
        msg = f"{r['level']} ({r['score']}) {r['ecosystem']} {r['package']} [{r['ids']}] in {r['source']}"
        print(("BLOCKING " if r in blocking else "warning  ") + msg)
        if in_actions:
            print(f"::{'error' if r in blocking else 'warning'} title=Vulnerable dependency::{msg}")
    if not rows:
        print("No known vulnerabilities in the scanned dependency set.")

    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as s:
            s.write(f"### {args.title}: {'❌ blocking findings' if blocking else '✅ no blocking findings'}\n\n")
            if rows:
                s.write("| Severity | CVSS | Package | Advisories | Source |\n|---|---|---|---|---|\n")
                for r in rows:
                    s.write(f"| {r['level']} | {r['score']} | `{r['package']}` | {r['ids']} | {r['source']} |\n")
                s.write("\nFix: upgrade the dependency (Dependabot may already have a PR). If it does not "
                        "apply to Fern Wallet, add an expiring entry with a reason to `osv-scanner.toml`.\n")
            else:
                s.write("No known vulnerabilities.\n")
            s.write("\n")
    return 1 if blocking else 0


if __name__ == "__main__":
    sys.exit(main())
