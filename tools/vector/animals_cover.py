"""Hayvan Dostlari kapagi."""
import os, sys, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from animals import A, ORDER
from vec import render, resolve, shade
from PIL import Image
W, H = 960, 540
random.seed(5)
def group(parts, tx, ty, s):
    g = [f'<g transform="translate({tx},{ty}) scale({s})">']
    for d_, slot, st in parts:
        c, a = resolve(slot, {})
        stroke = f' stroke="{shade(c, 0.55)}" stroke-width="1.6" stroke-linejoin="round" stroke-opacity="0.85"' if st else ''
        g.append(f'<path d="{d_}" fill="{c}" fill-opacity="{a:.2f}"{stroke}/>')
    g.append('</g>'); return "\n".join(g)
bg = ['<defs><linearGradient id="g" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#8ED6FF"/><stop offset="1" stop-color="#D9F4FF"/></linearGradient></defs>',
      f'<rect width="{W}" height="{H}" fill="url(#g)"/>',
      '<ellipse cx="200" cy="80" rx="90" ry="30" fill="#FFFFFF"/><ellipse cx="760" cy="60" rx="110" ry="32" fill="#FFFFFF"/>',
      f'<ellipse cx="{W/2}" cy="{H+120}" rx="{W*0.8}" ry="260" fill="#95D5B2"/>',
      f'<ellipse cx="{W*0.2}" cy="{H+60}" rx="{W*0.4}" ry="200" fill="#74C69D"/>']
for _ in range(30):
    x, y = random.uniform(0, W), random.uniform(380, 540)
    c = random.choice(['#FF5C9E', '#FFD166', '#FFFFFF', '#C77DFF'])
    bg.append(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="7" fill="{c}"/><circle cx="{x:.0f}" cy="{y:.0f}" r="2.5" fill="#FFE066"/>')
slots = [('lion', 360, 110, 1.25), ('dog', 120, 210, 1.05), ('cat', 600, 200, 1.05), ('elephant', 10, 330, 0.95), ('duck', 790, 330, 0.85),
         ('frog', 230, 360, 0.85), ('monkey', 480, 330, 0.95), ('cow', 760, 90, 0.85), ('pig', -20, 60, 0.8)]
body = "".join(group(A[k], x, y, s) for k, x, y, s in slots)
svgtext = f'<svg xmlns="http://www.w3.org/2000/svg" width="{W*2}" height="{H*2}" viewBox="0 0 {W} {H}">' + "\n".join(bg) + body + '</svg>'
render(svgtext, "/tmp/animals_cover.png")
im = Image.open("/tmp/animals_cover.png").convert("RGB").resize((W, H), Image.LANCZOS)
im.save(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../app/src/main/res/drawable-nodpi/game_animals.webp"), "WEBP", quality=90)
im.save("/tmp/game_animals.png")
