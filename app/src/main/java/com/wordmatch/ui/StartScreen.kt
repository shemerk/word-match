package com.wordmatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordmatch.config.GameConfig
import com.wordmatch.game.GameViewModel
import com.wordmatch.model.GameState
import com.wordmatch.ui.theme.WarmGray

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(state: GameState, viewModel: GameViewModel, onSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title row with a settings gear on the leading (right, in RTL) side.
        Box(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onSettings, modifier = Modifier.align(Alignment.CenterStart)) {
                Text(Ui.SETTINGS_GEAR, fontSize = 24.sp)
            }
            Text(
                Ui.TITLE,
                modifier = Modifier.align(Alignment.Center),
                fontSize = GameConfig.FONT_TITLE_SP.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(Ui.PICK, fontSize = GameConfig.FONT_INSTRUCTION_SP.sp, color = WarmGray)

        Spacer(Modifier.height(24.dp))
        SectionLabel(Ui.CATEGORY_LABEL)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectChip(Ui.CATEGORY_ALL, selected = state.category == null) { viewModel.setCategory(null) }
            state.categories.forEach { cat ->
                SelectChip(Ui.categoryLabel(cat), selected = state.category == cat) { viewModel.setCategory(cat) }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(Ui.SIZE_LABEL)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameConfig.SESSION_SIZES.forEach { size ->
                SelectChip(size.toString(), selected = state.sessionSize == size) { viewModel.setSessionSize(size) }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            Ui.scoreToBeat(state.bestScore),
            fontSize = GameConfig.FONT_RECORD_SP.sp,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = viewModel::startSession,
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Text(Ui.START, fontSize = GameConfig.FONT_BUTTON_SP.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        fontSize = GameConfig.FONT_STATS_SP.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = GameConfig.FONT_CHIP_SP.sp) },
        colors = FilterChipDefaults.filterChipColors()
    )
}
