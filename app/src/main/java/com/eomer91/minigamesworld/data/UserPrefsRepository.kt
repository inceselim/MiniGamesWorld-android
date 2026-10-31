package com.eomer91.minigamesworld.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Cihazda tutulan oyuncu verileri: altin, favoriler, rekorlar, ayarlar. */
@Singleton
class UserPrefsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val GOLD = intPreferencesKey("gold")
        val FAVORITES = stringSetPreferencesKey("favorites")
        val SOUND = booleanPreferencesKey("sound_enabled")
        val VIBRATION = booleanPreferencesKey("vibration_enabled")
        val LEGACY_MIGRATED = booleanPreferencesKey("legacy_migrated")
        fun highScore(gameId: String) = intPreferencesKey(HIGH_SCORE_PREFIX + gameId)
    }

    private val prefs: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val gold: Flow<Int> = prefs.map { it[Keys.GOLD] ?: STARTING_GOLD }
    val favorites: Flow<Set<String>> = prefs.map { it[Keys.FAVORITES] ?: emptySet() }
    val soundEnabled: Flow<Boolean> = prefs.map { it[Keys.SOUND] ?: true }
    val vibrationEnabled: Flow<Boolean> = prefs.map { it[Keys.VIBRATION] ?: true }

    val highScores: Flow<Map<String, Int>> = prefs.map { p ->
        p.asMap().entries
            .filter { (key, value) -> key.name.startsWith(HIGH_SCORE_PREFIX) && value is Int }
            .associate { (key, value) -> key.name.removePrefix(HIGH_SCORE_PREFIX) to value as Int }
    }

    fun highScore(gameId: String): Flow<Int> = prefs.map { it[Keys.highScore(gameId)] ?: 0 }

    /** Yeterli altin varsa duser ve true dondurur. */
    suspend fun spendGold(amount: Int): Boolean {
        var spent = false
        dataStore.edit { p ->
            val current = p[Keys.GOLD] ?: STARTING_GOLD
            spent = current >= amount
            if (spent) p[Keys.GOLD] = current - amount
        }
        return spent
    }

    suspend fun addGold(amount: Int) {
        dataStore.edit { p -> p[Keys.GOLD] = (p[Keys.GOLD] ?: STARTING_GOLD) + amount }
    }

    suspend fun toggleFavorite(gameId: String) {
        dataStore.edit { p ->
            val current = p[Keys.FAVORITES] ?: emptySet()
            p[Keys.FAVORITES] = if (gameId in current) current - gameId else current + gameId
        }
    }

    /** Skor rekordan buyukse kaydeder. Yeni rekor ise true dondurur. */
    suspend fun submitScore(gameId: String, score: Int): Boolean {
        var isRecord = false
        dataStore.edit { p ->
            val best = p[Keys.highScore(gameId)] ?: 0
            isRecord = score > best
            if (isRecord) p[Keys.highScore(gameId)] = score
        }
        return isRecord
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.SOUND] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.VIBRATION] = enabled }
    }

    suspend fun isLegacyMigrated(): Boolean = prefs.first()[Keys.LEGACY_MIGRATED] ?: false

    suspend fun completeLegacyMigration(highScores: Map<String, Int>) {
        dataStore.edit { p ->
            highScores.forEach { (gameId, score) ->
                val key = Keys.highScore(gameId)
                if (score > (p[key] ?: 0)) p[key] = score
            }
            p[Keys.LEGACY_MIGRATED] = true
        }
    }

    companion object {
        const val STARTING_GOLD = 500
        private const val HIGH_SCORE_PREFIX = "high_score_"
    }
}
