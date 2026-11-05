"""Orman seti: Hayvan Dostlari'nin 12 hayvani + 6 orman hayvani (18)."""
import sys, os
from mjlib import *
import animals
from vec import P as _P

def fit(parts, box=(10, 22, 90, 114)):
    """200x200 viewBox cizimini (kontrol noktasi sinirlarina gore) tas yuzune olcekle."""
    import re
    xs, ys = [], []
    for d, c, st in parts:
        nums = [float(v) for v in re.findall(r'-?\d+(?:\.\d+)?', d)]
        xs += nums[0::2]; ys += nums[1::2]
    bx0, bx1, by0, by1 = min(xs), max(xs), min(ys), max(ys)
    x0, y0, x1, y1 = box; s = min((x1 - x0) / (bx1 - bx0), (y1 - y0) / (by1 - by0))
    dx = (x0 + x1) / 2 - (bx0 + bx1) / 2 * s; dy = (y0 + y1) / 2 - (by0 + by1) / 2 * s
    return [(scale_d(d, s, dx, dy), c, st) for d, c, st in parts]

def scale_d(d, s, dx, dy):
    import re
    toks = re.findall(r'[MLQCZ]|-?\d+(?:\.\d+)?', d)
    out = []; i = 0; idx = 0
    for tk in toks:
        if tk in 'MLQCZ': out.append(tk); idx = 0; continue
        v = float(tk); v = v * s + (dx if idx % 2 == 0 else dy); idx += 1
        out.append(fmt(v))
    # bitisik sayilari bosluklu birlestir
    res = ''
    for tk in out:
        if tk in 'MLQCZ': res += tk
        else: res += ('' if res and res[-1] in 'MLQCZ' else ' ') + tk
    return res.strip()

def eyes(x1, x2, y, r=4.5):
    out = []
    for x in (x1, x2): out += [(circ(x, y, r).d(), '#FFFFFF', True), (circ(x + 0.8, y + 0.6, r * 0.6).d(), '#1B1B1B', False), (circ(x + 2, y - 1.4, r * 0.22).d(), '#FFFFFF', False)]
    return out

def fox():
    return [(poly((22, 40), (30, 16), (46, 38)).d(), '#EF6C00', True), (poly((78, 40), (70, 16), (54, 38)).d(), '#EF6C00', True),
            (poly((27, 36), (31, 24), (40, 36)).d(), '#3E2723', False), (poly((73, 36), (69, 24), (60, 36)).d(), '#3E2723', False),
            (P().M(14, 50).C(18, 30, 82, 30, 86, 50).C(88, 72, 66, 100, 50, 108).C(34, 100, 12, 72, 14, 50).Z().d(), '#FB8C00', True),
            (P().M(22, 66).Q(40, 70, 50, 108).Q(60, 70, 78, 66).Q(70, 96, 50, 108).Q(30, 96, 22, 66).Z().d(), '#FFF3E0', False),
            *eyes(37, 63, 62), (E(50, 98, 6, 4.5).d(), '#212121', True)]

def bear():
    return [(circ(24, 36, 12).d(), '#8D5A34', True), (circ(76, 36, 12).d(), '#8D5A34', True), (circ(24, 36, 6).d(), '#C68B59', False), (circ(76, 36, 6).d(), '#C68B59', False),
            (circ(50, 68, 36).d(), '#A0683D', True), (E(50, 84, 16, 13).d(), '#E7C29A', True), *eyes(36, 64, 60),
            (E(50, 78, 6, 4.5).d(), '#2B1B10', True), (P().M(44, 88).Q(50, 94, 56, 88).Z().d(), '#2B1B10', True)]

def rabbit():
    return [(E(36, 34, 8, 24).d(), '#ECEFF1', True), (E(64, 34, 8, 24).d(), '#ECEFF1', True), (E(36, 34, 4, 17).d(), '#F8BBD0', False), (E(64, 34, 4, 17).d(), '#F8BBD0', False),
            (circ(50, 80, 30).d(), '#FAFAFA', True), *eyes(39, 61, 76), (poly((46, 86), (54, 86), (50, 91)).d(), '#F06292', True),
            (E(32, 92, 6, 4).d(), '#55F48FB1', False), (E(68, 92, 6, 4).d(), '#55F48FB1', False)]

def owl():
    return [(P().M(20, 34).L(34, 46).L(66, 46).L(80, 34).L(84, 90).C(80, 116, 20, 116, 16, 90).Z().d(), '#8D6E63', True),
            (E(50, 96, 22, 18).d(), '#D7CCC8', False), (circ(36, 60, 14).d(), '#FFFFFF', True), (circ(64, 60, 14).d(), '#FFFFFF', True),
            (circ(36, 60, 7).d(), '#FFA000', False), (circ(64, 60, 7).d(), '#FFA000', False), (circ(36, 60, 3.6).d(), '#1B1B1B', False), (circ(64, 60, 3.6).d(), '#1B1B1B', False),
            (poly((45, 72), (55, 72), (50, 82)).d(), '#FFB300', True)]

def panda():
    return [(circ(24, 40, 12).d(), '#212121', True), (circ(76, 40, 12).d(), '#212121', True), (circ(50, 72, 36).d(), '#FFFFFF', True),
            (rot_path(E(36, 66, 9, 12), 36, 66, 30).d(), '#212121', False), (rot_path(E(64, 66, 9, 12), 64, 66, -30).d(), '#212121', False),
            (circ(37, 64, 3.6).d(), '#FFFFFF', False), (circ(63, 64, 3.6).d(), '#FFFFFF', False),
            (E(50, 84, 6, 4).d(), '#212121', True), (P().M(44, 92).Q(50, 97, 56, 92).Z().d(), '#212121', True)]

def ladybug():
    out = [(circ(50, 38, 14).d(), '#212121', True), (P().M(44, 26).Q(36, 14, 30, 16).M(56, 26).Q(64, 14, 70, 16).Z().d(), '#212121', False),
           (circ(50, 76, 34).d(), '#E53935', True), (rect(49, 42, 51, 110).d(), '#212121', False)]
    for x, y, r in ((34, 62, 6), (66, 62, 6), (30, 84, 5), (70, 84, 5), (40, 100, 5), (60, 100, 5)): out.append((circ(x, y, r).d(), '#212121', False))
    out += eyes(44, 56, 36, 3.4)
    return out

def faces():
    keys = ['dog', 'cat', 'cow', 'sheep', 'pig', 'chicken', 'duck', 'horse', 'frog', 'lion', 'elephant', 'monkey']
    return [fit(animals.A[k]) for k in keys] + [fox(), bear(), rabbit(), owl(), panda(), ladybug()]
