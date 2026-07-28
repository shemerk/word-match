# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

**WordMatch** — Android (Kotlin + Jetpack Compose) flashcard game teaching English to Hebrew-speaking kids. UI is 100% Hebrew, full RTL. Game direction is fixed: **show the Hebrew word, the child types the English translation**, checked against `WordItem.english`. See `DESIGN_PLAN_EnglishHebrewApp.md` for the original brief (note: the plan predates the direction decision and is internally inconsistent about it — the code is the source of truth).

## Commands

Uses the Gradle wrapper (Gradle 8.11.1, AGP 8.7.3, Kotlin 2.0.21, JDK 17). Run from repo root.

```bash
./gradlew testDebugUnitTest        # JVM unit tests — run these, they need no device
./gradlew assembleDebug            # builds app/build/outputs/apk/debug/learn-english.apk
./gradlew assembleDebugAndroidTest # compiles the instrumentation suite without running it
./gradlew connectedAndroidTest     # runs the 8 Compose UI tests — needs a booted emulator/device
```

Run a single JVM test:
```bash
./gradlew testDebugUnitTest --tests "com.wordmatch.AnswerVerifierTest"
./gradlew testDebugUnitTest --tests "com.wordmatch.GameViewModelTest.streakBonusIsCappedAndAdded"
```

Install / launch on a running emulator (adb is on PATH via `$ANDROID_HOME/platform-tools`):
```bash
adb install -r app/build/outputs/apk/debug/learn-english.apk
adb shell am start -n com.wordmatch/.MainActivity
```

- The APK is always named `learn-english.apk` (set via `applicationVariants` in `app/build.gradle.kts`).
- `gradle.properties` holds `JSONBIN_BIN_ID` (piped into `BuildConfig`); blank = bundled words only.
- Emulator tip: the **first tap after a cold launch is often swallowed** — tap twice when scripting via `adb shell input tap`.

## Architecture

Single-activity Compose app. No DI framework — `MainActivity` constructs everything by hand and holds it in a `GameViewModel` via a `ViewModelProvider.Factory` (survives rotation).

**State & flow** — `GameViewModel` owns the entire app flow through one `StateFlow<GameState>`. `GameState.screen` (`Screen.START | PLAYING | SUMMARY`) is the top-level router that `MainScreen` switches on. There is no navigation library. In-play status is expressed by booleans on `GameState` (`isAnswerCorrect: Boolean?`, `forfeited`) plus `checkNonce` (bumped on every answer so the UI can re-fire confetti/shake even on a repeated result). A session is finite: `startSession()` filters the word pool by category, then `advance()` counts `wordsCompleted` up to `sessionSize` before routing to `SUMMARY`.

**Data loading** — `WordRepositoryImpl.loadWords()` tries the JSONBin bin (only if `JSONBIN_BIN_ID` is set) and **falls back to bundled `assets/words.json` on any failure or empty result**, so the app always has words. Words come from `~/Downloads/words.docx`; the repo keeps two shapes of the same 52 words:
- `app/src/main/assets/words.json` — wrapped `{"record":[...]}` (parsed by `JsonBinResponse`, used offline).
- `jsonbin_upload.json` (repo root) — **bare array**, the shape to paste into JSONBin. This matters: JSONBin's `GET /v3/b/{id}/latest` wraps stored content under `record`, so storing the wrapped object double-wraps and breaks parsing. See `JSONBIN_UPLOAD_GUIDE.md`.

**Persistence** — `ScoreStore` (impl `PrefsScoreStore`, `InMemoryScoreStore` for tests) saves best score + best streak **per session-size bucket** (10/20/50/100), namespaced keys, plus the sound on/off pref. This is the entire scope of "Phase 4" — no other post-MVP features exist.

**Answer checking** — `AnswerVerifier` is the single source for matching: case-insensitive, whitespace-collapsed, and a single **leading article (`a/an/the/to`) is optional on either side** (so `cat` == `A cat`, `run` == `To run`). `hint()` reuses the same normalization to mask the answer.

**Config** — `GameConfig` is the single source of truth for every tunable: points, streak-bonus formula (`streakBonus()`, capped), animation durations, session sizes, and **all font sizes** (never hardcode a text size in a composable). `ui/UiStrings.kt` (`Ui` object) is the single source for all Hebrew UI copy and the English→Hebrew category-label map — the app deliberately does **not** use `strings.xml` (single language, no localization needed).

**UI** — `MainScreen` (router + RTL wrapper + settings dialog) → `StartScreen` / `GameScreen` / `SummaryScreen`. The whole tree is forced RTL via `CompositionLocalProvider(LocalLayoutDirection provides Rtl)`; the English answer field overrides to `TextDirection.Ltr`. `Effects.kt` holds the confetti canvas and `rememberReducedMotion()` (reads `Settings.Global.ANIMATOR_DURATION_SCALE == 0`); confetti/shake/flip are suppressed when reduced motion is on.

## Testing

Two suites, deliberately split (user chose "JVM logic now, instrumentation later"):
- `src/test/` — pure-JVM logic (`AnswerVerifierTest`, `GameViewModelTest`). `GameViewModelTest` drives the ViewModel with a fake repo, `InMemoryScoreStore`, an injected sequential `pickNext` for determinism, and `StandardTestDispatcher`.
- `src/androidTest/` — 8 Compose UI flows in `WordMatchGameTest`, loading the deterministic 5-word `src/androidTest/assets/words_test.json`. They build a `GameViewModel` with an injected sequential picker so "next word" is reproducible, and drive through the start screen. Assertions match on literal Hebrew text / the `answerInput` test tag.

`GameViewModel`'s constructor exposes `sound`, `store`, and `pickNext` seams specifically so both suites can inject test doubles.

## External resources

- JSONBin.io bins (word-list hosting): https://jsonbin.io/app/bins
