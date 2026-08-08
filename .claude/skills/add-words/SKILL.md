---
name: add-words
description: Add a batch of new Hebrew→English vocabulary words to WordMatch. Appends rows to BOTH app/src/main/assets/words.json (wrapped "record") and jsonbin_upload.json (bare array), auto-assigning ids and the next batch number so the new words become the app's selectable "new words" set. Use when the user says "add words", "new words batch", "add vocabulary", or pastes Hebrew/English pairs to add.
---

# Add words to WordMatch

Adds a batch of vocabulary. New words get the **next batch number**, which makes them the
app's "new words" set on the start screen (the newest batch auto-demotes the previous one).

## The two files — keep them in sync

The same words live in two shapes (this is a known footgun — see CLAUDE.md):

- `app/src/main/assets/words.json` — **wrapped**: `{ "record": [ ...items ] }` (offline/bundled).
- `jsonbin_upload.json` (repo root) — **bare array**: `[ ...items ]` (paste target for JSONBin).

Every add must update **both**. They must end up with the identical item list.

## Item shape

```json
{ "id": 79, "hebrew": "כלב", "english": "A dog", "category": "animals", "batch": 1 }
```

- `id` — integer, unique. Next id = current max id + 1, incrementing per word.
- `hebrew` — the prompt shown to the child. Multiple accepted forms separated by ", " (e.g. `"אהבה, אוהב"`).
- `english` — the checked answer. A single leading article (`a`/`an`/`the`/`to`) is optional on
  either side (AnswerVerifier handles it), so `"A dog"` also accepts `dog`. Match the style of
  existing rows (verbs as `"To run"`, nouns as `"A cat"`).
- `category` — lowercase English key. Reuse an existing key when it fits (see the category list in
  words.json / the CATEGORY_HE map in `ui/UiStrings.kt`). A new key falls back to showing the raw
  key, so prefer an existing one unless none fits.
- `batch` — **all words in one add share the SAME batch = current max batch + 1.** Never edit the
  batch of existing rows.

## Steps

1. Read `app/src/main/assets/words.json`. Find the current **max `id`** and **max `batch`**.
2. `nextBatch = maxBatch + 1`. Starting id = `maxId + 1`.
3. Build the new item objects (ids incrementing, all with `batch = nextBatch`).
4. Append them to the `record` array in `words.json` (before the closing `]`).
5. Append the **same** items to the bare array in `jsonbin_upload.json`.
6. Show the user the added rows and the batch number.

If the user gives only Hebrew+English (no category), infer a sensible existing category and tell
them what you picked.

## Notes

- If `JSONBIN_BIN_ID` in `gradle.properties` is set, remind the user that live play reads JSONBin
  until it fails — the words.json change only shows offline unless they also paste
  `jsonbin_upload.json` into the bin. Blank id = bundled words only, nothing extra to do.
- Don't renumber or reorder existing rows.
