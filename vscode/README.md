# FMCuttingBoard for VS Code

> **Work in progress — not yet published.** The JetBrains version is available on the
> [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/33170-fmcuttingboard).

FMCuttingBoard moves FileMaker objects (scripts, script steps, fields, tables, layout objects, custom
functions and value lists) between FileMaker's clipboard and editable `fmxmlsnippet` XML files, and adds
editing support for FileMaker calculations (`.fmcalc`).

This extension is kept in feature parity with the JetBrains plugin; see [`docs/parity.md`](../docs/parity.md)
for what is done so far.

## Development

```sh
cd vscode
npm install
npm run check     # type-check + unit tests + shared golden-fixture tests
npm run build     # bundle to dist/extension.js
```

Press <kbd>F5</kbd> in VS Code with the `vscode/` folder open to launch an Extension Development Host.

### How it is structured

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
