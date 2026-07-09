#!/usr/bin/env python3
"""
Capture and polish Google Play phone screenshots.

Best-practice pattern (common on Play Store top charts):
  - 2 hero screenshots showing core states only
  - Branded headline band (~30% height) with short benefit copy
  - Cropped app UI below (no ads, settings, or compliance chrome)
  - Final 9:16 portrait (1080×2400)

Requires: adb, Pillow, a connected device/emulator, release APK installed.
"""
from __future__ import annotations

import argparse
import subprocess
import sys
import time
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFilter, ImageFont
except ImportError:
    print("Install Pillow: pip install Pillow", file=sys.stderr)
    sys.exit(1)

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "com.lizz.neversleep"
ACTIVITY = f"{PACKAGE}/.MainActivity"
OUTPUT_W, OUTPUT_H = 1080, 2400
HEADER_RATIO = 0.30

FONT_BOLD_CANDIDATES = [
    "/System/Library/Fonts/SFNS.ttf",
    "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
]
FONT_REG_CANDIDATES = [
    "/System/Library/Fonts/SFNS.ttf",
    "/System/Library/Fonts/Supplemental/Arial.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
]


def run(cmd: list[str], check: bool = True, *, text: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, check=check, capture_output=True, text=text)


def pick_font(candidates: list[str], size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    for path in candidates:
        if Path(path).exists():
            try:
                return ImageFont.truetype(path, size)
            except OSError:
                continue
    return ImageFont.load_default()


def adb(device: str, *args: str) -> None:
    run(["adb", "-s", device, *args])


def resolve_device(preferred: str | None) -> str:
    if preferred:
        return preferred
    result = run(["adb", "devices"])
    emu = None
    phys = None
    for line in result.stdout.splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device":
            if parts[0].startswith("emulator-"):
                emu = parts[0]
            else:
                phys = parts[0]
    device = emu or phys
    if not device:
        raise SystemExit("No adb device/emulator connected.")
    return device


def capture_screen(device: str, dest: Path) -> None:
    proc = run(["adb", "-s", device, "exec-out", "screencap", "-p"], check=True, text=False)
    dest.write_bytes(proc.stdout)


def launch_capture(device: str, *, never_mode: bool) -> None:
    adb(device, "shell", "am", "force-stop", PACKAGE)
    run(
        [
            "adb",
            "-s",
            device,
            "shell",
            "am",
            "start",
            "-n",
            ACTIVITY,
            "--ez",
            "screenshot_mode",
            "true",
            "--ez",
            "capture_never",
            "true" if never_mode else "false",
        ],
    )
    time.sleep(3.5)


def crop_app_panel(raw: Image.Image) -> Image.Image:
    """Keep the hero UI; trim status bar and any bottom safe-area."""
    w, h = raw.size
    top = int(h * 0.04)
    bottom = int(h * 0.90)
    return raw.crop((0, top, w, bottom))


def draw_header(
    draw: ImageDraw.ImageDraw,
    title: str,
    subtitle: str,
    accent: tuple[int, int, int],
    header_h: int,
    title_font: ImageFont.ImageFont,
    sub_font: ImageFont.ImageFont,
) -> None:
    draw.text((72, int(header_h * 0.28)), title, fill="#FFFFFF", font=title_font)
    draw.text((72, int(header_h * 0.52)), subtitle, fill="#B8B0FF", font=sub_font)
    draw.rounded_rectangle(
        (72, int(header_h * 0.72), 72 + 56, int(header_h * 0.72) + 6),
        radius=3,
        fill=accent,
    )


def polish_screenshot(
    raw: Image.Image,
    title: str,
    subtitle: str,
    accent: tuple[int, int, int],
) -> Image.Image:
    header_h = int(OUTPUT_H * HEADER_RATIO)
    content_h = OUTPUT_H - header_h

    canvas = Image.new("RGB", (OUTPUT_W, OUTPUT_H), "#0D0D1A")
    draw = ImageDraw.Draw(canvas)

    for y in range(header_h):
        t = y / max(header_h, 1)
        r = int(10 + 18 * t)
        g = int(8 + 10 * t)
        b = int(22 + 30 * t)
        draw.line([(0, y), (OUTPUT_W, y)], fill=(r, g, b))

    glow = Image.new("RGBA", (OUTPUT_W, header_h), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse(
        (OUTPUT_W - 280, -80, OUTPUT_W + 120, 220),
        fill=(*accent, 40),
    )
    gd.ellipse((-60, header_h - 180, 200, header_h + 60), fill=(124, 77, 255, 30))
    glow = glow.filter(ImageFilter.GaussianBlur(30))
    canvas.paste(glow, (0, 0), glow)

    title_font = pick_font(FONT_BOLD_CANDIDATES, 56)
    sub_font = pick_font(FONT_REG_CANDIDATES, 30)
    draw = ImageDraw.Draw(canvas)
    draw_header(draw, title, subtitle, accent, header_h, title_font, sub_font)

    panel = crop_app_panel(raw.convert("RGB"))
    panel_w, panel_h = panel.size
    scale = min(OUTPUT_W / panel_w, content_h / panel_h)
    new_w = int(panel_w * scale)
    new_h = int(panel_h * scale)
    panel = panel.resize((new_w, new_h), Image.Resampling.LANCZOS)

    px = (OUTPUT_W - new_w) // 2
    py = header_h + (content_h - new_h) // 2

    # Soft fade at top of app panel into header
    mask = Image.new("L", (new_w, new_h), 255)
    md = ImageDraw.Draw(mask)
    md.rectangle((0, 0, new_w, int(new_h * 0.08)), fill=0)
    for y in range(int(new_h * 0.08)):
        alpha = int(255 * (y / max(new_h * 0.08, 1)))
        md.line([(0, y), (new_w, y)], fill=alpha)
    canvas.paste(panel, (px, py), mask)

    return canvas


def make_comparison(normal: Image.Image, never: Image.Image, dest: Path) -> None:
    """Optional preview: both states in one wide image (not for Play upload)."""
    w, h = 1080, 2400
    gap = 24
    out = Image.new("RGB", (w * 2 + gap, h), "#0D0D1A")
    out.paste(normal.resize((w, h), Image.Resampling.LANCZOS), (0, 0))
    out.paste(never.resize((w, h), Image.Resampling.LANCZOS), (w + gap, 0))
    out.save(dest, optimize=True)


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate polished Play Store screenshots")
    parser.add_argument("--device", help="adb device id (default: emulator preferred)")
    parser.add_argument(
        "--apk",
        type=Path,
        default=ROOT / "dist/release/androidApp-release.apk",
        help="Release APK to install before capture",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=ROOT / "assets/play-store/screenshots",
    )
    parser.add_argument("--skip-install", action="store_true")
    args = parser.parse_args()

    device = resolve_device(args.device)
    out_dir = args.output
    raw_dir = out_dir / "raw"
    out_dir.mkdir(parents=True, exist_ok=True)
    raw_dir.mkdir(exist_ok=True)

    print(f"==> Device: {device}")

    adb(device, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    if not args.skip_install and args.apk.exists():
        print(f"==> Installing {args.apk}")
        run(["adb", "-s", device, "install", "-r", str(args.apk)])
    adb(device, "shell", "appops", "set", PACKAGE, "WRITE_SETTINGS", "allow")

    launch_capture(device, never_mode=False)
    normal_raw = raw_dir / "normal.png"
    capture_screen(device, normal_raw)
    print(f"    raw {normal_raw}")

    launch_capture(device, never_mode=True)
    never_raw = raw_dir / "never-sleep.png"
    capture_screen(device, never_raw)
    print(f"    raw {never_raw}")

    specs = [
        (
            normal_raw,
            out_dir / "01-normal.png",
            "Normal mode",
            "Your usual screen timeout",
            (255, 183, 77),
        ),
        (
            never_raw,
            out_dir / "02-never-sleep.png",
            "Never sleep",
            "Screen stays on until you switch back",
            (0, 229, 255),
        ),
    ]

    polished: list[Image.Image] = []
    for raw_path, out_path, title, subtitle, accent in specs:
        img = polish_screenshot(Image.open(raw_path), title, subtitle, accent)
        img.save(out_path, optimize=True)
        polished.append(img)
        print(f"    {out_path}")

    compare_path = out_dir / "preview-side-by-side.png"
    make_comparison(polished[0], polished[1], compare_path)
    print(f"    {compare_path} (preview only)")

    # Remove legacy 4-up captures if present
    for legacy in ["03-settings-dialog.png", "04-disclaimer-expanded.png"]:
        legacy_path = out_dir / legacy
        if legacy_path.exists():
            legacy_path.unlink()

    print("==> Done. Upload 01-normal.png and 02-never-sleep.png to Play Console.")


if __name__ == "__main__":
    main()