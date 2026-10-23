"""Network-free operator tool tests. Run: python -m unittest discover -s scripts/tests -v."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
import warnings
import zipfile

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("report_verifier", ROOT / "scripts/verify-report.py")
verifier = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verifier)


class SetupTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        (self.root / "scripts").mkdir()
        shutil.copy2(ROOT / "scripts/setup.py", self.root / "scripts/setup.py")

    def tearDown(self):
        self.temp.cleanup()

    def run_setup(self, *arguments):
        return subprocess.run([sys.executable, str(self.root / "scripts/setup.py"), *arguments],
                              text=True, capture_output=True, check=False)

    def values(self):
        return dict(line.split("=", 1) for line in (self.root / ".env").read_text().splitlines()
                    if line and not line.startswith("#"))

    def test_demo_has_distinct_secrets_without_console_disclosure(self):
        run = self.run_setup("--demo", "--email", "owner@example.test")
        self.assertEqual(run.returncode, 0, run.stderr)
        values = self.values()
        secrets = [values[key] for key in ("POSTGRES_ADMIN_PASSWORD", "MIGRATION_PASSWORD",
                    "DATABASE_PASSWORD", "ADMIN_PASSWORD", "DEMO_PASSWORD")]
        self.assertEqual(len(set(secrets)), 5)
        for value in secrets:
            self.assertGreaterEqual(len(value), 32)
            self.assertNotIn(value, run.stdout + run.stderr)
        self.assertEqual(values["ADMIN_EMAIL"], "owner@example.test")
        self.assertEqual(values["DEMO_DATA"], "true")
        self.assertEqual(values["PRODUCTION"], "false")

    def test_real_onboarding_does_not_enable_demo(self):
        self.assertEqual(self.run_setup().returncode, 0)
        self.assertEqual(self.values()["DEMO_DATA"], "false")
        self.assertEqual(self.values()["DEMO_PASSWORD"], "")

    def test_existing_configuration_is_not_overwritten(self):
        self.assertEqual(self.run_setup().returncode, 0)
        before = (self.root / ".env").read_bytes()
        self.assertNotEqual(self.run_setup("--demo").returncode, 0)
        self.assertEqual((self.root / ".env").read_bytes(), before)

    def test_unsafe_email_rejected_without_creating_configuration(self):
        for email in ("missing-at", "bad\n@example.test", "bad$@example.test", "bad @example.test"):
            with self.subTest(email=email):
                self.assertNotEqual(self.run_setup("--email", email).returncode, 0)
                self.assertFalse((self.root / ".env").exists())

    @unittest.skipIf(os.name == "nt", "POSIX permission bits do not represent Windows ACLs")
    def test_configuration_permissions_are_private(self):
        self.assertEqual(self.run_setup().returncode, 0)
        self.assertEqual((self.root / ".env").stat().st_mode & 0o777, 0o600)


class ReportTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.archive = Path(self.temp.name) / "report.zip"
        self.data = b"Fictional inspection evidence only."
        self.manifest = {"algorithm": "SHA-256", "files": [{"path": "evidence/example.txt",
            "bytes": len(self.data), "sha256": hashlib.sha256(self.data).hexdigest()}]}

    def tearDown(self):
        self.temp.cleanup()

    def write(self, manifest=None, entries=None):
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            with zipfile.ZipFile(self.archive, "w", zipfile.ZIP_DEFLATED) as output:
                output.writestr("manifest.json", json.dumps(self.manifest if manifest is None else manifest))
                for name, content in ([("evidence/example.txt", self.data)] if entries is None else entries):
                    output.writestr(name, content)

    def test_valid_package(self):
        self.write()
        self.assertEqual(verifier.verify(self.archive), 1)

    def test_tampered_bytes(self):
        self.write(entries=[("evidence/example.txt", b"Changed evidence")])
        with self.assertRaisesRegex(ValueError, "Integrity mismatch"):
            verifier.verify(self.archive)

    def test_wrong_declared_size(self):
        self.manifest["files"][0]["bytes"] += 1
        self.write()
        with self.assertRaisesRegex(ValueError, "Integrity mismatch"):
            verifier.verify(self.archive)

    def test_unlisted_entry(self):
        self.write(entries=[("evidence/example.txt", self.data), ("unexpected.txt", b"extra")])
        with self.assertRaisesRegex(ValueError, "Unlisted"):
            verifier.verify(self.archive)

    def test_duplicate_zip_entries(self):
        self.write(entries=[("evidence/example.txt", self.data)] * 2)
        with self.assertRaisesRegex(ValueError, "Duplicate ZIP"):
            verifier.verify(self.archive)

    def test_unsafe_paths_rejected_without_extracting(self):
        for name in ("../escape.txt", "/absolute.txt", "dir/../../escape.txt", "dir\\escape.txt"):
            with self.subTest(name=name):
                self.write(entries=[(name, b"data")])
                with self.assertRaisesRegex(ValueError, "Unsafe path"):
                    verifier.verify(self.archive)

    def test_duplicate_manifest_entries(self):
        self.manifest["files"].append(dict(self.manifest["files"][0]))
        self.write()
        with self.assertRaisesRegex(ValueError, "Duplicate or self-referential"):
            verifier.verify(self.archive)

    def test_manifest_cannot_checksum_itself(self):
        self.manifest["files"][0]["path"] = "manifest.json"
        self.write()
        with self.assertRaisesRegex(ValueError, "self-referential"):
            verifier.verify(self.archive)

    def test_unsupported_algorithm(self):
        self.manifest["algorithm"] = "MD5"
        self.write()
        with self.assertRaisesRegex(ValueError, "Unsupported algorithm"):
            verifier.verify(self.archive)

    def test_missing_declared_file(self):
        self.write(entries=[])
        with self.assertRaises(KeyError):
            verifier.verify(self.archive)

    def test_command_reports_validity_not_authenticity(self):
        self.write()
        run = subprocess.run([sys.executable, str(ROOT / "scripts/verify-report.py"), str(self.archive)],
                             text=True, capture_output=True, check=False)
        self.assertEqual(run.returncode, 0, run.stderr)
        self.assertIn("does not authenticate", run.stdout)
        self.assertIn("1 files", run.stdout)

    def test_command_exits_nonzero_on_bad_zip(self):
        self.archive.write_bytes(b"Not a ZIP")
        run = subprocess.run([sys.executable, str(ROOT / "scripts/verify-report.py"), str(self.archive)],
                             text=True, capture_output=True, check=False)
        self.assertNotEqual(run.returncode, 0)
        self.assertIn("FAILED", run.stderr)


if __name__ == "__main__":
    unittest.main()
