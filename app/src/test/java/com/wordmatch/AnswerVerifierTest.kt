package com.wordmatch

import com.wordmatch.game.AnswerVerifier
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

    @Test fun wrongAnswers() {
        assertFalse(AnswerVerifier.isCorrect("dog", "cat"))
        assertFalse(AnswerVerifier.isCorrect("app", "apple"))
        assertFalse(AnswerVerifier.isCorrect("", "apple"))
    }
}
