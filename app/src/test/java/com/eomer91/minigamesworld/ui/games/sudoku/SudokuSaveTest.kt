package com.eomer91.minigamesworld.ui.games.sudoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class SudokuSaveTest {

    @Test
    fun jsonRoundTrip() {
        val p = SudokuGenerator(Random(7)).generate(SudokuDifficulty.NORMAL)
        val notes = List(81) { if (it == 3) listOf(1, 5, 9) else emptyList() }
        val save = SudokuSave(
            difficulty = SudokuDifficulty.NORMAL.name,
            givens = p.givens,
            solution = p.solution,
            entries = List(81) { 0 },
            notes = notes,
            mistakes = 2,
            hintsUsed = 1,
            elapsedSeconds = 431,
        )
        assertEquals(save, SudokuSave.fromJson(save.toJson()))
    }

    @Test
    fun invalidSave_returnsNull() {
        assertNull(SudokuSave.fromJson("nope"))
        assertNull(
            SudokuSave.fromJson(
                """{"difficulty":"NORMAL","givens":[1],"solution":[1],"entries":[0],"notes":[[]],"mistakes":0,"hintsUsed":0,"elapsedSeconds":0}""",
            ),
        )
    }
}
