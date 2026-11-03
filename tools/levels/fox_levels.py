"""Tilki Macerasi bolumleri: elle tasarlanmis parcalar (chunk) birlestirilir -> FoxLevels.kt.
Harita 14 satir; zemin 12-13. Karakterler:
 # zemin  B tugla  ? soru(sikke)  H soru(kalp)  = tek yonlu platform  ^ diken  S yay  L lav
 c sikke  * yildiz  K kontrol noktasi  F bayrak  @ baslangic  e salyangoz  b ari  h kirpi
"""
import os
H=14
def C(*rows):
    rows=list(rows)
    assert len(rows)==H, (len(rows), rows)
    w=max(len(r) for r in rows)
    return [r.ljust(w,'.') for r in rows]
G="#"
def ground(w, pit=None):
    """pit: (baslangic, genislik)"""
    r=[G]*w
    if pit:
        for i in range(pit[0],pit[0]+pit[1]): r[i]='.'
    s="".join(r); return [s,s]
def blank(w): return ["."*w]
def build(w, top, pit=None):
    """top: satir 0'dan 11'e kadar ust kisim (12 satir), alt: zemin"""
    rows=[r.ljust(w,'.') for r in top]
    rows=["."*w]*(12-len(rows))+rows
    return C(*(rows+ground(w,pit)))

CH={}
# Not: build() ust satirlari 11. satira (zeminin hemen ustu) hizalar. Ziplama ~4 karo, yay ~7 karo.
CH['start']=build(10,["..@......."])
CH['flat']=build(8,["..c.c.c.","........","........"])
CH['flat_e']=build(10,["...c.c.c..","..........","..........","......e..."])
CH['gap2']=build(7,["..ccc..",".......",".......","......."],pit=(2,2))
CH['gap3']=build(8,["...cc...","..c..c..","........","........","........"],pit=(2,3))
CH['stairs']=build(10,["......####","....######","..########"])
CH['stairs_down']=build(10,["####......","######....","########.."])
CH['blocks']=build(12,["...B?B?B....","............","............","............","......e....."])
CH['blocks_h']=build(12,["....?BHB?...","............","............","............","..e.....e..."])
CH['platforms']=build(16,["..........c.....","........===.....","...c............","..===......===..","................","................"],pit=(2,12))
CH['spikes']=build(10,["...ccc....","..........","..........","...^^^...."])
CH['spring']=build(12,["......ccc...","......===...","............","............","............","............","..S........."])
CH['bees']=build(14,["....b.....b...","..............","..c.c.c.c.c...","..............","..............",".............."])
CH['hedge']=build(14,["....=====.....","..............","...h......h..."])
CH['tower']=build(9,["..c......","....##...","....##...","..e.##.e."])
CH['star_high']=build(12,["......*.....","............","....?B?.....","............","............","..B......B..","............","............"])
CH['star_pit']=build(10,["....*.....","..........","..........",".........."],pit=(3,3))
CH['check']=build(6,["..K..."])
CH['coins_arc']=build(10,["...cccc...","..c....c..","..........","..........","...e......"])
CH['end']=build(16,["..............F.","...........##...","..........###...","........#####..."])
CH['lava2']=build(8,["..ccc...","........","........","........"],pit=(2,3))
CH['hard_plat']=build(18,["...........c......","..........===.....","..................","......===.........","..................","..===........===..","..................","..b........b......"],pit=(2,14))
CH['spike_run']=build(16,["....cc....cc....","................","................","..^^..^^^..^^..."])
CH['spring_gap']=build(14,["........c.c.c.","..............","..............","..............","..............","..............","..S..........."],pit=(4,6))
CH['star_spring']=build(10,["...*......","...===....","..........","..........","..........","..........",".S........"])

def assert_star_counts():
    pass

WORLDS=[
 # Cayir
 [["start","flat","gap2","blocks","flat_e","star_high","gap2","stairs","stairs_down","check","coins_arc","spring","star_spring","bees","blocks_h","star_pit","flat","end"],
  ["start","flat_e","blocks","gap3","coins_arc","star_high","tower","check","bees","spring","star_spring","gap3","blocks_h","star_pit","stairs","end"],
  ["start","blocks","gap3","tower","star_high","platforms","check","hedge","spikes","star_spring","gap3","bees","star_pit","blocks_h","end"],
  ["start","flat_e","platforms","star_high","spikes","tower","check","hedge","spring_gap","star_spring","bees","gap3","star_pit","blocks","stairs","end"],
  ["start","spikes","platforms","star_high","hedge","spring_gap","check","hard_plat","bees","star_spring","spike_run","tower","star_pit","end"]],
 # Magara
 [["start","flat_e","gap2","blocks","star_high","hedge","check","coins_arc","spring","star_spring","tower","star_pit","end"],
  ["start","hedge","gap3","platforms","star_high","check","spikes","bees","star_spring","blocks_h","star_pit","stairs","end"],
  ["start","tower","spike_run","star_high","platforms","check","hedge","spring_gap","star_spring","gap3","star_pit","end"],
  ["start","bees","hard_plat","star_high","check","spikes","hedge","star_spring","spring_gap","tower","star_pit","end"],
  ["start","spike_run","hard_plat","star_high","hedge","check","platforms","spring_gap","star_spring","bees","spike_run","star_pit","tower","end"]],
 # Kale
 [["start","flat_e","lava2","blocks","star_high","spikes","check","tower","star_spring","lava2","star_pit","end"],
  ["start","spike_run","lava2","star_high","platforms","check","hedge","star_spring","lava2","blocks_h","star_pit","end"],
  ["start","hard_plat","star_high","spikes","check","spring_gap","hedge","star_spring","lava2","star_pit","tower","end"],
  ["start","spike_run","platforms","star_high","check","hard_plat","star_spring","hedge","spike_run","star_pit","end"],
  ["start","hard_plat","spike_run","star_high","check","hedge","spring_gap","star_spring","hard_plat","lava2","star_pit","tower","spike_run","end"]],
]

def assemble(names, world):
    rows=[""]*H
    for n in names:
        ch=CH[n]
        for i in range(H): rows[i]+=ch[i]
    # Kale: cukurlarin dibi lav
    if world==2:
        rows[13]="".join('L' if c=='.' else c for c in rows[13])
    # Magara ve kale: tavan
    if world>=1:
        rows[0]="".join('#' if c=='.' else c for c in rows[0])
    for r in rows: assert len(r)==len(rows[0])
    stars=sum(r.count('*') for r in rows)
    assert stars==3, (names, stars)
    assert sum(r.count('@') for r in rows)==1
    assert sum(r.count('F') for r in rows)==1
    return rows

if __name__=="__main__":
    for k,v in CH.items():
        assert len(v)==H, k
        assert len(set(len(r) for r in v))==1, k
    out=["package com.eomer91.minigamesworld.ui.games.fox","",
         "/** tools/levels/fox_levels.py ile uretildi. Satir 0 ust; zemin 12-13. Karakterler icin betige bak. */",
         "object FoxLevels {","    val worlds: List<List<List<String>>> = listOf("]
    for w,levels in enumerate(WORLDS):
        out.append("        listOf(")
        for names in levels:
            rows=assemble(names,w)
            out.append("            listOf(")
            for r in rows: out.append(f'                "{r}",')
            out.append("            ),")
        out.append("        ),")
    out.append("    )")
    out.append("}")
    here=os.path.dirname(os.path.abspath(__file__))
    p=os.path.join(here,"../../app/src/main/java/com/eomer91/minigamesworld/ui/games/fox/FoxLevels.kt")
    open(p,"w").write("\n".join(out)+"\n")
    for w,levels in enumerate(WORLDS):
        print("world",w,[len(assemble(n,w)[0]) for n in levels])
