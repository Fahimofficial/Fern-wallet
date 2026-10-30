import os
import unittest

import junit_summary
from tests.helpers import ScriptTestCase

PASSING = """<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.ivy.A" tests="2" failures="0" errors="0">
  <testcase name="one" classname="com.ivy.A"/>
  <testcase name="two" classname="com.ivy.A"><skipped/></testcase>
</testsuite>"""

FAILING = """<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.ivy.B" tests="1" failures="1" errors="0">
  <testcase name="breaks" classname="com.ivy.B">
    <failure message="expected:&lt;1&gt; but was:&lt;2&gt;">java.lang.AssertionError
at com.ivy.B.breaks</failure>
  </testcase>
</testsuite>"""

EMPTY = """<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="connected" tests="0" failures="0" errors="0"/>"""

SUITE_ERROR = """<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="connected" tests="0" failures="0" errors="1"/>"""


class JunitSummaryTest(ScriptTestCase):
    def results(self, **files):
        for name, content in files.items():
            self.write(f"mod/build/test-results/{name}.xml", content)
        return os.path.join(self.tmp, "**/build/test-results/**/*.xml")

    def test_passing_results(self):
        pattern = self.results(a=PASSING)
        code, out = self.run_main(junit_summary.main, ["Unit", pattern, "--min-tests", "1"])
        self.assertEqual(code, 0)
        self.assertIn("2 tests, 0 failed, 1 skipped", out)
        self.assertIn("✅", self.summary())

    def test_failed_test_fails_and_is_listed(self):
        pattern = self.results(a=PASSING, b=FAILING)
        code, out = self.run_main(junit_summary.main, ["Unit", pattern])
        self.assertEqual(code, 1)
        self.assertIn("FAILED", out)
        self.assertIn("breaks", self.summary())
        self.assertIn("expected:<1> but was:<2>", self.summary())

    def test_no_results_fails_with_min_tests(self):
        code, out = self.run_main(junit_summary.main, ["Unit", os.path.join(self.tmp, "nothing/*.xml"),
                                                       "--min-tests", "1"])
        self.assertEqual(code, 1)
        self.assertIn("only 0 test(s) ran", out)

    def test_result_file_without_tests_fails_when_required(self):
        pattern = self.results(a=PASSING, b=EMPTY)
        self.assertEqual(self.run_main(junit_summary.main, ["Android", pattern])[0], 0)
        code, out = self.run_main(junit_summary.main, ["Android", pattern, "--require-tests-per-file"])
        self.assertEqual(code, 1)
        self.assertIn("no test cases in", out)

    def test_missing_module_results_fail_with_min_files(self):
        pattern = self.results(a=PASSING)
        code, out = self.run_main(junit_summary.main, ["Android", pattern, "--min-files", "2"])
        self.assertEqual(code, 1)
        self.assertIn("expected at least 2", out)

    def test_suite_level_errors_without_test_cases_fail(self):
        pattern = self.results(a=SUITE_ERROR)
        code, _ = self.run_main(junit_summary.main, ["Android", pattern])
        self.assertEqual(code, 1)

    def test_unreadable_or_hostile_xml_fails(self):
        pattern = self.results(a=PASSING, b='<!DOCTYPE x [<!ENTITY e SYSTEM "file:///etc/passwd">]><testsuite/>')
        code, out = self.run_main(junit_summary.main, ["Unit", pattern])
        self.assertEqual(code, 1)
        self.assertIn("unreadable result file", out)


if __name__ == "__main__":
    unittest.main()
