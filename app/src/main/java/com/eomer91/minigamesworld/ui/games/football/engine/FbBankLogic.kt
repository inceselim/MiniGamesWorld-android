package com.eomer91.minigamesworld.ui.games.football.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/** Kullanicinin banka hesabi: tek kredi (esit haftalik taksit) ve vadeli mevduat. */
class FbBank {
    /** Kalan anapara, ilk cekilen tutar, yillik faiz (%), haftalik taksit, kalan hafta */
    var loanLeft: Long = 0
    var loanTaken: Long = 0
    var loanRate: Float = 0f
    var loanWeekly: Long = 0
    var loanWeeksLeft: Int = 0
    /** Mevduat bakiyesi ve bugune kadar kazanilan faiz */
    var deposit: Long = 0
    var depositEarned: Long = 0

    val hasLoan: Boolean get() = loanLeft > 0 && loanWeeksLeft > 0
}

object FbBanking {
    /** Mevduat yillik faizi (%) */
    const val DEPOSIT_RATE = 2.0f
    /** Erken kapama ucreti (kalan anaparanin %'si) */
    const val EARLY_FEE_PCT = 1
    val TERMS_YEARS = intArrayOf(1, 2, 3)

    /** Yillik tahmini gelir (transfer haric): gecen sezon defteri, yoksa model tahmini. */
    fun yearRevenue(world: FbWorld, c: FbClub): Long {
        val last = c.lastLedger.withIndex().filter { Fin.entries[it.index].income && it.index != Fin.TRANSFER_IN.ordinal }.sumOf { it.value }
        if (last > 0) return last
        val lg = world.leagues.getOrNull(c.league)
        val tv = if (lg != null) (FbClubLife.tvPool(world, lg) / lg.clubs.size).toLong() else 0L
        val tickets = if (lg != null) ((lg.clubs.size - 1) * FbClubLife.attendance(world, c, -1, CompKind.LEAGUE) * FbClubLife.ticketPrice(world, c) *
            FbClubLife.revenueMul(world, c)).toLong() else 0L
        return (FbClubLife.merchYear(world, c) + FbClubLife.adsYear(world, c)).toLong() + c.sponsorIncome + tv + tickets
    }

    /** Bankanin verebilecegi en yuksek kredi: yillik gelirin %40'i (itibarla %30-%50), en az 500 bin. */
    fun creditLimit(world: FbWorld, c: FbClub): Long {
        val rep = c.reputation / 10000f
        var lim = (yearRevenue(world, c) * (0.30f + rep * 0.20f)).toLong()
        if (c.balance < 0) lim = max(0L, lim + c.balance / 2)
        return FbEconomy.roundValue(max(500_000L, lim))
    }

    /** Yillik faiz: itibar yuksekse ucuz (4-8%), eksi bakiyede +3, uzun vadede +0.5/yil. */
    fun loanRate(c: FbClub, years: Int): Float {
        val rep = (c.reputation / 10000f).coerceIn(0f, 1f)
        var r = 4f + (1f - rep) * 4f + (years - 1) * 0.5f
        if (c.balance < 0) r += 3f
        return (r * 10).toInt() / 10f
    }

    /** Esit taksit (anuite) */
    fun weeklyPayment(amount: Long, ratePct: Float, weeks: Int): Long {
        val r = ratePct / 100.0 / 52.0
        if (r <= 0) return amount / weeks
        return (amount * r / (1 - (1 + r).pow(-weeks))).roundToLong()
    }

    /** Kredi cek; hata durumunda false (zaten kredi var / limit asildi). */
    fun takeLoan(world: FbWorld, amount: Long, years: Int): Boolean {
        val c = world.clubs.getOrNull(world.userClub) ?: return false
        val b = world.bank
        if (b.hasLoan || amount <= 0 || amount > creditLimit(world, c)) return false
        val weeks = years * 52
        b.loanRate = loanRate(c, years)
        b.loanTaken = amount; b.loanLeft = amount
        b.loanWeeksLeft = weeks
        b.loanWeekly = weeklyPayment(amount, b.loanRate, weeks)
        c.balance += amount // anapara gelir sayilmaz
        return true
    }

    /** Krediyi kalan anapara + %1 ucretle kapat. */
    fun repayLoan(world: FbWorld): Boolean {
        val c = world.clubs.getOrNull(world.userClub) ?: return false
        val b = world.bank
        if (!b.hasLoan) return false
        val fee = b.loanLeft * EARLY_FEE_PCT / 100
        if (c.balance < b.loanLeft + fee) return false
        c.balance -= b.loanLeft
        c.book(Fin.INTEREST, fee)
        clearLoan(b)
        return true
    }

    fun earlyRepayCost(b: FbBank): Long = b.loanLeft + b.loanLeft * EARLY_FEE_PCT / 100

    private fun clearLoan(b: FbBank) { b.loanLeft = 0; b.loanWeeksLeft = 0; b.loanWeekly = 0; b.loanTaken = 0; b.loanRate = 0f }

    fun depositMoney(world: FbWorld, amount: Long): Boolean {
        val c = world.clubs.getOrNull(world.userClub) ?: return false
        val a = min(amount, c.balance)
        if (a <= 0) return false
        c.balance -= a; world.bank.deposit += a
        return true
    }

    fun withdraw(world: FbWorld, amount: Long): Boolean {
        val c = world.clubs.getOrNull(world.userClub) ?: return false
        val a = min(amount, world.bank.deposit)
        if (a <= 0) return false
        world.bank.deposit -= a; c.balance += a
        return true
    }

    /** Haftalik (FbClubLife.weekly): taksit ve mevduat faizi. */
    fun weekly(world: FbWorld) {
        val c = world.clubs.getOrNull(world.userClub) ?: return
        val b = world.bank
        if (b.hasLoan) {
            val interest = (b.loanLeft * (b.loanRate / 100.0 / 52.0)).roundToLong()
            var principal = b.loanWeekly - interest
            if (b.loanWeeksLeft <= 1 || principal >= b.loanLeft) principal = b.loanLeft
            c.book(Fin.INTEREST, interest)
            c.balance -= principal
            c.monthOut += principal // taksitin tamami aylik gidere yazilsin
            b.loanLeft -= principal
            b.loanWeeksLeft--
            if (b.loanLeft <= 0 || b.loanWeeksLeft <= 0) clearLoan(b)
        }
        if (b.deposit > 0) {
            val earn = (b.deposit * (DEPOSIT_RATE / 100.0 / 52.0)).roundToLong()
            b.deposit += earn; b.depositEarned += earn
        }
    }
}
