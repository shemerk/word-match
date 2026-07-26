package com.wordmatch.data

import android.content.Context

/**
 * Persists the best score and best streak to beat, kept SEPARATELY per session-size bucket
 * (10 / 20 / 50 / 100 words), plus the sound on/off preference.
 */
interface ScoreStore {
    fun bestScore(size: Int): Int
    fun bestStreak(size: Int): Int

    /** Saves any new best score/streak for [size]. Returns true if the best SCORE was beaten. */
    fun saveIfBetter(size: Int, score: Int, streak: Int): Boolean

    fun soundEnabled(): Boolean
    fun setSoundEnabled(on: Boolean)

    /** Clears all best scores/streaks (all buckets). Sound preference is left untouched. */
    fun resetAll()
}

/** SharedPreferences-backed store. Keys are namespaced by size so buckets never collide. */
class PrefsScoreStore(context: Context) : ScoreStore {
    private val prefs = context.getSharedPreferences("wordmatch_scores", Context.MODE_PRIVATE)

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
}

/** In-memory store for unit tests (no Android dependency). */
class InMemoryScoreStore(private var sound: Boolean = true) : ScoreStore {
    private val scores = mutableMapOf<Int, Int>()
    private val streaks = mutableMapOf<Int, Int>()

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
    override fun resetAll() { scores.clear(); streaks.clear() }
}
