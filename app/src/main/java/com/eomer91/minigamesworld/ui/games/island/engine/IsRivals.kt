package com.eomer91.minigamesworld.ui.games.island.engine

/** Rekabet: reklam, iflas, oyun sonu. (Botlar ve kirli isler 2. adimda.) */
object IsRivals {

    /** Ust uste bu kadar ay eksi bakiyeyle kapanan sirket iflas eder */
    const val RED_LIMIT = 3

    /** 30 gunluk reklam kampanyasinin bedeli (sinif ve harita boyutuna gore) */
    fun adCost(w: IsWorld, cls: Int): Long {
        val base = longArrayOf(60_000, 120_000, 200_000)[cls]
        val size = when (w.settings.mapSize) { MapSize.SMALL -> 0.6; MapSize.MEDIUM -> 1.0; MapSize.LARGE -> 1.6 }
        return (base * size).toLong() / 1000 * 1000
    }

    fun startAd(w: IsWorld, c: Int, cls: Int, days: Int = 30): Boolean {
        val co = w.companies[c]
        val cost = adCost(w, cls) * days / 30
        if (co.balance < cost || !co.alive) return false
        co.book(Fin.ADS, cost)
        co.adUntil[cls] = maxOf(co.adUntil[cls], w.day) + days
        w.addNews(IsNews(w.day, NewsKind.AD, c, value = cls.toLong()))
        return true
    }

    fun monthEnd(w: IsWorld) {
        for (co in w.companies) {
            if (!co.alive) continue
            if (co.redMonths == RED_LIMIT - 1) w.addNews(IsNews(w.day, NewsKind.WARNING, co.id, value = co.balance))
            if (co.redMonths >= RED_LIMIT) goBankrupt(w, co)
        }
        checkGoal(w)
    }

    fun goBankrupt(w: IsWorld, co: Company) {
        co.bankrupt = true
        w.addNews(IsNews(w.day, NewsKind.BANKRUPT, co.id))
        // Binalar sahipsiz kalir (2. adimda acik artirmaya cikacak); simdilik kapali
        for (b in w.buildings) if (b != null && b.owner == co.id) b.closedDays = Int.MAX_VALUE / 2
    }

    fun checkGoal(w: IsWorld) {
        val alive = w.companies.filter { it.alive }
        val user = w.companies[0]
        when (w.settings.goal) {
            Goal.LAST_STANDING -> {
                if (!user.alive) { w.gameOver = true; w.winner = alive.maxByOrNull { IsSim.netWorth(w, it.id) }?.id ?: -1 }
                else if (alive.size == 1) { w.gameOver = true; w.winner = 0 }
            }
            Goal.RICHEST_20Y -> {
                if (!user.alive) { w.gameOver = true; w.winner = alive.maxByOrNull { IsSim.netWorth(w, it.id) }?.id ?: -1 }
                else if (w.day >= 20 * 360) { w.gameOver = true; w.winner = alive.maxBy { IsSim.netWorth(w, it.id) }.id }
            }
            Goal.ENDLESS -> if (!user.alive) { w.gameOver = true; w.winner = -1 }
        }
    }
}
