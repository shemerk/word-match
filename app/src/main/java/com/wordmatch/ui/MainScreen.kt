package com.wordmatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import com.wordmatch.config.GameConfig
import com.wordmatch.game.GameViewModel
import com.wordmatch.model.GameState
import com.wordmatch.model.GameStatus
import com.wordmatch.ui.theme.SoftGreen
import com.wordmatch.ui.theme.SunnyOrange
import com.wordmatch.ui.theme.WarmGray

/** All Hebrew UI copy in one place (Phase 2 moves these to strings.xml). */
private object Ui {
    const val LOADING = "טוען..."
    const val ERROR = "שגיאה בטעינת המילים"
    const val INSTRUCTION = "התרגם את המילה"
    const val LANG_HINT = "עברית → אנגלית"
    const val INPUT_PLACEHOLDER = "תשובתך כאן"
    const val CHECK = "בדוק"
    const val FORFEIT = "הפקר"
    const val GOT_IT = "הבנתי!"
    const val CORRECT = "✅ כל הכבוד!"
    const val WRONG = "❌ עוד לא... נסה שוב"
    const val ANSWER_WAS = "התשובה היתה: "
}

@Composable
fun MainScreen(viewModel: GameViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    // Force full RTL for the whole screen.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
            when (state.status) {
                GameStatus.LOADING -> CenteredMessage(Ui.LOADING)
                GameStatus.ERROR -> CenteredMessage(Ui.ERROR)
                else -> GameView(state, viewModel)
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (text == Ui.LOADING) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
        }
        Text(text, fontSize = GameConfig.FONT_FEEDBACK_SP.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun GameView(state: GameState, viewModel: GameViewModel) {
    val word = state.currentWord
    // Answer field resets whenever the word changes; kept on a wrong answer so the child can retry.
    var answer by remember(word?.id) { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderStats(state)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { (state.streak % 5) / 5f },
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )

        Spacer(Modifier.height(32.dp))
        Text(Ui.INSTRUCTION, fontSize = GameConfig.FONT_INSTRUCTION_SP.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(Ui.LANG_HINT, fontSize = GameConfig.FONT_LANG_HINT_SP.sp, color = WarmGray)

        Spacer(Modifier.height(20.dp))
        WordCard(word?.hebrew ?: "")

        Spacer(Modifier.height(28.dp))
        when {
            state.status == GameStatus.FORFEIT ->
                ForfeitReveal(word?.english ?: "", onGotIt = viewModel::acknowledgeForfeit)

            state.isAnswerCorrect == true ->
                Text(
                    Ui.CORRECT,
                    fontSize = GameConfig.FONT_FEEDBACK_SP.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftGreen
                )

            else -> AnswerArea(
                answer = answer,
                wrong = state.isAnswerCorrect == false,
                onAnswerChange = { answer = it },
                onCheck = { viewModel.checkAnswer(answer) },
                onForfeit = viewModel::forfeit
            )
        }
    }
}

@Composable
private fun HeaderStats(state: GameState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🔥 ${state.streak}", fontSize = GameConfig.FONT_STATS_SP.sp, fontWeight = FontWeight.Bold)
        Text("נקודות: ${state.score}", fontSize = GameConfig.FONT_STATS_SP.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WordCard(hebrew: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Text(
            text = hebrew,
            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = GameConfig.FONT_WORD_CARD_SP.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AnswerArea(
    answer: String,
    wrong: Boolean,
    onAnswerChange: (String) -> Unit,
    onCheck: () -> Unit,
    onForfeit: () -> Unit
) {
    OutlinedTextField(
        value = answer,
        onValueChange = onAnswerChange,
        modifier = Modifier.fillMaxWidth().testTag("answerInput"),
        singleLine = true,
        // Answer is English -> left-to-right text inside the RTL screen.
        textStyle = TextStyle(fontSize = GameConfig.FONT_INPUT_SP.sp, textDirection = TextDirection.Ltr),
        keyboardOptions = KeyboardOptions.Default,
        placeholder = { Text(Ui.INPUT_PLACEHOLDER, color = WarmGray) }
    )

    if (wrong) {
        Spacer(Modifier.height(12.dp))
        Text(Ui.WRONG, fontSize = GameConfig.FONT_FEEDBACK_SP.sp, fontWeight = FontWeight.Bold, color = SunnyOrange)
    }

    Spacer(Modifier.height(16.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onForfeit,
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WarmGray)
        ) { Text(Ui.FORFEIT, fontSize = GameConfig.FONT_BUTTON_SP.sp) }

        Button(
            onClick = onCheck,
            modifier = Modifier.weight(1f).height(56.dp)
        ) { Text(Ui.CHECK, fontSize = GameConfig.FONT_BUTTON_SP.sp) }
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
