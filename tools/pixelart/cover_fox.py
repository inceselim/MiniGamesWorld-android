"""Tilki Macerasi kapak gorseli + bolum on izlemesi (FoxLevels.kt'den okur)."""
import os, re, sys, math, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art import *
from PIL import Image, ImageDraw
here = os.path.dirname(os.path.abspath(__file__))
R = lambda a: a.render()
T = 16
src = open(os.path.join(here, "../../app/src/main/java/com/eomer91/minigamesworld/ui/games/fox/FoxLevels.kt")).read()
rows = re.findall(r'"([^"]+)"', src)
LEVELS = [rows[i:i+14] for i in range(0, len(rows), 14)]
SKY = [((94,183,240),(189,232,255)), ((18,15,30),(42,36,64)), ((42,15,26),(110,42,34))]
img_cache = {}
def S(key, f):
    if key not in img_cache: img_cache[key] = R(f)
    return img_cache[key]

def render(world, level, c0, cols, fox_at=None):
    lv = LEVELS[world*5+level]; H = len(lv)
    im = Image.new("RGBA", (cols*T, H*T))
    top, bot = SKY[world]; d = ImageDraw.Draw(im)
    for y in range(H*T):
        k = y/(H*T); d.line([(0,y),(cols*T,y)], fill=tuple(int(top[i]+(bot[i]-top[i])*k) for i in range(3))+(255,))
    if world == 0:
        for i in range(0, cols*T, 90): d.ellipse([i-30, 120, i+90, 260], fill=(143,203,155,255))
        for i in range(40, cols*T, 70): d.ellipse([i-20, 160, i+60, 260], fill=(95,174,110,255))
        for i in range(10, cols*T, 120): d.ellipse([i, 20, i+40, 34], fill=(255,255,255,230)); d.ellipse([i+10, 12, i+30, 28], fill=(255,255,255,230))
    def put(sp, x, y):
        im.alpha_composite(sp, (int(x), int(y)))
    for c in range(cols):
        cc = c0+c
        for r in range(H):
            ch = lv[r][cc]; x = c*T; y = r*T
            above = lv[r-1][cc] if r > 0 else '#'
            if ch == '#':
                k = ['dirt' if above == '#' else 'grass', 'cave' if above == '#' or r == 0 else 'caveTop', 'castle'][world]
                put(S('t'+k, tile(k)), x, y)
            elif ch in 'BUS=^L':
                put(S(ch, tile({'B':'brick','U':'used','S':'spring','=':'plat','^':'spikes','L':'lava'}[ch])), x, y)
            elif ch in '?H': put(S('q', tile('q')), x, y)
            elif ch == 'c': cimg = S('coin', coin()); put(cimg, x+(T-cimg.width)//2, y+(T-cimg.height)//2)
            elif ch == '*': put(S('star', star()), x, y)
            elif ch == 'K': cp = S('cp', checkpoint(False)); put(cp, x, y+T-cp.height)
            elif ch == 'F': fp = S('flag', flagpole()); put(fp, x, 12*T-fp.height)
            elif ch in 'ebh':
                sp = S(ch, {'e': snail(0), 'b': bee(0), 'h': hedgehog(0)}[ch]); put(sp, x-8, y+T-sp.height+2)
            elif ch == '@' and fox_at is None:
                sp = S('fox', fox(0)); put(sp, x-8, y+T-sp.height+2)
    if fox_at:
        sp = S('fox3', fox(3)); put(sp, fox_at[0]*T-8, fox_at[1]*T)
    return im

if __name__ == "__main__":
    # On izleme: her dunyanin ilk bolumunun basi
    prev = Image.new("RGBA", (40*T, 3*14*T))
    for w in range(3): prev.alpha_composite(render(w, 0, 0, 40), (0, w*14*T))
    prev.resize((prev.width*2, prev.height*2), Image.NEAREST).save(os.path.join(here, "fox_level_preview.png"))
    # Kapak: 1. dunya, sahne
    sc = render(0, 0, 14, 24, fox_at=(9, 6.3))
    sc = sc.crop((0, 14*T - 216, 24*T, 14*T))
    cover = sc.resize((960, 540), Image.NEAREST).convert("RGB")
    cover.save(os.path.join(here, "../../app/src/main/res/drawable-nodpi/game_fox_adventure.webp"), "WEBP", quality=90)
    cover.save(os.path.join(here, "fox_cover_preview.png"))
    print("ok")
