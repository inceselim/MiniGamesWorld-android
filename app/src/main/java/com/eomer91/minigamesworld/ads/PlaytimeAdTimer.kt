package com.eomer91.minigamesworld.ads

/**
 * Uzun suren oyunlarda (Ulkeler Savasi, Borsa Ustasi) belirli aktif oyun suresinde bir
 * gecis reklami icin sayac. Sadece oyun gercekten akarken sayilir (duraklatma/arka plan sayilmaz).
 * Reklam, oyunun dogal bir mola noktasinda (tur / gun degisimi) gosterilir; oyuncu bir seye
 * dokunurken aniden cikmaz.
 */
class PlaytimeAdTimer(private val intervalSeconds: Float = DEFAULT_INTERVAL_SECONDS) {
    private var played = 0f

    fun onPlay(dt: Float) {
        played += dt
    }

    /** Sure dolduysa true dondurur ve sayaci sifirlar. */
    fun consumeIfDue(): Boolean {
        if (played < intervalSeconds) return false
        played = 0f
        return true
    }

    fun reset() {
        played = 0f
    }

    companion object {
        const val DEFAULT_INTERVAL_SECONDS = 180f
    }
}
