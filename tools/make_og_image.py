"""Builds the 1200x630 social share image. Usage: make_og_image.py <screenshots_dir> <out.png>

<screenshots_dir> needs list9_plus.png and detail_top.png (full-size app screenshots).
"""
import sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont

shots, out = sys.argv[1], sys.argv[2]
W, H = 1200, 630
FONTS = "/usr/share/fonts/opentype/inter/"
img = Image.new("RGB", (W, H), (249, 115, 22))
d = ImageDraw.Draw(img)
bold = ImageFont.truetype(FONTS + "Inter-ExtraBold.otf", 58)
reg = ImageFont.truetype(FONTS + "Inter-SemiBold.otf", 30)


def rounded(size, radius):
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size[0] - 1, size[1] - 1), radius, fill=255)
    return mask


# App icon with a white edge so it stands out from the orange background.
d.rounded_rectangle((64, 74, 196, 206), 32, fill="white")
icon = Image.open("store/play-store-icon-512.png").convert("RGBA").resize((120, 120))
img.paste(icon, (70, 80), rounded((120, 120), 28))

d.text((70, 230), "Invoice Maker", font=bold, fill="white")
d.text((70, 298), "Small Business", font=bold, fill=(255, 237, 213))
for i, line in enumerate(["Unlimited free invoices", "Multiple currencies", "Your data stays on your phone"]):
    d.text((70, 400 + i * 48), "• " + line, font=reg, fill="white")

for name, x, y in (("detail_top", 820, 70), ("list9_plus", 660, 120)):
    shot = Image.open(f"{shots}/{name}.png").convert("RGB").resize((250, 542))
    frame = Image.new("RGBA", (266, 558), (0, 0, 0, 0))
    ImageDraw.Draw(frame).rounded_rectangle((0, 0, 265, 557), 30, fill=(17, 24, 39))
    frame.paste(shot, (8, 8), rounded((250, 542), 24))
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((x + 8, y + 14, x + 274, y + 572), 30, fill=(0, 0, 0, 90))
    shadow = shadow.filter(ImageFilter.GaussianBlur(12))
    img.paste(shadow, (0, 0), shadow)
    img.paste(frame, (x, y), frame)

img.save(out, optimize=True)
