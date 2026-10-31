package com.eomer91.minigamesworld.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * React Native surumu verileri AsyncStorage ile "RKStorage" adli SQLite veritabanina
 * (tablo: catalystLocalStorage) yaziyordu. Guncelleme sonrasi bu dosya cihazda durur;
 * ilk acilista oradaki rekorlari okuyup DataStore'a aktaririz. Eski dosyaya dokunmayiz.
 */
class LegacyDataMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPrefsRepository,
) {
    suspend fun migrateIfNeeded() = withContext(Dispatchers.IO) {
        if (prefs.isLegacyMigrated()) return@withContext

        val scores = mutableMapOf<String, Int>()
        val dbFile = context.getDatabasePath(LEGACY_DB_NAME)
        if (dbFile.exists()) {
            runCatching {
                SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                    db.rawQuery("SELECT key, value FROM catalystLocalStorage", null).use { cursor ->
                        while (cursor.moveToNext()) {
                            val gameId = LEGACY_SCORE_KEYS[cursor.getString(0)] ?: continue
                            val score = cursor.getString(1)?.trim()?.toIntOrNull() ?: continue
                            scores[gameId] = score
                        }
                    }
                }
            }.onFailure { Log.w(TAG, "Legacy data could not be read", it) }
        }

        prefs.completeLegacyMigration(scores)
    }

    private companion object {
        const val TAG = "LegacyDataMigrator"
        const val LEGACY_DB_NAME = "RKStorage"

        /** AsyncStorage anahtari -> yeni oyun id'si */
        val LEGACY_SCORE_KEYS = mapOf(
            "block-puzzle-high-score" to "block-puzzle",
        )
    }
}
