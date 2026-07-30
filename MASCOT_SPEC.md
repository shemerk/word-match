# Mascot Spec — Soccer Player That Levels Up (Todo #4)

A persistent soccer-player avatar that starts weak at level 1 and grows stronger — better kit,
better equipment — as the child accumulates lifetime points across many play sessions. Level-ups
get progressively harder (10, then 20, 40, 80… points per level). This is a **feature**, not a
one-liner; this doc is the plan, no code shipped yet.

## Goal / feel

The reward loop today is per-session (best score to beat). The mascot adds a **long-term** loop the
child returns to: "my player got new boots." It survives app restarts and is independent of the
per-session-size high scores. The hook is not the badge — it's **anticipation** ("just 15 more
points!") and **ownership** ("that's *my* guy") — see §4a.

## Decisions (locked with the user)

1. **Reset keeps the mascot.** `resetAll()` clears best scores only. Lifetime points live under a
   separate key and get their own `resetProgress()`, never touched by score reset. A kid clearing
   high scores must not lose their player.
2. **Dedicated level-up sound.** File added: `app/src/main/res/raw/level_up.mp3`.
   ⚠️ It was delivered as `level-up.mp3`; **Android resource names forbid dashes** (see the
   SoundManager comment: "lowercase, no dashes"), so it's been renamed to `level_up.mp3` →
   `R.raw.level_up`. Don't reintroduce the dash.
3. **Real sprites from day one — no emoji phase.** Tier art is generated with the local ComfyUI
   install (§6). The old "emoji-first Phase A" is dropped; there is one phase, shipping real art.
4. **Slow curve — `LEVEL_BASE_COST = 100`** (not 10). The mascot is a *long-term* loop: a good
   10-word session earns ~100–170 pts, so the top art tier takes ~10 sessions, not one. Cumulative
   thresholds are now 0, 100, 300, 700, 1500, 3100…. `GameConfigTest` derives from the constants, so
   this stays the single knob to retune pace.

---

## 1. Persistence — lifetime points

Add to [ScoreStore](app/src/main/java/com/wordmatch/data/ScoreStore.kt) (and both impls
`PrefsScoreStore` / `InMemoryScoreStore`):

```kotlin
fun totalPoints(): Int
fun addPoints(delta: Int)
fun resetProgress()   // wipes lifetime points ONLY; separate from resetAll()
```

- SharedPreferences key `"total_points"`, global (NOT namespaced per session-size like the scores).
- `resetAll()` must **not** touch `"total_points"`. Settings can expose `resetProgress()` separately
  (or not at all) — but the two must never be wired together.

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

`levelFor` is the single source of truth — sprite tier, badge, rank title, and progress bar all
derive from it. **Art tier is capped at the last defined sprite; the level number is not** — see the
prestige stars in §4a(5). A level-99 kid shows the top sprite plus a star count.

## 3. Wiring into the game

In [GameViewModel.checkAnswer()](app/src/main/java/com/wordmatch/game/GameViewModel.kt) correct
branch, after computing `gained`:

```kotlin
store.addPoints(gained)
```

Expose on [GameState](app/src/main/java/com/wordmatch/model/Models.kt):
`totalPoints`, `level`, and `levelProgress` (into/needed). Recompute from `store.totalPoints()` when
entering START and after each correct answer. Detect **level-up** by comparing `level` before/after
the point add → drives the celebration (§4a(2)) and `sound.playLevelUp()`.

## 4. UI — placement & UX

Two surfaces, one shared `MascotTier` derived from `levelFor()`:

- **StartScreen — the trophy case (primary).** Big centered mascot sprite + Hebrew rank title + a
  progress bar to the next level. This is the screen the child lingers on between sessions; it's
  where the payoff lives. Removing the category chips (§5) frees the vertical space this needs.
- **GameScreen header — compact (secondary).** Small avatar + level badge sitting next to the
  existing 🔥 streak / points row in
  [HeaderStats](app/src/main/java/com/wordmatch/ui/GameScreen.kt#L138). Keeps the goal visible
  mid-play without stealing the word card's focus.

## 4a. Engagement — the "wow" (why a kid comes back)

A progress bar that ticks up is not a reward loop. These five turn it into one. **(1), (2), (5) ship
now** (near-free, they ride existing plumbing); **(3), (4) are follow-ups** once the art proves the
loop is fun.

**(1) Next-unlock preview — ship now. Cheapest, biggest.**
On StartScreen, under the mascot, show the *next* tier's reward, teased: the next sprite shown
locked/greyed + Hebrew "עוד 15 נקודות → נעליים מוזהבות". Anticipation outpulls reward; the
near-miss framing ("just 15 more!") is the strongest draw in kid games. You already compute
`levelProgress` — this is a label + a dimmed `Image`. When the child is on the top tier, swap the
teaser for the prestige-star goal (5).

**(2) Mascot reacts live — ship now.**
The character responds to *this* answer, not just the lifetime total:
- correct → quick cheer/jump (scale bounce);
- level-up → bigger pop + "עלית רמה!" + `playLevelUp()`;
- forfeit → brief slump.
Ties the avatar to the moment. Reuse the confetti/shake plumbing and **respect
`rememberReducedMotion()`** ([Effects.kt](app/src/main/java/com/wordmatch/ui/Effects.kt)) — no
bounce when reduced motion is on, same rule as confetti/shake. A single alternate "cheer" sprite per
tier is optional polish; a scale-bounce of the base sprite is enough to ship.

**(5) Infinite prestige tail — ship now. 3-line cap safety-net.**
Past the top art tier, keep leveling with a ⭐ counter on the badge/title ("אלוף ⭐⭐⭐"). `levelFor`
is already uncapped; just render `level − topTierLevel` as stars above the sprite. Without this the
loop dies the moment a keen kid maxes the 5 tiers — the single most important fix to the original
plan.

**(3) Name your player + pick a jersey — follow-up.**
One-time prompt: kid types a name and taps one of ~4 jersey colours (store two strings in prefs).
Then it's "**דני** עלה לרמה 3". Ownership is the biggest retention lever there is. Needs a small
dialog + a config colour list; defer until the base loop is in.

**(4) Collectible shelf — follow-up.**
Decouple *collectibles* from the evolving sprite: every few levels drop a named keepsake (ball,
boots, trophy, stadium) onto a little shelf on StartScreen that persists. Kids re-open apps to look
at their stuff. Config list keyed by level. Needs its own art set; defer with (3).

## 5. Remove the category selector (task)

The mascot needs the vertical real-estate the category chips occupy, and "כל הקטגוריות" (all) is the
right default anyway. **Remove the category UI; keep every word in play.**

- In [StartScreen.kt](app/src/main/java/com/wordmatch/ui/StartScreen.kt), delete the
  `SectionLabel(Ui.CATEGORY_LABEL)` + its `FlowRow` of chips (currently lines ~58–64). Put the
  mascot trophy-case block there instead.
- `state.category` stays `null`, so `startSession()`'s `pool = if (s.category == null) allWords …`
  keeps loading the full pool — no logic change needed.
- `GameViewModel.setCategory()`, `state.categories`, and `Ui.CATEGORY_*` become unused.
  **ponytail:** leave the ViewModel seam dormant (it's covered by tests, harmless) or delete it in a
  separate cleanup pass — don't let dead-code removal balloon this task. Do delete the now-unused
  `Ui.CATEGORY_LABEL` / `Ui.CATEGORY_ALL` strings if nothing else references them.

## 6. Style — soccer tiers & art

Titles in Hebrew, added to the `Ui` object in
[UiStrings.kt](app/src/main/java/com/wordmatch/ui/UiStrings.kt). Equipment escalates each tier:

| Level | Title (he) | drawable            | Look / equipment |
|-------|------------|---------------------|------------------|
| 1     | מתחיל       | `mascot_tier1.png`  | barefoot, plain shirt, no ball |
| 2     | שחקן מגרש   | `mascot_tier2.png`  | cleats + a ball |
| 3     | כוכב עולה   | `mascot_tier3.png`  | full team kit, captain armband |
| 4     | קפטן        | `mascot_tier4.png`  | gloves, pro kit, shin guards |
| 5+    | אלוף        | `mascot_tier5.png`  | trophy + stadium background (+ ⭐ per level over 5) |

Tier list is config-driven — a `MascotTier` enum/list holding `title` + `drawableRes`, chosen by
`levelFor()`. Add a row + a PNG to extend; no logic change.

### Generating the sprites — ComfyUI

ComfyUI is installed locally. Generator script + models live in the sibling project
`c:\dev\life-reset-game`.

1. **Start the server** (once, leave running): `C:\dev\life-reset-game\scripts\start_comfy_server.bat`
   → serves `http://127.0.0.1:8188`.
2. **Generate each tier** with `comfy_generate.py`. Use **`--model flux`** — its 3D-render aesthetic
   gives the friendly Pixar-ish look kids read as "cute", far better than the `juggernaut` dark-
   fantasy card style. Keep a **fixed `--seed`** across all five so the same character recurs at
   every tier (only the kit changes), and generate square:

   ```bash
   python "C:\dev\life-reset-game\scripts\comfy_generate.py" \
     "cute chibi kid soccer player, barefoot, plain white shirt, big friendly smile, \
      3D Pixar-style render, soft studio lighting, centered, flat pale mint background" \
     "C:\tmp\mascot_tier1.png" --model flux --size 1024 --seed 777
   ```

   Repeat for tiers 2–5, same seed, swapping only the equipment clause to match the table
   (tier2 "cleats and a soccer ball", tier3 "full team kit and captain armband", tier4 "goalkeeper
   gloves, shin guards, pro kit", tier5 "holding a golden trophy, stadium background, confetti").
3. **Background:** prompt a **flat solid pale colour** (as above) so the sprite drops cleanly onto a
   rounded card in the UI — the script has no alpha/cut-out step. If a true transparent cut-out is
   wanted later, that's a manual/follow-up step; a solid card background is enough to ship.
4. **Install:** drop the five PNGs into `app/src/main/res/drawable/` as `mascot_tier1…5.png` and
   reference by `R.drawable.mascot_tierN`. **ponytail:** one PNG each in plain `drawable/` (Compose
   scales it) — skip density buckets for a single-device hobby app.
5. Review each render; regen with a tweaked prompt (same seed) until the character is consistent and
   clearly escalates in gear tier to tier.

## 7. Sound

Add to [SoundManager](app/src/main/java/com/wordmatch/util/SoundManager.kt):

```kotlin
interface SoundManager {
    fun playCorrect(); fun playWrong(); fun playLevelUp()   // new
}
```

`AndroidSoundManager` loads `R.raw.level_up` into the existing `SoundPool` and plays it on level-up;
`NoOpSoundManager` gets an empty override. Gate it on the same sound-on pref as the others.

## 8. Files touched

- `ScoreStore.kt` — `totalPoints` / `addPoints` / `resetProgress` (+ both impls).
- `GameConfig.kt` — level constants + `levelFor` / `levelProgress` + tier list + any new font sizes
  (rank title, "next unlock" label) — **font sizes go in config, never hardcoded**.
- `GameViewModel.kt` — `addPoints` on correct; expose level/progress on state; level-up detection →
  `playLevelUp()`.
- `Models.kt` (GameState) — `totalPoints`, `level`, `levelProgress`.
- `SoundManager.kt` — `playLevelUp()` (+ interface, both impls).
- `UiStrings.kt` — rank titles, "עלית רמה!", "עוד N נקודות → …"; remove unused `CATEGORY_*`.
- New `ui/Mascot.kt` — the composable (big trophy-case + compact header variants), tier lookup,
  next-unlock teaser, reaction bounce.
- `StartScreen.kt` — remove category chips (§5); mount the big mascot + progress + teaser.
- `GameScreen.kt` — mount the compact avatar/badge; wire the reaction to correct/level-up/forfeit.
- `app/src/main/res/drawable/mascot_tier1…5.png` — generated sprites (§6).
- `app/src/main/res/raw/level_up.mp3` — already added.

## 9. Tests

- `GameConfigTest` (new, JVM): `levelFor` and `levelProgress` at boundaries — 0→L1, 9→L1, 10→L2,
  29→L2, 30→L3, and a large value: art tier clamps to top **while `levelFor` keeps climbing** (feeds
  the prestige stars). This is the one non-trivial branch; cover it.
- `GameViewModelTest`: after N correct answers, `store.totalPoints()` equals the summed `gained`, and
  `state.level` reflects `levelFor(total)`; crossing a threshold flips the level-up detection.
- `ScoreStoreTest` (or extend existing): `resetAll()` leaves `totalPoints()` intact; `resetProgress()`
  zeroes it. This guards Decision 1 — the "kid loses their player" regression.
