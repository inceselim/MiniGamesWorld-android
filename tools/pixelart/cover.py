import sys,random,math; sys.path.insert(0, __import__("os").path.dirname(__file__))
from art import *
from PIL import Image, ImageDraw
def R(a): return a.render()
W,H=320,180
img=Image.new("RGBA",(W,H),(94,158,68,255)); d=ImageDraw.Draw(img)
random.seed(5)
for _ in range(90):
    x=random.randrange(W); y=random.randrange(H); r=random.randrange(6,16)
    d.ellipse([x-r,y-r//2,x+r,y+r//2],fill=random.choice([(79,138,58,255),(112,176,80,255)]))
for _ in range(160):
    x=random.randrange(W); y=random.randrange(H)
    d.line([(x,y),(x,y-2)],fill=(70,125,52,255)); d.line([(x+2,y),(x+2,y-3)],fill=(140,200,90,255))
def put(im,x,y,flip=False,scale=1):
    if flip: im=im.transpose(Image.FLIP_LEFT_RIGHT)
    if scale!=1: im=im.resize((im.width*scale,im.height*scale),Image.NEAREST)
    img.alpha_composite(im,(int(x),int(y)))
put(R(tree()),-6,-10); put(R(tree()),262,96); put(R(bush()),80,150); put(R(rock()),200,8); put(R(mushroom()),150,160)
cx,cy=150,95
glow=Image.new("RGBA",(W,H),(0,0,0,0)); gd=ImageDraw.Draw(glow)
for r in range(40,0,-4): gd.ellipse([cx-r,cy-r,cx+r,cy+r],outline=(143,227,107,40+r))
img.alpha_composite(glow)
put(R(treant(0)),210,20,True)
for (f,x,y,fl) in [(goblin,90,60,False),(goblin,100,112,False),(wolf,190,110,True),(orc,48,84,False),(bat,110,22,False),(bat,150,30,True),(goblin,240,140,True),(bat,40,30,False)]:
    put(R(f(random.randint(0,1))),x,y,fl)
for (x,y) in [(78,55),(130,140),(176,72),(200,150),(118,92),(240,60)]: put(R(gem()),x,y)
put(R(gem(True)),170,128); put(R(coin()),136,124); put(R(coin()),186,138)
put(R(hero(0)),cx-16,cy-26)
for i in range(4):
    a=i*math.pi/2+0.4; bx=cx+math.cos(a)*24; by=cy-8+math.sin(a)*24
    d=ImageDraw.Draw(img); d.polygon([(bx,by-5),(bx+2,by),(bx,by+5),(bx-2,by)],fill=(232,237,245,255),outline=(75,85,99,255))
d.line([(cx+10,cy-14),(cx+34,cy-28)],fill=(111,224,255,255),width=2); d.ellipse([cx+31,cy-31,cx+38,cy-24],fill=(230,251,255,255))
img=img.resize((960,540),Image.NEAREST).convert("RGB")
img.save(__import__("os").path.join(__import__("os").path.dirname(__file__), "../../app/src/main/res/drawable-nodpi/game_forest_guardian.webp"),"WEBP",quality=90)
img.save("cover_preview.png")
