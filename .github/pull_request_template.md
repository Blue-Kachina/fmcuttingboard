## What and why

<!-- What changes, and why. -->

## Parity (JetBrains ↔ VS Code)

- [ ] Implemented in both plugins, **or** the gap is recorded in `docs/parity.md`
- [ ] Shared behavior lives in `shared/` (data or golden fixtures), not hardcoded in one plugin
- [ ] Changed `shared/data`? Regenerated what depends on it (`node shared/tools/generate-golden.mjs`,
      `npm run generate:grammar` in `vscode/`) and updated `shared/fixtures/golden/function-signatures.txt` if needed
- [ ] User-facing change? Entry under `## [Unreleased]` in the affected `CHANGELOG.md`
      (`jetbrains/CHANGELOG.md` feeds the JetBrains Marketplace notes; keep VS Code entries out of it)

## Checked

- [ ] CI is green (shared validation, JetBrains build/tests/verifier, VS Code tests/integration/package)
- [ ] Clipboard changes: tried the paste round trip in FileMaker (checklist in `docs/parity.md`)
