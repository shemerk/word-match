package com.wordmatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordmatch.config.GameConfig
import com.wordmatch.game.GameViewModel
import com.wordmatch.model.GameState
import com.wordmatch.ui.theme.SunnyOrange
import com.wordmatch.ui.theme.WarmGray

@Composable
fun SummaryScreen(state: GameState, viewModel: GameViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(Ui.SUMMARY_TITLE, fontSize = GameConfig.FONT_TITLE_SP.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        if (state.newRecord) {
            Spacer(Modifier.height(12.dp))
            Text(Ui.NEW_RECORD, fontSize = GameConfig.FONT_FEEDBACK_SP.sp, fontWeight = FontWeight.Bold, color = SunnyOrange)
        }

        Spacer(Modifier.height(24.dp))
        Text("${state.score}", fontSize = GameConfig.FONT_SUMMARY_SCORE_SP.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        Text(Ui.POINTS_WORD, fontSize = GameConfig.FONT_SUMMARY_STAT_SP.sp, color = WarmGray)

        Spacer(Modifier.height(20.dp))
        // "X מתוך N נכון"
        Text(
            "${state.correctCount}${Ui.OF}${state.sessionSize}${Ui.CORRECT_WORD}",
            fontSize = GameConfig.FONT_SUMMARY_STAT_SP.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${Ui.SESSION_BEST_STREAK}${state.bestStreakThisSession} 🔥",
            fontSize = GameConfig.FONT_SUMMARY_STAT_SP.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(Ui.scoreToBeat(state.bestScore), fontSize = GameConfig.FONT_RECORD_SP.sp, color = WarmGray)

        Spacer(Modifier.height(32.dp))
        Button(onClick = viewModel::playAgain, modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Text(Ui.PLAY_AGAIN, fontSize = GameConfig.FONT_BUTTON_SP.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = viewModel::backToStart, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(Ui.TO_MENU, fontSize = GameConfig.FONT_BUTTON_SP.sp)
        }
    }
}
