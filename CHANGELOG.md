# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to Semantic Versioning as the plugin matures.

## [Unreleased]

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
