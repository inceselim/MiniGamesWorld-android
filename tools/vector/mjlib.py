"""Mahjong tas cizim yardimcilari. Tas yuzu viewBox: 100 x 130."""
import math
from vec import P, ellipse, rect, poly, join, shade, fmt
from fontTools.ttLib import TTCollection, TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.boundsPen import BoundsPen

FW, FH = 100, 130
_cjk = None
_lat = None

def _font(cjk):
    global _cjk, _lat
    if cjk:
        if _cjk is None: _cjk = TTCollection('/usr/share/fonts/opentype/noto/NotoSerifCJK-Bold.ttc').fonts[3]
        return _cjk
    if _lat is None: _lat = TTFont('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf')
    return _lat

def glyph(ch, cx, cy, h, cjk=True, w=None):
    """Karakterin dis hatlarini (cx, cy) merkezli, yuksekligi h (ya da genisligi w) olacak sekilde yol verisi olarak dondurur."""
    f = _font(cjk); gs = f.getGlyphSet(); name = f.getBestCmap()[ord(ch)]
    bp = BoundsPen(gs); gs[name].draw(bp); x0, y0, x1, y1 = bp.bounds
    s = h / (y1 - y0)
    if w is not None: s = min(s, w / (x1 - x0))
    tx = cx - (x0 + x1) / 2 * s; ty = cy + (y0 + y1) / 2 * s
    sp = SVGPathPen(gs, ntos=lambda v: fmt(v))
    gs[name].draw(TransformPen(sp, (s, 0, 0, -s, tx, ty)))
    return sp.getCommands()

def E(cx, cy, rx, ry=None): return ellipse(cx, cy, rx, ry if ry is not None else rx)
def circ(cx, cy, r): return ellipse(cx, cy, r, r)

def rrect(x0, y0, x1, y1, r):
    k = 0.5523 * r
    return (P().M(x0 + r, y0).L(x1 - r, y0).C(x1 - r + k, y0, x1, y0 + r - k, x1, y0 + r)
            .L(x1, y1 - r).C(x1, y1 - r + k, x1 - r + k, y1, x1 - r, y1)
            .L(x0 + r, y1).C(x0 + r - k, y1, x0, y1 - r + k, x0, y1 - r)
            .L(x0, y0 + r).C(x0, y0 + r - k, x0 + r - k, y0, x0 + r, y0).Z())

def star(cx, cy, ro, ri, n=5, rot=-90):
    pts = []
    for i in range(n * 2):
        r = ro if i % 2 == 0 else ri
        a = math.radians(rot + i * 180 / n)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return poly(*pts)

def petals(cx, cy, n, r_out, w, rot=0):
    ps = []
    for i in range(n):
        a = math.radians(rot + i * 360 / n)
        ps.append(leaf(cx, cy, cx + r_out * math.cos(a), cy + r_out * math.sin(a), w))
    return join(*ps)

def leaf(x0, y0, x1, y1, w):
    """(x0,y0)'dan (x1,y1)'e badem biciminde yaprak; w en genis yarim genislik."""
    dx, dy = x1 - x0, y1 - y0; L = math.hypot(dx, dy); nx, ny = -dy / L * w * 1.3, dx / L * w * 1.3
    mx, my = (x0 + x1) / 2, (y0 + y1) / 2
    return P().M(x0, y0).Q(mx + nx, my + ny, x1, y1).Q(mx - nx, my - ny, x0, y0).Z()

def ring(cx, cy, r1, r2):
    """Halka (cift yollu, evenOdd degil: ic daire ters yonde)."""
    o = circ(cx, cy, r1); k = 0.5523
    i = (P().M(cx - r2, cy).C(cx - r2, cy + r2 * k, cx - r2 * k, cy + r2, cx, cy + r2)
         .C(cx + r2 * k, cy + r2, cx + r2, cy + r2 * k, cx + r2, cy)
         .C(cx + r2, cy - r2 * k, cx + r2 * k, cy - r2, cx, cy - r2)
         .C(cx - r2 * k, cy - r2, cx - r2, cy - r2 * k, cx - r2, cy).Z())
    return join(o, i)

def rot_path(p, cx, cy, deg):
    a = math.radians(deg); ca, sa = math.cos(a), math.sin(a)
    q = P(p.W)
    for c, args in p.cmds:
        out = []
        for i in range(0, len(args), 2):
            x, y = args[i] - cx, args[i + 1] - cy
            out += [cx + x * ca - y * sa, cy + x * sa + y * ca]
        q.cmds.append((c, out))
    return q

def move(p, dx, dy, s=1.0, ox=0, oy=0):
    q = P(p.W)
    for c, args in p.cmds:
        q.cmds.append((c, [(ox + (v - ox) * s + dx) if i % 2 == 0 else (oy + (v - oy) * s + dy) for i, v in enumerate(args)]))
    return q
