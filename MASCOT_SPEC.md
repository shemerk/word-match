# Mascot Spec — Soccer Player That Levels Up (Todo #4)

A persistent soccer-player avatar that starts weak at level 1 and grows stronger — better kit,
better equipment — as the child accumulates lifetime points across many play sessions. Level-ups
get progressively harder (10, then 20, 40, 80… points per level). This is a **feature**, not a
one-liner; this doc is the plan, no code shipped yet.

## Goal / feel

The reward loop today is per-session (best score to beat). The mascot adds a **long-term** loop the
child returns to: "my player got new boots." It survives app restarts and is independent of the
per-session-size high scores.

---

## 1. Persistence — lifetime points

Add to [ScoreStore](app/src/main/java/com/wordmatch/data/ScoreStore.kt) (and both impls
`PrefsScoreStore` / `InMemoryScoreStore`):

```kotlin
fun totalPoints(): Int
fun addPoints(delta: Int)
```

- SharedPreferences key `"total_points"`, global (NOT namespaced per session-size like the scores).
- **Decision — keep on reset:** `resetAll()` today clears best scores. It must **not** wipe the
  mascot, or a kid loses their player by clearing high scores. Give the mascot its own
  `resetProgress()` (or just never reset it). Confirm with the user.

## 2. Level math — config-driven

Costs double each level: L1→L2 = 10, L2→L3 = 20, L3→L4 = 40, L4→L5 = 80…
Cumulative thresholds: 0, 10, 30, 70, 150, 310…

Add to [GameConfig](app/src/main/java/com/wordmatch/config/GameConfig.kt), fully documented:

```kotlin
/** Points needed for the first level-up (level 1 -> 2). */
const val LEVEL_BASE_COST = 10
/** Each level costs this many times the previous one (2 = doubles every level). */
const val LEVEL_GROWTH = 2

/** Level (1-based) for a given lifetime point total. Level 1 = [0, BASE), etc. */
fun levelFor(totalPoints: Int): Int { /* subtract BASE, BASE*GROWTH, … until it doesn't fit */ }

/** Points earned inside the current level and points that level needs, for a progress bar.
 *  e.g. total 45 -> level 3, (15 into this level, of 40 needed). */
fun levelProgress(totalPoints: Int): Pair<Int, Int>
```

`levelFor` is the single source of truth — UI, badge, and sprite tier all derive from it. Cap the
tier art at the last defined tier (a level-99 kid still shows the top sprite).

## 3. Wiring into the game

In [GameViewModel.checkAnswer()](app/src/main/java/com/wordmatch/game/GameViewModel.kt) correct
branch, after computing `gained`:

```kotlin
store.addPoints(gained)
```

Expose on [GameState](app/src/main/java/com/wordmatch/model/Models.kt):
`totalPoints`, `level`, and `levelProgress` (into/needed). Recompute from `store.totalPoints()` when
entering START and after each correct answer. Detect **level-up** by comparing `level` before/after
the point add → drives the celebration (below).

## 4. UI — placement & UX

Two surfaces, one shared `MascotTier` derived from `levelFor()`:

- **StartScreen — the trophy case (primary).** Big centered mascot sprite + Hebrew rank title + a
  progress bar to the next level ("עוד 25 נקודות לרמה הבאה"). This is the screen the child lingers
  on between sessions; it's where the payoff lives.
- **GameScreen header — compact (secondary).** Small avatar + level badge sitting next to the
  existing 🔥 streak / points row in
  [HeaderStats](app/src/main/java/com/wordmatch/ui/GameScreen.kt#L138). Keeps the goal visible
  mid-play without stealing the word card's focus.

**Level-up moment (mid-session):** when `level` increases after a correct answer, pop the header
avatar (scale bounce), briefly show "עלית רמה!" and play a fanfare — reuse a success variant from
todo #3, or reserve one clip as the dedicated level-up sound. **Respect** `rememberReducedMotion()`
(already used in [Effects.kt](app/src/main/java/com/wordmatch/ui/Effects.kt)) — no bounce when
reduced motion is on, same as confetti/shake.

## 5. Style — soccer tiers

Titles in Hebrew, added to the `Ui` object in
[UiStrings.kt](app/src/main/java/com/wordmatch/ui/UiStrings.kt). Equipment escalates each tier:

| Level | Title (he) | Look / equipment |
|-------|------------|------------------|
| 1     | מתחיל       | barefoot, plain shirt, no ball |
| 2     | שחקן מגרש   | cleats + a ball |
| 3     | כוכב עולה   | full team kit, captain armband |
| 4     | קפטן        | gloves, pro kit, shin guards |
| 5+    | אלוף        | trophy + stadium background |

(Tier count is a config list — add rows without touching logic.)

## 6. Assets — start cheap, upgrade behind the same API

**Phase A (ship now, zero art):** map each tier to an **emoji/vector** combo — e.g.
🧒 → 🧒👟 → ⚽🧒 → 🧤 → 🏆. A `MascotTier` enum holds `title` + `emoji`, chosen by `levelFor()`.
No designer, no drawables, works today.

**Phase B (later):** drop in per-tier PNG/vector sprites in `res/drawable`, keyed by the *same*
`levelFor()` tier index. Zero logic change — only the rendering swaps emoji for `Image`. Don't build
Phase B until Phase A proves the loop is fun.

## 7. Files touched (Phase A estimate)

- `ScoreStore.kt` — `totalPoints`/`addPoints` (+ both impls).
- `GameConfig.kt` — level constants + `levelFor` / `levelProgress` + tier list.
- `GameViewModel.kt` — `addPoints` on correct; expose level/progress on state; level-up detection.
- `Models.kt` (GameState) — `totalPoints`, `level`, `levelProgress`.
- `UiStrings.kt` — rank titles + "level up" copy.
- New `ui/Mascot.kt` — the composable (big + compact variants), tier→emoji map.
- `StartScreen.kt` / `GameScreen.kt` — mount the mascot.

## 8. Tests

- `GameConfigTest` (new, JVM): `levelFor` and `levelProgress` at boundaries — 0→L1, 9→L1, 10→L2,
  29→L2, 30→L3, and a large value clamps to top tier. This is the one non-trivial branch; cover it.
- `GameViewModelTest`: after N correct answers, `store.totalPoints()` equals the summed `gained`,
  and `state.level` reflects `levelFor(total)`.

## Open questions for the user

1. Keep the mascot on "reset scores", or wipe it too? (Recommend: keep.)
2. Dedicated level-up sound, or reuse a success clip?
3. Emoji-first (Phase A) acceptable to ship, or wait for real sprites?
