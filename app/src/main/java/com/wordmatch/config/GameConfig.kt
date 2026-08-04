package com.wordmatch.config

/**
 * Single source of truth for every tunable value. Adjust gameplay/typography here, not in code.
 * Direction is fixed: show the Hebrew word, learner types the English translation.
 */
object GameConfig {

    /** Base points added per correct answer (before any streak bonus). */
    const val POINTS_PER_CORRECT = 10

    /** Points deducted from the base reward per hint tap on the current word (streak bonus is not
     *  penalized). e.g. base 10, 2 hints -> 8 + streak bonus. */
    const val HINT_PENALTY = 1

    /** Floor for the per-word base reward after the hint penalty, so a fully-hinted win still scores
     *  something. Applies before the streak bonus is added. Must be >= 0. */
    const val MIN_POINTS_PER_CORRECT = 1

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

    /** How many times a wrongly-answered word is entered into the draw pool while unsolved, making
     *  it more likely to reappear until the child gets it right (in-memory, per session). 1 = no
     *  weighting; 3 = a missed word is ~3x as likely as an unmissed one. */
    const val MISSED_WORD_WEIGHT = 3

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

    // ---- Card collection (win a random soccer-player card every POINTS_PER_CARD points) ----

    /** Lifetime points that unlock one new card. Every time the running total crosses a multiple of
     *  this, a random not-yet-owned card is awarded. ~5-10 correct answers, i.e. roughly one short
     *  session per card — frequent enough to stay exciting for a kid. */
    const val POINTS_PER_CARD = 100

    /** Columns in the album grid. 3 keeps each card large enough to read the baked-in name plate. */
    const val ALBUM_COLUMNS = 3

    /** Width/height ratio of a card, matching the source grid cell (176x192). Keeps cards un-stretched. */
    const val CARD_ASPECT = 176f / 192f

    /** Corner radius for card frames in the album and the reveal (dp). */
    const val CARD_CORNER_DP = 12

    /** Card-win reveal: flip-in duration (ms) and the enlarged card size (dp). */
    const val CARD_REVEAL_FLIP_MS = 500
    const val CARD_REVEAL_SIZE_DP = 220

    /** Full-card zoom (tapping an owned card in the album): size (dp). */
    const val CARD_ZOOM_SIZE_DP = 300

    // ---- Card / album font sizes (sp) ----

    /** Album screen title. */
    const val FONT_ALBUM_TITLE_SP = 24

    /** "X / Y" collected count + the "N points to next card" teaser. */
    const val FONT_ALBUM_PROGRESS_SP = 18

    /** Start-screen album button label. */
    const val FONT_ALBUM_BUTTON_SP = 20

    /** "כרטיס חדש!" title on the card-win reveal. */
    const val FONT_CARD_REVEAL_TITLE_SP = 26

    /** The "?" glyph shown on a not-yet-owned (locked) album slot. */
    const val FONT_LOCKED_CARD_SP = 40
}
