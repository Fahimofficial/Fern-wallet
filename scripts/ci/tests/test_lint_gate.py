import os
import unittest

import lint_gate
from tests.helpers import ScriptTestCase


def lint_xml(*issues: tuple[str, str]) -> str:
    body = "".join(
        f'<issue id="{issue_id}" severity="{severity}" category="Security" message="Problem {issue_id}">'
        f'<location file="{{root}}/app/src/Main.kt" line="12"/></issue>'
        for issue_id, severity in issues
    )
    return f'<?xml version="1.0" encoding="UTF-8"?><issues format="6">{body}</issues>'


class LintGateTest(ScriptTestCase):
    def report(self, *issues):
        return self.write("lint.xml", lint_xml(*issues).replace("{root}", os.getcwd()))

    def test_only_warnings_pass(self):
        code, out = self.run_main(lint_gate.main, [self.report(("UnusedResources", "Warning"), ("X", "Hint"))])
        self.assertEqual(code, 0)
        self.assertIn("Hint=1, Warning=1", out)
        self.assertIn("no Error/Fatal", self.summary())

    def test_error_and_fatal_block_with_relative_location(self):
        code, out = self.run_main(lint_gate.main, [self.report(("SecureRandom", "Error"), ("Fatal1", "Fatal"))])
        self.assertEqual(code, 1)
        self.assertIn("app/src/Main.kt:12: [SecureRandom]", out)
        self.assertIn("2 blocking issue(s)", self.summary())

    def test_empty_report_passes(self):
        code, _ = self.run_main(lint_gate.main, [self.report()])
        self.assertEqual(code, 0)

    def test_missing_report_fails(self):
        code, out = self.run_main(lint_gate.main, [os.path.join(self.tmp, "missing.xml")])
        self.assertEqual(code, 1)
        self.assertIn("not found", out)

    def test_malformed_or_hostile_report_fails(self):
        for content in ("<issues><issue", '<!DOCTYPE x [<!ENTITY a "b">]><issues/>'):
            with self.subTest(content=content):
                code, out = self.run_main(lint_gate.main, [self.write("bad.xml", content)])
                self.assertEqual(code, 1)
                self.assertIn("cannot read", out)


if __name__ == "__main__":
    unittest.main()
