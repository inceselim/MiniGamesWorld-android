package com.eomer91.minigamesworld.ui.games.island.engine

import kotlin.math.max

/** Kirli is turu */
enum class Dirty(val baseCost: Long, val success: Float, val caught: Float) {
    /** Rakip binayi birkac gun kapatir */
    SABOTAGE(80_000, 0.75f, 0.25f),
    /** Rakibin itibarini dusurur */
    PROPAGANDA(100_000, 0.8f, 0.2f),
    /** Rakibin binalarinin bir kismi grev yuzunden kapanir */
    STRIKE(150_000, 0.65f, 0.3f),
}

/** Iflas eden sirketin binasi icin acik artirma */
class IsAuction(val id: Int, val building: Int, val minBid: Long, val endDay: Int) {
    var topBid: Long = 0
    var topBidder: Int = -1
    var done: Boolean = false
}

/** Rekabet: reklam, kirli isler, iflas, acik artirma, oyun sonu. */
object IsRivals {

    /** Ust uste bu kadar ay eksi bakiyeyle kapanan sirket iflas eder */
    const val RED_LIMIT = 3
    /** Ayni sirketin iki kirli isi arasinda en az bu kadar gun */
    const val DIRTY_COOLDOWN = 30
    const val AUCTION_DAYS = 14

    private fun sizeMul(w: IsWorld) = when (w.settings.mapSize) { MapSize.SMALL -> 0.6; MapSize.MEDIUM -> 1.0; MapSize.LARGE -> 1.6 }

    // ------------------------------------------------------------------ reklam

    /** 30 gunluk reklam kampanyasinin bedeli (sinif ve harita boyutuna gore) */
    fun adCost(w: IsWorld, cls: Int): Long {
        val base = longArrayOf(60_000, 120_000, 200_000)[cls]
        return (base * sizeMul(w)).toLong() / 1000 * 1000
    }

    fun startAd(w: IsWorld, c: Int, cls: Int, days: Int = 30): Boolean {
        val co = w.companies[c]
        val cost = adCost(w, cls) * days / 30
        if (co.balance < cost || !co.alive) return false
        co.book(Fin.ADS, cost)
        co.adUntil[cls] = max(co.adUntil[cls], w.day) + days
        w.addNews(IsNews(w.day, NewsKind.AD, c, value = cls.toLong()))
        return true
    }

    // ------------------------------------------------------------------ kirli isler

    fun dirtyCost(w: IsWorld, d: Dirty): Long = (d.baseCost * sizeMul(w)).toLong() / 1000 * 1000

    /** Kirli is yapilabilir mi (bekleme suresi, para) */
    fun canDirty(w: IsWorld, actor: Int, d: Dirty): Boolean {
        val co = w.companies[actor]
        return co.alive && w.day - co.lastDirtyDay >= DIRTY_COOLDOWN && co.balance >= dirtyCost(w, d)
    }

    fun dirtyReadyIn(w: IsWorld, actor: Int): Int = max(0, DIRTY_COOLDOWN - (w.day - w.companies[actor].lastDirtyDay))

    /**
     * Kirli is. target: SABOTAGE icin bina id'si, diger ikisi icin sirket id'si.
     * Sonuc: 0 basarisiz, 1 basarili, 2 yakalandi (ceza + itibar kaybi).
     */
    fun doDirty(w: IsWorld, actor: Int, d: Dirty, target: Int): Int {
        if (!canDirty(w, actor, d)) return -1
        val co = w.companies[actor]
        val cost = dirtyCost(w, d)
        co.book(Fin.DIRTY, cost)
        co.lastDirtyDay = w.day
        val targetCo = when (d) {
            Dirty.SABOTAGE -> w.buildings.getOrNull(target)?.owner ?: return 0
            else -> target
        }
        if (targetCo < 0 || targetCo == actor || !w.companies[targetCo].alive) return 0
        val ok = w.rnd.chance(d.success)
        if (ok) when (d) {
            Dirty.SABOTAGE -> {
                val b = w.buildings[target] ?: return 0
                b.closedDays = max(b.closedDays, w.rnd.range(10, 20))
                w.addNews(IsNews(w.day, NewsKind.SABOTAGE, targetCo, type = b.type.id, value = b.id.toLong()))
            }
            Dirty.PROPAGANDA -> {
                val t = w.companies[targetCo]
                val loss = 8f + w.rnd.float() * 7f
                t.reputation = (t.reputation - loss).coerceAtLeast(0f)
                w.addNews(IsNews(w.day, NewsKind.PROPAGANDA, targetCo, value = loss.toLong()))
            }
            Dirty.STRIKE -> {
                var closed = 0
                for (b in w.buildings) if (b != null && b.owner == targetCo && b.type.cap > 0 && b.buildLeft <= 0 && w.rnd.chance(0.3f)) {
                    b.closedDays = max(b.closedDays, w.rnd.range(5, 10)); closed++
                }
                w.addNews(IsNews(w.day, NewsKind.STRIKE, targetCo, value = closed.toLong()))
            }
        }
        // Yakalanma (basarili olsa da olmasa da)
        if (w.rnd.chance(if (ok) d.caught else d.caught * 1.5f)) {
            val fine = cost * 3
            co.book(Fin.FINES, fine)
            co.reputation = (co.reputation - 15f).coerceAtLeast(0f)
            w.addNews(IsNews(w.day, NewsKind.CAUGHT, actor, other = targetCo, value = fine, type = d.ordinal))
            return 2
        }
        return if (ok) 1 else 0
    }

    // ------------------------------------------------------------------ iflas ve acik artirma

    fun monthEnd(w: IsWorld) {
        for (co in w.companies) {
            if (!co.alive) continue
            val deep = co.balance < -overdraft(w, co.id)
            if (co.redMonths == RED_LIMIT - 1 && deep) w.addNews(IsNews(w.day, NewsKind.WARNING, co.id, value = co.balance))
            if (co.redMonths >= RED_LIMIT && deep) goBankrupt(w, co)
        }
        checkGoal(w)
    }

    /** Bankanin izin verdigi eksi bakiye: bunun altinda 3 ay kalan sirket iflas eder */
    fun overdraft(w: IsWorld, c: Int): Long = maxOf(250_000L, IsSim.assets(w, c) / 10)

    /** Sirket batar: binalari acik artirmaya cikar, bos arsalari belediyeye doner, borclari silinir */
    fun goBankrupt(w: IsWorld, co: Company) {
        co.bankrupt = true
        co.loanLeft = 0; co.loanMonthsLeft = 0; co.loanPayment = 0
        w.addNews(IsNews(w.day, NewsKind.BANKRUPT, co.id))
        for (i in w.owner.indices) if (w.owner[i] == co.id && w.bAt[i] < 0) w.owner[i] = -1
        for (b in w.buildings) {
            if (b == null || b.owner != co.id) continue
            if (b.type.cap == 0) { IsSim.demolish(w, b); clearLand(w, b); continue } // park, palmiye, yol
            b.closedDays = Int.MAX_VALUE / 2
            val min = (IsSim.buildingValue(b) * 0.4).toLong() / 1000 * 1000
            w.auctions.add(IsAuction(w.auctions.size, b.id, max(10_000L, min), w.day + AUCTION_DAYS))
        }
        if (w.auctions.any { !it.done }) w.addNews(IsNews(w.day, NewsKind.AUCTION, co.id, value = w.auctions.count { !it.done }.toLong()))
    }

    private fun clearLand(w: IsWorld, b: Building) {
        for (yy in b.y until b.y + b.type.h) for (xx in b.x until b.x + b.type.w) w.owner[w.idx(xx, yy)] = -1
    }

    fun nextBid(a: IsAuction): Long = if (a.topBidder < 0) a.minBid else (a.topBid * 1.05).toLong() / 1000 * 1000 + 1000

    fun bid(w: IsWorld, c: Int, a: IsAuction, amount: Long): Boolean {
        if (a.done || w.day > a.endDay || amount < nextBid(a) || a.topBidder == c) return false
        val co = w.companies[c]
        if (!co.alive || co.balance < amount) return false
        a.topBid = amount; a.topBidder = c
        // Son dakika teklifi sureyi biraz uzatir
        if (a.endDay - w.day < 2) return true
        return true
    }

    /** Gunluk: suresi dolan acik artirmalari sonuclandir */
    fun dailyAuctions(w: IsWorld) {
        for (a in w.auctions) {
            if (a.done || w.day < a.endDay) continue
            a.done = true
            val b = w.buildings[a.building] ?: continue
            val winner = a.topBidder
            if (winner >= 0 && w.companies[winner].alive && w.companies[winner].balance >= a.topBid) {
                w.companies[winner].book(Fin.BUILD, a.topBid)
                b.owner = winner
                b.closedDays = 0
                b.price = b.type.price
                for (yy in b.y until b.y + b.type.h) for (xx in b.x until b.x + b.type.w) w.owner[w.idx(xx, yy)] = winner
                IsSim.refreshLocation(w, b)
                w.addNews(IsNews(w.day, NewsKind.AUCTION_WON, winner, value = a.topBid, type = b.type.id))
            } else {
                // Alici yok: bina yikilir, arsa belediyeye doner
                IsSim.demolish(w, b, refund = false)
                clearLand(w, b)
            }
        }
        if (w.auctions.size > 200 && w.auctions.all { it.done }) w.auctions.clear()
    }

    // ------------------------------------------------------------------ oyun sonu

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
