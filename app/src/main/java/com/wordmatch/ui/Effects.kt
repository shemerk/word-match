package com.wordmatch.ui

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.wordmatch.config.GameConfig
import com.wordmatch.ui.theme.SoftGreen
import com.wordmatch.ui.theme.SunnyOrange
import com.wordmatch.ui.theme.VibrantBlue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * True when the user has turned system animations off (Developer Options "animator duration scale" =
 * 0, the Android signal for "reduce motion"). Confetti/shake/flip are suppressed when this is set.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        scale == 0f
    }
}

private data class ConfettiPiece(val angle: Float, val spread: Float, val color: Color, val radius: Float)

/**
 * Lightweight confetti burst centered near the top of the screen. [progress] runs 0f..1f; pieces fly
 * outward, fall under gravity, and fade. Draws nothing at the endpoints so it fully disappears.
 * ponytail: canvas math, no animation library.
 */
@Composable
fun ConfettiOverlay(progress: Float) {
    if (progress <= 0f || progress >= 1f) return
    val palette = listOf(SunnyOrange, SoftGreen, VibrantBlue, Color(0xFFF43F5E), Color(0xFFFACC15))
    val pieces = remember {
        List(GameConfig.CONFETTI_PIECES) {
            val a = Random.nextDouble(0.0, 2 * PI).toFloat()
            ConfettiPiece(
                angle = a,
                spread = Random.nextFloat() * 0.4f + 0.3f,
                color = palette[Random.nextInt(palette.size)],
                radius = Random.nextFloat() * 8f + 6f
            )
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val origin = Offset(size.width / 2f, size.height * 0.32f)
        val reach = size.minDimension
        pieces.forEach { p ->
            val dx = cos(p.angle) * p.spread * reach * progress
            val dy = sin(p.angle) * p.spread * reach * progress + (reach * 0.9f) * progress * progress
            drawCircle(
                color = p.color.copy(alpha = 1f - progress),
                radius = p.radius,
                center = Offset(origin.x + dx, origin.y + dy)
            )
        }
    }
}
