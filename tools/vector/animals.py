"""Hayvanlar cizimleri (viewBox 200x200) -> AnimalArt.kt (+ on izleme)."""
import os, sys, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from vec import *

def sym(p): return join(p, p.mirror())
def E(cx, cy, rx, ry=None): return ellipse(cx, cy, rx, ry if ry is not None else rx)
def eyes(dx=22, cy=92, r=11, iris='#2B2B2B', white=True):
    out = []
    if white: out.append((sym(E(100 - dx, cy, r, r * 1.1)).d(), '#FFFFFF', True))
    out.append((sym(E(100 - dx + 1, cy + 1, r * 0.62, r * 0.7)).d(), iris, False))
    out.append((sym(E(100 - dx + 3.5, cy - 3, r * 0.25)).d(), '#FFFFFF', False))
    return out
def smile(cx=100, cy=140, w=14, color='#3A2A20'):
    return (P().M(cx - w, cy).Q(cx, cy + w * 0.9, cx + w, cy).Q(cx, cy + w * 0.5, cx - w, cy).Z().d(), color, False)
def cheeks(dx=40, cy=125, color='#55FF7A9C'): return (sym(E(100 - dx, cy, 10, 6)).d(), color, False)
def whiskers(y=128):
    ps = []
    for k, dy in enumerate([-6, 2, 10]):
        ps.append(P().M(70, y + dy * 0.6).L(30, y + dy - 4).L(30, y + dy - 2).L(70, y + dy * 0.6 + 2).Z())
    return (sym(join(*ps)).d(), '#5A3A2A', False)

A = {}
A['dog'] = [
    (sym(P().M(56, 62).C(28, 70, 22, 136, 44, 150).C(60, 146, 66, 104, 64, 74).Z()).d(), '#8B5A2B', True),
    (E(100, 105, 56, 52).d(), '#C68B59', True),
    (P().M(100, 60).C(86, 64, 84, 96, 100, 100).C(116, 96, 114, 64, 100, 60).Z().d(), '#F1D3B3', False),
    (E(100, 132, 30, 22).d(), '#F1D3B3', True),
    *eyes(24, 96, 11), (E(100, 118, 11, 8).d(), '#222222', True), (E(97, 115, 3, 2).d(), '#888888', False),
    (P().M(100, 126).L(100, 136).Z().d(), '#222222', False),
    (P().M(90, 138).Q(100, 160, 110, 138).Z().d(), '#FF7A8A', True), smile(100, 136, 12), cheeks(42, 124),
]
A['cat'] = [
    (sym(poly((50, 88), (58, 28), (94, 66))).d(), '#F4A261', True),
    (sym(poly((60, 78), (64, 44), (84, 66))).d(), '#FFB4C2', False),
    (E(100, 110, 58, 50).d(), '#F4A261', True),
    (sym(P().M(60, 70).Q(70, 76, 66, 88).Q(62, 78, 60, 70).Z()).d(), '#D9823F', False),
    (P().M(94, 62).L(106, 62).L(104, 80).L(96, 80).Z().d(), '#D9823F', False),
    *eyes(24, 104, 12, iris='#3E8E41'),
    (poly((92, 122), (108, 122), (100, 131)).d(), '#FF7A8A', True),
    (P().M(100, 131).Q(92, 142, 84, 136).Q(92, 139, 100, 131).Z().d(), '#5A3A2A', False),
    (P().M(100, 131).Q(108, 142, 116, 136).Q(108, 139, 100, 131).Z().d(), '#5A3A2A', False),
    whiskers(128), cheeks(42, 128),
]
spots = join(E(66, 84, 14, 18), E(134, 120, 12, 10), E(118, 70, 9, 7))
A['cow'] = [
    (sym(P().M(60, 64).Q(40, 40, 52, 30).Q(60, 44, 70, 60).Z()).d(), '#F1E3C6', True),
    (sym(E(46, 92, 20, 11)).d(), '#FFFFFF', True), (sym(E(46, 92, 12, 6)).d(), '#FFB4C2', False),
    (E(100, 100, 52, 56).d(), '#FFFFFF', True), (spots.d(), '#2B2B2B', False),
    (E(100, 142, 40, 26).d(), '#FFB4C2', True), (sym(E(86, 142, 6, 8)).d(), '#C0707F', False),
    *eyes(22, 98, 10), (P().M(80, 160).Q(100, 172, 120, 160).Q(100, 166, 80, 160).Z().d(), '#C0707F', False),
]
wool = join(*[E(100 + 58 * math.cos(a), 100 + 52 * math.sin(a), 22) for a in [i * math.pi / 6 for i in range(12)]], E(100, 100, 56, 52))
A['sheep'] = [
    (wool.d(), '#FFFFFF', True),
    (sym(E(52, 108, 18, 9)).d(), '#4A4A4A', True),
    (E(100, 112, 34, 42).d(), '#4A4A4A', True),
    (join(E(100, 70, 18, 12), E(84, 74, 11), E(116, 74, 11)).d(), '#FFFFFF', True),
    *eyes(14, 106, 8, iris='#1B1B1B'),
    (P().M(92, 136).Q(100, 142, 108, 136).L(100, 146).Z().d(), '#F5A0B4', False), cheeks(26, 126, '#66FF9AB4'),
]
A['pig'] = [
    (sym(poly((52, 76), (54, 34), (88, 58))).d(), '#F7A8B8', True),
    (E(100, 108, 60, 52).d(), '#F7A8B8', True),
    (E(100, 124, 26, 18).d(), '#F28CA3', True), (sym(E(91, 124, 5, 8)).d(), '#B0506A', False),
    *eyes(26, 92, 10), smile(100, 148, 12, '#B0506A'), cheeks(46, 120, '#66FF6A8C'),
]
A['chicken'] = [
    (join(E(86, 40, 13), E(102, 34, 14), E(118, 42, 12)).d(), '#E63946', True),
    (E(100, 108, 56, 60).d(), '#FFFFFF', True),
    *eyes(22, 98, 10),
    (poly((84, 116), (116, 116), (100, 136)).d(), '#FFB703', True),
    (join(E(96, 146, 7, 11), E(106, 144, 6, 9)).d(), '#E63946', True), cheeks(42, 122),
]
A['duck'] = [
    (P().M(96, 54).Q(90, 34, 104, 30).Q(100, 42, 108, 50).Z().d(), '#FFD166', True),
    (E(100, 104, 58, 54).d(), '#FFD166', True),
    *eyes(24, 92, 10),
    (P().M(66, 124).Q(100, 100, 134, 124).Q(100, 152, 66, 124).Z().d(), '#F77F00', True),
    (P().M(72, 124).Q(100, 130, 128, 124).Q(100, 128, 72, 124).Z().d(), '#B85C00', False), cheeks(46, 112),
]
A['horse'] = [
    (sym(poly((66, 50), (72, 16), (88, 44))).d(), '#8B5A2B', True),
    (P().M(64, 60).C(60, 30, 140, 30, 136, 60).C(140, 110, 136, 140, 126, 170).Q(100, 186, 74, 170).C(64, 140, 60, 110, 64, 60).Z().d(), '#A0673A', True),
    (P().M(88, 30).Q(110, 20, 124, 44).Q(116, 70, 104, 62).Q(98, 46, 88, 30).Z().d(), '#3B2416', True),
    (E(100, 154, 30, 22).d(), '#D9A57A', True), (sym(E(88, 156, 5, 7)).d(), '#5C3A21', False),
    *eyes(26, 92, 10), (P().M(100, 70).L(106, 82).L(100, 90).L(94, 82).Z().d(), '#FFFFFF', False),
]
A['frog'] = [
    (sym(E(66, 62, 24)).d(), '#6BBF59', True),
    (P().M(36, 110).C(36, 60, 164, 60, 164, 110).C(164, 160, 36, 160, 36, 110).Z().d(), '#6BBF59', True),
    (sym(E(66, 62, 15)).d(), '#FFFFFF', False), (sym(E(67, 64, 8, 9)).d(), '#1B1B1B', False), (sym(E(70, 59, 3)).d(), '#FFFFFF', False),
    (P().M(58, 118).Q(100, 150, 142, 118).Q(100, 140, 58, 118).Z().d(), '#2D6A4F', False),
    (sym(E(94, 100, 2.5, 2)).d(), '#2D6A4F', False), (sym(E(56, 120, 10, 6)).d(), '#66FF7A9C', False),
]
mane = join(*[E(100 + 64 * math.cos(a), 104 + 62 * math.sin(a), 20) for a in [i * math.pi / 7 for i in range(14)]], E(100, 104, 64, 62))
A['lion'] = [
    (mane.d(), '#C8701E', True),
    (sym(E(60, 66, 13)).d(), '#F4C46A', True),
    (E(100, 108, 46, 44).d(), '#F4C46A', True),
    (E(100, 128, 22, 16).d(), '#FBE2A8', False),
    *eyes(18, 98, 9),
    (poly((92, 118), (108, 118), (100, 127)).d(), '#5C3A21', True), smile(100, 134, 10, '#5C3A21'),
]
A['elephant'] = [
    (sym(P().M(60, 80).C(10, 50, 6, 150, 56, 136).Z()).d(), '#9AA5B1', True),
    (sym(P().M(56, 88).C(26, 72, 24, 134, 54, 128).Z()).d(), '#F2B5C0', False),
    (E(100, 100, 50, 52).d(), '#9AA5B1', True),
    (P().M(88, 120).C(86, 150, 90, 170, 112, 178).Q(118, 172, 112, 166).C(100, 160, 106, 146, 112, 120).Z().d(), '#9AA5B1', True),
    *eyes(20, 96, 9), (sym(P().M(76, 132).Q(80, 152, 88, 146).Q(84, 140, 82, 128).Z()).d(), '#FFFFFF', True), cheeks(36, 116),
]
A['monkey'] = [
    (sym(E(46, 104, 18)).d(), '#8B5A2B', True), (sym(E(46, 104, 10)).d(), '#E8C29A', False),
    (E(100, 100, 52, 54).d(), '#8B5A2B', True),
    (P().M(100, 90).C(80, 66, 54, 84, 62, 110).C(66, 140, 84, 156, 100, 156).C(116, 156, 134, 140, 138, 110).C(146, 84, 120, 66, 100, 90).Z().d(), '#E8C29A', True),
    *eyes(18, 102, 10), (sym(E(95, 124, 2.5, 2)).d(), '#5C3A21', False), smile(100, 136, 16, '#5C3A21'),
    (P().M(96, 48).Q(100, 30, 112, 36).Q(104, 40, 104, 50).Z().d(), '#8B5A2B', True),
]
ORDER = ['dog', 'cat', 'cow', 'sheep', 'pig', 'chicken', 'duck', 'horse', 'frog', 'lion', 'elephant', 'monkey']
BG = ['#FFE3C2', '#FFD9E2', '#D8F3DC', '#E0F0FF', '#FFE0EC', '#FFF3BF', '#D0F4FF', '#F1E3D3', '#E9F7D4', '#FFE8B6', '#E5E7FF', '#F4E1CF']

if __name__ == "__main__" and "--preview" in sys.argv:
    from PIL import Image
    sheet = Image.new("RGB", (200 * 6, 200 * 2), "white")
    for i, k in enumerate(ORDER):
        render(svg(A[k], {}, W=200, H=200, bg=BG[i], scale=1), "/tmp/an.png")
        sheet.paste(Image.open("/tmp/an.png").convert("RGB"), ((i % 6) * 200, (i // 6) * 200))
    sheet.save("/tmp/animals_sheet.png")

if __name__ == "__main__" and "--kotlin" in sys.argv:
    out = ["package com.eomer91.minigamesworld.ui.games.animals", "", "import com.eomer91.minigamesworld.ui.games.common.VP", "",
           "/** tools/vector/animals.py ile uretildi (viewBox 200x200). */", "object AnimalArt {", "    val parts: Map<String, List<VP>> = mapOf("]
    for k in ORDER: out.append(f'        "{k}" to {kt_parts(A[k], "        ")},')
    out.append("    )")
    out.append("    val background: Map<String, Long> = mapOf(" + ", ".join(f'"{k}" to 0xFF{BG[i][1:]}' for i, k in enumerate(ORDER)) + ")")
    out.append("}")
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../app/src/main/java/com/eomer91/minigamesworld/ui/games/animals/AnimalArt.kt")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "w").write("\n".join(out) + "\n")
    print("wrote")
