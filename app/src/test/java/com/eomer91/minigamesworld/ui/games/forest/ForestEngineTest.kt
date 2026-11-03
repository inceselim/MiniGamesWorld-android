package com.eomer91.minigamesworld.ui.games.forest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ForestEngineTest {

    /** Daire cizerek kosan, her seviyede ilk secenegi alan basit bot. */
    private fun simulate(seconds: Float, stats: HeroStats = HeroStats(), seed: Int = 1): ForestEngine {
        val e = ForestEngine(ForestMap.EDGE, stats, Random(seed))
        val dt = 1f / 30f
        var t = 0f
        while (t < seconds && !e.dead && !e.victory) {
            if (e.pendingLevelUps > 0) e.apply(e.rollChoices().first())
            e.update(dt, cos(t * 0.4f), sin(t * 0.4f))
            t += dt
        }
        return e
    }

    @Test
    fun earlyGameProgresses() {
        val e = simulate(60f)
        assertTrue("kills=${e.kills}", e.kills > 10)
        assertTrue("level=${e.level}", e.level > 2)
        assertFalse(e.dead)
    }

    @Test
    fun bossSpawnsAndHeroCanWinWithStrongStats() {
        val strong = HeroStats(maxHp = 5000f, attack = 400f, defence = 300f)
        val e = simulate(ForestEngine.BOSS_TIME + 120f, strong)
        assertTrue(e.bossSpawned)
        assertTrue("victory=${e.victory} kills=${e.kills}", e.victory)
    }

    @Test
    fun choicesNeverExceedSlots() {
        val e = ForestEngine(ForestMap.EDGE, HeroStats(), Random(3))
        repeat(80) { e.pendingLevelUps = 1; e.apply(e.rollChoices().first()) }
        assertTrue(e.weapons.size <= ForestEngine.MAX_WEAPONS)
        assertTrue(e.weapons.values.all { it <= ForestEngine.MAX_LEVEL })
        assertEquals(listOf(Upgrade.Heal), e.rollChoices())
    }

    @Test
    fun evolutionOfferedWhenReady() {
        val e = ForestEngine(ForestMap.EDGE, HeroStats(), Random(4))
        e.weapons[WeaponType.BOLT] = ForestEngine.MAX_LEVEL
        assertTrue(e.rollChoices().none { it is Upgrade.Evolve })
        e.passives[PassiveType.MIGHT] = 1
        val choices = e.rollChoices()
        assertEquals(Upgrade.Evolve(WeaponType.BOLT), choices.first())
        e.pendingLevelUps = 1
        e.apply(choices.first())
        assertTrue(WeaponType.BOLT in e.evolved)
        assertTrue(e.rollChoices().none { it is Upgrade.Evolve })
    }

    @Test
    fun questsTrackProgress() {
        val e = ForestEngine(ForestMap.EDGE, HeroStats(), Random(5))
        val quests = ForestQuests.forMap(ForestMap.EDGE)
        assertEquals(3, quests.size)
        val kill = quests.first { it.type == QuestType.KILLS }
        e.kills = kill.target
        assertTrue(ForestQuests.isDone(kill, e))
        val boss = quests.first { it.type == QuestType.BOSS_FAST }
        assertFalse(ForestQuests.isDone(boss, e))
        e.bossSpawnTime = 270f
        e.bossKillTime = 270f + boss.target - 5
        assertTrue(ForestQuests.isDone(boss, e))
    }

    @Test
    fun reviveOnlyOnce() {
        val e = ForestEngine(ForestMap.EDGE, HeroStats(), Random(2))
        e.dead = true
        e.revive()
        assertFalse(e.dead)
        e.dead = true
        e.revive()
        assertTrue(e.dead)
    }

    @Test
    fun profileJsonRoundTrip() {
        val p = ForestProfile(unlocked = 2, cleared = listOf(0, 1), bestKills = mapOf(0 to 512))
        assertEquals(p, ForestProfile.fromJson(p.toJson()))
    }
}
