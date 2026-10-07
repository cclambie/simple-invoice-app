"""Draws the launcher icon with PIL. Usage: make_icon.py <out_dir> <res_dir|-> [--free]

Coordinates are in adaptive-icon dp (108 x 108 canvas, 66dp safe circle in the middle),
drawn at SCALE x and downsampled for smooth edges.
"""
import os
import sys
from PIL import Image, ImageDraw, ImageFont

ORANGE = (249, 115, 22)
ORANGE_DARK = (194, 65, 12)
GREY = (209, 213, 219)
WHITE = (255, 255, 255)
FONT = "/usr/share/fonts/opentype/inter/Inter-ExtraBold.otf"
SCALE = 16
S = 108 * SCALE

out_dir, res_dir = sys.argv[1], sys.argv[2]
with_free = "--free" in sys.argv


def p(*v):
    return [round(x * SCALE) for x in v]


def text_centered(draw, cx, cy, text, size, fill):
    font = ImageFont.truetype(FONT, round(size * SCALE))
    l, t, r, b = draw.textbbox((0, 0), text, font=font)
    draw.text((cx * SCALE - (l + r) / 2, cy * SCALE - (t + b) / 2), text, font=font, fill=fill)


def foreground(mono=False):
    """Document on a transparent canvas. mono: white shape with text and lines knocked out."""
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    hole = (0, 0, 0, 0)
    ink = hole if mono else ORANGE_DARK
    lines = hole if mono else GREY
    accent = hole if mono else ORANGE

    # Document with a folded top-right corner. Corners stay inside the 33dp-radius safe circle.
    x0, y0, x1, y1, fold = 35, 28, 73, 80, 10
    d.rounded_rectangle(p(x0, y0, x1, y1), radius=3 * SCALE, fill=WHITE)
    d.polygon(p(x1 - fold, y0, x1 + 1, y0, x1 + 1, y0 + fold + 1), fill=(0, 0, 0, 0))
    d.polygon(p(x1 - fold, y0, x1 - fold, y0 + fold, x1, y0 + fold), fill=hole if mono else GREY)

    text_centered(d, 53, 45, "INVOICE", 6.6, ink)
    for y, w in ((54, 26), (59.5, 20), (65, 24)):
        d.rounded_rectangle(p(40, y, 40 + w, y + 2.2), radius=1.1 * SCALE, fill=lines)
    if with_free:
        d.rounded_rectangle(p(39, 69.5, 67, 77.5), radius=4 * SCALE, fill=hole if mono else ORANGE)
        text_centered(d, 53, 73.6, "FREE", 5.6, hole if mono else WHITE)
    else:
        d.rounded_rectangle(p(52, 71, 68, 74.5), radius=1.7 * SCALE, fill=accent)
    return img


def full_square(fg):
    bg = Image.new("RGBA", (S, S), ORANGE + (255,))
    bg.alpha_composite(fg)
    return bg


def masked(img, shape, size):
    """Preview through a launcher mask: the 72dp visible area of the 108dp canvas."""
    crop = img.crop(p(18, 18, 90, 90)).resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size * 4, size * 4), 0)
    md = ImageDraw.Draw(mask)
    if shape == "circle":
        md.ellipse((0, 0, size * 4, size * 4), fill=255)
    else:
        md.rounded_rectangle((0, 0, size * 4, size * 4), radius=size * 4 * 0.3, fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(crop, (0, 0), mask.resize((size, size), Image.LANCZOS))
    return out


os.makedirs(out_dir, exist_ok=True)
fg = foreground()
full = full_square(fg)
tag = "free" if with_free else "plain"

# Previews: big, home-screen sized, and themed (monochrome) on a tinted background.
sheet = Image.new("RGBA", (3 * 220 + 40, 260), (32, 33, 36, 255))
sheet.paste(masked(full, "circle", 200), (20, 30), masked(full, "circle", 200))
sheet.paste(masked(full, "squircle", 200), (240, 30), masked(full, "squircle", 200))
small = masked(full, "circle", 96)
sheet.paste(small, (480, 40), small)
small2 = masked(full, "squircle", 48)
sheet.paste(small2, (600, 64), small2)
mono = Image.new("RGBA", (S, S), (60, 72, 88, 255))
mono_fg = foreground(mono=True)
tint = Image.new("RGBA", (S, S), (200, 220, 255, 255))
mono.paste(tint, (0, 0), mono_fg)
m = masked(mono, "circle", 96)
sheet.paste(m, (480, 150), m)
sheet.save(os.path.join(out_dir, f"preview-{tag}.png"))

full.crop(p(0, 0, 108, 108)).resize((512, 512), Image.LANCZOS).convert("RGB").save(
    os.path.join(out_dir, f"play-store-icon-{tag}.png"))

if res_dir != "-":
    for density, px in (("mdpi", 108), ("hdpi", 162), ("xhdpi", 216), ("xxhdpi", 324), ("xxxhdpi", 432)):
        d = os.path.join(res_dir, f"mipmap-{density}")
        os.makedirs(d, exist_ok=True)
        fg.resize((px, px), Image.LANCZOS).save(os.path.join(d, "ic_launcher_foreground.png"))
        mono_fg.resize((px, px), Image.LANCZOS).save(os.path.join(d, "ic_launcher_monochrome.png"))
