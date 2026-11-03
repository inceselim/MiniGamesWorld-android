"""Moda Sovu kapak gorseli (960x540)."""
import os, sys, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dressup import *

def look_parts(lk, colors):
    out = list(HAIR[lk['hair']]['back']) + BODY + BASE
    def add(cat, k, col):
        if not k: return
        for d_, slot, st in cat[k]['parts']:
            if slot.startswith('main'):
                suf = slot[4:]
                slot = shade(col, 0.78) if suf == 'D' else shade(col, 1.35) if suf == 'L' else col
            out.append((d_, slot, st))
    add(BOTTOMS, lk.get('bottom'), lk.get('cb', '#3A6EA5')); add(TOPS, lk.get('top'), lk.get('ct', '#5BC0EB'))
    add(DRESSES, lk.get('dress'), lk.get('cd', '#E5383B')); add(SHOES, lk.get('shoes'), lk.get('cs', '#D00000'))
    out += HANDS + FACE
    if lk.get('extra') and lk['extra'] != 'bag': add(EXTRA, lk['extra'], lk.get('ce', '#4CC9F0'))
    out += HAIR[lk['hair']]['front']; add(HEAD, lk.get('head'), lk.get('ch', '#FF4D6D'))
    if lk.get('extra') == 'bag': add(EXTRA, 'bag', lk.get('ce', '#B5179E'))
    return out

W, H = 960, 540
random.seed(4)
bg = [f'<defs><linearGradient id="g" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#7B2CBF"/><stop offset="1" stop-color="#FF5C9E"/></linearGradient>'
      f'<radialGradient id="s" cx="0.5" cy="0.4" r="0.6"><stop offset="0" stop-color="#FFFFFF" stop-opacity="0.45"/><stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/></radialGradient></defs>',
      f'<rect width="{W}" height="{H}" fill="url(#g)"/>', f'<rect width="{W}" height="{H}" fill="url(#s)"/>']
for _ in range(70):
    x, y, r = random.uniform(0, W), random.uniform(0, H * 0.8), random.uniform(1.5, 4)
    bg.append(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="{r:.1f}" fill="#FFFFFF" fill-opacity="{random.uniform(0.4, 0.9):.2f}"/>')
bg.append(f'<ellipse cx="{W/2}" cy="{H+40}" rx="{W*0.7}" ry="140" fill="#3C096C" fill-opacity="0.55"/>')
looks = [
    (dict(hair='ponytail', dress='party', shoes='heels', extra='necklace', cd='#E5383B'), dict(skin='#E8B48F', hair='#F2C14E'), 130, 0.9),
    (dict(hair='buns', dress='ball', shoes='heels', head='crown', cd='#F2A7C3'), dict(skin='#FCE3D3', hair='#8E44AD'), 480, 1.03),
    (dict(hair='curly', top='crop', bottom='tutu', shoes='sneakers', head='flower', ct='#4CC9F0', cb='#FF8FAB', cs='#06D6A0', ch='#FFD166'), dict(skin='#8D5524', hair='#1B1B1B'), 830, 0.9),
]
groups = []
for lk, cols, cx, sc in looks:
    s = sc * 1.25
    parts = look_parts(lk, {})
    g = [f'<g transform="translate({cx - 100 * s:.1f},{H - 400 * s - 4:.1f}) scale({s:.3f})">']
    for d_, slot, st in parts:
        c, a = resolve(slot, cols)
        stroke = f' stroke="{shade(c, 0.55)}" stroke-width="1.4" stroke-linejoin="round" stroke-opacity="0.85"' if st else ''
        g.append(f'<path d="{d_}" fill="{c}" fill-opacity="{a:.2f}"{stroke}/>')
    g.append('</g>')
    groups.append("\n".join(g))
svgtext = f'<svg xmlns="http://www.w3.org/2000/svg" width="{W*2}" height="{H*2}" viewBox="0 0 {W} {H}">' + "\n".join(bg) + "\n".join([groups[0], groups[2], groups[1]]) + '</svg>'
render(svgtext, "/tmp/dressup_cover.png")
from PIL import Image
im = Image.open("/tmp/dressup_cover.png").convert("RGB").resize((W, H), Image.LANCZOS)
im.save(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../app/src/main/res/drawable-nodpi/game_dress_up.webp"), "WEBP", quality=90)
im.save("/tmp/game_dress_up.png")
