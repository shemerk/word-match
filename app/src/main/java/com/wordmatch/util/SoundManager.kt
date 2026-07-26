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

    private val correctId = pool.load(context, R.raw.success, 1)
    private val wrongId = pool.load(context, R.raw.fail, 1)

    override fun playCorrect() { pool.play(correctId, 1f, 1f, 1, 0, 1f) }
    override fun playWrong() { pool.play(wrongId, 1f, 1f, 1, 0, 1f) }
}

/** No-op for tests / silent mode. */
class NoOpSoundManager : SoundManager {
    override fun playCorrect() {}
    override fun playWrong() {}
}
