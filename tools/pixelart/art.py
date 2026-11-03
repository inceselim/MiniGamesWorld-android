"""Basit piksel art DSL: sekilleri doldur, sonra otomatik golgelendirme + dis hat."""
from PIL import Image
import math
# Malzemeler: (acik, orta, koyu)
M = {
 'leaf':  ((143,227,107),(63,163,77),(37,107,51)),
 'cloak': ((94,184,92),(46,125,70),(27,82,52)),
 'skin':  ((255,214,170),(240,184,140),(196,134,96)),
 'leather':((176,118,64),(139,90,43),(92,58,28)),
 'gold':  ((255,236,140),(247,208,70),(201,151,28)),
 'glow':  ((230,255,255),(111,224,255),(46,143,184)),
 'goblin':((140,214,120),(95,174,110),(52,118,74)),
 'red':   ((255,120,120),(224,72,72),(143,36,36)),
 'white': ((255,255,255),(232,232,236),(190,190,200)),
 'fur':   ((214,218,226),(163,169,179),(104,110,122)),
 'orc':   ((120,170,150),(78,130,112),(48,88,76)),
 'band':  ((110,130,220),(61,74,138),(40,48,96)),
 'bat':   ((198,150,240),(166,110,224),(94,47,138)),
 'batwing':((126,80,180),(94,47,138),(60,28,92)),
 'bark':  ((168,112,64),(124,78,40),(80,48,22)),
 'iron':  ((220,226,236),(160,168,180),(100,108,122)),
 'eye':   ((40,30,40),(25,20,30),(15,10,20)),
 'eyeY':  ((255,250,180),(250,220,80),(200,160,30)),
}
OUT=(28,24,42)
class Art:
    def __init__(s,w,h): s.w,s.h=w,h; s.g=[[None]*w for _ in range(h)]; s.flat=[[False]*w for _ in range(h)]; s.t=[[1]*w for _ in range(h)]
    def px(s,x,y,m,flat=False,tone=1):
        x,y=int(round(x)),int(round(y))
        if 0<=x<s.w and 0<=y<s.h: s.g[y][x]=m; s.flat[y][x]=flat; s.t[y][x]=tone
    @staticmethod
    def shade(nx,ny):
        d=nx*-0.55+ny*-0.83
        return 0 if d>0.35 else (2 if d<-0.25 else 1)
    def rect(s,x0,y0,x1,y1,m,flat=False):
        cx=(x0+x1)/2; cy=(y0+y1)/2; hw=max(0.5,(x1-x0)/2); hh=max(0.5,(y1-y0)/2)
        for y in range(y0,y1+1):
            for x in range(x0,x1+1): s.px(x,y,m,flat,s.shade((x-cx)/hw*0.8,(y-cy)/hh*0.8))
    def ell(s,cx,cy,rx,ry,m,flat=False):
        for y in range(int(cy-ry-1),int(cy+ry+2)):
            for x in range(int(cx-rx-1),int(cx+rx+2)):
                nx=(x-cx)/(rx+0.01); ny=(y-cy)/(ry+0.01)
                if nx*nx+ny*ny<=1.0: s.px(x,y,m,flat,s.shade(nx,ny))
    def poly(s,pts,m,flat=False):
        xs=[p[0] for p in pts]; ys=[p[1] for p in pts]
        cx=(min(xs)+max(xs))/2; cy=(min(ys)+max(ys))/2; hw=max(1,(max(xs)-min(xs))/2); hh=max(1,(max(ys)-min(ys))/2)
        for y in range(int(min(ys)),int(max(ys))+1):
            for x in range(int(min(xs)),int(max(xs))+1):
                if s._inside(x+0.5,y+0.5,pts): s.px(x,y,m,flat,s.shade((x-cx)/hw*0.8,(y-cy)/hh*0.8))
    def _inside(s,x,y,pts):
        c=False; j=len(pts)-1
        for i in range(len(pts)):
            xi,yi=pts[i]; xj,yj=pts[j]
            if (yi>y)!=(yj>y) and x<(xj-xi)*(y-yi)/(yj-yi+1e-9)+xi: c=not c
            j=i
        return c
    def line(s,x0,y0,x1,y1,m,flat=False):
        n=int(max(abs(x1-x0),abs(y1-y0)))+1
        for i in range(n+1):
            t=i/max(n,1); s.px(x0+(x1-x0)*t,y0+(y1-y0)*t,m,flat)
    def render(s,light=(-1,-1)):
        """Her piksel: malzemenin sinirina gore acik/orta/koyu ton; disa koyu hat."""
        img=Image.new("RGBA",(s.w,s.h),(0,0,0,0))
        def m(x,y): return s.g[y][x] if 0<=x<s.w and 0<=y<s.h else None
        for y in range(s.h):
            for x in range(s.w):
                mat=s.g[y][x]
                if mat is None: continue
                hi,mid,lo=M[mat]
                if s.flat[y][x]: c=mid
                else:
                    tone=s.t[y][x]
                    down=m(x,y+1)!=mat; right=m(x+1,y)!=mat; up=m(x,y-1)!=mat
                    if down or right: tone=2
                    elif up and tone>0: tone=max(0,tone-1)
                    # doku: yaprak ve kurk icin seyrek nokta
                    if mat in ('leaf','fur','bark','cloak') and tone==1 and (x*7+y*13)%9==0: tone=2
                    if mat in ('leaf',) and tone==1 and (x*5+y*11)%13==0: tone=0
                    c=(hi,mid,lo)[tone]
                img.putpixel((x,y),c+(255,))
        # dis hat
        out=img.copy()
        for y in range(s.h):
            for x in range(s.w):
                if s.g[y][x] is None:
                    for dx,dy in ((1,0),(-1,0),(0,1),(0,-1)):
                        if m(x+dx,y+dy) is not None: out.putpixel((x,y),OUT+(255,)); break
        return out

def hero(frame=0):
    a=Art(32,32)
    # asa (arkada)
    a.line(24,5,24,29,'leather')
    a.ell(24,5,2.2,2.2,'glow')
    # pelerin arka
    a.poly([(9,12),(20,12),(22,27),(7,27)],'cloak')
    # bacaklar
    lx = (0,0) if frame==0 else (1,-1)
    a.rect(11+lx[0],24,13+lx[0],28,'leather'); a.rect(16+lx[1],24,18+lx[1],28,'leather')
    a.rect(10+lx[0],28,13+lx[0],29,'leather',flat=False); a.rect(16+lx[1],28,19+lx[1],29,'leather')
    # govde / tunik
    a.poly([(10,14),(19,14),(21,25),(8,25)],'leaf')
    a.rect(9,20,20,21,'leather')          # kemer
    a.rect(14,20,15,21,'gold',flat=True)  # toka
    # kollar
    a.rect(20,15,22,20,'leaf'); a.ell(22.5,20.5,1.6,1.4,'skin')  # asayi tutan el
    a.rect(7,15,9,20,'leaf'); a.ell(8,21,1.4,1.3,'skin')
    # bas + baslik
    a.ell(14.5,9,6.2,6.0,'cloak')
    a.ell(16,10,3.8,3.6,'skin')
    a.rect(12,5,19,7,'cloak')
    # baslik ucu
    a.poly([(9,6),(13,2),(14,6)],'cloak')
    # gozler
    a.px(16,10,'eye',True); a.px(18,10,'eye',True)
    return a

def goblin(frame=0):
    a=Art(32,32)
    # sopa
    a.line(25,12,22,24,'bark'); a.ell(25.5,11,2.2,2.6,'bark')
    a.px(26,10,'iron',True); a.px(24,12,'iron',True)
    # bacaklar
    o=0 if frame==0 else 1
    a.rect(11,24,13,29-o,'goblin'); a.rect(17,24,19,28+o,'goblin')
    a.rect(10,28-o,13,29-o,'leather'); a.rect(17,28+o,20,29+o,'leather')
    # govde
    a.ell(15,20,6,5.5,'goblin')
    a.poly([(9,20),(21,20),(20,26),(10,26)],'leather')
    # kollar
    a.rect(20,17,22,22,'goblin'); a.rect(7,17,9,23,'goblin')
    # bas
    a.ell(15,11,7,5.6,'goblin')
    # kulaklar
    a.poly([(8,10),(2,6),(5,12)],'goblin'); a.poly([(22,10),(28,6),(25,12)],'goblin')
    # gozler, agiz
    a.rect(11,10,12,11,'red',True); a.rect(17,10,18,11,'red',True)
    a.rect(12,14,18,14,'eye',True); a.px(13,13,'white',True); a.px(17,13,'white',True)
    return a

def wolf(frame=0):
    a=Art(32,32)
    o=0 if frame==0 else 1
    # kuyruk
    a.poly([(25,15),(31,10),(30,14),(27,18)],'fur')
    # govde
    a.ell(17,18,9,5,'fur')
    # bacaklar
    a.rect(10+o,21,12+o,28,'fur'); a.rect(14-o,22,16-o,28,'fur'); a.rect(20+o,21,22+o,28,'fur'); a.rect(24-o,21,26-o,27,'fur')
    # bas
    a.ell(8,14,5,4.2,'fur')
    a.poly([(1,15),(6,13),(7,18),(2,18)],'fur')   # burun
    a.poly([(6,7),(9,11),(5,11)],'fur'); a.poly([(10,7),(12,11),(9,11)],'fur')  # kulaklar
    a.px(1,15,'eye',True)
    a.rect(6,13,7,13,'eyeY',True)
    a.line(3,18,6,18,'white',True)
    return a

def orc(frame=0):
    a=Art(32,32)
    o=0 if frame==0 else 1
    a.rect(9,24,13,29-o,'orc'); a.rect(18,24,22,29+o,'orc')
    a.rect(8,28-o,13,29-o,'leather'); a.rect(18,28+o,23,29+o,'leather')
    a.ell(15.5,19,10,7,'orc')
    a.poly([(7,20),(24,20),(23,27),(8,27)],'leather')
    a.rect(7,21,24,22,'bark'); a.rect(14,21,17,22,'iron',True)
    # omuzluk
    a.ell(6,15,3.5,3,'iron'); a.ell(25,15,3.5,3,'iron')
    a.rect(3,16,6,23,'orc'); a.rect(25,16,28,23,'orc')
    # bas
    a.ell(15.5,9,6.5,5.5,'orc')
    a.rect(9,4,22,6,'band')
    a.rect(12,8,13,9,'red',True); a.rect(18,8,19,9,'red',True)
    a.rect(12,12,19,13,'eye',True); a.px(12,11,'white',True); a.px(19,11,'white',True)
    return a

def bat(frame=0):
    a=Art(32,32)
    up = frame==0
    if up:
        a.poly([(13,15),(1,5),(4,13),(1,19),(8,18)],'batwing'); a.poly([(19,15),(31,5),(28,13),(31,19),(24,18)],'batwing')
    else:
        a.poly([(13,15),(2,20),(6,24),(10,21)],'batwing'); a.poly([(19,15),(30,20),(26,24),(22,21)],'batwing')
    a.ell(16,16,5,5,'bat')
    a.poly([(12,12),(13,7),(15,11)],'bat'); a.poly([(20,12),(19,7),(17,11)],'bat')
    a.rect(13,15,14,16,'white',True); a.rect(18,15,19,16,'white',True)
    a.px(14,16,'eye',True); a.px(18,16,'eye',True)
    a.px(15,19,'white',True); a.px(17,19,'white',True)
    return a

def treant(frame=0):
    a=Art(48,48)
    o=0 if frame==0 else 1
    # kok/bacaklar
    a.rect(14,38,19,45-o,'bark'); a.rect(28,38,33,45+o,'bark')
    a.poly([(11,46-o),(21,46-o),(19,42),(13,42)],'bark'); a.poly([(26,46+o),(36,46+o),(34,42),(28,42)],'bark')
    # govde
    a.ell(24,30,13,12,'bark')
    # kollar (dal)
    a.poly([(11,26),(2,20),(1,24),(9,32)],'bark'); a.poly([(37,26),(46,20),(47,24),(39,32)],'bark')
    a.ell(2,19,3,3,'leaf'); a.ell(46,19,3,3,'leaf')
    # tac
    a.ell(24,13,16,10,'leaf'); a.ell(14,10,7,6,'leaf'); a.ell(34,10,7,6,'leaf'); a.ell(24,5,8,5,'leaf')
    # yuz
    a.rect(17,25,20,27,'eyeY',True); a.rect(28,25,31,27,'eyeY',True)
    a.poly([(18,33),(30,33),(28,37),(20,37)],'eye')
    a.rect(20,33,21,34,'white',True); a.rect(27,33,28,34,'white',True)
    # yosun
    a.px(22,40,'leaf',True); a.px(30,36,'leaf',True); a.px(15,32,'leaf',True)
    return a

if __name__=="__main__":
    sprites=[('hero0',hero(0)),('hero1',hero(1)),('goblin0',goblin(0)),('goblin1',goblin(1)),('wolf0',wolf(0)),('wolf1',wolf(1)),
             ('orc0',orc(0)),('orc1',orc(1)),('bat0',bat(0)),('bat1',bat(1)),('treant0',treant(0))]
    sc=6; cols=4; cell=48*sc+16
    sheet=Image.new("RGB",(cols*cell,((len(sprites)+cols-1)//cols)*cell),(92,150,70))
    for i,(n,a) in enumerate(sprites):
        im=a.render().resize((a.w*sc,a.h*sc),Image.NEAREST)
        sheet.paste(im,((i%cols)*cell+8,(i//cols)*cell+8),im)
    sheet.save("sheet_preview.png")

M.update({
 'mush':((255,140,140),(220,60,60),(150,30,40)),
 'stem':((250,240,220),(225,212,190),(170,155,135)),
 'stone':((200,206,214),(150,156,166),(98,104,116)),
 'moss':((150,200,90),(110,160,70),(70,115,50)),
 'gem':((235,220,255),(170,120,240),(100,60,170)),
 'gemB':((220,250,255),(90,200,255),(40,120,200)),
 'meat':((255,150,140),(214,82,72),(140,40,40)),
 'bone':((255,255,245),(232,228,214),(180,175,160)),
 'chestw':((200,140,80),(150,95,48),(100,60,28)),
 'flowerY':((255,245,170),(250,215,70),(200,160,30)),
 'flowerP':((255,200,240),(240,130,210),(170,70,150)),
 'trunk':((150,100,60),(110,72,40),(72,46,24)),
 'leafD':((110,190,90),(50,135,65),(30,90,48)),
})
def tree():
    a=Art(48,56)
    a.rect(20,34,27,52,'trunk'); a.poly([(16,53),(31,53),(28,48),(19,48)],'trunk')
    a.ell(24,26,18,12,'leafD'); a.ell(14,22,10,9,'leaf'); a.ell(34,22,10,9,'leaf'); a.ell(24,14,12,10,'leaf'); a.ell(24,28,12,7,'leafD')
    return a
def bush():
    a=Art(24,16); a.ell(8,9,7,6,'leaf'); a.ell(16,9,7,6,'leaf'); a.ell(12,6,7,5,'leaf')
    a.px(7,7,'flowerP',True); a.px(16,5,'flowerP',True); return a
def rock():
    a=Art(18,13); a.ell(9,7,8,5.5,'stone'); a.ell(6,4,3,2,'moss'); return a
def mushroom():
    a=Art(12,12); a.rect(5,6,6,11,'stem'); a.ell(5.5,5,5,3.5,'mush'); a.px(3,4,'stem',True); a.px(7,3,'stem',True); return a
def flower(c):
    a=Art(8,9); a.line(4,4,4,8,'leaf',True); a.ell(4,3,2,2,c); a.px(4,3,'flowerY',True); return a
def gem(big=False):
    a=Art(12,14) if big else Art(10,12)
    m='gemB' if big else 'gem'
    w=a.w; h=a.h
    a.poly([(w/2,0),(w-1,h*0.4),(w/2,h-1),(0,h*0.4)],m); return a
def coin():
    a=Art(11,11); a.ell(5,5,5,5,'gold'); a.rect(5,3,5,7,'gold',True); return a
def meat():
    a=Art(14,12); a.ell(6,6,6,5,'meat'); a.rect(10,5,13,7,'bone'); a.ell(13,5,1.2,1.2,'bone'); a.ell(13,8,1.2,1.2,'bone'); return a
def chest():
    a=Art(24,20); a.rect(1,8,22,19,'chestw'); a.ell(11.5,8,11,6,'chestw'); a.rect(1,10,22,11,'gold'); a.rect(10,9,13,14,'gold'); a.px(11,12,'eye',True); return a

# ---------------- Ekipman ikonlari (16x16, notr renk; nadirlik cercevesi oyunda cizilir)
M.update({'steel':((236,240,246),(178,186,198),(112,120,134)),'cloth':((214,190,150),(170,140,100),(120,94,62)),
          'ruby':((255,150,170),(230,70,100),(150,30,60)),'sapph':((170,220,255),(80,150,240),(40,80,170))})
def icon_weapon():
    a=Art(16,16); a.line(3,13,11,5,'leather'); a.line(4,13,12,5,'leather'); a.ell(12.5,3.5,2.6,2.6,'glow'); a.rect(2,13,4,14,'gold'); return a
def icon_helmet():
    a=Art(16,16); a.ell(8,8,6,5.5,'steel'); a.rect(2,9,13,11,'steel'); a.rect(5,9,10,10,'eye',True); a.rect(7,1,8,3,'ruby'); return a
def icon_armor():
    a=Art(16,16); a.poly([(3,3),(6,2),(8,4),(10,2),(13,3),(14,8),(12,14),(4,14),(2,8)],'steel'); a.rect(7,5,8,12,'gold'); return a
def icon_belt():
    a=Art(16,16); a.rect(1,6,14,9,'leather'); a.rect(6,5,9,10,'gold'); a.rect(7,7,8,8,'leather',True); return a
def icon_pants():
    a=Art(16,16); a.rect(4,2,11,5,'cloth'); a.poly([(4,5),(8,5),(7,14),(4,14)],'cloth'); a.poly([(8,5),(11,5),(11,14),(8,14)],'cloth'); a.rect(4,2,11,3,'leather'); return a
def icon_boots():
    a=Art(16,16); a.rect(3,2,7,11,'leather'); a.rect(3,11,12,13,'leather'); a.rect(3,3,7,4,'gold'); return a
def icon_cloak():
    a=Art(16,16); a.poly([(5,1),(10,1),(14,14),(1,14)],'cloak'); a.rect(6,1,9,3,'gold'); return a
def icon_necklace():
    a=Art(16,16); a.line(3,2,8,9,'gold'); a.line(12,2,8,9,'gold'); a.ell(8,11,2.6,2.6,'sapph'); return a
def icon_earring():
    a=Art(16,16); a.ell(8,4,2.5,2.5,'gold'); a.rect(8,6,8,9,'gold'); a.poly([(8,9),(11,12),(8,15),(5,12)],'ruby'); return a
def icon_ring():
    a=Art(16,16); a.ell(8,10,5,4.5,'gold'); a.ell(8,10,3,2.5,'gold'); a.ell(8,4.5,2.5,2.5,'ruby');
    for y in range(8,13):
        for x in range(6,11):
            if ((x-8)/3)**2+((y-10)/2.5)**2<=0.8: a.g[y][x]=None
    return a
def chest_royal():
    a=Art(24,20); a.rect(1,8,22,19,'bat'); a.ell(11.5,8,11,6,'bat'); a.rect(1,10,22,11,'gold'); a.rect(10,9,13,14,'gold'); a.rect(1,8,2,19,'gold'); a.rect(21,8,22,19,'gold'); a.px(11,12,'eye',True); return a
