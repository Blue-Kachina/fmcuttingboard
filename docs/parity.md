# Feature parity: JetBrains ↔ VS Code

This is the definition of "done" for keeping the two plugins in step. **The VS Code port is complete when
every row that is ✅ for JetBrains is also ✅ for VS Code.** Update this file in any PR that changes
behavior in either plugin.

Legend: ✅ done · ⚠️ partial (see note) · ⏸ deliberately off for now (see note) · ❌ not yet · n/a doesn't apply. Versions are the first release
that has the feature in that plugin.

Shared foundations (both plugins must pass them, see `shared/fixtures/README.md`):
`shared/data/clipboard-formats.json`, `shared/data/filemaker-functions.json`, `shared/fixtures/`.

| Foundation | JetBrains | VS Code | Notes |
|---|:---:|:---:|---|
| Reads shared data (formats, functions) | ✅ Unreleased | ✅ Unreleased | |
| Passes shared golden fixtures | ✅ Unreleased | ✅ Unreleased | Same 230 cases in both; CI runs both on every push |
| Passes raw FileMaker captures | — | — | No captures recorded yet |

## Clipboard workflow

| Capability | JetBrains | VS Code | Notes |
|---|:---:|:---:|---|
| Get FileMaker clipboard → new `.xml` file (smart action) | ✅ 1.0.0 | ✅ Unreleased | Falls back to `.fmcalc` when the clipboard isn't an fmxmlsnippet |
| Plain clipboard text → new `.fmcalc` file | ✅ 1.0.0 | ✅ Unreleased | |
| Push active `.xml` editor (incl. unsaved edits) → FileMaker clipboard | ✅ 1.0.0 | ✅ Unreleased | |
| Snippet types: script, script steps, fields, tables, custom functions, value lists, layout objects | ✅ 1.0.0 | ✅ Unreleased | Detection order and format names come from `clipboard-formats.json` |
| Windows: read FileMaker native formats (`Mac-XM*`) | ✅ 1.0.0 | ✅ Unreleased | JetBrains: JNA. VS Code: PowerShell/.NET helper (~0.25 s per clipboard operation) |
| Windows: write FileMaker native formats + CF_UNICODETEXT | ✅ 1.0.0 | ✅ Unreleased | Both fall back to plain text if the native write fails |
| macOS: read clipboard | ⚠️ 1.0.0 | ⚠️ Unreleased | Text only |
| macOS: write FileMaker native pasteboard types | ❌ | ❌ | Type names unknown; needs research on a Mac (`docs/MacPasteboardResearch.md`) |
| Linux | n/a | n/a | FileMaker doesn't run on Linux |
| Preview before writing the clipboard (setting) | ✅ 1.0.0 | ✅ Unreleased | |
| "Send to FileMaker" prompt on fmxmlsnippet `.xml` files | ✅ 1.0.0 | ✅ Unreleased | JetBrains: editor banner. VS Code: CodeLens + editor title button |
| Keyboard shortcuts `Ctrl+Alt+C, X` / `Ctrl+Alt+C, P` | ✅ 1.0.0 | ✅ Unreleased | |
| Editor and project/explorer context menus | ✅ 1.0.0 | ✅ Unreleased | JetBrains also has a Tools → FMCuttingBoard menu; VS Code uses the Command Palette (category "FMCuttingBoard") |

## Settings

| Setting | JetBrains | VS Code | Notes |
|---|:---:|:---:|---|
| Base directory name (default `.fmCuttingBoard`, auto `.gitignore`) | ✅ 1.0.0 | ✅ Unreleased | JetBrains: per project. VS Code: per workspace folder (`fmcuttingboard.baseDirName`) |
| File name pattern (default `{timestamp}`) | ✅ 1.0.0 | ✅ Unreleased | |
| Preview before clipboard write | ✅ 1.0.0 | ✅ Unreleased | |
| Enable diagnostics | ✅ 1.0.0 | ✅ Unreleased | |

## Diagnostics

| Capability | JetBrains | VS Code | Notes |
|---|:---:|:---:|---|
| Dump clipboard formats to the log | ✅ 1.0.0 | ⚠️ Unreleased | Windows only. JetBrains also appends the report to `<project>/docs/FileMaker-Native-Clipboard-Analysis.md`; VS Code only logs it (writing into a user's `docs/` folder looks like a leftover from early development; consider dropping it from JetBrains too) |
| Save raw clipboard capture (fixture source) | ✅ Unreleased | ✅ Unreleased | Windows only. Same folder layout in both plugins |
| Verbose logging | ✅ 1.0.0 | ✅ Unreleased | JetBrains: `-Dfmcuttingboard.verbose`. VS Code: **FMCuttingBoard** output channel log level |
| Notifications with "Show details" | ✅ 1.0.0 | ✅ Unreleased | Same messages in both plugins |

## `.fmcalc` language support

| Capability | JetBrains | VS Code | Notes |
|---|:---:|:---:|---|
| File type / language registration | ✅ 1.0.0 | ✅ Unreleased | |
| Syntax highlighting | ✅ Unreleased (⚠️ 1.0.0) | ✅ Unreleased | Same rules in both, driven by shared data: any call is a function (case-insensitive), `Get ( X )` for any X, constants from `calc-language.json`, `Table::Field`, `${ }`, `¶`, `$`/`$$` variables, Unicode names. Only difference: VS Code also colors `\"` escapes inside strings |
| Comment toggling (`//`, `/* */`) | ✅ 1.0.0 | ✅ Unreleased | |
| Brace matching / auto-closing | ✅ 1.0.0 | ✅ Unreleased | |
| Function completion | ✅ 1.0.0 | ✅ Unreleased | Same items, type text and `Name(p1; p2)` template. `Get()` constants come from `calc-language.json` in both. VS Code also completes named constants (`JSONString`, `Bold`, …): JetBrains ⚠️ for that part |
| Signature help / parameter info | ❌ | ✅ Unreleased | JetBrains handler exists but is disabled in `plugin.xml` |
| Hover documentation | ❌ | ✅ Unreleased | Functions, `Get()` constants, named constants |
| Snippets / live templates (`let`, `if`, `case`) | ✅ 1.0.0 | ✅ Unreleased | A VS Code test fails if the snippets drift from the JetBrains live templates |
| Folding of `Let` / `Case` / `If` | ✅ 1.0.0 | ❌ | Post-v1 |
| Diagnostics: unmatched brackets, unterminated strings, invalid control characters | ✅ Unreleased (⚠️ 1.0.x: never ran) | ✅ Unreleased | Brackets inside strings and comments are ignored. Same results in both IDEs (golden "diagnostics" cases) |
| Diagnostics: argument-count errors | ✅ Unreleased (⚠️ 1.0.x: never ran) | ✅ Unreleased | Only as good as `filemaker-functions.json` |
| Diagnostics: unknown functions | ⏸ | ⏸ | Off in both until `filemaker-functions.json` says `"complete": true` (the fmscriptinventory catalogue) |
| Diagnostics: `Let`/`While` variable used outside its scope | ✅ Unreleased | ✅ Unreleased | Follows Claris's scoping rules; unknown names are never reported (they are usually fields). JetBrains weak warning = VS Code Information |
| Formatter + code style options (incl. `DO_NOT_INDENT_TOP_LET_VARIABLES`) | ✅ 1.0.0 | ❌ | Post-v1 |
| Quick fixes: comma → semicolon, insert missing semicolons | ✅ 1.0.0 | ❌ | Post-v1 |

### JetBrains lexer follow-ups: done (Unreleased)

All of the gaps found while building the VS Code grammar are fixed:

- `::`, `¶` and `${ }` are proper tokens.
- The Java keywords are gone.
- Strings are double-quoted only, and may span lines.
- Calls and named constants are classified case-insensitively, from shared data.
- `Get ( X )` is highlighted for any X.

The same change also fixed the following:

- **Parser precedence:** it now comes from `calc-language.json` (Claris's order). `&`, `^`, `xor`, `<>`, `<=` and `>=` no longer stop the parser, and unary `-`/`+` parse.
- **Annotator:** brackets and control characters inside strings and comments no longer cause false errors, and an unterminated string is reported.
- **No new undefined-variable warnings:** `Get()` arguments, named constants and field references are no longer treated as possible undefined variables.

`Let`/`While` follow-up: done too.

- **Parsing:** `[ … ]` variable lists and `Field[n]` repetitions are parsed.
- **Undefined-variable checks:** these were replaced by a scope check that never reports unknown names (they're
  usually fields).
- **The annotator itself:** it had never run in a real project (its run-once guard always returned) and now runs.

## Manual FileMaker paste checklist

Fixtures prove the bytes are right; only FileMaker proves it accepts them. Re-run before each release
of either plugin: copy each type in FileMaker → get it into the IDE → push it back → paste into FileMaker.

| Snippet type | FileMaker version | JetBrains | VS Code | Date |
|---|---|:---:|:---:|---|
| Script | | | | |
| Script steps | | | | |
| Fields | | | | |
| Tables | | | | |
| Custom functions | | | | |
| Value lists | | | | |
| Layout objects | | | | |
