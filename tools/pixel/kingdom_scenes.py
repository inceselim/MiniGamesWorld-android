"""Krallik sahnesi arka plani (192x84) ve fetih haritasi (128x360) + dugum koordinatlari."""
import os, sys, math, random
from PIL import Image
OUT = sys.argv[1] if len(sys.argv) > 1 else 'app/src/main/res/drawable-nodpi'
sys.path.insert(0, os.path.dirname(__file__))
from kingdom_sprites import P

def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]

def dither_grad(im, x0, y0, x1, y1, cols):
    """Dikey, bantli + bayer dither gecisli gokyuzu."""
    n = len(cols) - 1
    for y in range(y0, y1):
        t = (y - y0) / max(1, (y1 - y0 - 1)) * n
        i = min(int(t), n - 1); f = t - i
        for x in range(x0, x1):
            c = cols[i + 1] if f * 16 > BAYER[y % 4][x % 4] else cols[i]
            im.putpixel((x, y), c)

def scene_bg():
    W, H = 192, 84
    im = Image.new('RGB', (W, H))
    dither_grad(im, 0, 0, W, 50, [(41, 54, 111), (59, 93, 201), (65, 166, 246), (115, 239, 247)])
    # gunes
    for y in range(6, 18):
        for x in range(150, 164):
            if (x - 157) ** 2 + (y - 12) ** 2 <= 36: im.putpixel((x, y), P['y1'] if (x - 157) ** 2 + (y - 12) ** 2 <= 16 else P['y2'])
    # uzak daglar
    for x in range(W):
        h = int(34 + 6 * math.sin(x / 13) + 4 * math.sin(x / 5.3 + 1))
        for y in range(h, 52):
            im.putpixel((x, y), P['s3'] if y > h + 1 else P['s2'])
        if h < 32: im.putpixel((x, h), P['s1'])
    # yakin tepeler
    for x in range(W):
        h = int(44 + 3 * math.sin(x / 19 + 2) + 2 * math.sin(x / 7))
        for y in range(h, H):
            im.putpixel((x, y), P['g3'])
    # cimen zemin
    random.seed(4)
    for y in range(52, H):
        for x in range(W):
            c = P['g2']
            if (x * 7 + y * 13) % 23 == 0: c = P['g1']
            if (x * 5 + y * 3) % 31 == 0: c = P['g3']
            im.putpixel((x, y), c)
    for x in range(W): im.putpixel((x, 52), P['g1'])
    # yol (ortadan asagi)
    for y in range(64, H):
        hw = 4 + (y - 64) // 3
        for x in range(96 - hw, 96 + hw):
            im.putpixel((x, y), P['e1'] if (x + y) % 7 else P['e2'])
    # cicekler
    for _ in range(40):
        x, y = random.randrange(W), random.randrange(56, H)
        if abs(x - 96) > 14: im.putpixel((x, y), random.choice([P['wh'], P['y1'], P['r1'], P['p1']]))
    return im

# Fetih haritasi: alttan uste 15 bolge, zigzag patika.
MAP_W, MAP_H = 128, 360
NODES = []
def world_map():
    im = Image.new('RGB', (MAP_W, MAP_H))
    random.seed(9)
    # zemin bantlari: alt cimen -> orman -> dag -> volkan -> karanlik
    bands = [(0, 70, 'night'), (70, 130, 'lava'), (130, 200, 'mount'), (200, 280, 'forest'), (280, 360, 'grass')]
    base = dict(grass=(P['g2'], P['g1'], P['g3']), forest=(P['g3'], P['g2'], (25, 80, 70)),
                mount=(P['s2'], P['s1'], P['s3']), lava=((92, 40, 50), P['r2'], (60, 30, 40)),
                night=((40, 32, 70), P['p3'], (28, 24, 50)))
    for y in range(MAP_H):
        for x in range(MAP_W):
            wy = y + int(4 * math.sin(x / 9.0) + 2 * math.sin(x / 4.0))
            kind = next((k for a, b, k in bands if a <= wy < b), 'grass' if wy >= 280 else 'night')
            c0, c1, c2 = base[kind]
            v = (x * 7 + y * 11 + (x * y) % 13) % 53
            im.putpixel((x, y), c1 if v == 0 else c2 if v == 17 else c0)
    def put(x, y, c):
        if 0 <= x < MAP_W and 0 <= y < MAP_H: im.putpixel((x, y), c)
    # sus: agaclar, daglar, lav golleri, yildizlar
    for _ in range(45):
        x, y = random.randrange(4, MAP_W - 4), random.randrange(212, 335)
        for dy in range(5):
            for dx in range(-dy // 2 - 1, dy // 2 + 2): put(x + dx, y + dy, P['g3'] if dx > 0 else (25, 90, 60))
        put(x, y + 5, P['w3'])
    for _ in range(14):
        x, y = random.randrange(8, MAP_W - 8), random.randrange(140, 188)
        for dy in range(8):
            for dx in range(-dy, dy + 1): put(x + dx, y + dy, P['s1'] if dx < 0 else P['s3'])
        put(x, y, P['wh']); put(x - 1, y + 1, P['wh']); put(x + 1, y + 1, P['wh'])
    for _ in range(7):
        x, y = random.randrange(10, MAP_W - 10), random.randrange(80, 120)
        for dy in range(-2, 3):
            for dx in range(-5, 6):
                if dx * dx / 25 + dy * dy / 4 <= 1: put(x + dx, y + dy, P['r1'] if (dx + dy) % 3 else P['y2'])
    for _ in range(40):
        put(random.randrange(MAP_W), random.randrange(0, 65), random.choice([P['wh'], P['y1'], P['b1']]))
    # dugumler: alttan uste zigzag
    for i in range(15):
        y = MAP_H - 20 - i * 23
        x = 64 + int(36 * math.sin(i * 1.15))
        NODES.append((x, y))
    # patika
    for (x0, y0), (x1, y1) in zip(NODES, NODES[1:]):
        steps = max(abs(x1 - x0), abs(y1 - y0))
        for k in range(steps):
            t = k / steps; x = round(x0 + (x1 - x0) * t); y = round(y0 + (y1 - y0) * t)
            if k % 4 < 3:
                for dx in (0, 1): put(x + dx, y, P['e1']); put(x + dx, y + 1, P['e2'])
    return im

if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    scene_bg().save(os.path.join(OUT, 'ik_scene_bg.png'), optimize=True)
    world_map().save(os.path.join(OUT, 'ik_world_map.png'), optimize=True)
    print('NODES', NODES)
