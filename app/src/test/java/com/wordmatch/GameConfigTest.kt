package com.wordmatch

import com.wordmatch.config.GameConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Level math is the one non-trivial branch in the mascot feature. Assertions derive from the
 * config constants (not hardcoded thresholds) so retuning LEVEL_BASE_COST/GROWTH can't break them.
 */
class GameConfigTest {

    private val base = GameConfig.LEVEL_BASE_COST
    private val growth = GameConfig.LEVEL_GROWTH

    @Test fun levelForAtBoundaries() {
        assertEquals(1, GameConfig.levelFor(0))
        assertEquals(1, GameConfig.levelFor(base - 1))
        assertEquals(2, GameConfig.levelFor(base))                 // first threshold
        assertEquals(2, GameConfig.levelFor(base + base * growth - 1))
        assertEquals(3, GameConfig.levelFor(base + base * growth)) // second threshold
    }

    @Test fun levelKeepsClimbingPastArtCap() {
        // Art clamps at MASCOT_TIER_COUNT, but the level number does not (feeds prestige stars).
        val level = GameConfig.levelFor(Int.MAX_VALUE / 2)
        assertTrue("expected level past the art cap", level > GameConfig.MASCOT_TIER_COUNT)
    }

    @Test fun levelProgressReportsIntoAndNeeded() {
        assertEquals(0 to base, GameConfig.levelProgress(0))               // fresh level 1
        assertEquals(0 to base * growth, GameConfig.levelProgress(base))   // just reached level 2
        // Halfway into level 2:
        val half = base + base * growth / 2
        assertEquals((base * growth / 2) to (base * growth), GameConfig.levelProgress(half))
    }

    @Test fun pointsToNextLevelCountsDown() {
        assertEquals(base, GameConfig.pointsToNextLevel(0))
        assertEquals(1, GameConfig.pointsToNextLevel(base - 1))
    }
}
