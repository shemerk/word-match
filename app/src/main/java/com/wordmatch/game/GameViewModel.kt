package com.wordmatch.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordmatch.config.GameConfig
import com.wordmatch.data.InMemoryScoreStore
import com.wordmatch.data.ScoreStore
import com.wordmatch.data.WordRepository
import com.wordmatch.model.GameState
import com.wordmatch.model.Screen
import com.wordmatch.model.WordItem
import com.wordmatch.util.NoOpSoundManager
import com.wordmatch.util.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the whole flow: START (pick theme + size) -> PLAYING (finite session) -> SUMMARY.
 * Shows the Hebrew word, checks the English answer, tracks streak/score, and persists the best
 * score/streak to beat per session-size bucket.
 *
 * [pickNext] is injectable so tests can force a deterministic order.
 */
class GameViewModel(
    private val repository: WordRepository,
    private val sound: SoundManager = NoOpSoundManager(),
    private val store: ScoreStore = InMemoryScoreStore(),
    private val pickNext: (words: List<WordItem>, lastId: Int?) -> WordItem = { words, lastId ->
        if (words.size == 1) words[0] else words.filter { it.id != lastId }.random()
    }
) : ViewModel() {

    private val _state = MutableStateFlow(GameState(soundEnabled = store.soundEnabled()))
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var allWords: List<WordItem> = emptyList()
    private var pool: List<WordItem> = emptyList()   // words for the active session (filtered)
    private var lastWordId: Int? = null
    // Words answered wrong this session; drawn at higher probability until solved (in-memory only).
    private val missedIds = mutableSetOf<Int>()

    /** Draw pool with missed words duplicated so they reappear sooner (see MISSED_WORD_WEIGHT). */
    private fun weightedPool(): List<WordItem> =
        pool.flatMap { w -> if (w.id in missedIds) List(GameConfig.MISSED_WORD_WEIGHT) { w } else listOf(w) }

    init { load() }

    private fun load() {
        viewModelScope.launch {
            allWords = runCatching { repository.loadWords() }.getOrDefault(emptyList())
            if (allWords.isEmpty()) {
                _state.value = _state.value.copy(loading = false, error = true)
            } else {
                val categories = allWords.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
                _state.value = _state.value.copy(loading = false, categories = categories, totalWords = allWords.size)
                refreshRecords()
                refreshMascot()
            }
        }
    }

    // ---- Start screen ----

    fun setCategory(category: String?) {
        _state.value = _state.value.copy(category = category)
    }

    fun setSessionSize(size: Int) {
        _state.value = _state.value.copy(sessionSize = size)
        refreshRecords()
    }

    private fun refreshRecords() {
        val size = _state.value.sessionSize
        _state.value = _state.value.copy(bestScore = store.bestScore(size), bestStreak = store.bestStreak(size))
    }

    /** Recompute mascot fields from the store (lifetime points may have changed while playing). */
    private fun refreshMascot() {
        val total = store.totalPoints()
        val (into, need) = GameConfig.levelProgress(total)
        _state.value = _state.value.copy(
            totalPoints = total,
            level = GameConfig.levelFor(total),
            levelInto = into,
            levelNeed = need,
            playerName = store.playerName(),
            jerseyColor = store.jerseyColor()
        )
    }

    fun setPlayerName(name: String) {
        store.setPlayerName(name)
        _state.value = _state.value.copy(playerName = name)
    }

    fun setJerseyColor(index: Int) {
        store.setJerseyColor(index)
        _state.value = _state.value.copy(jerseyColor = index)
    }

    fun startSession() {
        val s = _state.value
        pool = if (s.category == null) allWords else allWords.filter { it.category == s.category }
        if (pool.isEmpty()) return
        lastWordId = null
        missedIds.clear()
        _state.value = s.copy(
            screen = Screen.PLAYING,
            score = 0,
            streak = 0,
            wordsCompleted = 0,
            correctCount = 0,
            bestStreakThisSession = 0,
            isAnswerCorrect = null,
            forfeited = false,
            newRecord = false,
            currentWord = pickNext(weightedPool(), null).also { lastWordId = it.id }
        )
    }

    // ---- In-session ----

    fun checkAnswer(userAnswer: String) {
        val s = _state.value
        val word = s.currentWord ?: return
        if (!s.awaitingAnswer && s.isAnswerCorrect != false) return // ignore after solve/forfeit
        if (s.isAnswerCorrect == true) return

        if (AnswerVerifier.isCorrect(userAnswer, word.english)) {
            if (s.soundEnabled) sound.playCorrect()
            missedIds -= word.id
            val newStreak = s.streak + 1
            // Base points + a capped bonus for the run already going (see GameConfig.streakBonus).
            val gained = GameConfig.POINTS_PER_CORRECT + GameConfig.streakBonus(s.streak)

            // Lifetime mascot points: add, then detect a level-up by comparing before/after.
            store.addPoints(gained)
            val total = store.totalPoints()
            val newLevel = GameConfig.levelFor(total)
            val leveledUp = newLevel > s.level
            val (into, need) = GameConfig.levelProgress(total)
            if (leveledUp && s.soundEnabled) sound.playLevelUp()

            _state.value = s.copy(
                isAnswerCorrect = true,
                streak = newStreak,
                bestStreakThisSession = maxOf(s.bestStreakThisSession, newStreak),
                score = s.score + gained,
                lastGained = gained,
                correctCount = s.correctCount + 1,
                checkNonce = s.checkNonce + 1,
                totalPoints = total,
                level = newLevel,
                levelInto = into,
                levelNeed = need,
                levelUpNonce = if (leveledUp) s.levelUpNonce + 1 else s.levelUpNonce
            )
            viewModelScope.launch {
                delay(GameConfig.AUTO_ADVANCE_CORRECT_MS)
                advance()
            }
        } else {
            if (s.soundEnabled) sound.playWrong()
            missedIds += word.id
            _state.value = s.copy(isAnswerCorrect = false, streak = 0, checkNonce = s.checkNonce + 1)
        }
    }

    fun forfeit() {
        val s = _state.value
        if (s.currentWord == null || s.isAnswerCorrect == true || s.forfeited) return
        _state.value = s.copy(forfeited = true)
    }

    /** Abandon the current session and return to the start screen. Nothing is recorded (incomplete). */
    fun exitGame() {
        _state.value = _state.value.copy(screen = Screen.START)
        refreshRecords()
        refreshMascot()
    }

    /** "Got it!" after a forfeit reveal. */
    fun acknowledgeForfeit() = advance()

    private fun advance() {
        val s = _state.value
        val completed = s.wordsCompleted + 1
        if (completed >= s.sessionSize) {
            endSession(completed)
            return
        }
        val next = pickNext(weightedPool(), lastWordId)
        lastWordId = next.id
        _state.value = s.copy(
            currentWord = next,
            wordsCompleted = completed,
            isAnswerCorrect = null,
            forfeited = false
        )
    }

    private fun endSession(completed: Int) {
        val s = _state.value
        val newRecord = store.saveIfBetter(s.sessionSize, s.score, s.bestStreakThisSession)
        _state.value = s.copy(
            screen = Screen.SUMMARY,
            wordsCompleted = completed,
            isAnswerCorrect = null,
            forfeited = false,
            newRecord = newRecord,
            bestScore = store.bestScore(s.sessionSize),
            bestStreak = store.bestStreak(s.sessionSize)
        )
    }

    // ---- Summary ----

    fun playAgain() = startSession()

    fun backToStart() {
        _state.value = _state.value.copy(screen = Screen.START)
        refreshRecords()
        refreshMascot()
    }

    // ---- Settings ----

    fun toggleSound() {
        val on = !_state.value.soundEnabled
        store.setSoundEnabled(on)
        _state.value = _state.value.copy(soundEnabled = on)
    }

    fun resetScores() {
        store.resetAll()
        refreshRecords()
    }

    /** Wipe lifetime mascot progress: level back to 1, total earned points to 0. */
    fun resetProgress() {
        store.resetProgress()
        refreshMascot()
    }
}
