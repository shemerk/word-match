package com.wordmatch.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordmatch.R
import com.wordmatch.config.GameConfig
import com.wordmatch.model.GameState
import com.wordmatch.ui.theme.WarmGray
import kotlinx.coroutines.delay

/** Tier sprite for a level, clamped to the last piece of art (levels past the cap reuse the top). */
private fun tierDrawable(level: Int): Int = when (level.coerceIn(1, GameConfig.MASCOT_TIER_COUNT)) {
    1 -> R.drawable.mascot_tier1
    2 -> R.drawable.mascot_tier2
    3 -> R.drawable.mascot_tier3
    4 -> R.drawable.mascot_tier4
    else -> R.drawable.mascot_tier5
}

/** Chosen team colour; wraps if the stored index ever outruns the list. */
internal fun jerseyColor(index: Int): Color =
    Color(GameConfig.JERSEY_COLORS[index.mod(GameConfig.JERSEY_COLORS.size)])

/**
 * Start-screen trophy case — the long-term payoff the child lingers on between sessions:
 * name + big sprite + rank + progress to next level + next-unlock teaser + the tier shelf.
 */
@Composable
fun MascotTrophyCase(state: GameState, onEditPlayer: () -> Unit) {
    val accent = jerseyColor(state.jerseyColor)
    val pointsToNext = (state.levelNeed - state.levelInto).coerceAtLeast(0)
    val fraction = if (state.levelNeed > 0) state.levelInto.toFloat() / state.levelNeed else 0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (state.playerName.isBlank()) "?" else state.playerName,
                fontSize = GameConfig.FONT_PLAYER_NAME_SP.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            TextButton(onClick = onEditPlayer) { Text(Ui.EDIT_PLAYER, fontSize = GameConfig.FONT_STATS_SP.sp) }
        }

        Image(
            painter = painterResource(tierDrawable(state.level)),
            contentDescription = Ui.rankTitle(state.level),
            modifier = Modifier
                .size(GameConfig.MASCOT_BIG_DP.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(3.dp, accent, RoundedCornerShape(16.dp))
        )

        Spacer(Modifier.height(6.dp))
        Text(Ui.rankTitle(state.level), fontSize = GameConfig.FONT_RANK_TITLE_SP.sp, fontWeight = FontWeight.Bold)
        Text(Ui.levelLabel(state.level), fontSize = GameConfig.FONT_RECORD_SP.sp, color = WarmGray)

        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            color = accent,
            modifier = Modifier.fillMaxWidth().height(10.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            Ui.nextUnlock(state.level, pointsToNext),
            fontSize = GameConfig.FONT_MASCOT_TEASER_SP.sp,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))
        MascotShelf(state.level, accent)
    }
}

/** The collectible shelf: every tier as a thumbnail — unlocked bright, current framed, locked dimmed. */
@Composable
private fun MascotShelf(level: Int, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (tier in 1..GameConfig.MASCOT_TIER_COUNT) {
            val unlocked = level >= tier
            Image(
                painter = painterResource(tierDrawable(tier)),
                contentDescription = null,
                modifier = Modifier
                    .size(GameConfig.MASCOT_SHELF_DP.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .then(if (level == tier) Modifier.border(2.dp, accent, RoundedCornerShape(8.dp)) else Modifier)
                    .alpha(if (unlocked) 1f else 0.3f)
            )
        }
    }
}

/**
 * Compact header avatar with a level badge. Reacts to the moment: a pop on a correct answer, a
 * bigger pop on a level-up, a dim slump on a forfeit. All motion is gated by [reducedMotion].
 */
@Composable
fun MascotBadge(state: GameState, reducedMotion: Boolean) {
    val accent = jerseyColor(state.jerseyColor)
    val scale = remember { Animatable(1f) }

    LaunchedEffect(state.checkNonce) {
        if (reducedMotion || state.checkNonce == 0 || state.isAnswerCorrect != true) return@LaunchedEffect
        scale.snapTo(GameConfig.MASCOT_BOUNCE_SCALE)
        scale.animateTo(1f, tween(300))
    }
    LaunchedEffect(state.levelUpNonce) {
        if (reducedMotion || state.levelUpNonce == 0) return@LaunchedEffect
        scale.snapTo(GameConfig.MASCOT_BOUNCE_SCALE + 0.25f)
        scale.animateTo(1f, tween(450))
    }

    Box(contentAlignment = Alignment.BottomEnd) {
        Image(
            painter = painterResource(tierDrawable(state.level)),
            contentDescription = Ui.rankTitle(state.level),
            modifier = Modifier
                .size(GameConfig.MASCOT_COMPACT_DP.dp)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                .alpha(if (state.forfeited) 0.5f else 1f)
                .clip(RoundedCornerShape(8.dp))
        )
        Text(
            state.level.toString(),
            fontSize = GameConfig.FONT_LEVEL_BADGE_SP.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .clip(CircleShape)
                .background(accent)
                .padding(horizontal = 5.dp)
        )
    }
}

/** Transient "עלית רמה!" banner, shown mid-session when a correct answer crosses a level boundary. */
@Composable
fun BoxScope.LevelUpBanner(levelUpNonce: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(levelUpNonce) {
        if (levelUpNonce == 0) return@LaunchedEffect
        visible = true
        delay(GameConfig.LEVEL_UP_BANNER_MS)
        visible = false
    }
    if (visible) {
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.secondary
        ) {
            Text(
                Ui.LEVEL_UP,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                fontSize = GameConfig.FONT_LEVEL_UP_SP.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** One-time (editable) prompt: pick a player name and a team colour. */
@Composable
fun PlayerSetupDialog(
    currentName: String,
    currentJersey: Int,
    onSave: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var jersey by remember { mutableStateOf(currentJersey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), jersey) }, enabled = name.isNotBlank()) {
                Text(Ui.NAME_SAVE)
            }
        },
        title = { Text(Ui.NAME_PROMPT_TITLE) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text(Ui.NAME_PLACEHOLDER, color = WarmGray) }
                )
                Spacer(Modifier.height(16.dp))
                Text(Ui.JERSEY_LABEL, fontSize = GameConfig.FONT_STATS_SP.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GameConfig.JERSEY_COLORS.indices.forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(jerseyColor(i))
                                .border(
                                    width = if (i == jersey) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    shape = CircleShape
                                )
                                .clickable { jersey = i }
                        )
                    }
                }
            }
        }
    )
}
