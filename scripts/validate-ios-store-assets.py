#!/usr/bin/env python3
"""Validate dimensions, opacity, and count for the iOS App Store delivery."""

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
DELIVERY = ROOT / "assets" / "app-store" / "ios"

EXPECTED = {
    DELIVERY / "icon" / "app-icon-default-1024.png": (1024, 1024),
    DELIVERY / "icon" / "app-icon-dark-1024.png": (1024, 1024),
    DELIVERY / "icon" / "app-icon-tinted-1024.png": (1024, 1024),
    DELIVERY / "icon" / "app-icon-marketing-1024.png": (1024, 1024),
}

SCREENSHOT_SETS = {
    DELIVERY / "screenshots" / "iphone-6.9" / "en-US": (1320, 2868),
    DELIVERY / "screenshots" / "ipad-13" / "en-US": (2064, 2752),
}


def validate_image(path: Path, expected_size: tuple[int, int]) -> None:
    if not path.exists():
        raise AssertionError(f"Missing {path.relative_to(ROOT)}")
    with Image.open(path) as image:
        if image.size != expected_size:
            raise AssertionError(
                f"{path.relative_to(ROOT)} is {image.size}, expected {expected_size}"
            )
        if image.mode not in {"RGB", "L"}:
            raise AssertionError(
                f"{path.relative_to(ROOT)} has mode {image.mode}; alpha is not allowed"
            )


def main() -> None:
    for path, expected_size in EXPECTED.items():
        validate_image(path, expected_size)

    for directory, expected_size in SCREENSHOT_SETS.items():
        screenshots = sorted(directory.glob("*.png"))
        if len(screenshots) != 5:
            raise AssertionError(
                f"{directory.relative_to(ROOT)} has {len(screenshots)} screenshots; expected 5"
            )
        for screenshot in screenshots:
            validate_image(screenshot, expected_size)

    print("Validated 4 icon files and 10 App Store screenshots: dimensions and opacity are correct.")


if __name__ == "__main__":
    main()
