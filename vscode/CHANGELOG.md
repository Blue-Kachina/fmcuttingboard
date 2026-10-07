# Changelog

All notable changes to the VS Code extension are documented in this file. (The JetBrains plugin has its
own changelog in `jetbrains/CHANGELOG.md`.)

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]
### Added
- Project scaffold (TypeScript, esbuild, Vitest).
- Port of the JetBrains plugin's platform-independent core: fmxmlsnippet detection and validation,
  FileMaker clipboard payload encoding/decoding, file naming, and the FileMaker function registry. Verified
  against the shared golden fixtures that the JetBrains plugin also passes.
