---
name: add-words
description: Add a batch of new Hebrew→English vocabulary words to WordMatch, for a specific child (Oren or Roni — each has their own dictionary/bin). Appends rows to that child's word file(s), auto-assigning ids and (for a new batch) the next batch number so the new words become that child's selectable "new words" set. Use when the user says "add words", "new words batch", "add vocabulary", or pastes Hebrew/English pairs to add.
---

# Add words to WordMatch

Adds a batch of vocabulary **to one child's dictionary**. New words get the **next batch number**,
which makes them that child's "new words" set on the start screen (the newest batch auto-demotes the
previous one).

## Which child?

Each child in `GameConfig.CHILDREN` has their own dictionary (bin), so words are added per child.
Ask which child if the user didn't say; default to **Oren**.

| Child | File(s) to update | Bin id (for upload) |
|-------|-------------------|---------------------|
| **Oren** | `app/src/main/assets/words.json` (wrapped) **and** `jsonbin_upload.json` (bare array) | `gradle.properties` → `JSONBIN_BIN_ID` |
| **Roni** | `roni_upload.json` (bare array) — bin-only, **no** bundled asset | `6a7f3b7fda38895dfee52cd0` |

A new child added to `CHILDREN` follows Roni's pattern: one `<id>_upload.json` bare array + their bin.

## The file shapes — keep a child's copies in sync

The same words can live in two shapes (this is a known footgun — see CLAUDE.md):

- **wrapped**: `{ "record": [ ...items ] }` (offline/bundled asset — Oren only).
- **bare array**: `[ ...items ]` (the push target for JSONBin — every bin-backed child).

For a child with both shapes (Oren), every add must update **both** to the identical item list.
For a bin-only child (Roni), update just the one bare-array file.

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

1. Determine the target child (see table above) and their file(s) + bin id.
2. Read that child's bare-array file (`jsonbin_upload.json` for Oren, `<id>_upload.json` otherwise).
   Find the current **max `id`** and **max `batch`**.
3. `nextBatch = maxBatch + 1`. Starting id = `maxId + 1`.
4. Build the new item objects (ids incrementing, all with `batch = nextBatch`).
5. Append them to the bare array in the child's `*_upload.json`.
6. **Oren only:** append the **same** items to the `record` array in `app/src/main/assets/words.json`.
7. Show the user the added rows and the batch number.
8. Push the child's bare-array file to their bin (see table for the bin id):
   ```powershell
   $binId = 'THE_CHILDS_BIN_ID'
   $file  = 'roni_upload.json'   # or jsonbin_upload.json for Oren
   $key = $env:JSONBIN_MASTER_KEY
   $body = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)
   Invoke-RestMethod -Uri "https://api.jsonbin.io/v3/b/$binId" -Method Put `
     -Headers @{ 'X-Master-Key' = $key; 'Content-Type' = 'application/json; charset=utf-8' } `
     -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
   ```
   For Oren, read the bin id from `gradle.properties` (`JSONBIN_BIN_ID`); blank ⇒ bundled only, skip
   the upload. Read `JSONBIN_MASTER_KEY` from the `JSONBIN_MASTER_KEY` environment variable. If the
   env var is missing, ask the user to paste their JSONBin Master Key (starts with `$2a$` or `$2b$`),
   then run the upload. Confirm success or report the error.

If the user gives only Hebrew+English (no category), infer a sensible existing category and tell
them what you picked.

## Notes

- Don't renumber or reorder existing rows. Ids are per child (each file starts at 1).
- Never mix children in one add — a batch targets exactly one child's dictionary.
- Oren with a blank `JSONBIN_BIN_ID` = bundled words only, skip the upload step entirely.
