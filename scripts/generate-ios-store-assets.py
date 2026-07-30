#!/usr/bin/env python3
"""Build the Never Sleep iOS icon catalog and App Store screenshot artwork."""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont, ImageOps


ROOT = Path(__file__).resolve().parents[1]
DELIVERY = ROOT / "assets" / "app-store" / "ios"
ICON_SOURCE = DELIVERY / "icon" / "source"
ICON_OUTPUT = DELIVERY / "icon"
APP_ICONSET = (
    ROOT
    / "app"
    / "iosApp"
    / "iosApp"
    / "Assets.xcassets"
    / "AppIcon.appiconset"
)
SCREENSHOT_SOURCE = DELIVERY / "screenshots" / "source"
SCREENSHOT_OUTPUT = DELIVERY / "screenshots"

FONT_REGULAR = Path("/System/Library/Fonts/SFNS.ttf")
FONT_ROUNDED = Path("/System/Library/Fonts/SFNSRounded.ttf")

CARDS = (
    (
        "01-ready.png",
        "A display that\nstays awake",
        "Begin a foreground session whenever this screen needs to remain visible.",
        ((17, 9, 12), (111, 49, 32)),
    ),
    (
        "02-active.png",
        "One tap.\nQuiet focus.",
        "A calm night state makes an active session unmistakable.",
        ((4, 6, 17), (20, 37, 91)),
    ),
    (
        "03-duration.png",
        "Choose the time\nyou need",
        "Use a focused interval — or keep going until you end it.",
        ((14, 8, 15), (106, 52, 38)),
    ),
    (
        "04-help.png",
        "Honest by\ndesign",
        "Foreground-only behavior is explained clearly, with privacy one tap away.",
        ((4, 7, 20), (24, 31, 71)),
    ),
    (
        "05-transition.png",
        "Day and night,\nclearly",
        "Warm ready and cool active states make every change visible at a glance.",
        ((7, 9, 26), (82, 43, 72)),
    ),
)

PLATFORMS = {
    "iphone": {
        "size": (1320, 2868),
        "output": SCREENSHOT_OUTPUT / "iphone-6.9" / "en-US",
        "screen_width": 1000,
        "screen_top": 624,
        "title_size": 94,
        "subtitle_size": 38,
        "eyebrow_size": 26,
        "margin": 118,
        "corner": 54,
    },
    "ipad": {
        "size": (2064, 2752),
        "output": SCREENSHOT_OUTPUT / "ipad-13" / "en-US",
        "screen_width": 1480,
        "screen_top": 648,
        "title_size": 112,
        "subtitle_size": 44,
        "eyebrow_size": 28,
        "margin": 166,
        "corner": 46,
    },
}


def font(path: Path, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(path), size)


def opaque_square(path: Path, size: int = 1024) -> Image.Image:
    image = Image.open(path).convert("RGB")
    side = min(image.size)
    left = (image.width - side) // 2
    top = (image.height - side) // 2
    image = image.crop((left, top, left + side, top + side))
    return image.resize((size, size), Image.Resampling.LANCZOS)


def build_icons() -> None:
    default = opaque_square(ICON_SOURCE / "generated-default.png")
    dark = opaque_square(ICON_SOURCE / "generated-dark.png")

    # Apple uses the tinted appearance as a monochrome source. Keep the
    # generated silhouette intact, compress the background, and lift the mark.
    gray = ImageOps.grayscale(default)
    gray = ImageEnhance.Contrast(gray).enhance(1.28)
    gray = gray.point(
        lambda value: int(255 * math.pow(max(0, value) / 255, 0.82))
    )
    tinted = Image.merge("RGB", (gray, gray, gray))

    ICON_OUTPUT.mkdir(parents=True, exist_ok=True)
    APP_ICONSET.mkdir(parents=True, exist_ok=True)

    outputs = {
        "app-icon-default-1024.png": default,
        "app-icon-dark-1024.png": dark,
        "app-icon-tinted-1024.png": tinted,
    }
    for name, image in outputs.items():
        image.save(ICON_OUTPUT / name, "PNG", optimize=True)
        image.save(APP_ICONSET / name, "PNG", optimize=True)

    default.save(ICON_OUTPUT / "app-icon-marketing-1024.png", "PNG", optimize=True)
    build_icon_contact_sheet(outputs)


def build_icon_contact_sheet(icons: dict[str, Image.Image]) -> None:
    canvas = Image.new("RGB", (1800, 680), (7, 8, 18))
    draw = ImageDraw.Draw(canvas)
    label_font = font(FONT_REGULAR, 32)
    title_font = font(FONT_ROUNDED, 52)
    draw.text((90, 52), "Never Sleep · iOS icon appearances", font=title_font, fill="white")

    labels = ("Default", "Dark", "Tinted")
    names = tuple(icons.keys())
    for index, (name, label) in enumerate(zip(names, labels, strict=True)):
        preview = icons[name].resize((400, 400), Image.Resampling.LANCZOS)
        mask = Image.new("L", preview.size, 0)
        ImageDraw.Draw(mask).rounded_rectangle((0, 0, 399, 399), radius=92, fill=255)
        x = 90 + index * 560
        canvas.paste(preview, (x, 146), mask)
        text_width = draw.textlength(label, font=label_font)
        draw.text(
            (x + (400 - text_width) / 2, 578),
            label,
            font=label_font,
            fill=(201, 204, 224),
        )

    canvas.save(ICON_OUTPUT / "icon-appearances-preview.png", "PNG", optimize=True)


def gradient(size: tuple[int, int], start: tuple[int, int, int], end: tuple[int, int, int]) -> Image.Image:
    width, height = size
    colors = []
    for y in range(height):
        t = y / max(1, height - 1)
        eased = t * t * (3 - 2 * t)
        colors.append(
            tuple(
                round(start[channel] * (1 - eased) + end[channel] * eased)
                for channel in range(3)
            )
        )
    strip = Image.new("RGB", (1, height))
    strip.putdata(colors)
    return strip.resize((width, height))


def add_atmosphere(image: Image.Image, index: int) -> None:
    draw = ImageDraw.Draw(image, "RGBA")
    width, height = image.size
    points = (
        (0.12, 0.18, 3),
        (0.82, 0.13, 2),
        (0.91, 0.34, 4),
        (0.09, 0.56, 2),
        (0.84, 0.74, 3),
        (0.17, 0.88, 2),
    )
    for point_index, (x_ratio, y_ratio, radius) in enumerate(points):
        x = int(width * ((x_ratio + index * 0.031) % 0.92))
        y = int(height * y_ratio)
        radius = round(radius * width / 1320)
        draw.ellipse(
            (x - radius, y - radius, x + radius, y + radius),
            fill=(180, 205, 255, 104),
        )


def fit_inside(image: Image.Image, max_width: int, max_height: int) -> Image.Image:
    scale = min(max_width / image.width, max_height / image.height)
    return image.resize(
        (round(image.width * scale), round(image.height * scale)),
        Image.Resampling.LANCZOS,
    )


def paste_rounded(
    canvas: Image.Image,
    screenshot: Image.Image,
    position: tuple[int, int],
    radius: int,
) -> None:
    x, y = position
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    shadow_draw = ImageDraw.Draw(shadow)
    shadow_draw.rounded_rectangle(
        (x - 18, y + 18, x + screenshot.width + 18, y + screenshot.height + 54),
        radius=radius + 12,
        fill=(0, 0, 0, 110),
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=30))
    canvas.paste(shadow, (0, 0), shadow)

    mask = Image.new("L", screenshot.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, screenshot.width - 1, screenshot.height - 1),
        radius=radius,
        fill=255,
    )
    canvas.paste(screenshot, position, mask)

    border = ImageDraw.Draw(canvas, "RGBA")
    border.rounded_rectangle(
        (x, y, x + screenshot.width - 1, y + screenshot.height - 1),
        radius=radius,
        outline=(255, 255, 255, 44),
        width=2,
    )


def draw_wrapped_text(
    draw: ImageDraw.ImageDraw,
    text: str,
    position: tuple[int, int],
    text_font: ImageFont.FreeTypeFont,
    fill: tuple[int, int, int],
    max_width: int,
    spacing: int,
) -> int:
    x, y = position
    lines: list[str] = []
    for paragraph in text.splitlines():
        words = paragraph.split()
        current = ""
        for word in words:
            candidate = f"{current} {word}".strip()
            if draw.textlength(candidate, font=text_font) <= max_width:
                current = candidate
            else:
                if current:
                    lines.append(current)
                current = word
        if current:
            lines.append(current)
    for line in lines:
        draw.text((x, y), line, font=text_font, fill=fill)
        bbox = draw.textbbox((x, y), line, font=text_font)
        y = bbox[3] + spacing
    return y


def build_store_screenshots() -> None:
    build_ipad_state_comparison()

    for platform, config in PLATFORMS.items():
        source_dir = SCREENSHOT_SOURCE / platform
        output_dir: Path = config["output"]
        output_dir.mkdir(parents=True, exist_ok=True)
        for index, (name, title, subtitle, palette) in enumerate(CARDS):
            source = source_dir / name
            if not source.exists():
                continue

            canvas = gradient(config["size"], *palette)
            add_atmosphere(canvas, index)
            draw = ImageDraw.Draw(canvas)
            margin = config["margin"]
            eyebrow_font = font(FONT_REGULAR, config["eyebrow_size"])
            title_font = font(FONT_ROUNDED, config["title_size"])
            subtitle_font = font(FONT_REGULAR, config["subtitle_size"])

            draw.text(
                (margin, 98),
                "NEVER SLEEP  ·  iOS",
                font=eyebrow_font,
                fill=(179, 185, 216),
            )
            title_bottom = draw_wrapped_text(
                draw,
                title,
                (margin, 154),
                title_font,
                (247, 246, 252),
                config["size"][0] - 2 * margin,
                round(config["title_size"] * 0.05),
            )
            draw_wrapped_text(
                draw,
                subtitle,
                (margin, title_bottom + 24),
                subtitle_font,
                (214, 216, 229),
                config["size"][0] - 2 * margin,
                round(config["subtitle_size"] * 0.30),
            )

            screenshot = Image.open(source).convert("RGB")
            max_height = config["size"][1] - config["screen_top"] - 82
            screenshot = fit_inside(
                screenshot,
                config["screen_width"],
                max_height,
            )
            x = (config["size"][0] - screenshot.width) // 2
            paste_rounded(
                canvas,
                screenshot,
                (x, config["screen_top"]),
                config["corner"],
            )
            canvas.save(output_dir / name, "PNG", optimize=True)


def build_ipad_state_comparison() -> None:
    """Make a truthful state comparison from the two real iPad captures."""
    source_dir = SCREENSHOT_SOURCE / "ipad"
    ready_path = source_dir / "01-ready.png"
    active_path = source_dir / "02-active.png"
    if not ready_path.exists() or not active_path.exists():
        return

    ready = Image.open(ready_path).convert("RGB")
    active = Image.open(active_path).convert("RGB")
    if active.size != ready.size:
        active = active.resize(ready.size, Image.Resampling.LANCZOS)

    width, height = ready.size
    transition_width = round(width * 0.17)
    transition_start = width // 2 - transition_width // 2
    mask_values = []
    for x in range(width):
        t = min(1, max(0, (x - transition_start) / transition_width))
        eased = t * t * (3 - 2 * t)
        mask_values.append(round(255 * eased))
    mask_strip = Image.new("L", (width, 1))
    mask_strip.putdata(mask_values)
    mask = mask_strip.resize((width, height))
    Image.composite(active, ready, mask).save(
        source_dir / "05-transition.png",
        "PNG",
        optimize=True,
    )


def main() -> None:
    build_icons()
    build_store_screenshots()
    print(f"Generated iOS delivery assets under {DELIVERY}")


if __name__ == "__main__":
    main()
