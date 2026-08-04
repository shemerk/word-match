package com.wordmatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordmatch.config.GameConfig
import com.wordmatch.game.GameViewModel
import com.wordmatch.model.Screen

@Composable
fun MainScreen(viewModel: GameViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    // Force full RTL for the whole app.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
            when {
                state.loading -> Centered { CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text(Ui.LOADING) }
                state.error -> Centered { Text(Ui.ERROR, fontSize = GameConfig.FONT_FEEDBACK_SP.sp) }
                state.screen == Screen.START -> StartScreen(state, viewModel, onSettings = { showSettings = true })
                state.screen == Screen.PLAYING -> GameScreen(state, viewModel)
                state.screen == Screen.SUMMARY -> SummaryScreen(state, viewModel)
                state.screen == Screen.ALBUM -> AlbumScreen(state, viewModel)
            }
        }
        if (showSettings) {
            SettingsDialog(
                soundOn = state.soundEnabled,
                onToggleSound = viewModel::toggleSound,
                onReset = viewModel::resetScores,
                onResetCollection = viewModel::resetCollection,
                onClose = { showSettings = false }
            )
        }
        // Card-win reveal — global so it fires on any screen (incl. summary, when the last word wins a card).
        val newCard = state.newCardId
        if (newCard != null) {
            CardRevealDialog(newCard, state.newCardNonce, rememberReducedMotion(), onDismiss = viewModel::acknowledgeCard)
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = { content() }
    )
}

@Composable
private fun SettingsDialog(
    soundOn: Boolean,
    onToggleSound: () -> Unit,
    onReset: () -> Unit,
    onResetCollection: () -> Unit,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text(Ui.CLOSE) } },
        title = { Text(Ui.SETTINGS_TITLE) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Ui.SOUND, fontSize = GameConfig.FONT_STATS_SP.sp)
                    Switch(checked = soundOn, onCheckedChange = { onToggleSound() })
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                    Text(Ui.RESET_SCORES)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onResetCollection, modifier = Modifier.fillMaxWidth()) {
                    Text(Ui.RESET_COLLECTION)
                }
            }
        }
    )
}
