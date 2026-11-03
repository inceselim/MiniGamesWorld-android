"""Makyaj Salonu cizimleri -> MakeupArt.kt (+ on izleme). viewBox 300x360."""
import os, sys, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from vec import *

W = 300
def Pw(): return P(W)
def sym(p): return join(p, p.mirror())
def el(cx, cy, rx, ry):
    e = ellipse(cx, cy, rx, ry); e.W = W; return e

S, SD, HR, HD, HL, MN = 'skin', 'skinD', 'hair', 'hairD', 'hairL', 'main'

FACE_SHAPE = Pw().M(58, 160).C(58, 95, 100, 58, 150, 58).C(200, 58, 242, 95, 242, 160).C(242, 225, 200, 282, 150, 286).C(100, 282, 58, 225, 58, 160).Z()
BODY = [
    (Pw().M(118, 262).L(182, 262).L(186, 318).L(114, 318).Z().d(), S, True),
    (Pw().M(30, 360).C(36, 320, 76, 304, 116, 298).Q(150, 324, 184, 298).C(224, 304, 264, 320, 270, 360).Z().d(), MN, True),
    (Pw().M(116, 298).Q(150, 324, 184, 298).L(178, 296).Q(150, 314, 122, 296).Z().d(), 'mainD', False),
    (sym(el(62, 176, 12, 20)).d(), S, True),
    (FACE_SHAPE.d(), S, True),
]
HAIR = {
    'long': dict(back=[(Pw().M(52, 170).C(40, 80, 90, 36, 150, 36).C(210, 36, 260, 80, 248, 170).C(256, 230, 262, 300, 266, 360).L(34, 360).C(38, 300, 44, 230, 52, 170).Z().d(), HR, True)],
                 front=[(Pw().M(58, 156).C(52, 84, 98, 46, 150, 46).C(202, 46, 248, 84, 242, 156).C(234, 118, 214, 96, 190, 88).C(178, 106, 150, 114, 124, 106).C(104, 114, 80, 128, 58, 156).Z().d(), HR, True),
                        (Pw().M(90, 70).Q(150, 50, 210, 70).Q(150, 60, 90, 78).Z().d(), HL, False)]),
    'bob': dict(back=[(Pw().M(50, 170).C(40, 80, 90, 38, 150, 38).C(210, 38, 260, 80, 250, 170).C(254, 214, 250, 244, 238, 262).Q(150, 272, 62, 262).C(50, 244, 46, 214, 50, 170).Z().d(), HR, True)],
                front=[(Pw().M(60, 136).C(56, 74, 100, 44, 150, 44).C(200, 44, 244, 74, 240, 136).Q(150, 112, 60, 136).Z().d(), HR, True),
                       (sym(Pw().M(58, 136).C(52, 180, 54, 220, 64, 258).L(80, 256).C(70, 220, 66, 180, 70, 134).Z()).d(), HR, True),
                       (Pw().M(92, 66).Q(150, 50, 208, 66).Q(150, 58, 92, 74).Z().d(), HL, False)]),
    'bun': dict(back=[(el(150, 34, 40, 30).d(), HR, True), (Pw().M(58, 152).C(54, 72, 100, 44, 150, 44).C(200, 44, 246, 72, 242, 152).Z().d(), HR, True)],
                front=[(Pw().M(58, 152).C(54, 80, 100, 50, 150, 50).C(200, 50, 246, 80, 242, 152).C(228, 108, 192, 86, 152, 92).C(116, 96, 80, 118, 58, 152).Z().d(), HR, True),
                       (Pw().M(120, 26).Q(150, 14, 180, 26).Q(150, 20, 120, 30).Z().d(), HL, False)]),
}
# Goz (sol); sag ayna
almond = Pw().M(94, 166).Q(113, 146, 132, 166).Q(113, 184, 94, 166).Z()
EYES = [
    (sym(almond).d(), '#FFFFFF', True),
    (sym(el(113, 166, 8, 8)).d(), 'eye', False),
    (sym(el(113, 166, 4, 4)).d(), '#1B1B2A', False),
    (sym(el(116, 162.5, 2.3, 2.3)).d(), '#FFFFFF', False),
    (sym(Pw().M(92, 166).Q(113, 145, 134, 166).L(134, 163).Q(113, 141, 92, 163).Z()).d(), '#2A1A12', False),
]
BROWS = [(sym(Pw().M(90, 134).Q(112, 120, 136, 130).L(136, 135).Q(112, 126, 90, 139).Z()).d(), HD, False)]
def lash(style):
    spikes = []
    n, ln, w = (5, 7, 1.6) if style == 1 else (7, 12, 2.2)
    for i in range(n):
        t = 0.15 + 0.7 * i / (n - 1)
        # ust goz kapagi egrisi (Q 94,166 / 113,145 / 132,166)
        x = (1 - t) ** 2 * 93 + 2 * (1 - t) * t * 113 + t * t * 133
        y = (1 - t) ** 2 * 165 + 2 * (1 - t) * t * 144 + t * t * 165
        ang = math.radians(-90 - 50 * (1 - t))  # dis kosede sola yatik
        dx, dy = math.cos(ang) * ln, math.sin(ang) * ln
        nx, ny = -math.sin(ang) * w, math.cos(ang) * w
        spikes.append(Pw().M(x - nx, y - ny).L(x + nx, y + ny).L(x + dx, y + dy).Z())
    p = join(*spikes)
    if style == 2:
        p = join(p, Pw().M(130, 163).L(146, 152).L(134, 166).Z())
    return [(sym(p).d(), '#1B1B1B', False)]
LASHES = {1: lash(1), 2: lash(2)}
NOSE = [(Pw().M(144, 206).Q(150, 214, 157, 208).Q(150, 211, 144, 206).Z().d(), SD, False), (el(150, 200, 4, 7).d(), '#22FFFFFF', False)]
upper = Pw().M(124, 238).Q(137, 228, 150, 234).Q(163, 228, 176, 238).Q(150, 244, 124, 238).Z()
lower = Pw().M(124, 238).Q(150, 244, 176, 238).Q(164, 258, 150, 258).Q(136, 258, 124, 238).Z()
LIPS_BASE = [(join(upper, lower).d(), 'lips', True)]
LIPS_SHAPE = join(upper, lower).d()
LIPS_TOP = [(Pw().M(126, 238).Q(150, 246, 174, 238).Q(150, 242, 126, 238).Z().d(), '#66000000', False),
            (el(156, 248, 6, 2.5).d(), '#55FFFFFF', False)]
SHADOW_SHAPE = sym(el(113, 158, 27, 15)).d()

if __name__ == "__main__" and "--preview" in sys.argv:
    from PIL import Image
    imgs = []
    for i, (h, lstyle, cols) in enumerate([('long', 0, dict(skin='#F6D2B8', hair='#6B3E26', main='#FF8FAB', lips='#D98A8A')),
                                           ('bob', 1, dict(skin='#C68642', hair='#1B1B1B', main='#4CC9F0', lips='#B5646E')),
                                           ('bun', 2, dict(skin='#FCE3D3', hair='#F2C14E', main='#9D4EDD', lips='#E39A9A'))]):
        parts = HAIR[h]['back'] + BODY
        if i > 0: parts += [(SHADOW_SHAPE, '#99C77DFF' if i == 2 else '#8806D6A0', False)]
        parts += EYES + (LASHES[lstyle] if lstyle else []) + BROWS + NOSE + LIPS_BASE
        if i == 2: parts += [(LIPS_SHAPE, '#E6E5383B', False)]
        parts += LIPS_TOP + HAIR[h]['front']
        if i > 0: parts += [(sym(el(100, 212, 22, 14)).d(), '#55FF5C8A', False)]
        render(svg(parts, cols, W=300, H=360, bg='#FDEBF3', scale=1), f"/tmp/mk_{i}.png")
        imgs.append(Image.open(f"/tmp/mk_{i}.png"))
    sheet = Image.new("RGB", (900, 360), "white")
    for i, im in enumerate(imgs): sheet.paste(im, (i * 300, 0))
    sheet.save("/tmp/mk_sheet.png")

if __name__ == "__main__" and "--kotlin" in sys.argv:
    out = ["package com.eomer91.minigamesworld.ui.games.makeup", "", "import com.eomer91.minigamesworld.ui.games.common.VP", "",
           "/** tools/vector/makeup.py ile uretildi (viewBox 300x360). */", "object MakeupArt {"]
    for name, parts in [('body', BODY), ('eyes', EYES), ('brows', BROWS), ('nose', NOSE), ('lipsBase', LIPS_BASE), ('lipsTop', LIPS_TOP)]:
        out.append(f"    val {name} = {kt_parts(parts, '    ')}")
    out.append("    val hairBack: Map<String, List<VP>> = mapOf(")
    for k, v in HAIR.items(): out.append(f'        "{k}" to {kt_parts(v["back"], "        ")},')
    out.append("    )")
    out.append("    val hairFront: Map<String, List<VP>> = mapOf(")
    for k, v in HAIR.items(): out.append(f'        "{k}" to {kt_parts(v["front"], "        ")},')
    out.append("    )")
    out.append(f"    val lashes: List<List<VP>> = listOf(emptyList(), {kt_parts(LASHES[1], '    ')}, {kt_parts(LASHES[2], '    ')})")
    out.append(f'    const val LIPS_SHAPE = "{LIPS_SHAPE}"')
    out.append(f'    const val SHADOW_SHAPE = "{SHADOW_SHAPE}"')
    out.append("}")
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../app/src/main/java/com/eomer91/minigamesworld/ui/games/makeup/MakeupArt.kt")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "w").write("\n".join(out) + "\n")
    print("wrote")
