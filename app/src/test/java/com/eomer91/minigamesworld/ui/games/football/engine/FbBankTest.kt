package com.eomer91.minigamesworld.ui.games.football.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class FbBankTest {

    private fun world(): FbWorld {
        val w = FbGen(11L).generate()
        val lg = w.leagues.first { FbCountries.all[it.country].code == "ENG" && it.tier == 1 }
        w.userClub = lg.clubs.sortedBy { w.clubs[it].reputation }[lg.clubs.size / 2]
        FbSeason.start(w)
        return w
    }

    @Test
    fun loanRepaysWithInterest() {
        val w = world()
        val c = w.clubs[w.userClub]
        val limit = FbBanking.creditLimit(w, c)
        val rate = FbBanking.loanRate(c, 1)
        println("Kredi limiti ${limit / 1_000_000}M, 1 yil faiz %$rate, 3 yil %${FbBanking.loanRate(c, 3)}")
        assertTrue(limit >= 500_000)
        val b0 = c.balance
        assertTrue(FbBanking.takeLoan(w, limit / 2, 1))
        assertTrue(!FbBanking.takeLoan(w, 1000, 1)) // ikinci kredi yok
        assertEquals(b0 + limit / 2, c.balance)
        val weekly = w.bank.loanWeekly
        assertTrue(weekly * 52 > limit / 2)          // faiz var
        assertTrue(weekly * 52 < limit / 2 * 1.10)   // makul
        val interest0 = c.ledger[Fin.INTEREST.ordinal]
        repeat(52) { FbBanking.weekly(w) }
        assertTrue(!w.bank.hasLoan)
        assertEquals(0L, w.bank.loanLeft)
        val paidInterest = c.ledger[Fin.INTEREST.ordinal] - interest0
        println("Odenen faiz ${paidInterest / 1000}K, taksit ${weekly / 1000}K/hafta")
        val full = (limit / 2 * rate / 100).toLong()
        assertTrue(paidInterest in full / 3..full)
    }

    @Test
    fun earlyRepayAndDeposit() {
        val w = world()
        val c = w.clubs[w.userClub]
        c.balance = 50_000_000
        assertTrue(FbBanking.takeLoan(w, 1_000_000, 2))
        repeat(10) { FbBanking.weekly(w) }
        val cost = FbBanking.earlyRepayCost(w.bank)
        val bal = c.balance
        assertTrue(FbBanking.repayLoan(w))
        assertEquals(bal - cost, c.balance)
        assertTrue(!w.bank.hasLoan)
        // Mevduat
        assertTrue(FbBanking.depositMoney(w, 10_000_000))
        assertEquals(10_000_000L, w.bank.deposit)
        repeat(52) { FbBanking.weekly(w) }
        println("Mevduat 1 yil: ${w.bank.deposit}")
        assertTrue(w.bank.deposit in 10_190_000L..10_210_000L)
        val before = c.balance
        assertTrue(FbBanking.withdraw(w, Long.MAX_VALUE))
        assertEquals(0L, w.bank.deposit)
        assertTrue(c.balance > before + 10_000_000)
    }

    @Test
    fun monthlyTotalsAndSave() {
        val w = world()
        FbBanking.takeLoan(w, 600_000, 1)
        FbBanking.depositMoney(w, 100_000)
        val cal = FbSeason.calendar(w)
        while (w.day < cal.dayOf(9, 1)) FbSeason.advanceDay(w, autoUser = true)
        val c = w.clubs[w.userClub]
        println("Agustos: gelir ${c.lastMonthIn / 1000}K gider ${c.lastMonthOut / 1000}K; Eylul ilk gun gelir ${c.monthIn / 1000}K")
        assertTrue(c.lastMonthIn > 0 && c.lastMonthOut > 0)
        assertTrue(c.lastMonthOut > w.bank.loanWeekly * 4)
        val bytes = ByteArrayOutputStream().also { FbSave.write(w, it) }.toByteArray()
        val w2 = FbSave.read(ByteArrayInputStream(bytes))
        val c2 = w2.clubs[w2.userClub]
        assertEquals(c.lastMonthIn, c2.lastMonthIn); assertEquals(c.monthOut, c2.monthOut)
        assertEquals(w.bank.loanLeft, w2.bank.loanLeft); assertEquals(w.bank.loanWeeksLeft, w2.bank.loanWeeksLeft)
        assertEquals(w.bank.deposit, w2.bank.deposit); assertEquals(w.bank.loanRate, w2.bank.loanRate)
    }
}
