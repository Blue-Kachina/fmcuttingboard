# FMCuttingBoard for VS Code

> **Work in progress — not yet published.** The JetBrains version is available on the
> [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/33170-fmcuttingboard).

FMCuttingBoard moves FileMaker objects (scripts, script steps, fields, tables, layout objects, custom
functions and value lists) between FileMaker's clipboard and editable `fmxmlsnippet` XML files, and adds
editing support for FileMaker calculations (`.fmcalc`).

This extension is kept in feature parity with the JetBrains plugin; see [`docs/parity.md`](../docs/parity.md)
for what is done so far.

## Using it

1. In FileMaker, copy some objects (script steps, fields, a layout selection, …).
2. In VS Code, run **FMCuttingBoard: Get FileMaker Clipboard Content** (<kbd>Ctrl+Alt+C</kbd> <kbd>X</kbd>). The
   snippet is saved as XML in `.fmCuttingBoard/` in your workspace folder and opened. Anything that isn't a
   FileMaker object (for example a calculation) is saved as a `.fmcalc` file instead.
3. Edit the XML, then run **Push Clipboard Into FileMaker** (<kbd>Ctrl+Alt+C</kbd> <kbd>P</kbd>, the CodeLens at
   the top of the file, or the clipboard button in the editor title bar), and paste in FileMaker.

On Windows, the extension talks to FileMaker's own clipboard formats through a small PowerShell script that
ships with it (each clipboard operation takes about a quarter of a second). On macOS, only plain text is
supported for now.

## Development

```sh
cd vscode
npm install
npm run check              # type-check + unit tests + shared golden-fixture tests
npm run build              # bundle to dist/extension.js
npm run test:integration   # runs the extension inside VS Code 1.101 (downloaded to .vscode-test/)
```

Press <kbd>F5</kbd> in VS Code with the `vscode/` folder open to launch an Extension Development Host.

To test the PowerShell bridge against the real Windows clipboard (it overwrites the clipboard, then restores
its text), run `FMCB_CLIPBOARD_TESTS=1 npx vitest run test/powershellBridge.test.ts`.

### How it is structured

- `src/actions/` ports the JetBrains actions (same flows and messages) against small interfaces in `ports.ts`;
  `src/host/` and `src/extension.ts` are the only code that uses the VS Code API.
- `src/clipboard/` decides what to read and write; `resources/fmclipboard.ps1` only moves bytes.
- `src/core/` is a 1:1 TypeScript port of the JetBrains plugin's platform-independent logic. File names
  match the Java class names (e.g. `FmClipboardCodec.ts` ↔ `FmClipboardCodec.java`), and it must not import
  `vscode`, so it can be unit-tested in plain Node.
- Data shared with the JetBrains plugin (`../shared/data/*.json`) is bundled at build time. Edit it there,
  never in this folder.
- `test/sharedGoldenFixtures.test.ts` runs the shared fixtures in `../shared/fixtures/`. The JetBrains plugin
  runs the same files; if either plugin disagrees with them, it fails CI. See
  [`shared/fixtures/README.md`](../shared/fixtures/README.md).

## Releasing

Releases are triggered by `vscode-v<version>` tags (not plain `v*` tags); see the root README.
