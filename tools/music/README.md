# Oyun müzikleri

`compose.py` küçük bir chiptune sentezleyicidir (kare/üçgen dalga + gürültü davul). Notalar betiğin içinde yazılıdır;
müzikler tamamen burada üretildiği için telif sorunu yoktur.

```
pip install numpy      # ffmpeg de gerekli (ogg/vorbis için)
python3 compose.py     # res/raw/music_mine_run.ogg, music_fox.ogg, music_forest.ogg
```

Her parça tam ölçü uzunluğundadır ve yankı dairesel uygulanır; böylece döngü noktasında kesinti olmaz.
Uygulamada `feedback/GameMusic.kt` çalar; Ayarlar → Müzik ile kapatılabilir.
