package com.eomer91.minigamesworld.ui.games.tower

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TowerEngineTest {

    private fun engine(perm: Map<TowerStat, Int> = emptyMap(), perks: Map<EconomyPerk, Int> = emptyMap()) =
        TowerEngine(perm, perks, Random(5))

    /** [seconds] boyunca 1/60 adimlarla simule eder. */
    private fun simulate(e: TowerEngine, start: TowerRun, seconds: Float): TowerRun {
        var run = start
        var t = 0f
        while (t < seconds && !run.gameOver) {
            run = e.update(run, 1f / 60f).first
            t += 1f / 60f
        }
        return run
    }

    @Test
    fun firstWaves_areSurvivableWithoutUpgrades() {
        val e = engine()
        val run = simulate(e, e.newRun(), 60f)
        assertTrue("wave=${run.wave} hp=${run.health}", !run.gameOver && run.wave >= 3)
        assertTrue(run.kills > 0 && run.silver > 0 && run.score > 0)
    }

    @Test
    fun withoutDefense_towerEventuallyFalls() {
        // Hasari neredeyse sifir bir kule
        val e = engine()
        val weak = e.newRun().copy(runLevels = mapOf(TowerStat.RANGE to -14))
        val run = simulate(e, weak, 600f)
        assertTrue(run.gameOver)
    }

    @Test
    fun permanentUpgrades_stackWithRunUpgrades() {
        val e = engine(perm = mapOf(TowerStat.DAMAGE to 5))
        var run = e.newRun().copy(silver = 1000)
        val before = e.damage(run)
        run = e.buyRunUpgrade(run, TowerStat.DAMAGE)
        assertTrue(e.damage(run) > before)
        assertEquals(6, e.level(run, TowerStat.DAMAGE))
        assertEquals(1000 - TowerBalance.runCost(0), run.silver)
    }

    @Test
    fun healthUpgrade_addsCurrentHealth() {
        val e = engine()
        val run = e.newRun().copy(silver = 100)
        val up = e.buyRunUpgrade(run, TowerStat.HEALTH)
        assertTrue(up.health > run.health)
    }

    @Test
    fun cannotBuyWithoutSilver() {
        val e = engine()
        val run = e.newRun().copy(silver = 0)
        assertEquals(run, e.buyRunUpgrade(run, TowerStat.DAMAGE))
    }

    @Test
    fun startSilverPerk() {
        val e = engine(perks = mapOf(EconomyPerk.START_SILVER to 3))
        assertEquals(60, e.newRun().silver)
    }

    @Test
    fun bossEveryTenWaves() {
        val e = engine()
        assertTrue(EnemyType.BOSS in e.waveComposition(10))
        assertTrue(EnemyType.BOSS !in e.waveComposition(9))
    }
}
