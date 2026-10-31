package com.eomer91.minigamesworld.ui.games.snake

import kotlin.random.Random

data class Cell(val x: Int, val y: Int)

enum class SnakeDirection(val dx: Int, val dy: Int) {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

    fun isOpposite(other: SnakeDirection): Boolean = dx + other.dx == 0 && dy + other.dy == 0
}

data class SnakeGame(
    /** Ilk eleman bas. */
    val snake: List<Cell>,
    val food: Cell,
    val direction: SnakeDirection,
    val score: Int = 0,
    val foodsEaten: Int = 0,
    val gameOver: Boolean = false,
)

/** Snake kurallari. Android'e bagimli degil, unit test ile test edilir. */
class SnakeEngine(
    private val random: Random = Random.Default,
    val gridSize: Int = 16,
) {
    fun newGame(): SnakeGame {
        val snake = listOf(Cell(7, 8), Cell(6, 8), Cell(5, 8))
        return SnakeGame(snake = snake, food = spawnFood(snake) ?: Cell(0, 0), direction = SnakeDirection.RIGHT)
    }

    /** Yilanin olmadigi rastgele bir hucre; tahta doluysa null. */
    fun spawnFood(snake: List<Cell>): Cell? {
        val occupied = snake.toHashSet()
        val free = ArrayList<Cell>(gridSize * gridSize)
        for (y in 0 until gridSize) for (x in 0 until gridSize) {
            val cell = Cell(x, y)
            if (cell !in occupied) free += cell
        }
        return if (free.isEmpty()) null else free[random.nextInt(free.size)]
    }

    /** Yilani bir adim ilerletir. Yon kontrolu (ters yon vb.) cagirana aittir. */
    fun step(game: SnakeGame, direction: SnakeDirection = game.direction): SnakeGame {
        if (game.gameOver) return game

        val head = game.snake.first()
        val next = Cell(head.x + direction.dx, head.y + direction.dy)
        val eats = next == game.food
        // Yem yenmediyse kuyruk ayni adimda cekilir; kuyrugun eski yerine girmek serbesttir.
        val body = if (eats) game.snake else game.snake.dropLast(1)

        if (isOutside(next) || next in body) {
            return game.copy(direction = direction, gameOver = true)
        }

        val newSnake = listOf(next) + body
        if (!eats) return game.copy(snake = newSnake, direction = direction)

        val newFood = spawnFood(newSnake)
        return game.copy(
            snake = newSnake,
            food = newFood ?: game.food,
            direction = direction,
            score = game.score + POINTS_PER_FOOD,
            foodsEaten = game.foodsEaten + 1,
            // Tahta tamamen doldu: oyun kazanilarak biter.
            gameOver = newFood == null,
        )
    }

    /** Yem yedikce oyun hizlanir. */
    fun tickMillis(game: SnakeGame): Long =
        (INITIAL_TICK_MS - game.foodsEaten * SPEEDUP_PER_FOOD_MS).coerceAtLeast(MIN_TICK_MS)

    private fun isOutside(cell: Cell): Boolean =
        cell.x !in 0 until gridSize || cell.y !in 0 until gridSize

    companion object {
        const val POINTS_PER_FOOD = 10
        const val INITIAL_TICK_MS = 180L
        const val MIN_TICK_MS = 85L
        const val SPEEDUP_PER_FOOD_MS = 3L
    }
}
