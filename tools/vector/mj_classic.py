"""Klasik mahjong yuzleri (36 tur)."""
from mjlib import *

NAVY, RED, GREEN, BLUE, GOLD = '#1B2A4E', '#C62F2B', '#1D7A48', '#1F57A8', '#E0A21B'

def dot(cx, cy, r, col):
    return [(circ(cx, cy, r).d(), col, True), (circ(cx, cy, r * 0.74).d(), '#FFFFFF', False),
            (circ(cx, cy, r * 0.58).d(), col, False), (circ(cx, cy, r * 0.24).d(), '#FFFFFF', False)]

def corner(n):
    return [(glyph(str(n), 13, 16, 11, cjk=False), '#8C1B2A4E', False)]

DOTS = {
    2: [((50, 37), GREEN), ((50, 93), BLUE)],
    3: [((27, 30), BLUE), ((50, 65), RED), ((73, 100), GREEN)],
    4: [((30, 38), BLUE), ((70, 38), GREEN), ((30, 92), GREEN), ((70, 92), BLUE)],
    5: [((28, 32), BLUE), ((72, 32), GREEN), ((50, 65), RED), ((28, 98), GREEN), ((72, 98), BLUE)],
    6: [((32, 28), GREEN), ((68, 28), GREEN), ((32, 65), RED), ((68, 65), RED), ((32, 102), RED), ((68, 102), RED)],
    7: [((24, 22), GREEN), ((50, 36), GREEN), ((76, 50), GREEN), ((32, 78), RED), ((68, 78), RED), ((32, 106), RED), ((68, 106), RED)],
    8: [((32, y), BLUE) for y in (22, 51, 80, 109)] + [((68, y), BLUE) for y in (22, 51, 80, 109)],
    9: [((x, y), c) for y, c in ((28, BLUE), (65, RED), (102, GREEN)) for x in (24, 50, 76)],
}
DOT_R = {2: 17, 3: 14, 4: 15, 5: 13, 6: 13, 7: 11.5, 8: 11, 9: 11.5}

def dots_face(n):
    if n == 1:
        parts = [(circ(50, 65, 34).d(), GREEN, True), (petals(50, 65, 12, 33, 4.2, 15).d(), '#FFFFFF', False),
                 (circ(50, 65, 24).d(), RED, True), (ring(50, 65, 18, 14).d(), '#FFFFFF', False),
                 (circ(50, 65, 10).d(), BLUE, False), (circ(50, 65, 4).d(), '#FFFFFF', False)]
        return parts
    out = []
    for (x, y), c in DOTS[n]: out += dot(x, y, DOT_R[n], c)
    return out

def stick(cx, cy, h, col, w=10.5):
    top, bot = cy - h / 2, cy + h / 2
    parts = [(rrect(cx - w / 2, top, cx + w / 2, bot, w / 2).d(), col, True),
             (rect(cx - w / 2 + 0.6, cy - 1.6, cx + w / 2 - 0.6, cy + 1.6).d(), shade(col, 0.6), False),
             (rect(cx - 1.2, top + 3, cx + 1.2, bot - 3).d(), '#55FFFFFF', False)]
    return parts

BAMBOO = {
    2: [(50, 37, GREEN), (50, 93, GREEN)],
    3: [(50, 37, GREEN), (32, 93, GREEN), (68, 93, GREEN)],
    4: [(32, 37, GREEN), (68, 37, BLUE), (32, 93, BLUE), (68, 93, GREEN)],
    5: [(26, 37, GREEN), (74, 37, BLUE), (50, 65, RED), (26, 93, BLUE), (74, 93, GREEN)],
    6: [(x, y, GREEN) for y in (37, 93) for x in (26, 50, 74)],
    7: [(50, 22, RED)] + [(x, y, GREEN) for y in (60, 104) for x in (26, 50, 74)],
    8: [(x, y, GREEN if y < 60 else BLUE) for y in (37, 93) for x in (19, 39.5, 60.5, 81)],
    9: [(x, y, RED if x == 50 else (GREEN if x < 50 else BLUE)) for y in (25, 65, 105) for x in (24, 50, 76)],
}
STICK_H = {2: 44, 3: 44, 4: 44, 5: 40, 6: 44, 7: 34, 8: 44, 9: 32}

def bamboo_face(n):
    if n == 1:  # bambu filizi ve kus
        parts = [(rrect(46, 30, 54, 118, 4).d(), GREEN, True)]
        for y in (52, 76, 100): parts.append((rect(46.6, y - 1.6, 53.4, y + 1.6).d(), shade(GREEN, 0.6), False))
        parts += [(leaf(54, 50, 86, 30, 7).d(), GREEN, True), (leaf(46, 74, 14, 56, 7).d(), GREEN, True),
                  (leaf(54, 96, 84, 86, 6).d(), '#2E9E5E', True)]
        # kus
        parts += [(E(38, 28, 16, 12).d(), RED, True), (circ(26, 20, 8).d(), RED, True),
                  (poly((18, 20), (10, 23), (18, 24)).d(), GOLD, True), (circ(25, 18, 1.8).d(), '#1A1A1A', False),
                  (leaf(44, 26, 64, 12, 5).d(), BLUE, True), (leaf(40, 34, 62, 40, 4).d(), GOLD, True)]
        return parts
    out = []
    for x, y, c in BAMBOO[n]: out += stick(x, y, STICK_H[n], c)
    return out

NUMS = "一二三四五六七八九"
def char_face(n):
    return [(glyph(NUMS[n - 1], 50, 36, 34 if n not in (1, 2, 3) else (8 if n == 1 else 22 if n == 2 else 30), w=54), NAVY, False),
            (glyph('萬', 50, 90, 48, w=62), RED, False)] + corner(n)

def wind_face(ch):
    return [(glyph(ch, 50, 65, 66, w=74), NAVY, False)]

def dragon_red(): return [(glyph('中', 50, 65, 76, w=70), RED, False)]
def dragon_green(): return [(glyph('發', 50, 65, 74, w=76), GREEN, False)]
def dragon_white():
    return [(ring_rect(18, 20, 82, 110, 4).d(), BLUE, False), (ring_rect(26, 28, 74, 102, 2.5).d(), BLUE, False)]

def ring_rect(x0, y0, x1, y1, t):
    o = rect(x0, y0, x1, y1)
    i = P().M(x0 + t, y0 + t).L(x0 + t, y1 - t).L(x1 - t, y1 - t).L(x1 - t, y0 + t).Z()
    return join(o, i)

def flower_face():
    parts = [(P().M(50, 118).Q(46, 96, 50, 78).L(53, 78).Q(50, 96, 54, 118).Z().d(), '#6B4226', False),
             (leaf(52, 100, 74, 92, 5).d(), GREEN, True)]
    for cx, cy, r in ((50, 52, 20), (30, 82, 11), (72, 74, 12)):
        parts += [(petals(cx, cy, 5, r, r * 0.42, -90).d(), '#E85D8A', True), (circ(cx, cy, r * 0.32).d(), GOLD, True)]
    return parts

def season_face():
    parts = [(petals(50, 58, 16, 34, 3.6, 0).d(), '#F2B705', True), (petals(50, 58, 16, 26, 3.4, 11).d(), '#F59E0B', True),
             (circ(50, 58, 12).d(), '#B45309', True)]
    parts += [(P().M(49, 70).L(51, 70).Q(54, 96, 50, 120).L(48, 120).Q(52, 96, 49, 70).Z().d(), GREEN, False),
              (leaf(50, 102, 74, 96, 5).d(), GREEN, True), (leaf(50, 110, 26, 102, 5).d(), GREEN, True)]
    return parts

def faces():
    F = []
    F += [dots_face(n) for n in range(1, 10)]
    F += [bamboo_face(n) for n in range(1, 10)]
    F += [char_face(n) for n in range(1, 10)]
    F += [wind_face(c) for c in "東南西北"]
    F += [dragon_red(), dragon_green(), dragon_white(), flower_face(), season_face()]
    return F
