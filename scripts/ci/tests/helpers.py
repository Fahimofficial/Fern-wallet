"""Shared test helpers: temporary files and a clean GitHub Actions environment."""
from __future__ import annotations

import contextlib
import io
import os
import tempfile
import unittest
from unittest import mock


class ScriptTestCase(unittest.TestCase):
    """Runs each test in a temp directory with GitHub Actions env vars pointing at temp files."""

    def setUp(self) -> None:
        self._tmp = tempfile.TemporaryDirectory()
        self.tmp = self._tmp.name
        self.summary_path = os.path.join(self.tmp, "summary.md")
        env = {k: v for k, v in os.environ.items() if not k.startswith("GITHUB_")}
        env["GITHUB_STEP_SUMMARY"] = self.summary_path
        self._env = mock.patch.dict(os.environ, env, clear=True)
        self._env.start()

    def tearDown(self) -> None:
        self._env.stop()
        self._tmp.cleanup()

    def write(self, name: str, content: str | bytes) -> str:
        path = os.path.join(self.tmp, name)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        mode = "wb" if isinstance(content, bytes) else "w"
        with open(path, mode, **({} if isinstance(content, bytes) else {"encoding": "utf-8"})) as f:
            f.write(content)
        return path

    def summary(self) -> str:
        if not os.path.exists(self.summary_path):
            return ""
        with open(self.summary_path, encoding="utf-8") as f:
            return f.read()

    @staticmethod
    def run_main(main, argv: list[str]) -> tuple[int, str]:
        """Call a script's main(argv) and return (exit code, stdout)."""
        out = io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(io.StringIO()):
            code = main(argv)
        return code, out.getvalue()
