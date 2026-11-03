"""Pasta Dukkani kapak gorseli."""
import os, random
from PIL import Image
from vec import render
W, H = 960, 540
random.seed(9)
s = ['<defs><linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFE8D6"/><stop offset="1" stop-color="#FFC9DE"/></linearGradient>',
     '<linearGradient id="side" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#6A3E22"/><stop offset="0.5" stop-color="#8B5530"/><stop offset="1" stop-color="#5C341C"/></linearGradient>',
     '<linearGradient id="counter" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#F4B183"/><stop offset="1" stop-color="#D98A55"/></linearGradient></defs>',
     f'<rect width="{W}" height="{H}" fill="url(#bg)"/>']
# Duvar seritleri ve raflar
for i in range(0, W, 60): s.append(f'<rect x="{i}" y="0" width="30" height="{H}" fill="#FFFFFF" fill-opacity="0.18"/>')
s.append(f'<rect x="0" y="420" width="{W}" height="120" fill="url(#counter)"/>')
s.append(f'<rect x="0" y="414" width="{W}" height="12" fill="#B5683A"/>')
# Yan tarafta kek ve kurabiye
for (x, y, c) in [(110, 340, '#F7A8B8'), (840, 350, '#F6D98B')]:
    s.append(f'<ellipse cx="{x}" cy="{y+70}" rx="70" ry="16" fill="#FFFFFF"/>')
    s.append(f'<path d="M{x-48} {y} L{x-40} {y+64} L{x+40} {y+64} L{x+48} {y} Z" fill="#E9A06B" stroke="#9A5A2E" stroke-width="3"/>')
    s.append(f'<path d="M{x-56} {y+4} C{x-70} {y-50} {x+70} {y-50} {x+56} {y+4} Z" fill="{c}" stroke="#B5466E" stroke-width="3"/>')
    s.append(f'<circle cx="{x}" cy="{y-36}" r="11" fill="#D90429" stroke="#7A0010" stroke-width="2"/>')
# Ana pasta
cx, top, cw, ry, bh = 480, 210, 420, 62, 170
L, R, B = cx - cw / 2, cx + cw / 2, top + bh
s.append(f'<ellipse cx="{cx}" cy="{B+18}" rx="{cw*0.62}" ry="{ry*1.25}" fill="#FFFFFF" stroke="#E5E7EB" stroke-width="4"/>')
s.append(f'<path d="M{L} {top} L{L} {B} A{cw/2} {ry} 0 0 0 {R} {B} L{R} {top} Z" fill="url(#side)" stroke="#3B2414" stroke-width="3"/>')
s.append(f'<rect x="{L}" y="{top+bh*0.55}" width="{cw}" height="14" fill="#FFF3E6"/>')
# Krema damlalari
drips = [f'M{L} {top}']
n = 10
for i in range(n):
    x0 = L + cw * i / n; x1 = L + cw * (i + 1) / n; d = random.uniform(40, 95)
    drips.append(f'L{x0} {top+d*0.5} C{x0} {top+d} {x1} {top+d} {x1} {top+d*0.5}')
drips.append(f'L{R} {top} Z')
s.append(f'<path d="{" ".join(drips)}" fill="#FF8FC0" stroke="#D9468A" stroke-width="3"/>')
s.append(f'<ellipse cx="{cx}" cy="{top}" rx="{cw/2}" ry="{ry}" fill="#FFA6C9" stroke="#D9468A" stroke-width="3"/>')
s.append(f'<ellipse cx="{cx-90}" cy="{top-20}" rx="70" ry="14" fill="#FFFFFF" fill-opacity="0.35"/>')
# Serpme seker
for _ in range(60):
    x = random.uniform(L + 30, R - 30); y = top + random.uniform(-ry * 0.8, ry * 0.8)
    if ((x - cx) / (cw / 2)) ** 2 + ((y - top) / ry) ** 2 > 0.8: continue
    c = random.choice(['#4CC9F0', '#FFD166', '#06D6A0', '#FFFFFF', '#9D4EDD'])
    a = random.uniform(0, 180)
    s.append(f'<rect x="{x-6}" y="{y-2}" width="12" height="4" rx="2" fill="{c}" transform="rotate({a:.0f} {x:.0f} {y:.0f})"/>')
# Mumlar ve kirazlar
for x, y in [(cx - 80, top - 10), (cx, top - 24), (cx + 80, top - 10)]:
    s.append(f'<rect x="{x-9}" y="{y-90}" width="18" height="90" rx="4" fill="#4CC9F0" stroke="#1D6F8C" stroke-width="3"/>')
    for k in range(3): s.append(f'<rect x="{x-9}" y="{y-80+k*28}" width="18" height="8" fill="#FFFFFF" fill-opacity="0.8"/>')
    s.append(f'<path d="M{x} {y-128} C{x+16} {y-104} {x+10} {y-92} {x} {y-92} C{x-10} {y-92} {x-16} {y-104} {x} {y-128} Z" fill="#FFD166" stroke="#F77F00" stroke-width="3"/>')
    s.append(f'<ellipse cx="{x}" cy="{y-100}" rx="26" ry="26" fill="#FFE066" fill-opacity="0.25"/>')
for x, y in [(cx - 150, top + 6), (cx + 150, top + 6), (cx - 40, top + 30), (cx + 40, top + 30)]:
    s.append(f'<path d="M{x} {y-14} Q{x+8} {y-40} {x+22} {y-46}" stroke="#2D6A4F" stroke-width="4" fill="none"/>')
    s.append(f'<circle cx="{x}" cy="{y}" r="17" fill="#D90429" stroke="#7A0010" stroke-width="3"/><circle cx="{x-6}" cy="{y-6}" r="5" fill="#FFFFFF" fill-opacity="0.6"/>')
for _ in range(25):
    s.append(f'<circle cx="{random.uniform(0, W):.0f}" cy="{random.uniform(0, 400):.0f}" r="{random.uniform(2, 4):.1f}" fill="#FFFFFF" fill-opacity="0.8"/>')
svgtext = f'<svg xmlns="http://www.w3.org/2000/svg" width="{W*2}" height="{H*2}" viewBox="0 0 {W} {H}">' + "\n".join(s) + '</svg>'
render(svgtext, "/tmp/cake_cover.png")
im = Image.open("/tmp/cake_cover.png").convert("RGB").resize((W, H), Image.LANCZOS)
im.save(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../app/src/main/res/drawable-nodpi/game_cake_shop.webp"), "WEBP", quality=90)
im.save("/tmp/game_cake_shop.png")
