"""Dama kapak gorseli."""
from board3d import *
img = background((40, 22, 12), (18, 10, 6), glow=(110, 70, 30))
b = Board3D()
draw_board(img, b, (233, 200, 145), (185, 131, 78), (91, 58, 30))
W_TOP, W_SIDE, B_TOP, B_SIDE = (255, 250, 236), (196, 186, 160), (62, 62, 70), (20, 20, 24)
pos = {}
for v in (1, 2):
    for u in range(8): pos[(u, v)] = 'b'
for v in (5, 6):
    for u in range(8): pos[(u, v)] = 'w'
for k in [(3, 2), (5, 1), (3, 5), (6, 6)]: pos.pop(k, None)
pos[(3, 3)] = 'b'; pos[(3, 4)] = 'w'; pos[(5, 3)] = 'W'
for v in range(8):
    for u in range(8):
        p = pos.get((u, v))
        if not p: continue
        if p.lower() == 'w': disc(img, b, u, v, W_TOP, W_SIDE, rim=(200, 190, 165), crown=p == 'W')
        else: disc(img, b, u, v, B_TOP, B_SIDE, rim=(10, 10, 12))
# Atlama oku
d = ImageDraw.Draw(img)
(x0, y0), (x1, y1) = b.p(3.5, 4.5), b.p(3.5, 2.5)
d.line([(x0, y0-20*SS), (x1, y1-20*SS)], fill=(74, 222, 128), width=8*SS)
d.polygon([(x1-18*SS, y1-6*SS), (x1+18*SS, y1-6*SS), (x1, y1-40*SS)], fill=(74, 222, 128))
save(img, "game_checkers")
