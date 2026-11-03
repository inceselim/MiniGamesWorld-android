package com.eomer91.minigamesworld.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val MUSIC = booleanPreferencesKey("music_enabled")
        val LEGACY_MIGRATED = booleanPreferencesKey("legacy_migrated")
        fun highScore(gameId: String) = intPreferencesKey(HIGH_SCORE_PREFIX + gameId)
        fun savedGame(gameId: String) = stringPreferencesKey("saved_game_$gameId")
    }

    private val prefs: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val gold: Flow<Int> = prefs.map { it[Keys.GOLD] ?: STARTING_GOLD }
    val favorites: Flow<Set<String>> = prefs.map { it[Keys.FAVORITES] ?: emptySet() }
    val soundEnabled: Flow<Boolean> = prefs.map { it[Keys.SOUND] ?: true }
    val vibrationEnabled: Flow<Boolean> = prefs.map { it[Keys.VIBRATION] ?: true }
    val musicEnabled: Flow<Boolean> = prefs.map { it[Keys.MUSIC] ?: true }

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

    suspend fun setMusicEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.MUSIC] = enabled }
    }

    /** Yarim kalan oyunun kaydi (JSON). Oyun kendi formatini belirler. */
    fun savedGame(gameId: String): Flow<String?> = prefs.map { it[Keys.savedGame(gameId)] }

    fun hasSavedGame(gameId: String): Flow<Boolean> = savedGame(gameId).map { it != null }

    suspend fun saveGame(gameId: String, json: String) {
        dataStore.edit { it[Keys.savedGame(gameId)] = json }
    }

    suspend fun clearSavedGame(gameId: String) {
        dataStore.edit { it.remove(Keys.savedGame(gameId)) }
    }

    /** Tower Defense kalici yukseltme seviyeleri (anahtar: yukseltme adi). */
    val towerUpgradeLevels: Flow<Map<String, Int>> = prefs.map { p ->
        p.asMap().entries
            .filter { (key, value) -> key.name.startsWith(TOWER_UPGRADE_PREFIX) && value is Int }
            .associate { (key, value) -> key.name.removePrefix(TOWER_UPGRADE_PREFIX) to value as Int }
    }

    /** Altinla kalici yukseltme: tek islemde altini duser ve seviyeyi artirir. Basarisizsa false. */
    suspend fun buyTowerUpgrade(name: String, cost: Int, maxLevel: Int): Boolean {
        var bought = false
        dataStore.edit { p ->
            val gold = p[Keys.GOLD] ?: STARTING_GOLD
            val key = intPreferencesKey(TOWER_UPGRADE_PREFIX + name)
            val level = p[key] ?: 0
            bought = gold >= cost && level < maxLevel
            if (bought) {
                p[Keys.GOLD] = gold - cost
                p[key] = level + 1
            }
        }
        return bought
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
        private const val TOWER_UPGRADE_PREFIX = "td_perm_"
    }
}
