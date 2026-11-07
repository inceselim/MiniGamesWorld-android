package com.eomer91.minigamesworld.ui.games.football.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

// ====================================================================== veri

enum class OfferStatus {
    /** Satici kulubun cevabi bekleniyor */
    PENDING,
    /** Satici karsi teklif verdi, alici cevap verecek */
    COUNTER,
    /** Bonservis anlasildi, oyuncuyla sozlesme gorusmesi */
    AGREED,
    COMPLETED, REJECTED, WITHDRAWN, PLAYER_REFUSED, EXPIRED,
}

class FbOffer(
    val id: Int,
    val player: Int,
    val from: Int,
    val to: Int,
    var fee: Long,
    val loan: Boolean,
    var loanWagePct: Int,
    var sellOnPct: Int,
) {
    var status = OfferStatus.PENDING
    var counterFee: Long = 0
    /** Cevap verilecek gun (sezon gunu) */
    var responseDay = 0
    var season = 0
    var rounds = 0
    /** Sozlesme gorusmesinde kalan sabir */
    var patience = 3
    val open: Boolean get() = status == OfferStatus.PENDING || status == OfferStatus.COUNTER || status == OfferStatus.AGREED
}

class FbAuction(val id: Int, val player: Int, val seller: Int, val reserve: Long, val endSeason: Int, val endDay: Int) {
    var topBid: Long = 0
    var topBidder: Int = -1
    var bids = 0
    var done = false
}

enum class NewsKind {
    OFFER_RECEIVED, OFFER_ACCEPTED, OFFER_REJECTED, OFFER_COUNTER, PLAYER_REFUSED, TRANSFER_DONE, TRANSFER_WORLD,
    RUMOUR, CONTRACT_EXPIRING, PLAYER_UNHAPPY, AUCTION_BID, AUCTION_RESULT, LOAN_RETURN, PRECONTRACT, YOUTH_INTAKE, RELEASED,
}

/** Haber/gelen kutusu kaydi: anlami [kind]'a gore (oyuncu, kulup, kulup2, deger). */
class FbNews(val kind: NewsKind, val season: Int, val day: Int, var player: Int, val club: Int, val club2: Int, val value: Long) {
    var read = false
    /** Gercekle iliskisi (soylenti guvenilirligi %) */
    var credibility = 0
}

class FbTransferRecord(val season: Int, val day: Int, val playerName: String, var player: Int, val from: Int, val to: Int, val fee: Long, val loan: Boolean)

class FbMarket {
    val offers = ArrayList<FbOffer>()
    val auctions = ArrayList<FbAuction>()
    val shortlist = ArrayList<Int>()
    val news = ArrayList<FbNews>()
    val history = ArrayList<FbTransferRecord>()
    var nextId = 0
    /** Pazar indeksi onbellegi (kaydedilmez) */
    var indexCache: Any? = null
    var indexKey = -1L

    fun addNews(n: FbNews) { news.add(n); while (news.size > 300) news.removeAt(0) }
}

/** Oyuncunun sozlesme talebi. */
class ContractDemand(val wage: Int, val minYears: Int, val maxYears: Int, val signingFee: Long)

/** Sozlesme cevabi: null = kabul, yoksa neden. */
enum class ContractReply { ACCEPT, WAGE_LOW, YEARS, NOT_INTERESTED, WALKED_AWAY }

/** Gozlemci onerisi. */
class Recommendation(val player: Int, val estCa: Int, val estPa: Int, val value: Long, val reason: Pos.Line)

/**
 * Transfer sistemi: pazar degeri, satici/alici yapay zekasi, teklif-karsi teklif, sozlesme gorusmesi,
 * kiralik, serbest oyuncu, acik artirma, takip listesi, soylentiler, gozlemci onerileri,
 * sozlesme yenileme/fesih, on sozlesme (Bosman), sezon sonu kiralik donusleri.
 */
object FbTransfer {

    // ================================================================== deger

    fun leagueLevel(world: FbWorld, club: Int): Int {
        if (club < 0) return 40
        val c = world.clubs[club]
        val tier = world.leagues.getOrNull(c.league)?.tier ?: 3
        return (FbCountries.all[c.country].level - (tier - 1) * 18).coerceIn(10, 100)
    }

    fun value(world: FbWorld, p: FbPlayer): Long {
        val age = world.age(p)
        val years = max(0, p.contractEnd - world.season)
        return FbEconomy.value(p, age, p.currentAbility(), if (p.club < 0) 0 else years + 1, leagueLevel(world, p.club))
    }

    /** Kadrodaki CA sirasi (0 en iyi). */
    fun squadRank(world: FbWorld, p: FbPlayer): Int {
        if (p.club < 0) return 99
        val ca = p.currentAbility()
        return world.clubs[p.club].players.count { world.players[it].currentAbility() > ca }
    }

    /** Satici kulubun kabul edecegi en dusuk bonservis. */
    fun sellerMin(world: FbWorld, p: FbPlayer): Long {
        if (p.club < 0) return 0
        val c = world.clubs[p.club]
        val v = value(world, p).toDouble()
        val rank = squadRank(world, p)
        var m = v * when { rank < 5 -> 1.6; rank < 11 -> 1.3; rank < 18 -> 1.05; else -> 0.8 }
        val age = world.age(p)
        if (age <= 21 && p.potential - p.currentAbility() > 20) m *= 1.2
        if (p.wantsAway) m *= 0.85
        if (c.balance < 0) m *= 0.85
        if (c.players.size <= 20) m *= 1.5
        if (p.askingPrice > 0) m = min(m, p.askingPrice * 0.92)
        if (p.contractEnd <= world.season && world.day >= FbSeason.calendar(world).winterWindowStart) m *= 0.5
        if (p.releaseClause > 0) m = min(m, p.releaseClause.toDouble())
        return FbEconomy.roundValue(max(10_000.0, m).roundToLong())
    }

    // ================================================================== oyuncu istegi

    /** Oyuncunun kulube gelmeye istekli olup olmadigi (itibar, hirs, sadakat). */
    fun willing(world: FbWorld, p: FbPlayer, buyer: Int): Boolean {
        val b = world.clubs[buyer]
        if (p.club < 0) return b.reputation >= p.reputation * 0.35 || world.age(p) >= 31
        val cur = world.clubs[p.club]
        val ratio = b.reputation.toFloat() / max(100, cur.reputation)
        val ambition = p.hidden(Hidden.AMBITION); val loyalty = p.hidden(Hidden.LOYALTY)
        var need = 0.85f + (ambition - 10) * 0.015f + (loyalty - 10) * 0.012f
        if (p.wantsAway) need -= 0.3f
        if (world.age(p) >= 30) need -= 0.15f
        if (squadRank(world, p) >= 18) need -= 0.25f
        if (b.reputation < p.reputation * 0.3f) return false
        return ratio >= need
    }

    fun demand(world: FbWorld, p: FbPlayer, buyer: Int): ContractDemand {
        val b = world.clubs[buyer]
        val age = world.age(p)
        var w = FbEconomy.wage(p.currentAbility(), age, b.reputation).toDouble()
        w *= 1.0 + (p.hidden(Hidden.AMBITION) - 10) * 0.02
        if (p.club >= 0 && p.club != buyer) w = max(w, p.wage * 1.1)
        if (p.club == buyer) w = max(w, p.wage * (if (p.currentAbility() > 120) 1.12 else 1.03))
        val wage = roundWage(w)
        val (mn, mx) = when { age <= 23 -> 2 to 5; age <= 29 -> 2 to 4; age <= 32 -> 1 to 3; else -> 1 to 2 }
        val fee = if (p.club < 0) (wage * 26L) else 0L
        return ContractDemand(wage, mn, mx, FbEconomy.roundValue(fee))
    }

    fun roundWage(w: Double): Int = when {
        w < 1000 -> (w / 50).roundToInt() * 50
        w < 20000 -> (w / 250).roundToInt() * 250
        else -> (w / 1000).roundToInt() * 1000
    }.coerceAtLeast(150)

    private fun reply(world: FbWorld, p: FbPlayer, buyer: Int, wage: Int, years: Int, rnd: FbRandom): ContractReply {
        val d = demand(world, p, buyer)
        if (p.club != buyer && !willing(world, p, buyer)) return ContractReply.NOT_INTERESTED
        if (years < d.minYears || years > d.maxYears) return ContractReply.YEARS
        val tol = 0.92f + rnd.float() * 0.05f
        return if (wage >= d.wage * tol) ContractReply.ACCEPT else ContractReply.WAGE_LOW
    }

    // ================================================================== kullanici: alim

    private fun rng(world: FbWorld, salt: Long) = FbRandom(world.seed xor (world.season * 1_000_003L) xor (world.day * 7919L) xor salt)

    fun windowOpen(world: FbWorld) = FbSeason.calendar(world).isWindow(world.day)

    /** Teklif ver. Donus: teklif veya hata kodu. */
    fun makeOffer(world: FbWorld, buyer: Int, playerId: Int, fee: Long, loan: Boolean = false, loanWagePct: Int = 100, sellOnPct: Int = 0): Pair<FbOffer?, String?> {
        val p = world.players[playerId]
        if (p.club < 0) return null to "FREE_AGENT"
        if (p.club == buyer) return null to "OWN"
        if (!windowOpen(world)) return null to "WINDOW"
        if (p.loanFrom >= 0) return null to "ON_LOAN"
        if (!loan && world.clubs[buyer].balance < fee) return null to "FUNDS"
        val m = world.market
        if (m.offers.any { it.open && it.player == playerId && it.from == buyer }) return null to "DUPLICATE"
        val o = FbOffer(m.nextId++, playerId, buyer, p.club, fee, loan, loanWagePct.coerceIn(0, 100), sellOnPct.coerceIn(0, 50))
        o.season = world.season; o.responseDay = world.day // cevap gece gelir
        m.offers.add(o)
        return o to null
    }

    fun withdraw(world: FbWorld, o: FbOffer) { if (o.open) o.status = OfferStatus.WITHDRAWN }

    /** Karsi teklifi kabul et (alici kullanici). */
    fun acceptCounter(world: FbWorld, o: FbOffer): Boolean {
        if (o.status != OfferStatus.COUNTER) return false
        if (world.clubs[o.from].balance < o.counterFee) return false
        o.fee = o.counterFee; o.status = OfferStatus.AGREED
        return true
    }

    /** Karsi teklife yeni bedelle cevap ver. */
    fun raiseOffer(world: FbWorld, o: FbOffer, fee: Long) {
        if (o.status != OfferStatus.COUNTER) return
        o.fee = fee; o.status = OfferStatus.PENDING; o.responseDay = world.day
    }

    /** Anlasilan transferde oyuncuya sozlesme teklifi (alici kullanici). */
    fun offerContract(world: FbWorld, o: FbOffer, wage: Int, years: Int, releaseClause: Int = 0): ContractReply {
        if (o.status != OfferStatus.AGREED) return ContractReply.WALKED_AWAY
        val p = world.players[o.player]
        if (o.loan) { complete(world, o, p.wage, 1, 0); return ContractReply.ACCEPT }
        val r = reply(world, p, o.from, wage, years, rng(world, o.id.toLong()))
        if (r == ContractReply.ACCEPT) { complete(world, o, wage, years, releaseClause); return r }
        if (r == ContractReply.NOT_INTERESTED) { o.status = OfferStatus.PLAYER_REFUSED; return r }
        o.patience--
        if (o.patience <= 0) { o.status = OfferStatus.PLAYER_REFUSED; return ContractReply.WALKED_AWAY }
        return r
    }

    /** Serbest oyuncuyla dogrudan sozlesme (pencere disinda da). */
    fun signFreeAgent(world: FbWorld, club: Int, playerId: Int, wage: Int, years: Int): ContractReply {
        val p = world.players[playerId]
        if (p.club != CLUB_NONE) return ContractReply.NOT_INTERESTED
        val r = reply(world, p, club, wage, years, rng(world, playerId.toLong()))
        if (r != ContractReply.ACCEPT) return r
        val d = demand(world, p, club)
        world.clubs[club].book(Fin.TRANSFER_OUT, d.signingFee)
        moveTo(world, p, club, wage, years)
        record(world, p, CLUB_NONE, club, 0, false)
        return r
    }

    // ================================================================== kullanici: satis ve sozlesme

    /** Gelen teklife cevap (satici kullanici): kabul, ret ya da karsi teklif. */
    fun respondIncoming(world: FbWorld, o: FbOffer, accept: Boolean, counter: Long = 0) {
        if (o.status != OfferStatus.PENDING || o.to != world.userClub) return
        when {
            accept -> { o.status = OfferStatus.AGREED; o.responseDay = world.day }
            counter > 0 -> { o.counterFee = counter; o.status = OfferStatus.COUNTER; o.responseDay = world.day }
            else -> o.status = OfferStatus.REJECTED
        }
    }

    fun setListed(p: FbPlayer, askingPrice: Long) { p.askingPrice = max(0, askingPrice) }
    fun setLoanListed(p: FbPlayer, listed: Boolean) { p.loanListed = listed }

    /** Sozlesme yenileme talebi (kendi oyuncusu). */
    fun renewalDemand(world: FbWorld, p: FbPlayer): ContractDemand = demand(world, p, p.club)

    fun offerRenewal(world: FbWorld, p: FbPlayer, wage: Int, years: Int, releaseClause: Int = 0): ContractReply {
        if (p.club < 0) return ContractReply.NOT_INTERESTED
        if (p.preContract >= 0) return ContractReply.NOT_INTERESTED
        if (p.wantsAway && p.morale < 40) return ContractReply.NOT_INTERESTED
        val r = reply(world, p, p.club, wage, years, rng(world, p.id * 13L))
        if (r == ContractReply.ACCEPT) {
            p.wage = wage; p.contractEnd = world.season + years - (if (world.day < 180) 1 else 0); p.releaseClause = releaseClause
            p.morale = min(100, p.morale + 8); p.wantsAway = false
        }
        return r
    }

    /** Sozlesme feshi: kalan maasin %60'i tazminat. */
    fun releaseCost(world: FbWorld, p: FbPlayer): Long {
        val weeks = max(1, (p.contractEnd - world.season + 1) * 52 - world.day / 7)
        return p.wage.toLong() * weeks * 6 / 10
    }

    fun release(world: FbWorld, p: FbPlayer) {
        if (p.club < 0) return
        val c = world.clubs[p.club]
        c.book(Fin.OTHER_OUT, releaseCost(world, p))
        c.players.remove(p.id); world.formationCache.remove(c.id)
        news(world, NewsKind.RELEASED, p.id, c.id, -1, 0)
        p.club = CLUB_NONE; p.askingPrice = 0; p.loanListed = false; p.wage = 0; p.contractEnd = world.season
    }

    // ================================================================== acik artirma

    fun startAuction(world: FbWorld, p: FbPlayer, reserve: Long, days: Int = 3): FbAuction? {
        if (p.club < 0 || !windowOpen(world)) return null
        val m = world.market
        if (m.auctions.any { !it.done && it.player == p.id }) return null
        val end = world.day + days.coerceIn(1, 7)
        val a = FbAuction(m.nextId++, p.id, p.club, reserve, world.season, min(end, 364))
        m.auctions.add(a)
        return a
    }

    /** Kullanici (veya YZ) teklif yukseltir. */
    fun bid(world: FbWorld, a: FbAuction, club: Int, amount: Long): Boolean {
        if (a.done || club == a.seller || amount <= a.topBid || world.clubs[club].balance < amount) return false
        if (a.topBid > 0 && amount < nextBid(a.topBid)) return false
        a.topBid = amount; a.topBidder = club; a.bids++
        if (a.seller == world.userClub || club == world.userClub || a.topBidder == world.userClub)
            news(world, NewsKind.AUCTION_BID, a.player, club, a.seller, amount)
        return true
    }

    fun minIncrement(v: Long): Long = FbEconomy.roundValue(max(10_000L, v / 20))

    /** Gecerli bir sonraki teklif (yuvarlanmis, mevcut tekliften buyuk). */
    fun nextBid(top: Long): Long { val n = FbEconomy.roundValue(top + minIncrement(top)); return if (n > top) n else top + minIncrement(top) }

    // ================================================================== gozlemci

    /** Kulubun gozlemcisinin potansiyel tahmini (yanilma payi gozlemci yetenegine bagli, kalici). */
    fun estimate(world: FbWorld, club: Int, p: FbPlayer): Pair<Int, Int> {
        if (p.id in world.revealed) return p.currentAbility() to p.potential
        val c = world.clubs[club]
        val own = p.club == club
        // Kendi oyuncusunu antrenorler/yardimci, digerlerini gozlemciler degerlendirir
        val ja = if (own) 18f else FbClubLife.bestStaff(world, c, StaffAttr.JUDGING_ABILITY, StaffRole.SCOUT, StaffRole.ASSISTANT)
        val jp = if (own) FbClubLife.bestStaff(world, c, StaffAttr.JUDGING_POTENTIAL, StaffRole.ASSISTANT, StaffRole.COACH, StaffRole.YOUTH_COACH)
            else FbClubLife.bestStaff(world, c, StaffAttr.JUDGING_POTENTIAL, StaffRole.SCOUT)
        val r = FbRandom(world.seed xor (p.id * 7_777_777L) xor (club * 31L))
        val ca = (p.currentAbility() + r.gauss() * (20f - ja) * 0.9f).roundToInt().coerceIn(1, 200)
        val pa = (p.potential + r.gauss() * (20f - jp) * 1.6f).roundToInt().coerceIn(ca, 200)
        return ca to pa
    }

    /** Kadro ihtiyacina gore oneriler (butce ve oyuncunun istegi dikkate alinir). */
    fun recommend(world: FbWorld, club: Int, maxFee: Long, limit: Int = 15): List<Recommendation> {
        val c = world.clubs[club]
        val squad = world.clubPlayers(club)
        val out = ArrayList<Recommendation>()
        for (line in listOf(Pos.Line.GK, Pos.Line.DEF, Pos.Line.MID, Pos.Line.ATT)) {
            val mine = squad.filter { it.bestPos().line == line }.map { it.currentAbility() }.sortedDescending()
            val starters = when (line) { Pos.Line.GK -> 1; Pos.Line.DEF -> 4; Pos.Line.MID -> 3; Pos.Line.ATT -> 3 }
            val weakest = mine.getOrNull(starters - 1) ?: 40
            for (p in world.players) {
                if (p.retired || p.club == club || p.bestPos().line != line) continue
                if (p.club >= 0 && p.loanFrom >= 0) continue
                val ca = p.currentAbility()
                val young = world.age(p) <= 21 && p.potential >= weakest + 15
                if (ca < weakest + 4 && !young) continue
                if (ca > weakest + 45) continue
                val fee = if (p.club < 0) 0 else sellerMin(world, p)
                if (fee > maxFee) continue
                if (!willing(world, p, club)) continue
                val (eca, epa) = estimate(world, club, p)
                out.add(Recommendation(p.id, eca, epa, fee, line))
            }
        }
        return out.sortedByDescending { r ->
            val gain = r.estCa + (r.estPa - r.estCa) * 0.3f
            gain / (1f + r.value / 2e6f).let { kotlin.math.sqrt(it) }
        }.take(limit)
    }

    // ================================================================== tamamlama

    private fun moveTo(world: FbWorld, p: FbPlayer, to: Int, wage: Int, years: Int) {
        if (p.club >= 0) { world.clubs[p.club].players.remove(p.id); world.formationCache.remove(p.club) }
        val c = world.clubs[to]
        c.players.add(p.id); world.formationCache.remove(to)
        p.club = to; p.wage = wage
        p.contractEnd = world.season + years - (if (world.day < 180) 1 else 0)
        p.askingPrice = 0; p.loanListed = false; p.wantsAway = false; p.preContract = -1; p.playTime = 0
        p.morale = min(100, p.morale + 10)
        val used = c.players.filter { it != p.id }.map { world.players[it].squadNumber }.toSet()
        if (p.squadNumber in used || p.squadNumber == 0) p.squadNumber = (2..99).firstOrNull { it !in used } ?: 99
    }

    private fun complete(world: FbWorld, o: FbOffer, wage: Int, years: Int, clause: Int) {
        val p = world.players[o.player]
        if (p.club != o.to) { o.status = OfferStatus.EXPIRED; return }
        val buyer = world.clubs[o.from]; val seller = world.clubs[o.to]
        if (o.loan) {
            if (o.fee > 0) { buyer.book(Fin.TRANSFER_OUT, o.fee); seller.book(Fin.TRANSFER_IN, o.fee) }
            seller.players.remove(p.id); world.formationCache.remove(seller.id)
            buyer.players.add(p.id); world.formationCache.remove(buyer.id)
            p.loanFrom = seller.id; p.club = buyer.id; p.loanUntil = world.season; p.loanWagePct = o.loanWagePct
            p.loanListed = false; p.playTime = 0
            val used = buyer.players.filter { it != p.id }.map { world.players[it].squadNumber }.toSet()
            if (p.squadNumber in used) p.squadNumber = (2..99).firstOrNull { it !in used } ?: 99
        } else {
            buyer.book(Fin.TRANSFER_OUT, o.fee)
            var toSeller = o.fee
            if (p.sellOnClub >= 0 && p.sellOnClub != seller.id && p.sellOnPct > 0) {
                val cut = o.fee * p.sellOnPct / 100
                world.clubs[p.sellOnClub].book(Fin.TRANSFER_IN, cut); toSeller -= cut
            }
            seller.book(Fin.TRANSFER_IN, toSeller)
            buyer.transferBudget = max(0, buyer.transferBudget - o.fee)
            seller.transferBudget += toSeller / 2
            moveTo(world, p, buyer.id, wage, years)
            p.releaseClause = clause
            p.sellOnClub = if (o.sellOnPct > 0) seller.id else -1; p.sellOnPct = o.sellOnPct
        }
        o.status = OfferStatus.COMPLETED
        record(world, p, seller.id, buyer.id, o.fee, o.loan)
        // Ayni oyuncu icin diger acik teklifler duser
        for (x in world.market.offers) if (x !== o && x.open && x.player == p.id) x.status = OfferStatus.EXPIRED
    }

    private fun record(world: FbWorld, p: FbPlayer, from: Int, to: Int, fee: Long, loan: Boolean) {
        val m = world.market
        m.history.add(FbTransferRecord(world.season, world.day, p.fullName, p.id, from, to, fee, loan))
        while (m.history.size > 8000) m.history.removeAt(0)
        val user = world.userClub
        if (from == user || to == user) news(world, NewsKind.TRANSFER_DONE, p.id, from, to, fee)
        else if (fee >= 15_000_000 || (to >= 0 && world.clubs[to].country == world.clubs.getOrNull(user)?.country && fee >= 2_000_000))
            news(world, NewsKind.TRANSFER_WORLD, p.id, from, to, fee)
    }

    private fun news(world: FbWorld, kind: NewsKind, player: Int, club: Int, club2: Int, value: Long, cred: Int = 0) {
        val n = FbNews(kind, world.season, world.day, player, club, club2, value)
        n.credibility = cred
        world.market.addNews(n)
    }

    // ================================================================== gunluk

    /** Her gun (FbClubLife.onDay ardindan). */
    fun onDay(world: FbWorld) {
        val cal = FbSeason.calendar(world)
        val window = cal.isWindow(world.day)
        val rnd = rng(world, 0xABCL)
        resolveOffers(world, rnd)
        auctions(world, rnd)
        if (window) {
            aiMarket(world, rnd, deadline = cal.isDeadline(world.day))
            rumours(world, rnd)
        }
        if (world.day == cal.winterWindowStart) contractWarnings(world)
        if (world.day == cal.winterWindowStart + 14) preContracts(world, rnd)
        // Pencere kapaninca acik kulup-kulup teklifleri duser
        if (!window) for (o in world.market.offers) if (o.open && o.season == world.season) o.status = OfferStatus.EXPIRED
        if (cal.weekday(world.day) == FbCalendar.MON) {
            unhappiness(world, rnd)
            if (window) distress(world)
        }
    }

    private fun resolveOffers(world: FbWorld, rnd: FbRandom) {
        val user = world.userClub
        for (o in world.market.offers.toList()) {
            if (!o.open || o.responseDay > world.day) continue
            val p = world.players[o.player]
            if (p.club != o.to) { o.status = OfferStatus.EXPIRED; continue }
            when {
                // Kullanici alici: satici YZ cevaplar
                o.from == user && o.status == OfferStatus.PENDING -> {
                    if (o.loan) {
                        val rank = squadRank(world, p)
                        val ok = (rank >= 14 || p.loanListed || world.age(p) <= 20) && o.loanWagePct >= (if (p.loanListed) 40 else 60) &&
                            world.clubs[user].reputation >= world.clubs[p.club].reputation * 0.4
                        o.status = if (ok) OfferStatus.AGREED else OfferStatus.REJECTED
                        news(world, if (ok) NewsKind.OFFER_ACCEPTED else NewsKind.OFFER_REJECTED, p.id, o.to, o.from, o.fee)
                        continue
                    }
                    val min = sellerMin(world, p)
                    o.rounds++
                    when {
                        o.fee >= min || (p.releaseClause in 1..o.fee) -> { o.status = OfferStatus.AGREED; news(world, NewsKind.OFFER_ACCEPTED, p.id, o.to, o.from, o.fee) }
                        o.fee >= min * 0.65 && o.rounds <= 3 -> {
                            o.counterFee = FbEconomy.roundValue((min * (1.08 - o.rounds * 0.03)).roundToLong())
                            o.status = OfferStatus.COUNTER
                            news(world, NewsKind.OFFER_COUNTER, p.id, o.to, o.from, o.counterFee)
                        }
                        else -> { o.status = OfferStatus.REJECTED; news(world, NewsKind.OFFER_REJECTED, p.id, o.to, o.from, o.fee) }
                    }
                }
                // Kullanici satici, karsi teklif verdi: YZ alici cevaplar
                o.to == user && o.status == OfferStatus.COUNTER -> {
                    val max = buyerMax(world, o.from, p)
                    if (o.counterFee <= max && world.clubs[o.from].balance >= o.counterFee) {
                        o.fee = o.counterFee; o.status = OfferStatus.AGREED
                        news(world, NewsKind.OFFER_ACCEPTED, p.id, o.from, o.to, o.fee)
                    } else { o.status = OfferStatus.REJECTED; news(world, NewsKind.OFFER_REJECTED, p.id, o.from, o.to, o.counterFee) }
                }
                // Kullanici satici, kabul etti: YZ alici oyuncuyla anlasir
                o.to == user && o.status == OfferStatus.AGREED -> aiSign(world, o, rnd)
            }
        }
    }

    /** YZ alicinin oyuncuyla sozlesmesi. */
    private fun aiSign(world: FbWorld, o: FbOffer, rnd: FbRandom) {
        val p = world.players[o.player]
        if (o.loan) { complete(world, o, p.wage, 1, 0); return }
        if (!willing(world, p, o.from)) {
            o.status = OfferStatus.PLAYER_REFUSED
            if (o.to == world.userClub || o.from == world.userClub) news(world, NewsKind.PLAYER_REFUSED, p.id, o.from, o.to, o.fee)
            return
        }
        val d = demand(world, p, o.from)
        complete(world, o, d.wage, d.minYears + rnd.int(d.maxYears - d.minYears + 1), 0)
    }

    /** Haftalik maas tavanina kalan pay (gelirin %72'si). */
    fun wageRoom(world: FbWorld, c: FbClub): Long {
        var inc = c.lastLedger.withIndex().filter { Fin.entries[it.index].income && it.index != Fin.TRANSFER_IN.ordinal }.sumOf { it.value }
        if (inc <= 0) {
            val lg = world.leagues.getOrNull(c.league)
            val tv = if (lg != null) (FbClubLife.tvPool(world, lg) / lg.clubs.size).toLong() else 0L
            val tickets = if (lg != null) ((lg.clubs.size - 1) * FbClubLife.attendance(world, c, -1, CompKind.LEAGUE) * FbClubLife.ticketPrice(world, c) *
                FbClubLife.revenueMul(world, c)).toLong() else 0L
            inc = (FbClubLife.merchYear(world, c) + FbClubLife.adsYear(world, c)).toLong() + c.sponsorIncome + tv + tickets
        }
        val wages = c.players.sumOf { world.players[it].wage.toLong() }
        return inc / 52 * 72 / 100 - wages
    }

    /** YZ alicinin bir oyuncuya odeyebilecegi en yuksek bedel. */
    fun buyerMax(world: FbWorld, buyer: Int, p: FbPlayer): Long {
        val c = world.clubs[buyer]
        val v = value(world, p).toDouble()
        val budget = min(c.transferBudget.toDouble(), max(0.0, c.balance.toDouble()))
        return min(v * 1.35, budget).roundToLong()
    }

    // ================================================================== YZ pazar

    private class MarketIndex(val byLine: Array<IntArray>, val caOf: IntArray)

    private fun marketIndex(world: FbWorld): MarketIndex {
        val m = world.market
        val key = world.season * 1000L + world.day / 3
        (m.indexCache as? MarketIndex)?.let { if (m.indexKey == key && it.caOf.size == world.players.size) return it }
        val caOf = IntArray(world.players.size)
        val lists = Array(4) { ArrayList<Int>() }
        for (p in world.players) {
            if (p.retired || p.club == CLUB_ABROAD) continue
            caOf[p.id] = p.currentAbility()
            lists[p.bestPos().line.ordinal].add(p.id)
        }
        val idx = MarketIndex(Array(4) { l -> lists[l].sortedBy { caOf[it] }.toIntArray() }, caOf)
        m.indexCache = idx; m.indexKey = key
        return idx
    }

    /** CA araligindaki ilk indeks (artan siralamada). */
    private fun lowerBound(arr: IntArray, caOf: IntArray, ca: Int): Int {
        var lo = 0; var hi = arr.size
        while (lo < hi) { val mid = (lo + hi) ushr 1; if (caOf[arr[mid]] < ca) lo = mid + 1 else hi = mid }
        return lo
    }

    private fun aiMarket(world: FbWorld, rnd: FbRandom, deadline: Boolean) {
        val idx = marketIndex(world)
        val clubs = world.clubs.filter { it.league >= 0 && it.id != world.userClub }
        val tries = if (deadline) 90 else 45
        repeat(tries) {
            val c = clubs[rnd.int(clubs.size)]
            if (c.transferBudget <= 0 && c.players.size >= 22) return@repeat
            aiClubTurn(world, c, idx, rnd)
        }
        // YZ kiralamalari: buyuk kulupler gencleri asagi gonderir
        repeat(if (deadline) 25 else 8) {
            val c = clubs[rnd.int(clubs.size)]
            aiLoanOut(world, c, rnd)
        }
    }

    private fun aiClubTurn(world: FbWorld, c: FbClub, idx: MarketIndex, rnd: FbRandom) {
        val squad = c.players.map { world.players[it] }
        // Zayif hat: ilk 11 icindeki en zayif hat ortalamasi
        val lines = Pos.Line.entries
        val need = IntArray(4) { 0 }
        val starters = intArrayOf(1, 4, 3, 3)
        var bestLine = -1; var bestGap = -999
        val topAvg = squad.map { it.currentAbility() }.sortedDescending().take(11).average().takeIf { !it.isNaN() } ?: 80.0
        for (l in lines) {
            val cas = squad.filter { it.bestPos().line == l }.map { it.currentAbility() }.sortedDescending()
            val weakest = cas.getOrNull(starters[l.ordinal] - 1) ?: 30
            need[l.ordinal] = weakest
            val countMin = intArrayOf(2, 7, 6, 4)[l.ordinal]
            val gap = (topAvg - weakest).toInt() + (if (cas.size < countMin) 25 else 0)
            if (gap > bestGap) { bestGap = gap; bestLine = l.ordinal }
        }
        if (bestLine < 0 || (bestGap < 4 && c.players.size >= 22)) return
        val target = need[bestLine]
        val arr = idx.byLine[bestLine]
        val lo = lowerBound(arr, idx.caOf, target + 3)
        val hi = lowerBound(arr, idx.caOf, target + 22)
        if (hi <= lo) return
        val budget = min(c.transferBudget, max(0, c.balance))
        repeat(6) {
            val pid = arr[lo + rnd.int(hi - lo)]
            val p = world.players[pid]
            if (p.club == c.id || p.retired || p.loanFrom >= 0 || p.club == CLUB_ABROAD) return@repeat
            if (world.age(p) >= 33 && rnd.chance(0.7f)) return@repeat
            val room = wageRoom(world, c)
            if (p.club == CLUB_NONE) {
                if (!willing(world, p, c.id)) return@repeat
                val d = demand(world, p, c.id)
                if (d.wage > room && c.players.size >= 22) return@repeat
                if (d.wage > FbEconomy.wage(p.currentAbility(), world.age(p), c.reputation) * 1.4) return@repeat
                c.book(Fin.TRANSFER_OUT, d.signingFee)
                moveTo(world, p, c.id, d.wage, d.minYears)
                record(world, p, CLUB_NONE, c.id, 0, false)
                return
            }
            val seller = world.clubs[p.club]
            if (seller.reputation > c.reputation * 1.25f && squadRank(world, p) < 14) return@repeat
            if (seller.players.size <= 19) return@repeat
            val min = sellerMin(world, p)
            val max = min(buyerMax(world, c.id, p), budget)
            if (min > max) return@repeat
            if (!willing(world, p, c.id)) return@repeat
            if (demand(world, p, c.id).wage > room + (if (c.players.size > 24) p.wage / 2 else 0)) return@repeat
            val fee = FbEconomy.roundValue(((min + max) / 2.0 * (0.9 + rnd.float() * 0.1)).roundToLong().coerceAtLeast(min))
            if (seller.id == world.userClub) {
                // Kullanicinin oyuncusuna teklif: gelen kutusuna
                val m = world.market
                if (m.offers.count { it.open && it.to == seller.id } >= 4) return
                if (m.offers.any { it.open && it.player == p.id && it.from == c.id }) return
                val o = FbOffer(m.nextId++, p.id, c.id, seller.id, FbEconomy.roundValue((fee * (0.8 + rnd.float() * 0.15)).roundToLong()), false, 100, 0)
                o.season = world.season; o.responseDay = world.day + 5
                m.offers.add(o)
                news(world, NewsKind.OFFER_RECEIVED, p.id, c.id, seller.id, o.fee)
                return
            }
            val o = FbOffer(world.market.nextId++, p.id, c.id, seller.id, fee, false, 100, 0)
            o.season = world.season
            aiSign(world, o, rnd)
            if (o.status == OfferStatus.COMPLETED) {
                // Satici yerine yenisini arayabilsin diye butcesi artti (complete icinde)
                return
            }
        }
    }

    private fun aiLoanOut(world: FbWorld, c: FbClub, rnd: FbRandom) {
        if (c.players.size < 24) return
        val young = c.players.map { world.players[it] }.filter { world.age(it) <= 21 && it.loanFrom < 0 && squadRank(world, it) >= 18 && it.potential > it.currentAbility() + 10 }
        if (young.isEmpty()) return
        val p = young[rnd.int(young.size)]
        // Hedef: ayni ulkede daha alt seviye, oyuncunun oynayabilecegi kulup
        val cands = world.clubs.filter { it.league >= 0 && it.id != c.id && it.id != world.userClub && it.country == c.country && it.reputation < c.reputation && it.players.size < 28 }
        if (cands.isEmpty()) return
        val to = cands[rnd.int(cands.size)]
        val o = FbOffer(world.market.nextId++, p.id, to.id, c.id, 0, true, 50 + rnd.int(51), 0)
        o.season = world.season
        complete(world, o, p.wage, 1, 0)
    }

    // ================================================================== artirma

    private fun auctions(world: FbWorld, rnd: FbRandom) {
        for (a in world.market.auctions) {
            if (a.done) continue
            val p = world.players[a.player]
            if (p.club != a.seller) { a.done = true; continue }
            // YZ teklifleri: ilgilenen kulupler, degerlerinin altinda kaldikca artirir
            val v = value(world, p)
            // Ilgilenenler: odeyebilecek, oyuncunun gelmek isteyecegi kulupler (her artirma icin sabit tavanlar)
            val ar = FbRandom(world.seed xor (a.id * 7919L))
            val bidders = world.clubs.filter { it.league >= 0 && it.id != a.seller && it.id != world.userClub && it.balance > v / 3 && willing(world, p, it.id) }
                .map { it to min(buyerMax(world, it.id, p), (v * (0.8 + ar.float() * 0.55)).roundToLong()) }
                .sortedByDescending { it.second }.take(10)
            for ((b, max) in bidders.shuffled(java.util.Random(rnd.nextLong()))) {
                if (a.topBidder == b.id) continue
                val next = if (a.topBid == 0L) FbEconomy.roundValue(max(a.reserve, (v * 0.4).roundToLong())) else nextBid(a.topBid)
                if (next <= max) bid(world, a, b.id, next)
            }
            val ended = world.season > a.endSeason || (world.season == a.endSeason && world.day >= a.endDay)
            if (!ended) continue
            a.done = true
            if (a.topBidder >= 0 && a.topBid >= a.reserve) {
                val o = FbOffer(world.market.nextId++, p.id, a.topBidder, a.seller, a.topBid, false, 100, 0)
                o.season = world.season
                if (a.topBidder == world.userClub) { o.status = OfferStatus.AGREED; world.market.offers.add(o) }
                else aiSign(world, o, rnd)
                news(world, NewsKind.AUCTION_RESULT, p.id, a.topBidder, a.seller, a.topBid)
            } else news(world, NewsKind.AUCTION_RESULT, p.id, -1, a.seller, a.topBid)
        }
    }

    // ================================================================== haberler

    private fun rumours(world: FbWorld, rnd: FbRandom) {
        val user = world.userClub
        val n = 1 + rnd.int(3)
        val stars = marketIndex(world).let { idx -> idx.byLine.flatMap { arr -> arr.takeLast(80).toList() } }
        repeat(n) {
            val p = world.players[stars[rnd.int(stars.size)]]
            if (p.club < 0) return@repeat
            val cur = world.clubs[p.club]
            val suitors = world.clubs.filter { it.league >= 0 && it.id != p.club && it.reputation >= cur.reputation * 0.95f && it.balance > value(world, p) }
            if (suitors.isEmpty()) return@repeat
            val s = suitors[rnd.int(suitors.size)]
            news(world, NewsKind.RUMOUR, p.id, s.id, p.club, value(world, p), cred = 20 + rnd.int(60))
        }
        // Kullanicinin oyunculari hakkinda
        if (user >= 0 && rnd.chance(0.25f)) {
            val mine = world.clubs[user].players.map { world.players[it] }.sortedByDescending { it.reputation }.take(6)
            if (mine.isNotEmpty()) {
                val p = mine[rnd.int(mine.size)]
                val suitors = world.clubs.filter { it.league >= 0 && it.id != user && it.reputation >= world.clubs[user].reputation * 0.9f }
                if (suitors.isNotEmpty()) news(world, NewsKind.RUMOUR, p.id, suitors[rnd.int(suitors.size)].id, user, value(world, p), cred = 30 + rnd.int(50))
            }
        }
    }

    private fun contractWarnings(world: FbWorld) {
        val u = world.userClub
        if (u < 0) return
        for (pid in world.clubs[u].players) {
            val p = world.players[pid]
            if (p.contractEnd <= world.season && p.loanFrom < 0) news(world, NewsKind.CONTRACT_EXPIRING, p.id, u, -1, p.wage.toLong())
        }
    }

    /** Ocak ortasi: sozlesmesi biten oyuncularla YZ on sozlesme (Bosman) yapar. */
    private fun preContracts(world: FbWorld, rnd: FbRandom) {
        for (p in world.players) {
            if (p.club < 0 || p.contractEnd > world.season || p.preContract >= 0 || p.loanFrom >= 0) continue
            val isUser = p.club == world.userClub
            if (!isUser && !rnd.chance(0.25f)) continue
            if (p.currentAbility() < 90 || world.age(p) >= 32) continue
            if (!rnd.chance(if (isUser) 0.6f else 0.5f)) continue
            val cur = world.clubs[p.club]
            val suitors = world.clubs.filter { it.league >= 0 && it.id != p.club && it.reputation in (cur.reputation * 0.8f).toInt()..(cur.reputation * 1.6f).toInt() && it.id != world.userClub }
            if (suitors.isEmpty()) continue
            val s = suitors[rnd.int(suitors.size)]
            if (!willing(world, p, s.id)) continue
            p.preContract = s.id; p.preContractWage = demand(world, p, s.id).wage
            if (isUser) news(world, NewsKind.PRECONTRACT, p.id, s.id, p.club, 0)
        }
    }

    /** Borclu YZ kulupleri: butce sifir, yuksek maasli oyunculari satisa cikar, fazlalari serbest birakir. */
    private fun distress(world: FbWorld) {
        for (c in world.clubs) {
            if (c.league < 0 || c.id == world.userClub || c.balance >= 0) continue
            c.transferBudget = 0
            val ps = c.players.map { world.players[it] }.filter { it.loanFrom < 0 }
            val byWage = ps.sortedByDescending { it.wage }
            var listed = ps.count { it.askingPrice > 0 }
            for (p in byWage) {
                if (listed >= 3) break
                if (p.askingPrice == 0L) { p.askingPrice = FbEconomy.roundValue((value(world, p) * 0.85).roundToLong()); listed++ }
            }
            // Yedekte oturan yuksek maasli yaslilar: karsilikli fesih
            if (c.players.size > 24) byWage.firstOrNull { world.age(it) >= 31 && squadRank(world, it) >= 16 }?.let { release(world, it) }
        }
    }

    private fun unhappiness(world: FbWorld, rnd: FbRandom) {
        for (c in world.clubs) {
            if (c.league < 0) continue
            for (pid in c.players) {
                val p = world.players[pid]
                if (!p.wantsAway && p.morale < 30 && p.hidden(Hidden.AMBITION) > 9 && rnd.chance(0.15f)) {
                    p.wantsAway = true
                    if (c.id == world.userClub) news(world, NewsKind.PLAYER_UNHAPPY, p.id, c.id, -1, 0)
                    else if (p.askingPrice == 0L) p.askingPrice = sellerMin(world, p)
                } else if (p.wantsAway && p.morale > 60) p.wantsAway = false
            }
        }
    }

    // ================================================================== sezon gecisi

    /** Sezon sonu: kiralik donusu, on sozlesmeler, kullanicinin biten sozlesmeleri. Emeklilik/YZ yenilemeden once. */
    fun onSeasonEnd(world: FbWorld) {
        for (p in world.players) {
            if (p.loanFrom >= 0 && p.loanUntil <= world.season) {
                val parent = p.loanFrom
                if (p.club >= 0) world.clubs[p.club].players.remove(p.id)
                world.clubs[parent].players.add(p.id)
                if (parent == world.userClub || p.club == world.userClub) news(world, NewsKind.LOAN_RETURN, p.id, parent, p.club, 0)
                p.club = parent; p.loanFrom = -1; p.loanWagePct = 100; p.playTime = 0
            }
        }
        for (p in world.players) {
            if (p.preContract >= 0 && p.club != CLUB_RETIRED) {
                val from = p.club
                val to = p.preContract
                if (from >= 0) world.clubs[from].players.remove(p.id)
                world.clubs[to].players.add(p.id)
                p.club = to; p.wage = p.preContractWage; p.contractEnd = world.season + 1 + 2
                p.preContract = -1; p.askingPrice = 0
                record(world, p, from, to, 0, false)
            }
        }
        // Kullanici: yenilenmeyen sozlesmeler biter
        val u = world.userClub
        if (u >= 0) for (pid in world.clubs[u].players.toList()) {
            val p = world.players[pid]
            if (p.contractEnd <= world.season && p.loanFrom < 0) {
                world.clubs[u].players.remove(pid); p.club = CLUB_NONE; p.wage = 0
                news(world, NewsKind.RELEASED, p.id, u, -1, 0)
            }
        }
        for (o in world.market.offers) if (o.open) o.status = OfferStatus.EXPIRED
        world.market.offers.removeAll { !it.open && it.season < world.season }
        world.market.auctions.removeAll { it.done }
        world.formationCache.clear()
        world.market.indexCache = null
    }

    /** Yeni sezon basinda YZ butceleri. */
    fun setBudgets(world: FbWorld) {
        for (c in world.clubs) {
            if (c.league < 0 || c.id == world.userClub) continue
            val rev = c.lastLedger.withIndex().filter { Fin.entries[it.index].income && it.index != Fin.TRANSFER_IN.ordinal }.sumOf { it.value }
                .takeIf { it > 0 } ?: (c.sponsorIncome * 5)
            c.transferBudget = max(0L, (c.balance * 0.4 + rev * 0.1).roundToLong())
        }
    }

    /** Oyuncu listesi sikistirilinca kimlikleri guncelle. */
    fun remap(world: FbWorld, map: IntArray) {
        val m = world.market
        fun r(id: Int) = if (id in map.indices) map[id] else -1
        m.offers.removeAll { r(it.player) < 0 }
        val offers = m.offers.map { o ->
            FbOffer(o.id, r(o.player), o.from, o.to, o.fee, o.loan, o.loanWagePct, o.sellOnPct).also {
                it.status = o.status; it.counterFee = o.counterFee; it.responseDay = o.responseDay; it.season = o.season; it.rounds = o.rounds; it.patience = o.patience
            }
        }
        m.offers.clear(); m.offers.addAll(offers)
        m.auctions.removeAll { true }
        world.userLineup = world.userLineup?.map { r(it) }?.toIntArray()
        val rv = world.revealed.map { r(it) }.filter { it >= 0 }
        world.revealed.clear(); world.revealed.addAll(rv)
        val sl = m.shortlist.map { r(it) }.filter { it >= 0 }
        m.shortlist.clear(); m.shortlist.addAll(sl)
        for (n in m.news) n.player = r(n.player)
        for (h in m.history) h.player = r(h.player)
        m.indexCache = null
    }

}
