"""Blok Yagmuru kapak gorseli (960x540)."""
import os, random
from PIL import Image, ImageDraw, ImageFilter
here = os.path.dirname(os.path.abspath(__file__))
S = 2; W, H = 960*S, 540*S
random.seed(3)
img = Image.new("RGB", (W, H))
d = ImageDraw.Draw(img)
for y in range(H):
    k = y / H
    d.line([(0, y), (W, y)], fill=(int(12+20*k), int(18+10*k), int(48+40*k)))
COL = {'I':(34,211,238),'O':(250,204,21),'T':(168,85,247),'S':(34,197,94),'Z':(239,68,68),'J':(59,130,246),'L':(249,115,22)}
SH = {'I':[(0,0),(0,1),(0,2),(0,3)],'O':[(0,0),(0,1),(1,0),(1,1)],'T':[(0,1),(1,0),(1,1),(1,2)],'S':[(0,1),(0,2),(1,0),(1,1)],
      'Z':[(0,0),(0,1),(1,1),(1,2)],'J':[(0,0),(1,0),(1,1),(1,2)],'L':[(0,2),(1,0),(1,1),(1,2)]}
glow = Image.new("RGB", (W, H)); gd = ImageDraw.Draw(glow)
def block(dr, x, y, s, c, alpha=1.0):
    c = tuple(int(v*alpha) for v in c)
    p = s*0.06
    dr.rounded_rectangle([x+p, y+p, x+s-p, y+s-p], radius=s*0.15, fill=c)
    hl = tuple(min(255, int(v+(255-v)*0.45)) for v in c); sh = tuple(int(v*0.65) for v in c)
    dr.rounded_rectangle([x+2*p, y+2*p, x+s-2*p, y+2*p+s*0.2], radius=s*0.08, fill=hl)
    dr.rounded_rectangle([x+2*p, y+s*0.72, x+s-2*p, y+s-2*p], radius=s*0.08, fill=sh)
def piece(dr, t, x, y, s, rot=0, alpha=1.0):
    cells = SH[t]
    for _ in range(rot): cells = [(c, 3-r) for r, c in cells]
    mr = min(r for r, c in cells); mc = min(c for r, c in cells)
    for r, c in cells: block(dr, x+(c-mc)*s, y+(r-mr)*s, s, COL[t], alpha)
# Yagmur cizgileri
for _ in range(220):
    x = random.randrange(W); y = random.randrange(H); l = random.randrange(30, 120)*S//2
    c = random.choice(list(COL.values()))
    d.line([(x, y), (x, y+l)], fill=tuple(int(v*0.35) for v in c), width=S*2)
# Kuyu
cs = 24*S; bw, bh = 10*cs, 20*cs
bx = (W-bw)//2; by = (H-bh)//2 + 4*S
gd.rounded_rectangle([bx-20*S, by-20*S, bx+bw+20*S, by+bh+20*S], radius=30*S, fill=(60, 90, 255))
glow = glow.filter(ImageFilter.GaussianBlur(40*S))
img = Image.blend(img, Image.eval(glow, lambda v: v), 0.0)
img = Image.composite(img, img, Image.new("L", (W, H), 255))
from PIL import ImageChops
img = ImageChops.add(img, glow.point(lambda v: int(v*0.55)))
d = ImageDraw.Draw(img)
d.rounded_rectangle([bx-6*S, by-6*S, bx+bw+6*S, by+bh+6*S], radius=14*S, fill=(10, 16, 34), outline=(120, 160, 255), width=3*S)
for c in range(1, 10): d.line([(bx+c*cs, by), (bx+c*cs, by+bh)], fill=(26, 37, 64), width=S)
for r in range(1, 20): d.line([(bx, by+r*cs), (bx+bw, by+r*cs)], fill=(26, 37, 64), width=S)
stack = ["..........","..........","..........","..........","..........","..........","..........","..........","..........",
         "..........","..........","..........","........OO","J.......OO","JJJ...LTTT","ZZ.SSLLLTI","IZZSSIJJ.I","IOOLLIJ.ZI","IOOLTTJZZI","SSLLTTTZ.I"]
m = {'I':'I','O':'O','T':'T','S':'S','Z':'Z','J':'J','L':'L'}
for r, row in enumerate(stack):
    for c, ch in enumerate(row):
        if ch != '.': block(d, bx+c*cs, by+r*cs, cs, COL[ch])
# Dusen parca + hayalet
piece(d, 'T', bx+3*cs, by+4*cs, cs, rot=2)
for (r, c) in [(10,4),(10,5),(10,6),(11,5)]:
    d.rounded_rectangle([bx+c*cs+3*S, by+r*cs+3*S, bx+(c+1)*cs-3*S, by+(r+1)*cs-3*S], radius=4*S, outline=(168,85,247), width=2*S)
# Yan parcalar
for (t, x, y, s, rot, a) in [('L',110,70,46,0,1),('I',150,330,40,1,1),('S',740,90,44,0,1),('O',780,330,48,0,1),('J',40,220,30,1,0.6),('Z',860,240,30,0,0.6),('T',250,430,34,2,0.8),('I',600,40,26,0,0.5)]:
    piece(d, t, x*S, y*S, s*S, rot, a)
img = img.resize((960, 540), Image.LANCZOS)
img.save(os.path.join(here, "../../app/src/main/res/drawable-nodpi/game_block_rain.webp"), "WEBP", quality=90)
img.save("/tmp/block_rain_cover.png")
