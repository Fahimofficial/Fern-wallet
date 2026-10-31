#!/usr/bin/env python3
"""Fail if a CodeQL java-kotlin database doesn't contain the app's Kotlin sources."""

from __future__ import annotations

import argparse
import json
import os
import sys
import zipfile
from pathlib import Path

import ci_common

_DETAILS = """\
CodeQL's Kotlin extractor supports Kotlin versions with a delay. With an unsupported compiler it
can skip files without failing the build, which would leave Kotlin code unanalysed behind a green
check. This compares the Kotlin files in the database's source archive with the Kotlin files the
analysed build compiles (src/main of every module except CI tooling, buildSrc and test-support
modules) and fails below --min-ratio.

Usage: check_codeql_coverage.py --db-locations '<analyze step db-locations output>' [--min-ratio 0.9]
"""

# Not compiled by `:app:assembleDebug`: CI tools, build logic and test-only helper modules.
EXCLUDED_PREFIXES = ("ci-actions/", "buildSrc/", "shared/data/model-testing/", "shared/ui/testing/",
                     "shared/data/core-testing/")


def expected_sources(repo: Path) -> set[str]:
    """Repository-relative paths of Kotlin files the CodeQL build is expected to compile."""
    found = set()
    for path in repo.rglob("*.kt"):
        rel = path.relative_to(repo).as_posix()
        if "/src/main/" in rel and "/build/" not in rel and not rel.startswith(EXCLUDED_PREFIXES):
            found.add(rel)
    return found


def archived_sources(db: Path, repo: Path) -> set[str]:
    """Repository-relative paths of Kotlin files in the database source archive (src.zip or src/)."""
    repo_part = repo.resolve().as_posix().lstrip("/") + "/"
    names: list[str] = []
    archive = db / "src.zip"
    if archive.is_file():
        with zipfile.ZipFile(archive) as z:
            names = z.namelist()
    elif (db / "src").is_dir():
        names = [p.relative_to(db / "src").as_posix() for p in (db / "src").rglob("*.kt")]
    found = set()
    for name in names:
        name = name.lstrip("/")
        if name.endswith(".kt") and name.startswith(repo_part):
            found.add(name[len(repo_part):])
    return found


def java_database(db_locations: str) -> Path:
    try:
        locations = json.loads(db_locations)
    except ValueError as e:
        raise ValueError(f"db-locations is not JSON: {e}") from e
    if not isinstance(locations, dict):
        raise ValueError("db-locations must be a JSON object")
    path = locations.get("java") or locations.get("java-kotlin")
    if not path:
        raise ValueError(f"no java database in db-locations ({', '.join(locations) or 'empty'})")
    return Path(path)


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(
        description=f"{__doc__}\n\n{_DETAILS}", formatter_class=argparse.RawDescriptionHelpFormatter
    )
    ap.add_argument("--db-locations", required=True)
    ap.add_argument("--repo", default=os.getcwd())
    ap.add_argument("--min-ratio", type=float, default=0.9)
    args = ap.parse_args(argv)
    repo = Path(args.repo)
    try:
        db = java_database(args.db_locations)
        expected = expected_sources(repo)
        extracted = archived_sources(db, repo) & expected
    except (OSError, ValueError, zipfile.BadZipFile) as e:
        print(f"ERROR: {e}")
        ci_common.annotate("error", "CodeQL coverage", str(e))
        return 1

    ratio = len(extracted) / len(expected) if expected else 0.0
    message = f"CodeQL analysed {len(extracted)} of {len(expected)} Kotlin source files ({ratio:.0%})"
    print(message)
    ok = bool(expected) and ratio >= args.min_ratio
    if not ok:
        missing = sorted(expected - extracted)[:20]
        print("Not in the database (first 20): " + ", ".join(missing))
        ci_common.annotate("error", "CodeQL coverage",
                           f"{message}; expected at least {args.min_ratio:.0%}. The Kotlin extractor probably doesn't "
                           f"support this Kotlin version yet (docs/CI_CD.md → Notes).")
    ci_common.append_summary(f"### {'✅' if ok else '❌'} {message}\n\n")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
