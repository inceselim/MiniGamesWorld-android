"""8 Top Bilardo kapagi."""
import os, math, random
from PIL import Image, ImageDraw, ImageFont, ImageFilter
here = os.path.dirname(os.path.abspath(__file__))
SS = 2; W, H = 960 * SS, 540 * SS
img = Image.new("RGB", (W, H), (20, 14, 10)); d = ImageDraw.Draw(img)
for y in range(H):
    k = y / H; d.line([(0, y), (W, y)], fill=(int(40 - 20 * k), int(26 - 12 * k), int(18 - 8 * k)))
rail = 46 * SS; tl, tt, tr, tb = 70 * SS, 60 * SS, W - 70 * SS, H - 60 * SS
d.rounded_rectangle([tl - rail, tt - rail, tr + rail, tb + rail], radius=40 * SS, fill=(110, 64, 32))
d.rounded_rectangle([tl - rail + 10 * SS, tt - rail + 10 * SS, tr + rail - 10 * SS, tb + rail - 10 * SS], radius=32 * SS, fill=(90, 50, 24))
d.rectangle([tl - 14 * SS, tt - 14 * SS, tr + 14 * SS, tb + 14 * SS], fill=(11, 110, 58))
felt = Image.new("RGB", (tr - tl, tb - tt), (14, 122, 64)); fd = ImageDraw.Draw(felt)
for r in range(int((tr - tl) * 0.6), 0, -12 * SS):
    c = int(26 + 12 * (1 - r / ((tr - tl) * 0.6)))
    fd.ellipse([(tr - tl) / 2 - r, (tb - tt) / 2 - r * 0.6, (tr - tl) / 2 + r, (tb - tt) / 2 + r * 0.6], fill=(c, 140 + c // 3, 70 + c // 4))
img.paste(felt, (tl, tt)); d = ImageDraw.Draw(img)
for k in range(1, 8):
    x = tl + (tr - tl) * k / 8
    for y in (tt - rail * 0.6, tb + rail * 0.6): d.ellipse([x - 5 * SS, y - 5 * SS, x + 5 * SS, y + 5 * SS], fill=(242, 227, 198))
for (px, py) in [(tl, tt), ((tl + tr) / 2, tt - 6 * SS), (tr, tt), (tl, tb), ((tl + tr) / 2, tb + 6 * SS), (tr, tb)]:
    r = 30 * SS
    d.ellipse([px - r, py - r, px + r, py + r], fill=(8, 8, 8))
COLORS = [(255, 255, 245), (247, 201, 72), (37, 99, 235), (220, 38, 38), (124, 58, 237), (249, 115, 22), (22, 163, 74), (127, 29, 29), (17, 17, 17)] * 2
R = 22 * SS
try:
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", int(R * 0.7))
except Exception:
    font = ImageFont.load_default()
def ball(cx, cy, n):
    d.ellipse([cx - R + 4 * SS, cy - R + 6 * SS, cx + R + 4 * SS, cy + R + 6 * SS], fill=(6, 60, 30))
    col = COLORS[n if n <= 8 else n - 8]
    if n >= 9:
        d.ellipse([cx - R, cy - R, cx + R, cy + R], fill=(255, 255, 245))
        band = Image.new("L", (2 * R, 2 * R), 0); bd = ImageDraw.Draw(band); bd.ellipse([0, 0, 2 * R - 1, 2 * R - 1], fill=255)
        strip = Image.new("L", (2 * R, 2 * R), 0); ImageDraw.Draw(strip).rectangle([0, R * 0.45, 2 * R, R * 1.55], fill=255)
        from PIL import ImageChops
        m = ImageChops.multiply(band, strip)
        img.paste(Image.new("RGB", (2 * R, 2 * R), col), (int(cx - R), int(cy - R)), m)
    else:
        d.ellipse([cx - R, cy - R, cx + R, cy + R], fill=col)
    if n:
        d.ellipse([cx - R * 0.45, cy - R * 0.45, cx + R * 0.45, cy + R * 0.45], fill=(255, 255, 245))
        d.text((cx, cy + 1), str(n), fill=(0, 0, 0), font=font, anchor="mm")
    d.ellipse([cx - R * 0.6, cy - R * 0.65, cx - R * 0.15, cy - R * 0.25], fill=tuple(min(255, c + 90) for c in col) if n != 0 else (255, 255, 255))
order = [1, 9, 2, 10, 8, 3, 11, 4, 12, 5, 13, 6, 14, 7, 15]
ax, ay, k = tl + (tr - tl) * 0.68, (tt + tb) / 2, 0
for row in range(5):
    for i in range(row + 1):
        ball(ax + row * R * 1.75, ay + (i - row / 2) * R * 2.02, order[k]); k += 1
cx, cy = tl + (tr - tl) * 0.24, (tt + tb) / 2 + 30 * SS
for t in range(0, int(ax - cx - R * 2), 22 * SS):
    d.line([(cx + t, cy - t * 0.12), (cx + t + 11 * SS, cy - (t + 11 * SS) * 0.12)], fill=(255, 255, 255), width=3 * SS)
ball(cx, cy, 0)
ang = math.atan2(-0.12, 1)
L0, L1 = R * 1.8, R * 1.8 + 520 * SS
sx, sy = cx - math.cos(ang) * L0, cy - math.sin(ang) * L0
ex, ey = cx - math.cos(ang) * L1, cy - math.sin(ang) * L1
d.line([(sx, sy), (ex, ey)], fill=(217, 160, 102), width=14 * SS)
mx, my = cx - math.cos(ang) * (L0 + 300 * SS), cy - math.sin(ang) * (L0 + 300 * SS)
d.line([(mx, my), (ex, ey)], fill=(59, 36, 20), width=17 * SS)
d.line([(sx, sy), (sx - math.cos(ang) * 10 * SS, sy - math.sin(ang) * 10 * SS)], fill=(96, 165, 250), width=14 * SS)
img = img.resize((960, 540), Image.LANCZOS)
img.save(os.path.join(here, "../../app/src/main/res/drawable-nodpi/game_pool.webp"), "WEBP", quality=90)
img.save("/tmp/game_pool.png")
