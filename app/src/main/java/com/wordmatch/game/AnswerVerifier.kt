package com.wordmatch.game

import com.wordmatch.config.GameConfig
import java.util.Locale

/**
 * Case-insensitive, whitespace-tolerant answer matching for English answers.
 * A single leading article/"to" is optional on either side (see [GameConfig.LEADING_ARTICLES]).
 */
object AnswerVerifier {

    fun isCorrect(userInput: String, correctAnswer: String): Boolean =
        normalize(userInput) == normalize(correctAnswer)

    fun normalize(raw: String): String {
        val collapsed = raw.trim().lowercase(Locale.ENGLISH).replace(Regex("\\s+"), " ")
        val space = collapsed.indexOf(' ')
        if (space <= 0) return collapsed
        val firstToken = collapsed.substring(0, space)
        return if (firstToken in GameConfig.LEADING_ARTICLES) collapsed.substring(space + 1) else collapsed
    }
}
