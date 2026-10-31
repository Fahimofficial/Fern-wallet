import json
import os
import unittest
import zipfile

import check_codeql_coverage
from tests.helpers import ScriptTestCase


class CodeqlCoverageTest(ScriptTestCase):
    def setUp(self):
        super().setUp()
        self.repo = os.path.join(self.tmp, "repo")
        self.sources = [f"feature/home/src/main/java/com/x/F{i}.kt" for i in range(10)]
        for path in self.sources:
            self.write(f"repo/{path}", "class X")
        for ignored in ("feature/home/src/test/java/T.kt", "ci-actions/base/src/main/kotlin/C.kt",
                        "shared/ui/testing/src/main/java/U.kt", "feature/home/build/generated/src/main/G.kt"):
            self.write(f"repo/{ignored}", "class Y")

    def database(self, extracted: list[str]) -> str:
        db = os.path.join(self.tmp, "db")
        os.makedirs(db, exist_ok=True)
        prefix = os.path.realpath(self.repo).lstrip("/")
        with zipfile.ZipFile(os.path.join(db, "src.zip"), "w") as z:
            for path in extracted:
                z.writestr(f"{prefix}/{path}", "class X")
            z.writestr(f"{prefix}/feature/home/src/main/java/com/x/Other.java", "class J {}")
        return json.dumps({"java": db})

    def check(self, extracted: list[str]) -> tuple[int, str]:
        return self.run_main(check_codeql_coverage.main, ["--db-locations", self.database(extracted),
                                                          "--repo", self.repo, "--min-ratio", "0.9"])

    def test_expected_sources_exclude_tests_tools_and_build_output(self):
        self.assertEqual(check_codeql_coverage.expected_sources(check_codeql_coverage.Path(self.repo)),
                         set(self.sources))

    def test_full_coverage_passes(self):
        code, out = self.check(self.sources)
        self.assertEqual(code, 0)
        self.assertIn("10 of 10", out)

    def test_missing_kotlin_fails(self):
        code, out = self.check(self.sources[:5])
        self.assertEqual(code, 1)
        self.assertIn("5 of 10", out)
        code, _ = self.check([])
        self.assertEqual(code, 1)

    def test_bad_db_locations(self):
        for value in ("not json", "[]", json.dumps({"python": "/x"})):
            with self.subTest(value=value):
                code, _ = self.run_main(check_codeql_coverage.main, ["--db-locations", value, "--repo", self.repo])
                self.assertEqual(code, 1)


if __name__ == "__main__":
    unittest.main()
