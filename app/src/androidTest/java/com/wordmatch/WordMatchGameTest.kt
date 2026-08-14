package com.wordmatch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.wordmatch.data.InMemoryScoreStore
import com.wordmatch.data.WordRepository
import com.wordmatch.game.GameViewModel
import com.wordmatch.model.JsonBinResponse
import com.wordmatch.model.WordItem
import com.wordmatch.ui.MainScreen
import com.wordmatch.util.SoundManager
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UI flows for the Hebrew -> English game, driven by the deterministic 5-word
 * words_test.json in androidTest/assets. Run on a device/emulator:
 *   ./gradlew connectedAndroidTest
 *
 * Word order is forced sequential (apple, cat, happy, house, water, then it wraps) via an injected
 * picker, so "next word" assertions are reproducible.
 */
@RunWith(AndroidJUnit4::class)
class WordMatchGameTest {

    @get:Rule
    val rule = createComposeRule()

    private val sound = CountingSound()

    private class CountingSound : SoundManager {
        var correct = 0
        var wrong = 0
        var levelUp = 0
        override fun playCorrect() { correct++ }
        override fun playWrong() { wrong++ }
        override fun playLevelUp() { levelUp++ }
    }

    private class FakeRepo(private val words: List<WordItem>) : WordRepository {
        override suspend fun loadWords(binId: String): List<WordItem> = words
    }

    private fun loadTestWords(): List<WordItem> {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        val json = ctx.assets.open("words_test.json").bufferedReader().use { it.readText() }
        return Gson().fromJson(json, JsonBinResponse::class.java).record
    }

    private fun buildViewModel(): GameViewModel {
        val words = loadTestWords()
        val sequential: (List<WordItem>, Int?) -> WordItem = { list, lastId ->
            val idx = list.indexOfFirst { it.id == lastId } // -1 when lastId == null -> first word
            list[(idx + 1) % list.size]
        }
        // Pre-own the first card so a full 10-word session (170 lifetime pts, one 100-pt line) never
        // pops the card-win reveal over the UI mid-flow — these tests assert on the game, not the album.
        val store = InMemoryScoreStore().apply { unlockCard(1) }
        return GameViewModel(FakeRepo(words), sound, store, sequential)
    }

    private fun ComposeContentTestRule.awaitText(text: String, timeoutMs: Long = 4000) {
        waitUntil(timeoutMs) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText(text).assertIsDisplayed()
    }

    /** Set content, wait for the start screen, then begin a session (default: all words, size 10). */
    private fun startGame() {
        rule.setContent { MainScreen(buildViewModel()) }
        // Wait for the button to exist, then scroll it into view (the album card sits above it).
        rule.waitUntil(4000) { rule.onAllNodesWithText("התחל").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("התחל").performScrollTo().performClick()
    }

    // 1: Start screen -> begin -> first Hebrew word shows.
    @Test
    fun beginsAndShowsFirstWord() {
        startGame()
        rule.awaitText("תפוח")
    }

    // 2: Correct answer -> success, streak, points, sound, auto-advance to next word.
    @Test
    fun correctAnswerFlow() {
        startGame()
        rule.awaitText("תפוח")
        rule.onNodeWithTag("answerInput").performTextInput("apple")
        rule.onNodeWithText("בדוק").performClick()

        rule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()
        rule.onNodeWithText("🔥 1").assertIsDisplayed()
        rule.onNodeWithText("נקודות: 10").assertIsDisplayed()
        assertEquals(1, sound.correct)

        rule.awaitText("חתול") // auto-advance
    }

    // 3: Case-insensitive answer.
    @Test
    fun caseInsensitiveAnswer() {
        startGame()
        rule.awaitText("תפוח")
        rule.onNodeWithTag("answerInput").performTextInput("APPLE")
        rule.onNodeWithText("בדוק").performClick()
        rule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()
    }

    // 4: Surrounding whitespace trimmed.
    @Test
    fun whitespaceTrimmed() {
        startGame()
        rule.awaitText("תפוח")
        rule.onNodeWithTag("answerInput").performTextInput("  apple  ")
        rule.onNodeWithText("בדוק").performClick()
        rule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()
    }

    // 5: Wrong answer -> error, streak reset, no points, sound, input stays for retry.
    @Test
    fun wrongAnswerFlow() {
        startGame()
        rule.awaitText("תפוח")
        rule.onNodeWithTag("answerInput").performTextInput("banana")
        rule.onNodeWithText("בדוק").performClick()

        rule.onNodeWithText("❌ עוד לא... נסה שוב").assertIsDisplayed()
        rule.onNodeWithText("🔥 0").assertIsDisplayed()
        rule.onNodeWithText("נקודות: 0").assertIsDisplayed()
        assertEquals(1, sound.wrong)
        rule.onNodeWithTag("answerInput").assertIsDisplayed()
    }

    // 6: Retry after a wrong answer succeeds.
    @Test
    fun retryAfterWrong() {
        startGame()
        rule.awaitText("תפוח")
        rule.onNodeWithTag("answerInput").performTextInput("banana")
        rule.onNodeWithText("בדוק").performClick()
        rule.onNodeWithText("❌ עוד לא... נסה שוב").assertIsDisplayed()

        rule.onNodeWithTag("answerInput").performTextClearance()
        rule.onNodeWithTag("answerInput").performTextInput("apple")
        rule.onNodeWithText("בדוק").performClick()

        rule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()
        rule.onNodeWithText("🔥 1").assertIsDisplayed()
    }

    // 7: Forfeit -> reveal English answer, no penalty, "Got it" advances.
    @Test
    fun forfeitFlow() {
        startGame()
        rule.awaitText("תפוח")
        rule.onNodeWithText("מוותר").performClick()

        rule.onNodeWithText("התשובה היתה: apple").assertIsDisplayed()
        rule.onNodeWithText("🔥 0").assertIsDisplayed()
        rule.onNodeWithText("נקודות: 0").assertIsDisplayed()

        rule.onNodeWithText("הבנתי!").performClick()
        rule.awaitText("חתול")
    }

    // 8: Full session of 10 words (5-word pool wraps once), all correct -> Summary + score 100.
    @Test
    fun fullSessionReachesSummary() {
        // sequential order for size 10 over the 5 test words:
        val order = listOf(
            "תפוח" to "apple", "חתול" to "cat", "שמח" to "happy", "בית" to "house", "מים" to "water",
            "תפוח" to "apple", "חתול" to "cat", "שמח" to "happy", "בית" to "house", "מים" to "water"
        )
        startGame()
        order.forEach { (he, en) ->
            rule.awaitText(he)
            rule.onNodeWithTag("answerInput").performTextInput(en)
            rule.onNodeWithText("בדוק").performClick()
        }
        rule.awaitText("סיום!")               // summary screen
        // 10 correct: base 100 + capped streak bonuses (0,2,4,6,8,10,10,10,10,10) = 170
        rule.onNodeWithText("170").assertIsDisplayed()
        rule.onNodeWithText("🏆 שיא חדש!").assertIsDisplayed() // first record
    }
}
