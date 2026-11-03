"""Maden Kosusu kapagi (RunnerScreen cizimine benzer sahte 3B)."""
import os, sys, math
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "../pixelart"))
from art import fox
from PIL import Image, ImageDraw, ImageFilter
here = os.path.dirname(os.path.abspath(__file__))
SS = 2; W, H = 960 * SS, 540 * SS
img = Image.new("RGB", (W, H)); d = ImageDraw.Draw(img)
for y in range(H):
    k = y / H; d.line([(0, y), (W, y)], fill=(int(11 + 48 * k), int(7 + 30 * k), int(5 + 15 * k)))
HOR, BOT, DEPTH, FAR = H * 0.34, H * 0.97, 8.0, 70.0
s = lambda z: DEPTH / (z + DEPTH)
Y = lambda z: HOR + (BOT - HOR) * s(z)
LW = lambda z: W * 0.2 * s(z)
X = lambda lane, z: W / 2 + lane * LW(z)
U = lambda z: LW(z) * 0.8
glow = Image.new("RGB", (W, H)); gd = ImageDraw.Draw(glow)
gd.ellipse([W / 2 - 300 * SS, HOR - 160 * SS, W / 2 + 300 * SS, HOR + 160 * SS], fill=(150, 90, 30))
glow = glow.filter(ImageFilter.GaussianBlur(80 * SS))
from PIL import ImageChops
img = ImageChops.add(img, glow); d = ImageDraw.Draw(img)
d.polygon([(0, 0), (X(-1.8, FAR), HOR - U(FAR) * 3), (X(-1.8, FAR), Y(FAR)), (X(-1.8, -0.5), H), (0, H)], fill=(52, 36, 26))
d.polygon([(W, 0), (X(1.8, FAR), HOR - U(FAR) * 3), (X(1.8, FAR), Y(FAR)), (X(1.8, -0.5), H), (W, H)], fill=(52, 36, 26))
d.polygon([(X(-1.8, FAR), Y(FAR)), (X(1.8, FAR), Y(FAR)), (X(1.8, -0.5), H), (X(-1.8, -0.5), H)], fill=(80, 56, 38))
fogc = lambda c, z: tuple(int(v + (26 - v) * min(1, z / FAR) * 0.75) for v in c)
z = 0.3
while z < FAR:
    th = max(2, U(z) * 0.12); y = Y(z)
    d.rectangle([X(-1.6, z), y - th / 2, X(1.6, z), y + th / 2], fill=fogc((92, 58, 33), z)); z += 2
for lane in (-1, 0, 1):
    for side in (-0.28, 0.28):
        d.line([(X(lane + side, -0.5), H), (X(lane + side, FAR), Y(FAR))], fill=(156, 163, 175), width=5 * SS)
for tz in (6, 18, 30, 42):
    for sx in (-1.75, 1.75):
        x, y = X(sx, tz), Y(tz) - U(tz) * 2.2; r = U(tz)
        d.rectangle([x - r * 0.06, y, x + r * 0.06, y + r * 0.5], fill=(92, 58, 33))
        d.ellipse([x - r * 0.3, y - r * 0.3, x + r * 0.3, y + r * 0.3], fill=fogc((255, 179, 71), tz))
        d.ellipse([x - r * 0.15, y - r * 0.15, x + r * 0.15, y + r * 0.15], fill=(255, 230, 140))
def rock(lane, z):
    x, y, lw, u = X(lane, z), Y(z), LW(z), U(z)
    d.ellipse([x - lw * 0.45, y - u * 1.15, x + lw * 0.45, y + u * 0.05], fill=fogc((122, 127, 135), z))
    d.ellipse([x - lw * 0.1, y - u * 0.9, x + lw * 0.4, y], fill=fogc((91, 96, 104), z))
    d.ellipse([x - lw * 0.3, y - u * 1.05, x - lw * 0.05, y - u * 0.8], fill=fogc((163, 168, 176), z))
def beam(z):
    for lane in (-1, 0, 1):
        x, y, lw, u = X(lane, z), Y(z), LW(z), U(z); top = y - u * 1.55
        d.rectangle([x - lw * 0.5, top, x + lw * 0.5, top + u * 0.32], fill=fogc((107, 66, 38), z))
        d.rectangle([x - lw * 0.5, top, x - lw * 0.44, y], fill=fogc((74, 46, 26), z))
        d.rectangle([x + lw * 0.44, top, x + lw * 0.5, y], fill=fogc((74, 46, 26), z))
        d.rectangle([x - lw * 0.3, top + u * 0.12, x + lw * 0.3, top + u * 0.2], fill=fogc((255, 209, 102), z))
def coin(lane, z):
    x, y, lw, u = X(lane, z), Y(z) - U(z) * 0.55, LW(z), U(z); r = lw * 0.16
    d.ellipse([x - r, y - r, x + r, y + r], fill=fogc((184, 134, 11), z)); d.ellipse([x - r * 0.72, y - r * 0.72, x + r * 0.72, y + r * 0.72], fill=fogc((255, 209, 102), z))
def barrel(lane, z):
    x, y, lw, u = X(lane, z), Y(z), LW(z), U(z); bw, bh = lw * 0.58, u * 0.8
    d.rounded_rectangle([x - bw / 2, y - bh, x + bw / 2, y], radius=bw * 0.25, fill=fogc((139, 90, 43), z))
    for k in (0.2, 0.75): d.rectangle([x - bw / 2, y - bh * (1 - k) - u * 0.04, x + bw / 2, y - bh * (1 - k) + u * 0.04], fill=fogc((75, 85, 99), z))
beam(30)
rock(-1, 18); rock(1, 18)
barrel(1, 8)
for i in range(5): coin(0, 4 + i * 2.5)
for i in range(3): coin(-1, 24 + i * 3)
# Oyuncu
z = 0.0; x = X(0, z); lw, u, ground = LW(z), U(z), Y(z) - U(z) * 0.05
lift = u * 0.55
base = ground - lift; cw, ch = lw * 0.78, u * 0.62
d.ellipse([x - lw * 0.42, ground - u * 0.08, x + lw * 0.42, ground + u * 0.12], fill=(20, 14, 10))
fx = fox(3).render()
fh = int(u * 1.15); fw = int(fh * fx.width / fx.height)
fximg = fx.resize((fw, fh), Image.NEAREST)
img.paste(fximg, (int(x - fw / 2), int(base - ch - fh * 0.85)), fximg); d = ImageDraw.Draw(img)
d.polygon([(x - cw / 2, base - ch), (x + cw / 2, base - ch), (x + cw * 0.42, base - u * 0.12), (x - cw * 0.42, base - u * 0.12)], fill=(110, 122, 140), outline=(45, 55, 72), width=4 * SS)
d.rectangle([x - cw / 2, base - ch, x + cw / 2, base - ch + u * 0.08], fill=(45, 55, 72))
for k in (-0.25, 0.25): d.rectangle([x + cw * k - u * 0.03, base - ch, x + cw * k + u * 0.03, base - u * 0.12], fill=(184, 134, 11))
for k in (-0.3, 0.3):
    cx, cy = x + cw * k, base - u * 0.08
    d.ellipse([cx - u * 0.14, cy - u * 0.14, cx + u * 0.14, cy + u * 0.14], fill=(26, 32, 44)); d.ellipse([cx - u * 0.06, cy - u * 0.06, cx + u * 0.06, cy + u * 0.06], fill=(113, 128, 150))
# Hiz cizgileri
for i in range(14):
    a = -math.pi / 2 + (i - 7) * 0.22
    x0 = W / 2 + math.cos(a) * W * 0.55 * (1 if i % 2 else 0.9); y0 = HOR + math.sin(a) * H * 0.9
d = ImageDraw.Draw(img)
img = img.resize((960, 540), Image.LANCZOS)
img.save(os.path.join(here, "../../app/src/main/res/drawable-nodpi/game_mine_run.webp"), "WEBP", quality=90)
img.save("/tmp/game_mine_run.png")
