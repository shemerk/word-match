package com.wordmatch

import com.wordmatch.config.GameConfig
import com.wordmatch.data.InMemoryScoreStore
import com.wordmatch.data.WordRepository
import com.wordmatch.game.GameViewModel
import com.wordmatch.model.Screen
import com.wordmatch.model.WordItem
import com.wordmatch.util.SoundManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    private class CountingSound : SoundManager {
        var correct = 0; var wrong = 0; var levelUp = 0
        override fun playCorrect() { correct++ }
        override fun playWrong() { wrong++ }
        override fun playLevelUp() { levelUp++ }
    }

    // Deterministic order: next word after lastId in list order (wraps).
    private val sequential: (List<WordItem>, Int?) -> WordItem = { list, lastId ->
        val idx = list.indexOfFirst { it.id == lastId }
        list[(idx + 1) % list.size]
    }

    // Deterministic card award: always the lowest unowned id.
    private val firstCard: (List<Int>) -> Int = { it.min() }

    private val apple = WordItem(1, "Apple", "תפוח", "fruits")
    private val cat = WordItem(2, "A cat", "חתול", "animals")

    private fun vm(
        words: List<WordItem> = listOf(apple, cat),
        sound: SoundManager = CountingSound(),
        store: InMemoryScoreStore = InMemoryScoreStore()
    ) = GameViewModel(FakeRepo(words), sound, store, sequential, firstCard)

    @Test fun loadsToStartScreenWithCategories() = runTest {
        val vm = vm()
        advanceUntilIdle()
        val s = vm.state.value
        assertFalse(s.loading)
        assertEquals(Screen.START, s.screen)
        assertEquals(listOf("animals", "fruits"), s.categories)
    }

    @Test fun emptyRepoGivesError() = runTest {
        val vm = vm(words = emptyList())
        advanceUntilIdle()
        assertTrue(vm.state.value.error)
    }

    @Test fun startSessionShowsFirstWord() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.startSession()
        assertEquals(Screen.PLAYING, vm.state.value.screen)
        assertNotNull(vm.state.value.currentWord)
    }

    @Test fun categoryFilterLimitsPool() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.setCategory("fruits")
        vm.startSession()
        assertEquals("fruits", vm.state.value.currentWord?.category)
    }

    @Test fun correctAnswerScoresStreakAndCorrectCount() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.setSessionSize(10); vm.startSession()
        vm.checkAnswer("apple")
        val s = vm.state.value
        assertEquals(true, s.isAnswerCorrect)
        assertEquals(10, s.score)
        assertEquals(1, s.streak)
        assertEquals(1, s.correctCount)
    }

    @Test fun articleTolerantAnswer() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.setCategory("animals"); vm.setSessionSize(10); vm.startSession() // "A cat"
        vm.checkAnswer("cat")
        assertEquals(true, vm.state.value.isAnswerCorrect)
    }

    @Test fun wrongAnswerResetsStreakKeepsWord() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.setSessionSize(10); vm.startSession()
        vm.checkAnswer("banana")
        val s = vm.state.value
        assertEquals(false, s.isAnswerCorrect)
        assertEquals(0, s.streak)
        assertEquals(0, s.score)
        assertNotNull(s.currentWord) // still on the same word to retry
    }

    @Test fun forfeitRevealsWithoutPenalty() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.setSessionSize(10); vm.startSession()
        vm.forfeit()
        assertTrue(vm.state.value.forfeited)
        assertEquals(0, vm.state.value.score)
    }

    @Test fun sessionEndsAtSizeAndReachesSummary() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.setSessionSize(2); vm.startSession() // apple then cat
        vm.checkAnswer("apple"); advanceUntilIdle() // auto-advance -> cat
        vm.checkAnswer("cat"); advanceUntilIdle()   // auto-advance -> summary
        val s = vm.state.value
        assertEquals(Screen.SUMMARY, s.screen)
        assertEquals(22, s.score) // 10 (streak 0) + 12 (streak 1 bonus +2)
        assertEquals(2, s.correctCount)
        assertEquals(2, s.bestStreakThisSession)
    }

    @Test fun streakBonusIsCappedAndAdded() = runTest {
        val a = WordItem(1, "a", "א", "x"); val b = WordItem(2, "b", "ב", "x"); val c = WordItem(3, "c", "ג", "x")
        val vm = vm(words = listOf(a, b, c))
        advanceUntilIdle()
        vm.setSessionSize(3); vm.startSession()
        vm.checkAnswer("a"); advanceUntilIdle() // +10  (streak 0)
        vm.checkAnswer("b"); advanceUntilIdle() // +12  (streak 1)
        vm.checkAnswer("c"); advanceUntilIdle() // +14  (streak 2)
        assertEquals(36, vm.state.value.score)
    }

    @Test fun hintsDiscountBaseRewardNotStreakBonus() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.setSessionSize(10); vm.startSession()
        vm.checkAnswer("apple", hintsUsed = 2) // base 10 - 2 = 8 (streak 0, no bonus)
        assertEquals(8, vm.state.value.lastGained)
        assertEquals(8, vm.state.value.score)
    }

    @Test fun hintPenaltyFlooredAtMinimum() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.setSessionSize(10); vm.startSession()
        vm.checkAnswer("apple", hintsUsed = 99) // floored, not negative
        assertEquals(GameConfig.MIN_POINTS_PER_CORRECT, vm.state.value.lastGained)
    }

    @Test fun bestScorePersistsPerSizeAndFlagsNewRecord() = runTest {
        val store = InMemoryScoreStore()
        val vm = vm(store = store)
        advanceUntilIdle()
        vm.setSessionSize(2); vm.startSession()
        vm.checkAnswer("apple"); advanceUntilIdle()
        vm.checkAnswer("cat"); advanceUntilIdle()
        assertTrue(vm.state.value.newRecord)
        assertEquals(22, store.bestScore(2)) // 10 + 12
        assertEquals(0, store.bestScore(10)) // bucket 10 untouched

        // Replay, forfeit everything -> score 0 -> no new record, best stays 20.
        vm.playAgain()
        vm.forfeit(); vm.acknowledgeForfeit(); advanceUntilIdle()
        vm.forfeit(); vm.acknowledgeForfeit(); advanceUntilIdle()
        assertFalse(vm.state.value.newRecord)
        assertEquals(22, store.bestScore(2))
    }

    @Test fun crossingPointsThresholdAwardsACard() = runTest {
        // Seed just below one card's threshold so a single correct answer (apple, +10) crosses it.
        val store = InMemoryScoreStore().apply { addPoints(GameConfig.POINTS_PER_CARD - 10) }
        val snd = CountingSound()
        val vm = GameViewModel(FakeRepo(listOf(apple, cat)), snd, store, sequential, firstCard)
        advanceUntilIdle()
        vm.setSessionSize(10); vm.startSession()
        vm.checkAnswer("apple") // +10 -> exactly POINTS_PER_CARD -> one card unlocked
        val s = vm.state.value
        assertEquals(GameConfig.POINTS_PER_CARD, store.totalPoints())
        assertEquals(1, s.ownedCardIds.size)
        assertNotNull(s.newCardId)     // reveal armed
        assertEquals(1, s.newCardNonce)
        assertEquals(1, snd.levelUp)   // card-win fanfare fired once
    }

    @Test fun pointsBelowThresholdAwardNoCard() = runTest {
        val store = InMemoryScoreStore()
        val snd = CountingSound()
        val vm = GameViewModel(FakeRepo(listOf(apple, cat)), snd, store, sequential, firstCard)
        advanceUntilIdle()
        vm.setSessionSize(2); vm.startSession()
        vm.checkAnswer("apple"); advanceUntilIdle() // +10
        vm.checkAnswer("cat"); advanceUntilIdle()   // +12 -> total 22, below POINTS_PER_CARD
        assertEquals(22, store.totalPoints())
        assertTrue(vm.state.value.ownedCardIds.isEmpty())
        assertNull(vm.state.value.newCardId)
        assertEquals(0, snd.levelUp)
    }

    @Test fun soundGatedByPreference() = runTest {
        val snd = CountingSound()
        val vmOff = GameViewModel(FakeRepo(listOf(apple)), snd, InMemoryScoreStore(sound = false), sequential)
        advanceUntilIdle()
        vmOff.setSessionSize(10); vmOff.startSession()
        vmOff.checkAnswer("apple")
        assertEquals(0, snd.correct) // muted

        val snd2 = CountingSound()
        val vmOn = GameViewModel(FakeRepo(listOf(apple)), snd2, InMemoryScoreStore(sound = true), sequential)
        advanceUntilIdle()
        vmOn.setSessionSize(10); vmOn.startSession()
        vmOn.checkAnswer("apple")
        assertEquals(1, snd2.correct)
    }
}
