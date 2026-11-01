package com.eomer91.minigamesworld.ui.games.blockpuzzle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlockPuzzleSaveTest {

    @Test
    fun jsonRoundTrip_keepsBoardPiecesScoreAndGold() {
        val engine = BlockPuzzleEngine()
        val piece = BlockPiece(Shape(listOf(0 to 0, 1 to 0, 1 to 1)), color = 3)
        val board = engine.place(engine.emptyBoard(), piece, 2, 4)
        val state = BlockPuzzleUiState(
            board = board,
            pieces = listOf(piece, null, piece),
            placeable = listOf(true, false, true),
            score = 470,
            isLoading = false,
        )

        val restored = BlockPuzzleSave.fromJson(BlockPuzzleSave.from(state, goldAwarded = 2).toJson())!!

        assertEquals(board, restored.board)
        assertEquals(470, restored.score)
        assertEquals(2, restored.goldAwarded)
        assertEquals(listOf(piece, null, piece), restored.piecesAsDomain())
    }

    @Test
    fun brokenJson_returnsNull() {
        assertNull(BlockPuzzleSave.fromJson("not json"))
        assertNull(BlockPuzzleSave.fromJson("""{"version":99,"board":[],"pieces":[],"score":0,"goldAwarded":0}"""))
    }
}
