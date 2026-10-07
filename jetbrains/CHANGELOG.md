# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to Semantic Versioning as the plugin matures.

## [Unreleased]
### Added
- Diagnostics: new **Save Raw Clipboard Capture** action (Windows; visible when diagnostics are enabled). It saves the raw bytes of every clipboard format to `<cutting board folder>/captures/`, so they can be used as test fixtures.

### Fixed
- Clipboard data in UTF-16 without a byte-order mark could come out garbled: UTF-16LE was misdetected as UTF-16BE, and non-ASCII characters in UTF-16BE data could be misread.
- On macOS, reading byte-based clipboard flavors now uses the same, more robust decoding as on Windows.
- `.fmcalc` highlighting now follows FileMaker's syntax:
  - `Table::Field`, `¶` and `${ }` quoted names are recognized (previously shown as bad characters);
  - Java keywords such as `boolean`, `class`, `int` and `void` are no longer highlighted;
  - only double-quoted strings are accepted, and they may span lines;
  - logical operators (`and`, `OR`, `Xor`, `not`) are recognized in any case.
- Every function call is highlighted as a function in any case (`substitute(…)`, custom functions), and the argument of `Get ( … )` is highlighted as a constant.
- The parser now knows every FileMaker operator and Claris's precedence order. `&`, `^`, `xor`, `<>`, `<=` and `>=` no longer stop parsing, and unary minus/plus parse.
- Brackets or control characters inside strings and comments no longer cause "Unmatched closing" or "Invalid control character" errors. A missing closing quotation mark is now reported.
- `Get ( … )` arguments and field references are no longer reported as possibly undefined variables.
- `.fmcalc` error checking now actually runs. A bug meant it never ran in a project, so you may now see:
  - errors for unmatched closing brackets and unterminated strings;
  - errors for wrong argument counts in known functions;
  - a new weak warning when a `Let`/`While` variable is used outside the function that defines it (there it
    silently means a field).
- `Let ( [ … ] ; … )` and `While ( [ … ] ; … ; [ … ] ; … )` variable lists, and `Field[n]` repetitions, are now parsed.
- Binary expressions now include their left operand in the syntax tree.
- `Substitute` accepts any number of bracketed `[ search ; replace ]` pairs.

### Removed
- The "Script variable may be undefined" and "Undefined variable (not bound in any Let())" warnings. A plain
  name in a calculation is usually a field, and `$`/`$$` variables are set by scripts, so neither can be known
  from the calculation alone.
- "Unknown function" warnings are off until the shared function list is complete. Today it covers only some
  functions, so most real functions and every custom function would be flagged.

### Changed
- `.fmcalc` color settings: named constants (True, JSON types, text styles) now use the color scheme's
  *Constant* color (key `FM_CALC_CONSTANT`) instead of the keyword color. New keys: `FM_CALC_GET_CONSTANT`,
  `FM_CALC_FIELD` and `FM_CALC_PARAGRAPH_MARK`. `FM_CALC_KEYWORD_CONTROL_FLOW` and `FM_CALC_KEYWORD_TYPE` are removed.
- The FileMaker function list, the `Get()` completion constants and the clipboard format rules now load from data files shared with the upcoming VS Code extension, so both stay in sync. No behavior change.

## [1.0.6] - 2026-07-27
### Fixed
- Migrated `FmXmlSnippetNotificationProvider` from the deprecated `EditorNotifications.Provider` to `EditorNotificationProvider`.
- Replaced the deprecated `FoldingDescriptor.EMPTY` and `SyntaxHighlighterBase.EMPTY` fields with their `EMPTY_ARRAY` replacements.
- Migrated syntax-highlighting `TextAttributesKey`s off the deprecated `createTextAttributesKey(String, TextAttributes)` overload onto scheme-aware fallback keys (`DefaultLanguageHighlighterColors`); calculation syntax colors now follow the active editor color scheme instead of fixed hex values.
- Replaced the deprecated `LanguageCodeStyleSettingsProvider.getDefaultCommonSettings()` override with `customizeDefaults(...)`.
- Removed the `FormattingModelBuilder.createModel(PsiElement, CodeStyleSettings)` override, which the Plugin Verifier flagged as scheduled for removal; the modern `createModel(FormattingContext)` overload already covers all supported IDE versions (`sinceBuild=242`).

## [1.0.5] - 2026-07-24
### Changed
- Bumped the IntelliJ Platform Gradle Plugin from `2.0.1` to `2.18.1`.
- Documented known limitations, future ideas, and added an in-IDE "Documentation" link in Settings.
- Added a devcontainer for reproducibly testing Gradle builds in a clean Linux environment.
- Fixed an invalid `until-build=""` attribute in `plugin.xml` (rejected by the current Plugin
  Verifier) by no longer setting an empty `untilBuild`.
- Fixed an override-only API violation: actions no longer call `actionPerformed()` directly on
  sibling actions; shared logic now goes through a plain `perform(AnActionEvent)` method.

### Added
- Root `LICENSE` (MIT) and a README `## License` section.
- Explicit no-telemetry / no-network-access statement in the README.
- `pluginVerification` and `signing` configuration in `build.gradle.kts`.
- `verifyPlugin` step in CI.

## [1.0.4] - 2025-11-23
### Added
- Code Style settings for the FileMaker Calculation language (Settings > Editor > Code Style).
- Tabbed preview panel with a working simple-indent mode.
- IDE-default comment toggling (line/block) now works in `.fmcalc` files.

## [1.0.3] - 2025-11-23
### Added
- First-class FileMaker Calculation language support (`.fmcalc` files): lexer, PSI parser with
  operator precedence, syntax highlighting, code folding, brace matching, and a code style provider.
- Context-aware code completion and parameter hints sourced from a consolidated function metadata
  registry (280+ FileMaker functions).
- Formatting model with configurable spacing/indentation rules ("Reformat Code" support).
- Error detection (undefined variables, function parameter count) and quick fixes (comma → semicolon,
  missing semicolon insertion).
- New smart "Get FileMaker Clipboard Content" action that creates `.xml` or `.fmcalc` automatically
  depending on clipboard content.

## [1.0.2] - 2025-11-22
- No user-facing changes; version bump only.

## [1.0.1] - 2025-11-22
### Added
- Push Clipboard Into FileMaker no longer requires saving the file first.
- Notification banner with an action button for smoother round-tripping.

## [1.0.0] - 2025-11-22
### Added
- Initial release of FMCuttingBoard with Tools menu actions: Convert FM Clipboard To XML Clipboard,
  New XML File From FM Clipboard, and Push Clipboard Into FileMaker.
- Cross-platform clipboard access (Windows native path plus a JNA-based fallback) with detection for
  Fields, Tables, Scripts/Steps, Custom Functions, Value Lists, and Layout Objects.
- Settings page for base output directory and filename pattern, with optional pre-write preview.
- Clipboard parsing and XML conversion covered by unit tests; GitHub Actions CI on push/PR.

### Packaging
- Gradle configured for IntelliJ Platform 2024.3 and Java 21.
- Added convenience task `releasePlugin` that invokes `buildPlugin` to produce a distributable ZIP in `build/distributions`.
