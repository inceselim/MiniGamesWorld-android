package com.eomer91.minigamesworld.ui.games.animals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AnimalQuizTest {

    @Test
    fun questionsAreValid() {
        repeat(20) { seed ->
            val qs = AnimalQuiz.make(random = Random(seed))
            assertEquals(AnimalQuiz.QUESTIONS, qs.size)
            assertEquals(qs.size, qs.map { it.answer }.toSet().size)
            qs.forEach { q ->
                assertEquals(4, q.options.toSet().size)
                assertTrue(q.answer in q.options)
            }
        }
    }

    @Test
    fun everyAnimalHasArt() {
        Animal.entries.forEach { assertTrue(it.id, AnimalArt.parts[it.id]?.isNotEmpty() == true) }
    }

    @Test
    fun starThresholds() {
        assertEquals(3, AnimalQuiz.stars(9))
        assertEquals(2, AnimalQuiz.stars(7))
        assertEquals(1, AnimalQuiz.stars(5))
        assertEquals(0, AnimalQuiz.stars(4))
    }
}
