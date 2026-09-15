#!/usr/bin/env python3
"""Generate SCORECORE premium neon adaptive icon assets (wordmark + graphic)."""
from __future__ import annotations

import math
import os
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageEnhance

OUT = Path(__file__).resolve().parent
ROOT = Path("/workspace/Tanniscoring")

NAVY = (10, 22, 40, 255)          # #0A1628
LIME = (198, 255, 0, 255)         # #C6FF00
CYAN = (0, 229, 255, 255)         # #00E5FF
CHROME_HI = (220, 230, 245, 220)
CHROME_MID = (140, 155, 175, 200)
CHROME_LO = (70, 85, 105, 180)
BALL_SEAM = (10, 22, 40, 255)
WHITE = (255, 255, 255, 255)


def lerp(a, b, t):
    return a + (b - a) * t


def mix(c1, c2, t):
    return tuple(int(lerp(c1[i], c2[i], t)) for i in range(4))


def glow_layer(size: int, draw_fn, color, blur: float, alpha_scale: float = 1.0) -> Image.Image:
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    draw_fn(d)
    # tint to color with alpha
    r, g, b, a = color
    pix = layer.split()
    if len(pix) == 4:
        # keep alpha from strokes, recolor RGB
        solid = Image.new("RGBA", (size, size), (r, g, b, 0))
        solid.putalpha(pix[3])
        layer = solid
    if alpha_scale != 1.0:
        a_ch = layer.split()[3].point(lambda p: int(p * alpha_scale))
        layer.putalpha(a_ch)
    if blur > 0:
        layer = layer.filter(ImageFilter.GaussianBlur(radius=blur))
    return layer


def racket_outline(cx, cy, angle_deg, length, head_r, shaft_w, handle_len):
    """Return polyline points for a simplified tennis racket (head oval + shaft + handle).
    Angle 0 = pointing up. Returns list of (x,y) for head ellipse params and shaft line.
    """
    rad = math.radians(angle_deg)
    # tip of head (far from handle)
    tip_x = cx + math.sin(rad) * length
    tip_y = cy - math.cos(rad) * length
    # handle end
    hx = cx - math.sin(rad) * (length * 0.55 + handle_len)
    hy = cy + math.cos(rad) * (length * 0.55 + handle_len)
    # head center (along shaft from tip back)
    hcx = cx + math.sin(rad) * (length * 0.35)
    hcy = cy - math.cos(rad) * (length * 0.35)
    # throat (where head meets shaft)
    tx = cx + math.sin(rad) * (length * 0.05)
    ty = cy - math.cos(rad) * (length * 0.05)
    return {
        "head_c": (hcx, hcy),
        "head_r": head_r,
        "throat": (tx, ty),
        "handle_end": (hx, hy),
        "angle": rad,
        "shaft_w": shaft_w,
    }


def draw_racket(d: ImageDraw.ImageDraw, rk, color, width_scale=1.0, strings=True):
    hcx, hcy = rk["head_c"]
    hr = rk["head_r"]
    ang = rk["angle"]
    # oval head elongated along shaft
    rx = hr * 0.78
    ry = hr
    # draw ellipse by approximating rotated oval with polygon
    pts = []
    for i in range(48):
        t = 2 * math.pi * i / 48
        lx = rx * math.cos(t)
        ly = ry * math.sin(t)
        # rotate so major axis along racket
        x = hcx + lx * math.cos(ang) - ly * math.sin(ang)
        y = hcy + lx * math.sin(ang) + ly * math.cos(ang)
        # wait: ang is direction of shaft from center toward tip (up-ish)
        # better rotate so Y of oval aligns with shaft
        x = hcx + lx * math.cos(ang + math.pi / 2) - ly * math.sin(ang + math.pi / 2)
        y = hcy + lx * math.sin(ang + math.pi / 2) + ly * math.cos(ang + math.pi / 2)
        pts.append((x, y))
    # redo properly: local y along shaft (tip direction = -ang wait)
    pts = []
    for i in range(56):
        t = 2 * math.pi * i / 56
        lx = rx * math.cos(t)   # perpendicular
        ly = ry * math.sin(t)   # along shaft (positive toward tip)
        # tip direction unit: (sin, -cos)
        ux, uy = math.sin(ang), -math.cos(ang)
        px, py = -uy, ux  # perpendicular
        x = hcx + lx * px + ly * ux
        y = hcy + lx * py + ly * uy
        pts.append((x, y))

    sw = max(2, int(rk["shaft_w"] * width_scale))
    d.line(pts + [pts[0]], fill=color, width=sw, joint="curve")

    # strings (subtle grid inside head)
    if strings:
        sc = (color[0], color[1], color[2], max(40, color[3] // 3))
        for k in range(-3, 4):
            # cross strings
            offs = k * (rx * 0.28)
            p1 = (hcx + (-rx * 0.55) * px + (offs) * ux, hcy + (-rx * 0.55) * py + offs * uy)
            p2 = (hcx + (rx * 0.55) * px + (offs) * ux, hcy + (rx * 0.55) * py + offs * uy)
            # clip-ish: only draw if within rough oval
            d.line([p1, p2], fill=sc, width=max(1, sw // 4))
        for k in range(-4, 5):
            offs = k * (ry * 0.22)
            p1 = (hcx + offs * px + (-ry * 0.7) * ux, hcy + offs * py + (-ry * 0.7) * uy)
            p2 = (hcx + offs * px + (ry * 0.7) * ux, hcy + offs * py + (ry * 0.7) * uy)
            d.line([p1, p2], fill=sc, width=max(1, sw // 4))

    # shaft + handle
    throat = rk["throat"]
    hend = rk["handle_end"]
    # slightly thicker handle
    d.line([throat, hend], fill=color, width=sw, joint="curve")
    # grip bump near end
    gx = lerp(throat[0], hend[0], 0.72)
    gy = lerp(throat[1], hend[1], 0.72)
    d.line([(gx, gy), hend], fill=color, width=int(sw * 1.35))


def draw_tennis_ball(d, cx, cy, r, fill, seam):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=fill)
    # seams
    sw = max(2, int(r * 0.14))
    # left arc
    d.arc([cx - r * 1.15, cy - r * 0.85, cx - r * 0.05, cy + r * 0.85],
          start=300, end=60, fill=seam, width=sw)
    # right arc
    d.arc([cx + r * 0.05, cy - r * 0.85, cx + r * 1.15, cy + r * 0.85],
          start=120, end=240, fill=seam, width=sw)


def draw_shuttle(d, cx, cy, scale, color):
    """Geometric shuttlecock pointing up, cork at bottom."""
    # cork
    cr = scale * 0.22
    cork_y = cy + scale * 0.42
    d.ellipse([cx - cr, cork_y - cr, cx + cr, cork_y + cr], fill=color)
    # skirt triangle outline
    tip = (cx, cy - scale * 0.55)
    bl = (cx - scale * 0.48, cork_y - cr * 0.3)
    br = (cx + scale * 0.48, cork_y - cr * 0.3)
    w = max(2, int(scale * 0.09))
    d.line([tip, bl], fill=color, width=w)
    d.line([tip, br], fill=color, width=w)
    d.line([tip, (cx, cork_y - cr * 0.2)], fill=color, width=w)
    # crossbar
    by = cy - scale * 0.08
    bw = scale * 0.28
    d.line([(cx - bw, by), (cx + bw, by)], fill=color, width=max(1, w - 1))
    # soft fill
    fill = (color[0], color[1], color[2], max(30, color[3] // 5))
    d.polygon([tip, bl, br], fill=fill)


def draw_chrome_rim(img: Image.Image, margin_ratio=0.04):
    """Subtle metallic ring near edge (for full app-icon preview)."""
    s = img.size[0]
    m = int(s * margin_ratio)
    overlay = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)
    # outer soft ring
    for i, col in enumerate([CHROME_LO, CHROME_MID, CHROME_HI, CHROME_MID, CHROME_LO]):
        inset = m + i
        d.ellipse([inset, inset, s - 1 - inset, s - 1 - inset], outline=col, width=max(1, s // 220))
    # slight blur for chrome softness
    overlay = overlay.filter(ImageFilter.GaussianBlur(radius=max(1, s // 400)))
    return Image.alpha_composite(img, overlay)



def draw_wordmark(base: Image.Image, size: int, cx: float, anchor_y: float) -> Image.Image:
    """Draw SCORECORE under the center badge with neon glow."""
    from PIL import ImageFont
    label = "SCORECORE"
    font_paths = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
        "/usr/share/fonts/truetype/freefont/FreeSansBold.ttf",
    ]
    font = None
    font_size = max(10, int(size * 0.058))
    for fp in font_paths:
        if Path(fp).exists():
            try:
                font = ImageFont.truetype(fp, font_size)
                break
            except OSError:
                pass
    if font is None:
        font = ImageFont.load_default()

    tmp = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    td = ImageDraw.Draw(tmp)
    bbox = td.textbbox((0, 0), label, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    tx = cx - tw / 2
    # Sit below badge; stay inside adaptive safe-ish zone for FG
    ty = min(size * 0.76, max(anchor_y + size * 0.02, size * 0.695))

    def paint(d, fill, ox=0, oy=0):
        d.text((tx + ox, ty + oy), label, font=font, fill=fill)

    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    paint(gd, (198, 255, 0, 200))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=max(1.0, size * 0.018)))
    a = glow.split()[3].point(lambda p: int(p * 0.7))
    glow.putalpha(a)
    base = Image.alpha_composite(base, glow)

    mid = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    md = ImageDraw.Draw(mid)
    paint(md, (198, 255, 0, 230))
    mid = mid.filter(ImageFilter.GaussianBlur(radius=max(0.5, size * 0.006)))
    base = Image.alpha_composite(base, mid)

    crisp = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    cd = ImageDraw.Draw(crisp)
    paint(cd, (240, 255, 180, 255))
    paint(cd, LIME)
    # textbbox top can be negative; place underline under glyph bottoms
    descent = bbox[3]
    uw = tw * 0.96
    uy = ty + descent + max(3, size // 160)
    cd.line([(cx - uw / 2, uy), (cx + uw / 2, uy)], fill=CYAN, width=max(1, size // 220))
    base = Image.alpha_composite(base, crisp)
    return base


def compose_icon(size: int, with_chrome=True, transparent_bg=False) -> Image.Image:
    """Full composed icon at given size."""
    if transparent_bg:
        base = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    else:
        base = Image.new("RGBA", (size, size), NAVY)

    cx = cy = size / 2
    # Scale geometry to size. For adaptive FG, keep content in ~66% safe zone.
    # Crossed rackets span ~70% of canvas.
    length = size * 0.28
    head_r = size * 0.105
    shaft_w = size * 0.018
    handle_len = size * 0.08

    # Racket angles: crossed ~X — lime NW-SE, cyan NE-SW (premium sports)
    lime_rk = racket_outline(cx, cy, -38, length, head_r, shaft_w, handle_len)
    cyan_rk = racket_outline(cx, cy, 38, length, head_r, shaft_w, handle_len)

    def draw_both(d, color_lime, color_cyan, ws=1.0, strings=True):
        draw_racket(d, cyan_rk, color_cyan, width_scale=ws, strings=strings)
        draw_racket(d, lime_rk, color_lime, width_scale=ws, strings=strings)

    # Outer glow (large blur)
    glow_c = glow_layer(
        size,
        lambda d: draw_both(d, CYAN, CYAN, ws=1.4, strings=False),
        (0, 229, 255, 180),
        blur=size * 0.045,
        alpha_scale=0.55,
    )
    glow_l = glow_layer(
        size,
        lambda d: draw_both(d, LIME, LIME, ws=1.4, strings=False),
        (198, 255, 0, 180),
        blur=size * 0.045,
        alpha_scale=0.55,
    )
    base = Image.alpha_composite(base, glow_c)
    base = Image.alpha_composite(base, glow_l)

    # Mid glow
    mid_c = glow_layer(
        size,
        lambda d: draw_racket(d, cyan_rk, CYAN, 1.15, False),
        (0, 229, 255, 220),
        blur=size * 0.018,
        alpha_scale=0.75,
    )
    mid_l = glow_layer(
        size,
        lambda d: draw_racket(d, lime_rk, LIME, 1.15, False),
        (198, 255, 0, 220),
        blur=size * 0.018,
        alpha_scale=0.75,
    )
    base = Image.alpha_composite(base, mid_c)
    base = Image.alpha_composite(base, mid_l)

    # Crisp rackets
    crisp = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    dc = ImageDraw.Draw(crisp)
    draw_racket(dc, cyan_rk, CYAN, 1.0, True)
    draw_racket(dc, lime_rk, LIME, 1.0, True)
    # bright core
    draw_racket(dc, cyan_rk, (180, 250, 255, 255), 0.45, False)
    draw_racket(dc, lime_rk, (230, 255, 160, 255), 0.45, False)
    base = Image.alpha_composite(base, crisp)

    # Center badge plate (subtle dark disc + thin chrome)
    badge_r = size * 0.175
    badge = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    bd = ImageDraw.Draw(badge)
    # soft shadow under badge
    sh = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(sh).ellipse(
        [cx - badge_r * 1.05, cy - badge_r * 1.05 + size * 0.01,
         cx + badge_r * 1.05, cy + badge_r * 1.05 + size * 0.01],
        fill=(0, 0, 0, 90),
    )
    sh = sh.filter(ImageFilter.GaussianBlur(radius=size * 0.02))
    base = Image.alpha_composite(base, sh)

    bd.ellipse(
        [cx - badge_r, cy - badge_r, cx + badge_r, cy + badge_r],
        fill=(8, 16, 32, 255),
        outline=CHROME_MID,
        width=max(2, size // 180),
    )
    # inner neon ring
    bd.ellipse(
        [cx - badge_r * 0.92, cy - badge_r * 0.92, cx + badge_r * 0.92, cy + badge_r * 0.92],
        outline=(198, 255, 0, 90),
        width=max(1, size // 320),
    )
    base = Image.alpha_composite(base, badge)

    # Ball + shuttle inside badge (side-by-side, compact)
    icons = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    idr = ImageDraw.Draw(icons)
    ball_r = size * 0.072
    ball_cx = cx - size * 0.055
    shuttle_cx = cx + size * 0.065
    # glow behind icons
    gicons = glow_layer(
        size,
        lambda d: (
            draw_tennis_ball(d, ball_cx, cy, ball_r, LIME, BALL_SEAM),
            draw_shuttle(d, shuttle_cx, cy + size * 0.01, size * 0.13, LIME),
        ),
        (198, 255, 0, 200),
        blur=size * 0.012,
        alpha_scale=0.5,
    )
    # Actually glow_layer expects single draw - fix by drawing both in lambda properly
    def draw_badge_icons(d):
        draw_tennis_ball(d, ball_cx, cy, ball_r, LIME, BALL_SEAM)
        draw_shuttle(d, shuttle_cx, cy + size * 0.008, size * 0.125, LIME)

    gicons = glow_layer(size, draw_badge_icons, (198, 255, 0, 180), blur=size * 0.015, alpha_scale=0.6)
    base = Image.alpha_composite(base, gicons)

    draw_tennis_ball(idr, ball_cx, cy, ball_r, LIME, BALL_SEAM)
    # bright highlight on ball
    hr = ball_r * 0.28
    idr.ellipse(
        [ball_cx - ball_r * 0.35, cy - ball_r * 0.45, ball_cx - ball_r * 0.35 + hr, cy - ball_r * 0.45 + hr],
        fill=(255, 255, 220, 90),
    )
    draw_shuttle(idr, shuttle_cx, cy + size * 0.008, size * 0.125, LIME)
    base = Image.alpha_composite(base, icons)

    # SCORECORE wordmark (neon lime, premium condensed feel)
    base = draw_wordmark(base, size, cx, cy + badge_r * 0.55)

    if with_chrome and not transparent_bg:
        base = draw_chrome_rim(base)

    return base


def make_adaptive_fg(size: int) -> Image.Image:
    """Foreground layer for adaptive icon: transparent, content in safe zone (~66%)."""
    # Render at size with transparent bg, slightly scaled content already fits safe zone
    return compose_icon(size, with_chrome=False, transparent_bg=True)


def save_mipmap_set(module_res: Path, fg_master: Image.Image):
    """Write ic_launcher_foreground.png across densities (108dp base)."""
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
        img = fg_master.resize((px, px), Image.Resampling.LANCZOS)
        path = d / "ic_launcher_foreground.png"
        img.save(path, "PNG", optimize=True)
        print(f"  wrote {path} ({px}x{px})")


def main():
    print("Generating neon icon masters…")
    # High-res masters
    preview_1024 = compose_icon(1024, with_chrome=True, transparent_bg=False)
    preview_512 = compose_icon(512, with_chrome=True, transparent_bg=False)
    hi_res_512 = compose_icon(512, with_chrome=True, transparent_bg=False)

    # Soft rounded mask preview optional — keep square for Play / docs
    out_preview = OUT / "icon-neon-preview.png"
    preview_1024.save(out_preview, "PNG", optimize=True)
    print(f"wrote {out_preview} 1024")

    # Also save 512 alias next to it for convenience
    out_preview_512 = OUT / "icon-neon-preview-512.png"
    preview_512.save(out_preview_512, "PNG", optimize=True)
    print(f"wrote {out_preview_512} 512")

    out_hi = OUT / "hi-res-icon-neon.png"
    hi_res_512.save(out_hi, "PNG", optimize=True)
    print(f"wrote {out_hi} 512")

    # Adaptive foreground master (transparent) — render at 1024 then downscale
    fg_master = make_adaptive_fg(1024)

    # Also export a flat fg reference
    fg_ref = OUT / "ic_launcher_foreground_neon.png"
    fg_master.save(fg_ref, "PNG", optimize=True)
    print(f"wrote {fg_ref}")

    for mod in ("app", "wear"):
        print(f"Mipmaps for {mod}/…")
        save_mipmap_set(ROOT / mod / "src/main/res", fg_master)

    print("Done.")


if __name__ == "__main__":
    main()
