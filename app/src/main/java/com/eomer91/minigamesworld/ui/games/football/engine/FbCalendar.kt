package com.eomer91.minigamesworld.ui.games.football.engine

/**
 * Sezon takvimi. Sezon gunu 0 = 1 Temmuz, 364 = 30 Haziran.
 * Tum mac gunleri gercek takvime gore hafta gunune oturtulur (Cumartesi lig, Sali/Persembe Avrupa,
 * Carsamba kupa/ara hafta, Cuma/Pazartesi milli maclar).
 */
class FbCalendar(val season: Int) {
    private val start = epochDay(season, 7, 1)

    /** 0 = Pazartesi ... 6 = Pazar */
    fun weekday(day: Int): Int = Math.floorMod(start + day + 3, 7L).toInt()

    /** Sezon gunu -> (yil, ay, gun) */
    fun date(day: Int): Triple<Int, Int, Int> = civil(start + day)

    /** Belirli tarihe denk gelen sezon gunu (ay 7..12 sezon yili, 1..6 bir sonraki yil). */
    fun dayOf(month: Int, dom: Int): Int = (epochDay(if (month >= 7) season else season + 1, month, dom) - start).toInt()

    /** [from] gunune esit/sonraki ilk [wd] hafta gunu. */
    fun next(from: Int, wd: Int): Int { var d = from; while (weekday(d) != wd) d++; return d }
    /** [from] gunune esit/onceki ilk [wd] hafta gunu. */
    fun prev(from: Int, wd: Int): Int { var d = from; while (weekday(d) != wd) d--; return d }

    /** Hedef tarihin haftasindaki Sali (Avrupa haftasi baslangici). */
    fun tuesdayOfWeek(month: Int, dom: Int): Int {
        val d = dayOf(month, dom)
        return d - Math.floorMod(weekday(d) - TUE, 7)
    }

    // ------------------------------------------------------------------ sabit takvim

    /** Milli ara cumartesileri (o hafta sonu lig oynanmaz): Eylul, Ekim, Kasim, Mart. */
    val intlSaturdays: IntArray = intArrayOf(next(dayOf(9, 3), SAT), next(dayOf(10, 8), SAT), next(dayOf(11, 12), SAT), next(dayOf(3, 22), SAT))

    /** Milli mac gunleri: her aranin Cuma ve Pazartesisi. */
    val intlDays: IntArray = intlSaturdays.flatMap { listOf(it - 1, it + 2) }.toIntArray()

    /** Haziran milli mac gunleri (turnuva olmayan sezonlarda hazirlik/Uluslar Ligi finali). */
    val juneDays: IntArray = intArrayOf(next(dayOf(6, 3), FRI), next(dayOf(6, 3), FRI) + 4, next(dayOf(6, 3), FRI) + 7, next(dayOf(6, 3), FRI) + 11)

    /** Turnuva (Dunya Kupasi / Avrupa Sampiyonasi) gunleri: 3 grup + 5 eleme turu. */
    val tournamentDays: IntArray = run {
        val g1 = dayOf(6, 5)
        intArrayOf(g1, g1 + 4, g1 + 8, g1 + 12, g1 + 15, g1 + 18, g1 + 21, g1 + 24)
    }

    /** Avrupa on eleme haftalari (Sali): 3 tur x 2 ayak + play-off 2 ayak. */
    val euroQualTuesdays: IntArray = intArrayOf(
        tuesdayOfWeek(7, 8), tuesdayOfWeek(7, 15), tuesdayOfWeek(7, 22), tuesdayOfWeek(7, 29),
        tuesdayOfWeek(8, 5), tuesdayOfWeek(8, 12), tuesdayOfWeek(8, 19), tuesdayOfWeek(8, 26),
    )

    /** Lig asamasi 8 hafta (Sali). */
    val euroLeagueTuesdays: IntArray = intArrayOf(
        tuesdayOfWeek(9, 16), tuesdayOfWeek(9, 30), tuesdayOfWeek(10, 21), tuesdayOfWeek(11, 4),
        tuesdayOfWeek(11, 25), tuesdayOfWeek(12, 9), tuesdayOfWeek(1, 20), tuesdayOfWeek(1, 27),
    )

    /** Eleme haftalari (Sali): play-off, son 16, ceyrek, yari final (her biri 2 ayak). */
    val euroKoTuesdays: IntArray = intArrayOf(
        tuesdayOfWeek(2, 17), tuesdayOfWeek(2, 24), tuesdayOfWeek(3, 10), tuesdayOfWeek(3, 17),
        tuesdayOfWeek(4, 7), tuesdayOfWeek(4, 14), tuesdayOfWeek(4, 28), tuesdayOfWeek(5, 5),
    )

    /** Finaller: Konferans (Carsamba), Avrupa Ligi (Carsamba), Sampiyonlar Ligi (Cumartesi). */
    val eclFinal = next(dayOf(5, 26), WED)
    val elFinal = next(dayOf(5, 19), WED)
    val clFinal = next(dayOf(5, 29), SAT)
    val uefaSuperCup = next(dayOf(8, 11), WED)
    val domesticSuperCup = next(dayOf(8, 1), SAT)

    /** Ulusal kupa turlari (Carsamba, Avrupa haftasi olmayan haftalar) ve final (Cumartesi). */
    val cupDays: IntArray = intArrayOf(
        next(dayOf(8, 26), WED), next(dayOf(9, 23), WED), next(dayOf(10, 28), WED), next(dayOf(12, 16), WED),
        next(dayOf(1, 13), WED), next(dayOf(2, 10), WED), next(dayOf(3, 3), WED), next(dayOf(4, 21), WED),
        prev(dayOf(5, 23), SAT),
    )

    val leagueStart = next(dayOf(8, 8), SAT)
    val leagueEnd = cupDays.last() - 7 // kupa finali haftasindan once
    val playoffDays: IntArray = run { val w = next(dayOf(5, 23), WED) + 7; intArrayOf(w, w + 4, w + 11) }

    /** Avrupa haftasi mi (Pazartesi..Pazar araliginda Sali/Persembe Avrupa maci var mi) */
    fun isEuroWeek(day: Int): Boolean {
        val tue = day - Math.floorMod(weekday(day) - TUE, 7)
        return tue in euroLeagueTuesdays || tue in euroKoTuesdays
    }

    /**
     * Lig turu gunleri: Cumartesiler (milli ara ve kupa finali haric), yetmezse ara hafta Carsambalari
     * (ust lig icin Avrupa ve kupa haftalari haric). Turlar sezona esit dagitilir.
     */
    fun leagueRoundDays(rounds: Int, topTier: Boolean): IntArray {
        val sats = ArrayList<Int>()
        var d = leagueStart
        while (d <= leagueEnd) { if (d !in intlSaturdays) sats.add(d); d += 7 }
        val mids = ArrayList<Int>()
        d = next(leagueStart + 1, WED)
        while (d < leagueEnd) {
            val bad = d in cupDays || (topTier && isEuroWeek(d)) || intlSaturdays.any { kotlin.math.abs(it - d) <= 3 }
            if (!bad) mids.add(d)
            d += 7
        }
        val chosen = ArrayList<Int>()
        if (rounds <= sats.size) {
            for (i in 0 until rounds) chosen.add(sats[(i.toLong() * sats.size / rounds).toInt()])
        } else {
            chosen.addAll(sats)
            val need = rounds - sats.size
            // Ara haftalar: once Aralik-Ocak yogun donemi, sonra esit dagitim
            val ordered = mids.sortedBy { m -> val mo = date(m).second; if (mo == 12 || mo == 1) 0 else 1 }
            val pickFrom = if (need <= ordered.size) ordered.take(need) else ordered
            chosen.addAll(pickFrom)
            var extra = need - pickFrom.size
            // Hala yetmezse pazartesi/carsamba ikilileri (alt ligler)
            d = leagueStart + 3
            while (extra > 0 && d < leagueEnd) { if (d !in chosen && weekday(d) == TUE && d !in intlDays) { chosen.add(d); extra-- }; d += 7 }
        }
        return chosen.sorted().toIntArray()
    }

    companion object {
        const val MON = 0; const val TUE = 1; const val WED = 2; const val THU = 3; const val FRI = 4; const val SAT = 5; const val SUN = 6

        /** 1970-01-01'den gun sayisi (proleptik Gregoryen). */
        fun epochDay(y: Int, m: Int, d: Int): Long {
            val yy = if (m <= 2) y - 1 else y
            val era = Math.floorDiv(yy, 400)
            val yoe = yy - era * 400
            val mp = (m + 9) % 12
            val doy = (153 * mp + 2) / 5 + d - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era.toLong() * 146097 + doe - 719468
        }

        fun civil(z0: Long): Triple<Int, Int, Int> {
            val z = z0 + 719468
            val era = Math.floorDiv(z, 146097L)
            val doe = (z - era * 146097).toInt()
            val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val d = doy - (153 * mp + 2) / 5 + 1
            val m = if (mp < 10) mp + 3 else mp - 9
            val y = yoe + era.toInt() * 400 + if (m <= 2) 1 else 0
            return Triple(y, m, d)
        }
    }
}
