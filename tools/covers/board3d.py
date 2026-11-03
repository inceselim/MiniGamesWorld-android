"""Perspektif tahta kapaklari icin ortak yardimcilar (dama, satranc)."""
from PIL import Image, ImageDraw, ImageFilter
SS = 2
W, H = 960*SS, 540*SS

class Board3D:
    def __init__(self, cx=W/2, y_far=120*SS, y_near=520*SS, half_near=430*SS, depth=0.75):
        self.cx, self.y_far, self.y_near, self.half_near, self.depth = cx, y_far, y_near, half_near, depth
    def z(self, v):  # v: 0 (uzak) .. 8 (yakin)
        return 1 + self.depth*(8 - v)/8
    def p(self, u, v):
        z = self.z(v); z0 = self.z(0); z8 = self.z(8)
        t = (1/z - 1/z0) / (1/z8 - 1/z0)
        y = self.y_far + (self.y_near - self.y_far)*t
        x = self.cx + (u - 4)/4*self.half_near/z
        return (x, y)
    def scale(self, v):
        return self.half_near/4/self.z(v)

def background(top, bottom, glow=None):
    img = Image.new("RGB", (W, H)); d = ImageDraw.Draw(img)
    for y in range(H):
        k = y/H; d.line([(0, y), (W, y)], fill=tuple(int(top[i]+(bottom[i]-top[i])*k) for i in range(3)))
    if glow:
        g = Image.new("RGB", (W, H)); gd = ImageDraw.Draw(g)
        gd.ellipse([W*0.2, H*0.05, W*0.8, H*0.9], fill=glow)
        g = g.filter(ImageFilter.GaussianBlur(120*SS))
        from PIL import ImageChops
        img = ImageChops.add(img, g)
    return img

def draw_board(img, b, light, dark, frame):
    d = ImageDraw.Draw(img)
    # cerceve
    m = 0.35
    d.polygon([b.p(-m, -m), b.p(8+m, -m), b.p(8+m, 8+m), b.p(-m, 8+m)], fill=frame)
    x0, y0 = b.p(-m, 8+m); x1, y1 = b.p(8+m, 8+m)
    d.polygon([(x0, y0), (x1, y1), (x1, y1+22*SS), (x0, y0+22*SS)], fill=tuple(int(c*0.6) for c in frame))
    for v in range(8):
        for u in range(8):
            d.polygon([b.p(u, v), b.p(u+1, v), b.p(u+1, v+1), b.p(u, v+1)], fill=dark if (u+v) % 2 else light)

def disc(img, b, u, v, top, side, rim=None, height=0.22, radius=0.4, crown=False):
    d = ImageDraw.Draw(img)
    x, y = b.p(u+0.5, v+0.5); s = b.scale(v+0.5)
    rx = radius*s; ry = rx*0.55; h = height*s
    d.ellipse([x-rx, y-ry+h*0.15, x+rx, y+ry+h*0.15+6*SS], fill=(0, 0, 0))
    d.rectangle([x-rx, y-h, x+rx, y], fill=side)
    d.ellipse([x-rx, y-ry, x+rx, y+ry], fill=side)
    d.ellipse([x-rx, y-h-ry, x+rx, y-h+ry], fill=top)
    if rim: d.ellipse([x-rx*0.68, y-h-ry*0.68, x+rx*0.68, y-h+ry*0.68], outline=rim, width=max(1, int(3*SS*s/100)))
    if crown:
        cw = rx*0.9; ch = ry*1.2; bx = x-cw/2; by = y-h+ch*0.35; ty = by-ch
        d.polygon([(bx, by), (bx, ty+ch*0.3), (bx+cw*0.25, ty+ch*0.65), (bx+cw*0.5, ty), (bx+cw*0.75, ty+ch*0.65), (bx+cw, ty+ch*0.3), (bx+cw, by)], fill=(247, 201, 72), outline=(138, 97, 0))
    return x, y - h

def save(img, name):
    import os
    img = img.resize((960, 540), Image.LANCZOS)
    here = os.path.dirname(os.path.abspath(__file__))
    img.save(os.path.join(here, f"../../app/src/main/res/drawable-nodpi/{name}.webp"), "WEBP", quality=90)
    img.save(f"/tmp/{name}.png")
