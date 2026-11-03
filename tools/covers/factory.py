"""Fabrika Patronu kapagi."""
import os, sys, random
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "../vector"))
from vec import render
from PIL import Image
W, H = 960, 540
random.seed(8)
s = ['<defs><linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FDBA74"/><stop offset="0.6" stop-color="#FDE68A"/><stop offset="1" stop-color="#FEF3C7"/></linearGradient>',
     '<linearGradient id="wall" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#B45309"/><stop offset="1" stop-color="#92400E"/></linearGradient>',
     '<linearGradient id="wall2" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#64748B"/><stop offset="1" stop-color="#475569"/></linearGradient></defs>',
     f'<rect width="{W}" height="{H}" fill="url(#sky)"/>', '<circle cx="820" cy="110" r="60" fill="#FFF7D6"/>']
# Uzak sehir silueti
x = 0
while x < W:
    w = random.randint(40, 90); h = random.randint(60, 160)
    s.append(f'<rect x="{x}" y="{360-h}" width="{w}" height="{h}" fill="#F59E0B" fill-opacity="0.35"/>'); x += w + random.randint(0, 20)
# Duman
for cx, cy in [(250, 120), (430, 90), (610, 130)]:
    for k in range(5):
        r = 22 + k * 9
        s.append(f'<circle cx="{cx + k * 22}" cy="{cy - k * 26}" r="{r}" fill="#E5E7EB" fill-opacity="{0.85 - k * 0.14:.2f}"/>')
# Bacalar
for cx in (240, 420, 600):
    s.append(f'<rect x="{cx-18}" y="140" width="36" height="200" fill="#7F1D1D"/><rect x="{cx-22}" y="140" width="44" height="18" fill="#991B1B"/>')
    for k in range(4): s.append(f'<rect x="{cx-18}" y="{180 + k*40}" width="36" height="6" fill="#FFFFFF" fill-opacity="0.5"/>')
# Testere cati fabrika
s.append('<rect x="150" y="260" width="560" height="160" fill="url(#wall)"/>')
for i in range(7):
    x0 = 150 + i * 80
    s.append(f'<polygon points="{x0},260 {x0+80},210 {x0+80},260" fill="#78350F"/><polygon points="{x0+52},228 {x0+78},212 {x0+78},258 {x0+52},258" fill="#93C5FD"/>')
for i in range(6):
    s.append(f'<rect x="{180 + i*85}" y="300" width="50" height="40" rx="4" fill="#FDE68A" stroke="#78350F" stroke-width="4"/>')
s.append('<rect x="390" y="350" width="80" height="70" fill="#334155" stroke="#1E293B" stroke-width="4"/>')
# Ikinci bina
s.append('<rect x="700" y="300" width="200" height="120" fill="url(#wall2)"/><polygon points="700,300 800,250 900,300" fill="#334155"/>')
for i in range(3): s.append(f'<rect x="{720 + i*60}" y="330" width="40" height="34" fill="#BAE6FD" stroke="#1E293B" stroke-width="3"/>')
# Zemin ve bant
s.append(f'<rect x="0" y="420" width="{W}" height="{H-420}" fill="#57534E"/>')
s.append(f'<rect x="0" y="448" width="{W}" height="30" rx="8" fill="#334155"/>')
for i in range(0, W, 40): s.append(f'<line x1="{i}" y1="448" x2="{i+14}" y2="478" stroke="#64748B" stroke-width="4"/>')
for i, (x0, c) in enumerate([(40, '#D97706'), (180, '#B45309'), (330, '#D97706'), (520, '#92400E'), (690, '#D97706'), (840, '#B45309')]):
    s.append(f'<rect x="{x0}" y="402" width="70" height="48" rx="4" fill="{c}" stroke="#78350F" stroke-width="4"/>'
             f'<line x1="{x0}" y1="426" x2="{x0+70}" y2="426" stroke="#78350F" stroke-width="3"/><rect x="{x0+28}" y="402" width="14" height="48" fill="#FBBF24" fill-opacity="0.6"/>')
# Altin paralar
for x0, y0 in [(110, 500), (470, 506), (760, 498), (900, 510)]:
    s.append(f'<ellipse cx="{x0}" cy="{y0}" rx="22" ry="22" fill="#F59E0B" stroke="#92400E" stroke-width="4"/><text x="{x0}" y="{y0+8}" font-family="DejaVu Sans" font-weight="bold" font-size="24" text-anchor="middle" fill="#92400E">$</text>')
svgtext = f'<svg xmlns="http://www.w3.org/2000/svg" width="{W*2}" height="{H*2}" viewBox="0 0 {W} {H}">' + "\n".join(s) + '</svg>'
render(svgtext, "/tmp/factory_cover.png")
im = Image.open("/tmp/factory_cover.png").convert("RGB").resize((W, H), Image.LANCZOS)
im.save(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../app/src/main/res/drawable-nodpi/game_factory.webp"), "WEBP", quality=90)
im.save("/tmp/game_factory.png")
