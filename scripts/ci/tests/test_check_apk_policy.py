import json
import os
import unittest

import check_apk_policy
from tests.helpers import ScriptTestCase

NS = "http://schemas.android.com/apk/res/android:"


def attr(name: str, value: str, resource_id: str = "0x01010003", raw: bool = True) -> str:
    if raw:
        return f'A: {NS}{name}({resource_id})="{value}" (Raw: "{value}")'
    return f"A: {NS}{name}({resource_id})={value}"


def element(depth: int, tag: str, *attrs: str) -> list[str]:
    pad = "  " * depth
    return [f"{pad}E: {tag} (line=1)"] + [f"{pad}  {a}" for a in attrs]


def manifest(permissions=("android.permission.INTERNET",), app_flags=None, components=None) -> str:
    """Build `aapt2 dump xmltree` output in the format aapt2 37 prints."""
    flags = {"allowBackup": "false", "usesCleartextTraffic": "false"} if app_flags is None else app_flags
    lines = ["N: android=http://schemas.android.com/apk/res/android (line=2)"]
    lines += element(1, "manifest", attr("versionCode", "206", "0x0101021b", raw=False),
                     attr("versionName", "2025.07.17", "0x0101021c"),
                     'A: package="com.fernwallet.app" (Raw: "com.fernwallet.app")')
    for p in permissions:
        lines += element(3, "uses-permission", attr("name", p))
    lines += element(3, "application", *[attr(k, v, "0x01010280", raw=False) for k, v in flags.items()])
    for comp in components if components is not None else default_components():
        lines += comp
    return "\n".join(lines) + "\n"


def default_components() -> list[list[str]]:
    return [
        element(5, "activity", attr("name", "com.example.Main"), attr("exported", "true", "0x01010010", raw=False))
        + element(7, "intent-filter"),
        element(5, "service", attr("name", "com.example.Tile"),
                attr("permission", "android.permission.BIND_QUICK_SETTINGS_TILE", "0x01010006"),
                attr("exported", "true", "0x01010010", raw=False)),
        element(5, "receiver", attr("name", "com.example.Internal"), attr("exported", "false", "0x01010010", raw=False))
        + element(7, "intent-filter"),
    ]


BADGING = ("package: name='com.fernwallet.app' versionCode='206' versionName='2025.07.17'\n"
           "minSdkVersion:'28'\ntargetSdkVersion:'34'\napplication-label:'Fern Wallet'\n"
           "native-code: 'arm64-v8a' 'x86_64'\n")

POLICY = {
    "application": {"debuggable": False, "usesCleartextTraffic": False, "allowBackup": False},
    "permissions": {"android.permission.INTERNET": "network"},
    "exported_components": {
        "com.example.Main": {"reason": "launcher"},
        "com.example.Tile": {"permission": "android.permission.BIND_QUICK_SETTINGS_TILE", "reason": "tile"},
    },
}

EXPECTED = {"application ID": "com.fernwallet.app", "application label": "Fern Wallet",
            "versionCode": "206", "versionName": "2025.07.17"}


class EvaluateTest(unittest.TestCase):
    def evaluate(self, xmltree=None, policy=None, expected=None, badging=BADGING):
        report, permissions, exported = check_apk_policy.evaluate(
            xmltree or manifest(), badging, policy or POLICY, EXPECTED if expected is None else expected)
        return report, permissions, exported

    def test_compliant_apk_passes(self):
        report, permissions, exported = self.evaluate()
        self.assertEqual(report.errors, [])
        self.assertEqual(report.warnings, [])
        self.assertEqual(permissions, ["android.permission.INTERNET"])
        self.assertEqual(exported, 2)
        self.assertIn("minSdk=28 targetSdk=34", report.notes[0])

    def test_identity_and_version_mismatch(self):
        report, _, _ = self.evaluate(expected={**EXPECTED, "application ID": "com.fernwallet.app.debug",
                                               "versionCode": "207"})
        self.assertEqual(len(report.errors), 2)
        self.assertTrue(any("application ID" in e for e in report.errors))
        self.assertTrue(any("versionCode" in e for e in report.errors))

    def test_unsafe_application_flags(self):
        cases = {
            "debuggable": {"allowBackup": "false", "debuggable": "true"},
            "usesCleartextTraffic": {"allowBackup": "false", "usesCleartextTraffic": "true"},
            "allowBackup": {"usesCleartextTraffic": "false"},
        }
        for flag, flags in cases.items():
            with self.subTest(flag=flag):
                report, _, _ = self.evaluate(manifest(app_flags=flags))
                self.assertEqual(len(report.errors), 1, report.errors)
                self.assertIn(flag, report.errors[0])

    def test_new_permission_fails_and_removed_permission_warns(self):
        report, _, _ = self.evaluate(manifest(permissions=("android.permission.CAMERA",)))
        self.assertTrue(any("android.permission.CAMERA is not in the allowlist" in e for e in report.errors))
        self.assertTrue(any("android.permission.INTERNET is no longer requested" in w for w in report.warnings))

    def test_unlisted_exported_component_fails(self):
        extra = element(5, "provider", attr("name", "com.example.Files"),
                        attr("exported", "true", "0x01010010", raw=False))
        report, _, exported = self.evaluate(manifest(components=default_components() + [extra]))
        self.assertEqual(exported, 3)
        self.assertTrue(any("exported provider com.example.Files" in e for e in report.errors))

    def test_component_with_wrong_guarding_permission_fails(self):
        policy = json.loads(json.dumps(POLICY))
        policy["exported_components"]["com.example.Tile"]["permission"] = "android.permission.SOMETHING_ELSE"
        report, _, _ = self.evaluate(policy=policy)
        self.assertTrue(any("guarded by" in e for e in report.errors))

    def test_component_without_exported_attribute_but_with_intent_filter_counts_as_exported(self):
        legacy = element(5, "receiver", attr("name", "com.example.Legacy")) + element(7, "intent-filter")
        report, _, _ = self.evaluate(manifest(components=default_components() + [legacy]))
        self.assertTrue(any("com.example.Legacy" in e for e in report.errors))

    def test_missing_application_is_a_tool_error(self):
        with self.assertRaises(check_apk_policy.ToolError):
            self.evaluate(xmltree="N: android=x\n  E: manifest (line=2)\n")


class MainTest(ScriptTestCase):
    def fake_aapt2(self, xmltree: str) -> str:
        """An executable named aapt2 that prints canned `dump xmltree` / `dump badging` output."""
        self.write("xmltree.txt", xmltree)
        self.write("badging.txt", BADGING)
        path = self.write("bin/aapt2", "#!/bin/sh\n"
                          f'if [ "$2" = "xmltree" ]; then cat "{self.tmp}/xmltree.txt"; '
                          f'else cat "{self.tmp}/badging.txt"; fi\n')
        os.chmod(path, 0o700)
        return path

    def args(self, aapt2: str) -> list[str]:
        return ["--apk", self.write("app.apk", b"PK"), "--aapt2", aapt2,
                "--policy", self.write("policy.json", json.dumps(POLICY)),
                "--package", "com.fernwallet.app", "--label", "Fern Wallet"]

    def test_end_to_end_pass_and_fail(self):
        code, out = self.run_main(check_apk_policy.main, self.args(self.fake_aapt2(manifest())))
        self.assertEqual(code, 0, out)
        self.assertIn("result: passed", out)

        code, out = self.run_main(check_apk_policy.main,
                                  self.args(self.fake_aapt2(manifest(app_flags={"allowBackup": "false",
                                                                                "debuggable": "true"}))))
        self.assertEqual(code, 1)
        self.assertIn("debuggable", self.summary())

    def test_refuses_a_binary_that_is_not_aapt2(self):
        not_aapt2 = self.write("bin/sh-wrapper", "#!/bin/sh\necho hi\n")
        os.chmod(not_aapt2, 0o700)
        code, _ = self.run_main(check_apk_policy.main, self.args(not_aapt2))
        self.assertEqual(code, 2)

    def test_missing_apk_is_a_tool_error(self):
        args = self.args(self.fake_aapt2(manifest()))
        args[1] = os.path.join(self.tmp, "missing.apk")
        self.assertEqual(self.run_main(check_apk_policy.main, args)[0], 2)


if __name__ == "__main__":
    unittest.main()
