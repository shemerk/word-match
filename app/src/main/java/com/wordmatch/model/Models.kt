package com.wordmatch.model

import com.wordmatch.config.GameConfig

/** One vocabulary pair. Defaults let Gson tolerate missing fields. */
data class WordItem(
    val id: Int = 0,
    val english: String = "",
    val hebrew: String = "",
    val category: String = ""
)

/** JSONBin.io (and our bundled asset) wrap the list in a "record" object. */
data class JsonBinResponse(
    val record: List<WordItem> = emptyList()
)

/** Which top-level screen is showing. */
enum class Screen { START, PLAYING, SUMMARY, ALBUM }

data class GameState(
    val screen: Screen = Screen.START,
    val loading: Boolean = true,   // words still loading at app start
    val error: Boolean = false,    // load failed with no words at all

    // Start-screen selection
    val totalWords: Int = 0,                       // size of the whole loaded word bank
    val categories: List<String> = emptyList(),   // available themes (excludes "all")
    val category: String? = null,                 // null = all themes
    val sessionSize: Int = GameConfig.DEFAULT_SESSION_SIZE,

    // In-session play
    val currentWord: WordItem? = null,
    val streak: Int = 0,
    val score: Int = 0,
    val lastGained: Int = 0,       // points from the most recent correct answer (base + streak bonus)
    val wordsCompleted: Int = 0,   // finished words this session (0..sessionSize)
    val correctCount: Int = 0,     // for accuracy on the summary
    val bestStreakThisSession: Int = 0,
    // null = no attempt yet, true/false = last check. Wrong keeps input open for retry.
    val isAnswerCorrect: Boolean? = null,
    val forfeited: Boolean = false,
    // Bumped on every check so the UI can re-fire confetti/shake even on a repeated result.
    val checkNonce: Int = 0,

    // Records for the CURRENT sessionSize bucket (persisted separately per size)
    val bestScore: Int = 0,
    val bestStreak: Int = 0,
    val newRecord: Boolean = false,  // set on the summary when best score was beaten

    val soundEnabled: Boolean = true,

    // Card collection — recomputed from ScoreStore on START and after each correct answer.
    val totalPoints: Int = 0,               // lifetime points earned across all sessions (drives card awards)
    val ownedCardIds: Set<Int> = emptySet(), // ids of cards already won (see model.Deck)
    // Set to a freshly-won card id to trigger the reveal; cleared on acknowledge. Nonce re-fires the flip.
    val newCardId: Int? = null,
    val newCardNonce: Int = 0
) {
    /** True while the current word is awaiting a first answer (not yet solved/forfeited). */
    val awaitingAnswer: Boolean get() = isAnswerCorrect == null && !forfeited
}
