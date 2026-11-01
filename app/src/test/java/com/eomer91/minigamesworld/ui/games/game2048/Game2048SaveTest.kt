package com.eomer91.minigamesworld.ui.games.game2048

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Game2048SaveTest {

    @Test
    fun jsonRoundTrip() {
        val board = listOf(
            listOf(2, 4, 0, 0),
            listOf(0, 2048, 0, 8),
            listOf(0, 0, 0, 0),
            listOf(16, 0, 0, 2),
        )
        val state = Game2048State(board = board, score = 15320, reachedWinTile = true, isLoading = false)
        val restored = Game2048Save.fromJson(Game2048Save.from(state, goldAwarded = 76).toJson())!!

        assertEquals(board, restored.board)
        assertEquals(15320, restored.score)
        assertTrue(restored.reachedWinTile)
        assertEquals(76, restored.goldAwarded)
    }

    @Test
    fun brokenJson_returnsNull() {
        assertNull(Game2048Save.fromJson("{}"))
        assertNull(Game2048Save.fromJson("garbage"))
    }
}
