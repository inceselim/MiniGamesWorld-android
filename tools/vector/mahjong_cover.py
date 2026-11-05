"""Mahjong kapagi (960x540)."""
import os, sys, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mahjong import SETS
from mj_preview import tile, THEMES
from vec import render
from PIL import Image
random.seed(5)
W, H = 960, 540
C = SETS[0][1].faces(); faces = {k: m.faces() for k, m in SETS}
o = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W*2}" height="{H*2}" viewBox="0 0 {W} {H}">',
     '<defs><radialGradient id="bg" cx="0.5" cy="0.45" r="0.75"><stop offset="0" stop-color="#3C8A63"/><stop offset="1" stop-color="#123326"/></radialGradient></defs>',
     f'<rect width="{W}" height="{H}" fill="url(#bg)"/>']
# piramit: 3 kat
s = 0.62; ux, uy = 50 * s, 65 * s
layers = [[(x, y) for y in range(0, 8, 2) for x in range(0, 18, 2)], [(x, y) for y in range(1, 6, 2) for x in range(3, 15, 2)], [(x, y) for y in range(2, 5, 2) for x in range(6, 12, 2)], [(9, 3)]]
gid = 0
bw = 20 * ux; bh = 9 * uy
ox = (W - bw) / 2 + 10; oy = (H - bh) / 2 + 30
pick = [C[0], C[9], C[27], C[31], C[33], C[18], C[4], C[13], C[22], C[35], C[30], C[34]]
for z, L in enumerate(layers):
    for (x, y) in sorted(L, key=lambda p: (p[1], -p[0])):
        f = random.choice(pick)
        o.append(tile(f, THEMES['classic'], ox + x * ux + z * 8 * s, oy + y * uy - z * 9 * s, s, gid)); gid += 1
# kosede diger temalardan taslar
for i, (k, idx, x, y, rot) in enumerate([('fruit', 0, 40, 330, -14), ('ocean', 0, 150, 380, -6), ('night', 2, 800, 340, 10), ('forest', 13, 700, 390, 4)]):
    o.append(f'<g transform="rotate({rot} {x+40} {y+50})">' + tile(faces[k][idx], THEMES[k], x, y, 0.8, 500 + i) + '</g>')
o.append('</svg>')
render("\n".join(o), '/tmp/mj/cover.png')
im = Image.open('/tmp/mj/cover.png').convert('RGB').resize((W, H), Image.LANCZOS)
im.save(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../app/src/main/res/drawable-nodpi/game_mahjong.webp'), 'WEBP', quality=90)
im.save('/tmp/mj/game_mahjong.png')
