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

    /** Non-space letters in the (article-stripped) answer — the max a progressive hint can reveal. */
    fun letterCount(correctAnswer: String): Int = normalize(correctAnswer).count { it != ' ' }

    /**
     * Masked hint for the (article-stripped) answer, revealed "outside-in": first the leading letter,
     * then the trailing one, then filling inward from the left — so a couple of taps bracket the word.
     * Unrevealed letters become "_", spaces kept. [reveal] = how many letters to show.
     * e.g. hint("A cat",1)="c__", hint("A cat",2)="c_t", hint("It is",1)="i_ __".
     */
    fun hint(correctAnswer: String, reveal: Int = GameConfig.HINT_REVEAL_LETTERS): String {
        val target = normalize(correctAnswer)
        val revealed = revealOrder(target.count { it != ' ' }).take(reveal).toSet()
        var idx = 0
        return buildString {
            for (ch in target) {
                if (ch == ' ') append(' ') else { append(if (idx in revealed) ch else '_'); idx++ }
            }
        }
    }

    /** Letter positions in reveal priority: first, last, then inward from the left. */
    private fun revealOrder(letterCount: Int): List<Int> {
        if (letterCount <= 0) return emptyList()
        val order = mutableListOf(0)
        if (letterCount > 1) order += letterCount - 1
        for (i in 1 until letterCount - 1) order += i
        return order
    }
}
