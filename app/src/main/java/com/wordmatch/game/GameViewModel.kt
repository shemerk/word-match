package com.wordmatch.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordmatch.config.Child
import com.wordmatch.config.Direction
import com.wordmatch.config.GameConfig
import com.wordmatch.data.InMemoryScoreStore
import com.wordmatch.data.ScoreStore
import com.wordmatch.data.WordRepository
import com.wordmatch.model.Deck
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
import kotlin.random.Random

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
    },
    // Which unowned card to award when a points threshold is crossed. Injectable for deterministic tests.
    private val pickCard: (unowned: List<Int>) -> Int = { it.random() }
) : ViewModel() {

    // Active child: dictionary + progress namespace. Restored from the store's saved choice.
    private var child: Child = GameConfig.CHILDREN.firstOrNull { it.id == store.activeChildId() }
        ?: GameConfig.CHILDREN.first()

    private val _state = MutableStateFlow(
        GameState(soundEnabled = store.soundEnabled(), activeChildId = child.id, direction = store.direction())
    )
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var allWords: List<WordItem> = emptyList()
    private var newestBatch: Int = 0                  // highest batch present; > 0 means "new words" exist
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
            allWords = runCatching { repository.loadWords(child.binId) }.getOrDefault(emptyList())
            if (allWords.isEmpty()) {
                _state.value = _state.value.copy(loading = false, error = true)
            } else {
                val categories = allWords.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
                newestBatch = allWords.maxOf { it.batch }
                val hasNew = newestBatch > 0
                _state.value = _state.value.copy(
                    loading = false, error = false, categories = categories, totalWords = allWords.size,
                    category = null,   // reset — a stale category may not exist in the new dictionary
                    hasNewBatch = hasNew,
                    newBatchCount = allWords.count { it.batch == newestBatch },
                    newOnly = hasNew   // default to the newest batch when one exists
                )
                refreshRecords()
                refreshCards()
            }
        }
    }

    // ---- Start screen ----

    /** Switch the active child: swaps dictionary + progress namespace, then reloads its words. */
    fun setChild(id: String) {
        if (id == child.id) return
        child = GameConfig.CHILDREN.firstOrNull { it.id == id } ?: return
        store.setActiveChild(child.id)
        _state.value = _state.value.copy(loading = true, activeChildId = child.id)
        load()
    }

    fun setCategory(category: String?) {
        _state.value = _state.value.copy(category = category)
    }

    /** Toggle between the newest batch ("new words") and the whole bank. */
    fun setNewOnly(newOnly: Boolean) {
        _state.value = _state.value.copy(newOnly = newOnly)
    }

    fun setSessionSize(size: Int) {
        _state.value = _state.value.copy(sessionSize = size)
        refreshRecords()
    }

    /** Set the translation direction (persisted globally). Takes effect on the next session. */
    fun setDirection(d: Direction) {
        store.setDirection(d)
        _state.value = _state.value.copy(direction = d)
    }

    /** Resolve whether the prompt for the next word is shown in Hebrew. MIX re-rolls per word. */
    private fun promptIsHebrew(): Boolean = when (_state.value.direction) {
        Direction.HE_TO_EN -> true
        Direction.EN_TO_HE -> false
        Direction.MIX -> Random.nextBoolean()
    }

    private fun refreshRecords() {
        val size = _state.value.sessionSize
        _state.value = _state.value.copy(bestScore = store.bestScore(size), bestStreak = store.bestStreak(size))
    }

    /** Sync card fields from the store, first awarding any card the current point total has earned
     *  but not yet handed out (covers app start / an interrupted award). Awards here are silent. */
    private fun refreshCards() {
        reconcileCards()
        _state.value = _state.value.copy(
            totalPoints = store.totalPoints(),
            ownedCardIds = store.ownedCardIds()
        )
    }

    /** Award random unowned cards until the owned count matches points/POINTS_PER_CARD (capped at the
     *  deck size). Returns the ids awarded this call — in play that is 0 or 1 per correct answer. */
    private fun reconcileCards(): List<Int> {
        val deserved = minOf(store.totalPoints() / GameConfig.POINTS_PER_CARD, Deck.SIZE)
        val awarded = mutableListOf<Int>()
        var owned = store.ownedCardIds()
        while (owned.size < deserved) {
            val unowned = Deck.ALL.map { it.id }.filter { it !in owned }
            if (unowned.isEmpty()) break
            val pick = pickCard(unowned)
            store.unlockCard(pick)
            owned = owned + pick
            awarded += pick
        }
        return awarded
    }

    /** Dismiss the card-win reveal. */
    fun acknowledgeCard() {
        _state.value = _state.value.copy(newCardId = null)
    }

    fun openAlbum() {
        _state.value = _state.value.copy(screen = Screen.ALBUM)
    }

    fun closeAlbum() = backToStart()

    fun startSession() {
        val s = _state.value
        pool = when {
            s.newOnly && newestBatch > 0 -> allWords.filter { it.batch == newestBatch }
            s.category == null -> allWords
            else -> allWords.filter { it.category == s.category }
        }
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
            currentWord = pickNext(weightedPool(), null).also { lastWordId = it.id },
            promptIsHebrew = promptIsHebrew()
        )
    }

    // ---- In-session ----

    fun checkAnswer(userAnswer: String, hintsUsed: Int = 0) {
        val s = _state.value
        val word = s.currentWord ?: return
        if (!s.awaitingAnswer && s.isAnswerCorrect != false) return // ignore after solve/forfeit
        if (s.isAnswerCorrect == true) return

        // Grade against whichever language the child was asked to type.
        val target = if (s.promptIsHebrew) word.english else word.hebrew
        if (AnswerVerifier.isCorrect(userAnswer, target, hebrew = !s.promptIsHebrew)) {
            if (s.soundEnabled) sound.playCorrect()
            missedIds -= word.id
            val newStreak = s.streak + 1
            // Base points (minus a per-hint penalty, floored) + a capped bonus for the run already
            // going (see GameConfig.streakBonus). Hints discount the base only, never the bonus.
            val base = (GameConfig.POINTS_PER_CORRECT - hintsUsed * GameConfig.HINT_PENALTY)
                .coerceAtLeast(GameConfig.MIN_POINTS_PER_CORRECT)
            val gained = base + GameConfig.streakBonus(s.streak)

            // Lifetime points drive the card collection: add, then award a card if a 100-pt line crossed.
            store.addPoints(gained)
            val total = store.totalPoints()
            val wonCard = reconcileCards().lastOrNull()   // at most one per answer in practice
            if (wonCard != null && s.soundEnabled) sound.playLevelUp()

            _state.value = s.copy(
                isAnswerCorrect = true,
                streak = newStreak,
                bestStreakThisSession = maxOf(s.bestStreakThisSession, newStreak),
                score = s.score + gained,
                lastGained = gained,
                correctCount = s.correctCount + 1,
                checkNonce = s.checkNonce + 1,
                totalPoints = total,
                ownedCardIds = store.ownedCardIds(),
                newCardId = wonCard ?: s.newCardId,
                newCardNonce = if (wonCard != null) s.newCardNonce + 1 else s.newCardNonce
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
        refreshCards()
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
            forfeited = false,
            promptIsHebrew = promptIsHebrew()
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
        refreshCards()
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

    /** Wipe the collection: total earned points to 0 AND all cards removed (so points can't
     *  immediately re-award them). Kept separate from "reset scores". */
    fun resetCollection() {
        store.resetProgress()
        store.resetCards()
        refreshCards()
    }
}
