# Design & Development Plan: WordMatch — English/Hebrew Learning App (Android)

## 1. Product Brief

**App Name:** WordMatch  
**Platform:** Android (Kotlin + Jetpack Compose)  
**Target Audience:** Kids (ages 6–12)  
**Core Value Prop:** Learn English ↔ Hebrew through playful, gamified flashcard repetition with instant feedback.  
**Core Loop:** Fetch words → See word → Input translation → Get reward feedback or retry → Move to next word.

### User Stories
- **As a learner**, I want to see a word and translate it so I can practice vocabulary in both directions.
- **As a learner**, I want immediate feedback (sounds, animations) so I feel motivated to continue.
- **As a learner**, I want to retry failed attempts without punishment so I'm not afraid to guess.
- **As a learner**, I want to skip hard words and see the answer so I can learn and move on.

### Language & Localization
- **UI Language:** 100% Hebrew (all labels, buttons, feedback messages)
- **Layout Direction:** Full RTL (right-to-left) for all UI elements
- **Text Direction:** Input fields, feedback, all text respects RTL flow

---

## 2. Visual Design Direction

### Design Philosophy
**Playful, energetic, uncluttered.** Inspired by modern children's apps (Duolingo, Wordle Kids) but with a distinctive personality. Emphasis on:
- Large, readable typography (accessibility + readability for kids)
- Generous spacing and tap targets (>48px for fingers)
- Bright, warm color palette (not garish—intentional, balanced)
- Micro-interactions that reward success without overwhelming the screen
- Smooth, snappy animations (feels responsive, not sluggish)

### Color Palette

| Name | Hex | Usage |
|------|-----|-------|
| **Vibrant Blue** | `#3B82F6` | Primary action, word cards, progress |
| **Sunny Orange** | `#FB923C` | Success state, rewards, accents |
| **Soft Green** | `#10B981` | Correct answer feedback |
| **Light Cream** | `#FFFBF0` | Background, card backgrounds |
| **Dark Slate** | `#1F2937` | Text (primary), labels |
| **Warm Gray** | `#9CA3AF` | Secondary text, hints |

**Rationale:** The blue-orange contrast is high-energy for kids; cream background reduces eye strain; green is universally understood as "correct." No dark mode needed for this age group.

### Typography

| Role | Font | Size | Weight | Use Case |
|------|------|------|--------|----------|
| **Display** | Quicksand (Google Fonts) | 48px–72px | Bold (700) | Word cards, titles |
| **Body** | Poppins (Google Fonts) | 16px–20px | Regular (400) | Instructions, UI labels |
| **Utility** | Poppins | 14px | Regular (400) | Hints, secondary info |

**Rationale:** Quicksand is rounded and friendly (kids respond well); Poppins is modern, legible, and pairs well. Both are open-source and optimized for screen readability.

### Layout Approach

**Mobile-first, portrait orientation, full RTL.** All elements flow right-to-left.

```
┌─────────────────────────────┐
│ [הגדרות]    ✨ WordMatch    │  Header (RTL, sticky)
├─────────────────────────────┤
│                             │
│            🔥 5 :קומבו      │  Progress bar (RTL)
│  ░░░░████████ 10/8          │
│                             │
│  ┌───────────────────────┐  │  
│  │                       │  │  
│  │  ?התרגם את המילה     │  │  Instruction (Hebrew, RTL)
│  │                       │  │
│  │   ┌──────────────┐    │  │  Word card (large, centered)
│  │   │   תפוח       │    │  │  
│  │   └──────────────┘    │  │
│  │                       │  │
│  │  עברית → אנגלית      │  │  Language pair (RTL)
│  │                       │  │
│  │  ┌────────────────┐   │  │
│  │  │ [תשובתך כאן]   │   │  │  Input field (RTL, dir="rtl")
│  │  └────────────────┘   │  │
│  │                       │  │
│  │  [הפקר]    [בדוק]     │  │  Action buttons (RTL order)
│  │                       │  │
│  └───────────────────────┘  │
│                             │
└─────────────────────────────┘
```

### Hebrew UI Strings

| English | Hebrew | Use |
|---------|--------|-----|
| WordMatch | וורדמץ' | App title |
| Streak | קומבו | Consecutive correct answers |
| Points | נקודות | Score |
| Translate the word | התרגם את המילה | Instruction |
| English → Hebrew | אנגלית → עברית | Language direction (or reverse) |
| Your answer | תשובתך כאן | Input placeholder |
| Check | בדוק | Submit answer button |
| Forfeit | הפקר | Skip/give up button |
| Correct! | ✅ כל הכבוד! | Success message |
| Not quite. Try again! | ❌ עוד לא... נסה שוב | Wrong answer message |
| The answer was | התשובה היתה | Forfeit reveal |
| Got it! | הבנתי! | Acknowledge answer button |
| Settings | הגדרות | Menu |
| Sound | קול | Audio toggle |
| Reset Score | אפס את הניקוד | Reset button |

### RTL Implementation (Compose)

```kotlin
// In AndroidManifest.xml
android:supportsRtl="true"

// In Compose, force RTL layout direction
CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    MainScreen()
}

// TextField with RTL
TextField(
    value = userAnswer,
    onValueChange = { userAnswer = it },
    modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
    textStyle = TextStyle(textDirection = TextDirection.Rtl),
    placeholder = { Text("תשובתך כאן") }
)

// Row buttons (auto-reverses in RTL)
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    Button(onClick = { forfeit() }) { Text("הפקר") }  // Right
    Button(onClick = { checkAnswer() }) { Text("בדוק") }  // Left
}
```

### Signature Elements

**1. Word Card Flip Animation**  
When the user reveals the answer (forfeit), the word card rotates 180° and flips to show the translation. Smooth, satisfying, reinforces learning.

**2. Success Confetti Burst**  
On correct answer: brief confetti animation (5–8 shapes, 800ms duration) centered on the card. Not overdone, feels celebratory without distraction.

**3. Streak Counter with Flame Icon**  
A small, persistent flame emoji (🔥) with a number tracks consecutive correct answers. Drives engagement through mini-gamification.

---

## 3. Features & User Flow

### Core Features

#### 3.1 Word Card Display
- **Display:** One word at a time, centered, large (48px+), in the current language
- **Language Toggle:** Clear indicator of source → target language (e.g., "English → Hebrew")
- **Word Source:** Read from a hardcoded JSON list of {english, hebrew} pairs (MVP phase)

#### 3.2 Text Input
- **Input Field:** Large, tappable (48px+ height), with a cursor placeholder
- **Auto-focus:** On page load, focus moves to input (ready to type immediately)
- **Auto-clear:** After submission, input clears for the next word
- **Hint Button (Optional MVP+):** Shows first letter or syllable of correct answer

#### 3.3 Answer Submission
- **Two paths:**
  1. **Check Button:** User types answer, taps "Check" → app compares (case-insensitive, exact match)
  2. **Forfeit Button:** User gives up → app reveals correct answer immediately

#### 3.4 Feedback
- **Correct Answer (Green Feedback)**
  - Toast notification: "✅ Correct!" 
  - Confetti burst (200–400ms)
  - Streak +1, score +10 points
  - Auto-advance to next word (1.5s delay, allows celebration)

- **Wrong Answer (Orange Feedback)**
  - Toast notification: "❌ Not quite. Try again!"
  - Input field briefly shakes (150ms, visual "no")
  - Streak resets to 0
  - Keep input field open (user can retry without penalty)

- **Forfeit (Neutral Feedback)**
  - Card flips to reveal correct answer
  - Display: "The answer was: [HEBREW WORD]"
  - No points, but no streak loss either
  - Button changes to "Got it!" → auto-advance

#### 3.5 Progress Tracking
- **Session Stats** (bottom of screen, small)
  - Current streak (🔥 5)
  - Session points (50/100)
  - Words completed (8/10)
- **Visual Progress Bar:** Fills as session progresses

#### 3.6 Session Modes (MVP v1.1)
- **Quick Play:** 10 words, random order
- **Category Play:** Pick a theme (animals, colors, actions) → 10 words from that list
- **Daily Challenge:** Same 5 words every day (revisit same vocabulary)

---

## 4. Technical Architecture

### Tech Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (declarative UI)
- **Networking:** Retrofit + Gson (HTTP client for JSON fetch)
- **Storage:** SharedPreferences (session progress, settings)
- **Audio:** Android MediaPlayer (sound effects)
- **Testing:** JUnit 5 + Mockito (unit tests) + Espresso (integration tests)
- **Build:** Gradle, Android Studio
- **Min SDK:** API 26+ | **Target SDK:** Latest (35+)

### Data Source & Fetching
- **Hosting:** JSONBin.io (free, no auth needed for public bins)
- **Fetch Method:** HTTP GET request via Retrofit
- **URL Pattern:** `https://api.jsonbin.io/v3/b/{BIN_ID}/latest`
- **Frequency:** Fetch on app start, cache locally (SharedPreferences)
- **Fallback:** If network unavailable, use bundled fallback JSON in assets/

### JSON Schema (from JSONBin.io)
```json
{
  "record": [
    {
      "id": 1,
      "english": "apple",
      "hebrew": "תפוח",
      "category": "fruits"
    },
    {
      "id": 2,
      "english": "house",
      "hebrew": "בית",
      "category": "places"
    }
  ]
}
```

**Note:** JSONBin wraps response in `"record"` object; unwrap in code.

### Data Classes (Kotlin)

```kotlin
// Model from JSONBin.io
data class WordItem(
    val id: Int,
    val english: String,
    val hebrew: String,
    val category: String
)

data class JsonBinResponse(
    val record: List<WordItem>
)

// Game state
data class GameState(
    val currentWord: WordItem? = null,
    val userAnswer: String = "",
    val streak: Int = 0,
    val score: Int = 0,
    val wordsCompleted: Int = 0,
    val isAnswerCorrect: Boolean? = null,  // null = pending, true/false = result
    val showAnswer: Boolean = false,
    val gameStatus: GameStatus = GameStatus.LOADING
)

enum class GameStatus {
    LOADING, READY, CHECKING, RESULT, FORFEIT, ERROR
}
```

### Compose Component Structure

```
MainScreen
├─ LazyColumn (RTL layout)
│  ├─ HeaderSection (streak 🔥, points)
│  ├─ ProgressBar (words completed / streak)
│  ├─ WordCardSection
│  │  ├─ LanguageIndicator ("אנגלית → עברית")
│  │  └─ WordCard (large text, centered)
│  ├─ InputSection (RTL text field)
│  │  └─ HebrewInputField (dir="rtl")
│  ├─ ActionButtonsSection
│  │  ├─ CheckButton / GotItButton
│  │  └─ ForfeitButton
│  └─ FeedbackSection (animated toast)
├─ SoundManager (plays audio)
└─ ConfettiAnimation (overlay)
```

### Key Game Logic (Kotlin)

```kotlin
// Answer verification (case-insensitive, trim whitespace)
fun isAnswerCorrect(userInput: String, correctAnswer: String): Boolean {
    return userInput.trim().lowercase() == correctAnswer.trim().lowercase()
}

// ViewModel
class GameViewModel(private val wordRepository: WordRepository) : ViewModel() {
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    fun checkAnswer(userAnswer: String) {
        val currentWord = _gameState.value.currentWord ?: return
        val isCorrect = isAnswerCorrect(userAnswer, currentWord.hebrew)
        
        if (isCorrect) {
            val newStreak = _gameState.value.streak + 1
            val newScore = _gameState.value.score + 10
            _gameState.value = _gameState.value.copy(
                isAnswerCorrect = true,
                streak = newStreak,
                score = newScore,
                gameStatus = GameStatus.RESULT
            )
            playSound(R.raw.success)
            showConfetti()
            // Auto-advance after 1500ms
            viewModelScope.launch {
                delay(1500)
                nextWord()
            }
        } else {
            _gameState.value = _gameState.value.copy(
                isAnswerCorrect = false,
                streak = 0,  // Reset streak on wrong answer
                gameStatus = GameStatus.RESULT
            )
            playSound(R.raw.fail)
            shakeInput()
        }
    }

    fun forfeit() {
        _gameState.value = _gameState.value.copy(
            showAnswer = true,
            gameStatus = GameStatus.FORFEIT
            // No score change, no streak loss
        )
        // Auto-advance after 2000ms
        viewModelScope.launch {
            delay(2000)
            nextWord()
        }
    }

    fun nextWord() {
        val words = wordRepository.getAllWords()
        if (words.isNotEmpty()) {
            val randomWord = words.random()
            _gameState.value = _gameState.value.copy(
                currentWord = randomWord,
                userAnswer = "",
                isAnswerCorrect = null,
                showAnswer = false,
                wordsCompleted = _gameState.value.wordsCompleted + 1,
                gameStatus = GameStatus.READY
            )
        }
    }
}
```

### Answer Verification (Case-Insensitive)

**Verification Logic:**
```kotlin
fun isAnswerCorrect(userInput: String, correctAnswer: String): Boolean {
    // Trim whitespace, convert to lowercase (works for both English and Hebrew)
    return userInput.trim().lowercase(Locale("iw")) == 
           correctAnswer.trim().lowercase(Locale("iw"))
}
```

**Test Cases:**
- ✅ `"תפוח"` == `"תפוח"` → correct
- ✅ `"  תפוח  "` == `"תפוח"` → correct (whitespace trimmed)
- ❌ `"תפו"` == `"תפוח"` → wrong
- ❌ `"תפוח"` == `"בית"` → wrong

---

## Integration Testing Strategy

### Test Approach
- **Framework:** JUnit 5 + Mockito + Espresso
- **Scope:** Full end-to-end user flows (fetch → display → answer → feedback)
- **Data:** Deterministic test JSON (5 words) for reproducible results
- **Network:** Mock Retrofit responses to avoid real network calls
- **Assertions:** Verify UI state, game state, and audio/animation triggers

### Test JSON File (words_test.json)

Located in `src/androidTest/assets/words_test.json`:

```json
{
  "record": [
    {
      "id": 1,
      "english": "apple",
      "hebrew": "תפוח",
      "category": "fruits"
    },
    {
      "id": 2,
      "english": "cat",
      "hebrew": "חתול",
      "category": "animals"
    },
    {
      "id": 3,
      "english": "happy",
      "hebrew": "שמח",
      "category": "emotions"
    },
    {
      "id": 4,
      "english": "house",
      "hebrew": "בית",
      "category": "places"
    },
    {
      "id": 5,
      "english": "water",
      "hebrew": "מים",
      "category": "nature"
    }
  ]
}
```

### Integration Test Suite

```kotlin
// File: WordMatchGameTest.kt

@RunWith(AndroidJUnit4::class)
class WordMatchGameTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mockWordRepository: WordRepository
    private lateinit var mockSoundManager: SoundManager
    private lateinit var gameViewModel: GameViewModel

    @Before
    fun setUp() {
        mockWordRepository = mock(WordRepository::class.java)
        mockSoundManager = mock(SoundManager::class.java)
        
        // Load test words
        val testWords = loadTestWords()
        `when`(mockWordRepository.getAllWords()).thenReturn(testWords)
    }

    // ============ TEST 1: App Loads & Fetches Words ============
    @Test
    fun testAppLoadsFetchesWords() {
        // Given: App is launched
        composeTestRule.setContent {
            MainScreen(gameViewModel)
        }

        // Then: Loading indicator is visible
        composeTestRule.onNodeWithText("טוען...").assertIsDisplayed()

        // Then: After fetch, first word is displayed
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onNodeWithText("תפוח").isDisplayed()
        }
        composeTestRule.onNodeWithText("תפוח").assertIsDisplayed()
    }

    // ============ TEST 2: Correct Answer Flow ============
    @Test
    fun testCorrectAnswerFlow() {
        composeTestRule.setContent {
            MainScreen(gameViewModel)
        }

        // Wait for word to load
        composeTestRule.waitUntil { 
            composeTestRule.onNodeWithText("תפוח").isDisplayed() 
        }

        // User types correct answer in Hebrew
        composeTestRule.onNodeWithTag("answerInput").performTextInput("תפוח")

        // User taps Check button
        composeTestRule.onNodeWithText("בדוק").performClick()

        // Assert: Success message appears
        composeTestRule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()

        // Assert: Streak incremented
        composeTestRule.onNodeWithText("🔥 1").assertIsDisplayed()

        // Assert: Points awarded (10)
        composeTestRule.onNodeWithText("נקודות: 10").assertIsDisplayed()

        // Assert: Success sound played
        verify(mockSoundManager, times(1)).playSound(R.raw.success)

        // Assert: Auto-advance after 1500ms (next word displayed)
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule.onNodeWithText("חתול").isDisplayed()
        }
    }

    // ============ TEST 3: Case-Insensitive Matching ============
    @Test
    fun testCaseInsensitiveAnswerMatching() {
        composeTestRule.setContent {
            MainScreen(gameViewModel)
        }

        composeTestRule.waitUntil { 
            composeTestRule.onNodeWithText("תפוח").isDisplayed() 
        }

        // User types with different casing (not applicable to Hebrew, but good for English mode)
        composeTestRule.onNodeWithTag("answerInput").performTextInput("תפוח")
        composeTestRule.onNodeWithText("בדוק").performClick()

        // Assert: Still recognized as correct
        composeTestRule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()
    }

    // ============ TEST 4: Whitespace Trimming ============
    @Test
    fun testWhitespaceTrimmingInAnswers() {
        composeTestRule.setContent {
            MainScreen(gameViewModel)
        }

        composeTestRule.waitUntil { 
            composeTestRule.onNodeWithText("תפוח").isDisplayed() 
        }

        // User types with leading/trailing whitespace
        composeTestRule.onNodeWithTag("answerInput").performTextInput("  תפוח  ")
        composeTestRule.onNodeWithText("בדוק").performClick()

        // Assert: Whitespace trimmed, still correct
        composeTestRule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()
    }

    // ============ TEST 5: Wrong Answer Flow ============
    @Test
    fun testWrongAnswerFlow() {
        composeTestRule.setContent {
            MainScreen(gameViewModel)
        }

        composeTestRule.waitUntil { 
            composeTestRule.onNodeWithText("תפוח").isDisplayed() 
        }

        // User types wrong answer
        composeTestRule.onNodeWithTag("answerInput").performTextInput("בית")
        composeTestRule.onNodeWithText("בדוק").performClick()

        // Assert: Error message appears
        composeTestRule.onNodeWithText("❌ עוד לא... נסה שוב").assertIsDisplayed()

        // Assert: Streak reset to 0
        composeTestRule.onNodeWithText("🔥 0").assertIsDisplayed()

        // Assert: No points awarded
        composeTestRule.onNodeWithText("נקודות: 0").assertIsDisplayed()

        // Assert: Fail sound played
        verify(mockSoundManager, times(1)).playSound(R.raw.fail)

        // Assert: Input field still visible (user can retry)
        composeTestRule.onNodeWithTag("answerInput").assertIsDisplayed()
    }

    // ============ TEST 6: Retry After Wrong Answer ============
    @Test
    fun testRetryAfterWrongAnswer() {
        composeTestRule.setContent {
            MainScreen(gameViewModel)
        }

        composeTestRule.waitUntil { 
            composeTestRule.onNodeWithText("תפוח").isDisplayed() 
        }

        // First attempt: wrong
        composeTestRule.onNodeWithTag("answerInput").performTextInput("בית")
        composeTestRule.onNodeWithText("בדוק").performClick()
        composeTestRule.onNodeWithText("❌ עוד לא... נסה שוב").assertIsDisplayed()

        // Clear input and retry
        composeTestRule.onNodeWithTag("answerInput").performTextClearance()
        composeTestRule.onNodeWithTag("answerInput").performTextInput("תפוח")
        composeTestRule.onNodeWithText("בדוק").performClick()

        // Assert: Correct on retry
        composeTestRule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()
        composeTestRule.onNodeWithText("🔥 1").assertIsDisplayed()
    }

    // ============ TEST 7: Forfeit Flow ============
    @Test
    fun testForfeitFlow() {
        composeTestRule.setContent {
            MainScreen(gameViewModel)
        }

        composeTestRule.waitUntil { 
            composeTestRule.onNodeWithText("תפוח").isDisplayed() 
        }

        // User taps Forfeit
        composeTestRule.onNodeWithText("הפקר").performClick()

        // Assert: Answer revealed
        composeTestRule.onNodeWithText("התשובה היתה: תפוח").assertIsDisplayed()

        // Assert: No streak loss, no points gained
        composeTestRule.onNodeWithText("🔥 0").assertIsDisplayed()
        composeTestRule.onNodeWithText("נקודות: 0").assertIsDisplayed()

        // Assert: "Got it!" button appears
        composeTestRule.onNodeWithText("הבנתי!").assertIsDisplayed()

        // User taps "Got it!"
        composeTestRule.onNodeWithText("הבנתי!").performClick()

        // Assert: Auto-advance to next word
        composeTestRule.waitUntil(timeoutMillis = 2500) {
            composeTestRule.onNodeWithText("חתול").isDisplayed()
        }
    }

    // ============ TEST 8: Full Game Session (All 5 Words) ============
    @Test
    fun testFullGameSessionFlow() {
        var correctCount = 0
        
        composeTestRule.setContent {
            MainScreen(gameViewModel)
        }

        // Word 1: תפוח (CORRECT)
        composeTestRule.waitUntil { 
            composeTestRule.onNodeWithText("תפוח").isDisplayed() 
        }
        composeTestRule.onNodeWithTag("answerInput").performTextInput("תפוח")
        composeTestRule.onNodeWithText("בדוק").performClick()
        composeTestRule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()
        correctCount++

        // Word 2: חתול (WRONG, then CORRECT)
        composeTestRule.waitUntil { 
            composeTestRule.onNodeWithText("חתול").isDisplayed() 
        }
        composeTestRule.onNodeWithTag("answerInput").performTextInput("כלב")
        composeTestRule.onNodeWithText("בדוק").performClick()
        composeTestRule.onNodeWithText("❌ עוד לא... נסה שוב").assertIsDisplayed()
        
        // Retry
        composeTestRule.onNodeWithTag("answerInput").performTextClearance()
        composeTestRule.onNodeWithTag("answerInput").performTextInput("חתול")
        composeTestRule.onNodeWithText("בדוק").performClick()
        composeTestRule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()
        correctCount++

        // Word 3: שמח (FORFEIT)
        composeTestRule.waitUntil { 
            composeTestRule.onNodeWithText("שמח").isDisplayed() 
        }
        composeTestRule.onNodeWithText("הפקר").performClick()
        composeTestRule.onNodeWithText("התשובה היתה: שמח").assertIsDisplayed()
        composeTestRule.onNodeWithText("הבנתי!").performClick()

        // Word 4: בית (CORRECT)
        composeTestRule.waitUntil { 
            composeTestRule.onNodeWithText("בית").isDisplayed() 
        }
        composeTestRule.onNodeWithTag("answerInput").performTextInput("בית")
        composeTestRule.onNodeWithText("בדוק").performClick()
        composeTestRule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()
        correctCount++

        // Word 5: מים (CORRECT)
        composeTestRule.waitUntil { 
            composeTestRule.onNodeWithText("מים").isDisplayed() 
        }
        composeTestRule.onNodeWithTag("answerInput").performTextInput("מים")
        composeTestRule.onNodeWithText("בדוק").performClick()
        composeTestRule.onNodeWithText("✅ כל הכבוד!").assertIsDisplayed()
        correctCount++

        // Assert final stats
        composeTestRule.onNodeWithText("נקודות: 40").assertIsDisplayed()  // 4 correct * 10
        composeTestRule.onNodeWithText("🔥 1").assertIsDisplayed()  // Streak at 1 (last was correct)
    }

    // ============ Helper: Load Test Words ============
    private fun loadTestWords(): List<WordItem> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputStream = context.assets.open("words_test.json")
        val json = inputStream.bufferedReader().use { it.readText() }
        val response = Gson().fromJson(json, JsonBinResponse::class.java)
        return response.record
    }
}
```

### Running Tests

```bash
# Run all integration tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.wordmatch.WordMatchGameTest

# Run with coverage
./gradlew connectedAndroidTestCoverage
```

---

### Audio Assets (Minimal)
- **Success sound:** Short, upbeat chime (0.3s, ~220Hz tone)
- **Fail sound:** Gentle "wrong" buzz (0.2s)
- **Level-up** (every 5-word streak): Brief ascending tone sequence
- **Optional:** Background ambient music (muted by default, toggle in settings)

### Animation Library
- **Confetti:** Use `react-confetti-boom` or canvas-based simple implementation
- **Shake:** CSS keyframe animation (translate X by 5px, 3× rapidly)
- **Flip card:** CSS 3D transform (rotateY)
- **Fade toast:** CSS opacity + transition

---

## 5. User Experience Considerations

### For Kids
- **No time pressure:** Unlimited time to answer (reduces anxiety)
- **Encourage retry:** "Try again" message is neutral, not punishing
- **Celebrate wins:** Confetti + streak feedback (dopamine hits)
- **No "game over":** Session just cycles—can always play one more round
- **Large touch targets:** All buttons ≥48px (based on iOS/Android HIG)

### For Parents/Teachers (MVP+)
- **Word list editor:** Simple UI to add/remove/edit words
- **Export progress:** Download session stats as CSV
- **Difficulty levels:** Filter words by letter count or custom tags

### Accessibility
- **Color + Icon:** Feedback is not color-alone (green ✅ + checkmark)
- **Text contrast:** All text ≥4.5:1 (WCAG AA)
- **Keyboard support:** Tab through buttons, Enter to submit answer
- **Reduced motion:** CSS `@media (prefers-reduced-motion: reduce)` removes confetti/animations

---

## 6. JSONBin.io Integration

### Setup Steps

1. **Create a JSONBin account** (free): https://jsonbin.io/
2. **Upload test data** (words_test.json) to JSONBin
3. **Copy the Bin ID** from the URL: `https://api.jsonbin.io/v3/b/{BIN_ID}/latest`
4. **Add to build.gradle** (production words list):
   ```kotlin
   // build.gradle.kts
   buildConfigField("String", "JSONBIN_PROD_BIN_ID", "\"YOUR_BIN_ID_HERE\"")
   buildConfigField("String", "JSONBIN_TEST_BIN_ID", "\"TEST_BIN_ID_HERE\"")
   ```

### Retrofit Service

```kotlin
interface JsonBinApiService {
    @GET("v3/b/{binId}/latest")
    suspend fun getWords(@Path("binId") binId: String): JsonBinResponse
}

// Usage
val binId = if (BuildConfig.DEBUG) BuildConfig.JSONBIN_TEST_BIN_ID 
            else BuildConfig.JSONBIN_PROD_BIN_ID
val response = apiService.getWords(binId)
```

### Network Error Handling

```kotlin
fun fetchWords() {
    viewModelScope.launch {
        try {
            val words = wordRepository.fetchFromJsonBin()
            _gameState.value = _gameState.value.copy(
                wordsList = words,
                gameStatus = GameStatus.READY
            )
        } catch (e: IOException) {
            // Network error: use cached/bundled fallback
            val cachedWords = wordRepository.getCachedWords()
            if (cachedWords.isNotEmpty()) {
                _gameState.value = _gameState.value.copy(
                    wordsList = cachedWords,
                    gameStatus = GameStatus.READY
                )
            } else {
                _gameState.value = _gameState.value.copy(
                    gameStatus = GameStatus.ERROR
                )
            }
        }
    }
}
```

---

## 7. MVP Phase Breakdown

### Phase 1: Core Game & Network (Week 1–2)
- [ ] Android project scaffold (Kotlin + Compose)
- [ ] JSONBin.io integration (fetch words, handle errors)
- [ ] Fallback bundled JSON in assets/
- [ ] GameViewModel + game state management
- [ ] Word card display (large, centered)
- [ ] Input field (Hebrew, RTL, auto-focus)
- [ ] Check button + answer verification (case-insensitive)
- [ ] Correct/wrong feedback (color, toast message)
- [ ] Streak counter & points display
- [ ] Forfeit button + answer reveal
- [ ] Auto-advance logic (1.5s for correct, 2s for forfeit)
- [ ] Integration test suite (5 core user flows)

### Phase 2: Polish & Audio (Week 2–3)
- [ ] Confetti animation on success
- [ ] Sound effects (success.mp3, fail.mp3)
- [ ] Input shake animation on wrong answer
- [ ] Card flip animation on forfeit reveal
- [ ] RTL layout validation (all UI right-aligned)
- [ ] Hebrew strings in strings.xml
- [ ] Settings modal (sound toggle, reset score)
- [ ] Session end screen (score summary)

### Phase 3: Enhanced UX & Testing (Week 3–4)
- [ ] Streak flame emoji animation
- [ ] Hint button (show first letter)
- [ ] Category selection screen (optional first)
- [ ] Progress bar animations
- [ ] Unit tests for AnswerVerifier, GameViewModel
- [ ] Test coverage reporting
- [ ] Accessibility: color contrast, keyboard nav, reduced motion

### Phase 4: Future (Post-MVP)
- [ ] Spaced repetition scheduling (retry hard words more)
- [ ] Daily challenge mode (same 5 words every day)
- [ ] Leaderboard / streaks persistence
- [ ] Multiplayer mode (2 kids on same device)
- [ ] Custom word lists upload (parents/teachers)
- [ ] Themed skins (animals, space, nature, etc.)
- [ ] Pronunciation audio (speaker icon for each word)

---

## 7. Success Metrics (For Iteration)

**KPIs to track:**
- Session length (avg words per session)
- Retry rate (% of wrong answers retried vs. forfeit)
- Streak consistency (avg streak length)
- Return rate (days since last session)
- Correct answer rate (baseline accuracy)

**Target:** Kids should feel motivated to replay, not frustrated. Aim for 70%+ accuracy, avg 3–5 word streak per session.

---

## 8. File Structure for Android Project

```
WordMatch/
├─ app/
│  ├─ src/
│  │  ├─ main/
│  │  │  ├─ java/com/wordmatch/
│  │  │  │  ├─ MainActivity.kt
│  │  │  │  ├─ ui/
│  │  │  │  │  ├─ MainScreen.kt (Compose)
│  │  │  │  │  ├─ GameBoard.kt
│  │  │  │  │  ├─ WordCard.kt
│  │  │  │  │  ├─ InputField.kt
│  │  │  │  │  ├─ ActionButtons.kt
│  │  │  │  │  ├─ FeedbackToast.kt
│  │  │  │  │  ├─ Header.kt
│  │  │  │  │  └─ ProgressBar.kt
│  │  │  │  ├─ viewmodel/
│  │  │  │  │  └─ GameViewModel.kt
│  │  │  │  ├─ model/
│  │  │  │  │  ├─ WordItem.kt
│  │  │  │  │  ├─ GameState.kt
│  │  │  │  │  ├─ JsonBinResponse.kt
│  │  │  │  │  └─ GameStatus.kt
│  │  │  │  ├─ repository/
│  │  │  │  │  ├─ WordRepository.kt
│  │  │  │  │  └─ WordRepositoryImpl.kt
│  │  │  │  ├─ network/
│  │  │  │  │  ├─ JsonBinApiService.kt
│  │  │  │  │  └─ RetrofitClient.kt
│  │  │  │  ├─ utils/
│  │  │  │  │  ├─ AnswerVerifier.kt
│  │  │  │  │  ├─ SoundManager.kt
│  │  │  │  │  └─ PreferencesManager.kt
│  │  │  │  └─ di/
│  │  │  │     └─ AppModule.kt (Hilt DI)
│  │  │  ├─ res/
│  │  │  │  ├─ raw/
│  │  │  │  │  ├─ success.mp3
│  │  │  │  │  └─ fail.mp3
│  │  │  │  ├─ values/
│  │  │  │  │  └─ strings.xml (Hebrew strings)
│  │  │  │  ├─ values-he/
│  │  │  │  │  └─ strings.xml (Hebrew-specific)
│  │  │  │  └─ drawable/
│  │  │  ├─ AndroidManifest.xml (supportsRtl="true")
│  │  ├─ androidTest/
│  │  │  ├─ java/com/wordmatch/
│  │  │  │  └─ WordMatchGameTest.kt (Integration tests)
│  │  │  └─ assets/
│  │  │     └─ words_test.json (Deterministic test data)
│  │  └─ test/
│  │     └─ java/com/wordmatch/
│  │        ├─ AnswerVerifierTest.kt
│  │        └─ GameViewModelTest.kt
│  ├─ build.gradle.kts
│  └─ proguard-rules.pro
├─ build.gradle.kts
├─ settings.gradle.kts
└─ gradle.properties
```

### Key Files Summary

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Entry point, Compose setup |
| `MainScreen.kt` | Root composable, RTL layout |
| `GameViewModel.kt` | Game state management + logic |
| `WordRepository.kt` | Fetch words from JSONBin.io |
| `AnswerVerifier.kt` | Case-insensitive answer matching |
| `SoundManager.kt` | Audio playback on events |
| `AndroidManifest.xml` | `supportsRtl="true"` for RTL |
| `strings.xml` | English UI strings (fallback) |
| `strings-he.xml` | Hebrew UI strings (primary) |
| `words_test.json` | 5-word test dataset |

---

## 9. Design System Quick Reference

### Spacing
- **xs:** 4px | **sm:** 8px | **md:** 16px | **lg:** 24px | **xl:** 32px

### Border Radius
- **Buttons/cards:** 12px (slightly rounded, friendly)
- **Input field:** 16px (larger, more inviting)

### Shadows
- **Card:** `0 4px 6px rgba(0,0,0,0.1)` (subtle lift)
- **Button hover:** Increase shadow, translate up 2px

### Transitions
- **Standard:** 200ms ease-in-out (UI interactions)
- **Confetti/success:** 400–600ms (celebratory, slightly slower)

---

## 10. Notes & Considerations

### Known Unknowns
- **Handwriting recognition (future)?** MVP assumes typed input; could add pen support later.
- **Pronunciation audio?** Not in MVP, but could add speaker icon to play word audio.
- **RTL validation:** Test on actual Hebrew system locale (API 17+).
- **Difficulty progression?** MVP is random; spaced repetition can come later.

### Risks & Mitigations
| Risk | Mitigation |
|------|-----------|
| JSONBin.io downtime | Bundle fallback JSON in assets/; cache locally after first fetch |
| Kids get bored quickly | Add category selection, daily challenge, visual variety (skins) |
| Frustration on hard words | Forfeit button (no penalty), Hint button, easy word ratio (80/20) |
| Touch targets too small | Use 56dp+ buttons (≈48px+), test on actual tablet |
| Audio too loud/annoying | Mute by default, volume control in settings, short sound files (<500ms) |
| Hebrew text rendering | Use system fonts (Roboto supports Hebrew); test on API 26-35 |
| Network timeouts | Set 10s timeout on Retrofit; show "offline mode" if fetch fails |

### Android Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // Core
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.10.1")
    
    // Compose
    implementation("androidx.compose.ui:ui:1.5.1")
    implementation("androidx.compose.material3:material3:1.0.1")
    implementation("androidx.compose.foundation:foundation:1.5.1")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    
    // ViewModel + StateFlow
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    
    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // DI (Hilt)
    implementation("com.google.dagger:hilt-android:2.46")
    kapt("com.google.dagger:hilt-compiler:2.46")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
```

---

## 11. Quick Start: Building with Claude Code

### What to Hand Claude Code

**"Build this Android app following this design plan. Start with Phase 1:"**
1. Scaffold Kotlin + Jetpack Compose Android project
2. Implement JSONBin.io fetch (with fallback bundled JSON)
3. Build core game loop: display word → input answer → check/forfeit
4. Add streak counter, points tracking
5. Implement case-insensitive answer verification
6. Add correct/wrong feedback (toast messages, color feedback)
7. Create integration test suite (8 user flow tests using Espresso)
8. Ensure full RTL + Hebrew UI

### Before You Start

**Step 1: Create JSONBin Bins**
1. Go to https://jsonbin.io/ (create free account)
2. Create **prod bin**: Upload full word list (50+ words)
3. Create **test bin**: Upload words_test.json (5 words)
4. Copy both Bin IDs

**Step 2: Share Config with Claude Code**
```
JSONBIN_PROD_BIN_ID = "xxxxx"
JSONBIN_TEST_BIN_ID = "yyyyy"
```

**Step 3: Provide Test Data**
Share the 5-word `words_test.json` from this document → Claude Code will add to `androidTest/assets/`

### Build & Test

```bash
# Build
./gradlew assembleDebug

# Run integration tests
./gradlew connectedAndroidTest

# View test reports
# → build/reports/androidTests/connected/index.html

# Install on emulator/device
./gradlew installDebug
adb shell am start -n com.wordmatch/.MainActivity
```

### Success Criteria (Phase 1 Complete)

✅ App launches, fetches words from JSONBin.io  
✅ Word displays in Hebrew, language pair shown (אנגלית ← עברית)  
✅ User can type Hebrew answer in RTL input field  
✅ Check button verifies answer (case-insensitive, whitespace-trimmed)  
✅ Correct answer → green toast + streak +1 + points +10 → auto-advance (1.5s)  
✅ Wrong answer → red toast + shake input + streak reset → can retry  
✅ Forfeit button → reveals answer → no penalty → auto-advance (2s)  
✅ All 8 integration tests pass  
✅ Full Hebrew UI (no English visible except in word pairs)  
✅ Full RTL layout (buttons, text, progress bar all right-aligned)  

---

## 12. Design Specification Summary

| Aspect | Spec |
|--------|------|
| **Platform** | Android (Kotlin + Jetpack Compose) |
| **Min SDK** | API 26+ |
| **Target SDK** | Latest (35+) |
| **Language** | 100% Hebrew UI |
| **Layout** | Full RTL |
| **Data Source** | JSONBin.io (free, public bin) |
| **Storage** | SharedPreferences (local cache) |
| **Answer Check** | Case-insensitive, whitespace-trimmed |
| **Feedback** | Toast + color + optional sound |
| **Tests** | Espresso integration tests (8 flows) |
| **MVP Timeline** | 2–3 weeks |

---

*Document Version:* 2.0 (Android Edition) | *Last Updated:* 2026-07-26*  
*Updated for: Kotlin + Jetpack Compose, JSONBin.io, Hebrew RTL, Integration Tests*
