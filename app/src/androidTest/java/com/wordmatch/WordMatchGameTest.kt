package com.wordmatch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
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
 * The word order is forced sequential (apple, cat, happy, house, water) via an injected picker,
 * so assertions on "the next word" are reproducible.
 */
@RunWith(AndroidJUnit4::class)
class WordMatchGameTest {

    @get:Rule
    val rule = createComposeRule()

    private val sound = CountingSound()

    private class CountingSound : SoundManager {
        var correct = 0
        var wrong = 0
        override fun playCorrect() { correct++ }
        override fun playWrong() { wrong++ }
    }

    private class FakeRepo(private val words: List<WordItem>) : WordRepository {
        override suspend fun loadWords(): List<WordItem> = words
    }

    private fun loadTestWords(): List<WordItem> {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        val json = ctx.assets.open("words_test.json").bufferedReader().use { it.readText() }
        return Gson().fromJson(json, JsonBinResponse::class.java).record
    }

    /** Builds a ViewModel wired to the test words in a fixed, cycling order. */
    private fun buildViewModel(): GameViewModel {
        val words = loadTestWords()
        val sequential: (List<WordItem>, Int?) -> WordItem = { list, lastId ->
            val idx = list.indexOfFirst { it.id == lastId } // -1 when lastId == null -> first word
            list[(idx + 1) % list.size]
        }
        return GameViewModel(FakeRepo(words), sound, sequential)
    }

    private fun setContent() = rule.setContent { MainScreen(buildViewModel()) }

    private fun ComposeContentTestRule.awaitText(text: String, timeoutMs: Long = 4000) {
        waitUntil(timeoutMs) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText(text).assertIsDisplayed()
    }

    // 1: App loads and shows the first Hebrew word.
    @Test
    fun appLoadsAndShowsFirstWord() {
        setContent()
        rule.awaitText("תפוח")
    }

    // 2: Correct answer -> success, streak, points, sound, auto-advance to next word.
    @Test
    fun correctAnswerFlow() {
        setContent()
        rule.awaitText("תפוח")
        rule.onNodeWithTag("answerInput").performTextInput("apple")
        rule.onNodeWithText("בדוק").performClick()

        rule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()
        rule.onNodeWithText("🔥 1").assertIsDisplayed()
        rule.onNodeWithText("נקודות: 10").assertIsDisplayed()
        assertEquals(1, sound.correct)

        rule.awaitText("חתול") // auto-advance
    }

    // 3: Answer accepted case-insensitively.
    @Test
    fun caseInsensitiveAnswer() {
        setContent()
        rule.awaitText("תפוח")
        rule.onNodeWithTag("answerInput").performTextInput("APPLE")
        rule.onNodeWithText("בדוק").performClick()
        rule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()
    }

    // 4: Surrounding whitespace is trimmed.
    @Test
    fun whitespaceTrimmed() {
        setContent()
        rule.awaitText("תפוח")
        rule.onNodeWithTag("answerInput").performTextInput("  apple  ")
        rule.onNodeWithText("בדוק").performClick()
        rule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()
    }

    // 5: Wrong answer -> error, streak reset, no points, sound, input stays for retry.
    @Test
    fun wrongAnswerFlow() {
        setContent()
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
        setContent()
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
        setContent()
        rule.awaitText("תפוח")
        rule.onNodeWithText("הפקר").performClick()

        rule.onNodeWithText("התשובה היתה: apple").assertIsDisplayed()
        rule.onNodeWithText("🔥 0").assertIsDisplayed()
        rule.onNodeWithText("נקודות: 0").assertIsDisplayed()

        rule.onNodeWithText("הבנתי!").performClick()
        rule.awaitText("חתול")
    }

    // 8: Full 5-word session mixing correct / wrong+retry / forfeit.
    @Test
    fun fullGameSession() {
        setContent()

        // apple (correct)
        rule.awaitText("תפוח")
        rule.onNodeWithTag("answerInput").performTextInput("apple")
        rule.onNodeWithText("בדוק").performClick()
        rule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()

        // cat (wrong then correct)
        rule.awaitText("חתול")
        rule.onNodeWithTag("answerInput").performTextInput("dog")
        rule.onNodeWithText("בדוק").performClick()
        rule.onNodeWithText("❌ עוד לא... נסה שוב").assertIsDisplayed()
        rule.onNodeWithTag("answerInput").performTextClearance()
        rule.onNodeWithTag("answerInput").performTextInput("cat")
        rule.onNodeWithText("בדוק").performClick()
        rule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()

        // happy (forfeit)
        rule.awaitText("שמח")
        rule.onNodeWithText("הפקר").performClick()
        rule.onNodeWithText("התשובה היתה: happy").assertIsDisplayed()
        rule.onNodeWithText("הבנתי!").performClick()

        // house (correct)
        rule.awaitText("בית")
        rule.onNodeWithTag("answerInput").performTextInput("house")
        rule.onNodeWithText("בדוק").performClick()
        rule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()

        // water (correct)
        rule.awaitText("מים")
        rule.onNodeWithTag("answerInput").performTextInput("water")
        rule.onNodeWithText("בדוק").performClick()
        rule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()

        // 4 correct * 10 = 40; streak: apple1, cat reset->1, forfeit keeps, house2, water3.
        rule.onNodeWithText("נקודות: 40").assertIsDisplayed()
        rule.onNodeWithText("🔥 3").assertIsDisplayed()
    }
}
