#!/usr/bin/env python3
"""Extract SCORECORE neon icon from premium phone mock → Play + adaptive mipmaps."""
from pathlib import Path
from PIL import Image

MOCK = Path(
    "/home/box/agent-data/agents/1f1469fd-c9f5-4898-bdb1-3620bfd00013/attachments/"
    "ef51edcd866065bdd261203901d1f025be9c4a4c84089a2f6b290a9f4aced592.png"
)
OUT = Path(__file__).resolve().parent
ROOT = Path("/workspace/Tanniscoring")
NAVY = (10, 22, 40, 255)


def extract_icon() -> Image.Image:
    im = Image.open(MOCK).convert("RGBA")
    w, h = im.size
    cx, cy = w // 2, h // 2
    half = 276  # outer chrome rim; excludes glow rings / phone
    return im.crop((cx - half, cy - half, cx + half, cy + half))


def to_size(img: Image.Image, size: int) -> Image.Image:
    return img.resize((size, size), Image.Resampling.LANCZOS)


def make_adaptive_fg(icon: Image.Image, size: int, content_ratio: float = 0.94) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), NAVY)
    content = int(size * content_ratio)
    if content % 2:
        content += 1
    scaled = to_size(icon, content)
    ox = (size - content) // 2
    canvas.paste(scaled, (ox, ox), scaled)
    return canvas


def save_mipmap_set(module_res: Path, fg_master: Image.Image) -> None:
    densities = {
        "mipmap-mdpi": 108,
        "mipmap-hdpi": 162,
        "mipmap-xhdpi": 216,
        "mipmap-xxhdpi": 324,
        "mipmap-xxxhdpi": 432,
    }
    for folder, px in densities.items():
        d = module_res / folder
        d.mkdir(parents=True, exist_ok=True)
        to_size(fg_master, px).save(d / "ic_launcher_foreground.png", "PNG", optimize=True)


def main() -> None:
    icon = extract_icon()
    to_size(icon, 1024).save(OUT / "icon-neon-preview.png", "PNG", optimize=True)
    to_size(icon, 512).save(OUT / "hi-res-icon-neon.png", "PNG", optimize=True)
    to_size(icon, 512).save(OUT / "icon-neon-preview-512.png", "PNG", optimize=True)
    fg = make_adaptive_fg(icon, 1024)
    fg.save(OUT / "ic_launcher_foreground_neon.png", "PNG", optimize=True)
    to_size(fg, 108).save(OUT / "ic_launcher_foreground_108dp.png", "PNG", optimize=True)
    for mod in ("app", "wear"):
        save_mipmap_set(ROOT / mod / "src/main/res", fg)
    print("Done.")


if __name__ == "__main__":
    main()
