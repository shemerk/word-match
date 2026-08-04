package com.wordmatch.model

import androidx.annotation.DrawableRes
import com.wordmatch.R

/**
 * One collectible soccer-player card. [art] is a cut-out cell from the source grid images; the
 * player's name plate is baked into the artwork, so there is no separate name field to maintain.
 */
data class Card(val id: Int, @DrawableRes val art: Int)

/**
 * The whole deck — single source of truth for how many cards exist and their art.
 * Ids are 1-based and line up with the drawable names: card_01 = Man United #1 … card_52 = Racing.
 * To grow the deck: cut more card_NN.png into res/drawable and add them to [CARD_ART] in order.
 */
object Deck {
    val ALL: List<Card> = CARD_ART.mapIndexed { i, res -> Card(i + 1, res) }

    /** Number of distinct cards in the collection (the "Y" in "X / Y collected"). */
    val SIZE: Int get() = ALL.size

    /** Drawable for a card id, clamped so a stale/out-of-range id never crashes the reveal. */
    @DrawableRes fun art(id: Int): Int = ALL[(id - 1).coerceIn(0, ALL.lastIndex)].art
}

// card_01..card_26 = Manchester United grid, card_27..card_52 = Racing grid. Order = grid order.
private val CARD_ART = listOf(
    R.drawable.card_01, R.drawable.card_02, R.drawable.card_03, R.drawable.card_04,
    R.drawable.card_05, R.drawable.card_06, R.drawable.card_07, R.drawable.card_08,
    R.drawable.card_09, R.drawable.card_10, R.drawable.card_11, R.drawable.card_12,
    R.drawable.card_13, R.drawable.card_14, R.drawable.card_15, R.drawable.card_16,
    R.drawable.card_17, R.drawable.card_18, R.drawable.card_19, R.drawable.card_20,
    R.drawable.card_21, R.drawable.card_22, R.drawable.card_23, R.drawable.card_24,
    R.drawable.card_25, R.drawable.card_26, R.drawable.card_27, R.drawable.card_28,
    R.drawable.card_29, R.drawable.card_30, R.drawable.card_31, R.drawable.card_32,
    R.drawable.card_33, R.drawable.card_34, R.drawable.card_35, R.drawable.card_36,
    R.drawable.card_37, R.drawable.card_38, R.drawable.card_39, R.drawable.card_40,
    R.drawable.card_41, R.drawable.card_42, R.drawable.card_43, R.drawable.card_44,
    R.drawable.card_45, R.drawable.card_46, R.drawable.card_47, R.drawable.card_48,
    R.drawable.card_49, R.drawable.card_50, R.drawable.card_51, R.drawable.card_52
)
