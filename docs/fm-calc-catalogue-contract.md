# FileMaker calculation catalogue: contract with fmscriptinventory

FMCuttingBoard wants `.fmcalc` to be a first-class file type in both VS Code and JetBrains IDEs. That needs
complete, accurate facts about FileMaker's calculation language. Hand-maintaining them doesn't scale: today
`shared/data/filemaker-functions.json` has only 22 functions, so the JetBrains plugin flags most real functions
as "unknown".

The sister repo [fmscriptinventory](https://github.com/Blue-Kachina/fmscriptinventory) already catalogues
FileMaker script steps from help.claris.com and a FileMaker test-bed file. This document is the agreement for
it to also produce a **calculation catalogue**, which FMCuttingBoard (and `fmscriptui`) consume.

- **Format:** `fm-calc-catalogue/v1`, defined by [`shared/schemas/fm-calc-catalogue.schema.json`](../shared/schemas/fm-calc-catalogue.schema.json).
- **Example:** [`shared/fixtures/calc-catalogue/example.illustrative.json`](../shared/fixtures/calc-catalogue/example.illustrative.json).
  It is illustrative only; its values are placeholders, not verified facts.
- **Producer's plan:** section 10 of `database_structure_brainstorming.md` in fmscriptinventory.

## Decisions (2026-10-07)

| Topic | Decision |
|---|---|
| What the plugins ship | **Facts + links only.** We ship names, parameters, types, argument counts, categories, versions, compatibility and operators, plus a `helpUrl` to Claris's page and an optional short `summary` **written by us**. No Claris help text is copied into either plugin. The raw scraped HTML stays in fmscriptinventory's git-ignored database. |
| Delivery | **A vendored export.** fmscriptinventory exports one JSON file. FMCuttingBoard keeps a reviewed copy at `shared/data/fm-calc-catalogue.json`, refreshed with `node shared/tools/sync-calc-catalogue.mjs <export>`. Builds and CI never need the sister repo, and every data change is a diff in a PR. |
| Sequencing | FMCuttingBoard's Phase 4 is built **data-driven now**, on today's 22 functions. Grammars, completion, hover and validation are generated from or read from shared data, so the full catalogue improves both IDEs without code changes. |

## Why each part exists (IDE feature → data)

| `.fmcalc` feature (both IDEs) | Needs from the catalogue |
|---|---|
| Syntax highlighting (VS Code TextMate grammar, JetBrains lexer/annotator) | `functions[].name`, `getConstants[].name`, `constants[].name`, `operators[]` (incl. word operators `and`/`or`/`xor`/`not`), `syntax` (comments, strings, escapes, `¶`, variables, `Table::Field`, repetitions) |
| Completion | functions grouped by `category`, `signature`, `returnType`, `summary`; `getConstants` inside `Get ( … )`; `allowedConstants` for parameters such as JSON types |
| Signature help / parameter info | `parameters[]` with `optional`, `repeatable` and `group` (Case's test/result pairs, JSONSetElement's triples) |
| Hover | `signature`, `summary`, `returnType`, `lifecycle.since`, `compatibility`, `helpUrl` (an "Open Claris help" link) |
| Diagnostics: argument counts | `minArgs`/`maxArgs`, ideally `verified` against real FileMaker |
| Diagnostics: unknown function / Get constant | complete `functions` and `getConstants` lists (the main gap today) |
| Diagnostics: version awareness (later) | `lifecycle.since` / `deprecatedIn` / `removedIn`, e.g. "JSONSetElement requires FileMaker 16" |
| Formatter / brace matching / folding | `operators[].precedence` and `associativity`, `syntax.argumentSeparator` |
| Error-code hints (later) | `errorCodes[]`, e.g. hovering `401` in `Get ( LastError ) = 401` |

## Producer requirements

1. **Coverage:** every function on help.claris.com's functions reference, every `Get ( … )` argument, the named
   constants, every operator (including alternate spellings such as `<>` for `≠` and `<=` for `≤`), and the syntax rules.
2. **Exact names:** `name` is exactly what is typed in a calculation. Get constants are listed without `Get ( )`.
3. **Argument counts verified in FileMaker where possible:** for example, in `EverythingBagel.fmp12`, call
   `IsValidExpression` on calls with `minArgs - 1`, `minArgs`, `maxArgs` and `maxArgs + 1` arguments, and record
   the result in `verified`.
4. **Deterministic output:** stable ordering (functions by name, operators by precedence then symbol) and
   `generator.commit` set to the producing commit, with `dirty: false`, so vendoring diffs are meaningful.
5. **Licensing:** `summary` and `errorCodes[].label` are our own short wording (the schema caps `summary` at 300
   characters, and the sync script rejects longer ones). Never paste help text.
6. **Compatibility:** additive changes (new optional fields) stay `v1`. Renaming or removing fields, or changing
   meanings, requires `fm-calc-catalogue/v2` and a coordinated update here.

## Consumer side (FMCuttingBoard)

- `shared/tools/sync-calc-catalogue.mjs` validates an export (schema plus licensing guard), refuses exports from
  uncommitted changes, and writes `shared/data/fm-calc-catalogue.json`. CI validates the vendored file on every push.
- Until the catalogue lands, the language facts come from the hand-curated `shared/data/calc-language.json`
  (Get constants, named constants, operators with Claris's precedence, syntax rules, each with a `helpUrl`) and
  `shared/data/filemaker-functions.json` (functions). Its shapes match the catalogue's, so switching is mechanical.
- VS Code reads them through one adapter (`vscode/src/core/CalcLanguage.ts`) plus the grammar generator
  (`vscode/scripts/generate-grammar.mjs`). JetBrains reads the same files through `SharedData`.
- When the catalogue lands, `shared/data/filemaker-functions.json` will be retired, and its baseline
  `shared/fixtures/golden/function-signatures.txt` will be regenerated deliberately.

## Open questions

- Where the producer publishes the export: a committed `export/fm-calc-catalogue.json` in fmscriptinventory, or a
  release asset. Either works with the sync script.
- Localized function names: FileMaker calculations use English function names in every locale (to be confirmed by
  the producer), so `v1` is `locale: "en"` only.
