package com.eomer91.minigamesworld.ui.games.football.engine

/** Kadro secimi ve yapay zeka taktigi. */
object FbLineup {
    /** Oyuncunun bir mevkideki mac gucu (secim icin). */
    fun slotScore(p: FbPlayer, pos: Pos): Float {
        val fam = p.fam(pos)
        val base = p.posRating(pos) * (0.55f + 0.45f * fam / 20f)
        return base * (0.7f + 0.3f * p.condition / 100f) * (0.95f + 0.1f * p.morale / 100f)
    }

    fun available(p: FbPlayer) = p.injuryDays <= 0 && p.suspendedMatches <= 0

    /** En iyi 11 (dizilis sirasinda) ve 9 kisilik yedek. */
    fun pick(players: List<FbPlayer>, formation: Formation): Pair<List<FbPlayer>, List<FbPlayer>> {
        val pool = players.filter { available(it) }.toMutableList()
        val chosen = arrayOfNulls<FbPlayer>(11)
        // Once kaleci, sonra savunma ortasi, sonra digerleri (az secenekli mevkiler once)
        val order = formation.slots.indices.sortedBy { i -> when (formation.slots[i].pos) { Pos.GK -> 0; Pos.DC -> 1; Pos.ST -> 2; Pos.DM -> 3; else -> 4 } }
        for (i in order) {
            val pos = formation.slots[i].pos
            val best = pool.maxByOrNull { slotScore(it, pos) } ?: continue
            chosen[i] = best
            pool.remove(best)
        }
        val xi = chosen.filterNotNull()
        val bench = ArrayList<FbPlayer>()
        pool.filter { it.bestPos() == Pos.GK }.maxByOrNull { it.currentAbility() }?.let { bench.add(it); pool.remove(it) }
        bench.addAll(pool.sortedByDescending { it.currentAbility() * (0.7f + 0.3f * it.condition / 100f) }.take(9 - bench.size))
        return xi to bench
    }

    /** Kadroya en uygun dizilis: toplam slot gucu en yuksek olan. */
    fun bestFormation(players: List<FbPlayer>): Formation =
        Formation.entries.maxBy { f -> pick(players, f).first.withIndex().sumOf { (i, p) -> slotScore(p, f.slots[i].pos).toDouble() } }

    /** Ortalama ilk 11 gucu (karsilastirma icin). */
    fun teamStrength(players: List<FbPlayer>, formation: Formation): Float {
        val xi = pick(players, formation).first
        return xi.withIndex().sumOf { (i, p) -> slotScore(p, formation.slots[i].pos).toDouble() }.toFloat() / 11f
    }

    /** Yapay zeka taktigi: rakibe gore oyun anlayisi. */
    fun aiTactics(own: Float, opp: Float, home: Boolean, formation: Formation, seed: Int): Tactics {
        val diff = own - opp + if (home) 0.4f else -0.2f
        val mentality = when {
            diff > 1.5f -> Mentality.ATTACKING
            diff > 0.3f -> Mentality.BALANCED
            diff > -1.2f -> Mentality.BALANCED
            diff > -2.5f -> Mentality.DEFENSIVE
            else -> Mentality.VERY_DEFENSIVE
        }
        val r = (seed and 0x7fffffff) % 100
        return Tactics(
            formation = formation,
            mentality = mentality,
            passing = if (r < 30) PassingStyle.SHORT else if (r < 75) PassingStyle.MIXED else PassingStyle.DIRECT,
            tempo = if (diff > 1f) Tempo.FAST else Tempo.NORMAL,
            pressing = if (diff > 1f && r % 3 == 0) Pressing.HIGH else Pressing.MEDIUM,
            defLine = if (mentality == Mentality.VERY_DEFENSIVE) DefLine.DEEP else DefLine.NORMAL,
            counter = mentality.risk < 0f,
            width = if (r % 4 == 0) Width.WIDE else Width.NORMAL,
        )
    }

    /** Kulupten mac takimi kur. */
    fun side(world: FbWorld, club: Int, tactics: Tactics, home: Boolean): MatchSide {
        val c = world.clubs[club]
        val players = world.clubPlayers(club)
        val (xi, bench) = pick(players, tactics.formation)
        val s = MatchSide(club, c.name, tactics, home)
        xi.forEachIndexed { i, p -> s.onPitch[i] = MatchPlayer(p, tactics.formation.slots[i].pos) }
        bench.forEach { s.bench.add(MatchPlayer(it, it.bestPos())) }
        return s
    }

    /** Yapay zeka vs yapay zeka mac kurulumu. */
    fun aiMatch(world: FbWorld, homeClub: Int, awayClub: Int, seed: Long, needWinner: Boolean = false, aggregateOffset: Int = 0): MatchSim {
        val hp = world.clubPlayers(homeClub); val ap = world.clubPlayers(awayClub)
        val hf = bestFormation(hp); val af = bestFormation(ap)
        val hs = teamStrength(hp, hf); val as_ = teamStrength(ap, af)
        val ht = aiTactics(hs, as_, true, hf, (seed xor homeClub.toLong()).toInt())
        val at = aiTactics(as_, hs, false, af, (seed xor awayClub.toLong() * 31).toInt())
        return MatchSim(side(world, homeClub, ht, true), side(world, awayClub, at, false), seed, needWinner, aggregateOffset)
    }
}
