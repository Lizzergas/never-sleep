"""Release guards must fail before a misconfigured production build is packaged."""
import contextlib
import importlib.util
import io
from pathlib import Path
import shutil
import struct
import tempfile
import unittest
from unittest.mock import patch

SOURCE_ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("android_release", SOURCE_ROOT / "scripts/android-release.py")
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseGuardsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.addCleanup(patch.stopall)
        patch.object(release, "ROOT", self.root).start()
        patch.object(release, "METADATA", self.root / "metadata").start()
        (self.root / "key").touch()
        self.write("keystore.properties", "storeFile=key\nstorePassword=fixture\nkeyAlias=upload\nkeyPassword=fixture\n")
        self.write("app/androidApp/admob.properties",
                   "admob.app.id=ca-app-pub-1111111111111111~1111111111\n"
                   "admob.banner.unit.id=ca-app-pub-1111111111111111/2222222222\n")
        self.write("web/apps/landing/public/app-ads.txt",
                   "google.com, pub-1111111111111111, DIRECT, f08c47fec0942fa0\n")

    def write(self, name, text):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)
        return path

    def check_config(self):
        with contextlib.redirect_stdout(io.StringIO()):
            return release.check_config()

    def test_valid_config(self):
        self.assertEqual(self.check_config()["keyAlias"], "upload")

    def test_missing_signing(self):
        (self.root / "keystore.properties").unlink()
        with self.assertRaisesRegex(ValueError, "Missing"):
            self.check_config()

    def test_missing_key(self):
        (self.root / "key").unlink()
        with self.assertRaisesRegex(ValueError, "keystore does not exist"):
            self.check_config()

    def test_rejects_google_test_publisher(self):
        self.write("app/androidApp/admob.properties",
                   "admob.app.id=ca-app-pub-3940256099942544~3347511713\n"
                   "admob.banner.unit.id=ca-app-pub-3940256099942544/6300978111\n")
        with self.assertRaisesRegex(ValueError, "test publisher"):
            self.check_config()

    def test_rejects_mismatched_banner_publisher(self):
        path = self.root / "app/androidApp/admob.properties"
        path.write_text(path.read_text().replace("1111111111111111/", "2222222222222222/"))
        with self.assertRaisesRegex(ValueError, "same publisher"):
            self.check_config()

    def test_rejects_wrong_ads_txt(self):
        self.write("web/apps/landing/public/app-ads.txt", "")
        with self.assertRaisesRegex(ValueError, "app-ads.txt"):
            self.check_config()

    def test_current_assets_and_rejected_tall_screenshot(self):
        shutil.copytree(SOURCE_ROOT / "fastlane/metadata/android", release.METADATA)
        with contextlib.redirect_stdout(io.StringIO()):
            release.check_assets()
        shot = release.METADATA / "en-US/images/phoneScreenshots/01-normal.png"
        data = bytearray(shot.read_bytes())
        data[16:24] = struct.pack(">II", 1080, 2400)
        shot.write_bytes(data)
        with self.assertRaisesRegex(ValueError, "dimensions/format"):
            release.check_assets()

    def test_truncated_png_reports_error(self):
        path = self.root / "broken.png"
        path.write_bytes(b"\x89PNG\r\n\x1a\n")
        with self.assertRaisesRegex(ValueError, "Invalid PNG"):
            release.png_info(path)


if __name__ == "__main__":
    unittest.main()
