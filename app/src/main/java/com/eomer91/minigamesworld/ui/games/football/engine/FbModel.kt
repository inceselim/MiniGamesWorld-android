package com.eomer91.minigamesworld.ui.games.football.engine

/** Kulupsuz (serbest) oyuncu. */
const val CLUB_NONE = -1
/** Avrupa disi ulkenin kendi liginde oynayan (yonetilemeyen) oyuncu. */
const val CLUB_ABROAD = -2
/** Futbolu birakmis oyuncu. */
const val CLUB_RETIRED = -3

enum class Foot { RIGHT, LEFT, BOTH }

class FbPlayer(
    /** Dunya listesindeki sira (sezon sonu sikistirmada degisebilir) */
    var id: Int,
    var firstName: String,
    var lastName: String,
    /** FbCountries.all icindeki sira */
    val nation: Int,
    val birthYear: Int,
    /** Yilin gunu (0..364) */
    val birthDay: Int,
    /** Attr.entries sirasinda, x10 (10..200), isaretsiz byte */
    val attrs: ByteArray,
    /** Hidden.entries sirasinda, 1..20 */
    val hidden: ByteArray,
    /** Pos.entries sirasinda mevki yatkinligi 1..20 */
    val posFam: ByteArray,
    var potential: Int,
    var foot: Foot,
    var heightCm: Int,
) {
    var club: Int = CLUB_NONE
    /** Haftalik maas (Euro) */
    var wage: Int = 0
    /** Sozlesmenin bittigi sezonun baslangic yili (o sezon sonunda biter) */
    var contractEnd: Int = 0
    var releaseClause: Int = 0
    var squadNumber: Int = 0
    /** 0..100 */
    var morale: Int = 70
    /** 0..100 mac kondisyonu */
    var condition: Int = 100
    /** Son maclarin ortalama puani x10 (60 = 6.0) */
    var form: Int = 65
    var injuryDays: Int = 0
    var suspendedMatches: Int = 0
    /** 0..10000 un */
    var reputation: Int = 0
    /** Transfer listesinde mi, istenen bonservis (0 = listede degil) */
    var askingPrice: Long = 0
    /** Kiralik ise asil kulup (yoksa -1) */
    var loanFrom: Int = -1
    var caps: Int = 0
    var internationalGoals: Int = 0
    /** Son haftalardaki oynama suresi (dakika, haftalik %25 sonumlenir) */
    var playTime: Int = 0
    /** Bireysel antrenman alani (TrainArea sirasi, -1 yok) */
    var trainArea: Int = -1
    /** Mevki antrenmani (Pos sirasi, -1 yok) */
    var trainPos: Int = -1
    /** Birikmis gelisim (CA birimi); 0.6'yi gecince yeteneklere islenir */
    var devAcc: Float = 0f
    /** Kiralik: bitis sezonu ve kiralayan kulubun odedigi maas yuzdesi */
    var loanUntil: Int = 0
    var loanWagePct: Int = 100
    /** Kiralik listesinde mi */
    var loanListed: Boolean = false
    /** Sezon sonu serbest gecis icin anlastigi kulup (-1 yok), anlasilan maas */
    var preContract: Int = -1
    var preContractWage: Int = 0
    /** Sonraki satistan pay alacak kulup ve yuzdesi */
    var sellOnClub: Int = -1
    var sellOnPct: Int = 0
    /** Ayrilmak istiyor (transfer talebi) */
    var wantsAway: Boolean = false
    val retired: Boolean get() = club == CLUB_RETIRED

    fun attr(a: Attr): Float = (attrs[a.ordinal].toInt() and 0xFF) / 10f
    fun setAttr(a: Attr, v: Float) { attrs[a.ordinal] = (v * 10f).toInt().coerceIn(10, 200).toByte(); invalidate() }

    // Hesap onbellegi: yetenek veya mevki yatkinligi degisince invalidate() cagrilmali
    private var cBest: Pos? = null
    private var cCa = -1
    private var cRating: FloatArray? = null
    fun invalidate() { cBest = null; cCa = -1; cRating = null }

    /** Mevkideki yetenek puani (1..20 olcegi, onbellekli) */
    fun posRating(p: Pos): Float {
        val r = cRating ?: FloatArray(Pos.COUNT) { PosWeights.rating(attrs, Pos.entries[it]) }.also { cRating = it }
        return r[p.ordinal]
    }
    fun hidden(h: Hidden): Int = hidden[h.ordinal].toInt()
    fun fam(p: Pos): Int = posFam[p.ordinal].toInt()

    /** En yatkin mevki */
    fun bestPos(): Pos = cBest ?: Pos.entries.maxBy { posFam[it.ordinal].toInt() * 100 + (posRating(it) * 2).toInt() }.also { cBest = it }
    fun currentAbility(): Int { if (cCa < 0) cCa = PosWeights.currentAbility(attrs, bestPos()); return cCa }
    fun age(currentYear: Int, dayOfYear: Int): Int = currentYear - birthYear - if (dayOfYear < birthDay) 1 else 0
    val fullName: String get() = "$firstName $lastName"
}

enum class StaffRole { ASSISTANT, COACH, GK_COACH, FITNESS_COACH, SCOUT, PHYSIO, YOUTH_COACH }

enum class StaffAttr {
    COACH_ATTACK, COACH_DEFENCE, COACH_TECHNICAL, COACH_TACTICAL, COACH_MENTAL, COACH_FITNESS, COACH_GK,
    WORKING_WITH_YOUTH, JUDGING_ABILITY, JUDGING_POTENTIAL, PHYSIOTHERAPY, MOTIVATING, DISCIPLINE, MAN_MANAGEMENT,
}

class FbStaff(
    val id: Int,
    var firstName: String,
    var lastName: String,
    val nation: Int,
    val birthYear: Int,
    var role: StaffRole,
    /** StaffAttr sirasinda 1..20 */
    val attrs: ByteArray,
) {
    var club: Int = CLUB_NONE
    var wage: Int = 0
    var contractEnd: Int = 0
    var reputation: Int = 0
    fun attr(a: StaffAttr) = attrs[a.ordinal].toInt()
    val fullName: String get() = "$firstName $lastName"
}

class FbClub(
    val id: Int,
    var name: String,
    var shortName: String,
    val country: Int,
    val city: String,
) {
    var league: Int = -1
    /** 0..10000 */
    var reputation: Int = 0
    var color1: Int = 0
    var color2: Int = 0
    var stadiumName: String = ""
    var capacity: Int = 0
    /** 1..20 */
    var trainingFacilities: Int = 10
    var youthFacilities: Int = 10
    var youthRecruitment: Int = 10
    var stadiumLevel: Int = 10
    var socialFacilities: Int = 10
    /** Euro */
    var balance: Long = 0
    var transferBudget: Long = 0
    /** Haftalik toplam maas butcesi */
    var wageBudget: Long = 0
    /** Yillik sponsor geliri */
    var sponsorIncome: Long = 0
    var fanBase: Int = 0
    val players: MutableList<Int> = ArrayList()
    val staff: MutableList<Int> = ArrayList()
    /** Kullanicinin (veya yapay zekanin) menajer adi */
    var managerName: String = ""

    // ---- Yonetim (adim 4)
    /** Bilet fiyat seviyesi -2 (cok ucuz) .. +2 (cok pahali) */
    var ticketLevel: Int = 0
    /** Forma sponsoru adi ve sozlesmenin bittigi sezon */
    var sponsorName: String = ""
    var sponsorEnd: Int = 0
    /** Takim antrenman odagi (TrainArea, -1 dengeli) ve yogunluk (0 hafif, 1 normal, 2 yogun) */
    var trainArea: Int = -1
    var intensity: Int = 1
    /** Tesis yatirimi: Facility sirasi (-1 yok), bitis gunu (sezon gunu, sezon yili ile), maliyet */
    var upgradeType: Int = -1
    var upgradeSeason: Int = 0
    var upgradeDay: Int = 0
    /** Sezon defteri (Fin sirasinda) ve bir onceki sezon */
    val ledger = LongArray(Fin.entries.size)
    val lastLedger = LongArray(Fin.entries.size)
    /** Haftalik bakiye gecmisi (bin Euro), en eski -> en yeni, en fazla 60 */
    val balanceHistory = ArrayList<Int>()
    /** Kombine/kapali gise sayaci (stadyum genisletme karari) */
    var soldOut: Int = 0
    /** Son lig sirasi (sezon sonu), 0 bilinmiyor */
    var lastPosition: Int = 0

    fun book(f: Fin, amount: Long) {
        ledger[f.ordinal] += amount
        balance += if (f.income) amount else -amount
    }
}

class FbLeague(
    val id: Int,
    val country: Int,
    val tier: Int,
) {
    /** Kullanici duzenledi ise ozel ad; bossa arayuz "Ulke N. Lig" yazar */
    var customName: String = ""
    val clubs: MutableList<Int> = ArrayList()
    var promoted: Int = 0
    var relegated: Int = 0
    var playoffSpots: Int = 0
}

/** Tum oyun dunyasi. */
class FbWorld(
    val seed: Long,
) {
    /** Mevcut sezonun baslangic yili (2026 => 2026/27) */
    var season: Int = START_SEASON
    /** Sezonun gun sayaci (1 Temmuz = 0) */
    var day: Int = 0
    var userClub: Int = -1
    var managerName: String = ""

    val players: ArrayList<FbPlayer> = ArrayList()
    val clubs: ArrayList<FbClub> = ArrayList()
    val leagues: ArrayList<FbLeague> = ArrayList()
    val staff: ArrayList<FbStaff> = ArrayList()

    /** Sezon/takvim durumu (FbSeason.start ile kurulur) */
    var state: FbSeasonState? = null
    /** Kullanicinin kayitli taktigi (yoksa yapay zeka secer) */
    var userTactics: Tactics? = null
    /** Transfer pazari, takip listesi, haber kutusu */
    val market: FbMarket = FbMarket()
    /** Takim -> dizilis onbellegi (kaydedilmez) */
    val formationCache: HashMap<Int, Formation> = HashMap()

    /** Ulke indeksi -> lig kademeleri (sirali lig id) */
    fun leaguesOf(country: Int): List<FbLeague> = leagues.filter { it.country == country }.sortedBy { it.tier }

    fun clubPlayers(club: Int): List<FbPlayer> = clubs[club].players.map { players[it] }

    /** Yilin gunu (dogum gunu/yas hesabi): sezon 1 Temmuz'da basladigindan +181. */
    fun dayOfYear(): Int = (day + 181) % 365
    fun calendarYear(): Int = season + if (day + 181 >= 365) 1 else 0
    fun age(p: FbPlayer) = p.age(calendarYear(), dayOfYear())

    /**
     * Emekli oyunculari listeden cikarir ve kimlikleri yeniden numaralar (sezon sonunda, istatistikler
     * sifirlanmisken). Donen dizi: eski id -> yeni id (-1 silindi).
     */
    fun compactPlayers(): IntArray {
        val map = IntArray(players.size) { -1 }
        val kept = ArrayList<FbPlayer>(players.size)
        for (p in players) if (!p.retired) { map[p.id] = kept.size; p.id = kept.size; kept.add(p) }
        players.clear(); players.addAll(kept)
        for (c in clubs) { val l = c.players.mapNotNull { map[it].takeIf { n -> n >= 0 } }; c.players.clear(); c.players.addAll(l) }
        formationCache.clear()
        return map
    }

    companion object {
        const val START_SEASON = 2026
    }
}
