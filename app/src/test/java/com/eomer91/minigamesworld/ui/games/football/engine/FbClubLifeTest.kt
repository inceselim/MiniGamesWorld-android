package com.eomer91.minigamesworld.ui.games.football.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FbClubLifeTest {

    private fun clone(w: FbWorld, p: FbPlayer): FbPlayer {
        val c = FbPlayer(p.id, p.firstName, p.lastName, p.nation, p.birthYear, p.birthDay, p.attrs.copyOf(), p.hidden.copyOf(), p.posFam.copyOf(), p.potential, p.foot, p.heightCm)
        c.club = p.club; c.morale = p.morale
        return c
    }

    private fun quality(coach: Float, fac: Float, intensity: Int = 1) =
        TrainQuality(FloatArray(TrainArea.entries.size) { coach }, fac, coach, fac, 10f, coach, 10f, intensity, -1, 10f)

    @Test
    fun developmentDependsOnTraining() {
        val w = FbGen(4L).generate()
        val young = w.players.filter { it.club >= 0 && w.age(it) in 17..18 && it.potential - it.currentAbility() >= 35 }.take(40)
        var good = 0.0; var poor = 0.0; var single = 0.0
        for (p in young) {
            val a = clone(w, p); val b = clone(w, p)
            a.playTime = 300; b.playTime = 300
            val ca0 = p.currentAbility()
            val ra = FbRandom(1); val rb = FbRandom(1)
            repeat(52) { FbClubLife.develop(w, a, quality(18f, 19f), ra); FbClubLife.develop(w, b, quality(5f, 4f), rb) }
            good += a.currentAbility() - ca0; poor += b.currentAbility() - ca0
            assertTrue(a.currentAbility() <= a.potential + 3)
        }
        good /= young.size; poor /= young.size
        println("17-18 yas, 1 yil: iyi tesis/antrenor +%.1f CA, zayif +%.1f CA".format(good, poor))
        assertTrue(good > poor * 1.4)
        assertTrue(good in 6.0..20.0)
        // Yasli oyuncu geriler, fizikseller once
        val old = w.players.filter { it.club >= 0 && w.age(it) >= 33 && it.bestPos() != Pos.GK }.take(30)
        var dCa = 0.0; var dPace = 0.0; var dDec = 0.0
        for (p in old) {
            val a = clone(w, p); val r = FbRandom(2)
            val pace0 = a.attr(Attr.PACE); val dec0 = a.attr(Attr.DECISIONS); val ca0 = a.currentAbility()
            repeat(52) { FbClubLife.develop(w, a, quality(10f, 10f), r) }
            dCa += a.currentAbility() - ca0; dPace += a.attr(Attr.PACE) - pace0; dDec += a.attr(Attr.DECISIONS) - dec0
            single = a.currentAbility().toDouble()
        }
        println("33+ yas, 1 yil: CA %.1f, hiz %.2f, karar %.2f".format(dCa / old.size, dPace / old.size, dDec / old.size))
        assertTrue(dCa / old.size < -2.0)
        assertTrue(dPace / old.size < dDec / old.size)
        assertTrue(single > 0)
    }

    @Test
    fun positionTraining() {
        val w = FbGen(4L).generate()
        val p = clone(w, w.players.first { it.club >= 0 && it.bestPos() == Pos.MC && it.fam(Pos.DM) < 10 && w.age(it) < 24 })
        p.trainPos = Pos.DM.ordinal
        val f0 = p.fam(Pos.DM)
        val r = FbRandom(3)
        repeat(30) { FbClubLife.develop(w, p, quality(14f, 14f), r) }
        println("DM yatkinligi $f0 -> ${p.fam(Pos.DM)} (30 hafta)")
        assertTrue(p.fam(Pos.DM) >= f0 + 3)
    }

    @Test
    fun seasonFinanceAndYouth() {
        val w = FbGen(8L).generate()
        FbSeason.start(w)
        val cal = FbSeason.calendar(w)
        val before = w.players.size
        val clubCount = w.clubs.count { it.league >= 0 }
        while (w.day <= cal.dayOf(3, 15)) FbSeason.advanceDay(w, autoUser = true)
        val youth = w.players.drop(before).filter { it.club >= 0 }
        println("altyapi: ${youth.size} oyuncu ($clubCount kulup), yas " + youth.map { w.age(it) }.distinct().sorted() + ", en iyi PA " + youth.maxOf { it.potential })
        assertTrue(youth.size >= clubCount * 2)
        assertTrue(youth.all { w.age(it) in 15..17 }) // alim gunu dogum gunu olan 17 olabilir
        // Sezonu bitir: defter
        var r: DayReport
        do { r = FbSeason.advanceDay(w, autoUser = true) } while (!r.seasonEnded)
        val clubs = w.clubs.filter { it.league >= 0 }
        fun rev(c: FbClub) = c.lastLedger.withIndex().filter { Fin.entries[it.index].income }.sumOf { it.value }
        fun net(c: FbClub) = c.lastLedger.withIndex().sumOf { (i, v) -> if (Fin.entries[i].income) v else -v }
        val ratios = clubs.map { net(it).toDouble() / maxOf(1L, rev(it)) }.sorted()
        val med = ratios[ratios.size / 2]
        println("net/gelir medyan=%.2f".format(med))
        assertTrue(med in -0.1..0.25)
        val eng = w.leaguesOf(FbCountries.all.indexOfFirst { it.code == "ENG" })[0].clubs.map { w.clubs[it] }.sortedByDescending { it.reputation }
        assertTrue(rev(eng.first()) > rev(eng.last()))
        for (c in clubs) {
            assertTrue(c.lastLedger[Fin.WAGES.ordinal] > 0)
            assertTrue(c.lastLedger[Fin.TV.ordinal] > 0)
        }
        assertTrue(w.players.none { it.retired })
        val sizes = clubs.map { it.players.size }
        assertTrue(sizes.min() >= 20 && sizes.max() <= 34)
    }

    @Test
    fun upgradesStaffSponsorFriendly() {
        val w = FbGen(9L).generate()
        val c = w.clubs.first { it.league >= 0 && w.leagues[it.league].tier == 1 && FbCountries.all[it.country].code == "ENG" }
        w.userClub = c.id
        FbSeason.start(w)
        c.balance = 500_000_000
        val lvl = c.trainingFacilities
        val cost = FbClubLife.upgradeCost(c, Facility.TRAINING)
        assertEquals(null, FbClubLife.startUpgrade(w, c, Facility.TRAINING))
        assertEquals("BUSY", FbClubLife.startUpgrade(w, c, Facility.YOUTH))
        assertEquals(cost, c.ledger[Fin.UPGRADES.ordinal])
        val days = FbClubLife.upgradeDays(c, Facility.TRAINING)
        // Reklamla hizlandirma: kalan sure 30 gun kisalir
        val left0 = FbClubLife.upgradeDaysLeft(w, c)
        assertEquals(days, left0)
        assertEquals(FbClubLife.RUSH_DAYS, FbClubLife.rushUpgrade(w, c))
        assertEquals(days - FbClubLife.RUSH_DAYS, FbClubLife.upgradeDaysLeft(w, c))
        repeat(days - FbClubLife.RUSH_DAYS + 8) { FbSeason.advanceDay(w, autoUser = true) }
        assertEquals(lvl + 1, c.trainingFacilities)
        assertEquals(-1, c.upgradeType)
        // Personel
        val s = FbClubLife.freeStaff(w).first { it.reputation <= c.reputation }
        assertEquals(null, FbClubLife.hireStaff(w, c, s))
        assertTrue(s.id in c.staff)
        FbClubLife.fireStaff(w, c, s)
        assertTrue(s.id !in c.staff && s.club == CLUB_NONE)
        // Sponsor
        val offers = FbClubLife.sponsorOffers(w, c)
        assertEquals(3, offers.size)
        FbClubLife.acceptSponsor(w, c, offers[2])
        assertEquals(offers[2].yearly, c.sponsorIncome)
        // Dostluk maci
        val opp = w.leagues[c.league].clubs.first { it != c.id }
        var day = w.day + 3
        while (FbSeason.arrangeFriendly(w, c.id, opp, day) != FbSeason.FriendlyResult.OK && day < 360) day++
        assertTrue(day < 360)
        while (w.day <= day) FbSeason.advanceDay(w, autoUser = true)
        assertTrue(w.state!!.fixtures.any { w.state!!.comps[it.comp].kind == CompKind.FRIENDLY && it.played })
    }
}
