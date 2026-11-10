package com.eomer91.minigamesworld.ui.games.island.engine

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/** Ada Patronu motoru. Takvim: 12 ay x 30 gun; gun 0 = baslangic yilinin 1 Nisan'i. */

/** Tekrarlanabilir rastgele sayi (SplitMix64); durum kayda yazilir. */
class IsRandom(var state: Long) {
    fun nextLong(): Long {
        state += -0x61c8864680b583ebL
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }
    fun int(n: Int): Int = ((nextLong() ushr 1) % n).toInt()
    fun range(a: Int, b: Int): Int = a + int(b - a + 1)
    fun float(): Float = (nextLong() ushr 40).toFloat() / (1L shl 24).toFloat()
    fun chance(p: Float) = float() < p
    fun gauss(): Float {
        val u1 = max(1e-7f, float()); val u2 = float()
        return (sqrt(-2.0 * ln(u1.toDouble())) * kotlin.math.cos(2.0 * Math.PI * u2)).toFloat()
    }
    fun <T> pick(l: List<T>): T = l[int(l.size)]
}

enum class Terrain { DEEP, WATER, SAND, GRASS, FOREST, HILL;
    val isLand get() = this == SAND || this == GRASS || this == FOREST || this == HILL
}

/** Turist sinifi */
enum class TClass(
    /** Bir gecelik konaklama icin referans butce */
    val stayBudget: Float,
    /** Fiyat duyarliligi: buyukse fiyata daha cok bakar */
    val sens: Float,
    /** Kaliteye verilen onem */
    val qualityW: Float,
) {
    LOW(35f, 3.5f, 0.2f), MID(110f, 2.5f, 0.5f), HIGH(380f, 1.6f, 1f)
}

/** Bina kategorisi. visitRate/visitBudget: turist basina gunluk ziyaret ve ziyaret basina referans harcama (LOW, MID, HIGH). */
enum class Cat(val visitRate: FloatArray?, val visitBudget: FloatArray?, val fin: Fin?) {
    STAY(null, null, Fin.STAY),
    FOOD(floatArrayOf(1.6f, 1.6f, 1.6f), floatArrayOf(8f, 25f, 80f), Fin.FOOD),
    NIGHT(floatArrayOf(0.35f, 0.45f, 0.3f), floatArrayOf(12f, 20f, 40f), Fin.NIGHT),
    FUN(floatArrayOf(0.45f, 0.35f, 0.15f), floatArrayOf(10f, 18f, 30f), Fin.FUN),
    GAMBLE(floatArrayOf(0.04f, 0.12f, 0.35f), floatArrayOf(30f, 80f, 250f), Fin.GAMBLE),
    SHOP(floatArrayOf(0.4f, 0.5f, 0.6f), floatArrayOf(10f, 25f, 60f), Fin.SHOP),
    BEACH(floatArrayOf(0.5f, 0.6f, 0.6f), floatArrayOf(5f, 8f, 15f), Fin.BEACH),
    /** Park, palmiye, cicek: gelir getirmez, cevreyi guzellestirir */
    NATURE(null, null, null),
    ROAD(null, null, null),
    /** Herkese ait: havalimani, iskele */
    PUBLIC(null, null, null);

    val isAmenity get() = visitRate != null
}

/** Gelir-gider kalemleri */
enum class Fin(val income: Boolean) {
    STAY(true), FOOD(true), NIGHT(true), FUN(true), GAMBLE(true), SHOP(true), BEACH(true), SALES(true), OTHER_IN(true),
    BUILD(false), LAND(false), OPERATING(false), UPKEEP(false), ADS(false), INTEREST(false), DIRTY(false), FINES(false), OTHER_OUT(false)
}

/**
 * Bina turu. cap: konaklamada yatak, digerlerinde gunluk ziyaret kapasitesi. price: varsayilan fiyat
 * (gecelik ya da ziyaret basina). varCost: misafir/ziyaret basina degisken gider. upkeep: aylik sabit gider.
 * appeal: sinif basina cekicilik (0..1). noise: cevreye gurultu. beauty: cevreye guzellik.
 */
class BType(
    val id: Int, val key: String, val cat: Cat, val w: Int, val h: Int,
    val cost: Long, baseDays: Int, val upkeep: Long, val cap: Int, val quality: Int,
    val price: Float, val varCost: Float, val appeal: FloatArray,
    val noise: Float = 0f, val beauty: Float = 0f,
    /** Kumsalda ve denize bitisik olmali */
    val needsBeach: Boolean = false,
    /** Oyuncular insa edebilir mi */
    val buildable: Boolean = true,
    /** Yalnizca odullu reklamla kurulur: bina bedeli odenmez (arsa odenir), botlar kurmaz, yikinca para gelmez */
    val adOnly: Boolean = false,
) {
    /** Insaat suresi: kucuk susler disinda temel surenin 1.6 kati (reklamla kisaltilabilir) */
    val days: Int = if (baseDays <= 3) baseDays else kotlin.math.round(baseDays * BUILD_TIME_MUL).toInt()

    companion object { const val BUILD_TIME_MUL = 1.6f }
}

object IsCatalog {
    private fun a(l: Float, m: Float, h: Float) = floatArrayOf(l, m, h)
    val types: List<BType> = listOf(
        // ---- konaklama
        BType(0, "tent", Cat.STAY, 2, 2, 120_000, 10, 6_000, 60, 1, 15f, 4f, a(1f, 0.2f, 0f), beauty = 0f),
        BType(1, "pension", Cat.STAY, 2, 2, 350_000, 20, 8_000, 40, 2, 40f, 12f, a(0.9f, 0.5f, 0f)),
        BType(2, "bungalow", Cat.STAY, 3, 2, 600_000, 30, 14_000, 48, 3, 70f, 20f, a(0.3f, 1f, 0.3f), beauty = 0.5f),
        BType(3, "hotel3", Cat.STAY, 3, 3, 2_400_000, 60, 45_000, 150, 3, 85f, 28f, a(0.4f, 1f, 0.2f)),
        BType(4, "hotel4", Cat.STAY, 3, 3, 5_500_000, 90, 90_000, 200, 4, 140f, 45f, a(0f, 0.8f, 0.7f)),
        BType(5, "resort", Cat.STAY, 4, 4, 15_000_000, 120, 220_000, 300, 5, 260f, 90f, a(0f, 0.3f, 1f), beauty = 1f),
        BType(6, "villa", Cat.STAY, 2, 2, 900_000, 45, 12_000, 8, 5, 450f, 120f, a(0f, 0.1f, 1f), beauty = 0.5f),
        // ---- yeme-icme
        BType(7, "kiosk", Cat.FOOD, 1, 1, 150_000, 7, 3_000, 300, 1, 6f, 2.5f, a(1f, 0.6f, 0.1f)),
        BType(8, "fastfood", Cat.FOOD, 2, 1, 300_000, 14, 10_000, 500, 2, 12f, 5f, a(1f, 0.8f, 0.2f)),
        BType(9, "restaurant", Cat.FOOD, 2, 2, 700_000, 25, 20_000, 300, 3, 28f, 12f, a(0.4f, 1f, 0.6f)),
        BType(10, "finedining", Cat.FOOD, 2, 2, 1_600_000, 40, 35_000, 150, 5, 80f, 32f, a(0f, 0.4f, 1f), beauty = 0.3f),
        // ---- gece hayati
        BType(11, "pub", Cat.NIGHT, 2, 1, 500_000, 20, 12_000, 250, 2, 15f, 5f, a(1f, 0.8f, 0.3f), noise = 0.4f),
        BType(12, "disco", Cat.NIGHT, 2, 2, 1_400_000, 35, 40_000, 500, 3, 25f, 7f, a(0.8f, 1f, 0.6f), noise = 1f),
        // ---- eglence
        BType(13, "arcade", Cat.FUN, 2, 1, 600_000, 20, 15_000, 400, 2, 10f, 3f, a(1f, 0.7f, 0.2f), noise = 0.2f),
        BType(14, "funfair", Cat.FUN, 4, 3, 4_000_000, 75, 120_000, 1500, 3, 20f, 5f, a(0.9f, 1f, 0.5f), noise = 0.6f),
        BType(15, "waterpark", Cat.FUN, 4, 4, 6_000_000, 90, 160_000, 1800, 4, 30f, 8f, a(0.8f, 1f, 0.7f), noise = 0.3f),
        // ---- kumar
        BType(16, "casino", Cat.GAMBLE, 3, 3, 8_000_000, 100, 250_000, 600, 5, 120f, 40f, a(0.3f, 0.7f, 1f), noise = 0.2f),
        // ---- alisveris
        BType(17, "souvenir", Cat.SHOP, 1, 1, 120_000, 7, 4_000, 250, 2, 12f, 7f, a(0.8f, 1f, 0.6f)),
        BType(18, "market", Cat.SHOP, 2, 1, 400_000, 15, 12_000, 600, 2, 18f, 13f, a(1f, 0.9f, 0.4f)),
        BType(19, "mall", Cat.SHOP, 4, 3, 7_000_000, 100, 200_000, 3000, 4, 45f, 28f, a(0.6f, 1f, 1f), noise = 0.2f),
        // ---- plaj ve cevre
        BType(20, "beachclub", Cat.BEACH, 2, 1, 150_000, 10, 5_000, 400, 3, 8f, 1f, a(0.9f, 1f, 1f), needsBeach = true),
        BType(21, "park", Cat.NATURE, 2, 2, 60_000, 10, 1_500, 0, 0, 0f, 0f, a(0f, 0f, 0f), beauty = 3f),
        BType(22, "palm", Cat.NATURE, 1, 1, 3_000, 2, 50, 0, 0, 0f, 0f, a(0f, 0f, 0f), beauty = 0.6f),
        BType(23, "garden", Cat.NATURE, 1, 1, 6_000, 3, 150, 0, 0, 0f, 0f, a(0f, 0f, 0f), beauty = 1f),
        BType(24, "road", Cat.ROAD, 1, 1, 2_000, 1, 20, 0, 0, 0f, 0f, a(0f, 0f, 0f)),
        // ---- herkese ait
        BType(25, "airport", Cat.PUBLIC, 4, 3, 0, 0, 0, 0, 0, 0f, 0f, a(0f, 0f, 0f), noise = 0.8f, buildable = false),
        BType(26, "harbor", Cat.PUBLIC, 3, 2, 0, 0, 0, 0, 0, 0f, 0f, a(0f, 0f, 0f), buildable = false),
        // ---- reklamla kurulan (kayit uyumu icin sona eklendi)
        BType(27, "motel", Cat.STAY, 2, 2, 450_000, 20, 7_000, 60, 2, 32f, 10f, a(1f, 0.7f, 0f), adOnly = true),
    )
    val AIRPORT = types[25]
    val HARBOR = types[26]
    operator fun get(id: Int) = types[id]
    val MOTEL = types[27]
}

class Building(
    val id: Int, val type: BType, var owner: Int, val x: Int, val y: Int,
) {
    var price: Float = type.price
    /** Kalan insaat gunu; 0 ise acik */
    var buildLeft: Int = type.days
    /** Sabotaj vb. nedeniyle kapali kalinacak gun */
    var closedDays: Int = 0
    /** Iyilestirme seviyesi (0..2): kaliteyi ve kapasiteyi artirir */
    var level: Int = 0
    /** Bugunku misafir (STAY, sinif basina) ya da ziyaret (diger) */
    val today = FloatArray(3)
    /** Bu ayin ve gecen ayin geliri, misafir/ziyaret toplami */
    var monthRevenue: Long = 0
    var lastMonthRevenue: Long = 0
    var monthUse: Float = 0f
    var lastMonthUse: Float = 0f
    /** Sinif basina konum puani (haftalik guncellenir) */
    val loc = FloatArray(3)

    val open get() = buildLeft <= 0 && closedDays <= 0
    val quality get() = type.quality + level * 0.5f
    val capacity get() = (type.cap * (1f + 0.25f * level)).toInt()
    /** Bina merkezi (mesafe hesabi icin) */
    val cx get() = x + type.w / 2f
    val cy get() = y + type.h / 2f
}

enum class Personality { BALANCED, LUXURY, BUDGET, FUN, AGGRESSIVE }

class Company(val id: Int, var name: String, var color: Int, val bot: Boolean, var personality: Personality) {
    var balance: Long = 0
    /** Itibar 0..100 */
    var reputation: Float = 50f
    val ledger = LongArray(Fin.entries.size)
    val lastLedger = LongArray(Fin.entries.size)
    var monthIn: Long = 0
    var monthOut: Long = 0
    var lastMonthIn: Long = 0
    var lastMonthOut: Long = 0
    /** Reklam: sinif basina etkinin bitecegi gun */
    val adUntil = IntArray(3) { -1 }
    // Kredi (esit taksit, aylik)
    var loanLeft: Long = 0
    var loanRate: Float = 0f
    var loanPayment: Long = 0
    var loanMonthsLeft: Int = 0
    /** Ust uste eksi bakiyeyle kapanan ay */
    var redMonths: Int = 0
    var bankrupt: Boolean = false
    /** Son kirli isin gunu */
    var lastDirtyDay: Int = -999
    /** Bugun icin memnuniyet birikimi (agirlikli) */
    var satSum: Double = 0.0
    var satW: Double = 0.0

    fun book(f: Fin, amount: Long) {
        ledger[f.ordinal] += amount
        balance += if (f.income) amount else -amount
        if (f.income) monthIn += amount else monthOut += amount
    }

    val alive get() = !bankrupt
}

enum class MapType { ISLAND, ARCHIPELAGO, COAST, RANDOM }
enum class MapSize(val n: Int, val tourists: IntArray) {
    SMALL(64, intArrayOf(900, 550, 120)),
    MEDIUM(96, intArrayOf(1800, 1100, 250)),
    LARGE(128, intArrayOf(3200, 2000, 450)),
}
enum class Difficulty(val startMoney: Long, val botMoney: Long) {
    EASY(3_000_000, 1_500_000), NORMAL(1_500_000, 1_500_000), HARD(750_000, 2_500_000)
}
enum class Goal { LAST_STANDING, RICHEST_20Y, ENDLESS }

class IsSettings(
    var companyName: String = "",
    var color: Int = 0xFF2E86DE.toInt(),
    var bots: Int = 3,
    var difficulty: Difficulty = Difficulty.NORMAL,
    var mapType: MapType = MapType.ISLAND,
    var mapSize: MapSize = MapSize.MEDIUM,
    var goal: Goal = Goal.LAST_STANDING,
    var seed: Long = 1L,
)

enum class NewsKind { BUILT, BANKRUPT, AUCTION, AUCTION_WON, AD, SABOTAGE, CAUGHT, PROPAGANDA, STRIKE, LOAN, SEASON, MILESTONE, PRICE_WAR, WARNING }

class IsNews(val day: Int, val kind: NewsKind, val company: Int, val other: Int = -1, val value: Long = 0, val type: Int = -1) {
    var read = false
}

class IsWorld(val settings: IsSettings, val n: Int) {
    val terrain = ByteArray(n * n)
    /** Arsa sahibi: -1 belediye, -2 kamu (havalimani/iskele) */
    val owner = IntArray(n * n) { -1 }
    /** Bina id'si ya da -1 */
    val bAt = IntArray(n * n) { -1 }
    /** Kumsala/denize uzaklik (karo) */
    val waterDist = ByteArray(n * n)
    val buildings = ArrayList<Building?>()
    val companies = ArrayList<Company>()
    /** Gunluk turist potansiyeli (sinif basina, sezon etkisinden once) */
    val demand = DoubleArray(3)
    /** Bugun adada bulunan turist (sinif basina) */
    val present = FloatArray(3)
    /** Bugun gelmek isteyip yer bulamayan / baska yeri secen */
    val lost = FloatArray(3)
    var day: Int = 0
    var startYear: Int = 2026
    val rnd = IsRandom(settings.seed)
    val news = ArrayList<IsNews>()
    val auctions = ArrayList<IsAuction>()
    val userId get() = 0
    /** Zemin degisince artar (cizim onbellegi icin, kayda yazilmaz) */
    @Volatile var terrainVersion: Int = 0
    /** Testler icin: botlari durdur (kayda yazilmaz) */
    var botsPaused: Boolean = false
    var gameOver: Boolean = false
    var winner: Int = -1

    fun idx(x: Int, y: Int) = y * n + x
    fun inside(x: Int, y: Int) = x in 0 until n && y in 0 until n
    fun ter(x: Int, y: Int) = Terrain.entries[terrain[idx(x, y)].toInt()]

    /** 0..11, 0 = Ocak */
    val month get() = (3 + day / 30) % 12
    val year get() = startYear + (3 + day / 30) / 12
    val dayOfMonth get() = day % 30 + 1

    /** Simdiye kadar eklenen haber sayisi (liste 300'de kirpilir, bu sayac kirpilmaz; kayda yazilmaz) */
    var newsCount: Int = 0

    fun addNews(n: IsNews) {
        news.add(n)
        newsCount++
        if (news.size > 300) news.subList(0, news.size - 300).clear()
    }
}
