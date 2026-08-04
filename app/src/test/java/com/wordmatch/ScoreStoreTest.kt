package com.wordmatch

import com.wordmatch.data.InMemoryScoreStore
import org.junit.Assert.assertEquals
import org.junit.Test

/** Lifetime points and the card collection must survive a high-score reset, and only zero on resetProgress/resetCards. */
class ScoreStoreTest {

    @Test fun addPointsAccumulates() {
        val store = InMemoryScoreStore()
        store.addPoints(10); store.addPoints(15)
        assertEquals(25, store.totalPoints())
    }

    @Test fun resetAllKeepsLifetimePointsAndCards() {
        val store = InMemoryScoreStore()
        store.saveIfBetter(10, 99, 5)
        store.addPoints(40)
        store.unlockCard(3)
        store.resetAll()
        assertEquals(0, store.bestScore(10))       // scores wiped
        assertEquals(40, store.totalPoints())      // points untouched
        assertEquals(setOf(3), store.ownedCardIds()) // collection untouched
    }

    @Test fun resetProgressZeroesOnlyPoints() {
        val store = InMemoryScoreStore()
        store.saveIfBetter(10, 99, 5)
        store.addPoints(40)
        store.resetProgress()
        assertEquals(0, store.totalPoints())
        assertEquals(99, store.bestScore(10)) // scores untouched
    }

    @Test fun cardsUnlockAndReset() {
        val store = InMemoryScoreStore()
        store.unlockCard(1); store.unlockCard(5); store.unlockCard(1) // dupe ignored (a Set)
        assertEquals(setOf(1, 5), store.ownedCardIds())
        store.resetCards()
        assertEquals(emptySet<Int>(), store.ownedCardIds())
    }
}
