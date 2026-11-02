package com.eomer91.minigamesworld.ui.games.stack

import kotlin.math.abs

/** Tahta genisligi [StackEngine.BOARD_WIDTH] birim; bloklar bu birimlerle tutulur. */
data class StackBlock(val x: Float, val width: Float, val level: Int) {
    val right: Float get() = x + width
}

/** Kesilip dusen parca (sadece gorsel). */
data class Debris(val x: Float, val width: Float, val level: Int, val age: Float = 0f)

data class StackGame(
    /** Ilk eleman zemin bloku. */
    val blocks: List<StackBlock>,
    val movingX: Float,
    val movingWidth: Float,
    /** +1 saga, -1 sola. */
    val direction: Int,
    val speed: Float,
    val score: Int = 0,
    val combo: Int = 0,
    val gameOver: Boolean = false,
    val debris: List<Debris> = emptyList(),
) {
    val top: StackBlock get() = blocks.last()
    /** Kayan blogun bulundugu kat. */
    val movingLevel: Int get() = top.level + 1
    /** Yerlestirilen blok sayisi (zemin haric). */
    val height: Int get() = blocks.size - 1
}

enum class DropResult { PLACED, PERFECT, MISSED }

/** Stack kurallari. Android'e bagimli degil, unit test ile test edilir. */
class StackEngine {

    fun newGame(): StackGame {
        val base = StackBlock(x = (BOARD_WIDTH - START_WIDTH) / 2f, width = START_WIDTH, level = 0)
        return StackGame(
            blocks = listOf(base),
            movingX = leftStart(START_WIDTH),
            movingWidth = START_WIDTH,
            direction = 1,
            speed = START_SPEED,
        )
    }

    /** Kayan blogu [dt] saniye ilerletir; kenarlarda geri doner. Dusen parcalari yaslandirir. */
    fun update(game: StackGame, dt: Float): StackGame {
        val debris = game.debris.map { it.copy(age = it.age + dt) }.filter { it.age < DEBRIS_LIFETIME }
        if (game.gameOver) return game.copy(debris = debris)

        var x = game.movingX + game.direction * game.speed * dt
        var dir = game.direction
        val min = leftStart(game.movingWidth)
        val max = rightStart(game.movingWidth)
        if (x > max) { x = max - (x - max); dir = -1 }
        if (x < min) { x = min + (min - x); dir = 1 }
        return game.copy(movingX = x.coerceIn(min, max), direction = dir, debris = debris)
    }

    fun drop(game: StackGame): Pair<StackGame, DropResult> {
        if (game.gameOver) return game to DropResult.MISSED
        val top = game.top
        val left = maxOf(game.movingX, top.x)
        val right = minOf(game.movingX + game.movingWidth, top.right)
        val overlap = right - left
        val level = game.movingLevel

        if (overlap <= 0f) {
            val falling = Debris(game.movingX, game.movingWidth, level)
            return game.copy(gameOver = true, debris = game.debris + falling) to DropResult.MISSED
        }

        val perfect = abs(game.movingX - top.x) <= PERFECT_TOLERANCE
        val placed: StackBlock
        val newDebris: List<Debris>
        val combo: Int
        if (perfect) {
            combo = game.combo + 1
            // Ust uste kusursuz birakis: blok biraz genisler (odul).
            val grow = if (combo >= GROW_AFTER_COMBO) GROW_AMOUNT else 0f
            val width = (top.width + grow).coerceAtMost(START_WIDTH)
            val x = (top.x - (width - top.width) / 2f).coerceIn(0f, BOARD_WIDTH - width)
            placed = StackBlock(x, width, level)
            newDebris = emptyList()
        } else {
            combo = 0
            placed = StackBlock(left, overlap, level)
            newDebris = listOfNotNull(
                if (game.movingX < left) Debris(game.movingX, left - game.movingX, level) else null,
                if (game.movingX + game.movingWidth > right) Debris(right, game.movingX + game.movingWidth - right, level) else null,
            )
        }

        val nextLevel = level + 1
        val fromLeft = nextLevel % 2 == 1
        val next = game.copy(
            blocks = game.blocks + placed,
            movingWidth = placed.width,
            movingX = if (fromLeft) leftStart(placed.width) else rightStart(placed.width),
            direction = if (fromLeft) 1 else -1,
            speed = (game.speed + SPEEDUP_PER_BLOCK).coerceAtMost(MAX_SPEED),
            score = game.score + POINTS_PER_BLOCK + if (perfect) PERFECT_BONUS * combo.coerceAtMost(5) else 0,
            combo = combo,
            debris = game.debris + newDebris,
        )
        return next to if (perfect) DropResult.PERFECT else DropResult.PLACED
    }

    /** Blok tahtanin biraz disindan baslar ki oyuncu gelisini gorsun. */
    private fun leftStart(width: Float) = -width * 0.6f
    private fun rightStart(width: Float) = BOARD_WIDTH - width * 0.4f

    companion object {
        const val BOARD_WIDTH = 100f
        const val START_WIDTH = 60f
        const val START_SPEED = 45f
        const val MAX_SPEED = 130f
        const val SPEEDUP_PER_BLOCK = 2f
        const val PERFECT_TOLERANCE = 2.5f
        const val GROW_AFTER_COMBO = 3
        const val GROW_AMOUNT = 4f
        const val POINTS_PER_BLOCK = 10
        const val PERFECT_BONUS = 5
        const val DEBRIS_LIFETIME = 1.2f
    }
}
