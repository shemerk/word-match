# Mascot Sprite Prompts — 5 Tiers

Prompts to (re)generate the soccer-player mascot sprites for `res/drawable/mascot_tier1…5.png`.
Each tier prompt is **self-contained** — paste one alone into any image AI and it works.

## Consistency rules (read first)

The five sprites must look like **the same boy** at different stages. To get that:

1. **Keep the STYLE + CHARACTER block identical** across all five (the first two sentences). Only
   swap the **equipment clause**.
2. **Use the same seed** for all five (any fixed number, e.g. `777`). Same seed + same character text
   = the same face/body, only the gear changes.
3. **Square (1:1)** output, ~1024px.
4. **Flat solid pale background** (mint green) — the app drops the sprite onto a rounded card, so a
   busy background looks wrong. No transparency needed.
5. Model that does cute 3D "Pixar" renders works best (Flux, SDXL, Midjourney, DALL·E, etc.).

**Character + style block (shared):**
> cute chibi kid soccer player, same boy character, round face, big friendly smile, short brown hair,
> 3D Pixar-style render, soft studio lighting, centered full body, flat pale mint-green background

**Negative prompt (for tools that support one):**
> extra limbs, deformed hands, ugly, scary, text, watermark, logo, busy background, multiple people,
> adult, realistic photo, dark background

---

## Tier 1 — מתחיל (Beginner)
> cute chibi kid soccer player, same boy character, round face, big friendly smile, short brown hair,
> 3D Pixar-style render, soft studio lighting, centered full body, flat pale mint-green background,
> **barefoot, plain white t-shirt and white shorts, empty hands, no ball, no shoes, no equipment,
> shy beginner pose**

## Tier 2 — שחקן מגרש (Field Player)
> cute chibi kid soccer player, same boy character, round face, big friendly smile, short brown hair,
> 3D Pixar-style render, soft studio lighting, centered full body, flat pale mint-green background,
> **blue and white soccer jersey and shorts, red soccer cleats, holding a soccer ball under one arm,
> keen pose**

## Tier 3 — כוכב עולה (Rising Star)
> cute chibi kid soccer player, same boy character, round face, big friendly smile, short brown hair,
> 3D Pixar-style render, soft studio lighting, centered full body, flat pale mint-green background,
> **full blue team kit with shorts and long socks, bright yellow captain armband on the upper arm,
> red soccer cleats, confident hands-on-hips pose**

## Tier 4 — קפטן (Captain)
> cute chibi kid soccer player, same boy character, round face, big friendly smile, short brown hair,
> 3D Pixar-style render, soft studio lighting, centered full body, flat pale mint-green background,
> **goalkeeper outfit, green goalkeeper gloves on both hands, shin guards, professional kit,
> ready crouching goalkeeper stance**

## Tier 5 — אלוף (Champion)
> cute chibi kid soccer player, same boy character, round face, big friendly smile, short brown hair,
> 3D Pixar-style render, soft studio lighting, centered full body,
> **holding a large shiny golden trophy up high with both hands, gold medal around neck, full pro kit,
> bright stadium background with falling confetti, triumphant celebration pose**

*(Tier 5 intentionally drops the flat-mint background for a stadium — it's the payoff frame.)*

---

## ComfyUI commands (local, one per tier)

Server must be running (`C:\dev\life-reset-game\scripts\start_comfy_server.bat`). Same seed 777.

```bash
GEN="/c/dev/life-reset-game/scripts/comfy_generate.py"
OUT="/c/dev/english-learning-app/app/src/main/res/drawable"

python "$GEN" "cute chibi kid soccer player, same boy character, round face, big friendly smile, short brown hair, 3D Pixar-style render, soft studio lighting, centered full body, flat pale mint-green background, barefoot, plain white t-shirt and white shorts, empty hands, no ball, no equipment, shy beginner pose" "$OUT/mascot_tier1.png" --model flux --size 1024 --seed 777

python "$GEN" "cute chibi kid soccer player, same boy character, round face, big friendly smile, short brown hair, 3D Pixar-style render, soft studio lighting, centered full body, flat pale mint-green background, blue and white soccer jersey and shorts, red soccer cleats, holding a soccer ball under one arm, keen pose" "$OUT/mascot_tier2.png" --model flux --size 1024 --seed 777

python "$GEN" "cute chibi kid soccer player, same boy character, round face, big friendly smile, short brown hair, 3D Pixar-style render, soft studio lighting, centered full body, flat pale mint-green background, full blue team kit with shorts and long socks, bright yellow captain armband on the upper arm, red soccer cleats, confident hands-on-hips pose" "$OUT/mascot_tier3.png" --model flux --size 1024 --seed 777

python "$GEN" "cute chibi kid soccer player, same boy character, round face, big friendly smile, short brown hair, 3D Pixar-style render, soft studio lighting, centered full body, flat pale mint-green background, goalkeeper outfit, green goalkeeper gloves on both hands, shin guards, professional kit, ready crouching goalkeeper stance" "$OUT/mascot_tier4.png" --model flux --size 1024 --seed 777

python "$GEN" "cute chibi kid soccer player, same boy character, round face, big friendly smile, short brown hair, 3D Pixar-style render, soft studio lighting, centered full body, holding a large shiny golden trophy up high with both hands, gold medal around neck, full pro kit, bright stadium background with falling confetti, triumphant celebration pose" "$OUT/mascot_tier5.png" --model flux --size 1024 --seed 777
```

## Notes / gotchas

- **Flux ignores negative prompts** (flow-matching model). The tier-1 "no ball" was ignored last run —
  a ball snuck in. If it matters, either regenerate with a different seed, or crop it out by hand.
- If a tier's character drifts (different face), it's the seed — re-run that one tier with the SAME
  seed as the others until it matches.
- Sprites render **off-centre** sometimes (character to one side). The app frames them in a fixed
  square, so pick a render where the kid is reasonably centred.
- Filenames must stay lowercase `mascot_tierN.png` — Android resource names forbid dashes/caps.
