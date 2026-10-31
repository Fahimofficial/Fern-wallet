#!/usr/bin/env python3
"""Release version checks used by .github/workflows/release.yml.

validate      Check the tag format (v<YYYY.MM.DD>[-suffix]) and that it matches version-name in
              gradle/libs.versions.toml. Prints the outputs and appends them to $GITHUB_OUTPUT.
compare-code  Check version-code is greater than the one in the previous release's catalog.

The tag charset is strict on purpose: the tag ends up in file names, release titles and
shell commands. Exit code 0 = ok, 1 = check failed.
"""
from __future__ import annotations

import argparse
import os
import re
import sys
import tomllib

import ci_common

# Matched with fullmatch(): "$" would also accept a trailing newline.
TAG_RE = re.compile(r"v(?P<base>[0-9]{4}\.[0-9]{2}\.[0-9]{2})(?P<suffix>-[0-9A-Za-z][0-9A-Za-z.]{0,30})?")


class VersionError(ValueError):
    """A release precondition isn't met; the message says how to fix it."""


def parse_tag(tag: str) -> tuple[str, str]:
    """Return (version-name, suffix) for a valid release tag."""
    m = TAG_RE.fullmatch(tag)
    if not m:
        raise VersionError(f"'{tag}' must be v<YYYY.MM.DD> or v<YYYY.MM.DD>-<suffix> (letters, digits, dots)")
    return m.group("base"), m.group("suffix") or ""


def read_versions(catalog_text: str) -> tuple[str, int]:
    """Return (version-name, version-code) from the version catalog."""
    try:
        versions = tomllib.loads(catalog_text)["versions"]
        name, code = versions["version-name"], versions["version-code"]
    except (tomllib.TOMLDecodeError, KeyError, TypeError) as e:
        raise VersionError(f"cannot read version-name/version-code from the version catalog: {e}") from e
    if not re.fullmatch(r"[0-9]+", str(code)):
        raise VersionError(f"version-code '{code}' is not an integer")
    return str(name), int(code)


def validate(tag: str, catalog_text: str) -> dict[str, str]:
    """Validate the tag against the catalog and return the workflow outputs."""
    base, suffix = parse_tag(tag)
    name, code = read_versions(catalog_text)
    if base != name:
        raise VersionError(f"tag {tag} does not match version-name {name} in gradle/libs.versions.toml")
    return {
        "version_name": name,
        "version_code": str(code),
        "prerelease": "true" if suffix else "false",
        "artifact_base": f"FernWallet-{tag[1:]}-{code}",
    }


def compare_code(current: int, previous_catalog_text: str, previous_tag: str) -> int:
    """Raise unless current is greater than the previous release's version-code; return the previous code."""
    _, previous = read_versions(previous_catalog_text)
    if current <= previous:
        raise VersionError(f"version-code {current} must be greater than {previous} ({previous_tag}); "
                           f"Android refuses to install updates otherwise")
    return previous


def _read(path: str) -> str:
    with open(path, encoding="utf-8") as f:
        return f.read()


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="command", required=True)
    v = sub.add_parser("validate")
    v.add_argument("--tag", required=True)
    v.add_argument("--catalog", required=True)
    c = sub.add_parser("compare-code")
    c.add_argument("--current", required=True, type=int)
    c.add_argument("--previous-catalog", required=True)
    c.add_argument("--previous-tag", required=True)
    args = ap.parse_args(argv)

    try:
        if args.command == "validate":
            outputs = validate(args.tag, _read(args.catalog))
            lines = "".join(f"{k}={val}\n" for k, val in outputs.items())
            print(lines, end="")
            if os.environ.get("GITHUB_OUTPUT"):
                with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as out:
                    out.write(lines)
        else:
            previous = compare_code(args.current, _read(args.previous_catalog), args.previous_tag)
            print(f"version-code {args.current} > {previous} ({args.previous_tag})")
    except (OSError, VersionError) as e:
        print(f"ERROR: {e}")
        ci_common.annotate("error", "Release version", str(e))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
