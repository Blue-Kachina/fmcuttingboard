# Feature parity: JetBrains ↔ VS Code

This is the definition of "done" for keeping the two plugins in step. **The VS Code port is complete when
every row that is ✅ for JetBrains is also ✅ for VS Code.** Update this file in any PR that changes
behavior in either plugin.

Legend: ✅ done · ⚠️ partial (see note) · ❌ not yet · n/a doesn't apply. Versions are the first release
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
| File type / language registration | ✅ 1.0.0 | ❌ | |
| Syntax highlighting | ✅ 1.0.0 | ❌ | VS Code: TextMate grammar (planned for v1) |
| Comment toggling (`//`, `/* */`) | ✅ 1.0.0 | ❌ | VS Code v1 |
| Brace matching / auto-closing | ✅ 1.0.0 | ❌ | VS Code v1 |
| Function completion | ✅ 1.0.0 | ❌ | From `filemaker-functions.json`. VS Code v1 |
| Signature help / parameter info | ❌ | ❌ | JetBrains handler exists but is disabled in `plugin.xml`. VS Code v1 plans it. If VS Code ships it first, mark JetBrains ⚠️ |
| Hover documentation | ❌ | ❌ | VS Code v1 plans it |
| Snippets / live templates (`let`, `if`, `case`) | ✅ 1.0.0 | ❌ | VS Code v1 |
| Folding of `Let` / `Case` / `If` | ✅ 1.0.0 | ❌ | Post-v1 |
| Diagnostics: unmatched braces, invalid control characters | ✅ 1.0.0 | ❌ | Post-v1 |
| Diagnostics: argument-count errors | ✅ 1.0.0 | ❌ | Post-v1. Only as good as `filemaker-functions.json` |
| Diagnostics: unknown functions, unbound `Let` / `$` variables (weak warnings) | ✅ 1.0.0 | ❌ | Post-v1. Note: the registry has only 22 functions today, so many real functions are flagged as unknown |
| Formatter + code style options (incl. `DO_NOT_INDENT_TOP_LET_VARIABLES`) | ✅ 1.0.0 | ❌ | Post-v1 |
| Quick fixes: comma → semicolon, insert missing semicolons | ✅ 1.0.0 | ❌ | Post-v1 |

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
