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

    // ---- Mascot (soccer player that levels up on lifetime points) ----

    /** Points needed for the first level-up (level 1 -> 2). Tuned so the mascot is a LONG-term loop:
     *  a completed 100-word session earns ~1400-1970 pts, so the top art tier takes ~10 sessions, not one. */
    const val LEVEL_BASE_COST = 1000

    /** Each level costs this many times the previous one (2 = the cost doubles every level:
     *  1000, 2000, 4000, 8000…; cumulative thresholds 0, 1000, 3000, 7000, 15000, 31000…). Must be >= 2. */
    const val LEVEL_GROWTH = 2

    /** Number of distinct sprite/title tiers that have art. levelFor keeps climbing past this
     *  (extra levels show the top sprite + prestige stars), but the drawable/title clamps here.
     *  Add art + a title row and bump this to extend. */
    const val MASCOT_TIER_COUNT = 5

    /** Prestige stars shown next to the top rank are capped here so the title can't overflow. */
    const val MASCOT_MAX_STARS = 9

    /**
     * Team-colour choices for the jersey picker, as ARGB longs (Compose Color takes a Long).
     * This colours the nameplate / frame / progress bar — NOT the sprite (avoids tier×colour art).
     * Index is stored via ScoreStore.jerseyColor(). Order is the swatch order shown to the child.
     */
    val JERSEY_COLORS = listOf(
        0xFFE53935L, // red
        0xFF1E88E5L, // blue
        0xFF43A047L, // green
        0xFFFDD835L  // yellow
    )

    /** Big mascot sprite on the start-screen trophy case (dp, square). */
    const val MASCOT_BIG_DP = 160

    /** Compact mascot avatar in the game-screen header (dp, square). */
    const val MASCOT_COMPACT_DP = 40

    /** Thumbnail size of each tier in the start-screen "shelf" row (dp, square). */
    const val MASCOT_SHELF_DP = 44

    /** Scale the avatar bounces to on a correct answer / level-up (1.0 = no bounce). */
    const val MASCOT_BOUNCE_SCALE = 1.35f

    /** How long the "עלית רמה!" level-up banner stays up (ms). */
    const val LEVEL_UP_BANNER_MS = 1800L

    /** Rank title on the start-screen trophy case. */
    const val FONT_RANK_TITLE_SP = 26

    /** "עוד N נקודות ל…" next-unlock teaser under the mascot. */
    const val FONT_MASCOT_TEASER_SP = 16

    /** "עלית רמה!" level-up banner. */
    const val FONT_LEVEL_UP_SP = 28

    /** The child's player name shown above the mascot. */
    const val FONT_PLAYER_NAME_SP = 22

    /** Small level number badge on the compact header avatar. */
    const val FONT_LEVEL_BADGE_SP = 13

    /**
     * Level (1-based) for a given lifetime point total. Level 1 = [0, BASE), level 2 =
     * [BASE, BASE+BASE*GROWTH), … Uncapped: a huge total returns a large level (art clamps, not this).
     */
    fun levelFor(totalPoints: Int): Int {
        var level = 1
        var cost = LEVEL_BASE_COST
        var remaining = totalPoints
        while (remaining >= cost) {
            remaining -= cost
            cost *= LEVEL_GROWTH
            level++
        }
        return level
    }

    /** Points earned inside the current level, and the points that level needs, for a progress bar.
     *  e.g. total 45 -> (15 into this level, of 40 needed). */
    fun levelProgress(totalPoints: Int): Pair<Int, Int> {
        var cost = LEVEL_BASE_COST
        var remaining = totalPoints
        while (remaining >= cost) {
            remaining -= cost
            cost *= LEVEL_GROWTH
        }
        return remaining to cost
    }

    /** Points still needed to reach the next level from a given total. */
    fun pointsToNextLevel(totalPoints: Int): Int {
        val (into, need) = levelProgress(totalPoints)
        return need - into
    }
}
