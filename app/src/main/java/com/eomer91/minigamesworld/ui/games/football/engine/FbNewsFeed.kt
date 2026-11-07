package com.eomer91.minigamesworld.ui.games.football.engine

/**
 * FM tarzi haber akisi: mac onizleme/rapor, sakatlik ve ceza, kura, lider degisimi, ayin oyuncusu,
 * transfer donemi, tesis, finans uyarisi, taraftar tepkisi, kupa, milli davet, emeklilik, sezon odulleri.
 * Yalnizca kullanicinin kulubu, ligi ve buyuk organizasyonlarla ilgili haberler uretilir.
 */
object FbNewsFeed {

    private fun add(world: FbWorld, kind: NewsKind, player: Int = -1, club: Int = -1, club2: Int = -1, value: Long = 0, text: String = "") {
        val n = FbNews(kind, world.season, world.day, player, club, club2, value)
        n.text = text
        world.market.addNews(n)
    }

    private fun userLeagueComp(world: FbWorld): FbComp? {
        val u = world.userClub
        if (u < 0) return null
        val lg = world.clubs[u].league
        return world.state?.comps?.firstOrNull { it.kind == CompKind.LEAGUE && it.league == lg }
    }

    // ================================================================== gunluk

    fun daily(world: FbWorld) {
        val u = world.userClub
        val st = world.state ?: return
        if (u < 0) return
        val cal = FbSeason.calendar(world)
        val d = world.day
        // Yarinki mac onizlemesi
        for (id in st.byDay[d + 1] ?: emptyList<Int>()) {
            val f = st.fixtures[id]
            if (st.comps[f.comp].national || !f.involves(u)) continue
            add(world, NewsKind.MATCH_PREVIEW, club = if (f.home == u) f.away else f.home, club2 = f.comp, value = (d + 1).toLong())
        }
        if (d == 0 || d == cal.winterWindowStart) add(world, NewsKind.WINDOW_OPEN)
        if (d == cal.summerWindowEnd || d == cal.winterWindowEnd) add(world, NewsKind.WINDOW_CLOSE)
        // Ayin oyuncusu (Eylul - Mayis, ayin 1'i): ligde formu en yuksek, duzenli oynayan
        val (_, m, dom) = cal.date(d)
        if (dom == 1 && (m >= 9 || m <= 5)) {
            val comp = userLeagueComp(world)
            if (comp != null) {
                val clubs = comp.stages[0].groups.firstOrNull()?.toSet() ?: emptySet()
                val best = world.players.filter { !it.retired && it.club in clubs && it.playTime >= 180 }.maxByOrNull { it.form * 10 + it.currentAbility() / 20 }
                if (best != null) add(world, NewsKind.PLAYER_OF_MONTH, player = best.id, club = best.club, value = best.form.toLong())
            }
        }
        // Milli takim davetleri: ara haftasindan iki gun once
        if (cal.intlSaturdays.any { it - 2 == d }) callups(world)
    }

    private fun callups(world: FbWorld) {
        val u = world.userClub
        val mine = world.clubs[u].players.map { world.players[it] }
        val nations = mine.map { it.nation }.toSet()
        if (nations.isEmpty()) return
        val pools = FbSeason.nationPools(world)
        var n = 0
        for (nat in nations) {
            val sheet = FbSeason.nationalSheet(world, nat, pools)
            for (p in sheet.xi + sheet.bench) if (p.club == u && n < 6) { add(world, NewsKind.CALLUP, player = p.id, club2 = nat); n++ }
        }
    }

    // ================================================================== haftalik

    fun weekly(world: FbWorld) {
        val u = world.userClub
        if (u < 0) return
        val st = world.state ?: return
        val comp = userLeagueComp(world)
        if (comp != null && st.compFixtures(comp.id).any { it.played }) {
            val leader = st.table(comp.id, 0, 0).firstOrNull()
            val last = world.market.news.lastOrNull { it.kind == NewsKind.LEAGUE_LEADER && it.season == world.season && it.club2 == comp.league }
            if (leader != null && leader.p >= 3 && (last == null || last.club != leader.team))
                add(world, NewsKind.LEAGUE_LEADER, club = leader.team, club2 = comp.league, value = leader.pts.toLong())
        }
        val c = world.clubs[u]
        if (c.balance < 0) {
            val last = world.market.news.lastOrNull { it.kind == NewsKind.FINANCE_WARNING }
            if (last == null || last.season != world.season || world.day - last.day >= 28) add(world, NewsKind.FINANCE_WARNING, club = u, value = c.balance)
        }
    }

    // ================================================================== mac

    /** Kullanicinin maci islendikten sonra. [suspendedBefore] mactan once cezali olmayan oyuncular. */
    fun afterMatch(world: FbWorld, f: FbFixture, out: MatchOutcome, comp: FbComp, notSuspendedBefore: Set<Int>) {
        val u = world.userClub
        if (comp.national || u < 0 || !f.involves(u)) return
        val home = f.home == u
        val lines = if (home) out.home else out.away
        val gf = if (home) out.hg else out.ag
        val ga = if (home) out.ag else out.hg
        val motm = lines.maxByOrNull { it.rating }
        add(world, NewsKind.MATCH_REPORT, player = motm?.p?.id ?: -1, club = if (home) f.away else f.home, club2 = f.comp, value = gf * 100L + ga)
        for (l in lines) {
            if (l.injuryDays > 0) add(world, NewsKind.INJURY, player = l.p.id, club = u, value = l.p.injuryDays.toLong())
            if (l.p.suspendedMatches > 0 && l.p.id in notSuspendedBefore) add(world, NewsKind.SUSPENSION, player = l.p.id, club = u, value = l.p.suspendedMatches.toLong())
        }
        if (!comp.kind.official) return
        // Taraftar tepkisi: seriler
        val recent = world.state!!.recentResults(u, world.day, 5)
        if (recent.size == 5 && recent.all { it == 3 }) {
            val last = world.market.news.lastOrNull { it.kind == NewsKind.FANS_HAPPY }
            if (last == null || last.season != world.season || world.day - last.day > 30) add(world, NewsKind.FANS_HAPPY, club = u, value = 5)
        }
        val last3 = recent.takeLast(3)
        if (last3.size == 3 && last3.all { it == 0 }) {
            val last = world.market.news.lastOrNull { it.kind == NewsKind.FANS_ANGRY }
            if (last == null || last.season != world.season || world.day - last.day > 30) add(world, NewsKind.FANS_ANGRY, club = u, value = 3)
        }
    }

    /** Antrenman sakatligi (kullanici oyuncusu). */
    fun trainingInjury(world: FbWorld, p: FbPlayer) {
        if (p.club == world.userClub && world.userClub >= 0) add(world, NewsKind.INJURY, player = p.id, club = p.club, value = p.injuryDays.toLong(), text = "T")
    }

    // ================================================================== organizasyon

    private val BIG = setOf(CompKind.CHAMPIONS, CompKind.EUROPA, CompKind.CONFERENCE, CompKind.WORLD_CUP, CompKind.EURO)

    fun trophy(world: FbWorld, comp: FbComp, winner: Int) {
        val u = world.userClub
        if (u < 0 || winner < 0) return
        val mine = !comp.national && winner == u
        val userCountry = world.clubs[u].country
        val domestic = !comp.national && comp.country == userCountry && (comp.kind == CompKind.CUP ||
            (comp.kind == CompKind.LEAGUE && world.leagues[comp.league].tier == 1))
        if (mine || comp.kind in BIG || domestic) add(world, NewsKind.TROPHY, club = winner, club2 = comp.kind.ordinal, value = if (comp.league >= 0) comp.league.toLong() else -1)
    }

    fun cupDraw(world: FbWorld, comp: FbComp, stage: FbStage) {
        val u = world.userClub
        if (u < 0 || comp.national) return
        val tie = stage.ties.firstOrNull { it.a == u || it.b == u } ?: return
        add(world, NewsKind.CUP_DRAW, club = if (tie.a == u) tie.b else tie.a, club2 = comp.id, value = stage.label.ordinal.toLong())
    }

    fun europe(world: FbWorld) {
        val u = world.userClub
        if (u < 0) return
        for (c in world.state!!.comps) {
            if (c.kind !in setOf(CompKind.CHAMPIONS, CompKind.EUROPA, CompKind.CONFERENCE)) continue
            val inLp = c.stages.any { s -> u in s.extra }
            val inQ = c.stages.any { s -> s.ties.any { it.a == u || it.b == u } || u in s.byes }
            if (inLp || inQ) add(world, NewsKind.EURO_QUALIFIED, club = u, club2 = c.kind.ordinal, value = if (inLp) 1 else 0)
        }
    }

    // ================================================================== kulup

    fun facilityDone(world: FbWorld, c: FbClub, f: Facility) {
        if (c.id == world.userClub) add(world, NewsKind.FACILITY_DONE, club = c.id, value = f.ordinal.toLong())
    }

    fun youthIntake(world: FbWorld, c: FbClub, newcomers: List<FbPlayer>) {
        if (c.id != world.userClub || newcomers.isEmpty()) return
        val best = newcomers.maxBy { it.potential }
        add(world, NewsKind.YOUTH_INTAKE, player = best.id, club = c.id, value = newcomers.size.toLong())
    }

    fun retired(world: FbWorld, p: FbPlayer, formerClub: Int) {
        if (p.reputation >= 6500 || (formerClub == world.userClub && formerClub >= 0))
            add(world, NewsKind.RETIREMENT, player = -1, club = formerClub, value = world.age(p).toLong(), text = p.fullName)
    }

    fun graduate(world: FbWorld, p: FbPlayer) {
        if (p.club == world.userClub) add(world, NewsKind.YOUTH_GRADUATE, player = p.id, club = p.club)
    }

    /** Sezon sonu: kullanicinin liginde gol krali ve sezonun oyuncusu. */
    fun seasonAwards(world: FbWorld) {
        val st = world.state ?: return
        val comp = userLeagueComp(world) ?: return
        val entries = st.stats.entries.filter { (it.key shr 32).toInt() == comp.id }
        entries.maxByOrNull { it.value.goals * 100 + it.value.assists }?.let {
            add(world, NewsKind.AWARD_TOP_SCORER, player = (it.key and 0xffffffffL).toInt(), club2 = comp.league, value = it.value.goals.toLong(),
                text = world.players[(it.key and 0xffffffffL).toInt()].fullName)
        }
        entries.filter { it.value.apps >= 15 }.maxByOrNull { it.value.avgRating }?.let {
            add(world, NewsKind.AWARD_PLAYER_SEASON, player = (it.key and 0xffffffffL).toInt(), club2 = comp.league, value = (it.value.avgRating * 100).toLong(),
                text = world.players[(it.key and 0xffffffffL).toInt()].fullName)
        }
    }
}
