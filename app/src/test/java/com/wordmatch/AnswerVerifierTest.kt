package com.wordmatch

import com.wordmatch.game.AnswerVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerVerifierTest {

    @Test fun exactMatch() {
        assertTrue(AnswerVerifier.isCorrect("apple", "apple"))
    }

    @Test fun caseInsensitive() {
        assertTrue(AnswerVerifier.isCorrect("APPLE", "apple"))
        assertTrue(AnswerVerifier.isCorrect("Apple", "apple"))
    }

    @Test fun whitespaceTrimmedAndCollapsed() {
        assertTrue(AnswerVerifier.isCorrect("  apple  ", "apple"))
        assertTrue(AnswerVerifier.isCorrect("oren   has  a   cat", "Oren has a cat"))
    }

    @Test fun leadingArticleOptional() {
        assertTrue(AnswerVerifier.isCorrect("cat", "A cat"))
        assertTrue(AnswerVerifier.isCorrect("a cat", "A cat"))
        assertTrue(AnswerVerifier.isCorrect("run", "To run"))
        assertTrue(AnswerVerifier.isCorrect("To run", "run"))
    }

    @Test fun internalArticleIsKept() {
        assertTrue(AnswerVerifier.isCorrect("oren has a cat", "Oren has a cat"))
        assertFalse(AnswerVerifier.isCorrect("oren has cat", "Oren has a cat"))
    }

    @Test fun punctuationTreatedAsSpace() {
        assertTrue(AnswerVerifier.isCorrect("I live in Ramat Gan", "I live in Ramat-Gan"))
        assertTrue(AnswerVerifier.isCorrect("ice cream", "ice-cream"))
        assertTrue(AnswerVerifier.isCorrect("well, hello", "well hello"))
        assertTrue(AnswerVerifier.isCorrect("black — white", "black white")) // em-dash
    }

    @Test fun wrongAnswers() {
        assertFalse(AnswerVerifier.isCorrect("dog", "cat"))
        assertFalse(AnswerVerifier.isCorrect("app", "apple"))
        assertFalse(AnswerVerifier.isCorrect("", "apple"))
    }

    @Test fun hebrewExactMatch() {
        assertTrue(AnswerVerifier.isCorrect("חתול", "חתול", hebrew = true))
        assertFalse(AnswerVerifier.isCorrect("כלב", "חתול", hebrew = true))
    }

    @Test fun hebrewWhitespaceCollapsed() {
        assertTrue(AnswerVerifier.isCorrect("  לאורן   יש  חתול ", "לאורן יש חתול", hebrew = true))
    }

    @Test fun hebrewFinalFormLettersKept() {
        // ך ם ן ף ץ live inside the א-ת range and must survive normalization.
        assertTrue(AnswerVerifier.isCorrect("מלך", "מלך", hebrew = true))
    }

    @Test fun hebrewHintMasksLetters() {
        // חתול = 4 letters; reveal order is first, last, then inward (same as English).
        assertEquals("ח___", AnswerVerifier.hint("חתול", 1, hebrew = true))
        assertEquals("ח__ל", AnswerVerifier.hint("חתול", 2, hebrew = true))
    }

    @Test fun hintRevealsOutsideIn() {
        assertEquals("c__", AnswerVerifier.hint("A cat", 1))     // article stripped, first letter
        assertEquals("c_t", AnswerVerifier.hint("A cat", 2))     // then last: first + last
        assertEquals("r_n", AnswerVerifier.hint("To run", 2))
        assertEquals("i_ __", AnswerVerifier.hint("It is", 1))   // spaces preserved
    }
}
