package com.wordmatch

import com.wordmatch.data.WordRepository
import com.wordmatch.game.GameViewModel
import com.wordmatch.model.GameStatus
import com.wordmatch.model.WordItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private class FakeRepo(private val words: List<WordItem>) : WordRepository {
        override suspend fun loadWords(): List<WordItem> = words
    }

    private fun vmWith(vararg words: WordItem) = GameViewModel(FakeRepo(words.toList()))

    private val apple = WordItem(1, "Apple", "תפוח", "fruits")
    private val cat = WordItem(2, "A cat", "חתול", "animals")

    @Test fun loadsFirstWordThenReady() = runTest {
        val vm = vmWith(apple, cat)
        advanceUntilIdle()
        assertEquals(GameStatus.READY, vm.state.value.status)
        assertNotNull(vm.state.value.currentWord)
    }

    @Test fun emptyRepoGivesError() = runTest {
        val vm = vmWith()
        advanceUntilIdle()
        assertEquals(GameStatus.ERROR, vm.state.value.status)
    }

    @Test fun correctAnswerScoresAndStreaks() = runTest {
        val vm = vmWith(apple)
        advanceUntilIdle()
        vm.checkAnswer("apple") // pre-advance: delayed nextWord not yet run
        assertEquals(true, vm.state.value.isAnswerCorrect)
        assertEquals(10, vm.state.value.score)
        assertEquals(1, vm.state.value.streak)
    }

    @Test fun articleTolerantAnswerIsCorrect() = runTest {
        val vm = vmWith(cat)
        advanceUntilIdle()
        vm.checkAnswer("cat") // stored answer is "A cat"
        assertEquals(true, vm.state.value.isAnswerCorrect)
    }

    @Test fun wrongAnswerResetsStreakKeepsScoreAndCurrentWord() = runTest {
        val vm = vmWith(apple)
        advanceUntilIdle()
        vm.checkAnswer("banana")
        assertEquals(false, vm.state.value.isAnswerCorrect)
        assertEquals(0, vm.state.value.streak)
        assertEquals(0, vm.state.value.score)
        assertEquals(GameStatus.RESULT, vm.state.value.status)
        assertNotNull(vm.state.value.currentWord) // still on same word to retry
    }

    @Test fun forfeitRevealsWithoutPenalty() = runTest {
        val vm = vmWith(apple)
        advanceUntilIdle()
        vm.forfeit()
        assertEquals(true, vm.state.value.showAnswer)
        assertEquals(GameStatus.FORFEIT, vm.state.value.status)
        assertEquals(0, vm.state.value.score)
        assertEquals(0, vm.state.value.streak)
    }

    @Test fun autoAdvanceAfterCorrect() = runTest {
        val vm = vmWith(apple, cat)
        advanceUntilIdle()
        vm.checkAnswer("apple")
        advanceUntilIdle() // run the delayed nextWord()
        assertEquals(GameStatus.READY, vm.state.value.status)
        assertEquals(null, vm.state.value.isAnswerCorrect)
    }
}
