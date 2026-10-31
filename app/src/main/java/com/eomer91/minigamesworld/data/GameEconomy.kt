package com.eomer91.minigamesworld.data

/** Oyun ekonomisi kurallari tek yerde. */
object GameEconomy {
    /** Her bu kadar puan icin 1 altin. */
    const val POINTS_PER_GOLD = 100

    fun goldFor(score: Int): Int = (score / POINTS_PER_GOLD).coerceAtLeast(0)
}

/**
 * Bir oyun boyunca puan -> altin donusumunu takip eder. Altin, puan 100'un katlarini
 * gectikce aninda verilir (oyuncu oyunun ortasinda ciksa da kaybolmaz).
 * Oyun icinde ulasilan EN YUKSEK puana gore hesaplanir: geri alma (undo) ile
 * puan dusup tekrar yukselirse ayni altin ikinci kez verilmez.
 */
class GoldTracker {
    var awarded: Int = 0
        private set

    /** Yeni verilmesi gereken altin miktarini dondurur (0 olabilir). */
    fun onScore(score: Int): Int {
        val total = GameEconomy.goldFor(score)
        val delta = total - awarded
        if (delta <= 0) return 0
        awarded = total
        return delta
    }

    fun reset() {
        awarded = 0
    }
}
