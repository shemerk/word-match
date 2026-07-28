package com.wordmatch.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.wordmatch.R

/** Feedback audio. Kept behind an interface so the ViewModel stays unit-testable. */
interface SoundManager {
    fun playCorrect()
    fun playWrong()
}

// ponytail: no release() — SoundPool lives for the app's single activity; the OS reclaims it on exit.
class AndroidSoundManager(context: Context) : SoundManager {
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    // One success sound is picked at random per correct answer. Add another success_N.mp3 to
    // res/raw and list it here to grow the pool (names must be lowercase, no dashes).
    private val correctIds = listOf(R.raw.success, R.raw.success_1, R.raw.success_2, R.raw.success_3, R.raw.success_4)
        .map { pool.load(context, it, 1) }
    private val wrongId = pool.load(context, R.raw.fail, 1)

    override fun playCorrect() { pool.play(correctIds.random(), 1f, 1f, 1, 0, 1f) }
    override fun playWrong() { pool.play(wrongId, 1f, 1f, 1, 0, 1f) }
}

/** No-op for tests / silent mode. */
class NoOpSoundManager : SoundManager {
    override fun playCorrect() {}
    override fun playWrong() {}
}
