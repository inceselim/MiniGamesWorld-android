# Mini Games World (Android)

Play Store'daki `com.eomer91.MiniGamesWorldincesoy` uygulamasının Kotlin + Jetpack Compose ile yeniden yazılmış hali.
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
| Block Puzzle | ✅ Native | Sürükle-bırak, renkli parçalar, satır + sütun temizleme |
| 2048 | ✅ Native | Kaydırma, 2048'e ulaşınca devam |
| Snake | ✅ Native | İlk kaydırmada başlar, yem yedikçe hızlanır, arka planda durur |
| Memory Card | ✅ Native | 3D kart çevirme; hamle sayısı skora çevrilir (kusursuz = 1000) |
| Match 3, Sudoku, Stack, Bubble Shooter, Tower Defense | ⏳ Çok yakında | Detay ekranında altın harcanmaz, reklam gösterilmez |

Her oyunun kuralları ekrandan ayrı, saf Kotlin bir `…Engine` sınıfında durur ve `app/src/test` altında unit testleri vardır. Testleri çalıştırmak için test dosyasını açıp sınıf adının yanındaki yeşil oka bas ya da `./gradlew test` komutunu çalıştır.

Rekorlar oyun sırasında, rekor geçildiği anda kaydedilir. Oyuncu oyunun ortasında çıksa da rekor kaybolmaz.

## Altın (oyun parası)

- Başlangıç: 500 altın. Her oyuna giriş, detay ekranındaki ücret kadar altın harcar.
- **Her 100 puan = 1 altın.** Altın, puan 100'ün katlarını geçtiği anda verilir (oyunun ortasında çıksan da kaybolmaz). Oyun bitince panelde "+N altın kazandın" yazar.
- Memory Card'da puan oyun bitince hesaplandığı için altın da bitişte verilir (kusursuz oyun = 1000 puan = 10 altın).
- Hesap, o oyunda ulaşılan **en yüksek** puana göre yapılır. Block Puzzle'da geri alınan hamleler tekrar oynanınca aynı altın ikinci kez verilmez.
- Kural tek yerde: `data/GameEconomy.kt` (`POINTS_PER_GOLD`).

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
| Banner | Ana sekmelerin altında (Oyunlar, Ara, Favoriler, Profil, Ayarlar) |
| Geçiş (Interstitial) | Oynanabilir bir oyunun detay ekranında **Play**, oyun içinde **Restart** ve **Tekrar oyna** |
| Ödüllü (Rewarded) | Block Puzzle: **Parçaları değiştir**, **Geri al** |

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
- [x] Her 100 puan = 1 altın
- [x] Block Puzzle: ödüllü reklamla parça değiştirme ve geri alma
- [ ] Ödüllü reklamla altın kazanma (altın bitince)
- [ ] Diğer diller: fr, es, de, it, ja, ru, da, hu …
- [ ] Yeni oyunlar: Match 3, Sudoku, Stack, Bubble Shooter, Tower Defense
- [ ] Google ile giriş, kendi sunucu (Ktor + WebSocket), online/multiplayer
- [ ] Google Play Billing (IAP)
