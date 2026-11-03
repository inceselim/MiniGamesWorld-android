"""Boyama kitabi kapagi: yari boyanmis kelebek + boya kalemleri."""
import os
from PIL import Image, ImageDraw
here = os.path.dirname(os.path.abspath(__file__))
src = os.path.join(here, "../../app/src/main/res/drawable-nodpi/coloring_butterfly.png")
line = Image.open(src).convert("L")
art = Image.merge("RGB", [line, line, line])
fills = [((512, 520), (255, 209, 102)), ((300, 300), (199, 125, 255)), ((724, 300), (199, 125, 255)), ((280, 380), (255, 143, 171)),
         ((340, 740), (76, 201, 240)), ((682, 740), (76, 201, 240)), ((512, 300), (255, 179, 71)), ((280, 380), (255, 143, 171)), ((744, 380), (255, 143, 171)),
         ((60, 60), (230, 245, 255))]
for (x, y), c in fills:
    if line.getpixel((x, y)) > 128: ImageDraw.floodfill(art, (x, y), c, thresh=60)
# Kenar yumusatmasi icin cizgileri tekrar carp
from PIL import ImageChops
art = ImageChops.multiply(art, Image.merge("RGB", [line, line, line]))
W, H = 960, 540
cover = Image.new("RGB", (W, H), (255, 236, 179))
d = ImageDraw.Draw(cover)
for i in range(0, W, 40): d.rectangle([i, 0, i + 20, H], fill=(255, 228, 160))
pic = art.resize((470, 470), Image.LANCZOS)
cover.paste((255, 255, 255), (60, 30, 560, 510))
cover.paste(pic, (75, 35))
# Boya kalemleri
colors = [(229, 56, 59), (247, 127, 0), (255, 209, 102), (56, 176, 0), (58, 134, 255), (123, 44, 191), (255, 143, 171)]
import math
for i, c in enumerate(colors):
    ang = math.radians(-20 + i * 7)
    x0, y0 = 640 + i * 42, 470
    L, w = 330, 34
    dx, dy = math.sin(ang), -math.cos(ang)
    nx, ny = -dy, dx
    body = [(x0 + nx * w / 2, y0 + ny * w / 2), (x0 - nx * w / 2, y0 - ny * w / 2),
            (x0 - nx * w / 2 + dx * L, y0 - ny * w / 2 + dy * L), (x0 + nx * w / 2 + dx * L, y0 + ny * w / 2 + dy * L)]
    d.polygon(body, fill=c, outline=(40, 40, 40), width=3)
    tip0 = (x0 + dx * L, y0 + dy * L)
    cone = [(tip0[0] + nx * w / 2, tip0[1] + ny * w / 2), (tip0[0] - nx * w / 2, tip0[1] - ny * w / 2), (tip0[0] + dx * 44, tip0[1] + dy * 44)]
    d.polygon(cone, fill=(242, 214, 170), outline=(40, 40, 40), width=3)
    pt = (tip0[0] + dx * 30, tip0[1] + dy * 30)
    d.polygon([(pt[0] + nx * 7, pt[1] + ny * 7), (pt[0] - nx * 7, pt[1] - ny * 7), (tip0[0] + dx * 44, tip0[1] + dy * 44)], fill=c)
    band = [(x0 + nx * w / 2 + dx * 60, y0 + ny * w / 2 + dy * 60), (x0 - nx * w / 2 + dx * 60, y0 - ny * w / 2 + dy * 60),
            (x0 - nx * w / 2 + dx * 72, y0 - ny * w / 2 + dy * 72), (x0 + nx * w / 2 + dx * 72, y0 + ny * w / 2 + dy * 72)]
    d.polygon(band, fill=(255, 255, 255))
cover.save(os.path.join(here, "../../app/src/main/res/drawable-nodpi/game_coloring.webp"), "WEBP", quality=90)
cover.save("/tmp/game_coloring.png")
