import sys; sys.path.insert(0, __import__("os").path.dirname(__file__))
from art import *
from PIL import Image
sprites={'hero0':hero(0),'hero1':hero(1),'goblin0':goblin(0),'goblin1':goblin(1),'wolf0':wolf(0),'wolf1':wolf(1),
 'orc0':orc(0),'orc1':orc(1),'bat0':bat(0),'bat1':bat(1),'treant0':treant(0),'treant1':treant(1),
 'tree':tree(),'bush':bush(),'rock':rock(),'mushroom':mushroom(),'flowerY':flower('flowerY'),'flowerP':flower('flowerP'),
 'gem':gem(False),'gem2':gem(True),'coin':coin(),'meat':meat(),'chest':chest()}
imgs={k:v.render() for k,v in sprites.items()}
# preview
sc=5; names=list(imgs); cols=6; cell=60*sc
sheet=Image.new("RGB",(cols*cell,((len(names)+cols-1)//cols)*cell),(92,150,70))
for i,n in enumerate(names):
    im=imgs[n]; im2=im.resize((im.width*sc,im.height*sc),Image.NEAREST)
    sheet.paste(im2,((i%cols)*cell+5,(i//cols)*cell+5),im2)
sheet.save("all_sprites_preview.png")
# palette chars
colors=[]
for im in imgs.values():
    for p in im.get_flattened_data():
        if p[3] and p[:3] not in colors: colors.append(p[:3])
chars="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#%&*+-/:;<=>?@^_~ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêë"
assert len(colors)<=len(chars), len(colors)
cmap={c:chars[i] for i,c in enumerate(colors)}
out=["package com.eomer91.minigamesworld.ui.games.forest","",
"import android.graphics.Bitmap","import androidx.compose.ui.graphics.ImageBitmap","import androidx.compose.ui.graphics.asImageBitmap","",
"/**"," * Orman Muhafizi piksel art cizimleri (32x32 karakterler, golgeli). Her harf bir renk, '.' saydam.",
" * Bu dosya bir Python betigiyle (sekil + otomatik golge + dis hat) uretildi; elle de duzenlenebilir.",
" * Bir kez Bitmap'e cevrilir ve FilterQuality.None ile keskin buyutulur.", " */",
"object ForestSprites {","    private val palette: Map<Char, Int> = mapOf("]
for c,ch in cmap.items(): out.append(f"        '{ch}' to 0xFF{c[0]:02X}{c[1]:02X}{c[2]:02X}.toInt(),")
out.append("    )\n")
for k,im in imgs.items():
    rows=["".join(cmap[p[:3]] if p[3] else "." for p in [im.getpixel((x,y)) for x in range(im.width)]) for y in range(im.height)]
    body="\n".join(f'        "{r}",' for r in rows)
    out.append(f"    private val {k}Rows = listOf(\n{body}\n    )\n")
out.append("""    private fun build(rows: List<String>): ImageBitmap {
        val w = rows.first().length
        require(rows.all { it.length == w }) { "Sprite satirlari esit uzunlukta olmali" }
        val bmp = Bitmap.createBitmap(w, rows.size, Bitmap.Config.ARGB_8888)
        rows.forEachIndexed { y, row -> row.forEachIndexed { x, ch -> palette[ch]?.let { bmp.setPixel(x, y, it) } } }
        return bmp.asImageBitmap()
    }
""")
for k in imgs: out.append(f"    val {k}: ImageBitmap by lazy {{ build({k}Rows) }}")
out.append("}")
open(__import__("os").path.join(__import__("os").path.dirname(__file__), "../../app/src/main/java/com/eomer91/minigamesworld/ui/games/forest/ForestSprites.kt"),"w").write("\n".join(out)+"\n")
print(len(colors),"colors")
