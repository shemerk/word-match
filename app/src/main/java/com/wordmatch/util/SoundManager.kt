package com.wordmatch.util

/** Feedback audio. Kept behind an interface so the ViewModel stays unit-testable. */
interface SoundManager {
    fun playCorrect()
    fun playWrong()
}

// ponytail: Phase 2 swaps in a MediaPlayer-backed impl. No-op keeps Phase 1 buildable and silent.
class NoOpSoundManager : SoundManager {
    override fun playCorrect() {}
    override fun playWrong() {}
}
