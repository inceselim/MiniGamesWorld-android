package com.eomer91.minigamesworld.ui.games.football.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class FbTransferTest {

    private fun setup(seed: Long = 11L): FbWorld {
        val w = FbGen(seed).generate()
        val lg = w.leagues.first { FbCountries.all[it.country].code == "ENG" && it.tier == 1 }
        w.userClub = lg.clubs[6]
        FbSeason.start(w)
        w.clubs[w.userClub].balance = 300_000_000
        return w
    }

    private fun day(w: FbWorld) = FbSeason.advanceDay(w, autoUser = true)

    @Test
    fun buyNegotiateSign() {
        val w = setup()
        val me = w.clubs[w.userClub]
        val target = w.players.first { p ->
            p.club >= 0 && p.club != me.id && w.clubs[p.club].country != me.country && p.currentAbility() in 120..140 &&
                FbTransfer.willing(w, p, me.id) && FbTransfer.squadRank(w, p) in 3..12 && p.releaseClause == 0
        }
        val min = FbTransfer.sellerMin(w, target)
        println("hedef ${target.fullName} CA=${target.currentAbility()} deger=${FbTransfer.value(w, target)} satici min=$min")
        // Dusuk teklif: ret ya da karsi teklif
        val (low, err) = FbTransfer.makeOffer(w, me.id, target.id, min / 3)
        assertEquals(null, err)
        day(w)
        assertEquals(OfferStatus.REJECTED, low!!.status)
        val (mid, _) = FbTransfer.makeOffer(w, me.id, target.id, (min * 0.8).toLong())
        day(w)
        assertEquals(OfferStatus.COUNTER, mid!!.status)
        assertTrue(mid.counterFee >= min * 0.95)
        assertTrue(FbTransfer.acceptCounter(w, mid))
        assertEquals(OfferStatus.AGREED, mid.status)
        // Sozlesme: dusuk maas reddedilir, talep kabul edilir
        val d = FbTransfer.demand(w, target, me.id)
        assertEquals(ContractReply.WAGE_LOW, FbTransfer.offerContract(w, mid, d.wage / 2, d.minYears))
        assertEquals(ContractReply.YEARS, FbTransfer.offerContract(w, mid, d.wage, d.maxYears + 2))
        val balBefore = me.balance
        assertEquals(ContractReply.ACCEPT, FbTransfer.offerContract(w, mid, d.wage, d.minYears))
        assertEquals(me.id, target.club)
        assertTrue(target.id in me.players)
        assertEquals(balBefore - mid.fee, me.balance)
        assertEquals(OfferStatus.COMPLETED, mid.status)
        assertTrue(w.market.history.any { it.player == target.id && it.to == me.id })
        assertTrue(w.market.news.any { it.kind == NewsKind.TRANSFER_DONE && it.player == target.id })
    }

    @Test
    fun loanAndReturn() {
        val w = setup()
        val me = w.clubs[w.userClub]
        val big = w.clubs.filter { it.league >= 0 && it.id != me.id && it.reputation > me.reputation }.maxBy { it.reputation }
        val kid = big.players.map { w.players[it] }.filter { w.age(it) <= 21 }.maxBy { FbTransfer.squadRank(w, it) }
        val (o, err) = FbTransfer.makeOffer(w, me.id, kid.id, 0, loan = true, loanWagePct = 80)
        assertEquals(null, err)
        day(w)
        assertEquals(OfferStatus.AGREED, o!!.status)
        assertEquals(ContractReply.ACCEPT, FbTransfer.offerContract(w, o, 0, 1))
        assertEquals(me.id, kid.club); assertEquals(big.id, kid.loanFrom); assertEquals(80, kid.loanWagePct)
        var r: DayReport
        do { r = day(w) } while (!r.seasonEnded)
        // Sezon sonu geri doner (id sikistirmada degismis olabilir: isimle bul)
        val back = w.players.firstOrNull { it.firstName == kid.firstName && it.lastName == kid.lastName && it.birthYear == kid.birthYear }
        if (back != null && !back.retired) { assertEquals(-1, back.loanFrom); assertTrue(back.club != me.id || back.loanFrom < 0) }
    }

    @Test
    fun sellIncomingAndAuction() {
        val w = setup()
        val me = w.clubs[w.userClub]
        val mine = me.players.map { w.players[it] }.sortedByDescending { it.currentAbility() }
        // YZ alici teklifi (dogrudan olustur): kullanici kabul eder, ertesi gun tamamlanir
        val sold = mine[8]
        val buyer = w.clubs.filter { it.league >= 0 && it.id != me.id && FbTransfer.willing(w, sold, it.id) }.maxBy { it.reputation }
        buyer.balance = 500_000_000; buyer.transferBudget = 500_000_000
        val (o, _) = FbTransfer.makeOffer(w, buyer.id, sold.id, FbTransfer.value(w, sold))
        FbTransfer.respondIncoming(w, o!!, accept = true)
        val tin = me.ledger[Fin.TRANSFER_IN.ordinal]
        day(w)
        assertEquals(OfferStatus.COMPLETED, o.status)
        assertEquals(buyer.id, sold.club)
        assertEquals(tin + o.fee, me.ledger[Fin.TRANSFER_IN.ordinal])
        // Acik artirma
        val star = mine[1]
        star.wantsAway = true
        val a = FbTransfer.startAuction(w, star, reserve = 1_000_000, days = 3)!!
        repeat(4) { day(w) }
        assertTrue(a.done)
        println("acik artirma: ${a.bids} teklif, en yuksek ${a.topBid} (${if (a.topBidder >= 0) w.clubs[a.topBidder].name else "-"}), oyuncu simdi ${w.clubs.getOrNull(star.club)?.name}")
        assertTrue(w.market.news.any { it.kind == NewsKind.AUCTION_RESULT && it.player == star.id })
        if (a.topBid >= a.reserve && a.topBidder >= 0) assertTrue(star.club == a.topBidder || star.club == me.id)
    }

    @Test
    fun renewalReleaseFreeAgentRecommend() {
        val w = setup()
        val me = w.clubs[w.userClub]
        val p = me.players.map { w.players[it] }.first { w.age(it) in 22..29 }
        val d = FbTransfer.renewalDemand(w, p)
        assertEquals(ContractReply.WAGE_LOW, FbTransfer.offerRenewal(w, p, d.wage / 3, d.minYears))
        assertEquals(ContractReply.ACCEPT, FbTransfer.offerRenewal(w, p, d.wage, d.maxYears))
        assertEquals(w.season + d.maxYears - 1, p.contractEnd)
        // Fesih
        val q = me.players.map { w.players[it] }.minBy { it.currentAbility() }
        val cost = FbTransfer.releaseCost(w, q)
        val bal = me.balance
        FbTransfer.release(w, q)
        assertEquals(CLUB_NONE, q.club)
        assertEquals(bal - cost, me.balance)
        // Serbest oyuncu
        val fa = w.players.filter { it.club == CLUB_NONE && FbTransfer.willing(w, it, me.id) }.maxBy { it.currentAbility() }
        val fd = FbTransfer.demand(w, fa, me.id)
        assertEquals(ContractReply.ACCEPT, FbTransfer.signFreeAgent(w, me.id, fa.id, fd.wage, fd.minYears))
        assertEquals(me.id, fa.club)
        // Oneriler
        val recs = FbTransfer.recommend(w, me.id, 20_000_000)
        println("oneri: " + recs.take(5).map { val x = w.players[it.player]; "${x.fullName} ${x.bestPos()} ~${it.estCa}/${it.estPa} ${it.value / 1000}k" })
        assertTrue(recs.isNotEmpty())
        assertTrue(recs.all { it.value <= 20_000_000 && w.players[it.player].club != me.id })
        // Gozlemci tahmini sabit
        val x = w.players[recs[0].player]
        assertEquals(FbTransfer.estimate(w, me.id, x), FbTransfer.estimate(w, me.id, x))
    }

    @Test
    fun windowAndSave() {
        val w = setup()
        val me = w.clubs[w.userClub]
        val cal = FbSeason.calendar(w)
        val t = w.players.first { it.club >= 0 && it.club != me.id }
        w.market.shortlist.add(t.id)
        // Reklam odulleri: gizli ozellik acma ve kulube para
        w.revealed.add(t.id)
        assertEquals(t.currentAbility() to t.potential, FbTransfer.estimate(w, me.id, t))
        val bal0 = me.balance
        val got = FbClubLife.claimAdBonus(w)
        assertTrue(got >= 50_000 && me.balance == bal0 + got)
        repeat(FbClubLife.AD_BONUS_PER_WEEK - 1) { FbClubLife.claimAdBonus(w) }
        assertEquals(0, FbClubLife.adBonusLeft(w))
        assertEquals(0L, FbClubLife.claimAdBonus(w))
        FbTransfer.makeOffer(w, me.id, t.id, 1_000)
        repeat(3) { day(w) }
        val bytes = ByteArrayOutputStream().also { FbSave.write(w, it) }.toByteArray()
        val w2 = FbSave.read(ByteArrayInputStream(bytes))
        assertEquals(w.market.offers.size, w2.market.offers.size)
        assertEquals(w.market.news.size, w2.market.news.size)
        assertEquals(w.market.shortlist, w2.market.shortlist)
        assertEquals(w.market.history.size, w2.market.history.size)
        assertEquals(w.revealed, w2.revealed)
        assertEquals(w.adBonusCount, w2.adBonusCount)
        // Pencere disinda teklif verilemez
        while (w.day <= cal.summerWindowEnd) day(w)
        assertEquals("WINDOW", FbTransfer.makeOffer(w, me.id, w.players.first { it.club >= 0 && it.club != me.id && it.loanFrom < 0 }.id, 1_000_000).second)
        val moved = w.market.history.count { it.season == w.season }
        println("yaz donemi transfer sayisi: $moved")
        assertTrue(moved > 300)
    }
}
