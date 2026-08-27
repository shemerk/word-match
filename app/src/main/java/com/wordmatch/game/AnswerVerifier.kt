package com.wordmatch.game

import com.wordmatch.config.GameConfig
import java.util.Locale

/**
 * Case-insensitive, whitespace-tolerant answer matching.
 * English answers ([hebrew] = false): a single leading article/"to" is optional on either side
 * (see [GameConfig.LEADING_ARTICLES]). Hebrew answers ([hebrew] = true): kept exact after
 * whitespace/punctuation normalization — no leading-article strip (Hebrew "ה" is an attached
 * prefix, so stripping it would risk eating a legitimate first letter).
 */
object AnswerVerifier {

    fun isCorrect(userInput: String, correctAnswer: String, hebrew: Boolean = false): Boolean =
        normalize(userInput, hebrew) == normalize(correctAnswer, hebrew)

    fun normalize(raw: String, hebrew: Boolean = false): String {
        // Keep only answer letters + digits + space; everything else (punctuation, dash, em-dash,
        // nikud/geresh for Hebrew) becomes a space so "Ramat-Gan" == "Ramat Gan". Symmetric: runs
        // on both sides. Also drops apostrophes (none in the current word lists).
        val strip = if (hebrew) Regex("[^\\u05D0-\\u05EA0-9 ]") else Regex("[^a-z0-9 ]")
        val collapsed = raw.lowercase(Locale.ENGLISH)
            .replace(strip, " ")
            .replace(Regex("\\s+"), " ").trim()
        if (hebrew) return collapsed
        val space = collapsed.indexOf(' ')
        if (space <= 0) return collapsed
        val firstToken = collapsed.substring(0, space)
        return if (firstToken in GameConfig.LEADING_ARTICLES) collapsed.substring(space + 1) else collapsed
    }

    /** Non-space letters in the (article-stripped) answer — the max a progressive hint can reveal. */
    fun letterCount(correctAnswer: String, hebrew: Boolean = false): Int =
        normalize(correctAnswer, hebrew).count { it != ' ' }

    /**
     * Masked hint for the (article-stripped) answer, revealed "outside-in": first the leading letter,
     * then the trailing one, then filling inward from the left — so a couple of taps bracket the word.
     * Unrevealed letters become "_", spaces kept. [reveal] = how many letters to show.
     * e.g. hint("A cat",1)="c__", hint("A cat",2)="c_t", hint("It is",1)="i_ __".
     */
    fun hint(correctAnswer: String, reveal: Int = GameConfig.HINT_REVEAL_LETTERS, hebrew: Boolean = false): String {
        val target = normalize(correctAnswer, hebrew)
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
