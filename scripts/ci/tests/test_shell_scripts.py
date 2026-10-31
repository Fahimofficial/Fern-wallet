"""Tests for the shell scripts, with fake `gh`, `adb` and `gradlew` commands on PATH."""
import os
import stat
import shutil
# Tests run this repository's scripts with fixed argument lists.
import subprocess  # nosec B404
import unittest

from tests.helpers import ScriptTestCase

SCRIPTS = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASH = shutil.which("bash") or "/bin/bash"
SHA_A = "a" * 40
SHA_B = "b" * 40


class ShellScriptTestCase(ScriptTestCase):
    def setUp(self):
        super().setUp()
        self.bin = os.path.join(self.tmp, "bin")
        os.makedirs(self.bin)

    def fake(self, name: str, body: str) -> None:
        path = os.path.join(self.bin, name)
        with open(path, "w", encoding="utf-8") as f:
            f.write("#!/usr/bin/env bash\n" + body)
        os.chmod(path, stat.S_IRWXU)

    def run_script(self, script: str, *args: str, cwd: str | None = None) -> subprocess.CompletedProcess:
        env = {**os.environ, "PATH": f"{self.bin}:{os.environ.get('PATH', '')}",
               "GITHUB_REPOSITORY": "owner/repo", "WAIT_TIMEOUT_SECONDS": "2", "WAIT_INTERVAL_SECONDS": "1"}
        cmd = [BASH, os.path.join(SCRIPTS, script), *args]
        return subprocess.run(cmd, env=env, cwd=cwd or self.tmp, capture_output=True,  # nosec B603 # nosemgrep
                              text=True, timeout=60, check=False)


class WaitForWorkflowsTest(ShellScriptTestCase):
    def gh_runs(self, **runs: str) -> None:
        # runs: workflow file stem -> "status conclusion" (empty = no run yet)
        cases = "".join(f'  *"/{wf}.yml/runs"*) echo "{state} https://example/{wf}" ;;\n' for wf, state in runs.items())
        self.fake("gh", f'case "$2" in\n{cases}  *) exit 1 ;;\nesac\n')

    def test_all_succeeded(self):
        self.gh_runs(ci="completed success", security="completed success")
        result = self.run_script("wait_for_workflows.sh", SHA_A, "ci.yml", "security.yml")
        self.assertEqual(result.returncode, 0, result.stdout)

    def test_failed_workflow_blocks(self):
        self.gh_runs(ci="completed success", security="completed failure")
        result = self.run_script("wait_for_workflows.sh", SHA_A, "ci.yml", "security.yml")
        self.assertEqual(result.returncode, 1)
        self.assertIn("security.yml concluded 'failure'", result.stdout)

    def test_missing_run_times_out(self):
        self.gh_runs(ci="")
        result = self.run_script("wait_for_workflows.sh", SHA_A, "ci.yml")
        self.assertEqual(result.returncode, 1)
        self.assertIn("Timed out", result.stdout)

    def test_unknown_workflow_fails(self):
        self.gh_runs(ci="completed success")
        result = self.run_script("wait_for_workflows.sh", SHA_A, "nope.yml")
        self.assertEqual(result.returncode, 1)
        self.assertIn("Cannot read runs of nope.yml", result.stdout)

    def test_rejects_bad_input(self):
        self.gh_runs(ci="completed success")
        self.assertEqual(self.run_script("wait_for_workflows.sh", "abc", "ci.yml").returncode, 2)
        self.assertEqual(self.run_script("wait_for_workflows.sh", SHA_A, "../x.yml").returncode, 2)


class WaitForDependencySnapshotTest(ShellScriptTestCase):
    def gh_headers(self, warning: str) -> None:
        header = f"X-Github-Dependency-Graph-Snapshot-Warnings: {warning}" if warning is not None else ""
        self.fake("gh", f"printf 'HTTP/2.0 200 OK\\r\\n{header}\\r\\n\\r\\n[]'\n")

    def test_passes_when_snapshot_exists(self):
        self.gh_headers("")
        result = self.run_script("wait_for_dependency_snapshot.sh", SHA_A, SHA_B)
        self.assertEqual(result.returncode, 0, result.stdout)

    def test_fails_when_snapshot_never_arrives(self):
        warning = "No snapshots were found for the head SHA x."
        self.gh_headers(__import__("base64").b64encode(warning.encode()).decode())
        result = self.run_script("wait_for_dependency_snapshot.sh", SHA_A, SHA_B)
        self.assertEqual(result.returncode, 1)
        self.assertIn(warning, result.stdout)

    def test_api_error_fails(self):
        self.fake("gh", "echo 'HTTP 404'; exit 1\n")
        self.assertEqual(self.run_script("wait_for_dependency_snapshot.sh", SHA_A, SHA_B).returncode, 1)

    def test_rejects_bad_sha(self):
        self.gh_headers("")
        self.assertEqual(self.run_script("wait_for_dependency_snapshot.sh", "main", SHA_B).returncode, 2)


class RunAndroidTestsTest(ShellScriptTestCase):
    def setUp(self):
        super().setUp()
        self.fake("adb", 'case "$1" in shell) printf "package:com.ivy.data.test\\npackage:com.other\\n" ;; '
                         'logcat) [ "$2" = -c ] || echo "$CRASH_LOG" ;; *) echo "$@" >> "$ADB_LOG" ;; esac\n')
        os.environ["CRASH_LOG"] = ""
        os.environ["ADB_LOG"] = os.path.join(self.tmp, "adb.log")

    def gradlew(self, output: str, status: int = 0) -> None:
        path = os.path.join(self.tmp, "gradlew")
        with open(path, "w", encoding="utf-8") as f:
            f.write(f"#!/usr/bin/env bash\nprintf '%s\\n' '{output}'\nexit {status}\n")
        os.chmod(path, stat.S_IRWXU)

    def test_passes_and_removes_stale_test_packages(self):
        self.gradlew("BUILD SUCCESSFUL")
        result = self.run_script("run_android_tests.sh", "logs/t.log", ":m:connectedDebugAndroidTest")
        self.assertEqual(result.returncode, 0, result.stdout)
        with open(os.environ["ADB_LOG"], encoding="utf-8") as f:
            self.assertEqual(f.read().strip(), "uninstall com.ivy.data.test")

    def test_install_failure_fails_even_when_gradle_succeeds(self):
        self.gradlew("AndroidTestRunner failed on emulator-5554 Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]")
        result = self.run_script("run_android_tests.sh", "logs/t.log", ":m:connectedDebugAndroidTest")
        self.assertEqual(result.returncode, 1)
        self.assertIn("did not run", result.stdout)

    def test_prints_crash_from_logcat(self):
        os.environ["CRASH_LOG"] = "FATAL EXCEPTION: main\njava.lang.NoClassDefFoundError: java.lang.StackWalker"
        self.gradlew("BUILD SUCCESSFUL")
        result = self.run_script("run_android_tests.sh", "logs/t.log", ":m:connectedDebugAndroidTest")
        self.assertEqual(result.returncode, 0)
        self.assertIn("NoClassDefFoundError", result.stdout)
        self.assertTrue(os.path.exists(os.path.join(self.tmp, "logs", "logcat-crash.txt")))

    def test_gradle_failure_is_propagated(self):
        self.gradlew("BUILD FAILED", status=3)
        self.assertEqual(self.run_script("run_android_tests.sh", "t.log", ":m:x").returncode, 3)


class SmokeTestApkTest(ShellScriptTestCase):
    PKG = "com.fernwallet.app"

    def fake_adb(self, installed: bool) -> None:
        # Records install/uninstall calls; answers the shell queries the script makes.
        listed = f"package:{self.PKG}" if installed else ""
        self.fake("adb", f'''case "$1" in
  install|uninstall) echo "$@" >> "$ADB_LOG" ;;
  logcat) ;;
  shell)
    case "$2 $3" in
      "pm list") echo "{listed}" ;;
      "cmd package") echo "{self.PKG}/com.ivy.wallet.RootActivity" ;;
      "am start") echo "Status: ok" ;;
      "pidof "*) echo 1234 ;;
    esac ;;
esac
''')
        self.fake("sleep", "exit 0\n")
        os.environ["ADB_LOG"] = os.path.join(self.tmp, "adb.log")

    @staticmethod
    def adb_calls() -> list[str]:
        with open(os.environ["ADB_LOG"], encoding="utf-8") as f:
            return f.read().split("\n")[:-1]

    def test_removes_previous_install_before_installing(self):
        self.fake_adb(installed=True)
        result = self.run_script("smoke_test_apk.sh", "app.apk", self.PKG, "out")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(self.adb_calls(), [f"uninstall {self.PKG}", "install app.apk"])

    def test_fresh_emulator_installs_without_uninstall(self):
        self.fake_adb(installed=False)
        result = self.run_script("smoke_test_apk.sh", "app.apk", self.PKG, "out")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(self.adb_calls(), ["install app.apk"])


if __name__ == "__main__":
    unittest.main()
