# `.fmcalc` formatting and folding

Both IDEs format and fold FileMaker calculations with the same rules. There are two implementations:
`vscode/src/core/CalcFormatter.ts` and `CalcFolding.ts`, and `jetbrains/.../language/format/FmCalcFormatter.java`
and `language/folding/FileMakerCalculationFoldingBuilder.java`. They're kept identical by the golden cases in
`shared/fixtures/golden/cases.json` (`formatting`, `folding`). If you change a rule, change both implementations,
then add or adjust a golden case.

## Formatting

**Spacing (Claris style, not configurable)**, the way Claris's documentation writes calculations:

| Construct | Formatted |
|---|---|
| Function call | `If ( a > b ; "x" ; "y" )`: a space before `(`, inside the parentheses, and around `;` |
| Empty call | `Left ()` |
| Variable list | `[ a = 1 ; b = 2 ]` |
| Operators | one space around binary operators and word operators (`and`, `or`, `xor`); `not x`, `-3` |
| Repetitions | `Table::Field[2]` (compact) |
| Grouping | `( a + b )` |

Operators keep the spelling and case they were written with (`<>` stays `<>`, `AND` stays `AND`). Strings, `¶`,
`${ }` names and comments are never changed.

**Line breaks (pretty-printed).** Existing line breaks, indentation and blank lines are replaced:

- `Let` with two or more variables is always multi-line, one variable per line:
  ```
  Let ( [
  a = 1 ;
  b = 2
  ] ;
  	a + b
  )
  ```
  The outermost `Let`'s variables are flush with the `Let` when *Do not indent top let variables* is on (the
  default); nested `Let`s always indent their variables.
- `Case` with two or more test/result pairs (or one that doesn't fit) puts one pair per line, then the default.
- `While` is always multi-line, one argument per line.
- Any other call stays on one line if it fits within the maximum line length, and otherwise has one argument per
  line. Width counts indentation (a tab counts as the tab size).
- Anything containing a comment is multi-line. Comments stay attached: a comment on the same line as the token
  before it stays after that token, and other comments go on their own lines before the next token.
- One indentation level per nesting level, using tabs or spaces from the editor settings.

**Calculations that aren't well-formed are left untouched.** That includes unbalanced brackets, an unterminated
string, a control character, `{ }`, or anything the parser couldn't place, such as two expressions in a row. The
problems are reported as diagnostics instead.

**Settings**

| | JetBrains (*Settings → Editor → Code Style → FileMaker Calculation*) | VS Code |
|---|---|---|
| Indentation | Tabs and Indents: *Use tab character*, *Tab size*, *Indent* | the editor's indentation (`editor.insertSpaces`, `editor.tabSize`) |
| Maximum line length | Wrapping: *Hard wrap at* | `fmcuttingboard.format.maxLineLength` (default 120) |
| Top `Let` variables | Tabs and Indents: *Do not indent top let variables* | `fmcuttingboard.format.doNotIndentTopLetVariables` (default on) |

Formatting is idempotent: formatting the output again changes nothing (the golden harnesses check this).

## Folding

- The inside of multi-line `Let`, `Case`, `If` and `While` calls, in any case and with any spacing, folds to `…`.
- The inside of multi-line `[ … ]` lists folds to `…`.
- Multi-line block comments fold to `/*…*/`.

Parentheses inside strings and comments are ignored, and nested calls fold independently. VS Code keeps one fold
per starting line (the outer one), so a `Let`'s `[` on the same line as `Let (` doesn't get a separate fold there.
