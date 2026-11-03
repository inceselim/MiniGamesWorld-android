# Orman Muhafızı piksel art üretici

Karakterler şekillerle (elips, dikdörtgen, çokgen) tanımlanır; betik otomatik gölge, doku ve dış hat ekler.

```
pip install pillow
python3 export.py   # ForestSprites.kt'yi yeniden üretir + all_sprites_preview.png
python3 cover.py    # oyun kapağını yeniden üretir
```

Yeni bir karakter: `art.py`'ye bir fonksiyon ekle (ör. `def skeleton(frame)`), `export.py`'deki `sprites` sözlüğüne ekle, betiği çalıştır.
