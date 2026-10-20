#!/usr/bin/env python3
"""Validate local release inputs and package verified APK/AAB and Play metadata."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
METADATA = ROOT / "fastlane/metadata/android"
DIST = ROOT / "dist/release"


def properties(path):
    if not path.is_file():
        raise ValueError(f"Missing {path.relative_to(ROOT)}; see README.md.")
    result = {}
    for line in path.read_text().splitlines():
        if line.strip() and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def check_config():
    signing = properties(ROOT / "keystore.properties")
    for key in ("storeFile", "storePassword", "keyAlias", "keyPassword"):
        if not signing.get(key) or "YOUR_" in signing[key]:
            raise ValueError(f"Configure signing property {key}; values are never logged.")
    if not (ROOT / signing["storeFile"]).is_file():
        raise ValueError("The configured upload keystore does not exist.")
    ads = properties(ROOT / "app/androidApp/admob.properties")
    for key, separator in (("admob.app.id", "~"), ("admob.banner.unit.id", "/")):
        value = ads.get(key, "")
        if not re.fullmatch(r"ca-app-pub-\d{16}" + separator + r"\d{10}", value):
            raise ValueError(f"{key} must contain a real AdMob ID.")
        if "3940256099942544" in value:
            raise ValueError(f"{key} contains Google's test publisher ID.")
    publisher = ads["admob.app.id"].split("~")[0].removeprefix("ca-app-")
    if not ads["admob.banner.unit.id"].startswith(f"ca-app-{publisher}/"):
        raise ValueError("AdMob app and banner must belong to the same publisher.")
    expected = f"google.com, {publisher}, DIRECT, f08c47fec0942fa0"
    ads_txt = ROOT / "web/apps/landing/public/app-ads.txt"
    if expected not in ads_txt.read_text().splitlines():
        raise ValueError("app-ads.txt does not match the configured AdMob publisher.")
    print("Signing inputs and production AdMob IDs are configured.")
    return signing


def png_info(path):
    data = path.read_bytes()
    if len(data) < 33 or data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        raise ValueError(f"Invalid PNG: {path}")
    return (*struct.unpack(">II", data[16:24]), data[24], data[25])


def check_assets():
    locale = METADATA / "en-US"
    for name, limit in (("title", 30), ("short_description", 80), ("full_description", 4000)):
        value = (locale / f"{name}.txt").read_text().strip()
        if not value or len(value) > limit:
            raise ValueError(f"{name} must contain 1–{limit} characters.")
    images = locale / "images"
    for name, size in (("icon.png", (512, 512)), ("featureGraphic.png", (1024, 500))):
        path = images / name
        width, height, depth, color = png_info(path)
        if (width, height) != size or depth != 8 or color != 2:
            raise ValueError(f"{name}: expected opaque 8-bit RGB PNG at {size}.")
        if name == "icon.png" and path.stat().st_size > 1024 * 1024:
            raise ValueError("Play icon exceeds 1 MB.")
    screenshots = sorted((images / "phoneScreenshots").glob("*.png"))
    if not 2 <= len(screenshots) <= 8:
        raise ValueError("Supply between 2 and 8 phone screenshots.")
    for path in screenshots:
        width, height, depth, color = png_info(path)
        if (min(width, height) < 320 or max(width, height) > 3840
                or max(width, height) > 2 * min(width, height)
                or depth != 8 or color != 2):
            raise ValueError(f"Play screenshot dimensions/format are invalid: {path.name}")
    print(f"Play listing validated: icon, feature graphic, {len(screenshots)} phone screenshots.")


def sdk_tool(name):
    sdk = Path(os.environ.get("ANDROID_HOME", os.environ.get(
        "ANDROID_SDK_ROOT", str(Path.home() / "Library/Android/sdk"))))
    versions = sorted(
        (sdk / "build-tools").glob(f"*/{name}"),
        key=lambda p: tuple(int(n) for n in re.findall(r"\d+", p.parent.name)),
        reverse=True,
    )
    if not versions:
        raise ValueError(f"Cannot find {name}; set ANDROID_HOME to an installed Android SDK.")
    return str(versions[0])


def java_tool(name):
    candidate = Path(os.environ.get("JAVA_HOME", "")) / "bin" / name
    return str(candidate) if candidate.is_file() else name


def run(arguments, **kwargs):
    result = subprocess.run(arguments, capture_output=True, check=False, **kwargs)
    if result.returncode:
        # Commands only receive passwords through the environment.
        raise ValueError(f"{Path(arguments[0]).name} failed: "
                         f"{result.stderr.decode(errors='replace')[-1500:]}")
    return result.stdout


def package():
    signing = check_config()
    check_assets()
    apk = ROOT / "app/androidApp/build/outputs/apk/release/androidApp-release.apk"
    aab = ROOT / "app/androidApp/build/outputs/bundle/release/androidApp-release.aab"
    if not apk.is_file() or not aab.is_file():
        raise ValueError("Build assembleRelease and bundleRelease first.")
    env = dict(os.environ, NEVER_SLEEP_STORE_PASSWORD=signing["storePassword"])
    certificate = run(
        [java_tool("keytool"), "-exportcert", "-keystore", str(ROOT / signing["storeFile"]),
         "-alias", signing["keyAlias"], "-storepass:env", "NEVER_SLEEP_STORE_PASSWORD"],
        env=env,
    )
    fingerprint = hashlib.sha256(certificate).hexdigest()
    apk_result = run([sdk_tool("apksigner"), "verify", "--print-certs", str(apk)]).decode()
    if fingerprint not in apk_result.lower():
        raise ValueError("APK was not signed with the configured upload key.")
    with zipfile.ZipFile(aab) as bundle:
        if not any(n.upper().endswith((".RSA", ".DSA", ".EC")) for n in bundle.namelist()):
            raise ValueError("AAB is unsigned.")
    verification = run([java_tool("jarsigner"), "-J-Duser.language=en", "-verify", str(aab)])
    if b"jar verified." not in verification:
        raise ValueError("AAB signature verification did not succeed.")
    aab_cert = run([java_tool("keytool"), "-J-Duser.language=en", "-printcert",
                    "-jarfile", str(aab)]).decode().lower().replace(":", "")
    if fingerprint not in aab_cert:
        raise ValueError("AAB was not signed with the configured upload key.")
    badging = run([sdk_tool("aapt2"), "dump", "badging", str(apk)]).decode()
    match = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging)
    if not match or match[1] != "com.lizz.neversleep":
        raise ValueError("Unexpected application ID in APK.")
    if "application-debuggable" in badging:
        raise ValueError("Release APK must not be debuggable.")
    DIST.mkdir(parents=True, exist_ok=True)
    files = {}
    for source in (apk, aab):
        target = DIST / source.name
        shutil.copy2(source, target)
        files[target.name] = hashlib.sha256(target.read_bytes()).hexdigest()
    store = DIST / "play-store"
    if store.exists():
        shutil.rmtree(store)
    shutil.copytree(METADATA, store / "metadata/android", symlinks=False)
    shutil.copy2(ROOT / "assets/play-store/preview.html", store / "preview.html")
    # Keep the standalone preview's relative image paths intact.
    shutil.copy2(ROOT / "assets/play-store/icon-512.png", store / "icon-512.png")
    shutil.copy2(ROOT / "assets/play-store/feature-graphic.png", store / "feature-graphic.png")
    shutil.copytree(ROOT / "assets/play-store/screenshots", store / "screenshots",
                    ignore=shutil.ignore_patterns("raw"))
    shutil.make_archive(str(DIST / "play-store-assets"), "zip", store)
    files["play-store-assets.zip"] = hashlib.sha256((DIST / "play-store-assets.zip").read_bytes()).hexdigest()
    encoded = base64.encodebytes(certificate).decode()
    (DIST / "upload-certificate.pem").write_text(
        f"-----BEGIN CERTIFICATE-----\n{encoded}-----END CERTIFICATE-----\n"
    )
    manifest = {
        "applicationId": match[1], "versionCode": int(match[2]), "versionName": match[3],
        "uploadCertificateSha256": fingerprint, "sha256": files,
        "privacyPolicy": "https://neversleep.app/privacy",
        "developer": "Lizz Studio", "supportEmail": "home@lizz.com",
    }
    (DIST / "release-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    print(f"Verified signed APK/AAB and Play Store package: {DIST}")
    print(f"Version {match[3]} ({match[2]}); upload certificate SHA-256: {fingerprint}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["check-config", "check-assets", "package"])
    args = parser.parse_args()
    try:
        {"check-config": check_config, "check-assets": check_assets, "package": package}[args.action]()
    except (ValueError, OSError, zipfile.BadZipFile) as error:
        sys.exit(str(error))
