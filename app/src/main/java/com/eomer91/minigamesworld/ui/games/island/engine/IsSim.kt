package com.eomer91.minigamesworld.ui.games.island.engine

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Insa kontrolu sonucu */
enum class BuildError { OK, OUTSIDE, WATER, OCCUPIED, NOT_YOURS, NEEDS_BEACH, NO_MONEY, NOT_BUILDABLE }

object IsSim {
    /** Konaklamada "baska yere gitme" secenegi; amenity'lerde "gitmeme" */
    const val U0_STAY = 2.0
    const val U0_AMENITY = 1.5
    /** Turistin binadan amenity'ye gidebilecegi en uzak mesafe (karo) */
    const val RADIUS = 22
    const val DIST_K = 0.12
    /** Aylik turist artisi (binde 1) */
    const val GROWTH = 1.001
    val SEASON = floatArrayOf(0.45f, 0.5f, 0.6f, 0.75f, 0.9f, 1.15f, 1.4f, 1.45f, 1.1f, 0.8f, 0.55f, 0.5f)

    private val DECAY = DoubleArray(RADIUS + 2) { exp(-DIST_K * it) }

    // ------------------------------------------------------------------ arsa ve insaat

    /** Bir arsa karosunun belediyeden satin alma fiyati */
    fun landPrice(w: IsWorld, x: Int, y: Int): Long {
        val t = w.ter(x, y)
        var p = when (t) { Terrain.SAND -> 9_000.0; Terrain.GRASS -> 6_000.0; Terrain.FOREST -> 7_000.0; Terrain.HILL -> 4_000.0; else -> 0.0 }
        val wd = w.waterDist[w.idx(x, y)].toInt()
        p *= when { wd <= 2 -> 2.5; wd <= 5 -> 1.7; wd <= 10 -> 1.2; else -> 1.0 }
        // Cevrede bina coksa arsa pahalanir
        var cnt = 0
        for (yy in max(0, y - 6)..min(w.n - 1, y + 6)) for (xx in max(0, x - 6)..min(w.n - 1, x + 6)) {
            val b = w.bAt[w.idx(xx, yy)]
            if (b >= 0 && w.buildings[b]?.type?.cat?.let { it != Cat.ROAD && it != Cat.NATURE } == true) cnt++
        }
        p *= min(2.5, 1.0 + 0.012 * cnt)
        // Zaman icinde (turist arttikca) hafif artis
        p *= w.demand[1] / w.settings.mapSize.tourists[1]
        return (p / 100).toLong() * 100
    }

    /** Bu konuma insa etmenin toplam maliyeti (bina + gerekli arsa + agac temizleme) */
    fun buildCost(w: IsWorld, c: Int, type: BType, x: Int, y: Int): Long {
        var cost = type.cost
        for (yy in y until y + type.h) for (xx in x until x + type.w) {
            if (!w.inside(xx, yy)) continue
            val i = w.idx(xx, yy)
            if (w.owner[i] == -1) cost += landPrice(w, xx, yy)
            if (w.ter(xx, yy) == Terrain.FOREST && type.cat != Cat.NATURE) cost += 2_000
        }
        return cost
    }

    fun canBuild(w: IsWorld, c: Int, type: BType, x: Int, y: Int): BuildError {
        if (!type.buildable) return BuildError.NOT_BUILDABLE
        var sand = false; var shore = false
        for (yy in y until y + type.h) for (xx in x until x + type.w) {
            if (!w.inside(xx, yy)) return BuildError.OUTSIDE
            val i = w.idx(xx, yy)
            val t = w.ter(xx, yy)
            if (!t.isLand) return BuildError.WATER
            if (w.bAt[i] >= 0) return BuildError.OCCUPIED
            if (w.owner[i] != -1 && w.owner[i] != c) return BuildError.NOT_YOURS
            if (t == Terrain.SAND) sand = true
            if (w.waterDist[i].toInt() == 1) shore = true
        }
        if (type.needsBeach && !(sand && shore)) return BuildError.NEEDS_BEACH
        if (w.companies[c].balance < buildCost(w, c, type, x, y)) return BuildError.NO_MONEY
        return BuildError.OK
    }

    fun build(w: IsWorld, c: Int, type: BType, x: Int, y: Int): Building? {
        if (canBuild(w, c, type, x, y) != BuildError.OK) return null
        val co = w.companies[c]
        var land = 0L
        for (yy in y until y + type.h) for (xx in x until x + type.w) {
            val i = w.idx(xx, yy)
            if (w.owner[i] == -1) { land += landPrice(w, xx, yy); w.owner[i] = c }
        }
        val clear = buildCost(w, c, type, x, y) - type.cost - land
        co.book(Fin.LAND, land)
        co.book(Fin.BUILD, type.cost + max(0, clear))
        val b = Building(w.buildings.size, type, c, x, y)
        w.buildings.add(b)
        for (yy in y until y + type.h) for (xx in x until x + type.w) {
            val i = w.idx(xx, yy)
            w.bAt[i] = b.id
            if (w.ter(xx, yy) == Terrain.FOREST) w.terrain[i] = Terrain.GRASS.ordinal.toByte()
        }
        if (b.buildLeft <= 0) refreshLocation(w, b)
        return b
    }

    fun placePublic(w: IsWorld, type: BType, x: Int, y: Int) {
        val b = Building(w.buildings.size, type, -2, x, y)
        b.buildLeft = 0
        w.buildings.add(b)
        for (yy in y until y + type.h) for (xx in x until x + type.w) {
            val i = w.idx(xx, yy)
            w.bAt[i] = b.id; w.owner[i] = -2
            if (w.ter(xx, yy) == Terrain.FOREST) w.terrain[i] = Terrain.GRASS.ordinal.toByte()
        }
    }

    /** Yalnizca arsa satin al (rakibi engellemek ya da sonrasi icin) */
    fun buyLand(w: IsWorld, c: Int, x: Int, y: Int): Boolean {
        if (!w.inside(x, y)) return false
        val i = w.idx(x, y)
        if (w.owner[i] != -1 || !w.ter(x, y).isLand) return false
        val p = landPrice(w, x, y)
        val co = w.companies[c]
        if (co.balance < p) return false
        co.book(Fin.LAND, p); w.owner[i] = c
        return true
    }

    /** Binayi yik: insaat bedelinin %20'si geri gelir; arsa sirkette kalir */
    fun demolish(w: IsWorld, b: Building, refund: Boolean = true) {
        if (b.owner < 0) return
        val co = w.companies[b.owner]
        if (refund && b.buildLeft <= 0) co.book(Fin.SALES, b.type.cost / 5)
        for (yy in b.y until b.y + b.type.h) for (xx in b.x until b.x + b.type.w) w.bAt[w.idx(xx, yy)] = -1
        w.buildings[b.id] = null
    }

    /** Iyilestirme: kalite +0.5, kapasite +%25 */
    fun upgradeCost(b: Building): Long = (b.type.cost * (0.45 + 0.25 * b.level)).toLong()
    fun upgrade(w: IsWorld, b: Building): Boolean {
        if (b.level >= 2 || b.buildLeft > 0 || b.type.cap == 0 || b.owner < 0) return false
        val co = w.companies[b.owner]
        val cost = upgradeCost(b)
        if (co.balance < cost) return false
        co.book(Fin.BUILD, cost)
        b.level++
        b.buildLeft = max(3, b.type.days / 3)
        return true
    }

    fun setPrice(b: Building, p: Float) {
        b.price = p.coerceIn(b.type.price * 0.2f, b.type.price * 4f)
    }

    // ------------------------------------------------------------------ konum

    /** Sinif basina konum puani: deniz yakinligi, guzellik, gurultu, cevredeki eglence */
    fun refreshLocation(w: IsWorld, b: Building) {
        val n = w.n
        var minWd = 99
        for (yy in b.y until b.y + b.type.h) for (xx in b.x until b.x + b.type.w) minWd = min(minWd, w.waterDist[w.idx(xx, yy)].toInt())
        var beauty = 0f; var noise = 0f; var attractions = 0
        val r = 6
        for (yy in max(0, b.y - r) until min(n, b.y + b.type.h + r)) for (xx in max(0, b.x - r) until min(n, b.x + b.type.w + r)) {
            val i = w.idx(xx, yy)
            val t = w.ter(xx, yy)
            val d = max(abs(xx - b.cx), abs(yy - b.cy))
            if (t == Terrain.FOREST && d <= 4) beauty += 0.15f
            if (!t.isLand && d <= 4) beauty += 0.05f
            val oid = w.bAt[i]
            if (oid < 0 || oid == b.id) continue
            val o = w.buildings[oid] ?: continue
            // Bir binayi karo sayisi kadar saymamak icin yalnizca sol ust karosunda say
            if (o.x != xx || o.y != yy) continue
            val od = max(abs(o.cx - b.cx), abs(o.cy - b.cy))
            if (od <= 5) { beauty += o.type.beauty; if (o.buildLeft <= 0) noise += o.type.noise }
            if (o.type.cat.isAmenity && od <= 10) attractions++
        }
        val beachBonus = when { minWd <= 2 -> 0.6f; minWd <= 5 -> 0.3f; minWd <= 9 -> 0.1f; else -> 0f }
        val beautyBonus = min(0.6f, beauty * 0.05f)
        val attrBonus = if (b.type.cat == Cat.STAY) min(0.5f, attractions * 0.05f) else 0f
        val noiseW = floatArrayOf(0.15f, 0.3f, 0.6f)
        for (c in 0..2) {
            val beachW = if (c == 2) 1.5f else 1f
            val noisePenalty = if (b.type.cat == Cat.STAY) noiseW[c] * min(2f, noise) else 0f
            b.loc[c] = beachBonus * beachW + beautyBonus + attrBonus - noisePenalty
        }
    }

    fun refreshAllLocations(w: IsWorld) { for (b in w.buildings) if (b != null && b.owner >= 0) refreshLocation(w, b) }

    // ------------------------------------------------------------------ gunluk dongu

    private fun repTerm(w: IsWorld, owner: Int) = 0.03 * (w.companies[owner].reputation - 50f)
    private fun adTerm(w: IsWorld, owner: Int, c: Int) = if (w.companies[owner].adUntil[c] >= w.day) 0.5 else 0.0

    private fun sigmoid(x: Double) = 1.0 / (1.0 + exp(-x))

    /** Bir gun ilerlet */
    fun step(w: IsWorld) {
        if (w.gameOver) return
        // Insaat ve kapali binalar
        for (b in w.buildings) {
            if (b == null || b.owner < 0) continue
            b.today.fill(0f)
            if (b.closedDays > 0) b.closedDays--
            if (b.buildLeft > 0) {
                b.buildLeft--
                if (b.buildLeft == 0) {
                    refreshLocation(w, b)
                    w.addNews(IsNews(w.day, NewsKind.BUILT, b.owner, type = b.type.id))
                }
            }
        }
        val season = SEASON[w.month]
        val want = DoubleArray(3) { w.demand[it] * season }
        allocateStays(w, want)
        for (cat in Cat.entries) if (cat.isAmenity) allocateAmenity(w, cat)
        if (w.day % 7 == 6) refreshAllLocations(w)
        IsRivals.dailyAuctions(w)
        IsBots.daily(w)
        if (w.dayOfMonth == 30) monthEnd(w)
        w.day++
    }

    private fun allocateStays(w: IsWorld, want: DoubleArray) {
        val stays = w.buildings.filter { it != null && it.owner >= 0 && it.open && it.type.cat == Cat.STAY && w.companies[it.owner].alive }.map { it!! }
        val m = stays.size
        val e = Array(3) { DoubleArray(m) }
        for (c in 0..2) {
            val cl = TClass.entries[c]
            for (i in 0 until m) {
                val b = stays[i]
                val ap = b.type.appeal[c]
                if (ap <= 0f) { e[c][i] = 0.0; continue }
                val u = 3.0 * ap + 0.3 * cl.qualityW * b.quality + repTerm(w, b.owner) + adTerm(w, b.owner, c) + b.loc[c] - cl.sens * b.price / cl.stayBudget
                e[c][i] = exp(u)
            }
        }
        val capLeft = DoubleArray(m) { stays[it].capacity.toDouble() }
        val full = BooleanArray(m)
        val assign = Array(3) { DoubleArray(m) }
        val remaining = want.copyOf()
        val e0 = exp(U0_STAY)
        val lost = DoubleArray(3)
        repeat(4) {
            val wants = Array(3) { DoubleArray(m) }
            for (c in 0..2) {
                if (remaining[c] <= 0.0) continue
                var denom = e0
                for (i in 0 until m) if (!full[i]) denom += e[c][i]
                for (i in 0 until m) if (!full[i]) wants[c][i] = remaining[c] * e[c][i] / denom
                lost[c] += remaining[c] * e0 / denom
                remaining[c] = 0.0
            }
            for (i in 0 until m) {
                if (full[i]) continue
                val tw = wants[0][i] + wants[1][i] + wants[2][i]
                if (tw <= 0.0) continue
                val f = if (tw > capLeft[i]) { full[i] = true; capLeft[i] / tw } else 1.0
                for (c in 0..2) {
                    assign[c][i] += wants[c][i] * f
                    remaining[c] += wants[c][i] * (1 - f)
                }
                capLeft[i] -= tw * f
            }
        }
        for (c in 0..2) {
            lost[c] += remaining[c]
            w.lost[c] = lost[c].toFloat()
            var p = 0.0
            for (i in 0 until m) p += assign[c][i]
            w.present[c] = p.toFloat()
        }
        // Gelir, gider, memnuniyet
        for (i in 0 until m) {
            val b = stays[i]
            val co = w.companies[b.owner]
            var guests = 0.0
            for (c in 0..2) {
                val g = assign[c][i]
                if (g <= 0.0) continue
                b.today[c] = g.toFloat()
                guests += g
                val cl = TClass.entries[c]
                val v = 3.0 * b.type.appeal[c] + 0.3 * cl.qualityW * b.quality + b.loc[c] - cl.sens * b.price / cl.stayBudget
                co.satSum += g * sigmoid(1.2 * (v - 1.0)); co.satW += g
            }
            if (guests <= 0.0) continue
            val rev = (guests * b.price).toLong()
            co.book(Fin.STAY, rev)
            co.book(Fin.OPERATING, (guests * b.type.varCost).toLong())
            b.monthRevenue += rev; b.monthUse += guests.toFloat()
        }
    }

    private fun allocateAmenity(w: IsWorld, cat: Cat) {
        val ams = w.buildings.filter { it != null && it.owner >= 0 && it.open && it.type.cat == cat && w.companies[it.owner].alive }.map { it!! }
        if (ams.isEmpty()) return
        val rate = cat.visitRate!!; val budget = cat.visitBudget!!
        val m = ams.size
        // Mesafeden bagimsiz cekicilik
        val base = Array(3) { DoubleArray(m) }
        for (c in 0..2) {
            val cl = TClass.entries[c]
            for (i in 0 until m) {
                val b = ams[i]
                val ap = b.type.appeal[c]
                if (ap <= 0f) continue
                val u = 3.0 * ap + 0.3 * cl.qualityW * b.quality + repTerm(w, b.owner) + adTerm(w, b.owner, c) +
                    0.3 * (b.loc[c].coerceAtMost(0.6f)) - cl.sens * b.price / budget[c]
                base[c][i] = exp(u)
            }
        }
        val wantV = Array(3) { DoubleArray(m) }
        val e0 = exp(U0_AMENITY)
        val ew = DoubleArray(m)
        for (h in w.buildings) {
            if (h == null || h.owner < 0 || h.type.cat != Cat.STAY) continue
            val g = h.today
            if (g[0] + g[1] + g[2] <= 0f) continue
            for (i in 0 until m) {
                val d = max(abs(ams[i].cx - h.cx), abs(ams[i].cy - h.cy)).toInt()
                ew[i] = if (d > RADIUS) -1.0 else DECAY[d]
            }
            for (c in 0..2) {
                if (g[c] <= 0f) continue
                var denom = e0
                for (i in 0 until m) if (ew[i] > 0) denom += base[c][i] * ew[i]
                if (denom <= e0) continue
                val v = g[c] * rate[c]
                for (i in 0 until m) if (ew[i] > 0) wantV[c][i] += v * base[c][i] * ew[i] / denom
            }
        }
        for (i in 0 until m) {
            val b = ams[i]
            val tw = wantV[0][i] + wantV[1][i] + wantV[2][i]
            if (tw <= 0.0) continue
            val served = min(tw, b.capacity.toDouble())
            val f = served / tw
            val co = w.companies[b.owner]
            for (c in 0..2) {
                val v = wantV[c][i] * f
                b.today[c] = v.toFloat()
                if (v <= 0.0) continue
                val cl = TClass.entries[c]
                val val0 = 3.0 * b.type.appeal[c] + 0.3 * cl.qualityW * b.quality - cl.sens * b.price / budget[c]
                // Kalabalik (kapasite dolu) memnuniyeti dusurur
                val crowd = if (f < 1.0) 0.6 * (1 - f) else 0.0
                co.satSum += 0.3 * v * sigmoid(1.2 * (val0 - 1.0) - crowd); co.satW += 0.3 * v
            }
            val rev = (served * b.price).toLong()
            co.book(cat.fin!!, rev)
            co.book(Fin.OPERATING, (served * b.type.varCost).toLong())
            b.monthRevenue += rev; b.monthUse += served.toFloat()
        }
    }

    // ------------------------------------------------------------------ ay sonu

    private fun monthEnd(w: IsWorld) {
        for (co in w.companies) {
            if (!co.alive) continue
            // Bakim
            var upkeep = 0L
            for (b in w.buildings) if (b != null && b.owner == co.id) upkeep += if (b.buildLeft > 0 && b.level == 0) b.type.upkeep / 4 else (b.type.upkeep * (1 + 0.2 * b.level)).toLong()
            co.book(Fin.UPKEEP, upkeep)
            // Kredi taksiti
            if (co.loanMonthsLeft > 0 && co.loanLeft > 0) {
                val interest = (co.loanLeft * co.loanRate / 100.0 / 12.0).toLong()
                var principal = co.loanPayment - interest
                if (co.loanMonthsLeft <= 1 || principal >= co.loanLeft) principal = co.loanLeft
                co.book(Fin.INTEREST, interest)
                co.balance -= principal; co.monthOut += principal
                co.loanLeft -= principal; co.loanMonthsLeft--
                if (co.loanLeft <= 0 || co.loanMonthsLeft <= 0) clearLoan(co)
            }
            // Eksi bakiye faizi (%1.5/ay)
            if (co.balance < 0) co.book(Fin.INTEREST, (-co.balance * 0.015).toLong())
            // Itibar
            if (co.satW > 0) {
                val target = (co.satSum / co.satW * 100).toFloat()
                co.reputation += 0.25f * (target - co.reputation)
            } else co.reputation += 0.1f * (50f - co.reputation)
            co.reputation = co.reputation.coerceIn(0f, 100f)
            co.satSum = 0.0; co.satW = 0.0
            // Iflas takibi
            co.redMonths = if (co.balance < 0) co.redMonths + 1 else 0
            co.lastMonthIn = co.monthIn; co.lastMonthOut = co.monthOut
            co.monthIn = 0; co.monthOut = 0
        }
        for (b in w.buildings) if (b != null) {
            b.lastMonthRevenue = b.monthRevenue; b.monthRevenue = 0
            b.lastMonthUse = b.monthUse; b.monthUse = 0f
        }
        for (c in 0..2) w.demand[c] *= GROWTH
        // Mali yil: her 12 ayda bir defter devri
        if ((w.day + 1) % 360 == 0) for (co in w.companies) {
            co.ledger.copyInto(co.lastLedger); co.ledger.fill(0)
        }
        IsRivals.monthEnd(w)
    }

    // ------------------------------------------------------------------ kredi

    fun annualRevenue(co: Company): Long {
        val last = co.lastLedger.withIndex().filter { Fin.entries[it.index].income && it.index != Fin.SALES.ordinal }.sumOf { it.value }
        return if (last > 0) last else co.lastMonthIn * 12
    }

    /** Binanin tahmini degeri (insaat bedelinin %60'i, iyilestirmelerle artar) */
    fun buildingValue(b: Building): Long = (b.type.cost * (0.6 + 0.15 * b.level)).toLong()

    /** Sirketin binalarinin tahmini degeri */
    fun assets(w: IsWorld, c: Int): Long {
        var v = 0L
        for (b in w.buildings) if (b != null && b.owner == c) v += buildingValue(b)
        return v
    }

    fun netWorth(w: IsWorld, c: Int): Long { val co = w.companies[c]; return co.balance + assets(w, c) - co.loanLeft }

    fun creditLimit(w: IsWorld, c: Int): Long {
        val co = w.companies[c]
        val base = max(annualRevenue(co) / 2, assets(w, c) / 3)
        var lim = max(500_000L, base) - co.loanLeft
        if (co.balance < 0) lim += co.balance / 2
        return max(0L, lim / 10_000 * 10_000)
    }

    fun loanRate(w: IsWorld, c: Int, years: Int): Float {
        val co = w.companies[c]
        var r = 6f + (1f - co.reputation / 100f) * 6f + (years - 1) * 0.5f
        if (co.balance < 0) r += 3f
        return (r * 10).toInt() / 10f
    }

    fun monthlyPayment(amount: Long, rate: Float, months: Int): Long {
        val r = rate / 100.0 / 12.0
        if (r <= 0) return amount / months
        return (amount * r / (1 - (1 + r).pow(-months))).toLong()
    }

    fun takeLoan(w: IsWorld, c: Int, amount: Long, years: Int): Boolean {
        val co = w.companies[c]
        if (co.loanMonthsLeft > 0 || amount <= 0 || amount > creditLimit(w, c)) return false
        co.loanRate = loanRate(w, c, years)
        co.loanLeft = amount
        co.loanMonthsLeft = years * 12
        co.loanPayment = monthlyPayment(amount, co.loanRate, years * 12)
        co.balance += amount
        return true
    }

    fun repayCost(co: Company) = co.loanLeft + co.loanLeft / 100

    fun repayLoan(w: IsWorld, c: Int): Boolean {
        val co = w.companies[c]
        if (co.loanMonthsLeft <= 0) return false
        val cost = repayCost(co)
        if (co.balance < cost) return false
        co.balance -= co.loanLeft
        co.book(Fin.INTEREST, cost - co.loanLeft)
        clearLoan(co)
        return true
    }

    private fun clearLoan(co: Company) { co.loanLeft = 0; co.loanMonthsLeft = 0; co.loanPayment = 0; co.loanRate = 0f }
}
