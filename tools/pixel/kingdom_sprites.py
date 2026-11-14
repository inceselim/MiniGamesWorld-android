"""Krallik Savaslari pixel art seti.
Calistir: python3 tools/pixel/kingdom_sprites.py  -> app/src/main/res/drawable-nodpi/ik_*.png
Tum cizimler 32x32 (ikonlar 16x16), tek palet, otomatik dis cizgi."""
import os, sys
from PIL import Image

OUT = sys.argv[1] if len(sys.argv) > 1 else 'app/src/main/res/drawable-nodpi'
P = dict(
    o=(26, 28, 44),       # dis cizgi
    s1=(148, 176, 194), s2=(86, 108, 134), s3=(51, 60, 87),  # tas
    r1=(239, 125, 87), r2=(177, 62, 83), r3=(110, 38, 62),   # kirmizi cati
    w1=(214, 150, 96), w2=(150, 92, 58), w3=(96, 56, 40),    # ahsap
    g1=(167, 240, 112), g2=(56, 183, 100), g3=(37, 113, 121),  # cimen/yaprak
    y1=(255, 236, 160), y2=(255, 205, 117), y3=(222, 140, 60),  # altin
    b1=(115, 239, 247), b2=(65, 166, 246), b3=(59, 93, 201), b4=(41, 54, 111),  # mavi
    p1=(178, 102, 214), p2=(110, 60, 160), p3=(66, 36, 110),  # mor sancak
    k1=(255, 220, 185), k2=(232, 170, 130),  # ten
    wh=(244, 244, 244), gr=(170, 170, 190), dk=(40, 40, 52),
    e1=(190, 170, 130), e2=(140, 120, 90),  # toprak/yol
)


class S:
    def __init__(self, w=32, h=32):
        self.w, self.h = w, h
        self.px = [[None] * w for _ in range(h)]

    def p(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = P[c] if isinstance(c, str) else c

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.p(x, y, c)

    def hline(self, x0, x1, y, c):
        self.rect(x0, y, x1, y, c)

    def vline(self, x, y0, y1, c):
        self.rect(x, y0, x, y1, c)

    def tri(self, cx, top, half, base, c, shade=None):
        """Ucgen cati: tepe (cx,top), taban y=base, yarim genislik half."""
        h = base - top
        for y in range(top, base + 1):
            k = (y - top) / max(1, h)
            hw = round(half * k)
            for x in range(cx - hw, cx + hw + 1):
                self.p(x, y, shade if (shade and x < cx) else c)

    def get(self, x, y):
        return self.px[y][x] if 0 <= x < self.w and 0 <= y < self.h else None

    def outline(self, c='o'):
        add = []
        for y in range(self.h):
            for x in range(self.w):
                if self.px[y][x] is None and any(
                    self.get(x + dx, y + dy) not in (None,) and self.get(x + dx, y + dy) != P[c]
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                ):
                    add.append((x, y))
        for x, y in add:
            self.px[y][x] = P[c]
        return self

    def img(self):
        im = Image.new('RGBA', (self.w, self.h), (0, 0, 0, 0))
        for y in range(self.h):
            for x in range(self.w):
                if self.px[y][x]:
                    im.putpixel((x, y), self.px[y][x] + (255,))
        return im


def bricks(s, x0, y0, x1, y1, light='s1', mid='s2', dark='s3'):
    s.rect(x0, y0, x1, y1, mid)
    for y in range(y0, y1 + 1):
        row = (y - y0)
        if row % 3 == 2:
            s.hline(x0, x1, y, dark)
        else:
            off = 0 if (row // 3) % 2 == 0 else 2
            for x in range(x0 + off, x1 + 1, 4):
                s.p(x, y, dark)
    s.vline(x0, y0, y1, light)
    s.hline(x0, x1, y0, light)


def crenel(s, x0, x1, y, c='s2', top='s1'):
    for x in range(x0, x1 + 1):
        if (x - x0) % 3 != 2:
            s.rect(x, y - 2, x, y - 1, c)
            s.p(x, y - 2, top)


def banner(s, x, y, col='p2', light='p1'):
    s.vline(x, y, y + 7, 'w3')
    s.rect(x + 1, y, x + 4, y + 3, col)
    s.hline(x + 1, x + 4, y, light)
    s.p(x + 4, y + 3, None)
    s.p(x + 2, y + 1, 'y2')


def window(s, x, y, lit=True):
    s.rect(x, y, x + 1, y + 2, 'y2' if lit else 'dk')
    s.p(x, y, 'y1' if lit else 'dk')


def door(s, x, y, w=4, h=5):
    s.rect(x, y, x + w - 1, y + h - 1, 'w3')
    s.rect(x + 1, y + 1, x + w - 2, y + h - 1, 'w2')
    s.hline(x, x + w - 1, y, 's3')
    s.p(x + w - 2, y + h // 2 + 1, 'y2')


# ---------------- Binalar ----------------

def castle(tier):
    s = S()
    if tier == 1:
        bricks(s, 8, 14, 23, 29)
        crenel(s, 8, 23, 14)
        s.rect(12, 6, 19, 13, 's2'); bricks(s, 12, 6, 19, 13)
        s.tri(15, 1, 5, 6, 'r2', 'r1')
        door(s, 14, 24, 4, 6)
        window(s, 15, 8)
        banner(s, 16, 0)
    elif tier == 2:
        bricks(s, 4, 15, 27, 29)
        crenel(s, 4, 27, 15)
        for tx in (2, 24):
            bricks(s, tx, 8, tx + 5, 29)
            s.tri(tx + 2, 3, 4, 8, 'r2', 'r1')
            window(s, tx + 2, 12)
        bricks(s, 12, 6, 19, 15)
        crenel(s, 12, 19, 6)
        door(s, 13, 23, 6, 7)
        window(s, 15, 9)
        banner(s, 15, -1)
    else:
        bricks(s, 3, 14, 28, 29, 'y1', 's1', 's2')
        crenel(s, 3, 28, 14, 's1', 'y1')
        for tx in (1, 25):
            bricks(s, tx, 6, tx + 5, 29, 'y1', 's1', 's2')
            s.tri(tx + 2, 0, 4, 6, 'b3', 'b2')
            window(s, tx + 2, 10)
        bricks(s, 11, 3, 20, 14, 'y1', 's1', 's2')
        s.tri(15, -3, 6, 3, 'b3', 'b2')
        s.hline(11, 20, 4, 'y2')
        door(s, 13, 22, 6, 8)
        s.hline(13, 18, 21, 'y2')
        window(s, 13, 7); window(s, 17, 7)
        banner(s, 7, 6, 'p2', 'p1'); banner(s, 22, 6, 'p2', 'p1')
    return s.outline()


def gold_mine(tier):
    s = S()
    # kaya dag
    for y in range(8, 30):
        hw = int((y - 8) * 0.75) + 2
        for x in range(15 - hw, 16 + hw):
            s.p(x, y, 's2' if x > 15 - hw // 3 else 's1')
    for (x, y) in [(10, 18), (21, 15), (13, 12), (19, 24), (8, 25), (24, 26)]:
        s.rect(x, y, x + 1, y, 'y2'); s.p(x, y - 1, 'y1')
    # giris
    s.rect(11, 20, 19, 29, 'dk')
    s.rect(10, 19, 20, 19, 'w2'); s.vline(10, 19, 29, 'w2'); s.vline(20, 19, 29, 'w2')
    s.hline(10, 20, 19, 'w1')
    # vagon
    s.rect(21, 24, 28, 28, 'w2'); s.hline(21, 28, 24, 'w1')
    s.rect(22, 22, 27, 23, 'y2'); s.p(23, 21, 'y1'); s.p(25, 21, 'y2')
    s.p(22, 29, 'dk'); s.p(27, 29, 'dk')
    if tier >= 2:
        s.rect(2, 20, 7, 29, 'w2'); s.tri(4, 15, 4, 20, 'r2', 'r1'); window(s, 4, 23)
        s.rect(15, 4, 15, 8, 'w3'); s.rect(16, 4, 19, 6, 'y2')
    return s.outline()


def lumber(tier):
    s = S()
    def tree(cx, top, size):
        for y in range(top, top + size * 2):
            hw = (y - top) // 2 + 1
            hw = min(hw, size)
            for x in range(cx - hw, cx + hw + 1):
                s.p(x, y, 'g2' if x < cx + hw // 2 else 'g3')
            s.p(cx - hw + 1, y, 'g1') if (y - top) % 3 == 0 else None
        s.rect(cx, top + size * 2, cx + 1, top + size * 2 + 3, 'w3')
    tree(6, 2, 5); tree(25, 5, 5)
    # kutuk yigini
    for i, (x, y) in enumerate([(9, 25), (14, 25), (19, 25), (11, 21), (16, 21)]):
        s.rect(x, y, x + 4, y + 3, 'w2'); s.rect(x + 1, y + 1, x + 3, y + 2, 'w1'); s.p(x + 2, y + 1, 'w3')
    # balta kutuk
    s.rect(23, 25, 28, 29, 'w2'); s.hline(23, 28, 25, 'w1')
    s.vline(26, 19, 25, 'w3'); s.rect(26, 19, 28, 21, 'gr'); s.p(28, 19, 'wh')
    if tier >= 2:
        s.rect(1, 22, 8, 29, 'w2'); s.tri(4, 17, 5, 22, 'r2', 'r1'); door(s, 3, 25, 3, 5)
    return s.outline()


def quarry(tier):
    s = S()
    for (x0, y0, x1, y1) in [(3, 14, 13, 29), (11, 8, 22, 29), (19, 16, 28, 29)]:
        s.rect(x0, y0, x1, y1, 's2')
        s.hline(x0, x1, y0, 's1'); s.vline(x0, y0, y1, 's1')
        for y in range(y0 + 3, y1, 4):
            s.hline(x0 + 1, x1, y, 's3')
    s.rect(6, 24, 10, 27, 's1'); s.rect(14, 25, 18, 28, 's1'); s.hline(6, 10, 27, 's3'); s.hline(14, 18, 28, 's3')
    # kazma
    s.vline(24, 10, 15, 'w2'); s.hline(22, 26, 9, 'gr'); s.p(22, 10, 'gr'); s.p(26, 10, 'gr')
    if tier >= 2:
        s.vline(27, 2, 14, 'w3'); s.hline(23, 30, 2, 'w3'); s.vline(23, 3, 6, 'gr'); s.rect(22, 7, 24, 8, 's1')
    return s.outline()


def farm(tier):
    s = S()
    # tarla
    s.rect(1, 20, 30, 29, 'e2')
    for x in range(2, 30, 3):
        s.vline(x, 20, 29, 'y2'); s.p(x, 19, 'y1'); s.p(x + 1, 21, 'y3')
    # ambar
    s.rect(5, 9, 17, 19, 'r2'); s.vline(5, 9, 19, 'r1')
    s.tri(11, 3, 7, 9, 'w3', 'w2')
    s.rect(9, 13, 13, 19, 'w3'); s.p(11, 15, 'y2')
    s.hline(9, 13, 13, 'wh'); s.vline(9, 13, 19, 'wh'); s.vline(13, 13, 19, 'wh')
    # yel degirmeni / silo
    if tier >= 2:
        s.rect(21, 8, 26, 19, 'wh'); s.vline(21, 8, 19, 'gr'); s.tri(23, 3, 4, 8, 'r2', 'r1')
        s.hline(18, 29, 5, 'w2'); s.vline(23, 0, 10, 'w2')
    else:
        s.rect(21, 13, 25, 19, 'y2'); s.tri(23, 9, 3, 13, 'y3')
    return s.outline()


def warehouse(tier):
    s = S()
    s.rect(3, 13, 28, 29, 'w2')
    for x in range(3, 29, 3):
        s.vline(x, 13, 29, 'w3')
    s.hline(3, 28, 13, 'w1')
    s.tri(15, 4, 14, 13, 'r2', 'r1')
    door(s, 12, 20, 8, 10)
    s.p(13, 22, 'w1'); s.p(18, 22, 'w1')
    # sandiklar
    for (x, y) in [(22, 25), (25, 25), (23, 22)]:
        s.rect(x, y, x + 2, y + 2, 'y3'); s.p(x, y, 'y2')
    s.rect(4, 25, 7, 28, 'e1'); s.hline(4, 7, 25, 'y1')
    if tier >= 2:
        bricks(s, 3, 26, 28, 29)
        s.hline(3, 28, 12, 'y2')
    return s.outline()


def barracks(tier):
    s = S()
    base_l, base_m, base_d = ('s1', 's2', 's3') if tier >= 2 else ('w1', 'w2', 'w3')
    bricks(s, 3, 14, 28, 29, base_l, base_m, base_d)
    s.tri(15, 6, 14, 14, 'r2', 'r1')
    door(s, 13, 22, 6, 8)
    # kalkan + kiliclar
    s.rect(5, 18, 9, 22, 'b3'); s.hline(5, 9, 18, 'b2'); s.p(7, 20, 'y2'); s.rect(6, 23, 8, 23, 'b3'); s.p(7, 24, 'b3')
    s.p(22, 17, 'wh'); s.p(23, 18, 'wh'); s.p(24, 19, 'gr'); s.p(25, 20, 'gr'); s.p(25, 17, 'wh'); s.p(24, 18, 'wh'); s.p(22, 20, 'gr')
    banner(s, 26, 2, 'p2', 'p1')
    if tier >= 2:
        crenel(s, 3, 28, 14)
    return s.outline()


def archery(tier):
    s = S()
    # cit
    for x in range(1, 31, 4):
        s.vline(x, 21, 29, 'w2'); s.p(x, 20, 'w1')
    s.hline(1, 30, 23, 'w3'); s.hline(1, 30, 27, 'w3')
    # hedef
    s.rect(16, 6, 27, 17, 'wh')
    s.rect(18, 8, 25, 15, 'r2'); s.rect(19, 9, 24, 14, 'wh'); s.rect(20, 10, 23, 13, 'r2'); s.rect(21, 11, 22, 12, 'y2')
    s.vline(21, 18, 22, 'w3'); s.p(17, 18, 'w3'); s.p(26, 18, 'w3')
    # ok
    s.hline(10, 21, 11, 'w1'); s.p(9, 10, 'wh'); s.p(9, 12, 'wh'); s.p(10, 11, 'wh')
    # kulube
    s.rect(2, 11, 10, 19, 'w2'); s.tri(6, 6, 5, 11, 'g3', 'g2'); door(s, 5, 15, 3, 5)
    if tier >= 2:
        s.vline(14, 2, 19, 'w3'); s.rect(15, 2, 18, 5, 'g2'); s.p(16, 3, 'y2')
    return s.outline()


def stable(tier):
    s = S()
    s.rect(2, 13, 29, 29, 'w2')
    for x in range(2, 30, 3):
        s.vline(x, 13, 29, 'w3')
    s.tri(15, 5, 15, 13, 'r3', 'r2')
    # kapilar (yarim)
    for x0 in (5, 18):
        s.rect(x0, 19, x0 + 8, 29, 'w3'); s.rect(x0 + 1, 20, x0 + 7, 23, 'dk')
        s.hline(x0, x0 + 8, 24, 'w1')
    # at basi
    s.rect(8, 20, 11, 23, 'w1'); s.rect(9, 19, 10, 19, 'w1'); s.p(9, 21, 'dk'); s.rect(7, 20, 7, 22, 'w3')
    # saman
    s.rect(24, 9, 28, 12, 'y2'); s.hline(24, 28, 9, 'y1')
    if tier >= 2:
        s.rect(13, 1, 18, 6, 'w1'); s.tri(15, -1, 3, 1, 'r2'); s.p(15, 3, 'y2')
    return s.outline()


# ---------------- Askerler (24x24) ----------------

def soldier(kind):
    s = S(24, 24)
    body = dict(sword='b3', spear='g3', archer='y3', cav='p2')[kind]
    light = dict(sword='b2', spear='g2', archer='y2', cav='p1')[kind]
    if kind == 'cav':
        # at
        s.rect(4, 13, 16, 18, 'w2'); s.hline(4, 16, 13, 'w1')
        s.rect(15, 8, 19, 13, 'w2'); s.rect(18, 9, 20, 11, 'w2'); s.p(18, 9, 'dk')
        s.vline(15, 7, 12, 'w3')
        for x in (5, 8, 12, 15):
            s.vline(x, 19, 22, 'w3')
        s.vline(3, 13, 17, 'w3')
        # binici
        s.rect(8, 6, 12, 12, body); s.vline(8, 6, 12, light)
        s.rect(9, 2, 11, 5, 'k1'); s.hline(8, 12, 1, 'gr'); s.p(12, 2, 'gr')
        s.hline(12, 22, 6, 'wh'); s.p(23, 6, 'gr')
        return s.outline()
    s.rect(9, 3, 13, 7, 'k1'); s.p(10, 5, 'dk'); s.p(12, 5, 'dk'); s.hline(9, 13, 7, 'k2')
    helm = 'gr' if kind != 'archer' else 'g3'
    s.hline(8, 14, 2, helm); s.hline(9, 13, 1, helm)
    s.rect(8, 8, 14, 16, body); s.vline(8, 8, 16, light); s.hline(8, 14, 12, 'w3'); s.p(11, 12, 'y2')
    s.rect(9, 17, 10, 21, 'w3'); s.rect(12, 17, 13, 21, 'w3'); s.hline(8, 10, 22, 'dk'); s.hline(12, 14, 22, 'dk')
    s.rect(6, 9, 7, 13, body); s.rect(15, 9, 16, 13, body)
    if kind == 'sword':
        s.vline(18, 3, 13, 'wh'); s.vline(19, 4, 12, 'gr'); s.hline(16, 20, 13, 'y3'); s.vline(18, 14, 15, 'w3')
        s.rect(3, 9, 6, 14, 'r2'); s.vline(3, 9, 14, 'r1'); s.p(5, 11, 'y2')
    elif kind == 'spear':
        s.vline(18, 0, 22, 'w2'); s.rect(17, 0, 19, 1, 'wh'); s.p(18, -1, 'wh')
        s.rect(3, 9, 6, 15, 'b3'); s.vline(3, 9, 15, 'b2')
    else:
        for y in range(4, 17):
            dx = int(round(3 * (1 - ((y - 10.5) / 6.5) ** 2)))
            s.p(17 + dx, y, 'w2')
        s.vline(17, 4, 16, 'wh'); s.hline(10, 17, 10, 'w1'); s.p(10, 10, 'wh')
        s.rect(4, 6, 6, 13, 'w3'); s.hline(4, 6, 5, 'r2')
    return s.outline()


# ---------------- Kahramanlar (32x32 portre) ----------------

def hero(kind):
    s = S()
    bg = dict(arthur='b4', robin='g3', leonidas='r3')[kind]
    s.rect(1, 1, 30, 30, bg)
    s.rect(1, 22, 30, 30, dict(arthur='b3', robin='g2', leonidas='r2')[kind])
    # yuz
    s.rect(11, 9, 20, 19, 'k1'); s.vline(11, 9, 19, 'k2'); s.hline(11, 20, 19, 'k2')
    s.p(13, 13, 'dk'); s.p(18, 13, 'dk'); s.hline(14, 17, 17, 'k2')
    if kind == 'arthur':
        s.rect(10, 15, 21, 21, 'y3'); s.rect(12, 15, 19, 17, 'k1'); s.hline(14, 17, 17, 'w3')  # sakal
        s.rect(10, 6, 21, 8, 'y2'); s.p(10, 4, 'y2'); s.p(15, 3, 'y1'); s.p(21, 4, 'y2'); s.vline(10, 4, 6, 'y2'); s.vline(15, 3, 6, 'y2'); s.vline(21, 4, 6, 'y2')
        s.p(15, 7, 'r2'); s.p(12, 7, 'b2'); s.p(19, 7, 'b2')
        s.rect(5, 22, 26, 30, 'r2'); s.hline(5, 26, 22, 'wh')
    elif kind == 'robin':
        s.rect(9, 5, 22, 9, 'g2'); s.tri(15, 2, 7, 5, 'g2', 'g1'); s.rect(22, 3, 23, 6, 'r2')
        s.rect(5, 22, 26, 30, 'g3'); s.hline(5, 26, 22, 'w2'); s.vline(24, 12, 30, 'w2')
        s.hline(12, 19, 18, 'w3')
    else:
        s.rect(9, 3, 22, 10, 'y3'); s.vline(9, 3, 16, 'y3'); s.vline(22, 3, 16, 'y3')
        s.rect(13, 1, 18, 2, 'r2'); s.rect(14, -2, 17, 0, 'r2')
        s.vline(15, 10, 14, 'y3'); s.hline(11, 20, 10, 'y3')
        s.rect(5, 22, 26, 30, 'r2'); s.rect(3, 20, 9, 29, 'y3'); s.rect(4, 21, 8, 28, 'y2'); s.p(6, 24, 'r2')
    return s.outline()


# ---------------- Kaynak ikonlari (16x16) ----------------

def icon(kind):
    s = S(16, 16)
    if kind == 'gold':
        s.rect(3, 4, 12, 13, 'y2'); s.rect(4, 3, 11, 14, 'y2'); s.vline(4, 4, 13, 'y1'); s.hline(5, 11, 3, 'y1')
        s.rect(6, 6, 9, 11, 'y3'); s.rect(7, 7, 8, 10, 'y1')
        s.vline(12, 4, 13, 'y3'); s.hline(5, 11, 14, 'y3')
    elif kind == 'wood':
        s.rect(1, 6, 12, 11, 'w2'); s.hline(1, 12, 6, 'w1'); s.hline(1, 12, 11, 'w3')
        s.rect(11, 5, 14, 12, 'w1'); s.rect(12, 7, 13, 10, 'e1'); s.p(12, 8, 'w2')
        s.p(4, 8, 'w3'); s.p(8, 9, 'w3')
    elif kind == 'stone':
        s.rect(2, 7, 13, 13, 's2'); s.rect(4, 4, 11, 7, 's2'); s.hline(4, 11, 4, 's1'); s.vline(2, 7, 12, 's1')
        s.p(5, 5, 's1'); s.hline(3, 12, 13, 's3'); s.p(9, 9, 's3'); s.p(6, 10, 's3')
    else:
        for x in (4, 7, 10):
            s.vline(x, 6, 14, 'y3')
            s.rect(x - 1, 2, x + 1, 7, 'y2'); s.p(x, 1, 'y1'); s.p(x - 1, 3, 'y1')
        s.hline(3, 11, 11, 'w2')
    return s.outline()


def icon_map():
    s = S(16, 16)
    s.rect(2, 3, 13, 13, 'e1'); s.vline(2, 3, 13, 'y1'); s.hline(2, 13, 3, 'y1')
    s.vline(6, 3, 13, 'e2'); s.vline(10, 3, 13, 'e2')
    for (x, y) in [(3, 11), (4, 10), (5, 9), (6, 9), (7, 8), (8, 7)]: s.p(x, y, 'w2')
    s.p(10, 5, 'r2'); s.p(12, 5, 'r2'); s.p(11, 6, 'r2'); s.p(10, 7, 'r2'); s.p(12, 7, 'r2')
    s.rect(1, 2, 2, 14, 'w2'); s.rect(13, 2, 14, 14, 'w2')
    return s.outline()


def icon_book():
    s = S(16, 16)
    s.rect(2, 3, 13, 13, 'b3'); s.vline(2, 3, 13, 'b2'); s.hline(2, 13, 3, 'b2')
    s.rect(4, 4, 12, 12, 'wh'); s.vline(8, 4, 12, 'gr')
    for y in (6, 8, 10): s.hline(5, 7, y, 'gr'); s.hline(9, 11, y, 'gr')
    s.rect(11, 1, 12, 5, 'r2')
    return s.outline()


def save(s, name, scale=1):
    im = s.img() if isinstance(s, S) else s
    if scale > 1:
        im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, f'ik_{name}.png'), optimize=True)
    return im


SPRITES = {}
for t in (1, 2, 3):
    SPRITES[f'castle_{t}'] = castle(t)
for name, fn in dict(gold_mine=gold_mine, lumber=lumber, quarry=quarry, farm=farm, warehouse=warehouse,
                     barracks=barracks, archery=archery, stable=stable).items():
    for t in (1, 2):
        SPRITES[f'{name}_{t}'] = fn(t)
for k in ('sword', 'spear', 'archer', 'cav'):
    SPRITES[f'unit_{k}'] = soldier(k)
for k in ('arthur', 'robin', 'leonidas'):
    SPRITES[f'hero_{k}'] = hero(k)
for k in ('gold', 'wood', 'stone', 'food'):
    SPRITES[f'res_{k}'] = icon(k)
SPRITES['icon_map'] = icon_map()
SPRITES['icon_book'] = icon_book()

if __name__ == '__main__':
    for n, s in SPRITES.items():
        save(s, n)
    print(len(SPRITES), 'sprites ->', OUT)
