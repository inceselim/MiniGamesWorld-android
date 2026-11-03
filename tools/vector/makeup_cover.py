"""Makyaj Salonu kapak gorseli."""
import os, sys, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from makeup import *
from PIL import Image

W, H = 960, 540
cols = dict(skin='#F6D2B8', hair='#8E44AD', main='#FF5C9E', lips='#D98A8A')
parts = HAIR['long']['back'] + BODY + [(SHADOW_SHAPE, '#B3C77DFF', False)] + EYES + LASHES[2] + BROWS + NOSE + LIPS_BASE + \
        [(LIPS_SHAPE, '#EBE5383B', False)] + LIPS_TOP + HAIR['long']['front'] + [(sym(el(100, 212, 24, 15)).d(), '#66FF5C8A', False)]
def group(parts, tx, ty, s, colors):
    g = [f'<g transform="translate({tx},{ty}) scale({s})">']
    for d_, slot, st in parts:
        c, a = resolve(slot, colors)
        stroke = f' stroke="{shade(c, 0.55)}" stroke-width="1.4" stroke-linejoin="round" stroke-opacity="0.85"' if st else ''
        g.append(f'<path d="{d_}" fill="{c}" fill-opacity="{a:.2f}"{stroke}/>')
    g.append('</g>'); return "\n".join(g)
random.seed(2)
bg = ['<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#FFD6E8"/><stop offset="1" stop-color="#FF8FC0"/></linearGradient></defs>',
      f'<rect width="{W}" height="{H}" fill="url(#g)"/>']
for _ in range(40):
    x, y = random.uniform(0, W), random.uniform(0, H)
    bg.append(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="{random.uniform(2, 5):.1f}" fill="#FFFFFF" fill-opacity="0.7"/>')
# Ruj (sol)
props = ['<g transform="translate(150,140) rotate(-18)">',
         '<rect x="-34" y="120" width="68" height="150" rx="10" fill="#2B2B33" stroke="#111" stroke-width="3"/>',
         '<rect x="-38" y="104" width="76" height="26" rx="6" fill="#F7C948" stroke="#8A6100" stroke-width="3"/>',
         '<path d="M-26 104 L-26 30 Q-26 0 26 -24 L26 104 Z" fill="#E5383B" stroke="#8B1020" stroke-width="3"/>',
         '<path d="M-14 96 L-14 40 Q-12 22 4 12 L4 96 Z" fill="#FFFFFF" fill-opacity="0.25"/>', '</g>',
         # Far paleti (sag)
         '<g transform="translate(800,300) rotate(10)">',
         '<rect x="-110" y="-80" width="220" height="160" rx="22" fill="#2B2B33" stroke="#111" stroke-width="4"/>']
for i, c in enumerate(['#C77DFF', '#4CC9F0', '#FFD166', '#FF8FAB', '#06D6A0', '#F4A261']):
    x = -70 + (i % 3) * 70; y = -36 + (i // 3) * 72
    props.append(f'<circle cx="{x}" cy="{y}" r="28" fill="{c}" stroke="#111" stroke-width="3"/><circle cx="{x-8}" cy="{y-8}" r="8" fill="#FFFFFF" fill-opacity="0.35"/>')
props.append('</g>')
props += ['<g transform="translate(790,90) rotate(35)">', '<rect x="-10" y="0" width="20" height="150" rx="8" fill="#C08457" stroke="#5C3A21" stroke-width="3"/>',
          '<rect x="-14" y="-20" width="28" height="30" fill="#BFC5CC" stroke="#555" stroke-width="3"/>',
          '<path d="M-16 -20 Q-26 -70 0 -90 Q26 -70 16 -20 Z" fill="#FF8FAB" stroke="#B5466E" stroke-width="3"/>', '</g>']
svgtext = f'<svg xmlns="http://www.w3.org/2000/svg" width="{W*2}" height="{H*2}" viewBox="0 0 {W} {H}">' + "\n".join(bg) + group(parts, 300, -10, 1.53, cols) + "\n".join(props) + '</svg>'
render(svgtext, "/tmp/makeup_cover.png")
im = Image.open("/tmp/makeup_cover.png").convert("RGB").resize((W, H), Image.LANCZOS)
im.save(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../app/src/main/res/drawable-nodpi/game_makeup.webp"), "WEBP", quality=90)
im.save("/tmp/game_makeup.png")
