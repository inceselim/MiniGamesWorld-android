"""Yapboz kapagi: Tilki kapagindan kesilmis parcalar (JigsawScreen ile ayni kulakcik egrisi)."""
import os, sys, base64, random, math
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "../vector"))
from vec import render
from PIL import Image
here = os.path.dirname(os.path.abspath(__file__))
src = os.path.join(here, "../../app/src/main/res/drawable-nodpi/game_fox_adventure.webp")
Image.open(src).convert("RGB").save("/tmp/jg_src.png")
b64 = base64.b64encode(open("/tmp/jg_src.png", "rb").read()).decode()
W, H, COLS, ROWS = 960, 540, 6, 4
uw, uh = W * 0.8 / COLS, H * 0.8 / ROWS
random.seed(3)
hE = [[random.choice([1, -1]) for _ in range(COLS - 1)] for _ in range(ROWS)]
vE = [[random.choice([1, -1]) for _ in range(COLS)] for _ in range(ROWS - 1)]
TAB = [0.40, 0, 0.42, 0.08, 0.38, 0.14, 0.32, 0.24, 0.40, 0.30, 0.50, 0.30, 0.60, 0.30, 0.68, 0.24, 0.62, 0.14, 0.58, 0.08, 0.60, 0, 0.66, 0]
def edge(ax, ay, bx, by, sign, s):
    if sign == 0: return f"L{bx:.1f} {by:.1f}"
    ln = math.hypot(bx - ax, by - ay); dx, dy = (bx - ax) / ln, (by - ay) / ln; nx, ny = dy, -dx
    P = lambda t, k: (ax + dx * ln * t + nx * k * s * sign, ay + dy * ln * t + ny * k * s * sign)
    out = ["L%.1f %.1f" % P(0.34, 0)]
    for i in range(0, len(TAB), 6):
        a, b, c = P(TAB[i], TAB[i+1]), P(TAB[i+2], TAB[i+3]), P(TAB[i+4], TAB[i+5])
        out.append("C%.1f %.1f %.1f %.1f %.1f %.1f" % (*a, *b, *c))
    out.append(f"L{bx:.1f} {by:.1f}")
    return " ".join(out)
def piece(r, c):
    e = [0 if r == 0 else -vE[r-1][c], 0 if c == COLS-1 else hE[r][c], 0 if r == ROWS-1 else vE[r][c], 0 if c == 0 else -hE[r][c-1]]
    s = min(uw, uh)
    return "M0 0 " + edge(0, 0, uw, 0, e[0], s) + edge(uw, 0, uw, uh, e[1], s) + edge(uw, uh, 0, uh, e[2], s) + edge(0, uh, 0, 0, e[3], s) + " Z"
ox, oy = W * 0.1, H * 0.1
svg = [f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="{W*2}" height="{H*2}" viewBox="0 0 {W} {H}">',
       '<defs><linearGradient id="bg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#1E3A8A"/><stop offset="1" stop-color="#7C3AED"/></linearGradient></defs>',
       f'<rect width="{W}" height="{H}" fill="url(#bg)"/>',
       f'<rect x="{ox}" y="{oy}" width="{W*0.8}" height="{H*0.8}" fill="#0F172A" rx="6"/>',
       f'<image x="{ox}" y="{oy}" width="{W*0.8}" height="{H*0.8}" opacity="0.18" xlink:href="data:image/png;base64,{b64}"/>']
loose = {(0, 5): (180, 140, 18), (3, 4): (90, 70, -14), (2, 1): (-60, 150, 10), (1, 3): (40, -10, 0)}
order = sorted([(r, c) for r in range(ROWS) for c in range(COLS)], key=lambda rc: rc in loose)
for i, (r, c) in enumerate(order):
    d = piece(r, c)
    dx, dy, rot = loose.get((r, c), (0, 0, 0))
    x, y = ox + c * uw + dx, oy + r * uh + dy
    if (r, c) in loose and dx == 40: continue  # bir parca eksik
    svg.append(f'<clipPath id="p{i}"><path d="{d}"/></clipPath>')
    tr = f'translate({x:.1f},{y:.1f}) rotate({rot} {uw/2:.0f} {uh/2:.0f})'
    if (r, c) in loose: svg.append(f'<g transform="{tr} translate(5,7)"><path d="{d}" fill="#000" fill-opacity="0.35"/></g>')
    svg.append(f'<g transform="{tr}"><g clip-path="url(#p{i})"><image x="{-c*uw:.1f}" y="{-r*uh:.1f}" width="{W*0.8}" height="{H*0.8}" xlink:href="data:image/png;base64,{b64}"/></g>'
               f'<path d="{d}" fill="none" stroke="#FFFFFF" stroke-opacity="0.8" stroke-width="2"/></g>')
svg.append('</svg>')
render("\n".join(svg), "/tmp/jigsaw_cover.png")
im = Image.open("/tmp/jigsaw_cover.png").convert("RGB").resize((W, H), Image.LANCZOS)
im.save(os.path.join(here, "../../app/src/main/res/drawable-nodpi/game_jigsaw.webp"), "WEBP", quality=90)
im.save("/tmp/game_jigsaw.png")
