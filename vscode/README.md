# FMCuttingBoard for VS Code

Move FileMaker objects between FileMaker and your editor as readable XML, and edit FileMaker calculations with
real language support.

FMCuttingBoard handles scripts, script steps, fields, tables, layout objects, custom functions and value lists.
Copy them in FileMaker, get them as editable `fmxmlsnippet` XML, change them, and push them back to paste into
FileMaker. Also available [for JetBrains IDEs](https://plugins.jetbrains.com/plugin/33170-fmcuttingboard); both
are kept in feature parity ([status](https://github.com/Blue-Kachina/fmcuttingboard/blob/master/docs/parity.md)).

## Getting started

1. In FileMaker, copy some objects (script steps, fields, a layout selection, …).
2. In VS Code, run **FMCuttingBoard: Get FileMaker Clipboard Content** (<kbd>Ctrl+Alt+C</kbd> <kbd>X</kbd>). The
   snippet is saved as XML in `.fmCuttingBoard/` in your workspace folder and opened. Anything that isn't a
   FileMaker object (for example a calculation) is saved as a `.fmcalc` file instead.
3. Edit the XML. Then run **Push Clipboard Into FileMaker** (<kbd>Ctrl+Alt+C</kbd> <kbd>P</kbd>, the
   **Send To FileMaker Clipboard** link at the top of the file, or the clipboard button in the editor title bar),
   and paste in FileMaker.

The `.fmCuttingBoard` folder gets its own `.gitignore`, so clipboard snippets never end up in your repository.

## FileMaker calculations (`.fmcalc`)

- Syntax highlighting: functions, `Get ( … )` constants, `Table::Field` references, `$`/`$$` variables, strings,
  `¶`, comments and operators
- Completion of functions, `Get ( … )` constants and named constants (<kbd>Ctrl+Space</kbd>)
- Signature help while typing arguments, and hovers on functions and constants
- `let`, `if` and `case` snippets, comment toggling, bracket matching

## Settings

| Setting | Default | Description |
|---|---|---|
| `fmcuttingboard.baseDirName` | `.fmCuttingBoard` | Folder (in the workspace folder) where clipboard content is saved |
| `fmcuttingboard.fileNamePattern` | `{timestamp}` | File name without extension; `{timestamp}` is the current time in milliseconds |
| `fmcuttingboard.previewBeforeClipboardWrite` | `false` | Ask for confirmation, with a preview, before replacing the clipboard |
| `fmcuttingboard.enableDiagnostics` | `false` | Show the **Dump Clipboard Formats** and **Save Raw Clipboard Capture** commands |

For detailed logs, set the **FMCuttingBoard** output channel's level with **Developer: Set Log Level…**.

## Requirements and limitations

- **Windows:** full support. FileMaker's own clipboard formats are read and written through a small PowerShell
  script that ships with the extension (each clipboard operation takes about a quarter of a second). Policies
  that block PowerShell scripts make the extension fall back to plain text.
- **macOS:** plain text only for now. FileMaker's macOS clipboard types are still being researched.
- **Remote (WSL, SSH, containers):** the extension runs on your local machine, where FileMaker's clipboard is,
  and saves files into the remote workspace.
- VS Code 1.101 or newer.

## Privacy

FMCuttingBoard makes no network requests and collects no telemetry. It only reads and writes the clipboard and
files in your workspace's `.fmCuttingBoard` folder.

## License

[MIT](LICENSE)
