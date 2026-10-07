# Shared fixtures (the parity contract)

Both plugins (`jetbrains/` and `vscode/`) must pass every case in this folder. Together with
`docs/FileMaker-Native-Clipboard-Analysis.md` and `shared/data/clipboard-formats.json`, these files
are the specification of how FMCuttingBoard handles FileMaker's clipboard. If a plugin disagrees
with a fixture, the plugin is wrong, unless the spec itself is being changed on purpose. In that
case, change the spec, the fixtures and **both** plugins in the same PR.

| Path | What it holds |
|---|---|
| `snippets/<type>/*.xml` | fmxmlsnippet samples, grouped by the snippet type they must be detected as |
| `fmcalc/*.fmcalc` | FileMaker calculation samples for the `.fmcalc` language support |
| `golden/snippets.generated.json` | Per-snippet expectations. **Generated**: run `node shared/tools/generate-golden.mjs` after adding or changing a snippet |
| `golden/cases.json` | Hand-written edge cases (detection, normalization, decoding, extraction, XML validation, file naming) |
| `golden/function-signatures.txt` | How each function in `shared/data/filemaker-functions.json` is displayed (name, category, return type, signatures, description). Update it deliberately when that data changes |
| `clipboard/<type>/<capture>/` | Raw bytes captured from real FileMaker (see below) |

Fixture text files must use LF line endings. `.gitattributes` enforces this, and the generator checks it.

## How a test harness must use the golden files

Function names refer to `FmClipboardCodec`, `ClipboardFormats` and `DefaultFileMakerClipboardParser`
in the JetBrains plugin, and to their 1:1 TypeScript ports in the VS Code extension.

**`snippets.generated.json`**: for each case, read `fixture` (relative to this folder) as UTF-8 text, then:

1. `detectSnippetType(text)` must equal `expect.snippetType`.
2. `windowsFormatNames(type)` must equal `expect.windowsFormats`.
3. `encodeCustomFormatPayload(text)` must have `expect.customFormatPayload.length`/`sha256`/`headHex`.
   The same must hold for the CRLF version of the text (every `\n` replaced by `\r\n`), because the
   payload always uses LF.
4. `utf16leNullTerminated(text)` must match `expect.unicodeText.lf`, and the CRLF version must match `expect.unicodeText.crlf`.

**`cases.json` → `byteVariants`**: for each snippet fixture, for both the LF and the CRLF text, build each
variant's bytes as described in `build`, then:

- `decode: "text"`: `decodeBytesWithBomHeuristics(bytes)` must return exactly that text. `"skip"` means don't test it.
- `extract: "snippet"`: `extractFmxmlFromBytes(bytes)` must return the text from the first `<fmxmlsnippet`
  through the end of the last `</fmxmlsnippet>`.
- `extract: "lf-snippet"`: the same, but taken from the LF-normalized text.

**`cases.json` → `detection`**: `detectSnippetType(text)` must equal `expect`.

**`cases.json` → `normalizeToXmlText`**: the result must equal `expect`, where `null` means no snippet was found.

**`cases.json` → `decodeBytesWithBomHeuristics` / `extractFmxmlFromBytes`**: the input is either `hex`
(bytes) or `text` plus `encoding` (`utf8`, `utf16le` or `utf16be`, with no BOM). The result must equal
`expect`, where `null` means nothing was extracted.

**`cases.json` → `parseSnippet`**: run `FmXmlParser.parse(xml)`.

- With `error`: parsing must fail with exactly that message. The messages are shown to users, so both plugins must use the same wording.
- Otherwise, the parsed model must match `expect`, with `elementTypes` in the order `FIELDS, SCRIPTS, TABLES, LAYOUTS, CUSTOM_FUNCTIONS, VALUE_LISTS`.
- Then `DefaultXmlToClipboardConverter.convertToClipboardPayload(xml)` must return `payload`, or fail with exactly `payloadError`.

**`cases.json` → `fmSnippetDetectTypes`**: `FmSnippet.detectTypes(xml)` must equal `expect`, in the same order.

**`cases.json` → `fileNaming`**: `uniqueFileName(resolveFileName(pattern, extension, nowMillis), name ∈ existing)`
must equal `expect`. A `null` pattern means the setting is unset.

## Raw clipboard captures (`clipboard/`)

These are the bytes real FileMaker put on the clipboard. They are the strongest fixtures we have,
because they come from FileMaker itself rather than from our reading of the spec. To add one:

1. Use a throwaway FileMaker file with **no confidential data**. These files end up in a public repo.
2. In the JetBrains plugin, open Settings, search for **FMCuttingBoard**, and tick **Enable Diagnostics**.
3. In FileMaker, copy the objects. Then, in the IDE, run **Tools → FMCuttingBoard → Diagnostics: Save Raw
   Clipboard Capture** (it's also in the editor's right-click menu). The plugin saves a
   `captures/capture-<timestamp>/` folder inside your cutting-board folder (`.fmCuttingBoard` by default).
   That folder is git-ignored.
4. Fill in `fileMakerVersion` in its `capture.json`. Then move the folder (`capture.json` plus one `.bin`
   file per clipboard format) to `clipboard/<type>/<short-name>/`, using the same `<type>` folder names
   as `snippets/`. You can delete large bitmap formats (`CF_DIB`, `CF_DIBV5`, `CF_BITMAP`) to keep the
   repo small. Only the `Mac-*` and `CF_UNICODETEXT` files are tested.

For each capture, the harnesses check that every FileMaker custom format (`Mac-*`):

- has a valid length prefix;
- yields an fmxmlsnippet through `extractFmxmlFromBytes` that is detected as the folder's type;
- re-encodes with `encodeCustomFormatPayload` to the **same bytes**.

If FileMaker's own bytes ever differ from our encoding, that's a real spec discovery. Document it in
`docs/FileMaker-Native-Clipboard-Analysis.md`.
