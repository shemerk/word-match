package com.wordmatch.data

import android.content.Context
import com.wordmatch.config.Direction
import com.wordmatch.config.GameConfig

/**
 * Persists the best score and best streak to beat, kept SEPARATELY per session-size bucket
 * (10 / 20 / 50 / 100 words), plus the sound on/off preference and the lifetime card collection.
 *
 * Scores, points and cards are ALSO namespaced per child (see [setActiveChild]): each child has an
 * independent album and record book. The sound pref and the remembered active child are global.
 */
interface ScoreStore {
    /** The child whose progress is currently active (persisted). Defaults to the first configured. */
    fun activeChildId(): String
    /** Switches the active child; all score/points/card reads and writes now target that child. */
    fun setActiveChild(id: String)

    fun bestScore(size: Int): Int
    fun bestStreak(size: Int): Int

    /** Saves any new best score/streak for [size]. Returns true if the best SCORE was beaten. */
    fun saveIfBetter(size: Int, score: Int, streak: Int): Boolean

    fun soundEnabled(): Boolean
    fun setSoundEnabled(on: Boolean)

    /** Translation direction preference — global (not per-child), like [soundEnabled]. */
    fun direction(): Direction
    fun setDirection(d: Direction)

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

/** SharedPreferences-backed store. Keys are namespaced by active child AND (for scores) by size,
 *  so neither child nor bucket ever collides. */
class PrefsScoreStore(context: Context) : ScoreStore {
    private val prefs = context.getSharedPreferences("wordmatch_scores", Context.MODE_PRIVATE)

    // Card progress lives in a SEPARATE file so resetAll()'s per-child clear on wordmatch_scores can
    // never wipe it — this separation IS the "keep your cards on reset" guarantee.
    private val player = context.getSharedPreferences("wordmatch_player", Context.MODE_PRIVATE)

    // Active child prefixes every score/points/card key. Restored from the (global) saved choice.
    private var child = player.getString("active_child", null) ?: GameConfig.CHILDREN.first().id

    override fun activeChildId() = child
    override fun setActiveChild(id: String) {
        child = id
        player.edit().putString("active_child", id).apply()
    }

    override fun bestScore(size: Int) = prefs.getInt("${child}_score_$size", 0)
    override fun bestStreak(size: Int) = prefs.getInt("${child}_streak_$size", 0)

    override fun saveIfBetter(size: Int, score: Int, streak: Int): Boolean {
        val beatScore = score > bestScore(size)
        prefs.edit().apply {
            if (beatScore) putInt("${child}_score_$size", score)
            if (streak > bestStreak(size)) putInt("${child}_streak_$size", streak)
        }.apply()
        return beatScore
    }

    override fun soundEnabled() = prefs.getBoolean("sound_enabled", true)
    override fun setSoundEnabled(on: Boolean) = prefs.edit().putBoolean("sound_enabled", on).apply()

    override fun direction(): Direction =
        runCatching { Direction.valueOf(prefs.getString("direction", "")!!) }.getOrDefault(GameConfig.DEFAULT_DIRECTION)
    override fun setDirection(d: Direction) = prefs.edit().putString("direction", d.name).apply()

    override fun resetAll() {
        // Only the ACTIVE child's records — remove each bucket's keys (clear() would nuke both kids).
        prefs.edit().apply {
            GameConfig.SESSION_SIZES.forEach { remove("${child}_score_$it"); remove("${child}_streak_$it") }
        }.apply()
    }

    override fun totalPoints() = player.getInt("${child}_total_points", 0)
    override fun addPoints(delta: Int) = player.edit().putInt("${child}_total_points", totalPoints() + delta).apply()
    override fun resetProgress() = player.edit().remove("${child}_total_points").apply()

    override fun ownedCardIds(): Set<Int> =
        (player.getStringSet("${child}_owned_cards", emptySet()) ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()

    override fun unlockCard(id: Int) {
        // getStringSet returns a shared instance that must not be mutated in place — copy first.
        val cur = (player.getStringSet("${child}_owned_cards", emptySet()) ?: emptySet()).toMutableSet()
        cur += id.toString()
        player.edit().putStringSet("${child}_owned_cards", cur).apply()
    }

    override fun resetCards() = player.edit().remove("${child}_owned_cards").apply()
}

/** In-memory store for unit tests (no Android dependency). */
class InMemoryScoreStore(private var sound: Boolean = true) : ScoreStore {
    private val scores = mutableMapOf<Int, Int>()
    private val streaks = mutableMapOf<Int, Int>()
    private var points = 0
    private val cards = mutableSetOf<Int>()

    // ponytail: single namespace — no test switches child; add per-child maps if one ever needs to.
    private var child = GameConfig.CHILDREN.first().id
    override fun activeChildId() = child
    override fun setActiveChild(id: String) { child = id }

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

    private var direction = GameConfig.DEFAULT_DIRECTION
    override fun direction() = direction
    override fun setDirection(d: Direction) { direction = d }
    override fun resetAll() { scores.clear(); streaks.clear() } // points + cards untouched by design

    override fun totalPoints() = points
    override fun addPoints(delta: Int) { points += delta }
    override fun resetProgress() { points = 0 }

    override fun ownedCardIds(): Set<Int> = cards.toSet()
    override fun unlockCard(id: Int) { cards += id }
    override fun resetCards() { cards.clear() }
}
