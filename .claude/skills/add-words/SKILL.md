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
7. If `JSONBIN_BIN_ID` is set in `gradle.properties`, push `jsonbin_upload.json` to JSONBin automatically:
   ```powershell
   $key = 'PASTE_KEY_HERE'  # see below — read from env or ask user
   $body = [System.IO.File]::ReadAllText('jsonbin_upload.json', [System.Text.Encoding]::UTF8)
   Invoke-RestMethod -Uri "https://api.jsonbin.io/v3/b/$binId" -Method Put `
     -Headers @{ 'X-Master-Key' = $key; 'Content-Type' = 'application/json; charset=utf-8' } `
     -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
   ```
   Read `JSONBIN_BIN_ID` from `gradle.properties`. Read `JSONBIN_MASTER_KEY` from the
   `JSONBIN_MASTER_KEY` environment variable (`$env:JSONBIN_MASTER_KEY`). If the env var is
   missing, ask the user to paste their JSONBin Master Key (starts with `$2a$` or `$2b$`), then
   run the upload. Confirm success or report the error.

If the user gives only Hebrew+English (no category), infer a sensible existing category and tell
them what you picked.

## Notes

- Don't renumber or reorder existing rows.
- Blank `JSONBIN_BIN_ID` = bundled words only, skip the upload step entirely.
