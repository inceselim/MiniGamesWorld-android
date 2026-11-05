"""Yesim bahce seti: cicekler ve bitkiler (18)."""
from mjlib import *
import math
G, G2, BR = '#3A9D50', '#2B7A3D', '#7A4A24'

def stem(x0, y0, x1, y1, w=2.2, col=G2):
    return (P().M(x0 - w, y0).Q((x0 + x1) / 2 + 4, (y0 + y1) / 2, x1 - w, y1).L(x1 + w, y1).Q((x0 + x1) / 2 + 4 + 2 * w, (y0 + y1) / 2, x0 + w, y0).Z().d(), col, False)

def tulip():
    head = P().M(32, 40).L(40, 52).L(50, 36).L(60, 52).L(68, 40).C(72, 70, 64, 84, 50, 84).C(36, 84, 28, 70, 32, 40).Z()
    return [stem(50, 120, 50, 80), (leaf(50, 116, 30, 84, 7).d(), G, True), (leaf(51, 110, 72, 88, 6).d(), G, True),
            (head.d(), '#E53950', True), (P().M(50, 40).L(58, 54).Q(56, 76, 50, 82).Q(44, 76, 42, 54).Z().d(), '#C2213A', False)]

def rose():
    out = [stem(50, 122, 50, 80), (leaf(50, 104, 28, 94, 7).d(), G, True), (leaf(50, 96, 72, 86, 6).d(), G, True),
           (circ(50, 58, 26).d(), '#D81B4A', True)]
    for r, c in ((20, '#E8406A'), (14, '#D81B4A'), (8, '#E8406A')):
        out.append((P().M(50 - r, 58).C(50 - r, 58 - r * 1.2, 50 + r * 1.1, 58 - r * 1.1, 50 + r * 0.9, 58).C(50 + r * 0.7, 58 + r, 50 - r * 0.3, 58 + r * 0.6, 50 - r * 0.2, 58).Z().d(), c, True))
    return out

def daisy():
    return [stem(50, 122, 50, 70), (leaf(50, 108, 72, 96, 6).d(), G, True),
            (petals(50, 56, 14, 30, 4.4).d(), '#FFFFFF', True), (circ(50, 56, 10).d(), '#F5B700', True)]

def sunflower():
    return [stem(50, 122, 50, 74, 2.6), (leaf(50, 104, 26, 90, 8).d(), G, True), (leaf(50, 96, 76, 84, 7).d(), G, True),
            (petals(50, 52, 16, 30, 4.6, 11).d(), '#FFC107', True), (circ(50, 52, 14).d(), '#6D3F1A', True), (circ(50, 52, 8).d(), '#4E2C10', False)]

def lotus():
    out = [(E(50, 102, 40, 9).d(), '#3FA35A', True)]
    for x1, y1, c in ((24, 62, '#F7A8C4'), (76, 62, '#F7A8C4'), (34, 46, '#F48FB1'), (66, 46, '#F48FB1'), (50, 36, '#EC6F9C')):
        out.append((leaf(50, 98, x1, y1, 9).d(), c, True))
    return out

def sakura():
    out = [(P().M(10, 110).Q(40, 80, 90, 34).L(92, 38).Q(44, 84, 14, 114).Z().d(), BR, False)]
    for cx, cy, r in ((40, 76, 18), (72, 46, 15)):
        out += [(petals(cx, cy, 5, r, r * 0.45, -90).d(), '#F8BBD0', True), (circ(cx, cy, r * 0.25).d(), '#E91E63', False)]
    out.append((leaf(60, 70, 80, 82, 5).d(), G, True))
    return out

def clover():
    hearts = []
    for a in (-90, 0, 90, 180):
        r = math.radians(a); cx, cy = 50 + 15 * math.cos(r), 60 + 15 * math.sin(r)
        hearts.append(rot_path(P().M(cx, cy + 10).C(cx - 22, cy - 4, cx - 8, cy - 20, cx, cy - 8).C(cx + 8, cy - 20, cx + 22, cy - 4, cx, cy + 10).Z(), cx, cy, a + 90))
    return [stem(52, 118, 50, 66, 2.4), (join(*hearts).d(), '#2FA34A', True), (circ(50, 60, 4).d(), '#1F7A33', False)]

def maple():
    pts = [(50, 22), (56, 40), (70, 34), (66, 52), (84, 52), (74, 64), (86, 76), (64, 76), (66, 92), (52, 82), (50, 104), (48, 82), (34, 92), (36, 76), (14, 76), (26, 64), (16, 52), (34, 52), (30, 34), (44, 40)]
    return [(poly(*pts).d(), '#E5581F', True), (P().M(49, 50).L(51, 50).L(51, 120).L(49, 120).Z().d(), '#9C3A12', False)]

def mushroom():
    cap = P().M(16, 70).C(16, 36, 84, 36, 84, 70).Q(50, 78, 16, 70).Z()
    out = [(P().M(38, 70).Q(34, 108, 40, 114).L(60, 114).Q(66, 108, 62, 70).Z().d(), '#FFF3DC', True), (cap.d(), '#E53935', True)]
    for x, y, r in ((32, 56, 6), (52, 48, 7), (70, 60, 5), (44, 64, 4)): out.append((circ(x, y, r).d(), '#FFFFFF', False))
    return out

def cactus():
    out = [(rect(26, 100, 74, 122).d(), '#C9693A', True), (rect(22, 96, 78, 104).d(), '#B05A2E', True),
           (rrect(40, 36, 60, 100, 10).d(), '#4CAF50', True), (rrect(20, 52, 34, 80, 7).d(), '#4CAF50', True),
           (rrect(66, 44, 80, 72, 7).d(), '#4CAF50', True), (rect(32, 70, 42, 76).d(), '#4CAF50', False), (rect(58, 60, 68, 66).d(), '#4CAF50', False),
           (circ(50, 34, 6).d(), '#FF4D8D', True)]
    return out

def tree():
    crown = [circ(50, 52, 24), circ(32, 66, 16), circ(68, 66, 16), circ(50, 74, 16)]
    return ([(rect(45, 82, 55, 118).d(), BR, True)] + [(c.d(), '#43A047', True) for c in crown] + [(join(*crown).d(), '#43A047', False)] +
            [(circ(40, 48, 5).d(), '#E53935', False), (circ(62, 62, 5).d(), '#E53935', False), (circ(46, 72, 5).d(), '#E53935', False)])

def lavender():
    out = []
    for dx, top in ((-14, 36), (0, 26), (14, 36)):
        out.append(stem(50 + dx * 0.3, 120, 50 + dx, top + 30, 1.6))
        for k in range(6):
            y = top + k * 7; out.append((E(50 + dx + (2 if k % 2 else -2), y, 4.5, 5.5).d(), '#8E6CCF', True))
    return out

def acorn():
    return [(P().M(30, 64).C(30, 100, 46, 116, 50, 118).C(54, 116, 70, 100, 70, 64).Z().d(), '#C68642', True),
            (P().M(24, 66).C(22, 40, 78, 40, 76, 66).Q(50, 72, 24, 66).Z().d(), '#7A4A24', True),
            (rect(48, 34, 52, 48).d(), '#5A3418', False), (leaf(52, 40, 76, 26, 6).d(), G, True)]

def pinecone():
    out = [(rect(48, 18, 52, 34).d(), BR, False), (E(50, 72, 24, 40).d(), '#6B3F1D', True)]
    for y, n in ((42, 2), (56, 3), (70, 3), (84, 3), (98, 2)):
        for i in range(n):
            x = 50 + (i - (n - 1) / 2) * 15
            out.append((P().M(x - 8, y - 4).Q(x - 8, y + 9, x, y + 10).Q(x + 8, y + 9, x + 8, y - 4).Q(x, y + 2, x - 8, y - 4).Z().d(), '#B07A45', True))
    return out

def oakleaf():
    l = P().M(50, 22).C(64, 30, 58, 40, 70, 44).C(80, 48, 66, 58, 76, 64).C(86, 70, 68, 78, 72, 88).C(74, 98, 58, 98, 52, 108).L(48, 108).C(42, 98, 26, 98, 28, 88).C(32, 78, 14, 70, 24, 64).C(34, 58, 20, 48, 30, 44).C(42, 40, 36, 30, 50, 22).Z()
    return [(l.d(), '#7CB342', True), (P().M(49, 30).L(51, 30).L(51, 122).L(49, 122).Z().d(), '#4E7A23', False)]

def poppy():
    return [stem(50, 122, 50, 76), (leaf(50, 104, 72, 94, 6).d(), G, True),
            (petals(50, 54, 4, 26, 12, 45).d(), '#E53935', True), (circ(50, 54, 9).d(), '#212121', True)]

def bluebell():
    out = [(P().M(30, 118).Q(36, 60, 62, 26).L(65, 28).Q(40, 62, 34, 118).Z().d(), G2, False), (leaf(32, 114, 14, 80, 6).d(), G, True)]
    for x, y in ((62, 40), (52, 64), (44, 88)):
        out.append((P().M(x - 10, y).Q(x - 12, y + 16, x - 16, y + 20).L(x + 16, y + 20).Q(x + 12, y + 16, x + 10, y).Q(x, y - 8, x - 10, y).Z().d(), '#5C6BC0', True))
    return out

def sprout():
    return [(P().M(28, 92).L(72, 92).L(66, 122).L(34, 122).Z().d(), '#D07A44', True), (rect(24, 86, 76, 96).d(), '#B9643A', True),
            stem(50, 88, 50, 52, 2.4), (leaf(50, 56, 22, 38, 10).d(), '#66BB6A', True), (leaf(50, 52, 78, 30, 10).d(), '#43A047', True)]

def faces():
    return [tulip(), rose(), daisy(), sunflower(), lotus(), sakura(), clover(), maple(), mushroom(), cactus(), tree(), lavender(),
            acorn(), pinecone(), oakleaf(), poppy(), bluebell(), sprout()]
