# Mini Games World (Android)

Play Store'daki **Mini Games Hub** (`com.eomer91.MiniGamesWorldincesoy`) uygulamasının Kotlin + Jetpack Compose ile yeniden yazılmış hali.
Eski React Native/Expo kodu referans için `MiniGamesWorldincesoy` reposunda duruyor.

## Teknoloji

- Kotlin 2.3, Jetpack Compose (Material 3), tek Activity
- MVVM + Hilt (KSP), Navigation Compose (type-safe route'lar)
- DataStore: altın, favoriler, rekorlar, ayarlar
- Unity LevelPlay reklam aracısı (Unity Ads + AdMob yarışır; banner, geçiş, ödüllü) + UMP (GDPR onay ekranı)
- SoundPool ile ses efektleri, titreşim
- Uygulama içi dil seçimi (AppCompat per-app locales)
- Derleme: AGP 9.1 (built-in Kotlin), Gradle 9.4.1, Java 21 veya 25

## Projeyi açma

1. Android Studio'da **Open** ile bu klasörü aç ve Gradle senkronizasyonunu bekle.
   Gereksinimler: Android Studio Panda 2 (2025.3.2) veya üstü, Gradle JDK 21 ya da 25.
2. `app` konfigürasyonunu bir emülatörde ya da telefonda çalıştır.
3. Değişiklikleri GitHub'dan almak için **Git → Pull** kullan. Bu bir *pull request* değildir.

Debug build `com.eomer91.MiniGamesWorldincesoy.debug` paket adıyla kurulur. Böylece telefondaki mağaza sürümüyle yan yana durur ve Google'ın test reklamlarını gösterir.

## Oyunlar

| Oyun | Durum | Not |
|---|---|---|
| Block Puzzle | ✅ Native | Sürükle-bırak, renkli parçalar, satır + sütun temizleme, **kaldığı yerden devam** |
| 2048 | ✅ Native | Kaydırma, 2048'e ulaşınca devam, **kaldığı yerden devam** |
| Snake | ✅ Native | İlk kaydırmada başlar, yem yedikçe hızlanır, arka planda durur |
| Memory Card | ✅ Native | 3D kart çevirme; hamle sayısı skora çevrilir (kusursuz = 1000) |
| Sudoku | ✅ Native | Kolay/Normal/Zor, her seferinde yeni tek çözümlü bulmaca, notlar, 3 hata hakkı, süre, **kaldığı yerden devam** |
| Stack | ✅ Native | Tek dokunuş; taşan kısım kesilir, kusursuz bırakışta kombo ve blok büyümesi, giderek hızlanır |
| Match 3 | ✅ Native | 8×8, 6 taş (renk + şekil), kaydır ya da dokun, zincirleme patlama, 30 hamle, hamle bitince ödüllü +5 |
| Bubble Shooter | ✅ Native | Bal peteği tahta, duvardan seken nişan çizgisi, 3+ patlatma, kopanlar düşer, 5 boş atışta yeni sıra, seviyeler |
| Tower Defense | ✅ Native | Tek kule savunması: dalgalar, 4 düşman türü + boss, gümüşle oyun içi, altınla kalıcı (Atölye) yükseltmeler, 2× hız | Detay ekranında altın harcanmaz, reklam gösterilmez |
| Mayın Tarlası | ✅ Native | Kolay/Normal/Zor, ilk dokunuş güvenli, Kaz/Bayrak modu, basılı tut = bayrak, sayıya dokun = komşuları aç, ikinci şans (reklam), **kaldığı yerden devam** |
| Solitaire | ✅ Native | Klondike, 1 ya da 3 kart çekme, dokun = otomatik taşı, sürükle-bırak, sınırsız geri al, ipucu (reklam), otomatik bitir, **kaldığı yerden devam** |
| Dört Bağla | ✅ Native | Bilgisayara karşı (Kolay/Normal/Zor, negamax + alfa-beta) ya da 2 oyuncu, düşme animasyonu, seri skoru, hamle geri al (reklam) |
| Roket Zıpla | ✅ Native | Özgün Flappy tarzı: dokunarak it, asteroit kapıları, yıldız bonusu, hızlanma, kalkanla devam (reklam) |
| Balık Dünyası | ✅ Native | Olta at, kancayı sürükle, denizanalarından kaç; 9 balık türü, inciyle olta yükseltmeleri; tutulanlar akvaryuma: yem at, büyüt, inci topla, sat, akvaryumu büyüt, süsle; ekstra atış ve yem (reklam) |
| Borsa Ustası | ✅ Native | Eski "io stock trade" oyunundan (com.stockgameincesoy): 1.000 $ ile 60 gün, 4 hisse (AA, CCA, XAH, GLD), trend/söylenti/bilanço haberleri, %0,5 komisyon, içeriden bilgi (reklam), **kaldığı yerden devam** |
| Ülkeler Savaşı | ✅ Native | Eski "CountriesWar" oyunundan (com.countriesincesoyselim): 5 sn'lik otomatik turlar, 11 bina, 6 birlik, vergi/odak/yönetim biçimi, pazar, 1-7 bot, ilk 10 tur barış, casus ağı, Altın Çağ (elmas), elmas (reklam), **kaldığı yerden devam** |
| Orman Muhafızı | ✅ Native (adım 1) | Survivors tarzı, **yatay ekran**: joystick, otomatik saldırı, 6 silah + 6 pasif, seviye atlama seçimi, atılma + MP özel yeteneği, 4 düşman + boss (4:30), 5 harita, diriliş (reklam). Piksel art (`ForestSprites.kt`). Görevler (harita başına 3, altın ödülü) ve 6 silah evrimi. |

Her oyunun kuralları ekrandan ayrı, saf Kotlin bir `…Engine` sınıfında durur ve `app/src/test` altında unit testleri vardır. Testleri çalıştırmak için test dosyasını açıp sınıf adının yanındaki yeşil oka bas ya da `./gradlew test` komutunu çalıştır.

Rekorlar oyun sırasında, rekor geçildiği anda kaydedilir. Oyuncu oyunun ortasında çıksa da rekor kaybolmaz.

## Altın (oyun parası)

- Başlangıç: 500 altın. Her oyuna giriş, detay ekranındaki ücret kadar altın harcar.
- **Her 200 puan = 1 altın.** Altın, puan 200'ün katlarını geçtiği anda verilir (oyunun ortasında çıksan da kaybolmaz). Oyun bitince panelde "+N altın kazandın" yazar.
- Memory Card'da puan oyun bitince hesaplandığı için altın da bitişte verilir (kusursuz oyun = 1000 puan = 5 altın).
- Hesap, o oyunda ulaşılan **en yüksek** puana göre yapılır. Block Puzzle'da geri alınan hamleler tekrar oynanınca aynı altın ikinci kez verilmez.
- **Altın yetmezse:** Detay ekranında Play'e basınca yeterli altın yoksa bir pencere açılır: "Reklam izle (+50)". Ödüllü reklam sonuna kadar izlenirse **50 altın** eklenir, oyuncu tekrar Play'e basarak oyuna girer.
- Kurallar tek yerde: `data/GameEconomy.kt` (`POINTS_PER_GOLD`, `AD_REWARD_GOLD`).

## Kaldığı yerden devam (Block Puzzle, 2048, Sudoku, Mayın Tarlası, Solitaire, Borsa Ustası, Ülkeler Savaşı)

- Her hamleden sonra oyun (tahta, skor, o oyunda kazanılan altın; Block Puzzle'da ayrıca eldeki parçalar) cihaza kaydedilir. Oyundan çıkınca, uygulama kapansa bile kayıt durur.
- Detay ekranında kayıt varsa iki buton çıkar: **Kaldığın yerden devam et** (altın harcanmaz) ve **Yeni oyun · N altın** (kayıt silinir). Devam etmede de geçiş reklamı kuralı geçerlidir.
- Oyun bitince kayıt silinir, bir dahaki giriş yeni oyun olur.
- Block Puzzle'da geri alma hakkı kaydedilmez; devam edilen oyunda "Geri al" yeni bir hamleden sonra açılır.
- Kayıt formatları: `BlockPuzzleSave.kt`, `Game2048Save.kt`, `SudokuSave.kt`, `MinesSave.kt` (JSON, sürüm numaralı). Bozuk ya da eski bir kayıt sessizce yok sayılır ve yeni oyun başlar.

## Sudoku

- **Akış:** Zorluk seçimi (Kolay 40 / Normal 32 / Zor 26 ipucu) → bulmaca cihazda üretilir (tek çözümlü, her seferinde farklı) → oyun.
- **Tahta:** Seçili hücre, aynı satır/sütun/kutu ve aynı rakamlar vurgulanır. Hazır rakamlar beyaz, doğru girilenler mavi, yanlışlar kırmızı.
- **Araçlar:** Sil, Not modu (doğru rakam girilince ilgili notlar otomatik silinir), İpucu (ödüllü reklam: seçili ya da ilk boş hücre doğru rakamla dolar).
- **3 hata** → oyun biter; ödüllü reklamla devam (hata 2'ye iner, yanlış rakamlar silinir) ya da yeni bulmaca.
- **Puan:** taban (600 / 1200 / 2400) − hata×100 − ipucu×150 − hedef süreyi (6 / 12 / 20 dk) aşan her saniye; en az tabanın 1/5'i. Altın oyun bitince verilir.
- **Süre** sadece ekran açıkken işler; arka planda ve reklam sırasında durur.
- Kayıt: her hamlede ve 10 saniyede bir (`SudokuSave.kt`).

## Tower Defense (tek kule)

- Ortada tek kule, düşmanlar her yönden dalgalar halinde gelir; kule menzildeki en yakın düşmana otomatik ateş eder. Kuleye değen düşman can götürür.
- Düşmanlar: Normal, Hızlı (3. dalgadan), Zırhlı (5. dalgadan), her 10 dalgada Boss. Düşman canı her dalgada ~%13 artar.
- **Gümüş (oyun içi):** Öldürme ve dalga sonu bonusuyla kazanılır; Hasar, Atış hızı, Menzil, Can, Yenilenme alınır. Oyun bitince sıfırlanır.
- **Altın (kalıcı, Atölye):** Aynı 5 özellik + Gümüş bonusu + Başlangıç gümüşü; 20 seviyeye kadar, hiç sıfırlanmaz (DataStore'da `td_perm_*`).
- Fiyatlar: gümüş `10 × 1.32^seviye`, altın `15 × 1.45^seviye`. Denge `TowerBalance` içinde; bir Python simülasyonuyla ayarlandı (yükseltmesiz ~5. dalga).
- 1×/2× hız; oyun arka planda ve reklam sırasında durur.
- Ödüllü reklam: kule yıkılınca **yarı canla devam** (oyun başına bir kez). Atölye'ye dönüşte geçiş reklamı.

## Balık Dünyası

- **Liman** ekranı: Balık tut, Olta dükkânı, Akvaryum (bekleyen inci ve aç balık sayısı gösterilir).
- **Atış**: dokun → kanca iner; sürükleyerek yönlendir. İnerken ilk dokunulan balık misinayı yukarı çevirir; çıkarken kapasite kadar balık daha tutulur. Denizanası: inerken çarparsa misina geri döner, çıkarken son balık kaçar. Oturum = 3 atış.
- **İnci**: tutulan balıkların değeri (sardalya 2 … altın balık 100). Puan = inci × 10 → altın (200 puan = 1 altın) ve rekor.
- **Yükseltmeler** (`RodUpgrade`): misina 30 m + 15 m/sv (7 sv, `40 × 1.7^sv`), kanca 2 + 1 balık/sv (5 sv, `60 × 1.9^sv`), makara +%15 hız/sv (5 sv, `35 × 1.6^sv`).
- Kalıcı profil `FishProfile` (JSON) `saved_game_fish-world-profile` anahtarında; katalog id'sinden farklı olduğu için detay ekranında "Devam" çıkmaz. Akvaryum kapasitesi 12; dolunca fazla balıklar denize bırakılır (inciler yine kazanılır).
- **Akvaryum** (`Aquarium`, `TankSim`): suya dokun = yem düşer, aç balıklar yeme yüzer. Yem kabı 10, 6 dakikada +1 (reklamla +10, en fazla 20).
  - Balık yedikten sonra 45 dk toktur; her öğün +%10 büyüme (10 öğünde yetişkin, boyu büyür).
  - İnci üretimi saatte `değer × 0.08 × (0.5 + büyüme) × (1 + süs bonusu)`; en fazla 8 saat birikir, 12 saattir aç olan üretmez. Ölüm yok.
  - Sat: `değer × (2 + 4 × büyüme)` inci. Akvaryumu büyüt: +4 yer (40'a kadar), `120 × 1.7^sv`.
  - Süsler: Yosun 60 (+%5), Mercan 150 (+%8), Kale 400 (+%12), Hazine 900 (+%20).

## Borsa Ustası

- Gün 2,2 sn'de (2× hızda 1 sn) ilerler; duraklatılınca ⏭ ile elle ilerletilir. 60. günde oyun biter; skor = son net değer (nakit + hisseler), 200 puan = 1 altın.
- Hisseler: AA (Aero Avia, orta risk), CCA (Cosmo Cola, istikrarlı), XAH (Xahor Tech, yüksek risk), GLD (Golden Mine, piyasa düşerken yükselir).
- Her hissenin 6-14 gün süren gizli trendi var (yükseliş/yatay/düşüş); trend değişince haber çıkar. Değişimden 2 gün önce %80 ihtimalle söylenti çıkar (%80 doğru). Günde %4 ihtimalle bilanço sürprizi (±%10-25). Piyasa 10-20 günde bir boğa/ayı arasında döner.
- Denge Python simülasyonuyla kontrol edildi: rastgele al-sat ≈ 1.000 $, trendi takip eden ≈ 2.300 $ (medyan).

## Ülkeler Savaşı

- Değerler eski oyundan (`CountriesData.kt`): bina/birlik fiyatları, bakım, üretim, yönetim biçimi etkileri. Kurallar `CountriesEngine` içinde.
- Tur 5 sn'de bir ilerler (duraklatılabilir, savaş raporu açıkken durur). İlk 10 tur barış: kimse saldıramaz.
- Gelir: vergi (nüfus/1000 × 30 × vergi% × meclis verimi) + binalar; bakım düşülür. Ordu altın tüketmez, odun/kil/demir tüketir. Bir kaynak eksiye düşerse kıtlık: her birlikten %3 kaçar.
- Nüfus: çiftlik başına 1.000 kapasite; vergi yükseldikçe artış yavaşlar. Kışla başına 100 asker yeri.
- Savaş: saldırı gücü > savunma gücü ise kazanılır; yenilen ülke yok olur, altın ve nüfusunun %10-30'u alınır. Kazanan gönderdiği birliklerin bir kısmını, kaybeden hepsini kaybeder.
- Botlar: her tur kaynak toplar, eksi veren kaynağın binasını kurar, asker alır; barıştan sonra zorluğa göre %3-6 ihtimalle saldırır (ordusunun %60'ıyla, umutsuzsa saldırmaz).
- Elmas: reklamla +3. Casus ağı 5, Büyük çarşı 1, Altın Çağ (5 tur +%100 saldırı/savunma/nüfus) 5 elmas.
- Skor: zafer = 1000 × rakip + (3000 − 15 × tur) + nüfus/20; yenilgi = 300 × fethedilen + 5 × tur.

## Orman Muhafızı (survivors tarzı)

- Ekran: `ForestScreen` girişte yatay + tam ekran (`requestedOrientation`), çıkışta dikeye döner. `MainActivity` `configChanges` ile döndürmede yeniden oluşturulmaz. `PlayScreen` bu oyun için üst barı göstermez.
- Simülasyon `ForestEngine` (mutable sınıflar, 60 fps); ekran `viewModel.frame` sayacını okuyarak her karede çizer. Piksel art `ForestSprites.kt` içinde harf dizileri, bir kez `Bitmap`'e çevrilip `FilterQuality.None` ile büyütülür.
- Statlar `HeroStats` (HP, MP, saldırı %, savunma, saldırı hızı %, kritik şansı/hasarı, hız).
- Silah evrimleri (5. seviye silah + eşleşen pasif, seviye atlamada ilk seçenek): Büyü Oku+Güç→Ruhani Mızrak, Dönen Bıçaklar+Çeviklik→Kasırga, Yıldırım+Bilgelik→Fırtına, Diken Halkası+Dayanıklılık→Yaşam Sarmaşığı, Ok Yağmuru+Hızlı Çizme→Rüzgar Okları, Buz Patlaması+Mıknatıs→Mutlak Sıfır.
- Görevler `ForestQuests` (harita başına öldürme / seviye-evrim / boss'u X sn'de yen); ödül hub altını (5/8/12 × harita no), bir kez verilir, ölünce de tamamlananlar sayılır.
- Kalıcı profil `forest-guardian-profile` (açılan haritalar, tamamlanan görevler, en iyi skorlar). Oyunda ayrı para yok; görev ödülleri doğrudan hub altını. Skor = öldürme×5 + saniye×2 + harita bonusu → hub altını.

## Block Puzzle ödüllü reklam

Tahtanın altında iki buton var, ikisi de ödüllü (rewarded) reklam izletir:

| Buton | Ödül |
|---|---|
| **Parçaları değiştir** | Eldeki 3 parça yenileriyle değişir. Oyun bittiyse ve yeni parçalardan biri sığıyorsa oyun devam eder. |
| **Geri al** | Son yerleştirme geri alınır (tahta, parçalar, skor). Tek seviye; yeni bir hamle yapılınca tekrar açılır. Oyun bittiğinde de kullanılabilir. |

Ödül, reklam sonuna kadar izlenip kapatıldığında verilir. Reklam hazır değilse "Reklam henüz hazır değil" mesajı çıkar ve hiçbir şey değişmez.

## Mağaza sayfası (Play Console)

`store/` klasörü:
- `listing/<dil>.txt` – 10 dilde başlık (≤30), kısa (≤80) ve tam açıklama (≤4000). Play Console > Grow users > Store presence > Main store listing; diller "Manage translations > Add your own translations" ile eklenir.
- `release-notes-2.1.0.txt` – sürüm notları (`<en-US>…</en-US>` biçiminde, doğrudan yapıştırılır).
- `play_store_icon_512.png` – uygulama ikonu (512×512).
- `feature_graphic_1024x500.png` – tanıtım görseli.
- Ekran görüntüleri: telefondan en az 2, tercihen 4-8 (9:16), oyun içinden.

## Diller

Uygulama 10 dilde: İngilizce (varsayılan, `values/`), Türkçe, Almanca, İspanyolca, Fransızca, İtalyanca, Japonca, Rusça, Danca, Macarca (`values-xx/`). Dil, Ayarlar > Dil'den uygulama içinde seçilir (AppCompat per-app locales; Android 13+ sistem ayarlarında da görünür).

Yeni metin eklerken: önce `values/strings.xml`'e ekle, sonra **tüm** `values-xx/strings.xml` dosyalarına. Eksik çeviride Android İngilizceye düşer. Yeni dil eklerken `res/xml/locales_config.xml` ve `SettingsScreen.kt` içindeki `SUPPORTED_LANGUAGES` listesini de güncelle.

## Reklamlar

| Tür | Nerede |
|---|---|
| Banner | Ara, Favoriler, Profil ve Ayarlar sekmelerinin altında. Ana sayfada (Oyunlar) ve oyun ekranlarında yok. |
| Geçiş (Interstitial) | Oynanabilir bir oyunun detay ekranında **Play**, oyun içinde **Restart** ve **Tekrar oyna**; Ülkeler Savaşı ve Borsa Ustası'nda ayrıca **her ~3 dk aktif oyunda bir** tur/gün değişiminde (`PlaytimeAdTimer`, duraklatma sayılmaz) |
| Ödüllü (Rewarded) | Altın yetmeyince **Reklam izle (+50 altın)**; Block Puzzle: **Parçaları değiştir**, **Geri al**; Sudoku: **İpucu**, **3 hatadan sonra devam**; Match 3: **+5 hamle**; Bubble Shooter: **3 sırayı temizle ve devam et**; Tower Defense: **yarı canla devam**, Atölye'de altın yetmezse **reklamla +1 seviye**; Mayın Tarlası: **ikinci şans** (hepsi oyun başına bir kez); Solitaire: **ipucu**; Dört Bağla: **hamle geri al**; Roket Zıpla: **kalkanla devam** (oyun başına bir kez); Balık Dünyası: **bir atış daha** (oturum başına bir kez), **+10 yem**; Borsa Ustası: **içeriden bilgi** (5 gün trend okları, oyun başına bir kez); Ülkeler Savaşı: **+3 elmas**; Orman Muhafızı: **diriliş** (tur başına bir kez) |

- İki geçiş reklamı arasında en az **60 saniye** olur ve bu süre tüm butonlar için ortaktır. Ödüllü reklam izlendiğinde de bu süre yeniden başlar (art arda iki tam ekran reklam çıkmaz). Çok sık geçiş reklamı oyuncuyu kaçırır ve AdMob politikalarına takılabilir. Değiştirmek için: `ads/InterstitialAdManager.kt` → `MIN_INTERVAL_MS`.
- Reklam hazır değilse (internet yok, yüklenemedi) oyun beklemeden devam eder.
- Reklam gösterilirken Snake gibi gerçek zamanlı oyunlar kendiliğinden durur.
- Yeni bir oyuna geçiş reklamı eklemek tek satır: `val show = rememberInterstitialLauncher()` → `show { viewModel.restart() }`.

**Reklam ayarları (Unity LevelPlay)**

- Reklamlar Unity LevelPlay üzerinden gelir; Unity Ads ve AdMob her gösterimde yarışır, çok para vereni kazanır.
- Anahtarlar `gradle.properties` dosyasında: `LEVELPLAY_APP_KEY`, `LEVELPLAY_BANNER_ID`, `LEVELPLAY_INTERSTITIAL_ID`, `LEVELPLAY_REWARDED_ID`. `LEVELPLAY_APP_KEY` boşken release derlenmez (yanlışlıkla reklamsız sürüm çıkmasın).
- AdMob App ID manifest'e `app/build.gradle.kts` içinden giriyor (`admobAppId`); AdMob adaptörü bunu istiyor. AdMob reklam birimi ID'leri artık uygulamada değil, LevelPlay panelinde (Setup > SDK Networks) giriliyor.
- Test: LevelPlay panelinde telefonunu "Test devices" listesine ekle. Debug sürümde Ayarlar'ın en altında "Ad test suite" satırı adaptörleri tek tek dener. Kendi reklamına kendin tıklama.

## Ses efektleri ve titreşim

Dosyalar `res/raw/sfx_*.ogg` altında. RN projesindeki sesler OGG'ye çevrildi, toplamı yaklaşık 75 KB. Hepsi `feedback/GameFeedback.kt` üzerinden çalınır.

| Oyun | Olay | Ses | Titreşim |
|---|---|---|---|
| 2048 | Birleşmesiz kaydırma | drop (kısık) | – |
| | Birleşme | block | – |
| | 128+ taş oluşması | combo | hafif |
| | 2048'e ulaşma | clear | güçlü |
| | Oyun sonu | game over | güçlü |
| Bubble Shooter | Boş atış | drop (kısık) | – |
| | Patlatma | block | – |
| | 5+ patlatma ya da düşen balonlar | combo | hafif |
| | Tahta temizlendi | clear | güçlü |
| | Çizgiye ulaştı | game over | güçlü |
| Match 3 | 3'lü eşleşme | block | – |
| | 4'lü ya da 2. zincir | clear | hafif |
| | 5'li ya da 3+ zincir | combo | güçlü |
| | Geçersiz takas | swipe (kısık) | – |
| | Hamleler bitti | game over | güçlü |
| Stack | Blok bırakma | block | – |
| | Kusursuz bırakış | combo (3+ seride clear) | hafif |
| | Boşluğa düşme | game over | güçlü |
| Snake | Yem | drop | hafif |
| | Ölüm | game over | güçlü |
| Memory | Kart açma | swipe (kısık) | – |
| | Eşleşme | block | hafif |
| | Bitiş | clear | güçlü |
| Block Puzzle | Parça yerleştirme | block | hafif |
| | 1 satır/sütun temizleme | clear | hafif |
| | 2+ satır/sütun (kombo) | combo | güçlü |
| | Oyun sonu | game over | güçlü |

**Ayarlar** ekranındaki **Ses efektleri** ve **Titreşim** anahtarları bunları anında açıp kapatır. Emülatörde titreşim hissedilmez, gerçek telefonda denenmeli.

## Uygulama ikonu

- Uyarlanabilir (adaptive) ikon, vektör olarak: `res/drawable/ic_launcher_background.xml` (lacivert gradyan), `ic_launcher_foreground.xml` (oyun kolu, tuşlar uygulamanın dört renginde), `ic_launcher_monochrome.xml` (Android 13+ tema ikonları).
- Android 7 cihazlar için: `res/mipmap-nodpi/ic_launcher.png`.
- **Play Store mağaza ikonu** (512×512): `store/play_store_icon_512.png`. Play Console → **Mağaza varlığı → Ana mağaza girişi → Uygulama simgesi** bölümüne yüklenir; uygulama içindeki ikondan ayrı olarak güncellenmesi gerekir.

## Değişmemesi gerekenler

| Ayar | Değer | Neden |
|---|---|---|
| `applicationId` | `com.eomer91.MiniGamesWorldincesoy` | Farklı olursa Play Store yeni uygulama sayar |
| İmza anahtarı | Mevcut keystore | Güncelleme ancak aynı upload key ile yüklenebilir |
| `versionCode` | Her yüklemede +1 (şu an 7, versionName 2.1.0; mağazadaki eski RN sürümü 6 idi) | Play daha küçük ya da eşit versionCode'u kabul etmez |
| `targetSdk` | 36 | 31 Ağustos 2026'dan beri güncellemelerde zorunlu |
| Oyun id'leri (`block-puzzle`, `2048`…) | Sabit | Rekorlar bu anahtarlarla saklanır |

## Release imzası

Proje köküne `keystore.properties` adında bir dosya oluştur. Bu dosya git'e girmez:

```properties
storeFile=../keystore/upload-key.keystore
storePassword=...
keyAlias=...
keyPassword=...
```

`storeFile`, proje köküne göre yoldur. Keystore dosyasını repoya **koyma**, yedeğini güvenli bir yerde tut.

## Eski sürümden veri aktarımı

React Native sürümü sadece Block Puzzle rekorunu kaydediyordu (AsyncStorage → `RKStorage` SQLite). `LegacyDataMigrator`, güncellemeden sonraki ilk açılışta bu değeri okuyup yeni depoya aktarır. Eski dosyaya dokunmaz.

Bu aktarım sadece **release** paket adıyla test edilebilir, çünkü debug'ın paket adı farklı:

1. Telefonda mağaza sürümü (v3) kurulu olsun. Block Puzzle'da bir rekor yap.
2. Yeni sürümün AAB'sini Play Console → **Internal testing**'e yükle.
3. Test bağlantısından güncelle. Profil ekranında rekorun göründüğünü kontrol et.

## Yayınlama

1. `versionCode` değerini artır.
2. **Build → Generate Signed App Bundle** ile `.aab` üret (ya da `./gradlew bundleRelease` çalıştır).
3. Play Console'da önce Internal testing'e yükle, sonra Production'a yükselt.

## Yeni oyun eklemek / taşımak

1. Oyunu `ui/games/<oyun>/` altına yaz: `…Engine` (saf Kotlin + test), `…ViewModel`, `…Screen`.
2. Ortak parçaları kullan: `ScoreHeader`, `GameOverPanel`, `swipeGestures`, `rememberInterstitialLauncher`, `GameFeedback`.
3. `data/GameCatalog.kt` içinde oyunu `isPlayable = true` yap.
4. `ui/play/PlayScreen.kt` içindeki `when` bloğuna ekle.
5. Metinleri `res/values/strings.xml` ve çeviri dosyalarına koy.

## Yol haritası

- [x] İskelet: sekmeler, katalog, detay, altın/favori/rekor, dil, reklam + onay
- [x] Oyunları taşı: 2048, Snake, Memory Card, Block Puzzle
- [x] Geçiş reklamı (Play / Restart / Tekrar oyna)
- [x] Ses efektleri ve titreşim
- [ ] İlk sürüm: Internal testing → Production
- [x] Her 200 puan = 1 altın
- [x] Block Puzzle: ödüllü reklamla parça değiştirme ve geri alma
- [x] Altın yetmeyince ödüllü reklamla +50 altın
- [ ] Diğer diller: fr, es, de, it, ja, ru, da, hu …
- [ ] Yeni oyunlar: Match 3, Sudoku, Stack, Bubble Shooter, Tower Defense
- [ ] Google ile giriş, kendi sunucu (Ktor + WebSocket), online/multiplayer
- [ ] Google Play Billing (IAP)
