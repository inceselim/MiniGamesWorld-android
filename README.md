# Mini Games World (Android)

Play Store'daki **Mini Games Hub** (`com.eomer91.MiniGamesWorldincesoy`) uygulamasının Kotlin + Jetpack Compose ile yeniden yazılmış hali.
Eski React Native/Expo kodu referans için `MiniGamesWorldincesoy` reposunda duruyor.

## Teknoloji

- Kotlin 2.3, Jetpack Compose (Material 3), tek Activity
- MVVM + Hilt (KSP), Navigation Compose (type-safe route'lar)
- DataStore: altın, favoriler, rekorlar, ayarlar
- AdMob (banner + geçiş reklamı) + UMP (GDPR onay ekranı)
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
| Tower Defense | ⏳ Çok yakında | Detay ekranında altın harcanmaz, reklam gösterilmez |

Her oyunun kuralları ekrandan ayrı, saf Kotlin bir `…Engine` sınıfında durur ve `app/src/test` altında unit testleri vardır. Testleri çalıştırmak için test dosyasını açıp sınıf adının yanındaki yeşil oka bas ya da `./gradlew test` komutunu çalıştır.

Rekorlar oyun sırasında, rekor geçildiği anda kaydedilir. Oyuncu oyunun ortasında çıksa da rekor kaybolmaz.

## Altın (oyun parası)

- Başlangıç: 500 altın. Her oyuna giriş, detay ekranındaki ücret kadar altın harcar.
- **Her 200 puan = 1 altın.** Altın, puan 200'ün katlarını geçtiği anda verilir (oyunun ortasında çıksan da kaybolmaz). Oyun bitince panelde "+N altın kazandın" yazar.
- Memory Card'da puan oyun bitince hesaplandığı için altın da bitişte verilir (kusursuz oyun = 1000 puan = 5 altın).
- Hesap, o oyunda ulaşılan **en yüksek** puana göre yapılır. Block Puzzle'da geri alınan hamleler tekrar oynanınca aynı altın ikinci kez verilmez.
- **Altın yetmezse:** Detay ekranında Play'e basınca yeterli altın yoksa bir pencere açılır: "Reklam izle (+50)". Ödüllü reklam sonuna kadar izlenirse **50 altın** eklenir, oyuncu tekrar Play'e basarak oyuna girer.
- Kurallar tek yerde: `data/GameEconomy.kt` (`POINTS_PER_GOLD`, `AD_REWARD_GOLD`).

## Kaldığı yerden devam (Block Puzzle, 2048, Sudoku)

- Her hamleden sonra oyun (tahta, skor, o oyunda kazanılan altın; Block Puzzle'da ayrıca eldeki parçalar) cihaza kaydedilir. Oyundan çıkınca, uygulama kapansa bile kayıt durur.
- Detay ekranında kayıt varsa iki buton çıkar: **Kaldığın yerden devam et** (altın harcanmaz) ve **Yeni oyun · N altın** (kayıt silinir). Devam etmede de geçiş reklamı kuralı geçerlidir.
- Oyun bitince kayıt silinir, bir dahaki giriş yeni oyun olur.
- Block Puzzle'da geri alma hakkı kaydedilmez; devam edilen oyunda "Geri al" yeni bir hamleden sonra açılır.
- Kayıt formatları: `BlockPuzzleSave.kt`, `Game2048Save.kt`, `SudokuSave.kt` (JSON, sürüm numaralı). Bozuk ya da eski bir kayıt sessizce yok sayılır ve yeni oyun başlar.

## Sudoku

- **Akış:** Zorluk seçimi (Kolay 40 / Normal 32 / Zor 26 ipucu) → bulmaca cihazda üretilir (tek çözümlü, her seferinde farklı) → oyun.
- **Tahta:** Seçili hücre, aynı satır/sütun/kutu ve aynı rakamlar vurgulanır. Hazır rakamlar beyaz, doğru girilenler mavi, yanlışlar kırmızı.
- **Araçlar:** Sil, Not modu (doğru rakam girilince ilgili notlar otomatik silinir), İpucu (ödüllü reklam: seçili ya da ilk boş hücre doğru rakamla dolar).
- **3 hata** → oyun biter; ödüllü reklamla devam (hata 2'ye iner, yanlış rakamlar silinir) ya da yeni bulmaca.
- **Puan:** taban (600 / 1200 / 2400) − hata×100 − ipucu×150 − hedef süreyi (6 / 12 / 20 dk) aşan her saniye; en az tabanın 1/5'i. Altın oyun bitince verilir.
- **Süre** sadece ekran açıkken işler; arka planda ve reklam sırasında durur.
- Kayıt: her hamlede ve 10 saniyede bir (`SudokuSave.kt`).

## Block Puzzle ödüllü reklam

Tahtanın altında iki buton var, ikisi de ödüllü (rewarded) reklam izletir:

| Buton | Ödül |
|---|---|
| **Parçaları değiştir** | Eldeki 3 parça yenileriyle değişir. Oyun bittiyse ve yeni parçalardan biri sığıyorsa oyun devam eder. |
| **Geri al** | Son yerleştirme geri alınır (tahta, parçalar, skor). Tek seviye; yeni bir hamle yapılınca tekrar açılır. Oyun bittiğinde de kullanılabilir. |

Ödül, reklam sonuna kadar izlenip kapatıldığında verilir. Reklam hazır değilse "Reklam henüz hazır değil" mesajı çıkar ve hiçbir şey değişmez.

## Reklamlar

| Tür | Nerede |
|---|---|
| Banner | Ara, Favoriler, Profil ve Ayarlar sekmelerinin altında. Ana sayfada (Oyunlar) ve oyun ekranlarında yok. |
| Geçiş (Interstitial) | Oynanabilir bir oyunun detay ekranında **Play**, oyun içinde **Restart** ve **Tekrar oyna** |
| Ödüllü (Rewarded) | Altın yetmeyince **Reklam izle (+50 altın)**; Block Puzzle: **Parçaları değiştir**, **Geri al**; Sudoku: **İpucu**, **3 hatadan sonra devam**; Match 3: **+5 hamle**; Bubble Shooter: **3 sırayı temizle ve devam et** (ikisi de oyun başına bir kez) |

- İki geçiş reklamı arasında en az **60 saniye** olur ve bu süre tüm butonlar için ortaktır. Ödüllü reklam izlendiğinde de bu süre yeniden başlar (art arda iki tam ekran reklam çıkmaz). Çok sık geçiş reklamı oyuncuyu kaçırır ve AdMob politikalarına takılabilir. Değiştirmek için: `ads/InterstitialAdManager.kt` → `MIN_INTERVAL_MS`.
- Reklam hazır değilse (internet yok, yüklenemedi) oyun beklemeden devam eder.
- Reklam gösterilirken Snake gibi gerçek zamanlı oyunlar kendiliğinden durur.
- Yeni bir oyuna geçiş reklamı eklemek tek satır: `val show = rememberInterstitialLauncher()` → `show { viewModel.restart() }`.

**AdMob ayarları**

- App ID manifest'e `app/build.gradle.kts` içinden giriyor (`admobAppId`).
- Release reklam birimi ID'leri `gradle.properties` dosyasında: `ADMOB_BANNER_ID`, `ADMOB_INTERSTITIAL_ID`, `ADMOB_REWARDED_ID`. Boş bırakılan reklam release sürümde gösterilmez.
- Debug her zaman Google'ın test ID'lerini kullanır. Kendi reklamına kendin tıklama, AdMob hesabı kapanabilir.

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
| `versionCode` | Her yüklemede +1 (şu an 4) | Play daha küçük ya da eşit versionCode'u kabul etmez |
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

- [x] İskelet: sekmeler, katalog, detay, altın/favori/rekor, dil, AdMob + onay
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
