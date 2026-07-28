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
        val collapsed = raw.lowercase(Locale.ENGLISH)
            // Treat any punctuation / dash / em-dash as a space so "Ramat-Gan" == "Ramat Gan".
            // Symmetric: normalize() runs on both sides. Note: also drops apostrophes (none in the
            // current word list); map "'" to "" instead if contraction answers are ever added.
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ").trim()
        val space = collapsed.indexOf(' ')
        if (space <= 0) return collapsed
        val firstToken = collapsed.substring(0, space)
        return if (firstToken in GameConfig.LEADING_ARTICLES) collapsed.substring(space + 1) else collapsed
    }

    /**
     * Masked hint for the (article-stripped) answer: first [reveal] letters shown, remaining letters
     * become "_", spaces kept. e.g. hint("A cat", 1) = "c__", hint("It is", 1) = "i_ __".
     */
    fun hint(correctAnswer: String, reveal: Int = GameConfig.HINT_REVEAL_LETTERS): String {
        val target = normalize(correctAnswer)
        var shown = 0
        return buildString {
            for (ch in target) when {
                ch == ' ' -> append(' ')
                shown < reveal -> { append(ch); shown++ }
                else -> append('_')
            }
        }
    }
}
