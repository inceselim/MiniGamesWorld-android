# Vektör çizimler (Moda Şovu, Makyaj Salonu, Pasta Dükkanı)

Çizimler Python'da SVG yol verisi olarak tanımlanır, `rsvg-convert` ile önizlenir ve Kotlin'e aktarılır.
Uygulamada `ui/games/common/VectorArt.kt` bu yolları Compose `PathParser` ile çizer.
Renk yuvaları (`main`, `skin`, `hair`, `eye`, `lips`, `D`/`L` sonekli tonlar) çalışma anında seçilen renkle boyanır.

```
python3 dressup.py --preview   # /tmp/dress_sheet.png
python3 dressup.py --kotlin    # DressUpArt.kt
python3 makeup.py --preview    # /tmp/mk_sheet.png
python3 makeup.py --kotlin     # MakeupArt.kt
python3 dressup_cover.py ; python3 makeup_cover.py ; python3 cake_cover.py   # kapaklar
```
