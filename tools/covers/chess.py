"""Satranc kapak gorseli."""
from PIL import Image, ImageDraw
from board3d import *
from chess_pieces import P

def piece_img(k, white, s):
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    fill, line, detail = ((250,246,236),(30,30,30),(30,30,30)) if white else ((48,48,54),(10,10,12),(220,220,220))
    w = max(2, int(s*0.035))
    for pass_ in (0, 1):
        col = line if pass_ == 0 else fill; ext = w if pass_ == 0 else 0
        for sh in P[k]:
            t = sh[0]
            if t == 'poly':
                pts = [(x*s, y*s) for x, y in sh[1]]
                if pass_ == 0: d.line(pts+[pts[0]], fill=line, width=w*2, joint="curve")
                d.polygon(pts, fill=col)
            elif t == 'circle' and sh[3] >= 0.03:
                _, cx, cy, r = sh; d.ellipse([(cx-r)*s-ext, (cy-r)*s-ext, (cx+r)*s+ext, (cy+r)*s+ext], fill=col)
            elif t == 'ellipse':
                _, cx, cy, rx, ry = sh; d.ellipse([(cx-rx)*s-ext, (cy-ry)*s-ext, (cx+rx)*s+ext, (cy+ry)*s+ext], fill=col)
            elif t == 'rect':
                _, x0, y0, x1, y1 = sh; d.rectangle([x0*s-ext, y0*s-ext, x1*s+ext, y1*s+ext], fill=col)
    for sh in P[k]:
        if sh[0] == 'detail_line':
            _, x0, y0, x1, y1 = sh; d.line([(x0*s, y0*s), (x1*s, y1*s)], fill=detail, width=w)
        if sh[0] == 'circle' and sh[3] < 0.03:
            _, cx, cy, r = sh; d.ellipse([(cx-r)*s, (cy-r)*s, (cx+r)*s, (cy+r)*s], fill=detail)
    return img

img = background((20, 26, 44), (8, 10, 20), glow=(60, 80, 140))
b = Board3D(y_far=150*SS, y_near=560*SS, half_near=470*SS, depth=0.8)
draw_board(img, b, (240, 217, 181), (181, 136, 99), (70, 46, 28))
setup = {
    (4, 0): 'k', (3, 0): 'q', (0, 0): 'r', (7, 0): 'r', (2, 1): 'b', (6, 2): 'n',
    (0, 1): 'p', (1, 1): 'p', (5, 1): 'p', (6, 1): 'p', (7, 1): 'p', (3, 3): 'p', (4, 3): 'p',
    (4, 4): 'P', (2, 5): 'N', (5, 5): 'N', (0, 6): 'P', (1, 6): 'P', (5, 6): 'P', (6, 6): 'P', (7, 6): 'P',
    (4, 7): 'K', (3, 6): 'Q', (2, 7): 'B', (0, 7): 'R', (7, 7): 'R', (3, 4): 'P',
}
for v in range(8):
    for u in range(8):
        k = setup.get((u, v))
        if not k: continue
        s = b.scale(v + 0.5)
        size = int(s * 1.55)
        x, y = b.p(u + 0.5, v + 0.62)
        sp = piece_img(k.upper(), k.isupper(), size)
        shadow = Image.new("RGBA", img.size, (0, 0, 0, 0)); sd = ImageDraw.Draw(shadow)
        img.paste(sp, (int(x - size/2), int(y - size*0.9)), sp)
save(img, "game_chess")
