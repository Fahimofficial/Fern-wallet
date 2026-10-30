import json
import unittest

import osv_gate
from tests.helpers import ScriptTestCase


def report(*groups: tuple[str, str]) -> dict:
    return {"results": [{"source": {"path": "/x/bom.json"}, "packages": [
        {"package": {"name": f"com.example:{name}", "version": "1.0", "ecosystem": "Maven"},
         "groups": [{"ids": [f"GHSA-{name}"], "aliases": [f"CVE-{name}", f"GHSA-{name}"], "max_severity": score}]}
        for name, score in groups
    ]}]}


class LevelTest(unittest.TestCase):
    def test_severity_boundaries(self):
        cases = {"10": "Critical", "9.0": "Critical", "8.9": "High", "7.0": "High", "6.9": "Medium",
                 "4.0": "Medium", "3.9": "Low", "0.1": "Low", "0": "Informational",
                 "": "Unknown", None: "Unknown", "n/a": "Unknown"}
        for score, expected in cases.items():
            with self.subTest(score=score):
                self.assertEqual(osv_gate.level(score), expected)


class OsvGateTest(ScriptTestCase):
    def gate(self, data, *flags) -> tuple[int, str]:
        path = self.write("osv.json", json.dumps(data) if not isinstance(data, str) else data)
        return self.run_main(osv_gate.main, [path, *flags])

    def test_no_findings_pass(self):
        code, out = self.gate({"results": []})
        self.assertEqual(code, 0)
        self.assertIn("No known vulnerabilities", out)

    def test_medium_and_low_only_warn(self):
        code, out = self.gate(report(("m", "5.3"), ("l", "2.0")))
        self.assertEqual(code, 0)
        self.assertNotIn("BLOCKING", out)

    def test_high_critical_and_unknown_block(self):
        for name, score in (("h", "7.5"), ("c", "9.8"), ("u", "")):
            with self.subTest(score=score):
                code, out = self.gate(report((name, score), ("m", "5.0")))
                self.assertEqual(code, 1)
                self.assertIn("BLOCKING", out)
                self.assertIn(f"com.example:{name}@1.0", self.summary())

    def test_advisory_mode_reports_but_never_fails(self):
        code, out = self.gate(report(("c", "9.8")), "--advisory")
        self.assertEqual(code, 0)
        self.assertIn("warning", out)

    def test_findings_are_sorted_worst_first(self):
        rows = osv_gate.findings(report(("l", "1.0"), ("c", "9.1"), ("m", "5.0")))
        self.assertEqual([r.level for r in rows], ["Critical", "Medium", "Low"])

    def test_malformed_reports_are_errors(self):
        for data in ("not json", "[]", json.dumps({"results": {"oops": 1}})):
            with self.subTest(data=data):
                self.assertEqual(self.gate(data)[0], 2)


if __name__ == "__main__":
    unittest.main()
