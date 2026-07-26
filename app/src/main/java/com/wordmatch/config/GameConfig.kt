package com.wordmatch.config

/**
 * Single source of truth for every tunable value. Adjust gameplay/typography here, not in code.
 * Direction is fixed for Phase 1: show Hebrew word, learner types the English translation.
 */
object GameConfig {

    /** Points added per correct answer. */
    const val POINTS_PER_CORRECT = 10

    /** Delay before auto-advancing to the next word after a CORRECT answer (ms). Lets celebration show. */
    const val AUTO_ADVANCE_CORRECT_MS = 1500L

    /** Delay before auto-advancing after a FORFEIT once the answer is acknowledged (ms). */
    const val AUTO_ADVANCE_FORFEIT_MS = 2000L

    /**
     * Leading words stripped from BOTH sides before comparing an English answer, so a child may
     * type "cat" for "A cat" or "run" for "To run". Only the first token is stripped; internal
     * articles (e.g. "Oren has a cat") are preserved.
     */
    val LEADING_ARTICLES = setOf("a", "an", "the", "to")

    /** JSONBin.io base URL. The bin id (BuildConfig.JSONBIN_BIN_ID) is appended by Retrofit. */
    const val JSONBIN_BASE_URL = "https://api.jsonbin.io/"

    // ---- Font sizes (sp). Project rule: no text size is hardcoded in UI code. ----

    /** Displayed vocabulary word — large and centered. */
    const val FONT_WORD_CARD_SP = 56

    /** Instruction line ("translate to English"). */
    const val FONT_INSTRUCTION_SP = 20

    /** Language-direction indicator ("Hebrew -> English"). */
    const val FONT_LANG_HINT_SP = 16

    /** Text typed into the answer field. */
    const val FONT_INPUT_SP = 24

    /** Action button labels (Check / Forfeit / Got it). */
    const val FONT_BUTTON_SP = 18

    /** Header stats (streak, points, progress). */
    const val FONT_STATS_SP = 18

    /** Correct / wrong / reveal feedback message. */
    const val FONT_FEEDBACK_SP = 22
}
