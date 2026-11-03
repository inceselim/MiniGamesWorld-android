"""Giydirme oyunu cizimleri -> DressUpArt.kt (+ on izleme). viewBox 200x400."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from vec import *

def sym(p):  # sol parca + aynasi
    return join(p, p.mirror())

S = 'skin'; SD = 'skinD'; HR = 'hair'; HD = 'hairD'; HL = 'hairL'; MN = 'main'; MD = 'mainD'; ML = 'mainL'

# ---------------------------------------------------------------- Govde
leg = P().M(80, 226).C(80, 280, 84, 330, 86, 366).L(96, 366).C(97, 330, 99, 280, 100, 238).Z()
arm = P().M(76, 142).C(64, 146, 60, 160, 58, 190).C(56, 212, 54, 232, 52, 246).L(60, 248).C(64, 230, 66, 210, 69, 190).C(71, 172, 74, 160, 80, 152).Z()
torso = P().M(74, 140).C(70, 165, 74, 190, 76, 205).C(72, 215, 72, 228, 76, 238).L(124, 238).C(128, 228, 128, 215, 124, 205).C(126, 190, 130, 165, 126, 140).Q(100, 150, 74, 140).Z()
neck = P().M(92, 110).L(108, 110).L(109, 144).Q(100, 150, 91, 144).Z()
BODY = [
    (sym(leg).d(), S, True), (sym(ellipse(90, 369, 8, 5)).d(), S, True),
    (sym(arm).d(), S, True), (torso.d(), S, True), (neck.d(), S, True),
    (sym(ellipse(60, 86, 6, 9)).d(), S, True), (ellipse(100, 80, 40, 44).d(), S, True),
]
# Varsayilan ic giyim (hep cizilir)
BASE = [(P().M(76, 146).Q(100, 156, 124, 146).L(126, 206).C(128, 220, 128, 232, 126, 242).L(103, 246).L(100, 236).L(97, 246).L(74, 242).C(72, 232, 72, 220, 74, 206).Z().d(), '#E9E4F5', True)]
HANDS = [(sym(ellipse(56, 252, 6, 7)).d(), S, True)]
FACE = [
    (sym(ellipse(85, 87, 9, 11)).d(), '#FFFFFF', True),
    (sym(ellipse(86, 89, 7, 8.8)).d(), 'eye', False),
    (sym(ellipse(86, 90, 3.6, 4.3)).d(), '#1B1B2A', False),
    (sym(ellipse(89, 85.5, 2.2, 2.5)).d(), '#FFFFFF', False),
    (sym(ellipse(84, 93.5, 1.1, 1.2)).d(), '#CCFFFFFF', False),
    (sym(P().M(74.5, 82).Q(85, 69, 96, 80).L(96, 83).Q(85, 73, 74.5, 85).Z()).d(), '#2A1A12', False),
    (sym(P().M(76, 68).Q(85, 62, 94, 67).L(94, 69.5).Q(85, 65, 76, 70.5).Z()).d(), HD, False),
    (ellipse(100, 100, 1.8, 1.2).d(), SD, False),
    (P().M(91, 108).Q(100, 118, 109, 108).Q(100, 113, 91, 108).Z().d(), 'lips', False),
    (sym(ellipse(77, 102, 7, 3.8)).d(), '#55FF7A9C', False),
]

# ---------------------------------------------------------------- Saclar (back, front)
cap = P().M(58, 78).C(56, 40, 78, 26, 100, 26).C(122, 26, 144, 40, 142, 78).Z()
shine = P().M(70, 44).Q(100, 30, 130, 44).Q(100, 36, 70, 48).Z()
HAIR = {
    'long': dict(
        back=[(P().M(58, 70).C(56, 38, 78, 26, 100, 26).C(122, 26, 144, 38, 142, 70).C(150, 120, 150, 180, 150, 232).Q(125, 242, 100, 236).Q(75, 242, 50, 232).C(50, 180, 50, 120, 58, 70).Z().d(), HR, True)],
        front=[(P().M(58, 86).C(54, 50, 74, 30, 100, 30).C(126, 30, 146, 50, 142, 86).C(138, 72, 132, 62, 124, 58).C(120, 68, 108, 74, 96, 70).C(90, 76, 80, 78, 74, 74).C(68, 80, 62, 82, 58, 86).Z().d(), HR, True),
               (sym(P().M(58, 80).C(54, 110, 56, 140, 62, 170).L(70, 168).C(66, 140, 64, 110, 66, 84).Z()).d(), HR, True),
               (shine.d(), HL, False)]),
    'bob': dict(
        back=[(P().M(56, 72).C(54, 38, 78, 26, 100, 26).C(122, 26, 146, 38, 144, 72).C(148, 96, 146, 116, 140, 128).Q(120, 134, 100, 130).Q(80, 134, 60, 128).C(54, 116, 52, 96, 56, 72).Z().d(), HR, True)],
        front=[(P().M(58, 84).C(56, 48, 76, 30, 100, 30).C(124, 30, 144, 48, 142, 84).L(136, 72).L(128, 76).L(122, 64).L(112, 70).L(104, 62).L(96, 70).L(86, 64).L(80, 74).L(70, 70).L(64, 80).Z().d(), HR, True),
               (sym(P().M(57, 84).C(54, 100, 56, 116, 60, 126).L(68, 122).C(64, 110, 63, 96, 64, 86).Z()).d(), HR, True),
               (shine.d(), HL, False)]),
    'ponytail': dict(
        back=[(cap.d(), HR, True),
              (P().M(134, 46).C(172, 58, 170, 120, 160, 170).C(156, 192, 148, 204, 140, 200).C(150, 160, 152, 110, 130, 70).Z().d(), HR, True)],
        front=[(P().M(58, 86).C(56, 46, 80, 28, 104, 30).C(128, 32, 146, 52, 142, 86).C(134, 66, 120, 56, 100, 56).C(84, 58, 70, 70, 58, 86).Z().d(), HR, True),
               (ellipse(136, 50, 6, 7).d(), '#E84A8A', True), (shine.d(), HL, False)]),
    'buns': dict(
        back=[(sym(ellipse(66, 36, 19, 18)).d(), HR, True), (cap.d(), HR, True)],
        front=[(P().M(58, 84).C(56, 48, 76, 30, 100, 30).C(124, 30, 144, 48, 142, 84).L(136, 72).L(128, 76).L(122, 64).L(112, 70).L(104, 62).L(96, 70).L(86, 64).L(80, 74).L(70, 70).L(64, 80).Z().d(), HR, True),
               (sym(P().M(56, 32).Q(66, 24, 76, 30).Q(66, 28, 56, 36).Z()).d(), HL, False)]),
    'curly': dict(
        back=[(join(*[ellipse(x, y, r, r) for x, y, r in [(60, 60, 22), (140, 60, 22), (52, 100, 21), (148, 100, 21), (50, 140, 20), (150, 140, 20), (54, 178, 19), (146, 178, 19), (62, 208, 16), (138, 208, 16), (80, 34, 22), (120, 34, 22), (100, 28, 22)]]).d(), HR, True)],
        front=[(join(*[ellipse(x, y, r, r) for x, y, r in [(64, 70, 12), (74, 54, 13), (88, 46, 13), (102, 44, 13), (116, 46, 13), (128, 54, 13), (138, 70, 12)]]).d(), HR, True),
               (join(*[ellipse(x, y, 4, 3) for x, y in [(74, 50), (100, 40), (124, 50)]]).d(), HL, False)]),
    'braids': dict(
        back=[(cap.d(), HR, True),
              (sym(join(*[ellipse(58, 104 + 18 * i, 9 - i * 0.5, 11) for i in range(7)])).d(), HR, True),
              (sym(ellipse(58, 232, 6, 4)).d(), '#E84A8A', True)],
        front=[(sym(P().M(58, 86).C(54, 48, 76, 30, 100, 32).L(100, 50).C(86, 54, 70, 68, 58, 86).Z()).d(), HR, True), (shine.d(), HL, False)]),
}

# ---------------------------------------------------------------- Kiyafetler
def tee():
    return P().M(74, 140).Q(100, 152, 126, 140).L(144, 152).L(150, 176).L(136, 180).L(130, 166).C(128, 182, 128, 196, 128, 210).Q(100, 216, 72, 210).C(72, 196, 72, 182, 70, 166).L(64, 180).L(50, 176).L(56, 152).Z()
star = poly(*[(100 + (9 if i % 2 == 0 else 4) * __import__('math').sin(i * 3.14159 / 5), 182 - (9 if i % 2 == 0 else 4) * __import__('math').cos(i * 3.14159 / 5)) for i in range(10)])
TOPS = {
    'tee': dict(parts=[(tee().d(), MN, True), (P().M(88, 143).Q(100, 155, 112, 143).Q(100, 150, 88, 143).Z().d(), MD, False), (star.d(), '#FFFFFF', False)], tags=['casual', 'summer'], color='#5BC0EB'),
    'tank': dict(parts=[(P().M(80, 140).Q(100, 158, 120, 140).L(126, 142).C(128, 160, 128, 190, 128, 210).Q(100, 216, 72, 210).C(72, 190, 72, 160, 74, 142).Z().d(), MN, True),
                        (P().M(72, 196).Q(100, 202, 128, 196).L(128, 204).Q(100, 210, 72, 204).Z().d(), ML, False)], tags=['summer', 'casual'], color='#F7A1C4'),
    'crop': dict(parts=[(P().M(76, 140).Q(100, 152, 124, 140).L(128, 190).Q(100, 196, 72, 190).Z().d(), MN, True),
                        (sym(ellipse(68, 152, 13, 11)).d(), ML, True),
                        (P().M(72, 184).Q(100, 190, 128, 184).L(128, 190).Q(100, 196, 72, 190).Z().d(), MD, False)], tags=['party', 'summer'], color='#C77DFF'),
    'sweater': dict(parts=[(P().M(74, 140).Q(100, 148, 126, 140).C(140, 146, 146, 170, 148, 200).C(150, 222, 150, 236, 150, 244).L(138, 246).C(136, 226, 134, 200, 130, 176).C(130, 196, 130, 210, 130, 222).Q(100, 228, 70, 222).C(70, 210, 70, 196, 70, 176).C(66, 200, 64, 226, 62, 246).L(50, 244).C(50, 236, 50, 222, 52, 200).C(54, 170, 60, 146, 74, 140).Z().d(), MN, True),
                           (P().M(70, 214).Q(100, 222, 130, 214).L(130, 224).Q(100, 232, 70, 224).Z().d(), MD, False),
                           (sym(P().M(50, 236).L(62, 238).L(62, 246).L(50, 244).Z()).d(), MD, False),
                           (P().M(86, 142).Q(100, 152, 114, 142).L(116, 146).Q(100, 158, 84, 146).Z().d(), MD, False)], tags=['winter', 'casual'], color='#E76F51'),
    'blouse': dict(parts=[(tee().d(), MN, True),
                          (sym(P().M(84, 141).L(100, 152).L(96, 160).L(80, 148).Z()).d(), '#FFFFFF', True),
                          (join(*[ellipse(100, 166 + 14 * i, 2.2, 2.2) for i in range(3)]).d(), '#FFFFFF', False)], tags=['school', 'casual'], color='#A8DADC'),
    'jacket': dict(parts=[(P().M(74, 140).Q(100, 148, 126, 140).C(140, 146, 146, 170, 148, 200).C(150, 222, 150, 236, 150, 244).L(138, 246).C(136, 226, 134, 200, 130, 176).L(132, 226).L(104, 230).L(100, 160).L(96, 230).L(68, 226).L(70, 176).C(66, 200, 64, 226, 62, 246).L(50, 244).C(50, 236, 50, 222, 52, 200).C(54, 170, 60, 146, 74, 140).Z().d(), MN, True),
                          (sym(P().M(86, 140).L(100, 160).L(92, 172).L(80, 150).Z()).d(), MD, True),
                          (join(*[ellipse(108, 186 + 16 * i, 2.5, 2.5) for i in range(3)]).d(), '#F7C948', False)], tags=['winter', 'school'], color='#264653'),
}
BOTTOMS = {
    'mini': dict(parts=[(P().M(74, 204).Q(100, 212, 126, 204).L(138, 256).Q(100, 266, 62, 256).Z().d(), MN, True),
                        (join(P().M(88, 212).L(84, 262).L(86, 262).L(90, 212).Z(), P().M(112, 212).L(116, 262).L(114, 262).L(110, 212).Z(), P().M(99, 213).L(99, 264).L(101, 264).L(101, 213).Z()).d(), MD, False)], tags=['school', 'party', 'casual'], color='#E63946'),
    'long': dict(parts=[(P().M(74, 204).Q(100, 212, 126, 204).C(134, 250, 142, 300, 146, 340).Q(100, 352, 54, 340).C(58, 300, 66, 250, 74, 204).Z().d(), MN, True),
                        (P().M(54, 330).Q(100, 342, 146, 330).L(146, 340).Q(100, 352, 54, 340).Z().d(), MD, False)], tags=['fancy', 'casual'], color='#9D4EDD'),
    'jeans': dict(parts=[(P().M(73, 204).Q(100, 210, 127, 204).L(127, 240).C(122, 290, 116, 330, 114, 366).L(101, 366).L(100, 252).L(99, 366).L(86, 366).C(84, 330, 78, 290, 73, 240).Z().d(), MN, True),
                         (P().M(73, 204).Q(100, 210, 127, 204).L(127, 212).Q(100, 218, 73, 212).Z().d(), MD, False),
                         (ellipse(100, 214, 2.5, 2.5).d(), '#F7C948', False)], tags=['casual', 'winter', 'school'], color='#3A6EA5'),
    'shorts': dict(parts=[(P().M(73, 204).Q(100, 210, 127, 204).L(130, 256).L(103, 258).L(100, 244).L(97, 258).L(70, 256).Z().d(), MN, True),
                          (P().M(73, 204).Q(100, 210, 127, 204).L(127, 211).Q(100, 217, 73, 211).Z().d(), MD, False)], tags=['summer', 'casual'], color='#F4A261'),
    'tutu': dict(parts=[(P().M(76, 204).Q(100, 212, 124, 204).L(146, 252).Q(136, 260, 126, 254).Q(114, 264, 102, 256).Q(90, 264, 78, 254).Q(66, 260, 54, 252).Z().d(), ML, True),
                        (P().M(74, 204).Q(100, 212, 126, 204).L(150, 244).Q(138, 252, 128, 246).Q(118, 256, 106, 248).Q(96, 258, 86, 248).Q(74, 256, 64, 246).Q(56, 252, 50, 244).Z().d(), MN, True)], tags=['party', 'fancy'], color='#FF8FAB'),
}
bodice = P().M(78, 140).Q(100, 154, 122, 140).L(128, 146).C(128, 170, 126, 190, 124, 204)
DRESSES = {
    'party': dict(parts=[(P().M(78, 140).Q(100, 154, 122, 140).L(128, 146).C(128, 170, 126, 190, 124, 204).C(136, 226, 146, 250, 152, 276).Q(100, 292, 48, 276).C(54, 250, 64, 226, 76, 204).C(74, 190, 72, 170, 72, 146).Z().d(), MN, True),
                         (P().M(76, 198).Q(100, 206, 124, 198).L(124, 208).Q(100, 216, 76, 208).Z().d(), MD, True),
                         (join(P().M(100, 204).L(86, 196).L(86, 214).Z(), P().M(100, 204).L(114, 196).L(114, 214).Z()).d(), MD, True),
                         (join(*[ellipse(x, y, 1.6, 1.6) for x, y in [(70, 240), (90, 250), (112, 236), (128, 258), (80, 268), (104, 272), (138, 270), (62, 262)]]).d(), '#CCFFFFFF', False)], tags=['party', 'fancy'], color='#E5383B'),
    'gown': dict(parts=[(P().M(78, 140).Q(100, 154, 122, 140).L(128, 146).C(128, 170, 126, 190, 124, 204).C(136, 250, 156, 320, 162, 372).Q(100, 386, 38, 372).C(44, 320, 64, 250, 76, 204).C(74, 190, 72, 170, 72, 146).Z().d(), MN, True),
                        (join(P().M(100, 214).C(104, 260, 108, 320, 110, 378).L(106, 378).C(104, 320, 100, 260, 98, 214).Z(), P().M(86, 214).C(80, 260, 72, 320, 66, 376).L(63, 375).C(70, 320, 78, 260, 84, 214).Z(), P().M(114, 214).C(120, 260, 128, 320, 134, 376).L(137, 375).C(130, 320, 122, 260, 116, 214).Z()).d(), MD, False),
                        (P().M(78, 140).Q(100, 154, 122, 140).L(124, 150).Q(100, 164, 76, 150).Z().d(), '#F7C948', True)], tags=['fancy'], color='#4361EE'),
    'sundress': dict(parts=[(sym(P().M(82, 140).L(87, 140).L(89, 152).L(84, 152).Z()).d(), MN, True),
                            (P().M(76, 150).Q(100, 160, 124, 150).C(126, 170, 126, 188, 124, 200).C(134, 240, 140, 270, 144, 300).Q(100, 312, 56, 300).C(60, 270, 66, 240, 76, 200).C(74, 188, 74, 170, 76, 150).Z().d(), MN, True),
                            (join(*[ellipse(x, y, 4, 4) for x, y in [(80, 230), (110, 246), (92, 276), (126, 282), (68, 286), (118, 214)]]).d(), '#FFFFFF', False),
                            (join(*[ellipse(x, y, 1.6, 1.6) for x, y in [(80, 230), (110, 246), (92, 276), (126, 282), (68, 286), (118, 214)]]).d(), '#F7C948', False)], tags=['summer', 'casual'], color='#FFD166'),
    'ball': dict(parts=[(P().M(78, 140).Q(100, 154, 122, 140).L(128, 146).C(128, 170, 126, 190, 124, 204).C(150, 230, 170, 300, 174, 374).Q(100, 392, 26, 374).C(30, 300, 50, 230, 76, 204).C(74, 190, 72, 170, 72, 146).Z().d(), MN, True),
                        (P().M(76, 204).C(96, 230, 104, 230, 124, 204).C(150, 240, 160, 290, 150, 330).Q(100, 300, 50, 330).C(40, 290, 50, 240, 76, 204).Z().d(), ML, True),
                        (sym(ellipse(68, 150, 15, 12)).d(), ML, True),
                        (join(*[ellipse(x, y, 1.8, 1.8) for x, y in [(60, 300), (80, 340), (120, 340), (140, 300), (100, 360), (50, 356), (150, 356)]]).d(), '#DDFFFFFF', False)], tags=['fancy', 'party'], color='#F2A7C3'),
}
def shoe_pair(p): return sym(p).d()
SHOES = {
    'sneakers': dict(parts=[(shoe_pair(P().M(80, 362).L(97, 362).C(99, 368, 102, 372, 102, 376).L(78, 377).C(77, 372, 78, 366, 80, 362).Z()), MN, True),
                            (shoe_pair(rect(77, 375, 103, 380)), '#FFFFFF', True)], tags=['casual', 'school', 'summer'], color='#06D6A0'),
    'heels': dict(parts=[(shoe_pair(P().M(81, 360).L(97, 360).C(100, 368, 104, 374, 105, 379).L(84, 379).C(82, 372, 81, 366, 81, 360).Z()), MN, True),
                         (shoe_pair(rect(82, 379, 86, 388)), MD, True)], tags=['party', 'fancy'], color='#D00000'),
    'boots': dict(parts=[(shoe_pair(P().M(80, 300).L(99, 300).C(98, 330, 98, 350, 98, 364).C(101, 368, 104, 373, 104, 379).L(78, 379).C(78, 350, 80, 330, 80, 300).Z()), MN, True),
                         (shoe_pair(rect(79, 298, 100, 308)), MD, True)], tags=['winter', 'school'], color='#7F5539'),
    'sandals': dict(parts=[(shoe_pair(rect(80, 373, 101, 379)), MD, True), (shoe_pair(rect(83, 363, 97, 367)), MN, True),
                           (shoe_pair(rect(84, 355, 96, 358)), MN, True)], tags=['summer'], color='#FFB703'),
}
HEAD = {
    'none': dict(parts=[], tags=[], color='#E84A8A'),
    'headband': dict(parts=[(P().M(58, 64).C(62, 34, 84, 24, 100, 24).C(116, 24, 138, 34, 142, 64).L(136, 66).C(130, 40, 114, 32, 100, 32).C(86, 32, 70, 40, 64, 66).Z().d(), MN, True),
                            (join(P().M(124, 38).L(112, 28).L(112, 48).Z(), P().M(124, 38).L(136, 28).L(136, 48).Z()).d(), MN, True), (ellipse(124, 38, 4, 4).d(), MD, True)], tags=['school', 'casual', 'party'], color='#FF4D6D'),
    'crown': dict(parts=[(poly((72, 40), (76, 14), (88, 28), (100, 6), (112, 28), (124, 14), (128, 40)).d(), '#F7C948', True),
                         (join(ellipse(100, 30, 4, 4), ellipse(84, 34, 3, 3), ellipse(116, 34, 3, 3)).d(), MN, True)], tags=['fancy', 'party'], color='#E5383B'),
    'sunhat': dict(parts=[(ellipse(100, 44, 66, 14).d(), MN, True), (P().M(70, 44).C(70, 6, 130, 6, 130, 44).Z().d(), MN, True),
                          (P().M(70, 36).Q(100, 30, 130, 36).L(130, 44).Q(100, 40, 70, 44).Z().d(), MD, True)], tags=['summer'], color='#F1DCA7'),
    'beanie': dict(parts=[(P().M(58, 62).C(56, 24, 80, 14, 100, 14).C(120, 14, 144, 24, 142, 62).Z().d(), MN, True),
                          (P().M(56, 54).Q(100, 46, 144, 54).L(144, 66).Q(100, 58, 56, 66).Z().d(), MD, True),
                          (ellipse(100, 12, 10, 10).d(), '#FFFFFF', True)], tags=['winter'], color='#2A9D8F'),
    'flower': dict(parts=[(join(*[ellipse(128 + 8 * __import__('math').cos(a), 48 + 8 * __import__('math').sin(a), 6, 6) for a in [i * 1.2566 for i in range(5)]]).d(), MN, True),
                          (ellipse(128, 48, 4.5, 4.5).d(), '#FFD166', True)], tags=['summer', 'casual'], color='#FF70A6'),
}
EXTRA = {
    'none': dict(parts=[], tags=[], color='#E84A8A'),
    'necklace': dict(parts=[(P().M(86, 140).Q(100, 162, 114, 140).L(114, 143).Q(100, 166, 86, 143).Z().d(), '#F7C948', False), (ellipse(100, 160, 4, 5).d(), MN, True)], tags=['party', 'fancy'], color='#4CC9F0'),
    'sunglasses': dict(parts=[(join(ellipse(85, 87, 12, 9), ellipse(115, 87, 12, 9)).d(), '#E6111111', True), (rect(96, 84, 104, 87).d(), '#111111', False),
                              (sym(P().M(78, 82).L(84, 82).L(80, 88).Z()).d(), '#66FFFFFF', False)], tags=['summer'], color='#111111', fixed=True),
    'bag': dict(parts=[(P().M(146, 246).C(146, 228, 166, 228, 166, 246).L(162, 246).C(162, 234, 150, 234, 150, 246).Z().d(), MD, True),
                       (P().M(140, 246).L(172, 246).L(176, 282).L(136, 282).Z().d(), MN, True), (rect(152, 254, 160, 260).d(), '#F7C948', True)], tags=['school', 'casual', 'party'], color='#B5179E'),
    'scarf': dict(parts=[(P().M(80, 134).Q(100, 150, 120, 134).L(124, 146).Q(100, 164, 76, 146).Z().d(), MN, True),
                         (P().M(108, 150).L(118, 150).L(122, 196).L(110, 196).Z().d(), MN, True),
                         (P().M(110, 190).L(122, 190).L(122, 196).L(110, 196).Z().d(), MD, False)], tags=['winter'], color='#E63946'),
}

CATS = [('top', TOPS), ('bottom', BOTTOMS), ('dress', DRESSES), ('shoes', SHOES), ('head', HEAD), ('extra', EXTRA)]

# ---------------------------------------------------------------- On izleme
def outfit(hair, top=None, bottom=None, dress=None, shoes=None, head=None, extra=None, colors=None):
    c = dict(main='#E84A8A', skin='#F6D2B8', hair='#6B3E26'); c.update(colors or {})
    parts = list(HAIR[hair]['back']) + BODY + BASE
    def add(cat, k):
        if k: parts.extend(cat[k]['parts'])
    add(BOTTOMS, bottom); add(TOPS, top); add(DRESSES, dress); add(SHOES, shoes)
    parts += HANDS + FACE
    if extra and extra != 'bag': add(EXTRA, extra)
    parts += HAIR[hair]['front']
    add(HEAD, head)
    if extra == 'bag': add(EXTRA, extra)
    return parts

if __name__ == "__main__" and "--preview" in sys.argv:
    from PIL import Image
    looks = [
        dict(hair='long', top='tee', bottom='jeans', shoes='sneakers', head='headband'),
        dict(hair='ponytail', dress='party', shoes='heels', extra='necklace'),
        dict(hair='curly', top='sweater', bottom='long', shoes='boots', head='beanie', extra='scarf'),
        dict(hair='bob', top='tank', bottom='shorts', shoes='sandals', head='sunhat', extra='sunglasses'),
        dict(hair='buns', dress='ball', shoes='heels', head='crown'),
        dict(hair='braids', top='jacket', bottom='mini', shoes='boots', extra='bag'),
        dict(hair='long', dress='gown', shoes='heels', head='flower'),
        dict(hair='bob', top='crop', bottom='tutu', shoes='sneakers', head='flower'),
        dict(hair='curly', dress='sundress', shoes='sandals', head='sunhat'),
        dict(hair='ponytail', top='blouse', bottom='mini', shoes='sneakers', extra='bag'),
    ]
    skins = ['#F6D2B8', '#E8B48F', '#C68642', '#8D5524', '#FCE3D3']
    hairs = ['#6B3E26', '#F2C14E', '#1B1B1B', '#C0392B', '#8E44AD']
    imgs = []
    for i, lk in enumerate(looks):
        cols = {}
        lk = dict(lk)
        parts = []
        # her kategoride kendi rengi
        c = dict(skin=skins[i % 5], hair=hairs[i % 5])
        out = list(HAIR[lk['hair']]['back']) + BODY + BASE
        def add(cat, k):
            if k:
                col = cat[k]['color']
                for d_, slot, st in cat[k]['parts']:
                    out.append((d_, slot.replace('main', '@' + col) if slot.startswith('main') else slot, st))
        add(BOTTOMS, lk.get('bottom')); add(TOPS, lk.get('top')); add(DRESSES, lk.get('dress')); add(SHOES, lk.get('shoes'))
        out += HANDS + FACE
        if lk.get('extra') and lk['extra'] != 'bag': add(EXTRA, lk['extra'])
        out += HAIR[lk['hair']]['front']; add(HEAD, lk.get('head'))
        if lk.get('extra') == 'bag': add(EXTRA, 'bag')
        # '@#hex' + D/L
        fixed = []
        for d_, slot, st in out:
            if slot.startswith('@'):
                base = slot[1:8]; suf = slot[8:]
                slot = shade(base, 0.78) if suf == 'D' else shade(base, 1.35) if suf == 'L' else base
            fixed.append((d_, slot, st))
        render(svg(fixed, c, bg='#FDF0F6', scale=1), f"/tmp/dress_{i}.png")
        imgs.append(Image.open(f"/tmp/dress_{i}.png"))
    sheet = Image.new("RGB", (200 * 5, 400 * 2), "white")
    for i, im in enumerate(imgs): sheet.paste(im, ((i % 5) * 200, (i // 5) * 400))
    sheet.save("/tmp/dress_sheet.png")

if __name__ == "__main__" and "--kotlin" in sys.argv:
    out = ["package com.eomer91.minigamesworld.ui.games.dressup", "",
           "import com.eomer91.minigamesworld.ui.games.common.VP", "",
           "/** tools/vector/dressup.py ile uretildi (viewBox 200x400). Elle duzenlemeyin; betigi degistirip yeniden uretin. */",
           "object DressUpArt {"]
    out.append(f"    val body = {kt_parts(BODY, '    ')}")
    out.append(f"    val base = {kt_parts(BASE, '    ')}")
    out.append(f"    val hands = {kt_parts(HANDS, '    ')}")
    out.append(f"    val face = {kt_parts(FACE, '    ')}")
    out.append("    val hairBack: Map<String, List<VP>> = mapOf(")
    for k, v in HAIR.items(): out.append(f'        "{k}" to {kt_parts(v["back"], "        ")},')
    out.append("    )")
    out.append("    val hairFront: Map<String, List<VP>> = mapOf(")
    for k, v in HAIR.items(): out.append(f'        "{k}" to {kt_parts(v["front"], "        ")},')
    out.append("    )")
    for cat, items in CATS:
        out.append(f"    val {cat}: List<DressItem> = listOf(")
        for k, v in items.items():
            tags = ", ".join(f'"{t}"' for t in v["tags"])
            col = v["color"].lstrip("#")
            out.append(f'        DressItem("{k}", 0xFF{col}, setOf({tags}), {kt_parts(v["parts"], "        ")}),')
        out.append("    )")
    out.append("}")
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../app/src/main/java/com/eomer91/minigamesworld/ui/games/dressup/DressUpArt.kt")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "w").write("\n".join(out) + "\n")
    print("wrote", path, sum(len(l) for l in out))
