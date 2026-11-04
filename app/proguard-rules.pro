# Navigation type-safe route siniflari (kotlinx.serialization)
-keep @kotlinx.serialization.Serializable class com.eomer91.minigamesworld.** { *; }
-keepclassmembers class com.eomer91.minigamesworld.** {
    *** Companion;
}

# --- Room / WorkManager ---
# AdMob arka planda WorkManager kullanir; WorkManager veritabanini Room ile kurar.
# Room, uretilen *_Impl sinifini adiyla (reflection) bulur. R8 bu sinifi ya da bos
# kurucusunu silerse uygulama acilista "Failed to create an instance of WorkDatabase" ile coker.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }

# --- Unity LevelPlay + adaptorler (Unity dokumanlarindaki kurallar) ---
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keep class com.google.android.gms.ads.** { public *; }
-keep class com.google.android.gms.appset.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.ironsource.**
-keepclassmembers class com.ironsource.** { public *; }
-keep public class com.ironsource.**
-keep class com.ironsource.adapters.** { *; }
-keep public interface com.ironsource.mediationsdk.sdk** { *; }
-keep public interface com.ironsource.mediationsdk.impressionData.ImpressionDataListener { *; }
-dontwarn com.iab.omid.**
-keep class com.iab.omid.** { *; }
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
# Unity Ads
-keep class com.unity3d.ads.** { *; }
-keep class com.unity3d.services.** { *; }
-dontwarn com.google.ar.core.**
-dontwarn com.unity3d.services.**
-dontwarn com.ironsource.adapters.unityads.**
