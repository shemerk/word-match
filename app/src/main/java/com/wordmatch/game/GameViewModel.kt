package com.wordmatch.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordmatch.config.GameConfig
import com.wordmatch.data.WordRepository
import com.wordmatch.model.GameState
import com.wordmatch.model.GameStatus
import com.wordmatch.model.WordItem
import com.wordmatch.util.NoOpSoundManager
import com.wordmatch.util.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Game loop: load words -> show Hebrew word -> check English answer or forfeit -> advance.
 *
 * [pickNext] is injectable so tests can force a deterministic order; production picks a random
 * word that isn't the one just shown.
 */
class GameViewModel(
    private val repository: WordRepository,
    private val sound: SoundManager = NoOpSoundManager(),
    private val pickNext: (words: List<WordItem>, lastId: Int?) -> WordItem = { words, lastId ->
        if (words.size == 1) words[0] else words.filter { it.id != lastId }.random()
    }
) : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var words: List<WordItem> = emptyList()
    private var lastWordId: Int? = null

    init { load() }

    private fun load() {
        viewModelScope.launch {
            words = runCatching { repository.loadWords() }.getOrDefault(emptyList())
            if (words.isEmpty()) {
                _state.value = _state.value.copy(status = GameStatus.ERROR)
            } else {
                nextWord()
            }
        }
    }

    fun checkAnswer(userAnswer: String) {
        val s = _state.value
        val word = s.currentWord ?: return
        // Ignore extra taps once solved or while revealing a forfeited answer; wrong answers may retry.
        if (s.isAnswerCorrect == true || s.status == GameStatus.FORFEIT) return

        if (AnswerVerifier.isCorrect(userAnswer, word.english)) {
            sound.playCorrect()
            _state.value = s.copy(
                isAnswerCorrect = true,
                streak = s.streak + 1,
                score = s.score + GameConfig.POINTS_PER_CORRECT,
                status = GameStatus.RESULT
            )
            viewModelScope.launch {
                delay(GameConfig.AUTO_ADVANCE_CORRECT_MS)
                nextWord()
            }
        } else {
            sound.playWrong()
            _state.value = s.copy(
                isAnswerCorrect = false,
                streak = 0,
                status = GameStatus.RESULT
            )
        }
    }

    fun forfeit() {
        val s = _state.value
        if (s.currentWord == null || s.isAnswerCorrect == true) return
        _state.value = s.copy(showAnswer = true, status = GameStatus.FORFEIT)
    }

    /** Called by the "Got it!" button after a forfeit reveal. */
    fun acknowledgeForfeit() = nextWord()

    fun nextWord() {
        if (words.isEmpty()) return
        val s = _state.value
        val next = pickNext(words, lastWordId)
        lastWordId = next.id
        _state.value = s.copy(
            currentWord = next,
            isAnswerCorrect = null,
            showAnswer = false,
            // Count only words the learner has finished, not the very first one being shown.
            wordsCompleted = if (s.currentWord != null) s.wordsCompleted + 1 else s.wordsCompleted,
            status = GameStatus.READY
        )
    }
}
