# WordMatch (משחק מילים)

Android flashcard game that teaches English vocabulary to Hebrew-speaking kids.
The app shows a Hebrew word, the child types the English translation. UI is
100% Hebrew and fully RTL.

Built with Kotlin + Jetpack Compose. Single activity, no DI framework, no
navigation library — one `StateFlow<GameState>` drives the whole app.

## Features

- **Hebrew → English typing drill** with forgiving answer matching: case-insensitive,
  whitespace-collapsed, and a leading article (`a` / `an` / `the` / `to`) is optional
  on either side, so `cat` matches `A cat` and `run` matches `To run`.
- **Finite sessions** of 10 / 20 / 50 / 100 words, with a summary screen at the end.
- **Multiple child profiles** — each child has their own dictionary, high scores and
  card album. Profiles are defined in `GameConfig.CHILDREN`.
- **Remote word lists** — each child's vocabulary is hosted in a public
  [JSONBin.io](https://jsonbin.io) bin so words can be updated without shipping a new
  APK. Falls back to the bundled `assets/words.json` on any network failure.
- **New-words batches** — every word carries a `batch` number; the start screen offers
  "new words" (the highest batch) vs. "all words".
- **Card collection reward loop** — every 100 lifetime points unlocks a random soccer
  player card from a 52-card deck, viewable in the album screen.
- **Streaks, points, confetti**, per-session-size high scores, and a reduced-motion
  mode that honours the system animation scale.

## Build & run

Requires JDK 17. Uses the Gradle wrapper (Gradle 8.11.1, AGP 8.7.3, Kotlin 2.0.21).
`minSdk` 26, `targetSdk` 35.

```bash
./gradlew testDebugUnitTest   # JVM unit tests — no device needed
./gradlew assembleDebug       # -> app/build/outputs/apk/debug/learn-english.apk
./gradlew connectedAndroidTest # Compose UI tests — needs a booted emulator/device
```

Install on a running emulator or device:

```bash
adb install -r app/build/outputs/apk/debug/learn-english.apk
adb shell am start -n com.wordmatch/.MainActivity
```

## Configuration

- `gradle.properties` holds `JSONBIN_BIN_ID`, piped into `BuildConfig`. Leave it blank
  to run on the bundled word list only.
- `config/GameConfig.kt` is the single source of truth for every tunable: child
  profiles, points, the streak-bonus formula, animation durations, session sizes and
  all font sizes.
- `ui/UiStrings.kt` holds all Hebrew UI copy and the category-label map. The app
  deliberately does not use `strings.xml` — it is single-language by design.

## Word lists

Words live as a bare JSON array per child (`jsonbin_upload.json`, `roni_upload.json`),
pushed to that child's JSONBin bin. See [JSONBIN_UPLOAD_GUIDE.md](JSONBIN_UPLOAD_GUIDE.md)
for the upload steps and the bare-array gotcha (JSONBin wraps stored content under
`record`, so storing an already-wrapped object double-wraps it and breaks parsing).

## Project layout

```
app/src/main/java/com/wordmatch/
  MainActivity.kt      # builds everything by hand, holds the ViewModel
  config/GameConfig.kt # all tunables
  game/                # GameViewModel (app flow) + AnswerVerifier (matching)
  data/                # WordRepository (JSONBin + asset fallback), ScoreStore (prefs)
  model/               # WordItem, GameState, Deck
  network/             # JsonBinApi
  ui/                  # Start / Game / Summary / Album screens, strings, effects
```

Tests are split in two: `src/test/` for pure-JVM logic and `src/androidTest/` for
Compose UI flows against a deterministic 5-word fixture.
