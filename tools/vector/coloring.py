"""Boyama sayfalari: siyah cizgi resimler (512 viewBox) -> res/drawable-nodpi/coloring_*.png (1024 px).
Her bolge kapali olmali; uygulama dokunulan bolgeyi doldurur."""
import os, math, subprocess
here = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(here, "../../app/src/main/res/drawable-nodpi")

def svg(body):
    return ('<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 512 512">'
            '<rect width="512" height="512" fill="#FFFFFF"/>'
            '<g fill="none" stroke="#000000" stroke-width="6" stroke-linejoin="round" stroke-linecap="round">' + body + '</g></svg>')

def circle(cx, cy, r): return f'<circle cx="{cx}" cy="{cy}" r="{r}"/>'
def ell(cx, cy, rx, ry): return f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}"/>'
def path(d): return f'<path d="{d}"/>'
def rect(x, y, w, h, r=0): return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}"/>'
def line(x1, y1, x2, y2): return f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}"/>'
def star(cx, cy, R, r, n=5):
    pts = []
    for i in range(n * 2):
        rad = R if i % 2 == 0 else r
        a = -math.pi / 2 + i * math.pi / n
        pts.append(f"{cx + rad * math.cos(a):.1f},{cy + rad * math.sin(a):.1f}")
    return f'<polygon points="{" ".join(pts)}"/>'
def border(): return rect(8, 8, 496, 496, 24)

PAGES = {}
# 1 Ev, gunes, agac, cicek
PAGES['house'] = border() + line(8, 400, 504, 400) + \
    rect(130, 230, 200, 170) + path("M110 240 L230 140 L350 240 Z") + rect(205, 310, 50, 90, 6) + circle(245, 358, 4) + \
    rect(150, 260, 45, 40) + line(172, 260, 172, 300) + line(150, 280, 195, 280) + rect(265, 260, 45, 40) + line(287, 260, 287, 300) + line(265, 280, 310, 280) + \
    rect(280, 160, 26, 50) + circle(430, 80, 38) + "".join(line(430 + 48 * math.cos(a), 80 + 48 * math.sin(a), 430 + 66 * math.cos(a), 80 + 66 * math.sin(a)) for a in [i * math.pi / 4 for i in range(8)]) + \
    rect(400, 300, 22, 100) + circle(411, 270, 48) + ell(80, 90, 50, 22) + ell(120, 76, 34, 22) + \
    circle(60, 430, 14) + circle(60, 430, 5) + line(60, 444, 60, 480) + circle(120, 440, 12) + circle(120, 440, 4) + line(120, 452, 120, 480)
# 2 Kelebek
PAGES['butterfly'] = border() + ell(256, 270, 16, 110) + circle(256, 150, 24) + path("M246 130 Q220 70 200 60") + path("M266 130 Q292 70 312 60") + circle(198, 58, 8) + circle(314, 58, 8) + \
    path("M240 200 C150 80 40 110 70 220 C90 280 190 270 240 250 Z") + path("M272 200 C362 80 472 110 442 220 C422 280 322 270 272 250 Z") + \
    path("M240 270 C160 270 90 330 130 390 C170 440 230 380 242 320 Z") + path("M272 270 C352 270 422 330 382 390 C342 440 282 380 270 320 Z") + \
    circle(140, 190, 30) + circle(372, 190, 30) + circle(170, 360, 22) + circle(342, 360, 22) + circle(110, 220, 12) + circle(402, 220, 12)
# 3 Balik
PAGES['fish'] = border() + path("M90 260 C150 150 320 140 380 260 C320 380 150 370 90 260 Z") + path("M380 260 L470 190 L450 260 L470 330 Z") + \
    circle(160, 240, 18) + circle(165, 240, 6) + path("M115 285 Q135 295 150 285") + path("M220 160 Q260 110 310 170") + path("M230 340 Q260 390 290 350") + \
    path("M220 200 Q240 260 220 320") + path("M270 190 Q290 260 270 330") + path("M320 205 Q335 260 320 315") + \
    circle(80, 150, 16) + circle(110, 100, 11) + circle(70, 70, 8) + path("M8 440 Q60 410 110 440 T210 440 T310 440 T410 440 T504 440") + \
    path("M420 480 Q430 420 410 380 M440 480 Q460 430 450 400")
# 4 Cicek saksi
PAGES['flower'] = border() + "".join(ell(256 + 62 * math.cos(a), 180 + 62 * math.sin(a), 40, 40) for a in [i * 2 * math.pi / 8 for i in range(8)]) + circle(256, 180, 44) + \
    path("M256 224 L256 380") + path("M256 300 C210 270 180 290 170 320 C210 330 240 320 256 300 Z") + path("M256 330 C300 300 330 320 340 350 C300 360 270 350 256 330 Z") + \
    path("M170 380 L342 380 L320 490 L192 490 Z") + rect(160, 370, 192, 26, 8) + circle(80, 90, 26) + circle(440, 110, 20)
# 5 Kek (cupcake)
PAGES['cupcake'] = border() + path("M150 280 L362 280 L330 460 L182 460 Z") + "".join(line(182 + i * 30, 280, 192 + i * 24, 460) for i in range(1, 6)) + \
    path("M130 290 C110 240 150 200 190 210 C190 160 250 140 280 180 C320 150 380 180 370 230 C410 240 400 300 380 290 Z") + \
    path("M175 250 Q256 230 340 250") + circle(256, 135, 26) + path("M256 109 Q270 80 300 70") + \
    rect(200, 170, 12, 26, 4) + rect(300, 200, 12, 26, 4) + rect(240, 220, 26, 12, 4) + star(90, 100, 30, 13) + star(430, 90, 26, 11) + circle(440, 400, 18)
# 6 Araba
PAGES['car'] = border() + line(8, 420, 504, 420) + path("M70 360 L70 300 Q75 270 110 265 L160 260 L210 200 Q220 190 240 190 L340 190 Q360 190 372 205 L420 262 Q450 268 455 300 L455 360 Z") + \
    path("M220 260 L245 210 L295 210 L295 260 Z") + path("M310 260 L310 210 L345 210 L380 260 Z") + circle(150, 365, 42) + circle(150, 365, 18) + circle(380, 365, 42) + circle(380, 365, 18) + \
    rect(80, 300, 30, 18, 4) + rect(430, 300, 20, 16, 4) + line(300, 270, 300, 340) + ell(80, 80, 55, 25) + ell(380, 70, 60, 26) + circle(250, 80, 30)
# 7 Kedi
PAGES['cat'] = border() + path("M150 210 L130 90 L215 160 Q256 145 297 160 L382 90 L362 210 Q400 290 340 350 Q256 410 172 350 Q112 290 150 210 Z") + \
    path("M150 130 L175 175 M362 130 L337 175") + ell(205, 250, 26, 32) + ell(307, 250, 26, 32) + ell(210, 255, 10, 16) + ell(302, 255, 10, 16) + \
    path("M242 300 L270 300 L256 316 Z") + path("M256 316 Q240 340 222 330 M256 316 Q272 340 290 330") + \
    line(150, 300, 70, 285) + line(150, 315, 70, 320) + line(362, 300, 442, 285) + line(362, 315, 442, 320) + \
    path("M180 380 Q256 420 332 380 L350 490 L162 490 Z") + path("M340 470 Q440 470 430 380 Q425 350 445 340")
# 8 Roket
PAGES['rocket'] = border() + path("M256 50 C320 110 330 220 315 330 L197 330 C182 220 192 110 256 50 Z") + circle(256, 190, 36) + circle(256, 190, 22) + \
    path("M197 260 L140 340 L150 380 L200 330 Z") + path("M315 260 L372 340 L362 380 L312 330 Z") + rect(210, 330, 92, 26, 6) + \
    path("M225 356 Q256 470 287 356 Z") + path("M240 356 Q256 420 272 356 Z") + line(225, 110, 287, 110) + \
    star(90, 120, 32, 14) + star(420, 200, 26, 11) + star(110, 380, 22, 9) + star(410, 420, 30, 13) + circle(420, 80, 30) + path("M400 70 Q420 60 440 75")

if __name__ == "__main__":
    from PIL import Image
    thumbs = []
    for name, body in PAGES.items():
        png = os.path.join(OUT, f"coloring_{name}.png")
        subprocess.run(["rsvg-convert", "-o", "/tmp/c.png"], input=svg(body).encode(), check=True)
        im = Image.open("/tmp/c.png").convert("L")
        im.save(png, optimize=True)
        thumbs.append(im.resize((256, 256)))
    sheet = Image.new("L", (256 * 4, 512), 255)
    for i, t in enumerate(thumbs): sheet.paste(t, ((i % 4) * 256, (i // 4) * 256))
    sheet.save("/tmp/coloring_sheet.png")
    print(list(PAGES))
