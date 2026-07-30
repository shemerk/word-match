package com.wordmatch

import com.wordmatch.data.InMemoryScoreStore
import org.junit.Assert.assertEquals
import org.junit.Test

/** Lifetime mascot points must survive a high-score reset (Decision 1) and only zero on resetProgress. */
class ScoreStoreTest {

    @Test fun addPointsAccumulates() {
        val store = InMemoryScoreStore()
        store.addPoints(10); store.addPoints(15)
        assertEquals(25, store.totalPoints())
    }

    @Test fun resetAllKeepsLifetimePoints() {
        val store = InMemoryScoreStore()
        store.saveIfBetter(10, 99, 5)
        store.addPoints(40)
        store.resetAll()
        assertEquals(0, store.bestScore(10)) // scores wiped
        assertEquals(40, store.totalPoints()) // mascot untouched
    }

    @Test fun resetProgressZeroesOnlyPoints() {
        val store = InMemoryScoreStore()
        store.saveIfBetter(10, 99, 5)
        store.addPoints(40)
        store.resetProgress()
        assertEquals(0, store.totalPoints())
        assertEquals(99, store.bestScore(10)) // scores untouched
    }
}
