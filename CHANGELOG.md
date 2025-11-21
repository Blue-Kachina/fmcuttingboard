Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project adheres to Semantic Versioning as the plugin matures.

## [0.0.1] - 2025-11-21
### Added
- Initial development version of FMCuttingBoard with basic actions registered in the Tools menu.
- Clipboard parsing and XML conversion foundations with tests.
- Settings page for base directory and filename pattern.

### Packaging
- Gradle configured for IntelliJ Platform 2024.3 and Java 21.
- Added convenience task `releasePlugin` that invokes `buildPlugin` to produce a distributable ZIP in `build/distributions`.
