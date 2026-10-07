# Changelog

All notable changes to the VS Code extension are documented in this file. (The JetBrains plugin has its
own changelog in `jetbrains/CHANGELOG.md`.)

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]
### Added
- Commands (same names and behavior as the JetBrains plugin): **Get FileMaker Clipboard Content**
  (`Ctrl+Alt+C X`) and **Push Clipboard Into FileMaker** (`Ctrl+Alt+C P`), plus the hidden helper commands, in
  the Command Palette and the editor and explorer context menus.
- Windows: reads and writes FileMaker's native clipboard formats through a bundled PowerShell/.NET helper.
  macOS and Linux use plain text.
- "FileMaker XML Detected: Send To FileMaker Clipboard" CodeLens and an editor title button on fmxmlsnippet
  `.xml` files.
- Settings: `fmcuttingboard.baseDirName`, `fileNamePattern`, `previewBeforeClipboardWrite`, `enableDiagnostics`.
- Diagnostics (when enabled): **Dump Clipboard Formats** and **Save Raw Clipboard Capture**.
- `.fmcalc` (FileMaker calculation) language support:
  - syntax highlighting, generated from shared data;
  - comment toggling, bracket matching and auto-closing;
  - `let`/`if`/`case` snippets;
  - function, `Get()` constant and named constant completion;
  - hover and signature help;
  - problems reported as you type: unmatched brackets, unterminated strings, wrong argument counts, and
    `Let`/`While` variables used outside their scope (the same checks and messages as the JetBrains plugin).
- Project scaffold (TypeScript, esbuild, Vitest).
- Port of the JetBrains plugin's platform-independent core: fmxmlsnippet detection and validation,
  FileMaker clipboard payload encoding/decoding, file naming, and the FileMaker function registry. Verified
  against the shared golden fixtures that the JetBrains plugin also passes.
