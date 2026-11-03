package com.eomer91.minigamesworld.ui.games.animals

import androidx.annotation.StringRes
import com.eomer91.minigamesworld.R
import kotlin.random.Random

enum class Animal(val id: String, @StringRes val nameRes: Int, @StringRes val soundRes: Int) {
    DOG("dog", R.string.an_dog, R.string.an_snd_dog),
    CAT("cat", R.string.an_cat, R.string.an_snd_cat),
    COW("cow", R.string.an_cow, R.string.an_snd_cow),
    SHEEP("sheep", R.string.an_sheep, R.string.an_snd_sheep),
    PIG("pig", R.string.an_pig, R.string.an_snd_pig),
    CHICKEN("chicken", R.string.an_chicken, R.string.an_snd_chicken),
    DUCK("duck", R.string.an_duck, R.string.an_snd_duck),
    HORSE("horse", R.string.an_horse, R.string.an_snd_horse),
    FROG("frog", R.string.an_frog, R.string.an_snd_frog),
    LION("lion", R.string.an_lion, R.string.an_snd_lion),
    ELEPHANT("elephant", R.string.an_elephant, R.string.an_snd_elephant),
    MONKEY("monkey", R.string.an_monkey, R.string.an_snd_monkey),
}

enum class QuestionKind { NAME, SOUND }

data class AnimalQuestion(val answer: Animal, val options: List<Animal>, val kind: QuestionKind)

object AnimalQuiz {
    const val QUESTIONS = 10

    /** Tekrarsiz cevaplarla [n] soru; her soruda 4 farkli secenek. */
    fun make(n: Int = QUESTIONS, random: Random = Random.Default): List<AnimalQuestion> {
        val answers = Animal.entries.shuffled(random).take(n)
        return answers.map { a ->
            val others = Animal.entries.filter { it != a }.shuffled(random).take(3)
            AnimalQuestion(a, (others + a).shuffled(random), if (random.nextBoolean()) QuestionKind.NAME else QuestionKind.SOUND)
        }
    }

    fun stars(correct: Int, total: Int = QUESTIONS): Int = when {
        correct >= total * 0.9 -> 3
        correct >= total * 0.7 -> 2
        correct >= total * 0.5 -> 1
        else -> 0
    }
}
