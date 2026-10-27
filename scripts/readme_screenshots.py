#!/usr/bin/env python3
"""Builds the README screenshots in docs/screenshots from the Paparazzi golden images.

The goldens are rendered from Compose previews with generic sample data (no real
accounts, names or amounts). This script removes the emulated system navigation bar
and rounds the corners.

Usage (requires Pillow: `pip install pillow`):
    ./gradlew recordPaparazziDebug
    python3 scripts/readme_screenshots.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs" / "screenshots"

# docs file name -> Paparazzi golden (relative to the repo root)
SHOTS = {
    "home-light.png": "feature/home/src/test/snapshots/images/"
    "com.ivy.home_HomePaparazziTest_snapshot Home Screen[Light].png",
    "home-dark.png": "feature/home/src/test/snapshots/images/"
    "com.ivy.home_HomePaparazziTest_snapshot Home Screen[Dark].png",
    "accounts-light.png": "feature/accounts/src/test/snapshots/images/"
    "com.ivy.accounts_AccountsTabPaparazziTest_snapshot accountTab nonCompact composable[Light].png",
    "add-menu-light.png": "feature/main/src/test/snapshots/images/"
    "com.ivy.main_MainFabMenuPaparazziTest_snapshot add transaction menu[Light].png",
    "period-picker-light.png": "feature/home/src/test/snapshots/images/"
    "com.ivy.home_HomeOverlaysPaparazziTest_snapshot period picker[Light].png",
    "more-menu-light.png": "feature/home/src/test/snapshots/images/"
    "com.ivy.home_HomeOverlaysPaparazziTest_snapshot more menu[Light].png",
    "support-light.png": "feature/settings/src/test/snapshots/images/"
    "com.ivy.settings_SupportSectionPaparazziTest_snapshot support section[Light].png",
}

NAV_BAR_COLOR = (48, 48, 48)  # Paparazzi's emulated 3-button navigation bar
CORNER_RADIUS_FRACTION = 0.065  # of the image width


def crop_nav_bar(img: Image.Image) -> Image.Image:
    """Crops the emulated system navigation bar at the bottom, if present."""
    px = img.load()
    w, h = img.size
    y = h - 1
    while y > h // 2 and px[2, y][:3] == NAV_BAR_COLOR and px[w - 3, y][:3] == NAV_BAR_COLOR:
        y -= 1
    return img.crop((0, 0, w, y + 1)) if y < h - 1 else img


def round_corners(img: Image.Image) -> Image.Image:
    w, h = img.size
    scale = 4  # supersampled mask for smooth, anti-aliased corners
    radius = int(w * CORNER_RADIUS_FRACTION) * scale
    mask = Image.new("L", (w * scale, h * scale), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, w * scale - 1, h * scale - 1), radius, fill=255)
    out = img.convert("RGBA")
    out.putalpha(mask.resize((w, h), Image.LANCZOS))
    return out


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, src in SHOTS.items():
        img = Image.open(ROOT / src).convert("RGB")
        round_corners(crop_nav_bar(img)).save(OUT / name, optimize=True)
        print(f"wrote docs/screenshots/{name}")


if __name__ == "__main__":
    main()
