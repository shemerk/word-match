package com.wordmatch.data

import android.content.Context

/**
 * Persists the best score and best streak to beat, kept SEPARATELY per session-size bucket
 * (10 / 20 / 50 / 100 words), plus the sound on/off preference and the lifetime card collection.
 */
interface ScoreStore {
    fun bestScore(size: Int): Int
    fun bestStreak(size: Int): Int

    /** Saves any new best score/streak for [size]. Returns true if the best SCORE was beaten. */
    fun saveIfBetter(size: Int, score: Int, streak: Int): Boolean

    fun soundEnabled(): Boolean
    fun setSoundEnabled(on: Boolean)

    /** Clears all best scores/streaks (all buckets). Sound preference is left untouched.
     *  MUST NOT touch lifetime points or the card collection — clearing high scores keeps your cards. */
    fun resetAll()

    // ---- Card collection: lifetime progress, deliberately independent of the per-size high scores ----

    /** Total points earned across ALL sessions ever. Drives card awards (POINTS_PER_CARD each). */
    fun totalPoints(): Int
    /** Adds to the lifetime total (called on every correct answer). */
    fun addPoints(delta: Int)
    /** Zeroes lifetime points ONLY. Separate from [resetAll]; not wired to "reset scores". */
    fun resetProgress()

    /** Ids of the cards the child has unlocked so far (see model.Deck). */
    fun ownedCardIds(): Set<Int>
    /** Records one newly-won card. */
    fun unlockCard(id: Int)
    /** Empties the collection. Paired with [resetProgress] so points can't immediately re-award them. */
    fun resetCards()
}

/** SharedPreferences-backed store. Keys are namespaced by size so buckets never collide. */
class PrefsScoreStore(context: Context) : ScoreStore {
    private val prefs = context.getSharedPreferences("wordmatch_scores", Context.MODE_PRIVATE)

    // Card progress lives in a SEPARATE file so resetAll()'s clear() on wordmatch_scores can
    // never wipe it — this separation IS the "keep your cards on reset" guarantee.
    private val player = context.getSharedPreferences("wordmatch_player", Context.MODE_PRIVATE)

    override fun bestScore(size: Int) = prefs.getInt("score_$size", 0)
    override fun bestStreak(size: Int) = prefs.getInt("streak_$size", 0)

    override fun saveIfBetter(size: Int, score: Int, streak: Int): Boolean {
        val beatScore = score > bestScore(size)
        prefs.edit().apply {
            if (beatScore) putInt("score_$size", score)
            if (streak > bestStreak(size)) putInt("streak_$size", streak)
        }.apply()
        return beatScore
    }

    override fun soundEnabled() = prefs.getBoolean("sound_enabled", true)
    override fun setSoundEnabled(on: Boolean) = prefs.edit().putBoolean("sound_enabled", on).apply()

    override fun resetAll() {
        val on = soundEnabled()
        prefs.edit().clear().putBoolean("sound_enabled", on).apply()
    }

    override fun totalPoints() = player.getInt("total_points", 0)
    override fun addPoints(delta: Int) = player.edit().putInt("total_points", totalPoints() + delta).apply()
    override fun resetProgress() = player.edit().remove("total_points").apply()

    override fun ownedCardIds(): Set<Int> =
        (player.getStringSet("owned_cards", emptySet()) ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()

    override fun unlockCard(id: Int) {
        // getStringSet returns a shared instance that must not be mutated in place — copy first.
        val cur = (player.getStringSet("owned_cards", emptySet()) ?: emptySet()).toMutableSet()
        cur += id.toString()
        player.edit().putStringSet("owned_cards", cur).apply()
    }

    override fun resetCards() = player.edit().remove("owned_cards").apply()
}

/** In-memory store for unit tests (no Android dependency). */
class InMemoryScoreStore(private var sound: Boolean = true) : ScoreStore {
    private val scores = mutableMapOf<Int, Int>()
    private val streaks = mutableMapOf<Int, Int>()
    private var points = 0
    private val cards = mutableSetOf<Int>()

    override fun bestScore(size: Int) = scores[size] ?: 0
    override fun bestStreak(size: Int) = streaks[size] ?: 0

    override fun saveIfBetter(size: Int, score: Int, streak: Int): Boolean {
        val beatScore = score > bestScore(size)
        if (beatScore) scores[size] = score
        if (streak > bestStreak(size)) streaks[size] = streak
        return beatScore
    }

    override fun soundEnabled() = sound
    override fun setSoundEnabled(on: Boolean) { sound = on }
    override fun resetAll() { scores.clear(); streaks.clear() } // points + cards untouched by design

    override fun totalPoints() = points
    override fun addPoints(delta: Int) { points += delta }
    override fun resetProgress() { points = 0 }

    override fun ownedCardIds(): Set<Int> = cards.toSet()
    override fun unlockCard(id: Int) { cards += id }
    override fun resetCards() { cards.clear() }
}
