package com.eomer91.minigamesworld.ui.games.sudoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuEngineTest {

    @Test
    fun generatedPuzzle_isValidAndUnique() {
        SudokuDifficulty.entries.forEach { difficulty ->
            repeat(3) { seed ->
                val gen = SudokuGenerator(Random(seed * 31 + difficulty.ordinal))
                val p = gen.generate(difficulty)

                assertTrue(SudokuRules.isValidSolution(p.solution))
                // Ipuclari cozumle uyumlu
                p.givens.forEachIndexed { i, v -> if (v != 0) assertEquals(p.solution[i], v) }
                // Tek cozum
                assertEquals(1, gen.countSolutions(p.givens.toIntArray()))
                // Ipucu sayisi hedefe yakin (tekillik bazen daha fazla ipucu gerektirir)
                val clues = p.givens.count { it != 0 }
                assertTrue("clues=$clues", clues in difficulty.clues..difficulty.clues + 8)
            }
        }
    }

    @Test
    fun peers_are20Cells() {
        assertEquals(20, SudokuRules.peers(0).size)
        assertEquals(20, SudokuRules.peers(40).size)
    }

    @Test
    fun score_penaltiesAndFloor() {
        assertEquals(1200, sudokuScore(SudokuDifficulty.NORMAL, 0, 0, 300))
        assertEquals(1200 - 100 - 150 - 80, sudokuScore(SudokuDifficulty.NORMAL, 1, 1, 800))
        assertEquals(240, sudokuScore(SudokuDifficulty.NORMAL, 3, 5, 99999))
    }
}
