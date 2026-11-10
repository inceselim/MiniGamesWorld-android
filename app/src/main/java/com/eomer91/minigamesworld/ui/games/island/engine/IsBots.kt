package com.eomer91.minigamesworld.ui.games.island.engine

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Bot sirketler. Her bot haftada bir dusunur: fiyatlar, yatirim (tur + yer secimi), kredi, reklam,
 * kirli isler, acik artirma teklifleri, zor durumda kucultme.
 */
object IsBots {

    /** Kisilik basina bina tercih agirliklari (bina anahtari -> agirlik) */
    private val PREFS: Map<Personality, Map<String, Float>> = mapOf(
        Personality.LUXURY to mapOf("hotel4" to 1.6f, "resort" to 1.6f, "villa" to 1.4f, "bungalow" to 1f, "finedining" to 1.4f,
            "casino" to 1.3f, "beachclub" to 1.2f, "mall" to 1f, "restaurant" to 0.8f, "waterpark" to 0.7f, "hotel3" to 0.6f),
        Personality.BUDGET to mapOf("tent" to 1.5f, "pension" to 1.5f, "hotel3" to 1.1f, "kiosk" to 1.4f, "fastfood" to 1.4f,
            "market" to 1.2f, "arcade" to 1.1f, "pub" to 1.1f, "souvenir" to 1f, "beachclub" to 0.8f),
        Personality.FUN to mapOf("disco" to 1.6f, "pub" to 1.4f, "arcade" to 1.3f, "funfair" to 1.4f, "waterpark" to 1.3f,
            "bungalow" to 1f, "hotel3" to 1f, "fastfood" to 1f, "pension" to 0.8f, "beachclub" to 1f, "casino" to 0.8f),
        Personality.AGGRESSIVE to mapOf("hotel3" to 1.4f, "disco" to 1.2f, "fastfood" to 1.2f, "restaurant" to 1.1f, "pub" to 1.1f,
            "pension" to 1f, "hotel4" to 1f, "market" to 1f, "kiosk" to 1f, "arcade" to 0.9f, "casino" to 0.8f),
        Personality.BALANCED to mapOf("pension" to 1f, "bungalow" to 1f, "hotel3" to 1.1f, "hotel4" to 1f, "kiosk" to 0.9f,
            "fastfood" to 1f, "restaurant" to 1f, "pub" to 0.9f, "souvenir" to 0.9f, "market" to 0.9f, "beachclub" to 1f,
            "arcade" to 0.8f, "disco" to 0.7f, "tent" to 0.7f),
    )

    fun daily(w: IsWorld) {
        if (w.botsPaused) return
        for (co in w.companies) {
            if (!co.bot || !co.alive) continue
            if ((w.day + co.id * 2) % 7 != 0) continue
            think(w, co)
        }
        // Acik artirmalara teklif her gun
        for (a in w.auctions) if (!a.done) for (co in w.companies) if (co.bot && co.alive) considerBid(w, co, a)
    }

    private fun monthlyUpkeep(w: IsWorld, c: Int): Long {
        var u = 0L
        for (b in w.buildings) if (b != null && b.owner == c) u += b.type.upkeep
        return u
    }

    private fun reserve(w: IsWorld, co: Company) = monthlyUpkeep(w, co.id) * 3 + co.loanPayment * 3 + 150_000

    fun think(w: IsWorld, co: Company) {
        adjustPrices(w, co)
        if (co.balance < 0) { survive(w, co); return }
        // Borcu erken kapat (zenginse)
        if (co.loanMonthsLeft > 0 && co.balance > IsSim.repayCost(co) + reserve(w, co) * 3) IsSim.repayLoan(w, co.id)
        invest(w, co)
        fixRoads(w, co)
        maybeAdvertise(w, co)
        maybeDirty(w, co)
    }

    // ------------------------------------------------------------------ yollar

    /** Binayi yol agina bagla (para yetiyorsa) */
    private fun connectRoad(w: IsWorld, co: Company, b: Building) {
        val path = IsSim.roadPath(w, co.id, b, 40) ?: return
        if (path.isEmpty()) return
        if (IsSim.roadPathCost(w, co.id, path) <= co.balance - 50_000) { IsSim.buildRoadPath(w, co.id, path); return }
        // Tam baglantiya para yetmiyor: en azindan binanin dibine tek yol karosu (yolsuz cezadan kurtulur)
        if (b.road == 0 && co.balance > 60_000) IsSim.buildRoadPath(w, co.id, listOf(path.last()))
    }

    /** Yol agina bagli olmayan bir binayi duzelt (her dusunmede en fazla bir) */
    private fun fixRoads(w: IsWorld, co: Company) {
        if (co.balance < 100_000) return
        val cands = w.buildings.filter { it != null && it.owner == co.id && it.type.cat != Cat.ROAD && it.type.cat != Cat.NATURE && it.road < 2 }
        if (cands.isEmpty()) return
        // Ulasilamayan bir binaya takilip kalmamak icin rastgele bir aday
        connectRoad(w, co, cands[w.rnd.int(cands.size)]!!)
    }

    // ------------------------------------------------------------------ fiyat

    private fun occupancy(b: Building): Float {
        val cap = b.capacity.toFloat().coerceAtLeast(1f)
        return if (b.lastMonthUse > 0f) b.lastMonthUse / (cap * 30f) else (b.today[0] + b.today[1] + b.today[2]) / cap
    }

    private fun adjustPrices(w: IsWorld, co: Company) {
        for (b in w.buildings) {
            if (b == null || b.owner != co.id || b.type.cap == 0 || !b.open) continue
            val occ = (b.today[0] + b.today[1] + b.today[2]) / b.capacity.toFloat().coerceAtLeast(1f)
            val full = b.type.varCost + b.type.upkeep / (b.capacity * 30f * 0.6f)
            val floor = when (co.personality) {
                Personality.LUXURY -> b.type.price * 0.85f
                Personality.BUDGET, Personality.AGGRESSIVE -> b.type.varCost * 1.05f
                else -> full
            }
            val ceil = b.type.price * when (co.personality) { Personality.LUXURY -> 1.6f; Personality.BUDGET -> 1.3f; else -> 1.6f }
            var p = b.price
            // Konaklamada yuksek doluluk hedeflenir; amenity'lerde kapasite genis oldugundan hedef dusuk
            val stay = b.type.cat == Cat.STAY
            val hi = if (stay) 0.95f else 0.6f; val mid = if (stay) 0.85f else 0.4f
            val lo = if (stay) 0.45f else 0.08f; val lo2 = if (stay) 0.65f else 0.18f
            if (occ > hi) p *= 1.06f
            else if (occ > mid) p *= 1.02f
            else if (occ < lo) p *= 0.95f
            else if (occ < lo2) p *= 0.99f
            // Kar etmeyen binada fiyati maliyetin altina indirme
            if (!stay && b.lastMonthRevenue in 1 until b.type.upkeep) p = max(p, full)
            // Fiyat savasi: yakindaki ayni kategoride ucuz rakip varsa (agresif ve ucuzcu karsilik verir)
            if ((co.personality == Personality.AGGRESSIVE || co.personality == Personality.BUDGET) && occ < 0.7f) {
                val rival = cheapestRival(w, b, 15)
                if (rival != null && rival.price < p * 0.85f) {
                    val newP = max(floor, rival.price * 0.95f)
                    if (newP < p * 0.9f && w.rnd.chance(0.3f)) w.addNews(IsNews(w.day, NewsKind.PRICE_WAR, co.id, other = rival.owner, type = b.type.id))
                    p = min(p, newP)
                }
            }
            b.price = p.coerceIn(floor.coerceAtMost(ceil), ceil)
            IsSim.setPrice(b, b.price)
        }
    }

    private fun cheapestRival(w: IsWorld, b: Building, r: Int): Building? {
        var best: Building? = null
        for (o in w.buildings) {
            if (o == null || o.owner < 0 || o.owner == b.owner || o.type.cat != b.type.cat || !o.open) continue
            if (max(abs(o.cx - b.cx), abs(o.cy - b.cy)) > r) continue
            if (best == null || o.price / o.type.price < best.price / best.type.price) best = o
        }
        return best
    }

    // ------------------------------------------------------------------ yatirim

    /** Pazardaki bosluk: konaklamada yer bulamayan turist, amenity'de kapasiteye gore talep */
    private fun gapScore(w: IsWorld, t: BType): Float {
        return when {
            t.cat == Cat.STAY -> {
                // Bugunku eksik yatak, yillik ortalama sezona gore (yaz zirvesinde asiri insaati onler)
                val adj = 0.85f / IsSim.SEASON[w.month]
                var g = 0f
                for (c in 0..2) g += t.appeal[c] * w.lost[c] * adj * TClass.entries[c].stayBudget / 100f
                g / max(1f, t.cap.toFloat()) * 10f
            }
            t.cat.isAmenity -> {
                var demand = 0f
                for (c in 0..2) demand += w.present[c] * t.cat.visitRate!![c] * t.appeal[c] * t.cat.visitBudget!![c] / 20f
                var cap = 0f
                for (b in w.buildings) if (b != null && b.owner >= 0 && b.type.cat == t.cat && w.companies.getOrNull(b.owner)?.alive != false) cap += b.capacity * b.price / 20f
                ((demand - cap * 0.8f) / max(1f, t.cap * t.price / 20f)).coerceIn(-1f, 6f)
            }
            else -> 0f
        }
    }

    private fun invest(w: IsWorld, co: Company) {
        val budget = co.balance - reserve(w, co)
        if (budget < 100_000) return
        val prefs = PREFS[co.personality]!!
        // Tur secimi: tercih x bosluk, butceye siganlar
        val options = ArrayList<Pair<BType, Float>>()
        for ((key, pref) in prefs) {
            val t = IsCatalog.types.first { it.key == key }
            val gap = gapScore(w, t)
            if (gap <= 0.1f) continue
            // Bosluk var ama para yetmiyor: buyumek icin kredi
            if (t.cost * 1.25 > budget) { if (gap > 1f) maybeLoan(w, co); continue }
            options.add(t to pref * (0.3f + gap) * (0.7f + 0.6f * w.rnd.float()))
        }
        if (options.isEmpty()) {
            // Guzellestirme: binalarin yanina park/palmiye (sinirli)
            val stays = w.buildings.count { it != null && it.owner == co.id && it.type.cat == Cat.STAY }
            val deco = w.buildings.count { it != null && it.owner == co.id && it.type.cat == Cat.NATURE }
            if (budget > 200_000 && deco < stays * 2 && w.rnd.chance(0.3f)) decorate(w, co)
            return
        }
        val t = options.maxBy { it.second }.first
        val spot = findSpot(w, co, t) ?: return
        val b = IsSim.build(w, co.id, t, spot.first, spot.second) ?: return
        connectRoad(w, co, b)
    }

    private fun decorate(w: IsWorld, co: Company) {
        val own = w.buildings.filter { it != null && it.owner == co.id && it.type.cat == Cat.STAY }
        if (own.isEmpty()) return
        val h = own[w.rnd.int(own.size)]!!
        val t = if (w.rnd.chance(0.4f)) IsCatalog.types.first { it.key == "park" } else IsCatalog.types.first { it.key == "palm" }
        for (k in 0 until 20) {
            val x = h.x - 3 + w.rnd.int(h.type.w + 6); val y = h.y - 3 + w.rnd.int(h.type.h + 6)
            if (IsSim.canBuild(w, co.id, t, x, y) == BuildError.OK) { IsSim.build(w, co.id, t, x, y); return }
        }
    }

    /** Aday karolardan en iyisini sec */
    fun findSpot(w: IsWorld, co: Company, t: BType): Pair<Int, Int>? {
        val stays = w.buildings.filter { it != null && it.owner >= 0 && it.type.cat == Cat.STAY && it.open }.map { it!! }
        val own = w.buildings.filter { it != null && it.owner == co.id }.map { it!! }
        var best: Pair<Int, Int>? = null; var bestS = -1e9
        val samples = 260
        for (k in 0 until samples) {
            // Yarisi kendi binalarinin cevresinden (komsuluk), yarisi rastgele
            val (x, y) = if (own.isNotEmpty() && k % 2 == 0) {
                val o = own[w.rnd.int(own.size)]
                (o.x - 6 + w.rnd.int(o.type.w + 12)) to (o.y - 6 + w.rnd.int(o.type.h + 12))
            } else w.rnd.int(w.n) to w.rnd.int(w.n)
            if (IsSim.canBuild(w, co.id, t, x, y) != BuildError.OK) continue
            val s = scoreSpot(w, co, t, x, y, stays) - IsSim.buildCost(w, co.id, t, x, y) / max(1.0, t.cost.toDouble()) * 0.8
            if (s > bestS) { bestS = s; best = x to y }
        }
        return best
    }

    private fun scoreSpot(w: IsWorld, co: Company, t: BType, x: Int, y: Int, stays: List<Building>): Double {
        val cx = x + t.w / 2f; val cy = y + t.h / 2f
        var minWd = 99
        for (yy in y until y + t.h) for (xx in x until x + t.w) minWd = min(minWd, w.waterDist[w.idx(xx, yy)].toInt())
        return if (t.cat == Cat.STAY) {
            val luxury = t.appeal[2] > 0.5f
            var s = when { minWd <= 2 -> 1.2; minWd <= 5 -> 0.7; minWd <= 9 -> 0.3; else -> 0.0 } * (if (luxury) 1.5 else 1.0)
            // Yakindaki amenity'ler (iyi) ve gurultu (luks icin kotu)
            for (b in w.buildings) {
                if (b == null || b.owner < 0) continue
                val d = max(abs(b.cx - cx), abs(b.cy - cy))
                if (b.type.cat.isAmenity && d <= 12) s += 0.05
                if (luxury && d <= 5) s -= b.type.noise * 0.5
                if (b.type.cat == Cat.STAY && b.owner != co.id && d <= 4) s -= 0.15
            }
            s + w.rnd.float() * 0.2
        } else if (t.cat.isAmenity) {
            // Yurume mesafesindeki misafirler; ayni kategorideki rakip kapasite
            var guests = 0.0
            for (h in stays) {
                val d = max(abs(h.cx - cx), abs(h.cy - cy))
                if (d > IsSim.RADIUS) continue
                var g = 0.0
                for (c in 0..2) g += h.today[c] * t.appeal[c]
                guests += g * exp(-IsSim.DIST_K * d)
            }
            var comp = 0.0
            for (b in w.buildings) {
                if (b == null || b.owner < 0 || b.type.cat != t.cat) continue
                val d = max(abs(b.cx - cx), abs(b.cy - cy))
                if (d <= 10) comp += b.capacity * exp(-IsSim.DIST_K * d)
            }
            var s = guests / 100.0 - comp / 800.0
            if (t.needsBeach) s += 0.3
            // Gurultulu binayi rakip luks otellerin yanina koymak (agresif)
            if (co.personality == Personality.AGGRESSIVE && t.noise > 0.3f) for (h in stays)
                if (h.owner != co.id && h.type.appeal[2] > 0.5f && max(abs(h.cx - cx), abs(h.cy - cy)) <= 5) s += 0.4
            s + w.rnd.float() * 0.1
        } else 0.0
    }

    // ------------------------------------------------------------------ kredi, reklam, kirli is

    private fun maybeLoan(w: IsWorld, co: Company) {
        if (co.loanMonthsLeft > 0 || co.balance < 0) return
        // Karli bir sirketse buyumek icin kredi
        val profitable = co.lastMonthIn > co.lastMonthOut * 1.1
        val appetite = when (co.personality) { Personality.AGGRESSIVE -> 0.8f; Personality.LUXURY -> 0.6f; Personality.BUDGET -> 0.3f; else -> 0.5f }
        if (!profitable && w.day > 60) return
        if (!w.rnd.chance(appetite * 0.5f)) return
        val lim = IsSim.creditLimit(w, co.id)
        if (lim < 300_000) return
        val years = if (co.personality == Personality.AGGRESSIVE) 3 else 2
        IsSim.takeLoan(w, co.id, (lim * appetite).toLong() / 10_000 * 10_000, years)
    }

    private fun maybeAdvertise(w: IsWorld, co: Company) {
        val prob = when (co.personality) { Personality.FUN -> 0.12f; Personality.AGGRESSIVE -> 0.1f; Personality.LUXURY -> 0.08f; else -> 0.05f }
        if (!w.rnd.chance(prob)) return
        // En cok bos yatagi olan sinifa reklam
        val free = FloatArray(3)
        for (b in w.buildings) if (b != null && b.owner == co.id && b.type.cat == Cat.STAY && b.open) {
            val left = b.capacity - (b.today[0] + b.today[1] + b.today[2])
            for (c in 0..2) free[c] += left * b.type.appeal[c]
        }
        val cls = free.indices.maxBy { free[it] }
        if (free[cls] < 20f) return
        val cost = IsRivals.adCost(w, cls)
        if (co.balance > cost * 4 + reserve(w, co) && co.adUntil[cls] < w.day) IsRivals.startAd(w, co.id, cls)
    }

    private fun maybeDirty(w: IsWorld, co: Company) {
        val prob = when (co.personality) { Personality.AGGRESSIVE -> 0.18f; Personality.FUN -> 0.04f; Personality.BUDGET -> 0.03f; else -> 0.015f }
        if (!w.rnd.chance(prob)) return
        // Hedef: en zengin rakip (oyuncu dahil)
        val rivals = w.companies.filter { it.alive && it.id != co.id }
        if (rivals.isEmpty()) return
        val target = rivals.maxBy { IsSim.netWorth(w, it.id) }
        val d = Dirty.entries[w.rnd.int(3)]
        if (!IsRivals.canDirty(w, co.id, d) || co.balance < IsRivals.dirtyCost(w, d) * 4) return
        val arg = if (d == Dirty.SABOTAGE) {
            val bs = w.buildings.filter { it != null && it.owner == target.id && it.open && it.type.cap > 0 }
            if (bs.isEmpty()) return
            bs.maxBy { it!!.lastMonthRevenue }!!.id
        } else target.id
        IsRivals.doDirty(w, co.id, d, arg)
    }

    private fun considerBid(w: IsWorld, co: Company, a: IsAuction) {
        if (a.topBidder == co.id) return
        val b = w.buildings[a.building] ?: return
        val value = IsSim.buildingValue(b)
        val maxPay = (value * when (co.personality) { Personality.AGGRESSIVE -> 0.9; Personality.BUDGET -> 0.6; else -> 0.75 }).toLong()
        val next = IsRivals.nextBid(a)
        if (next > maxPay || co.balance - next < reserve(w, co)) return
        if (!w.rnd.chance(0.35f)) return
        IsRivals.bid(w, co.id, a, next)
    }

    // ------------------------------------------------------------------ zor durum

    private fun survive(w: IsWorld, co: Company) {
        // Once kredi dene
        if (co.loanMonthsLeft == 0) {
            val lim = IsSim.creditLimit(w, co.id)
            if (lim > -co.balance) { IsSim.takeLoan(w, co.id, min(lim, -co.balance * 2 + 100_000), 3); return }
        }
        // En kotu (zarar eden) binayi kapat
        if (co.redMonths >= 1) {
            val worst = w.buildings.filter { it != null && it.owner == co.id && it.buildLeft <= 0 && it.type.cap > 0 }
                .minByOrNull { it!!.lastMonthRevenue - it.type.upkeep }
            if (worst != null && worst.lastMonthRevenue < worst.type.upkeep) IsSim.demolish(w, worst)
        }
    }
}
