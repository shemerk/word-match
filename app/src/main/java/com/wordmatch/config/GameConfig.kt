package com.wordmatch.config

/**
 * Single source of truth for every tunable value. Adjust gameplay/typography here, not in code.
 * Direction is fixed: show the Hebrew word, learner types the English translation.
 */
object GameConfig {

    /** Base points added per correct answer (before any streak bonus). */
    const val POINTS_PER_CORRECT = 10

    /** Bonus points per streak step already built up when answering (rewards consecutive correct). */
    const val STREAK_BONUS_PER_STEP = 2

    /** Streak bonus is capped at this many steps so it stays a nudge, not a runaway multiplier.
     *  Max bonus = STREAK_BONUS_PER_STEP * STREAK_BONUS_CAP_STEPS (here +10, i.e. up to 2x base). */
    const val STREAK_BONUS_CAP_STEPS = 5

    /** Delay before auto-advancing to the next word after a CORRECT answer (ms). Lets celebration show. */
    const val AUTO_ADVANCE_CORRECT_MS = 1500L

    /** Session length options the player may pick on the start screen (number of words). */
    val SESSION_SIZES = listOf(10, 20, 50, 100)

    /** Session length selected by default. Must be one of [SESSION_SIZES]. */
    const val DEFAULT_SESSION_SIZE = 10

    /** Category chip shown for "all themes"; also the internal meaning of a null category. */
    const val CATEGORY_ALL = "all"

    /**
     * Leading words stripped from BOTH sides before comparing an English answer, so a child may
     * type "cat" for "A cat" or "run" for "To run". Only the first token is stripped; internal
     * articles (e.g. "Oren has a cat") are preserved.
     */
    val LEADING_ARTICLES = setOf("a", "an", "the", "to")

    /** Bonus points for a correct answer, given the streak already built up BEFORE this answer.
     *  Grows +2 per prior correct, capped at +10. e.g. streaks 0,1,2,5,9 -> bonus 0,2,4,10,10. */
    fun streakBonus(priorStreak: Int): Int =
        minOf(priorStreak, STREAK_BONUS_CAP_STEPS) * STREAK_BONUS_PER_STEP

    /** JSONBin.io base URL. The bin id (BuildConfig.JSONBIN_BIN_ID) is appended by Retrofit. */
    const val JSONBIN_BASE_URL = "https://api.jsonbin.io/"

    // ---- Animation (Chunk 2/3) ----

    /** Confetti pieces on a correct answer. Kept modest so it feels celebratory, not chaotic. */
    const val CONFETTI_PIECES = 26

    /** Confetti burst duration (ms). */
    const val CONFETTI_DURATION_MS = 900

    /** Wrong-answer input shake: total duration (ms) and horizontal travel (dp). */
    const val SHAKE_DURATION_MS = 300
    const val SHAKE_TRAVEL_DP = 12

    /** Forfeit card-flip duration (ms). */
    const val FLIP_DURATION_MS = 450

    /** Progress-bar fill animation duration (ms). */
    const val PROGRESS_ANIM_MS = 400

    /** Streak flame pulse scale on each increment (1.0 = no pulse). */
    const val FLAME_PULSE_SCALE = 1.5f

    /** Letters revealed from the start of the answer when the hint button is tapped. */
    const val HINT_REVEAL_LETTERS = 1

    // ---- Font sizes (sp). Project rule: no text size is hardcoded in UI code. ----

    /** App / start-screen title. */
    const val FONT_TITLE_SP = 34

    /** Displayed vocabulary word — large and centered. */
    const val FONT_WORD_CARD_SP = 56

    /** Line height for the word card; must exceed FONT_WORD_CARD_SP so wrapped multi-word entries
     *  (e.g. "לאורן יש חתול") don't overlap. ~1.25x the font size. */
    const val FONT_WORD_CARD_LINE_HEIGHT_SP = 70

    /** Instruction line ("translate to English"). */
    const val FONT_INSTRUCTION_SP = 20

    /** Language-direction indicator ("Hebrew -> English"). */
    const val FONT_LANG_HINT_SP = 16

    /** Text typed into the answer field. */
    const val FONT_INPUT_SP = 24

    /** Action button labels (Check / Forfeit / Got it / Start / Play again). */
    const val FONT_BUTTON_SP = 18

    /** Selectable chips (category, session size). */
    const val FONT_CHIP_SP = 16

    /** Header stats (streak, points, progress). */
    const val FONT_STATS_SP = 18

    /** Correct / wrong / reveal feedback message. */
    const val FONT_FEEDBACK_SP = 22

    /** Summary-screen big score number. */
    const val FONT_SUMMARY_SCORE_SP = 48

    /** Summary-screen secondary stat lines. */
    const val FONT_SUMMARY_STAT_SP = 20

    /** "Best to beat" / record labels. */
    const val FONT_RECORD_SP = 16
}
