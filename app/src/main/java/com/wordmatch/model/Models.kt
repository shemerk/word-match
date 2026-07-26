package com.wordmatch.model

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

enum class GameStatus { LOADING, READY, RESULT, FORFEIT, ERROR }

data class GameState(
    val currentWord: WordItem? = null,
    val streak: Int = 0,
    val score: Int = 0,
    val wordsCompleted: Int = 0,
    // null = no attempt yet, true/false = last check result. Wrong keeps input open for retry.
    val isAnswerCorrect: Boolean? = null,
    val showAnswer: Boolean = false,
    val status: GameStatus = GameStatus.LOADING
)
