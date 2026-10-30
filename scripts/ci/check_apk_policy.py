#!/usr/bin/env python3
"""
Check a built APK against the security/privacy policy in .github/apk-policy.json.

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
# Only used to run aapt2 with an argument list (never a shell); see run_aapt2().
import subprocess  # nosec B404
import sys
from dataclasses import dataclass, field

import ci_common

COMPONENT_TAGS = ("activity", "activity-alias", "service", "receiver", "provider")
PERMISSION_TAGS = ("uses-permission", "uses-permission-sdk-23")
TRUE_VALUES = ("true", "0xffffffff", "-1")
FALSE_VALUES = ("false", "0x0", "0")

ELEMENT_RE = re.compile(r"^(?P<indent>\s*)E: (?P<tag>[\w.-]+)")
# A: http://schemas.android.com/apk/res/android:exported(0x01010010)=true
# A: http://schemas.android.com/apk/res/android:name(0x01010003)="x" (Raw: "x")
# A: package="com.example" (Raw: "com.example")
ATTR_RE = re.compile(
    r"^\s*A: (?:http://schemas\.android\.com/apk/res/android:)?(?P<name>[\w:-]+)"
    r"(?:\(0x[0-9a-f]+\))?=(?P<value>.*?)(?: \(Raw: \"(?P<raw>.*)\"\))?$"
)


class ToolError(RuntimeError):
    """aapt2 couldn't be run or its output couldn't be understood."""


@dataclass
class Element:
    tag: str
    depth: int
    attrs: dict[str, str] = field(default_factory=dict)
    children: list["Element"] = field(default_factory=list)

    def walk(self):
        yield self
        for child in self.children:
            yield from child.walk()


@dataclass
class Report:
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)
    notes: list[str] = field(default_factory=list)


def parse_bool(value: str | None) -> bool | None:
    if value is None:
        return None
    v = value.strip().strip('"').lower()
    v = v.removeprefix("(type 0x12)")
    if v in TRUE_VALUES:
        return True
    if v in FALSE_VALUES:
        return False
    return None


def parse_manifest(xmltree: str) -> Element:
    """Parse `aapt2 dump xmltree` text into an element tree."""
    root = Element("<root>", -1)
    stack = [root]
    for line in xmltree.splitlines():
        m = ELEMENT_RE.match(line)
        if m:
            depth = len(m.group("indent"))
            while stack[-1].depth >= depth:
                stack.pop()
            element = Element(m.group("tag"), depth)
            stack[-1].children.append(element)
            stack.append(element)
            continue
        m = ATTR_RE.match(line)
        if m and len(stack) > 1:
            value = m.group("raw") if m.group("raw") is not None else m.group("value").strip('"')
            stack[-1].attrs[m.group("name")] = value
    return root


def run_aapt2(aapt2: str, *args: str) -> str:
    """Run aapt2 with an argument list (never a shell) after checking it really is aapt2."""
    if os.path.basename(aapt2) != "aapt2" or not os.path.isfile(aapt2) or not os.access(aapt2, os.X_OK):
        raise ToolError(f"{aapt2!r} is not an executable aapt2 binary")
    cmd = [aapt2, *args]
    try:
        # Argument list without a shell, the binary is checked above, and the inputs are the job's
        # own build output passed by the workflow, so there is no untrusted input to inject.
        result = subprocess.run(cmd, check=True, capture_output=True, text=True)  # nosec B603 # nosemgrep
    except subprocess.CalledProcessError as e:
        raise ToolError(f"{' '.join(cmd)} failed:\n{e.stderr}") from e
    return result.stdout


def find_element(root: Element, tag: str) -> Element:
    found = next((e for e in root.walk() if e.tag == tag), None)
    if found is None:
        raise ToolError(f"no <{tag}> in aapt2 output")
    return found


def check_identity(manifest: Element, badging: str, expected: dict[str, str | None], report: Report) -> None:
    man = find_element(manifest, "manifest")
    m = re.search(r"application-label:'([^']*)'", badging)
    actual = {
        "application ID": man.attrs.get("package"),
        "application label": m.group(1) if m else None,
        "versionCode": man.attrs.get("versionCode"),
        "versionName": man.attrs.get("versionName"),
    }
    sdk = re.search(r"(?:^|\s)(?:min)?[sS]dkVersion:'(\d+)'", badging, re.MULTILINE)
    tsdk = re.search(r"targetSdkVersion:'(\d+)'", badging)
    native = re.search(r"native-code: (.*)", badging)
    report.notes.append(
        f"package={actual['application ID']} label={actual['application label']!r} "
        f"versionCode={actual['versionCode']} versionName={actual['versionName']} "
        f"minSdk={sdk.group(1) if sdk else '?'} targetSdk={tsdk.group(1) if tsdk else '?'}")
    report.notes.append(f"native ABIs: {native.group(1).replace(chr(39), '') if native else 'none'}")
    for what, want in expected.items():
        if want is not None and want != actual[what]:
            report.errors.append(f"{what} is {actual[what]!r}, expected {want!r}")


def check_flags(manifest: Element, allowed: dict, report: Report) -> None:
    man = find_element(manifest, "manifest")
    app = find_element(manifest, "application")
    if parse_bool(app.attrs.get("debuggable")) and not allowed.get("debuggable", False):
        report.errors.append("android:debuggable=true; release builds must not be debuggable")
    if parse_bool(app.attrs.get("testOnly")) or parse_bool(man.attrs.get("testOnly")):
        report.errors.append("android:testOnly=true")
    if parse_bool(app.attrs.get("usesCleartextTraffic")) and not allowed.get("usesCleartextTraffic", False):
        report.errors.append("android:usesCleartextTraffic=true; all traffic must be HTTPS")
    if parse_bool(app.attrs.get("allowBackup")) is not False and not allowed.get("allowBackup", False):
        report.errors.append("android:allowBackup is not false (PRIVACY.md promises no system backup)")
    if "sharedUserId" in man.attrs:
        report.errors.append(f"android:sharedUserId={man.attrs['sharedUserId']!r} must not be used")
    if "networkSecurityConfig" in app.attrs:
        report.warnings.append("a networkSecurityConfig is set; review it for cleartext or user-CA trust")


def requested_permissions(manifest: Element) -> list[str]:
    return sorted({e.attrs.get("name", "?") for e in manifest.walk() if e.tag in PERMISSION_TAGS})


def check_permissions(requested: list[str], allowed: dict, report: Report) -> None:
    for perm in requested:
        if perm not in allowed:
            report.errors.append(f"permission {perm} is not in the allowlist. If it is intended, add it to "
                                 f".github/apk-policy.json with the feature that needs it, and update PRIVACY.md")
    for perm in allowed:
        if perm not in requested:
            report.warnings.append(f"allowlisted permission {perm} is no longer requested; remove it from the policy")


def exported_components(manifest: Element) -> list[Element]:
    """Components another app can start or bind (explicitly exported, or pre-Android-12 default)."""
    app = find_element(manifest, "application")
    found = []
    for comp in (e for e in app.walk() if e.tag in COMPONENT_TAGS):
        exported = parse_bool(comp.attrs.get("exported"))
        if exported is None:
            exported = any(c.tag == "intent-filter" for c in comp.children)
        if exported:
            found.append(comp)
    return found


def check_components(components: list[Element], allowed: dict, report: Report) -> None:
    names = []
    for comp in components:
        name = comp.attrs.get("name", "?")
        perm = comp.attrs.get("permission")
        names.append(name)
        if name not in allowed:
            report.errors.append(f"exported {comp.tag} {name} (permission={perm}) is not in the allowlist. Make it "
                                 f"android:exported=\"false\", or review it and add it to .github/apk-policy.json")
            continue
        want = allowed[name].get("permission")
        if want != perm:
            report.errors.append(f"exported {comp.tag} {name} is guarded by {perm!r}, policy expects {want!r}")
    for name in allowed:
        if name not in names:
            report.warnings.append(f"allowlisted exported component {name} no longer exists; remove it from the policy")


def evaluate(xmltree: str, badging: str, policy: dict, expected: dict[str, str | None]) -> tuple[Report, list[str], int]:
    """Run every check; returns the report, requested permissions and exported component count."""
    manifest = parse_manifest(xmltree)
    report = Report()
    check_identity(manifest, badging, expected, report)
    check_flags(manifest, policy.get("application", {}), report)
    permissions = requested_permissions(manifest)
    check_permissions(permissions, policy.get("permissions", {}), report)
    components = exported_components(manifest)
    check_components(components, policy.get("exported_components", {}), report)
    return report, permissions, len(components)


def render_summary(apk: str, report: Report) -> str:
    out = [f"### APK policy: {'❌ failed' if report.errors else '✅ passed'}\n\n",
           f"`{os.path.basename(apk)}`: {report.notes[0] if report.notes else ''}\n\n"]
    out.extend(f"- ❌ {ci_common.md_cell(e)}\n" for e in report.errors)
    out.extend(f"- ⚠️ {ci_common.md_cell(w)}\n" for w in report.warnings)
    out.append("\n")
    return "".join(out)


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--apk", required=True)
    ap.add_argument("--aapt2", required=True, help="path to the aapt2 binary from Android build-tools")
    ap.add_argument("--policy", required=True, help="path to .github/apk-policy.json")
    ap.add_argument("--package", help="expected application ID")
    ap.add_argument("--label", help="expected application label")
    ap.add_argument("--version-code")
    ap.add_argument("--version-name")
    args = ap.parse_args(argv)

    expected = {"application ID": args.package, "application label": args.label,
                "versionCode": args.version_code, "versionName": args.version_name}
    try:
        if not os.path.isfile(args.apk):
            raise ToolError(f"APK not found: {args.apk}")
        with open(args.policy, encoding="utf-8") as f:
            policy = json.load(f)
        xmltree = run_aapt2(args.aapt2, "dump", "xmltree", "--file", "AndroidManifest.xml", args.apk)
        badging = run_aapt2(args.aapt2, "dump", "badging", args.apk)
        report, permissions, exported_count = evaluate(xmltree, badging, policy, expected)
    except (OSError, ValueError, ToolError) as e:
        print(f"error: {e}", file=sys.stderr)
        ci_common.annotate("error", "APK policy", str(e))
        return 2

    print(f"APK policy check: {args.apk}")
    for note in report.notes:
        print(f"  info: {note}")
    print(f"  permissions ({len(permissions)}): {', '.join(permissions)}")
    print(f"  exported components: {exported_count}")
    for w in report.warnings:
        print(f"  warning: {w}")
        ci_common.annotate("warning", "APK policy", w)
    for e in report.errors:
        print(f"  ERROR: {e}")
        ci_common.annotate("error", "APK policy", e)
    ci_common.append_summary(render_summary(args.apk, report))
    print("  result:", "FAILED" if report.errors else "passed")
    return 1 if report.errors else 0


if __name__ == "__main__":
    sys.exit(main())
