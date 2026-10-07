# Developing FMCuttingBoard for VS Code

## Commands

```sh
cd vscode
npm install
npm run check              # grammar up to date + type-check + unit tests + shared golden-fixture tests
npm run build              # bundle to dist/extension.js
npm run test:integration   # runs the extension inside VS Code 1.101 (downloaded to .vscode-test/)
npm run package            # build a .vsix (what the release workflow publishes)
```

Press <kbd>F5</kbd> in VS Code with the `vscode/` folder open to launch an Extension Development Host.

To test the PowerShell bridge against the real Windows clipboard (it overwrites the clipboard, then restores its
text), run `FMCB_CLIPBOARD_TESTS=1 npx vitest run test/powershellBridge.test.ts`.

## How it is structured

- `src/actions/` ports the JetBrains actions (same flows and messages) against small interfaces in `ports.ts`.
  `src/host/` and `src/extension.ts` are the only code that uses the VS Code API.
- `.fmcalc`: `src/core/CalcLanguage.ts` is the single source of language facts (shared data).
  `scripts/generate-grammar.mjs` generates `syntaxes/fmcalc.tmLanguage.json` from the same data. Run
  `npm run generate:grammar` after changing `shared/data`; CI fails if the grammar is stale.
- `.fmcalc` diagnostics: `src/core/CalcLexer.ts`, `CalcParser.ts` and `CalcDiagnostics.ts` are faithful ports of
  the JetBrains lexer (+ adapter), parser (including its recovery quirks) and annotator. Argument counts depend on
  the exact parse, so change them only together with the Java side; the golden `lexer` and `diagnostics` cases in
  `shared/fixtures/golden/cases.json` fail otherwise.
- `src/clipboard/` decides what to read and write; `resources/fmclipboard.ps1` only moves bytes.
- `src/core/` is a 1:1 TypeScript port of the JetBrains plugin's platform-independent logic. File names match the
  Java class names (e.g. `FmClipboardCodec.ts` ↔ `FmClipboardCodec.java`). It must not import `vscode`, so it can
  be unit-tested in plain Node.
- Data shared with the JetBrains plugin (`../shared/data/*.json`) is bundled at build time. Edit it there, never in
  this folder.
- `test/sharedGoldenFixtures.test.ts` runs the shared fixtures in `../shared/fixtures/`. The JetBrains plugin runs
  the same files, so if either plugin disagrees with them, CI fails. See
  [`shared/fixtures/README.md`](../shared/fixtures/README.md).
- `LICENSE` is a copy of the repository's root `LICENSE` (vsce requires one in the extension folder); CI checks
  they are identical.

## Releasing

Releases are triggered by **`vscode-v<version>` tags** (plain `v*` tags publish nothing; see the root README).

1. Set `version` in `package.json`.
2. In `CHANGELOG.md`, rename `## [Unreleased]` to `## [<version>] - <date>` and add a new empty `## [Unreleased]`.
3. Commit, then push the tag `vscode-v<version>`. `.github/workflows/release-vscode.yml`:
   1. checks that the tag matches `package.json`;
   2. runs every check (including the integration tests);
   3. packages the `.vsix`;
   4. publishes it to the VS Code Marketplace and Open VSX;
   5. creates a GitHub Release with the `.vsix` and the changelog section.

To rehearse without publishing, run the **Release (VS Code)** workflow manually from the Actions tab
(`workflow_dispatch`). It does everything except publishing and the GitHub Release, and uploads the `.vsix` as
a workflow artifact.

### One-time setup (before the first release)

1. **VS Code Marketplace publisher:** create the publisher `bluekachina` at
   <https://marketplace.visualstudio.com/manage> (or change `publisher` in `package.json` to the one you
   create; it is part of the extension ID and can't change after publishing).
2. **Marketplace token:** in Azure DevOps, create a Personal Access Token with **Organization: All accessible
   organizations** and scope **Marketplace → Manage**. Save it as the repository secret `VSCE_PAT`.
3. **Open VSX** (used by VSCodium, Cursor and others): sign in at <https://open-vsx.org> with GitHub, sign the
   publisher agreement, and create an access token. Save it as the repository secret `OVSX_PAT`. Then create the
   namespace once: `npx ovsx create-namespace bluekachina -p <token>`.
