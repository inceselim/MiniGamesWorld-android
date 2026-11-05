"""Gece seti: gokyuzu ve uzay (18)."""
from mjlib import *
import math
GOLD, NAVY = '#FFC83D', '#283593'

def crescent_path(cx1, cy1, r1, cx2, cy2, r2, n=72):
    """(cx1,cy1,r1) dairesinden (cx2,cy2,r2) dairesi cikarilmis hilal."""
    out_pts = []
    for i in range(n * 4):
        a = 2 * math.pi * i / (n * 4); x, y = cx1 + r1 * math.cos(a), cy1 + r1 * math.sin(a)
        out_pts.append((x, y, math.hypot(x - cx2, y - cy2) >= r2))
    # disarida kalan ardisik yay
    k0 = next(i for i in range(len(out_pts)) if out_pts[i][2] and not out_pts[i - 1][2])
    arc1 = []
    i = k0
    while out_pts[i % len(out_pts)][2]: arc1.append(out_pts[i % len(out_pts)][:2]); i += 1
    a_end = math.atan2(arc1[-1][1] - cy2, arc1[-1][0] - cx2); a_start = math.atan2(arc1[0][1] - cy2, arc1[0][0] - cx2)
    # ic daire yayi: bitisten baslangica, birinci dairenin icinde kalan taraftan
    da = (a_start - a_end) % (2 * math.pi)
    mid = a_end + da / 2
    if math.hypot(cx2 + r2 * math.cos(mid) - cx1, cy2 + r2 * math.sin(mid) - cy1) > r1: da -= 2 * math.pi
    arc2 = [(cx2 + r2 * math.cos(a_end + da * j / n), cy2 + r2 * math.sin(a_end + da * j / n)) for j in range(n + 1)]
    return poly(*(arc1 + arc2))

def hl(x, y, rx, ry, c='#55FFFFFF'): return (E(x, y, rx, ry).d(), c, False)

def crescent():
    return [(crescent_path(50, 66, 36, 68, 54, 30).d(), GOLD, True), (star(80, 98, 7, 3).d(), GOLD, True)]

def fullmoon():
    return [(circ(50, 66, 36).d(), '#ECEFF1', True), (circ(38, 54, 8).d(), '#CFD8DC', True), (circ(62, 76, 10).d(), '#CFD8DC', True),
            (circ(64, 48, 5).d(), '#CFD8DC', True), (circ(38, 82, 4).d(), '#CFD8DC', True)]

def bigstar():
    return [(star(50, 68, 40, 17).d(), GOLD, True), (star(50, 68, 26, 11).d(), '#FFE082', False), hl(42, 54, 4, 6, '#88FFFFFF')]

def shooting():
    trail = P().M(70, 36).L(14, 98).L(20, 104).L(76, 44).Z()
    return [(trail.d(), '#66FFE082', False), (P().M(64, 32).L(22, 84).L(26, 88).L(68, 38).Z().d(), '#99FFC83D', False),
            (star(72, 40, 18, 8).d(), GOLD, True)]

def saturn():
    back = P().M(10, 76).C(10, 64, 90, 52, 90, 58).C(90, 66, 10, 82, 10, 76).Z()
    return [(rot_path(E(50, 68, 44, 10), 50, 68, -18).d(), '#FFB74D', True), (circ(50, 66, 26).d(), '#F6C177', True),
            (rect(24, 60, 76, 64).d(), '#55E09A4A', False), (rect(26, 72, 74, 75).d(), '#55E09A4A', False),
            (rot_path(P().M(6, 68).C(6, 78, 94, 78, 94, 68).C(94, 72, 6, 74, 6, 68).Z(), 50, 68, -18).d(), '#FFB74D', True)]

def earth():
    land = join(P().M(30, 46).C(40, 40, 52, 46, 50, 56).C(48, 64, 36, 62, 36, 72).C(36, 80, 26, 78, 22, 70).C(18, 60, 22, 50, 30, 46).Z(),
                P().M(60, 70).C(70, 64, 82, 70, 80, 82).C(78, 92, 66, 98, 58, 92).C(54, 86, 52, 76, 60, 70).Z(),
                P().M(58, 38).C(64, 34, 74, 38, 72, 46).C(68, 50, 60, 48, 58, 38).Z())
    return [(circ(50, 66, 36).d(), '#2F80ED', True), (land.d(), '#43A047', False), hl(36, 50, 8, 5, '#44FFFFFF')]

def rocket():
    body = P().M(50, 16).C(66, 30, 68, 64, 62, 92).L(38, 92).C(32, 64, 34, 30, 50, 16).Z()
    return [(P().M(40, 92).Q(50, 128, 60, 92).Z().d(), '#FF7043', True), (P().M(44, 92).Q(50, 114, 56, 92).Z().d(), '#FFD54F', False),
            (P().M(38, 70).L(24, 96).L(38, 92).Z().d(), '#E53935', True), (P().M(62, 70).L(76, 96).L(62, 92).Z().d(), '#E53935', True),
            (body.d(), '#ECEFF1', True), (P().M(50, 16).C(58, 22, 62, 30, 63, 36).L(37, 36).C(38, 30, 42, 22, 50, 16).Z().d(), '#E53935', False),
            (circ(50, 56, 9).d(), '#4FC3F7', True), hl(47, 53, 3, 2.5, '#88FFFFFF')]

def ufo():
    return [(P().M(30, 64).L(10, 120).L(90, 120).L(70, 64).Z().d(), '#33FFF59D', False),
            (P().M(30, 58).C(30, 30, 70, 30, 70, 58).Z().d(), '#80DEEA', True), hl(42, 46, 5, 4, '#88FFFFFF'),
            (E(50, 64, 42, 13).d(), '#9E9E9E', True), (E(50, 60, 30, 6).d(), '#BDBDBD', False)] + \
           [(circ(x, 66, 3.4).d(), GOLD, True) for x in (24, 38, 50, 62, 76)]

def satellite():
    return [(rect(8, 52, 34, 74).d(), '#3F51B5', True), (rect(66, 52, 92, 74).d(), '#3F51B5', True),
            (rect(20.5, 52, 21.5, 74).d(), '#9FA8DA', False), (rect(78.5, 52, 79.5, 74).d(), '#9FA8DA', False),
            (rect(34, 61, 66, 65).d(), '#78909C', False), (rrect(38, 48, 62, 78, 4).d(), '#B0BEC5', True),
            (P().M(50, 48).L(50, 34).L(52, 34).L(52, 48).Z().d(), '#78909C', False), (circ(51, 32, 4).d(), '#E53935', True),
            (P().M(42, 84).Q(50, 92, 58, 84).L(56, 82).Q(50, 88, 44, 82).Z().d(), '#B0BEC5', False)]

def telescope():
    tube = rot_path(rrect(20, 42, 84, 60, 4), 52, 51, -24)
    return [(P().M(50, 66).L(32, 118).L(36, 118).L(52, 72).L(68, 118).L(72, 118).L(54, 66).Z().d(), '#6D4C41', False),
            (tube.d(), '#5C6BC0', True), (rot_path(rect(76, 38, 86, 64), 52, 51, -24).d(), '#3949AB', True),
            (rot_path(rect(46, 40, 52, 62), 52, 51, -24).d(), GOLD, False), (star(26, 30, 7, 3).d(), GOLD, True)]

def alien():
    return [(P().M(36, 30).L(30, 14).L(33, 13).L(39, 29).Z().d(), '#7CB342', False), (P().M(64, 30).L(70, 14).L(67, 13).L(61, 29).Z().d(), '#7CB342', False),
            (circ(31, 13, 4).d(), '#AED581', True), (circ(69, 13, 4).d(), '#AED581', True),
            (P().M(50, 26).C(80, 26, 86, 60, 72, 84).C(64, 98, 56, 104, 50, 104).C(44, 104, 36, 98, 28, 84).C(14, 60, 20, 26, 50, 26).Z().d(), '#8BC34A', True),
            (rot_path(E(37, 62, 10, 6), 37, 62, 25).d(), '#1B1B1B', False), (rot_path(E(63, 62, 10, 6), 63, 62, -25).d(), '#1B1B1B', False),
            (circ(34, 60, 2).d(), '#FFFFFF', False), (circ(60, 60, 2).d(), '#FFFFFF', False), (P().M(42, 86).Q(50, 92, 58, 86).Z().d(), '#33691E', True)]

def astronaut():
    return [(rrect(26, 92, 74, 122, 10).d(), '#ECEFF1', True), (circ(50, 64, 32).d(), '#ECEFF1', True),
            (rrect(28, 48, 72, 82, 16).d(), '#283593', True), (P().M(34, 56).Q(42, 50, 52, 52).L(50, 58).Q(42, 58, 36, 62).Z().d(), '#7986CB', False),
            (rect(40, 100, 60, 112).d(), '#90A4AE', True), (circ(45, 106, 2).d(), '#E53935', False), (circ(55, 106, 2).d(), '#43A047', False)]

def comet():
    return [(P().M(64, 52).Q(30, 60, 8, 108).Q(40, 76, 70, 66).Z().d(), '#664FC3F7', False),
            (P().M(66, 50).Q(40, 64, 20, 100).Q(44, 72, 72, 62).Z().d(), '#9981D4FA', False),
            (circ(70, 54, 14).d(), '#E1F5FE', True), (circ(70, 54, 8).d(), '#FFFFFF', False)]

def galaxy():
    arms = []
    for k in range(2):
        a0 = k * math.pi; left, right = [], []
        for i in range(40):
            tt = i / 39; a = a0 + tt * 3.6; r = 5 + tt * 33; w = 1 + 6 * tt * (1 - tt * 0.6)
            x, y = 50 + r * math.cos(a), 66 + r * math.sin(a) * 0.85
            nx, ny = math.cos(a), math.sin(a) * 0.85
            left.append((x + nx * w, y + ny * w)); right.append((x - nx * w, y - ny * w))
        arms.append(poly(*(left + right[::-1])))
    return [(E(50, 66, 34, 28).d(), '#337E57C2', False), (arms[0].d(), '#9575CD', True), (arms[1].d(), '#7E57C2', True),
            (circ(50, 66, 11).d(), '#FFF59D', True), (circ(50, 66, 5).d(), '#FFFFFF', False)] + \
           [(star(x, y, 4, 1.6).d(), GOLD, False) for x, y in ((16, 30), (84, 34), (82, 108), (18, 104))]

def mars():
    return [(circ(50, 66, 36).d(), '#E5733C', True), (E(38, 54, 9, 6).d(), '#C2552A', False), (E(64, 80, 12, 7).d(), '#C2552A', False),
            (E(60, 46, 5, 3).d(), '#C2552A', False), (E(50, 32, 14, 4).d(), '#FFFFFF', False), hl(34, 74, 4, 6, '#33FFFFFF')]

def constellation():
    pts = [(16, 44), (32, 40), (46, 50), (58, 56), (64, 76), (86, 82), (82, 100), (60, 96)]
    lines = []
    for (x0, y0), (x1, y1) in zip(pts, pts[1:] + [pts[4]]):
        L = math.hypot(x1 - x0, y1 - y0); nx, ny = -(y1 - y0) / L, (x1 - x0) / L
        lines.append(poly((x0 + nx, y0 + ny), (x1 + nx, y1 + ny), (x1 - nx, y1 - ny), (x0 - nx, y0 - ny)))
    return [(join(*lines).d(), '#9FA8DA', False)] + [(star(x, y, 7, 3).d(), GOLD, True) for x, y in pts]

def lantern():
    return [(P().M(49, 14).L(51, 14).L(51, 30).L(49, 30).Z().d(), '#5D4037', False), (rect(38, 28, 62, 34).d(), '#8D6E63', True),
            (P().M(30, 66).C(30, 40, 70, 40, 70, 66).C(70, 92, 30, 92, 30, 66).Z().d(), '#E53935', True),
            (P().M(40, 66).C(40, 46, 60, 46, 60, 66).C(60, 86, 40, 86, 40, 66).Z().d(), '#FF7043', False), (circ(50, 66, 8).d(), '#FFE082', False),
            (rect(38, 90, 62, 96).d(), '#8D6E63', True), (P().M(46, 96).L(54, 96).L(56, 118).L(44, 118).Z().d(), GOLD, False)]

def cloudmoon():
    cl = [circ(40, 84, 16), circ(60, 80, 18), circ(26, 92, 12), circ(76, 94, 11), rrect(24, 86, 82, 106, 10)]
    return [(crescent_path(56, 50, 24, 68, 42, 20).d(), GOLD, True)] + [(c.d(), '#B3C1F2', True) for c in cl] + \
           [(join(*cl).d(), '#B3C1F2', False), (star(22, 38, 6, 2.5).d(), GOLD, True)]

def faces():
    return [crescent(), fullmoon(), bigstar(), shooting(), saturn(), earth(), rocket(), ufo(), satellite(), telescope(),
            alien(), astronaut(), comet(), galaxy(), mars(), constellation(), lantern(), cloudmoon()]
