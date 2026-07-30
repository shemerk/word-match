package com.wordmatch.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordmatch.config.GameConfig
import com.wordmatch.game.AnswerVerifier
import com.wordmatch.game.GameViewModel
import com.wordmatch.model.GameState
import com.wordmatch.ui.theme.SoftGreen
import com.wordmatch.ui.theme.SunnyOrange
import com.wordmatch.ui.theme.WarmGray
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GameScreen(state: GameState, viewModel: GameViewModel) {
    val word = state.currentWord
    val reducedMotion = rememberReducedMotion()

    // Answer + hint reset when the word changes; answer is kept on a wrong attempt for retry.
    var answer by remember(word?.id) { mutableStateOf("") }
    var hint by remember(word?.id) { mutableStateOf<String?>(null) }

    // Result animations, re-fired on every check via checkNonce (suppressed when reduced motion).
    val confetti = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val travelPx = with(LocalDensity.current) { GameConfig.SHAKE_TRAVEL_DP.dp.toPx() }
    LaunchedEffect(state.checkNonce) {
        if (state.checkNonce == 0 || reducedMotion) return@LaunchedEffect
        when (state.isAnswerCorrect) {
            true -> { confetti.snapTo(0f); confetti.animateTo(1f, tween(GameConfig.CONFETTI_DURATION_MS)) }
            false -> { shake.snapTo(1f); shake.animateTo(0f, tween(GameConfig.SHAKE_DURATION_MS)) }
            else -> {}
        }
    }
    val shakeX = sin(shake.value * PI.toFloat() * 5f) * travelPx * shake.value

    val progress by animateFloatAsState(
        targetValue = state.wordsCompleted.toFloat() / state.sessionSize,
        animationSpec = tween(if (reducedMotion) 0 else GameConfig.PROGRESS_ANIM_MS),
        label = "progress"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                TextButton(onClick = viewModel::exitGame) {
                    Text("← ${Ui.EXIT}", fontSize = GameConfig.FONT_BUTTON_SP.sp, color = WarmGray)
                }
            }
            HeaderStats(state, reducedMotion)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp))
            Spacer(Modifier.height(4.dp))
            Text(Ui.progress(state.wordsCompleted + 1, state.sessionSize), fontSize = GameConfig.FONT_RECORD_SP.sp, color = WarmGray)

            Spacer(Modifier.height(24.dp))
            Text(Ui.INSTRUCTION, fontSize = GameConfig.FONT_INSTRUCTION_SP.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(4.dp))
            Text(Ui.LANG_HINT, fontSize = GameConfig.FONT_LANG_HINT_SP.sp, color = WarmGray)

            Spacer(Modifier.height(20.dp))
            WordCard(hebrew = word?.hebrew ?: "", english = word?.english ?: "", flipped = state.forfeited, reducedMotion = reducedMotion)

            Spacer(Modifier.height(24.dp))
            when {
                state.forfeited ->
                    ForfeitReveal(word?.english ?: "", onGotIt = viewModel::acknowledgeForfeit)

                state.isAnswerCorrect == true ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Ui.CORRECT, fontSize = GameConfig.FONT_FEEDBACK_SP.sp, fontWeight = FontWeight.Bold, color = SoftGreen)
                        Text("+${state.lastGained} ${Ui.POINTS_WORD}", fontSize = GameConfig.FONT_STATS_SP.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }

                else -> AnswerArea(
                    answer = answer,
                    wrong = state.isAnswerCorrect == false,
                    shakeX = shakeX,
                    hintText = hint,
                    onHint = { hint = AnswerVerifier.hint(word?.english ?: "") },
                    onAnswerChange = { answer = it },
                    onCheck = { viewModel.checkAnswer(answer) },
                    onForfeit = viewModel::forfeit
                )
            }
        }
        ConfettiOverlay(confetti.value)
        LevelUpBanner(state.levelUpNonce)
    }
}

@Composable
private fun HeaderStats(state: GameState, reducedMotion: Boolean) {
    val flameScale = remember { Animatable(1f) }
    LaunchedEffect(state.streak) {
        if (!reducedMotion && state.streak > 0) {
            flameScale.snapTo(GameConfig.FLAME_PULSE_SCALE)
            flameScale.animateTo(1f, tween(300))
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MascotBadge(state, reducedMotion)
        Text(
            "🔥 ${state.streak}",
            modifier = Modifier
                .graphicsLayer { scaleX = flameScale.value; scaleY = flameScale.value }
                .semantics { contentDescription = "רצף ${state.streak}" },
            fontSize = GameConfig.FONT_STATS_SP.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "נקודות: ${state.score}",
            modifier = Modifier.semantics { contentDescription = "ניקוד ${state.score}" },
            fontSize = GameConfig.FONT_STATS_SP.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun WordCard(hebrew: String, english: String, flipped: Boolean, reducedMotion: Boolean) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(if (reducedMotion) 0 else GameConfig.FLIP_DURATION_MS),
        label = "flip"
    )
    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            rotationY = rotation
            cameraDistance = 12f * density
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        val showBack = rotation > 90f
        Text(
            text = if (showBack) english else hebrew,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp, horizontal = 16.dp)
                // Un-mirror the text once the card is past the halfway point.
                .graphicsLayer { rotationY = if (showBack) 180f else 0f }
                .semantics { contentDescription = if (showBack) "התשובה $english" else "המילה $hebrew" },
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = GameConfig.FONT_WORD_CARD_SP.sp,
            lineHeight = GameConfig.FONT_WORD_CARD_LINE_HEIGHT_SP.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AnswerArea(
    answer: String,
    wrong: Boolean,
    shakeX: Float,
    hintText: String?,
    onHint: () -> Unit,
    onAnswerChange: (String) -> Unit,
    onCheck: () -> Unit,
    onForfeit: () -> Unit
) {
    OutlinedTextField(
        value = answer,
        onValueChange = onAnswerChange,
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(shakeX.roundToInt(), 0) }
            .testTag("answerInput"),
        singleLine = true,
        // Answer is English -> left-to-right text inside the RTL screen.
        textStyle = TextStyle(fontSize = GameConfig.FONT_INPUT_SP.sp, textDirection = TextDirection.Ltr),
        placeholder = { Text(Ui.INPUT_PLACEHOLDER, color = WarmGray) }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onHint) { Text(Ui.HINT, fontSize = GameConfig.FONT_BUTTON_SP.sp) }
        if (hintText != null) {
            Text("${Ui.HINT_PREFIX}$hintText", fontSize = GameConfig.FONT_INPUT_SP.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        }
    }

    if (wrong) {
        Text(Ui.WRONG, fontSize = GameConfig.FONT_FEEDBACK_SP.sp, fontWeight = FontWeight.Bold, color = SunnyOrange)
        Spacer(Modifier.height(8.dp))
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onForfeit,
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WarmGray)
        ) { Text(Ui.FORFEIT, fontSize = GameConfig.FONT_BUTTON_SP.sp) }

        Button(onClick = onCheck, modifier = Modifier.weight(1f).height(56.dp)) {
            Text(Ui.CHECK, fontSize = GameConfig.FONT_BUTTON_SP.sp)
        }
    }
}

@Composable
private fun ForfeitReveal(english: String, onGotIt: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            Ui.ANSWER_WAS + english,
            fontSize = GameConfig.FONT_FEEDBACK_SP.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onGotIt, modifier = Modifier.height(56.dp).width(200.dp)) {
            Text(Ui.GOT_IT, fontSize = GameConfig.FONT_BUTTON_SP.sp)
        }
    }
}
