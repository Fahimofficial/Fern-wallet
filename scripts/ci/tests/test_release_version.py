import os
import unittest

import release_version
from tests.helpers import ScriptTestCase

CATALOG = '[versions]\nversion-name = "2026.10.27"\nversion-code = "207"\n'


class ParseTagTest(unittest.TestCase):
    def test_valid_tags(self):
        self.assertEqual(release_version.parse_tag("v2026.10.27"), ("2026.10.27", ""))
        self.assertEqual(release_version.parse_tag("v2026.10.27-rc.1"), ("2026.10.27", "-rc.1"))

    def test_rejects_malformed_tags_and_shell_metacharacters(self):
        bad = ["2026.10.27", "v2026.10.7", "v2026.10.27-", "v2026.10.27-$(id)", "v2026.10.27-a;b",
               "v2026.10.27-a b", "v2026.10.27-`x`", "v2026.10.27-" + "a" * 40, "release-1", "v2026.10.27\n"]
        for tag in bad:
            with self.subTest(tag=tag), self.assertRaises(release_version.VersionError):
                release_version.parse_tag(tag)


class ValidateTest(unittest.TestCase):
    def test_outputs_for_stable_and_prerelease(self):
        self.assertEqual(release_version.validate("v2026.10.27", CATALOG), {
            "version_name": "2026.10.27", "version_code": "207", "prerelease": "false",
            "artifact_base": "FernWallet-2026.10.27-207"})
        out = release_version.validate("v2026.10.27-rc.1", CATALOG)
        self.assertEqual(out["prerelease"], "true")
        self.assertEqual(out["artifact_base"], "FernWallet-2026.10.27-rc.1-207")

    def test_tag_must_match_version_name(self):
        with self.assertRaisesRegex(release_version.VersionError, "does not match"):
            release_version.validate("v2026.10.28", CATALOG)

    def test_bad_catalogs(self):
        for catalog in ('[versions]\nversion-name = "2026.10.27"\n', '[versions]\nversion-name = "2026.10.27"\n'
                        'version-code = "20a"\n', "not = [toml"):
            with self.subTest(catalog=catalog), self.assertRaises(release_version.VersionError):
                release_version.validate("v2026.10.27", catalog)


class CompareCodeTest(unittest.TestCase):
    def test_version_code_must_increase(self):
        self.assertEqual(release_version.compare_code(208, CATALOG, "v2026.10.27"), 207)
        for current in (207, 100):
            with self.subTest(current=current), self.assertRaisesRegex(release_version.VersionError, "greater"):
                release_version.compare_code(current, CATALOG, "v2026.10.27")


class MainTest(ScriptTestCase):
    def test_validate_writes_github_output(self):
        output = os.path.join(self.tmp, "output")
        os.environ["GITHUB_OUTPUT"] = output
        code, _ = self.run_main(release_version.main, ["validate", "--tag", "v2026.10.27",
                                                       "--catalog", self.write("libs.toml", CATALOG)])
        self.assertEqual(code, 0)
        with open(output, encoding="utf-8") as f:
            self.assertIn("version_code=207\n", f.read())

    def test_failures_exit_1(self):
        catalog = self.write("libs.toml", CATALOG)
        self.assertEqual(self.run_main(release_version.main, ["validate", "--tag", "v1-$(id)",
                                                              "--catalog", catalog])[0], 1)
        self.assertEqual(self.run_main(release_version.main, ["compare-code", "--current", "207",
                                                              "--previous-catalog", catalog,
                                                              "--previous-tag", "v2026.10.27"])[0], 1)


if __name__ == "__main__":
    unittest.main()
