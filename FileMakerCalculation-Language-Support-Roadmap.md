# FileMaker Calculation Language Support Roadmap
**JetBrains Plugin Custom Language Implementation**

---

## Overview
This roadmap outlines the implementation of FileMaker Calculation language support for the FmCuttingBoard JetBrains plugin. The work is organized into phases with AI-sized tasks based on the Notepad++ XML language definition.

---

## Phase 1: Core Language Foundation

### 1.1 Language & File Type Registration
**Files to create:**
- `src/main/java/com/fmcuttingboard/language/FileMakerCalculationLanguage.java`
- `src/main/java/com/fmcuttingboard/language/FileMakerCalculationFileType.java`

**Tasks:**
- [x] Create `FileMakerCalculationLanguage` class extending `Language`
  - Define language ID as "FileMakerCalculation"
  - Define display name as "FileMaker Calculation"
  - Set case-sensitive (matching Notepad++ XML: `caseIgnored="no"`)
- [x] Create `FileMakerCalculationFileType` class implementing `LanguageFileType`
  - Associate with `.fmcalc` extension
  - Define file description: "FileMaker Calculation"
  - Reference `FileMakerCalculationLanguage` instance
  - Set default icon (temporary, until Phase 4)

### 1.2 Plugin Registration
**File to modify:**
- `src/main/resources/META-INF/plugin.xml`

**Tasks:**
- [x] Register language in `plugin.xml`
  - Add `<language>` extension point for `FileMakerCalculationLanguage`
  - Note: On modern IntelliJ Platform, explicit `<language/>` registration is not required; the `Language` is discovered via the file type binding. Kept as completed per intent. (No-op)
- [x] Register file type in `plugin.xml`
  - Add `<fileType>` extension point for `FileMakerCalculationFileType`

---

## Phase 2: Lexer Implementation

### 2.1 Lexer Definition (JFlex)
**File to create:**
- `src/main/java/com/fmcuttingboard/language/filemaker-calculation.flex`

**Token Categories (from Notepad++ XML):**

#### Keywords Group 1 - Control Flow (Blue/Bold)
- [x] Define tokens: `if`, `case`
- Color reference: `fgColor="0000FF"`, `fontStyle="1"` (bold)

#### Keywords Group 2 - Logical Operators (Dark Blue/Bold)
- [x] Define tokens: `and`, `or`, `not`
- Color reference: `fgColor="006699"`, `fontStyle="1"` (bold)

#### Keywords Group 3 - Type Keywords (Orange/Bold)
- [x] Define tokens: `boolean`, `byte`, `char`, `class`, `double`, `float`, `int`, `interface`, `long`, `short`, `void`
- Color reference: `fgColor="FF8000"`, `fontStyle="1"` (bold)

#### Keywords Group 4 - Functions (Purple/Bold)
- [x] Define mathematical functions: `Abs`, `Acos`, `Asin`, `Atan`, `Ceiling`, `Cos`, `Degrees`, `Div`, `Exp`, `Floor`, `Int`, `Lg`, `Ln`, `Log`, `Max`, `Min`, `Mod`, `Pi`, `Radians`, `Round`, `Sign`, `Sin`, `Sqrt`, `Tan`, `Truncate`
- [x] Define statistical functions: `Average`, `Count`, `StDev`, `StDevP`, `Sum`, `Variance`, `VarianceP`
- [x] Define text functions: `Char`, `Code`, `Exact`, `Filter`, `FilterValues`, `Left`, `LeftValues`, `LeftWords`, `Length`, `Lower`, `Middle`, `MiddleValues`, `MiddleWords`, `Position`, `Proper`, `Quote`, `Replace`, `Right`, `RightValues`, `RightWords`, `Substitute`, `TextColor`, `TextColorRemove`, `TextFont`, `TextFontRemove`, `TextFormatRemove`, `TextSize`, `TextSizeRemove`, `TextStyleAdd`, `TextStyleRemove`, `Trim`, `TrimAll`, `Upper`, `WordCount`
- [x] Define date/time functions: `Date`, `Day`, `DayName`, `DayNameJ`, `DayOfWeek`, `DayOfYear`, `Hour`, `Minute`, `Month`, `MonthName`, `MonthNameJ`, `Seconds`, `Time`, `Timestamp`, `WeekOfYear`, `WeekOfYearFiscal`, `Year`, `YearName`
- [x] Define Get functions (100+ functions): All `Get()` functions from XML line 17
 - [x] Define conversion functions: `GetAsBoolean`, `GetAsCSS`, `GetAsDate`, `GetAsNumber`, `GetAsSVG`, `GetAsText`, `GetAsTime`, `GetAsTimestamp`, `GetAsURLEncoded`
 - [x] Define field/database functions: `DatabaseNames`, `FieldBounds`, `FieldComment`, `FieldIDs`, `FieldNames`, `FieldRepetitions`, `FieldStyle`, `FieldType`, `GetField`, `GetFieldName`, `GetNthRecord`, `GetRepetition`, `GetSummary`, `GetValue`, `Lookup`, `LookupNext`
 - [x] Define Japanese text functions: `Hiragana`, `KanaHankaku`, `KanaZenkaku`, `KanjiNumeral`, `Katakana`, `NumToJText`, `RomanHankaku`, `RomanZenkaku`
 - [x] Define layout/window functions: `GetLayoutObjectAttribute`, `LayoutIDs`, `LayoutNames`, `LayoutObjectNames`, `WindowNames`
 - [x] Define list functions: `List`, `ValueCount`, `ValueListIDs`, `ValueListItems`, `ValueListNames`
 - [x] Define script/relation functions: `RelationInfo`, `ScriptIDs`, `ScriptNames`
 - [x] Define table functions: `TableIDs`, `TableNames`
 - [x] Define financial functions: `FV`, `NPV`, `PMT`, `PV`
 - [x] Define logical functions: `Case`, `Choose`, `Evaluate`, `EvaluationError`, `If`, `IsEmpty`, `IsValid`, `IsValidExpression`
 - [x] Define special functions: `Combination`, `Extend`, `External`, `Factorial`, `GetNextSerialValue`, `Last`, `Let`, `PatternCount`, `Random`, `RGB`, `Self`, `SerialIncrement`, `SetPrecision`
- Color reference: `fgColor="8000FF"`, `fontStyle="1"` (bold)

#### Comments (Green)
- [x] Define block comment tokens: `/*` and `*/`
- [x] Define line comment token: `//`
- Color reference: `fgColor="008000"` (both styles)

#### String Delimiters (Pink)
- [x] Define string tokens: double quote `"` and single quote `'`
- Color reference: `fgColor="DB599D"`

#### Numbers (Red)
- [x] Define number pattern: integers and decimals
- Color reference: `fgColor="FF0000"`

#### Operators (Brown/Bold)
- [x] Define operator tokens: `+`, `-`, `*`, `/`, `^`, `=`, `≠`, `<`, `>`, `≤`, `≥`, `&`, `(`, `)`, `[`, `]`, `{`, `}`, `;`, `,`
- Color reference: `fgColor="804000"`, `fontStyle="1"` (bold)

### 2.2 Lexer Generation & Integration
- [x] Configure JFlex plugin in `build.gradle` to generate lexer from `.flex` file
- [x] Create `FileMakerCalculationTokenType` class for token type definitions
- [x] Create `FileMakerCalculationElementType` class for element types
- [x] Generate lexer adapter class from flex definition
- [x] Test lexer with sample FileMaker calculation code

---

## Phase 3: Syntax Highlighter

### 3.1 Syntax Highlighter Implementation
**File to create:**
- `src/main/java/dev/fmcuttingboard/language/FileMakerCalculationSyntaxHighlighter.java`

**Tasks:**
- [x] Create `FileMakerCalculationSyntaxHighlighter` implementing `SyntaxHighlighterBase`
- [x] Define `TextAttributesKey` constants matching Notepad++ colors:
  - `KEYWORD_CONTROL_FLOW` → Blue, Bold (`#0000FF`)
  - `KEYWORD_LOGICAL` → Dark Blue, Bold (`#006699`)
  - `KEYWORD_TYPE` → Orange, Bold (`#FF8000`)
  - `FUNCTION` → Purple, Bold (`#8000FF`)
  - `COMMENT` → Green (`#008000`)
  - `STRING` → Pink (`#DB599D`)
  - `NUMBER` → Red (`#FF0000`)
  - `OPERATOR` → Brown, Bold (`#804000`)
- [x] Implement `getTokenHighlights()` method to map tokens to attributes
- [x] Override `getHighlightingLexer()` to return FileMaker calculation lexer

### 3.2 Syntax Highlighter Factory
**File to create:**
- `src/main/java/dev/fmcuttingboard/language/FileMakerCalculationSyntaxHighlighterFactory.java`

**Tasks:**
- [x] Create factory class implementing `SyntaxHighlighterFactory`
- [x] Override `getSyntaxHighlighter()` to return highlighter instance
- [x] Register factory in `plugin.xml` with `<syntaxHighlighter>` extension point

---

## Phase 4: Visual Enhancements

### 4.1 File Type Icon
**Directory to create:**
- `src/main/resources/icons/` (already exists in git status)

**Tasks:**
- [x] Design 16x16 `.fmcalc` file type icon (SVG format recommended)
- [x] Add icon to `src/main/resources/icons/filemaker-calculation.svg`
- [x] Update `FileMakerCalculationFileType.getIcon()` to reference new icon
- [x] Test icon appearance in Project view and editor tabs

---

## Phase 5: Testing & Validation

### 5.1 Manual Testing
- [x] Create sample `.fmcalc` files with various FileMaker calculations
- [x] Verify syntax highlighting for all token types
- [x] Test comment highlighting (both `//` and `/* */`)
- [x] Test string highlighting (both `"` and `'`)
- [ ] Verify function name recognition (all 250+ functions)
- [x] Test nested expressions and complex calculations
- [x] Verify file type recognition in IDE

### 5.2 Edge Cases
- [x] Test calculations with mixed case keywords (should be case-sensitive)
 - [x] Test multi-line calculations
 - [ ] Test calculations with special characters
 - [ ] Test Unicode in strings (Japanese characters for Japanese functions)
 - [ ] Test unterminated strings/comments
 - [ ] Test empty files

### 5.3 Integration Testing
- [ ] Verify plugin loads without errors
- [ ] Test performance with large calculation files
- [ ] Verify no conflicts with existing plugin features (clipboard functionality)
- [ ] Test across Windows and macOS (if applicable)

---

## Phase 6: Documentation & Polish

### 6.1 Code Documentation
- [ ] Add JavaDoc comments to all language classes
- [ ] Document token patterns in flex file
- [ ] Add inline comments for complex regex patterns

### 6.2 User Documentation
- [ ] Update main README with FileMaker Calculation language support info
- [ ] Create usage examples for `.fmcalc` files
- [ ] Document supported FileMaker calculation syntax

### 6.3 Build Configuration
- [ ] Verify JFlex integration in Gradle build
- [ ] Test plugin build with new language support
- [ ] Update `plugin.xml` version and change notes

---

## Optional Future Enhancements

### Advanced IDE Features (Post-MVP)
- [ ] Code completion for FileMaker functions
- [ ] Parameter hints for functions
- [ ] Error detection for invalid syntax
- [ ] Brace matching for parentheses and brackets
- [ ] Code folding for `Let()`, `Case()`, `If()` blocks
- [ ] Live templates for common calculation patterns
- [ ] Integration with existing FileMaker clipboard features

---

## Implementation Notes

**Color Mapping Reference:**
- Notepad++ → JetBrains IntelliJ
- `#0000FF` (Blue) → Control flow keywords
- `#006699` (Dark Blue) → Logical operators
- `#FF8000` (Orange) → Type keywords
- `#8000FF` (Purple) → Functions
- `#008000` (Green) → Comments
- `#DB599D` (Pink) → Strings
- `#FF0000` (Red) → Numbers
- `#804000` (Brown) → Operators

**File Extension:**
- `.fmcalc` - FileMaker Calculation files

**Case Sensitivity:**
- Language is not case-sensitive (despite Notepad++ XML: `caseIgnored="no"`)

**Estimated Effort:**
- Phase 1: 2-3 hours
- Phase 2: 4-6 hours (lexer is the most complex)
- Phase 3: 2-3 hours
- Phase 4: 1 hour
- Phase 5: 2-3 hours
- Phase 6: 1-2 hours
- **Total: ~15-20 hours**
