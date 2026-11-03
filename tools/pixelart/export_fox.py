"""Tilki Macerasi sprite'larini FoxSprites.kt olarak uretir."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art import *
from export import export
sprites={'fox0':fox(0),'fox1':fox(1),'fox2':fox(2),'fox3':fox(3),'snail0':snail(0),'snail1':snail(1),'bee0':bee(0),'bee1':bee(1),
 'hedgehog0':hedgehog(0),'hedgehog1':hedgehog(1),'tGrass':tile('grass'),'tDirt':tile('dirt'),'tBrick':tile('brick'),'tQ':tile('q'),
 'tUsed':tile('used'),'tCave':tile('cave'),'tCaveTop':tile('caveTop'),'tCastle':tile('castle'),'tSpikes':tile('spikes'),
 'tSpring':tile('spring'),'tPlat':tile('plat'),'tLava':tile('lava'),'flagpole':flagpole(),'star':star(),'heart':heart(),
 'coin':coin(),'checkOff':checkpoint(False),'checkOn':checkpoint(True),'bush':bush(),'tree':tree(),'rock':rock(),'mushroom':mushroom()}
here=os.path.dirname(os.path.abspath(__file__))
out=os.path.join(here,"../../app/src/main/java/com/eomer91/minigamesworld/ui/games/fox/FoxSprites.kt")
os.makedirs(os.path.dirname(out),exist_ok=True)
export(sprites,"FoxSprites","com.eomer91.minigamesworld.ui.games.fox",out,"fox_preview.png")
