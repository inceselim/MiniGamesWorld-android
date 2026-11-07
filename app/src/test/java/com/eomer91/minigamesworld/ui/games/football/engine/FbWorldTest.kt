package com.eomer91.minigamesworld.ui.games.football.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class FbWorldTest {

    @Test
    fun countriesParse() {
        val all = FbCountries.all
        assertEquals(55, all.count { it.confed == Confed.UEFA })
        assertTrue(all.size > 90)
        for (c in FbCountries.uefa) if (c.tiers.isNotEmpty()) assertTrue("${c.code} sehir", c.cities.isNotEmpty())
        // Kod tekrari yok
        assertEquals(all.size, all.map { it.code }.toSet().size)
    }

    @Test
    fun generateWorld() {
        val t0 = System.currentTimeMillis()
        val w = FbGen(2026L).generate()
        val ms = System.currentTimeMillis() - t0
        println("uretim: ${ms}ms  lig=${w.leagues.size} kulup=${w.clubs.size} oyuncu=${w.players.size} personel=${w.staff.size}")
        assertTrue(w.leagues.size in 90..130)
        assertTrue(w.clubs.size in 1100..1700)
        // Kulup adlari benzersiz
        assertEquals(w.clubs.size, w.clubs.map { it.name }.toSet().size)
        // Her kulupte kaleci var, kadro boyutu makul
        for (c in w.clubs) {
            assertTrue(c.players.size in 20..26)
            assertTrue(c.players.count { w.players[it].bestPos() == Pos.GK } >= 2)
            assertTrue(c.players.all { w.players[it].club == c.id })
        }
        // Lig kalite sirasi: ust lig ortalamasi alt ligden yuksek
        fun avgCa(lg: FbLeague): Double = lg.clubs.flatMap { c -> w.clubs[c].players.map { w.players[it].currentAbility() }.sortedDescending().take(11) }.average()
        val eng = FbCountries.all.indexOfFirst { it.code == "ENG" }
        val engL = w.leaguesOf(eng)
        val tur = w.leaguesOf(FbCountries.all.indexOfFirst { it.code == "TUR" })
        val smr = w.leaguesOf(FbCountries.all.indexOfFirst { it.code == "SMR" })
        val e1 = avgCa(engL[0]); val e2 = avgCa(engL[1]); val e3 = avgCa(engL[2]); val t1 = avgCa(tur[0]); val s1 = avgCa(smr[0])
        println("CA ilk11 ort: ENG1=%.0f ENG2=%.0f ENG3=%.0f TUR1=%.0f SMR=%.0f".format(e1, e2, e3, t1, s1))
        assertTrue(e1 > e2 && e2 > e3 && e1 > t1 && t1 > s1)
        // En iyi oyuncular gercekci araliktaa
        val top = w.players.map { it.currentAbility() }.sortedDescending()
        println("en iyi CA: ${top.take(10)}  medyan: ${top[top.size / 2]}")
        assertTrue(top[0] in 165..200)
        // Potansiyel >= mevcut
        assertTrue(w.players.all { it.potential >= it.currentAbility() - 3 })
        // Genc yetenekler var
        val wonderkids = w.players.count { w.age(it) <= 19 && it.potential >= 160 }
        println("19 yas alti PA>=160: $wonderkids")
        assertTrue(wonderkids >= 5)
        // Ornek kulupler
        for (lg in listOf(engL[0], tur[0])) println(lg.clubs.take(6).map { w.clubs[it].name })
        val star = w.players.maxBy { it.currentAbility() }
        println("Yildiz: ${star.fullName} ${FbCountries.all[star.nation].code} yas=${w.age(star)} CA=${star.currentAbility()} PA=${star.potential} mevki=${star.bestPos()} maas=${star.wage}/hf deger=${FbEconomy.value(star, w.age(star), star.currentAbility(), 2, 90)}")
    }

    @Test
    fun deterministic() {
        val a = FbGen(77L).generate()
        val b = FbGen(77L).generate()
        assertEquals(a.players.size, b.players.size)
        assertEquals(a.players[500].fullName, b.players[500].fullName)
        assertTrue(a.players[500].attrs.contentEquals(b.players[500].attrs))
        assertEquals(a.clubs.map { it.name }, b.clubs.map { it.name })
    }

    @Test
    fun saveRoundTrip() {
        val w = FbGen(5L).generate()
        w.userClub = 12; w.managerName = "Selim"; w.day = 44
        w.players[3].firstName = "Düzenlendi"
        val bos = ByteArrayOutputStream()
        val t0 = System.currentTimeMillis()
        FbSave.write(w, bos)
        val t1 = System.currentTimeMillis()
        val r = FbSave.read(ByteArrayInputStream(bos.toByteArray()))
        val t2 = System.currentTimeMillis()
        println("kayit boyutu ${bos.size() / 1024} KB, yazma ${t1 - t0}ms, okuma ${t2 - t1}ms")
        assertEquals(w.players.size, r.players.size)
        assertEquals("Düzenlendi", r.players[3].firstName)
        assertEquals(w.userClub, r.userClub)
        for (k in listOf(0, 100, 5000, w.players.size - 1)) {
            val a = w.players[k]; val b = r.players[k]
            assertEquals(a.fullName, b.fullName)
            assertTrue(a.attrs.contentEquals(b.attrs))
            assertEquals(a.potential, b.potential); assertEquals(a.club, b.club); assertEquals(a.wage, b.wage)
            assertEquals(a.heightCm, b.heightCm); assertEquals(a.foot, b.foot)
        }
        for (k in listOf(0, w.clubs.size - 1)) {
            assertEquals(w.clubs[k].name, r.clubs[k].name)
            assertEquals(w.clubs[k].players, r.clubs[k].players)
            assertEquals(w.clubs[k].balance, r.clubs[k].balance)
        }
        assertEquals(w.leagues.map { it.clubs }, r.leagues.map { it.clubs })
        assertEquals(w.staff.size, r.staff.size)
    }
}
