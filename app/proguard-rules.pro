# Navigation type-safe route siniflari (kotlinx.serialization)
-keep @kotlinx.serialization.Serializable class com.eomer91.minigamesworld.** { *; }
-keepclassmembers class com.eomer91.minigamesworld.** {
    *** Companion;
}

# --- Room / WorkManager ---
# WorkManager (eskiden AdMob da kullaniyordu); WorkManager veritabanini Room ile kurar.
# Room, uretilen *_Impl sinifini adiyla (reflection) bulur. R8 bu sinifi ya da bos
# kurucusunu silerse uygulama acilista "Failed to create an instance of WorkDatabase" ile coker.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
