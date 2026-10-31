# Mini Games World (Android)

Play Store'daki `com.eomer91.MiniGamesWorldincesoy` uygulamasının Kotlin + Jetpack Compose ile yeniden yazılmış hali.
Eski React Native/Expo kodu referans için `MiniGamesWorldincesoy` reposunda duruyor.

## Teknoloji

- Kotlin, Jetpack Compose (Material 3), tek Activity
- MVVM + Hilt (DI), Navigation Compose (type-safe route'lar)
- DataStore: altın, favoriler, rekorlar, ayarlar
- AdMob + UMP (GDPR onay ekranı)
- Uygulama içi dil seçimi (AppCompat per-app locales)

## Projeyi açma

1. Android Studio'da **Open** ile bu klasörü aç, Gradle senkronizasyonunu bekle.
2. Android Studio kütüphaneler için daha yeni sürüm önerirse (sarı uyarılar) güncelleyebilirsin.
3. `app` konfigürasyonunu bir emülatörde ya da telefonda çalıştır.

Debug build `com.eomer91.MiniGamesWorldincesoy.debug` paket adıyla kurulur. Böylece telefondaki mağaza sürümüyle yan yana durur ve Google'ın test reklamlarını gösterir.

## Değişmemesi gerekenler

| Ayar | Değer | Neden |
|---|---|---|
| `applicationId` | `com.eomer91.MiniGamesWorldincesoy` | Farklı olursa Play Store yeni uygulama sayar |
| İmza anahtarı | Mevcut keystore | Güncelleme ancak aynı upload key ile yüklenebilir |
| `versionCode` | Her yüklemede +1 (şu an 4) | Play daha küçük ya da eşit versionCode'u kabul etmez |
| `targetSdk` | 36 | 31 Ağustos 2026'dan beri güncellemelerde zorunlu |

## Release imzası

Proje köküne `keystore.properties` adında bir dosya oluştur. Bu dosya git'e girmez:

```properties
storeFile=../keystore/upload-key.keystore
storePassword=...
keyAlias=...
keyPassword=...
```

`storeFile`, proje köküne göre yoldur. Keystore dosyasını repoya **koyma**, yedeğini güvenli bir yerde tut.

Expo projesinde bu bilgiler genelde `android/app/` altındaki `.keystore` dosyasında ve `android/gradle.properties` içindeki `MYAPP_UPLOAD_*` satırlarında bulunur.

## AdMob

- App ID manifest'e `app/build.gradle.kts` içinden giriyor (`admobAppId`).
- Release reklam birimi ID'lerini `gradle.properties` dosyasındaki `ADMOB_BANNER_ID`, `ADMOB_INTERSTITIAL_ID` ve `ADMOB_REWARDED_ID` satırlarına yaz. Boş kalırsa release'de reklam gösterilmez.
- Debug her zaman test ID'lerini kullanır. Kendi reklamına kendin tıklama, AdMob hesabı kapanabilir.

## Eski sürümden veri aktarımı

React Native sürümü sadece Block Puzzle rekorunu kaydediyordu (AsyncStorage → `RKStorage` SQLite). `LegacyDataMigrator`, güncellemeden sonraki ilk açılışta bu değeri okuyup yeni depoya aktarır. Eski dosyaya dokunmaz.

Bu aktarım sadece **release** paket adıyla test edilebilir, çünkü debug'ın paket adı farklı. Test için:

1. Telefonda mağaza sürümü (v3) kurulu olsun. Block Puzzle'da bir rekor yap.
2. Yeni sürümün AAB'sini Play Console → **Internal testing**'e yükle.
3. Test bağlantısından güncelle. Profil ekranında rekorun göründüğünü kontrol et.

## Yayınlama

1. `versionCode` değerini artır.
2. **Build → Generate Signed App Bundle** ile `.aab` üret (ya da `./gradlew bundleRelease` çalıştır).
3. Play Console'da önce Internal testing'e yükle, sonra Production'a yükselt.

## Yeni oyun eklemek / taşımak

1. `data/GameCatalog.kt` içindeki oyunu `isPlayable = true` yap.
2. Oyunu `ui/games/<oyun>/` altına yaz: motor (saf Kotlin, test edilebilir) + Compose ekranı.
3. `ui/play/PlayScreen.kt` içindeki `when` bloğuna ekle.
4. Metinleri `res/values/strings.xml` ve çeviri dosyalarına koy.

## Yol haritası

- [x] İskelet: sekmeler, katalog, detay, altın/favori/rekor, dil, AdMob + onay
- [ ] Oyunları taşı: 2048 → Snake → Memory Card → Block Puzzle
- [ ] Ödüllü reklamla altın kazanma, oyun sonu geçiş reklamı
- [ ] Ses ve titreşim
- [ ] Diğer diller: fr, es, de, it, ja, ru, da, hu …
- [ ] Yeni oyunlar: Match 3, Sudoku, Stack, Bubble Shooter, Tower Defense
- [ ] Google ile giriş, kendi sunucu (Ktor + WebSocket), online/multiplayer
- [ ] Google Play Billing (IAP)
