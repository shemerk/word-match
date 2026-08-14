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

**State & flow** — `GameViewModel` owns the entire app flow through one `StateFlow<GameState>`. `GameState.screen` (`Screen.START | PLAYING | SUMMARY | ALBUM`) is the top-level router that `MainScreen` switches on. There is no navigation library. In-play status is expressed by booleans on `GameState` (`isAnswerCorrect: Boolean?`, `forfeited`) plus `checkNonce` (bumped on every answer so the UI can re-fire confetti/shake even on a repeated result). A session is finite: `startSession()` filters the word pool by category, then `advance()` counts `wordsCompleted` up to `sessionSize` before routing to `SUMMARY`.

**Child profiles** — the app has multiple children (`GameConfig.CHILDREN`, a list of `Child(id, displayName, binId)`), each with **their own dictionary (bin), high scores, and card album**. The start screen has a child-selector chip row (shown only with 2+ profiles); `GameViewModel.setChild(id)` swaps the active `Child`, points `ScoreStore` at that child's namespace, and reloads their bin. The choice is persisted (`ScoreStore.activeChildId`) and restored on launch. Add a child by appending to `CHILDREN`. Oren's `binId` is `BuildConfig.JSONBIN_BIN_ID` (blank ⇒ bundled only); Roni's is a literal bin id in `CHILDREN`.

**Data loading** — `WordRepositoryImpl.loadWords(binId)` takes the **active child's bin as an argument** (not baked into the constructor, so switching child switches dictionary at runtime), tries that JSONBin bin (only if non-blank), and **falls back to bundled `assets/words.json` on any failure or empty result**, so the app always has words. Note the fallback asset is Oren's set — a non-Oren child offline shows Oren's words (acceptable degradation; there is no per-child bundled asset). The repo keeps a bare-array source file per bin-backed child, the shape to push to that child's JSONBin:
- `app/src/main/assets/words.json` — wrapped `{"record":[...]}` (parsed by `JsonBinResponse`, Oren's offline fallback).
- `jsonbin_upload.json` (repo root) — **bare array**, Oren's bin source.
- `roni_upload.json` (repo root) — **bare array**, Roni's bin source (bin-only, no bundled asset).

  Bare-array matters: JSONBin's `GET /v3/b/{id}/latest` wraps stored content under `record`, so storing the wrapped object double-wraps and breaks parsing. See `JSONBIN_UPLOAD_GUIDE.md`.

**Persistence** — `ScoreStore` (impl `PrefsScoreStore`, `InMemoryScoreStore` for tests) saves best score + best streak **per session-size bucket** (10/20/50/100), plus the lifetime card collection — **all namespaced by active child** (`setActiveChild`/`activeChildId`; keys prefixed by child id). Scores/streaks live in `wordmatch_scores`; the sound on/off pref and the remembered active child are global (unprefixed). The card collection — `totalPoints()` and unlocked card ids — is kept in a **separate `wordmatch_player` prefs file** so `resetAll()` can never wipe it. `resetAll` now removes only the **active child's** per-size keys (not a blanket `clear()`, which would wipe both kids). "Reset scores" (`resetScores`) and "reset card collection" (`resetCollection`, which zeroes points **and** cards so points can't immediately re-award them) are deliberately separate, and both act on the active child only.

**Card collection** — the reward loop (replaced an earlier soccer-mascot leveling system). Every `GameConfig.POINTS_PER_CARD` (100) lifetime points unlocks one **random** soccer-player card. `model/Deck` is the single source of truth for the deck: 52 cards cut from grid images into `res/drawable/card_01..card_52` (01–26 Man United, 27–52 Racing), each card's name plate baked into the art (no separate name field). `GameViewModel.reconcileCards()` awards random unowned cards until the owned count matches `min(totalPoints/POINTS_PER_CARD, Deck.SIZE)` — idempotent, so a silent call on START back-fills anything earned while the app was closed, while the call inside `checkAnswer` (≤1 award per answer) sets `newCardId`/`newCardNonce` to fire the win reveal. `pickCard` is an injectable seam for deterministic tests. The **album** (`Screen.ALBUM`, reached from the start-screen album button) shows the whole deck: owned cards framed, not-yet-won ones as dimmed "?" slots.

**Answer checking** — `AnswerVerifier` is the single source for matching: case-insensitive, whitespace-collapsed, and a single **leading article (`a/an/the/to`) is optional on either side** (so `cat` == `A cat`, `run` == `To run`). `hint()` reuses the same normalization to mask the answer.

**Config** — `GameConfig` is the single source of truth for every tunable: the `CHILDREN` list (id / display name / bin per child), points, streak-bonus formula (`streakBonus()`, capped), animation durations, session sizes, and **all font sizes** (never hardcode a text size in a composable). `ui/UiStrings.kt` (`Ui` object) is the single source for all Hebrew UI copy and the English→Hebrew category-label map — the app deliberately does **not** use `strings.xml` (single language, no localization needed).

**UI** — `MainScreen` (router + RTL wrapper + settings dialog) → `StartScreen` / `GameScreen` / `SummaryScreen` / `AlbumScreen`. `StartScreen` shows the child-selector chip row at the top (reuses `SelectChip`, one chip per `GameConfig.CHILDREN` entry, calling `setChild`). `AlbumScreen.kt` also holds the start-screen `AlbumButton`, the card grid + zoom, and `CardRevealDialog` — the reveal is rendered **globally in `MainScreen`** (keyed on `GameState.newCardId`), not inside `GameScreen`, so a card won on the last word of a session still shows even though play routes straight to the summary. The whole tree is forced RTL via `CompositionLocalProvider(LocalLayoutDirection provides Rtl)`; the English answer field overrides to `TextDirection.Ltr`. `Effects.kt` holds the confetti canvas and `rememberReducedMotion()` (reads `Settings.Global.ANIMATOR_DURATION_SCALE == 0`); confetti/shake/flip and the reveal flip are suppressed when reduced motion is on.

## Testing

Two suites, deliberately split (user chose "JVM logic now, instrumentation later"):
- `src/test/` — pure-JVM logic (`AnswerVerifierTest`, `GameViewModelTest`). `GameViewModelTest` drives the ViewModel with a fake repo, `InMemoryScoreStore`, an injected sequential `pickNext` for determinism, and `StandardTestDispatcher`.
- `src/androidTest/` — 8 Compose UI flows in `WordMatchGameTest`, loading the deterministic 5-word `src/androidTest/assets/words_test.json`. They build a `GameViewModel` with an injected sequential picker so "next word" is reproducible, and drive through the start screen. Assertions match on literal Hebrew text / the `answerInput` test tag.

`GameViewModel`'s constructor exposes `sound`, `store`, `pickNext`, and `pickCard` seams specifically so both suites can inject test doubles (`pickCard` makes card awards deterministic). `WordMatchGameTest` pre-owns one card in its store so a full session's lifetime points can't pop the card-win reveal over the UI mid-flow.

## External resources

- JSONBin.io bins (word-list hosting): https://jsonbin.io/app/bins
