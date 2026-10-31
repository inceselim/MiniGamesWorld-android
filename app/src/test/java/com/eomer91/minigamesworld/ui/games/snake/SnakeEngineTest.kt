package com.eomer91.minigamesworld.ui.games.snake

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnakeEngineTest {

    private val engine = SnakeEngine()

    private fun game(snake: List<Cell>, food: Cell, dir: SnakeDirection = SnakeDirection.RIGHT) =
        SnakeGame(snake = snake, food = food, direction = dir)

    @Test
    fun newGame_foodNotOnSnake() {
        repeat(50) {
            val g = engine.newGame()
            assertFalse(g.food in g.snake)
        }
    }

    @Test
    fun step_movesForwardKeepingLength() {
        val g = game(listOf(Cell(5, 5), Cell(4, 5), Cell(3, 5)), food = Cell(0, 0))
        val next = engine.step(g)
        assertEquals(listOf(Cell(6, 5), Cell(5, 5), Cell(4, 5)), next.snake)
        assertFalse(next.gameOver)
    }

    @Test
    fun step_eatingGrowsAndScores() {
        val g = game(listOf(Cell(5, 5), Cell(4, 5)), food = Cell(6, 5))
        val next = engine.step(g)
        assertEquals(3, next.snake.size)
        assertEquals(SnakeEngine.POINTS_PER_FOOD, next.score)
        assertEquals(1, next.foodsEaten)
        assertFalse(next.food in next.snake)
    }

    @Test
    fun step_hittingWallEndsGame() {
        val g = game(listOf(Cell(15, 3), Cell(14, 3)), food = Cell(0, 0))
        assertTrue(engine.step(g).gameOver)
    }

    @Test
    fun step_hittingBodyEndsGame() {
        // Kare seklinde kivrilmis yilan, basi govdesine giriyor
        val snake = listOf(Cell(5, 5), Cell(5, 6), Cell(4, 6), Cell(4, 5), Cell(4, 4))
        val g = game(snake, food = Cell(0, 0), dir = SnakeDirection.UP)
        assertTrue(engine.step(g, SnakeDirection.LEFT).gameOver)
    }

    @Test
    fun step_movingIntoTailCellIsAllowed() {
        // 2x2 dongu: bas, kuyrugun su anki yerine giriyor; kuyruk ayni adimda cekildigi icin serbest
        val snake = listOf(Cell(5, 5), Cell(5, 6), Cell(4, 6), Cell(4, 5))
        val g = game(snake, food = Cell(0, 0), dir = SnakeDirection.UP)
        val next = engine.step(g, SnakeDirection.LEFT)
        assertFalse(next.gameOver)
        assertEquals(Cell(4, 5), next.snake.first())
    }

    @Test
    fun opposite_directions() {
        assertTrue(SnakeDirection.UP.isOpposite(SnakeDirection.DOWN))
        assertFalse(SnakeDirection.UP.isOpposite(SnakeDirection.LEFT))
    }

    @Test
    fun spawnFood_nullWhenBoardFull() {
        val small = SnakeEngine(gridSize = 2)
        assertNull(small.spawnFood(listOf(Cell(0, 0), Cell(1, 0), Cell(1, 1), Cell(0, 1))))
        assertNotNull(small.spawnFood(listOf(Cell(0, 0))))
    }

    @Test
    fun speed_increasesButIsCapped() {
        val g = engine.newGame()
        assertEquals(SnakeEngine.INITIAL_TICK_MS, engine.tickMillis(g))
        assertEquals(SnakeEngine.MIN_TICK_MS, engine.tickMillis(g.copy(foodsEaten = 1000)))
    }
}
