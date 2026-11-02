package com.eomer91.minigamesworld.ui.games.countries

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CountriesEngineTest {
    private val engine = CountriesEngine(Random(3))

    private fun game() = engine.newGame(CwDifficulty.NORMAL, "Test", Polity.DEMOCRACY)

    @Test
    fun newGameHasBotsAndPeace() {
        val g = game()
        assertEquals(3, g.bots.size)
        assertTrue(g.peace)
        assertEquals(CwResult.PeaceTime, engine.attack(g, 1, mapOf(UnitType.KNIGHT.name to 10)))
    }

    @Test
    fun buildingCostsResources() {
        val g = game()
        val r = engine.build(g, Building.FARM) as CwResult.Ok
        assertEquals(g.player.level(Building.FARM) + 1, r.game.player.level(Building.FARM))
        assertTrue(r.game.player.gold < g.player.gold)
        assertEquals(CwResult.NotEnoughResources, engine.build(g, Building.ESPIONAGE))
    }

    @Test
    fun recruitRespectsLimits() {
        val g = game()
        val max = engine.maxRecruit(g.player, UnitType.SPEARMAN)
        assertTrue(max > 0)
        assertTrue(engine.recruit(g, UnitType.SPEARMAN, max) is CwResult.Ok)
        assertFalse(engine.recruit(g, UnitType.SPEARMAN, max + 1000) is CwResult.Ok)
    }

    @Test
    fun ticksRunAndStateSerializes() {
        var g = game()
        repeat(40) { g = engine.tick(g) }
        assertTrue(g.turn > 1)
        g.bots.forEach { b -> UnitType.entries.forEach { assertTrue(b.count(it) >= 0) } }
        assertEquals(g, CwGame.fromJson(g.toJson()))
    }

    @Test
    fun overwhelmingAttackWins() {
        var g = game()
        repeat(CountriesEngine.PEACE_TURNS + 1) { g = engine.tick(g) }
        if (g.over) return
        val target = g.bots.first { it.alive }
        val strong = g.copy(player = g.player.withArmy(UnitType.CATAPULT, 10_000))
        val r = engine.attack(strong, target.id, mapOf(UnitType.CATAPULT.name to 10_000)) as CwResult.Ok
        assertTrue(r.game.lastBattle!!.won)
        assertFalse(r.game.bots.first { it.id == target.id }.alive)
    }
}
