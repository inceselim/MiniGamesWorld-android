"""Okyanus seti (18)."""
from mjlib import *
import math

def eye(x, y, r=3.5): return [(circ(x, y, r).d(), '#FFFFFF', True), (circ(x + 0.6, y + 0.4, r * 0.55).d(), '#1B1B1B', False)]

def fish():
    body = P().M(20, 66).C(30, 40, 64, 40, 74, 66).C(64, 92, 30, 92, 20, 66).Z()
    tail = poly((72, 66), (92, 48), (88, 66), (92, 84))
    return [(tail.d(), '#FF8F00', True), (body.d(), '#FFA726', True), (leaf(44, 50, 58, 38, 4).d(), '#FF8F00', True),
            (P().M(40, 48).Q(46, 66, 40, 84).L(44, 84).Q(50, 66, 44, 48).Z().d(), '#FFF3E0', False), *eye(30, 62)]

def whale():
    body = P().M(14, 74).C(14, 50, 50, 44, 70, 60).Q(82, 70, 88, 50).Q(96, 66, 86, 76).Q(78, 96, 44, 96).C(24, 96, 14, 88, 14, 74).Z()
    return [(body.d(), '#4A7FD4', True), (P().M(16, 80).Q(40, 98, 70, 86).Q(48, 92, 18, 84).Z().d(), '#BBD3F7', False),
            *eye(30, 70, 3), (P().M(38, 44).Q(36, 30, 30, 26).M(40, 44).Q(44, 30, 50, 26).Z().d(), '#7FB0FF', False),
            (join(circ(30, 26, 3), circ(50, 26, 3), circ(40, 22, 3)).d(), '#7FB0FF', False)]

def octopus():
    out = []
    for i in range(6):
        x = 24 + i * 10.4
        out.append((P().M(x, 70).Q(x - 6 + i, 100, x + (4 if i % 2 else -4), 116).L(x + 6, 116).Q(x + 4 + i, 96, x + 8, 70).Z().d(), '#9C4DCC', True))
    out += [(E(50, 54, 28, 30).d(), '#AB5FDB', True), *eye(40, 56, 5), *eye(60, 56, 5), (P().M(44, 70).Q(50, 76, 56, 70).Z().d(), '#5E2585', True)]
    return out

def crab():
    out = []
    for s in (1, -1):
        for k in range(3):
            y = 74 + k * 9
            out.append((P().M(50 + s * 22, y).L(50 + s * 40, y + 6 + k * 3).L(50 + s * 41, y + 9 + k * 3).L(50 + s * 22, y + 4).Z().d(), '#C62828', False))
        cx = 50 + s * 34
        out.append((P().M(cx - 10, 44).Q(cx, 30, cx + 10, 44).L(cx + s * 2, 46).Q(cx, 40, cx - s * 2, 46).Z().d() if False else circ(cx, 44, 11).d(), '#E53935', True))
        out.append((poly((cx - 2, 36), (cx + 4, 46), (cx + 8, 34)).d(), '#FBE9E7', False))
        out.append((P().M(50 + s * 18, 70).L(cx - s * 2, 52).L(cx + s * 2, 54).L(50 + s * 22, 72).Z().d(), '#C62828', False))
    out += [(E(50, 78, 26, 18).d(), '#E53935', True), (rect(41, 56, 43, 66).d(), '#C62828', False), (rect(57, 56, 59, 66).d(), '#C62828', False),
            *eye(42, 56, 4), *eye(58, 56, 4), (P().M(44, 84).Q(50, 89, 56, 84).Z().d(), '#7F1010', True)]
    return out

def starfish():
    s = star(50, 66, 40, 17, 5)
    return [(s.d(), '#FF7043', True), (star(50, 66, 30, 13, 5).d(), '#FF8A65', False)] + \
           [(circ(50 + r * math.cos(math.radians(-90 + k * 72)), 66 + r * math.sin(math.radians(-90 + k * 72)), 2.2).d(), '#FFCCBC', False) for k in range(5) for r in (12, 22)]

def seahorse():
    b = P().M(54, 26).C(72, 24, 76, 44, 62, 48).L(68, 52).C(72, 70, 60, 84, 58, 96).C(56, 110, 70, 112, 66, 120).C(58, 122, 46, 112, 48, 98).C(50, 84, 44, 70, 46, 58).Q(36, 56, 34, 46).Q(40, 46, 44, 42).C(42, 32, 46, 26, 54, 26).Z()
    return [(b.d(), '#FFB300', True), (poly((40, 60), (28, 66), (40, 70)).d(), '#FF8F00', True), (poly((60, 28), (56, 18), (64, 24)).d(), '#FF8F00', True),
            *eye(56, 36, 3.5)] + [(rect(52, y, 62, y + 2).d(), '#E08900', False) for y in (62, 72, 82)]

def turtle():
    out = []
    for x, y in ((24, 60), (76, 60), (28, 96), (72, 96)): out.append((E(x, y, 10, 7).d(), '#8BC34A', True))
    out += [(circ(50, 38, 11).d(), '#8BC34A', True), *eye(46, 36, 2.6), *eye(54, 36, 2.6), (E(50, 78, 30, 30).d(), '#2E7D32', True)]
    for x, y, r in ((50, 78, 9), (36, 66, 7), (64, 66, 7), (36, 90, 7), (64, 90, 7)):
        out.append((star(x, y, r, r * 0.86, 6, 0).d(), '#66BB6A', False))
    return out

def jellyfish():
    out = []
    for i in range(5):
        x = 30 + i * 10
        out.append((P().M(x - 2, 66).C(x - 8, 82, x + 6, 94, x - 2, 116).L(x + 2, 116).C(x + 10, 94, x - 4, 82, x + 2, 66).Z().d(), '#E1A6F0', False))
    out += [(P().M(20, 68).C(20, 28, 80, 28, 80, 68).Q(70, 64, 65, 70).Q(58, 64, 50, 70).Q(42, 64, 35, 70).Q(30, 64, 20, 68).Z().d(), '#EA80FC', True),
            hl_(38, 46, 7, 4), *eye(42, 56, 3), *eye(58, 56, 3)]
    return out

def hl_(x, y, rx, ry): return (E(x, y, rx, ry).d(), '#66FFFFFF', False)

def scallop():
    out = [(P().M(50, 112).L(14, 64).C(16, 30, 84, 30, 86, 64).Z().d(), '#FFAB91', True)]
    for a in range(-60, 61, 20):
        r = math.radians(a - 90)
        out.append((P().M(50, 110).L(50 + 40 * math.cos(r) - 2, 70 + 40 * math.sin(r)).L(50 + 40 * math.cos(r) + 2, 70 + 40 * math.sin(r)).Z().d(), '#E07A5F', False))
    out += [(poly((40, 104), (36, 118), (64, 118), (60, 104)).d(), '#FF8A65', True)]
    return out

def anchor():
    ring_ = ring(50, 30, 9, 5)
    a = P().M(47, 38).L(53, 38).L(53, 106).Q(72, 104, 78, 82).L(70, 82).L(82, 70).L(88, 86).L(82, 84).Q(76, 112, 50, 116).Q(24, 112, 18, 84).L(12, 86).L(18, 70).L(30, 82).L(22, 82).Q(28, 104, 47, 106).Z()
    return [(ring_.d(), '#455A64', True), (a.d(), '#546E7A', True), (rect(32, 48, 68, 54).d(), '#546E7A', True)]

def dolphin():
    b = P().M(12, 76).L(26, 72).C(32, 52, 60, 42, 78, 52).C(88, 58, 90, 72, 84, 84).L(96, 92).L(80, 94).L(76, 106).L(72, 88).C(60, 84, 44, 82, 30, 82).L(14, 82).Z()
    return [(poly((52, 46), (62, 28), (66, 48)).d(), '#29B6F6', True), (b.d(), '#4FC3F7', True),
            (P().M(28, 80).C(44, 80, 58, 82, 72, 88).C(58, 76, 40, 76, 28, 76).Z().d(), '#E1F5FE', False),
            (poly((46, 80), (40, 96), (56, 82)).d(), '#29B6F6', True), *eye(34, 66, 2.8)]

def puffer():
    out = []
    for k in range(16):
        a = math.radians(k * 22.5); out.append((poly((50 + 30 * math.cos(a - 0.12), 68 + 30 * math.sin(a - 0.12)), (50 + 40 * math.cos(a), 68 + 40 * math.sin(a)), (50 + 30 * math.cos(a + 0.12), 68 + 30 * math.sin(a + 0.12))).d(), '#F9A825', True))
    out += [(poly((80, 68), (96, 54), (96, 82)).d(), '#F9A825', True), (circ(50, 68, 32).d(), '#FFEB3B', True), (P().M(20, 72).Q(50, 104, 80, 72).Q(50, 96, 20, 72).Z().d(), '#FFF9C4', False),
            *eye(40, 60, 5), *eye(60, 60, 5), (circ(50, 78, 4).d(), '#E65100', True)]
    return out

def sailboat():
    return [(P().M(12, 106).Q(31, 100, 50, 106).Q(69, 112, 88, 106).L(88, 116).Q(69, 122, 50, 116).Q(31, 110, 12, 116).Z().d(), '#4FC3F7', False),
            (P().M(16, 92).L(84, 92).L(74, 108).L(26, 108).Z().d(), '#8D5A34', True), (rect(48, 22, 52, 92).d(), '#5D3A1A', False),
            (poly((54, 26), (54, 86), (84, 86)).d(), '#FFFFFF', True), (poly((46, 34), (46, 86), (20, 86)).d(), '#EF5350', True)]

def coral():
    def br(x0, y0, x1, y1, w): return P().M(x0 - w, y0).Q((x0 + x1) / 2 - w, (y0 + y1) / 2, x1 - w * 0.6, y1).Q(x1, y1 - w, x1 + w * 0.6, y1).Q((x0 + x1) / 2 + w, (y0 + y1) / 2, x0 + w, y0).Z()
    segs = [br(*s) for s in ((50, 118, 50, 40, 6), (50, 90, 26, 54, 4.5), (50, 80, 76, 46, 4.5), (36, 70, 22, 40, 3.5), (66, 62, 80, 30, 3.5), (50, 60, 40, 30, 3.5))]
    return [(s.d(), '#FF6F91', True) for s in segs] + [(join(*segs).d(), '#FF6F91', False), (E(50, 120, 22, 4).d(), '#E5D3B3', False)]

def oyster():
    return [(P().M(14, 80).C(14, 60, 86, 60, 86, 80).Q(70, 84, 50, 84).Q(30, 84, 14, 80).Z().d(), '#B0BEC5', True),
            (P().M(14, 80).C(20, 112, 80, 112, 86, 80).Q(50, 90, 14, 80).Z().d(), '#90A4AE', True),
            (P().M(16, 78).C(14, 38, 86, 38, 84, 78).Q(50, 66, 16, 78).Z().d(), '#CFD8DC', True),
            (circ(50, 80, 11).d(), '#FFFFFF', True), hl_(46, 76, 3.5, 3)]

def lighthouse():
    return [(P().M(36, 116).L(42, 46).L(58, 46).L(64, 116).Z().d(), '#FFFFFF', True),
            (P().M(38.4, 88).L(39.8, 72).L(60.2, 72).L(61.6, 88).Z().d(), '#E53935', False),
            (P().M(41, 60).L(41.6, 52).L(58.4, 52).L(59, 60).Z().d(), '#E53935', False),
            (rect(38, 40, 62, 46).d(), '#37474F', True), (rect(42, 26, 58, 40).d(), '#FFEB3B', True), (poly((38, 26), (50, 14), (62, 26)).d(), '#E53935', True),
            (poly((58, 30), (90, 22), (90, 40)).d(), '#66FFF59D', False), (poly((42, 30), (10, 22), (10, 40)).d(), '#66FFF59D', False),
            (rect(46, 98, 54, 116).d(), '#5D4037', False)]

def shrimp():
    b = P().M(70, 40).C(90, 50, 86, 90, 60, 98).C(46, 102, 34, 96, 30, 86).L(20, 92).L(22, 80).L(14, 72).L(28, 76).C(34, 86, 48, 88, 58, 84).C(72, 78, 72, 56, 62, 48).Z()
    out = [(b.d(), '#FF8A65', True)]
    for a in (55, 66, 77): out.append((P().M(64 + (a - 55) * 0.6, a).Q(74, a + 4, 78, a + 2).L(78, a + 4).Q(72, a + 7, 63 + (a - 55) * 0.6, a + 3).Z().d(), '#F4511E', False))
    out += [*eye(70, 46, 3), (P().M(70, 40).Q(50, 20, 22, 24).L(22, 26).Q(48, 24, 68, 42).Z().d(), '#F4511E', False)]
    return out

def seaweed():
    out = []
    for x, h, c in ((34, 80, '#2E7D32'), (50, 96, '#43A047'), (66, 74, '#66BB6A')):
        p = P().M(x - 4, 120)
        for k in range(4): p.Q(x + (8 if k % 2 == 0 else -8), 120 - h * (k + 0.5) / 4, x - 2, 120 - h * (k + 1) / 4)
        p.L(x + 2, 120 - h)
        for k in range(3, -1, -1): p.Q(x + (12 if k % 2 == 0 else -4), 120 - h * (k + 0.5) / 4, x + 4, 120 - h * k / 4)
        out.append((p.Z().d(), c, True))
    out += [(circ(26, 50, 4).d(), '#B3E5FC', True), (circ(76, 40, 3).d(), '#B3E5FC', True), (circ(72, 28, 2).d(), '#B3E5FC', True)]
    return out

def faces():
    return [fish(), whale(), octopus(), crab(), starfish(), seahorse(), turtle(), jellyfish(), scallop(), anchor(), dolphin(), puffer(),
            sailboat(), coral(), oyster(), lighthouse(), shrimp(), seaweed()]
