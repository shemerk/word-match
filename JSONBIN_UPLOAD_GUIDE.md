# JSONBin.io Upload Guide (WordMatch)

The app **already works offline** using the bundled 52 words in
[app/src/main/assets/words.json](app/src/main/assets/words.json). JSONBin is only needed if you
want to update the word list **without rebuilding the app**. Do this whenever you like.

---

## Important: upload the *bare array*, not the wrapped object

JSONBin's read endpoint `GET /v3/b/{id}/latest` returns your stored content **wrapped** in a
`record` field:

```
you store:   [ {...}, {...} ]           <-- a bare JSON array
you get back: { "record": [ {...}, {...} ], "metadata": {...} }
```

The app expects exactly that `{ "record": [...] }` response. So upload the **array only**.
A ready-to-paste file is provided: **[jsonbin_upload.json](jsonbin_upload.json)** (the 52 words as a
bare array). Do **not** upload `words.json` (that one is wrapped for the bundled/offline path).

---

## Steps

1. **Create a free account** at https://jsonbin.io/ and log in.

2. **Create a bin**
   - Click **Create Bin** (the editor opens).
   - Delete the sample content.
   - Open **[jsonbin_upload.json](jsonbin_upload.json)**, copy **all** of it, paste into the editor.
   - Click **Save** (top-right).

3. **Make the bin PUBLIC** (so the app can read it with no API key)
   - In the bin view, find the **privacy / lock** toggle (bin is Private by default).
   - Switch it to **Public**.
   - Public bins are readable by anyone with the id — fine for a kids' word list.

4. **Copy the Bin ID**
   - It's the id segment in the bin URL, e.g. for
     `https://jsonbin.io/app/bins/6712abcd1234ef56789` the id is `6712abcd1234ef56789`.

5. **Verify the public read works** — paste this in a browser (replace the id):
   ```
   https://api.jsonbin.io/v3/b/YOUR_BIN_ID/latest
   ```
   You should see `{ "record": [ { "id": 1, "hebrew": "יש", "english": "Has", ... }, ... ] }`.
   If you instead get a 401/403, the bin isn't Public yet — redo step 3.

6. **Point the app at it** — open [gradle.properties](gradle.properties) and set:
   ```properties
   JSONBIN_BIN_ID=YOUR_BIN_ID
   ```

7. **Rebuild**
   ```bash
   ./gradlew assembleDebug
   ```
   On launch the app now fetches your bin; if the network fails it automatically falls back to the
   bundled `words.json`, so it never shows an empty screen.

---

## Updating words later

Edit the bin content in the JSONBin editor and **Save** — it creates a new version, and the app
reads `/latest`, so it picks up changes on next launch. No rebuild needed (the bin id doesn't
change). Keep uploading the **bare array** shape.

## Notes / data decisions

- One worksheet entry `Iglo` was corrected to the standard English spelling **`Igloo`** (Hebrew
  `איגלו`). Change it back in the bin if you want the worksheet spelling.
- A few Hebrew entries have two forms shown on the card (e.g. `זאת, זה`, `אהבה, אוהב`,
  `לגור, לחיות`). The child types the **English**, so this only affects display.
- The answer check accepts a leading `a / an / the / to` either way, is case-insensitive, and trims
  spaces — so `cat`, `a cat`, `A Cat` all pass for `A cat`, and `run` passes for `To run`.
