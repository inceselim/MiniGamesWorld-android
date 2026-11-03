package com.eomer91.minigamesworld.ui.games.cooking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class CakeLogicTest {

    private val order = CakeOrder(Flavor.CHOCOLATE, Frosting.PINK, setOf(Topping.CHERRY, Topping.STAR))

    private fun perfectCake(): CakeWork {
        var w = CakeWork()
        Ingredient.BASE.forEach { w = Cake.add(w, it, order) }
        w = Cake.add(w, Ingredient.COCOA, order)
        repeat(6) { w = Cake.stir(w, (2 * PI).toFloat()) }
        w = w.copy(bake = Cake.bakeResult(0.7f), frosting = Frosting.PINK)
        repeat(Cake.FROST_CELLS) { c -> repeat(10) { w = Cake.spread(w, c, 10f) } }
        repeat(2) { w = Cake.place(w, Topping.CHERRY, 0f, 0f) }
        repeat(2) { w = Cake.place(w, Topping.STAR, 0.2f, 0f) }
        return w
    }

    @Test
    fun ingredientsNeedBaseAndRightFlavor() {
        var w = CakeWork()
        Ingredient.BASE.forEach { w = Cake.add(w, it, order) }
        assertFalse(Cake.ingredientsDone(w))
        w = Cake.add(w, Ingredient.VANILLA, order) // yanlis aroma
        assertEquals(1, w.mistakes)
        assertFalse(Cake.ingredientsDone(w))
        w = Cake.add(w, Ingredient.FLOUR, order) // tekrar
        assertEquals(2, w.mistakes)
        w = Cake.add(w, Ingredient.COCOA, order)
        assertTrue(Cake.ingredientsDone(w))
        assertEquals(Flavor.CHOCOLATE, w.flavor)
    }

    @Test
    fun stirringFillsAfterFiveTurns() {
        var w = CakeWork()
        repeat(4) { w = Cake.stir(w, (2 * PI).toFloat()) }
        assertTrue(w.mix < 1f)
        w = Cake.stir(w, -(2 * PI).toFloat()) // ters yon de sayilir
        assertEquals(1f, w.mix, 0.001f)
    }

    @Test
    fun bakeWindows() {
        assertEquals(BakeResult.RAW, Cake.bakeResult(0.2f))
        assertEquals(BakeResult.UNDER, Cake.bakeResult(0.5f))
        assertEquals(BakeResult.PERFECT, Cake.bakeResult(0.7f))
        assertEquals(BakeResult.OK, Cake.bakeResult(0.85f))
        assertEquals(BakeResult.BURNT, Cake.bakeResult(1f))
    }

    @Test
    fun frostingNeedsColorFirst() {
        var w = CakeWork()
        w = Cake.spread(w, 0, 50f)
        assertEquals(0f, w.frostCoverage, 0.0001f)
        w = w.copy(frosting = Frosting.MINT)
        w = Cake.spread(w, 0, 50f)
        assertTrue(w.frostCoverage > 0f)
    }

    @Test
    fun perfectCakeGetsThreeStarsAndMistakesCost() {
        val good = perfectCake()
        assertEquals(3, Cake.stars(Cake.rate(order, good)))
        val burnt = good.copy(bake = BakeResult.BURNT, frosting = Frosting.WHITE)
        assertTrue(Cake.stars(Cake.rate(order, burnt)) < 3)
        assertTrue(Cake.rate(order, CakeWork()) < 0.2f)
    }
}
