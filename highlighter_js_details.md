# FileMaker Calculation — Syntax Details for highlight.js

## Case Sensitivity
Case-sensitive (`isCaseSensitive() = true`, matching the Notepad++ XML `caseIgnored="no"`).

---

## Token Types & Colors (from Notepad++ XML / Syntax Highlighter)

| Token | Color | Style | highlight.js class suggestion |
|---|---|---|---|
| `KEYWORD_CONTROL` (`if`, `case`) | `#0000FF` | bold | `keyword` |
| `KEYWORD_LOGICAL` (`and`, `or`, `not`, `xor`) | `#006699` | bold | `keyword` |
| `KEYWORD_TYPE` (type names, constants) | `#FF8000` | bold | `built_in` |
| `KEYWORD_FUNCTION` (all FM functions) | `#8000FF` | bold | `title.function` |
| `COMMENT` (both `//` and `/* */`) | `#008000` | normal | `comment` |
| `STRING` (`"..."` and `'...'`) | `#DB599D` | normal | `string` |
| `NUMBER` | `#FF0000` | normal | `number` |
| `OPERATOR` | `#804000` | bold | `operator` |

---

## Comments
- **Line comment:** `//` (to end of line)
- **Block comment:** `/* ... */`

---

## String Literals
- Double-quoted: `"..."` (backslash escapes, no newlines)
- Single-quoted: `'...'` (backslash escapes, no newlines)
- Pattern: `[\"]([^\\\r\n\"]|\\.)*[\"]` and `[']([^\\\r\n\']|\\.)*[']`

---

## Numbers
Pattern: `([0-9]+(\.[0-9]+)?([eE][+-]?[0-9]+)?|\.[0-9]+([eE][+-]?[0-9]+)?)`
- Integers, decimals, and scientific notation (e.g., `1`, `3.14`, `.5`, `1e10`, `2.5E-3`)

---

## Keywords — Control Flow (`#0000FF` bold)
```
if  case
```

## Keywords — Logical (`#006699` bold)
```
and  or  not  xor
```

## Keywords — Types & Constants (`#FF8000` bold)
```
boolean  byte  char  class  double  float  int  interface  long  short  void
True  true  False  false
JSONArray  JSONBoolean  JSONNull  JSONNumber  JSONObject  JSONRaw  JSONString
Plain  Bold  Italic  Underline  HighlightYellow  Condense  Extend  Strikethrough
SmallCaps  Superscript  Subscript  Uppercase  Lowercase  Titlecase  WordUnderline
DoubleUnderline  AllStyles
objectType  hasFocus  objectName  containsFocus  isFrontPanel  isActive
isObjectHidden  bounds  left  right  top  bottom  width  height  rotation
startPoint  endPoint  source  content  enclosingObject  containedObjects
```

---

## Keywords — Functions (`#8000FF` bold)
Full list from the lexer (also cross-referenced with `filemaker_functions.json`):

**Math:** `Abs Acos Asin Atan Ceiling Cos Degrees Div Exp Floor Int Lg Ln Log Max Min Mod Pi Radians Round Sign Sin Sqrt Tan Truncate`

**Statistical:** `Average Count StDev StDevP Sum Variance VarianceP`

**Text:** `Char Code Exact Filter FilterValues Left LeftValues LeftWords Length Lower Middle MiddleValues MiddleWords Position Proper Quote Replace Right RightValues RightWords Substitute TextColor TextColorRemove TextFont TextFontRemove TextFormatRemove TextSize TextSizeRemove TextStyleAdd TextStyleRemove Trim TrimAll Upper WordCount`

**Date/Time:** `Date Day DayName DayNameJ DayOfWeek DayOfYear Hour Minute Month MonthName MonthNameJ Seconds Time Timestamp WeekOfYear WeekOfYearFiscal Year YearName`

**Conversion:** `GetAsBoolean GetAsCSS GetAsDate GetAsNumber GetAsSVG GetAsText GetAsTime GetAsTimestamp GetAsURLEncoded`

**Get() family (keyword name only):** `Get` — the argument inside (e.g., `AccountName`) stays an IDENTIFIER

**Database/Fields:** `DatabaseNames FieldBounds FieldComment FieldIDs FieldNames FieldRepetitions FieldStyle FieldType GetField GetFieldName GetNthRecord GetRepetition GetSummary GetValue Lookup LookupNext`

**Japanese text:** `Hiragana KanaHankaku KanaZenkaku KanjiNumeral Katakana NumToJText RomanHankaku RomanZenkaku`

**Layout/Window:** `GetLayoutObjectAttribute LayoutIDs LayoutNames LayoutObjectNames WindowNames`

**List:** `List ValueCount ValueListIDs ValueListItems ValueListNames`

**Script/Relation:** `RelationInfo ScriptIDs ScriptNames`

**Table:** `TableIDs TableNames`

**Financial:** `FV NPV PMT PV`

**Logical/Special:** `Case Choose Evaluate EvaluationError If IsEmpty IsValid IsValidExpression Combination Extend External Factorial GetNextSerialValue Last Let PatternCount Random RGB Self SerialIncrement SetPrecision`

**JSON:** `JSONSetElement JSONGetElement JSONDeleteElement JSONListKeys JSONListValues JSONFormatElements`

**Encoding:** `Base64Encode Base64Decode TextEncode TextDecode`

**Query/Iteration:** `ExecuteSQL While`

---

## Operators (`#804000` bold)
Multi-char: `<=  >=  <>`
Single-char: `+ - * / = ^ < > & ; ,`
Unicode: `≠` (U+2260), `≤` (U+2264), `≥` (U+2265)

---

## Brackets (brace matching, not colored)
`(` `)` `[` `]` `{` `}`

---

## Identifiers
- Plain: `[A-Za-z_][A-Za-z0-9_]*`
- Script variables (local): `$[A-Za-z_][A-Za-z0-9_]*`
- Script variables (global): `$$[A-Za-z_][A-Za-z0-9_]*`

---

## Key Notes for highlight.js
1. **Function detection pattern:** functions are bare identifiers (no `(` required by the lexer to classify them) — highlight.js can use `functionNameLike + lookahead for \s*(` to distinguish function calls from bare identifiers like `Pi`, `Random`, `Self` (which appear without parens).
2. **`Get` is special:** `Get` as a token, followed by `(` then an argument like `AccountName`. The whole `Get ( SomeName )` could be matched as a unit for a richer highlight.
3. **Argument separator is `;` not `,`** — `;` is an operator token.
4. **String delimiters:** both `"` and `'` are valid, but they don't nest or interpolate.
