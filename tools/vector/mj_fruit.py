"""Meyve bahcesi seti (18 tur)."""
from mjlib import *
import math
LEAF, STEM = '#3FA34D', '#7A4A24'

def hl(cx, cy, rx, ry, a='#66FFFFFF'): return (E(cx, cy, rx, ry).d(), a, False)

def apple():
    body = P().M(50, 46).C(30, 30, 10, 48, 14, 76).C(18, 104, 36, 116, 50, 108).C(64, 116, 82, 104, 86, 76).C(90, 48, 70, 30, 50, 46).Z()
    return [(P().M(48, 48).Q(46, 34, 52, 24).L(55, 26).Q(50, 36, 52, 48).Z().d(), STEM, True),
            (leaf(54, 34, 76, 22, 6).d(), LEAF, True), (body.d(), '#E53935', True), hl(32, 64, 6, 11)]

def cherries():
    s = [(P().M(34, 82).Q(40, 50, 62, 26).L(64, 28).Q(44, 52, 37, 82).Z().d(), STEM, False),
         (P().M(70, 88).Q(68, 56, 62, 27).L(65, 27).Q(71, 56, 73, 88).Z().d(), STEM, False),
         (leaf(63, 27, 84, 20, 6).d(), LEAF, True)]
    for cx, cy in ((34, 92), (70, 96)):
        s += [(circ(cx, cy, 17).d(), '#D81B3C', True), hl(cx - 6, cy - 6, 4, 6)]
    return s

def banana():
    b = P().M(22, 34).Q(18, 96, 80, 106).Q(92, 104, 90, 96).Q(46, 92, 34, 34).Z()
    return [(b.d(), '#FFD23F', True), (P().M(30, 40).Q(34, 86, 82, 98).Q(46, 92, 34, 40).Z().d(), '#F4B400', False),
            (rect(20, 28, 34, 36).d(), STEM, True), (circ(90, 99, 3).d(), STEM, False)]

def grapes():
    out = [(P().M(50, 34).Q(52, 22, 60, 16).L(62, 18).Q(56, 24, 54, 34).Z().d(), STEM, False), (leaf(54, 28, 82, 24, 8).d(), LEAF, True)]
    pts = [(36, 46), (50, 44), (64, 46), (30, 62), (44, 60), (58, 60), (72, 62), (38, 76), (52, 76), (66, 76), (44, 92), (58, 92), (51, 107)]
    for x, y in pts: out += [(circ(x, y, 9.5).d(), '#7B3FA0', True), hl(x - 3, y - 3, 2.4, 3)]
    return out

def strawberry():
    body = P().M(50, 118).C(30, 104, 16, 70, 24, 50).C(32, 38, 68, 38, 76, 50).C(84, 70, 70, 104, 50, 118).Z()
    out = [(body.d(), '#E8283F', True)]
    for x, y in [(36, 58), (50, 54), (64, 58), (30, 74), (44, 70), (58, 70), (70, 74), (38, 88), (52, 86), (64, 88), (46, 102), (56, 102)]:
        out.append((E(x, y, 1.8, 2.8).d(), '#FFE08A', False))
    out += [(petals(50, 44, 6, 18, 5, -90).d(), LEAF, True), (rect(48, 22, 52, 40).d(), LEAF, False)]
    return out

def orange():
    out = [(circ(50, 72, 34).d(), '#FB8C00', True), hl(36, 58, 7, 10, '#55FFFFFF')]
    for x, y in [(60, 62), (66, 74), (56, 84), (44, 78), (40, 92), (64, 92)]: out.append((circ(x, y, 1.6).d(), '#E07000', False))
    out += [(leaf(52, 40, 74, 28, 7).d(), LEAF, True), (circ(50, 40, 3.5).d(), STEM, False)]
    return out

def lemon():
    b = P().M(14, 70).Q(18, 56, 26, 52).C(40, 36, 60, 36, 74, 52).Q(82, 56, 86, 70).Q(82, 84, 74, 88).C(60, 104, 40, 104, 26, 88).Q(18, 84, 14, 70).Z()
    return [(b.d(), '#FFE135', True), hl(36, 60, 9, 5), (leaf(66, 46, 84, 30, 6).d(), LEAF, True)]

def pear():
    b = P().M(50, 36).C(38, 36, 38, 58, 34, 70).C(22, 86, 26, 116, 50, 116).C(74, 116, 78, 86, 66, 70).C(62, 58, 62, 36, 50, 36).Z()
    return [(P().M(49, 38).Q(48, 28, 54, 20).L(56, 22).Q(52, 28, 52, 38).Z().d(), STEM, False),
            (leaf(53, 28, 74, 22, 6).d(), LEAF, True), (b.d(), '#B5D334', True), hl(38, 88, 6, 10)]

def watermelon():
    dy = 12
    r = P().M(10, 52 + dy).L(90, 52 + dy).C(90, 74 + dy, 72, 98 + dy, 50, 98 + dy).C(28, 98 + dy, 10, 74 + dy, 10, 52 + dy).Z()
    w = P().M(16, 52 + dy).L(84, 52 + dy).C(84, 70 + dy, 70, 90 + dy, 50, 90 + dy).C(30, 90 + dy, 16, 70 + dy, 16, 52 + dy).Z()
    f = P().M(20, 52 + dy).L(80, 52 + dy).C(80, 68 + dy, 68, 86 + dy, 50, 86 + dy).C(32, 86 + dy, 20, 68 + dy, 20, 52 + dy).Z()
    out = [(r.d(), '#2E8B3E', True), (w.d(), '#F3F7DA', False), (f.d(), '#F2434F', False)]
    for x, y in [(34, 60), (50, 62), (66, 60), (42, 72), (58, 72), (50, 80)]: out.append((E(x, y + dy, 2, 3.2).d(), '#2A1A12', False))
    return out

def _chord(cx, cy, a, b, x0, y0, dx, dy, w):
    """(x0,y0)+t(dx,dy) dogrusunun elips icindeki kismi, w kalinliginda serit."""
    import math
    px, py = (x0 - cx) / a, (y0 - cy) / b; qx, qy = dx / a, dy / b
    A = qx * qx + qy * qy; B = 2 * (px * qx + py * qy); C = px * px + py * py - 1
    D = B * B - 4 * A * C
    if D <= 0: return None
    t1, t2 = (-B - math.sqrt(D)) / (2 * A), (-B + math.sqrt(D)) / (2 * A)
    L = math.hypot(dx, dy); nx, ny = -dy / L * w / 2, dx / L * w / 2
    ax, ay, bx, by = x0 + dx * t1, y0 + dy * t1, x0 + dx * t2, y0 + dy * t2
    return poly((ax + nx, ay + ny), (bx + nx, by + ny), (bx - nx, by - ny), (ax - nx, ay - ny))

def pineapple():
    out = [(petals(50, 42, 5, 26, 5, -90 - 72).d(), LEAF, True), (leaf(50, 44, 50, 14, 6).d(), '#2F8F3A', True)]
    out.append((E(50, 84, 26, 32).d(), '#F2B632', True))
    lines = []
    for k in range(-4, 5):
        for sgn in (1, -1):
            c = _chord(50, 84, 24.5, 30.5, 50 + k * 13, 84, sgn * 1, 1.4, 1.6)
            if c: lines.append(c)
    out.append((join(*lines).d(), '#C98A1E', False))
    return out

def peach():
    b = P().M(50, 46).C(24, 40, 14, 64, 18, 84).C(22, 106, 40, 116, 52, 112).C(70, 116, 86, 100, 84, 78).C(82, 56, 70, 44, 50, 46).Z()
    return [(b.d(), '#FFAB76', True), (P().M(50, 48).Q(40, 78, 52, 110).Q(44, 78, 52, 48).Z().d(), '#F28B5B', False),
            hl(32, 70, 6, 10, '#66FFE0D0'), (leaf(50, 46, 70, 30, 7).d(), LEAF, True)]

def kiwi():
    out = [(circ(50, 70, 34).d(), '#8D6E3F', True), (circ(50, 70, 30).d(), '#8BC34A', False), (circ(50, 70, 12).d(), '#F1F8D9', False)]
    for i in range(12):
        a = i * math.pi / 6
        out.append((E(50 + 18 * math.cos(a), 70 + 18 * math.sin(a), 1.8, 1.8).d(), '#1E1E1E', False))
    return out

def avocado():
    o = P().M(50, 26).C(36, 26, 32, 48, 28, 66).C(20, 92, 30, 116, 50, 116).C(70, 116, 80, 92, 72, 66).C(68, 48, 64, 26, 50, 26).Z()
    i = P().M(50, 32).C(40, 32, 38, 52, 34, 68).C(28, 90, 36, 110, 50, 110).C(64, 110, 72, 90, 66, 68).C(62, 52, 60, 32, 50, 32).Z()
    return [(o.d(), '#2F5D1E', True), (i.d(), '#D8E58C', False), (circ(50, 86, 15).d(), '#8B5A2B', True), hl(45, 81, 4, 5, '#55FFFFFF')]

def blueberries():
    out = [(leaf(50, 40, 76, 26, 7).d(), LEAF, True)]
    for x, y in [(36, 60), (64, 64), (48, 92)]:
        out += [(circ(x, y, 18).d(), '#3F51B5', True), hl(x - 6, y - 6, 4, 5), (star(x, y, 5, 2.2, 5).d(), '#283593', False)]
    return out

def plum():
    b = E(50, 76, 30, 36)
    return [(P().M(49, 42).Q(47, 30, 54, 22).L(56, 24).Q(51, 32, 52, 42).Z().d(), STEM, False), (leaf(54, 30, 76, 22, 6).d(), LEAF, True),
            (b.d(), '#8E2D6B', True), (P().M(52, 42).Q(62, 76, 52, 112).Q(58, 76, 52, 42).Z().d(), '#6E1F52', False), hl(38, 64, 6, 11, '#44FFFFFF')]

def coconut():
    return [(circ(50, 74, 34).d(), '#6D4C2F', True), (circ(50, 74, 29).d(), '#FFFDF5', False), (circ(50, 74, 21).d(), '#EDE3CC', False),
            hl(42, 66, 6, 4, '#88FFFFFF'), (leaf(70, 40, 92, 22, 6).d(), LEAF, True), (leaf(64, 42, 70, 16, 5).d(), '#2F8F3A', True)]

def pomegranate():
    return [(poly((40, 40), (44, 28), (48, 38), (50, 26), (52, 38), (56, 28), (60, 40)).d(), '#B0172E', True),
            (circ(50, 76, 34).d(), '#C2185B', True), hl(36, 62, 6, 10), (E(62, 88, 10, 8).d(), '#55FFFFFF', False)]

def raspberry():
    out = [(petals(50, 40, 5, 14, 4, -90).d(), LEAF, True)]
    for row, (y, xs) in enumerate([(52, (38, 50, 62)), (66, (32, 44, 56, 68)), (80, (36, 48, 60)), (94, (42, 54)), (106, (48,))]):
        for x in xs: out += [(circ(x, y, 8).d(), '#E3245A', True), hl(x - 2.5, y - 2.5, 2, 2.4)]
    return out

def faces():
    return [apple(), cherries(), banana(), grapes(), strawberry(), orange(), lemon(), pear(), watermelon(),
            pineapple(), peach(), kiwi(), avocado(), blueberries(), plum(), coconut(), pomegranate(), raspberry()]
