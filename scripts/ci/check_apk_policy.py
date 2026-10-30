#!/usr/bin/env python3
"""Check a built APK against the security/privacy policy in .github/apk-policy.json.

It reads the manifest that actually ships (after library manifests are merged) with
`aapt2 dump`, so a dependency that silently adds a permission or an exported component
fails the check and has to be reviewed and added to the allowlist in a pull request.

Checks:
  * package name, app label, versionCode/versionName (when given)
  * not debuggable, not testOnly, no cleartext traffic, Android backup disabled, no sharedUserId
  * every requested permission is allowlisted (new permissions fail; removed ones warn)
  * every exported component is allowlisted, with the expected guarding permission

Exit code 0 = policy satisfied, 1 = violations, 2 = usage/tool error.
Only the Python standard library is used.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
from dataclasses import dataclass, field

COMPONENT_TAGS = ("activity", "activity-alias", "service", "receiver", "provider")
PERMISSION_TAGS = ("uses-permission", "uses-permission-sdk-23")

ELEMENT_RE = re.compile(r"^(?P<indent>\s*)E: (?P<tag>[\w.-]+)")
# A: http://schemas.android.com/apk/res/android:exported(0x01010010)=true
# A: http://schemas.android.com/apk/res/android:name(0x01010003)="x" (Raw: "x")
# A: package="com.example" (Raw: "com.example")
ATTR_RE = re.compile(
    r"^\s*A: (?:http://schemas\.android\.com/apk/res/android:)?(?P<name>[\w:-]+)"
    r"(?:\(0x[0-9a-f]+\))?=(?P<value>.*?)(?: \(Raw: \"(?P<raw>.*)\"\))?$"
)


@dataclass
class Element:
    tag: str
    depth: int
    attrs: dict[str, str] = field(default_factory=dict)
    children: list["Element"] = field(default_factory=list)


def parse_bool(value: str | None) -> bool | None:
    if value is None:
        return None
    v = value.strip().strip('"').lower()
    if v in ("true", "0xffffffff", "(type 0x12)0xffffffff", "-1"):
        return True
    if v in ("false", "0x0", "(type 0x12)0x0", "0"):
        return False
    return None


def parse_manifest(xmltree: str) -> Element:
    root = Element("<root>", -1)
    stack = [root]
    for line in xmltree.splitlines():
        m = ELEMENT_RE.match(line)
        if m:
            depth = len(m.group("indent"))
            while stack[-1].depth >= depth:
                stack.pop()
            el = Element(m.group("tag"), depth)
            stack[-1].children.append(el)
            stack.append(el)
            continue
        m = ATTR_RE.match(line)
        if m and len(stack) > 1:
            value = m.group("raw") if m.group("raw") is not None else m.group("value").strip('"')
            stack[-1].attrs[m.group("name")] = value
    return root


def walk(el: Element):
    yield el
    for c in el.children:
        yield from walk(c)


def run(cmd: list[str]) -> str:
    try:
        return subprocess.run(cmd, check=True, capture_output=True, text=True).stdout
    except FileNotFoundError:
        sys.exit(f"error: tool not found: {cmd[0]}")
    except subprocess.CalledProcessError as e:
        sys.exit(f"error: {' '.join(cmd)} failed:\n{e.stderr}")


def gh_annotation(level: str, msg: str) -> None:
    if os.environ.get("GITHUB_ACTIONS") == "true":
        print(f"::{level} title=APK policy::{msg}")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--apk", required=True)
    ap.add_argument("--aapt2", required=True, help="path to the aapt2 binary from Android build-tools")
    ap.add_argument("--policy", required=True, help="path to .github/apk-policy.json")
    ap.add_argument("--package", help="expected application ID")
    ap.add_argument("--label", help="expected application label")
    ap.add_argument("--version-code")
    ap.add_argument("--version-name")
    args = ap.parse_args()

    with open(args.policy, encoding="utf-8") as f:
        policy = json.load(f)

    manifest = parse_manifest(run([args.aapt2, "dump", "xmltree", "--file", "AndroidManifest.xml", args.apk]))
    badging = run([args.aapt2, "dump", "badging", args.apk])

    errors: list[str] = []
    warnings: list[str] = []
    notes: list[str] = []

    man = next((e for e in walk(manifest) if e.tag == "manifest"), None)
    app = next((e for e in walk(manifest) if e.tag == "application"), None)
    if man is None or app is None:
        print("error: could not find <manifest>/<application> in aapt2 output", file=sys.stderr)
        return 2

    # --- identity ---------------------------------------------------------------------------
    pkg = man.attrs.get("package")
    m = re.search(r"application-label:'([^']*)'", badging)
    label = m.group(1) if m else None
    vc, vn = man.attrs.get("versionCode"), man.attrs.get("versionName")
    sdk = re.search(r"(?:^|\s)(?:min)?[sS]dkVersion:'(\d+)'", badging, re.MULTILINE)
    tsdk = re.search(r"targetSdkVersion:'(\d+)'", badging)
    native = re.search(r"native-code: (.*)", badging)
    notes.append(f"package={pkg} label={label!r} versionCode={vc} versionName={vn} "
                 f"minSdk={sdk.group(1) if sdk else '?'} targetSdk={tsdk.group(1) if tsdk else '?'}")
    notes.append(f"native ABIs: {native.group(1).replace(chr(39), '') if native else 'none'}")
    for expected, actual, what in (
        (args.package, pkg, "application ID"),
        (args.label, label, "application label"),
        (args.version_code, vc, "versionCode"),
        (args.version_name, vn, "versionName"),
    ):
        if expected is not None and expected != actual:
            errors.append(f"{what} is {actual!r}, expected {expected!r}")

    # --- application flags --------------------------------------------------------------------
    flags = policy.get("application", {})
    if parse_bool(app.attrs.get("debuggable")) and not flags.get("debuggable", False):
        errors.append("android:debuggable=true; release builds must not be debuggable")
    if parse_bool(app.attrs.get("testOnly")) or parse_bool(man.attrs.get("testOnly")):
        errors.append("android:testOnly=true")
    if parse_bool(app.attrs.get("usesCleartextTraffic")) and not flags.get("usesCleartextTraffic", False):
        errors.append("android:usesCleartextTraffic=true; all traffic must be HTTPS")
    if parse_bool(app.attrs.get("allowBackup")) is not False and not flags.get("allowBackup", False):
        errors.append("android:allowBackup is not false (PRIVACY.md promises no system backup)")
    if "sharedUserId" in man.attrs:
        errors.append(f"android:sharedUserId={man.attrs['sharedUserId']!r} must not be used")
    if "networkSecurityConfig" in app.attrs:
        warnings.append("a networkSecurityConfig is set; review it for cleartext or user-CA trust")

    # --- permissions --------------------------------------------------------------------------
    requested = sorted({e.attrs.get("name", "?") for e in walk(manifest) if e.tag in PERMISSION_TAGS})
    allowed = policy.get("permissions", {})
    for perm in requested:
        if perm not in allowed:
            errors.append(f"permission {perm} is not in the allowlist. If it is intended, add it to "
                          f".github/apk-policy.json with the feature that needs it, and update PRIVACY.md")
    for perm in allowed:
        if perm not in requested:
            warnings.append(f"allowlisted permission {perm} is no longer requested; remove it from the policy")

    # --- exported components ------------------------------------------------------------------
    allowed_components = policy.get("exported_components", {})
    exported_found = []
    for comp in (e for e in walk(app) if e.tag in COMPONENT_TAGS):
        name = comp.attrs.get("name", "?")
        has_filter = any(c.tag == "intent-filter" for c in comp.children)
        exported = parse_bool(comp.attrs.get("exported"))
        if exported is None:
            exported = has_filter  # pre-Android-12 default
        if not exported:
            continue
        perm = comp.attrs.get("permission")
        exported_found.append(name)
        if name not in allowed_components:
            errors.append(f"exported {comp.tag} {name} (permission={perm}) is not in the allowlist. Make it "
                          f"android:exported=\"false\", or review it and add it to .github/apk-policy.json")
            continue
        want = allowed_components[name].get("permission")
        if want != perm:
            errors.append(f"exported {comp.tag} {name} is guarded by {perm!r}, policy expects {want!r}")
    for name in allowed_components:
        if name not in exported_found:
            warnings.append(f"allowlisted exported component {name} no longer exists; remove it from the policy")

    # --- report ---------------------------------------------------------------------------------
    print(f"APK policy check: {args.apk}")
    for n in notes:
        print(f"  info: {n}")
    print(f"  permissions ({len(requested)}): {', '.join(requested)}")
    print(f"  exported components: {len(exported_found)}")
    for w in warnings:
        print(f"  warning: {w}")
        gh_annotation("warning", w)
    for e in errors:
        print(f"  ERROR: {e}")
        gh_annotation("error", e)

    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as s:
            s.write(f"### APK policy: {'❌ failed' if errors else '✅ passed'}\n\n")
            s.write(f"`{os.path.basename(args.apk)}`: {notes[0]}\n\n")
            for e in errors:
                s.write(f"- ❌ {e}\n")
            for w in warnings:
                s.write(f"- ⚠️ {w}\n")
            s.write("\n")

    print("  result:", "FAILED" if errors else "passed")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
