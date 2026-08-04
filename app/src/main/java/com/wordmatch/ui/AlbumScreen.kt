package com.wordmatch.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.wordmatch.config.GameConfig
import com.wordmatch.game.GameViewModel
import com.wordmatch.model.Card as CardModel
import com.wordmatch.model.Deck
import com.wordmatch.model.GameState
import com.wordmatch.ui.theme.WarmGray

private val cornerShape = RoundedCornerShape(GameConfig.CARD_CORNER_DP.dp)

/**
 * Start-screen entry point to the collection: shows the count, a progress bar, and how many points
 * are left until the next card. Replaces the old mascot trophy case.
 */
@Composable
fun AlbumButton(state: GameState, onClick: () -> Unit) {
    val owned = state.ownedCardIds.size
    val complete = owned >= Deck.SIZE
    val pointsToNext = GameConfig.POINTS_PER_CARD - state.totalPoints % GameConfig.POINTS_PER_CARD

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "🎴 ${Ui.ALBUM_BUTTON}",
                fontSize = GameConfig.FONT_ALBUM_BUTTON_SP.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                Ui.albumProgress(owned, Deck.SIZE),
                fontSize = GameConfig.FONT_ALBUM_PROGRESS_SP.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { owned.toFloat() / Deck.SIZE },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (complete) Ui.ALBUM_COMPLETE else Ui.pointsToNextCard(pointsToNext),
                fontSize = GameConfig.FONT_RECORD_SP.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/** Full-screen album: back bar + count + progress + the grid of every card (owned or locked). */
@Composable
fun AlbumScreen(state: GameState, viewModel: GameViewModel) {
    val owned = state.ownedCardIds.size
    var zoomCard by remember { mutableStateOf<CardModel?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Box(Modifier.fillMaxWidth()) {
            TextButton(onClick = viewModel::closeAlbum, modifier = Modifier.align(Alignment.CenterStart)) {
                Text(Ui.ALBUM_BACK, fontSize = GameConfig.FONT_BUTTON_SP.sp)
            }
            Text(
                Ui.ALBUM_TITLE,
                modifier = Modifier.align(Alignment.Center),
                fontSize = GameConfig.FONT_ALBUM_TITLE_SP.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            if (owned >= Deck.SIZE) Ui.ALBUM_COMPLETE else Ui.albumProgress(owned, Deck.SIZE),
            modifier = Modifier.fillMaxWidth(),
            fontSize = GameConfig.FONT_ALBUM_PROGRESS_SP.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { owned.toFloat() / Deck.SIZE },
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )

        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(GameConfig.ALBUM_COLUMNS),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(Deck.ALL, key = { it.id }) { card ->
                CardCell(card, owned = card.id in state.ownedCardIds, onClick = { zoomCard = card })
            }
        }
    }

    zoomCard?.let { CardZoomDialog(it, onDismiss = { zoomCard = null }) }
}

/** One album slot: the real card if owned, else a dimmed "?" mystery frame. Only owned cards zoom. */
@Composable
private fun CardCell(card: CardModel, owned: Boolean, onClick: () -> Unit) {
    val base = Modifier
        .fillMaxWidth()
        .aspectRatio(GameConfig.CARD_ASPECT)
        .clip(cornerShape)
    if (owned) {
        Image(
            painter = painterResource(card.art),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = base
                .border(2.dp, MaterialTheme.colorScheme.primary, cornerShape)
                .clickable(onClick = onClick)
        )
    } else {
        Box(
            base.background(WarmGray.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(Ui.CARD_LOCKED, fontSize = GameConfig.FONT_LOCKED_CARD_SP.sp, fontWeight = FontWeight.Bold, color = WarmGray)
        }
    }
}

/** Tap an owned card -> see it enlarged. */
@Composable
private fun CardZoomDialog(card: CardModel, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Image(
            painter = painterResource(card.art),
            contentDescription = null,
            modifier = Modifier
                .size(GameConfig.CARD_ZOOM_SIZE_DP.dp)
                .clip(cornerShape)
                .border(3.dp, MaterialTheme.colorScheme.primary, cornerShape)
                .clickable(onClick = onDismiss)
        )
    }
}

/**
 * The win moment: a card flips in over the game screen with "כרטיס חדש!" when a 100-pt line is
 * crossed. [nonce] re-fires the flip so two wins in a row still animate. Flip is skipped in reduced motion.
 */
@Composable
fun CardRevealDialog(cardId: Int, nonce: Int, reducedMotion: Boolean, onDismiss: () -> Unit) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(nonce) {
        if (reducedMotion) return@LaunchedEffect
        rotation.snapTo(-90f)
        rotation.animateTo(0f, tween(GameConfig.CARD_REVEAL_FLIP_MS))
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    Ui.CARD_WON_TITLE,
                    fontSize = GameConfig.FONT_CARD_REVEAL_TITLE_SP.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Image(
                    painter = painterResource(Deck.art(cardId)),
                    contentDescription = null,
                    modifier = Modifier
                        .size(GameConfig.CARD_REVEAL_SIZE_DP.dp)
                        .graphicsLayer { rotationY = rotation.value; cameraDistance = 12f * density }
                        .clip(cornerShape)
                        .border(3.dp, MaterialTheme.colorScheme.primary, cornerShape)
                )
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onDismiss) {
                    Text(Ui.GOT_IT, fontSize = GameConfig.FONT_BUTTON_SP.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
