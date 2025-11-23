# FileMaker Calculation Language - First-Class IDE Implementation Roadmap

## Current State Analysis

✅ **Completed:**
- File type (.fmcalc) registered
- Lexer with token types (keywords, functions, operators, comments, strings)
- Syntax highlighting
- Brace matching
- Code folding (Let, Case, If) (needs to be expanded to include other structures too)

## Phase 1: Complete Function Knowledge Base
**Goal:** IDE knows ALL FileMaker functions with complete parameter metadata

### 1.1 Consolidate Function Information
- Extract all functions from:
  - `FileMakerCalcs_InNotepadPlusPlus.xml` (~200 functions in Words4)
  - `filemaker-vscode-bundle-master/syntaxes/FileMaker.tmLanguage` (line 77: 280+ functions)
  - resources/filemaker_functions.json (copy of the VSCode snippets with detailed signatures and parameters)
- Cross-reference with official FileMaker documentation to ensure completeness
- Create comprehensive function registry

Progress: Implemented best‑effort consolidation utilities
- FunctionMetadataLoader.extractSnippetFunctionNames(String) parses VSCode snippet keys
- FunctionMetadataLoader.parseVsCodeSnippets(String) builds FunctionMetadata from snippet bodies (heuristic)
- Added unit test to ensure we extract 200+ function names from the bundled VSCode snippets

### 1.2 Build Function Metadata Registry
- Create `FileMakerFunctionRegistry.java` class
- Populate with all functions using `FunctionMetadata` and `FunctionParameter`
- Organize by categories: Math, Text, Date/Time, Logical, Get(), Aggregate, etc.
- Include parameter names, types, optional/required status, return types, descriptions
- Use VSCode snippets as primary source for parameter signatures

### 1.3 Update Lexer with Complete Function List
- Update `filemaker-calculation.flex` with all functions
- Ensure all Get() parameter constants are recognized
- Add FileMaker-specific constants (JSONArray, JSONBoolean, True/False, etc.)
- Recognize script variables ($var, $$globalVar)

## Phase 2: Enhanced Code Completion
**Goal:** IntelliSense-quality code completion

### 2.1 Context-Aware Completion
- Replace hardcoded list in `FileMakerCalculationCompletionContributor` with `FileMakerFunctionRegistry`
- Show function signature in completion tooltip
- Display parameter names and types
- Show function category and description
- Add completion for Get() parameter constants

### 2.2 Smart Completion Features
- Template-based insertion with parameter placeholders
- Tab stops between parameters (using IntelliJ's template system)
- Variable completion ($vars, $$globalVars)
- Field reference completion (if context available)

## Phase 3: Advanced Parameter Hints
**Goal:** Real-time parameter guidance like Visual Studio IntelliSense

### 3.1 Enhanced Parameter Info Handler
- Update `FileMakerCalculationParameterInfoHandler` to use `FileMakerFunctionRegistry`
- Show full function signature with all parameters
- Highlight current parameter position
- Display parameter types and descriptions
- Support for variadic functions (e.g., Case with multiple test/result pairs)

### 3.2 Optional Parameter Indicators
- Mark optional parameters in hints
- Show default values where applicable
- Handle functions with variable parameter counts

## Phase 4: Grammar-Based PSI Parser
**Goal:** Structural understanding of calculation syntax

### 4.1 Define BNF Grammar
- Create `FileMakerCalculation.bnf` using Grammar-Kit (placeholder added documenting intended grammar) ✓
- Define structure for:
  - Function calls ✓
  - Expressions (parenthesized, identifiers, literals) ✓
  - Note: Binary/unary operators, Let/Case/If, fields/variables to be expanded in future iterations

### 4.2 Generate PSI Classes
- Provide minimal PSI element wrappers for key nodes (function call, arg list, argument, paren expr, identifier, literal) ✓
- Create PSI tree structure that represents calculation semantics (partial) ✓
- Implement visitors later when grammar expands

### 4.3 Replace Bootstrap Parser
- Replace `FileMakerCalculationPsiParser` flat parser with a lightweight recursive‑descent parser that builds structured nodes ✓
- Update `FileMakerCalculationParserDefinition` to map element types to PSI implementations ✓
- Basic verification with sample inputs; further tests to be added in Phase 9

Refinement (2025‑11‑23):
- Introduced basic operator parsing with precedence (arithmetic, comparison, logical AND/OR) ✓
- Added unary NOT handling ✓
- New PSI node types: `BINARY_EXPRESSION`, `UNARY_EXPRESSION` ✓
- ParserDefinition updated to expose new PSI nodes ✓

## Phase 5: Intelligent Code Formatting
**Goal:** Reformat Code that produces human-readable calculations

### 5.1 Define Formatting Rules
- Map FileMaker constructs to JetBrains formatting concepts:
  - Block structures: Let(), Case(), If() → indentation blocks
  - Parameter separators (semicolons) → spacing rules
  - Operators → spacing rules (conservative default: one space around operators)
  - Parentheses → no space just inside "(" or before ")"
  - Line breaks → indent contents between parentheses for multi-line argument lists

### 5.2 Implement Formatting Model Builder
- Replace stub in `FileMakerCalculationFormattingModelBuilder`
- Create Block implementations for each PSI element type
- Define spacing rules between tokens
- Define indentation rules for nested structures
- Handle alignment of parameters

### 5.3 Code Style Settings
- Create configurable code style settings UI
- Options: indent size, space around operators, semicolon spacing, line wrapping
- Integration with IDE's Code Style preferences

## Phase 6: Advanced Language Features
**Goal:** IDE features matching mainstream languages

### 6.1 Enhanced Error Detection
- Update `FileMakerCalculationAnnotator` with PSI-based validation
- Type checking where possible
- Function parameter count validation
- Undefined variable warnings
- Unmatched parentheses/brackets

### 6.2 Quick Fixes
- Implement intention actions for common issues
- Auto-complete missing parameters
- Convert between inline and stacked formats
- Add missing semicolons

### 6.3 Refactoring Support
- Rename variable refactoring
- Extract to Let variable
- Inline Let variable
- Convert If to Case (and vice versa)

### 6.4 Code Navigation
- Find Usages for variables
- Go to Declaration for variables
- Structure view showing Let variables, nested functions
- Breadcrumbs navigation

## Phase 7: Live Templates & Snippets
**Goal:** Rapid calculation authoring

### 7.1 Port VSCode Snippets
- Convert snippets from `resources/filemaker_functions.json`
- Create IntelliJ Live Templates
- Include both inline and stacked variants (Let, Case, If, ExecuteSQL, etc.)

### 7.2 Custom Live Template Context
- Define FileMaker-specific template context
- Enable templates only in .fmcalc files

## Phase 8: Documentation & Help
**Goal:** Inline documentation for functions

### 8.1 Quick Documentation Provider
- Implement `DocumentationProvider` for FileMaker language
- Show function documentation on Ctrl+Q (Quick Documentation)
- Include parameter descriptions, return type, examples
- Source from official FileMaker documentation

### 8.2 External Documentation Links
- Link to FileMaker online documentation
- Context-sensitive help URLs

## Phase 9: Testing & Polish
**Goal:** Production-ready implementation

### 9.1 Comprehensive Testing
- Unit tests for lexer, parser, formatting
- Integration tests for completion, hints, validation
- Edge case testing (nested functions, complex expressions)
- Performance testing with large calculations

### 9.2 Performance Optimization
- Optimize function registry lookup
- Cache PSI structures where appropriate
- Profile and optimize hot paths

### 9.3 Documentation
- User guide for FileMaker calculation editing
- Developer documentation for extending the language support

## Implementation Strategy

**AI-Sized Work Units:** Each phase/task should take 1-4 hours of focused development

**Recommended Order:**
1. Phase 1 (Function Knowledge) - Foundation for everything else
2. Phase 4.1-4.2 (Grammar) - Enables structural understanding
3. Phase 2 (Completion) - High-value user feature
4. Phase 3 (Parameter Hints) - High-value user feature
5. Phase 5 (Formatting) - High-value user feature
6. Phase 4.3 (Parser Integration) - Complete PSI implementation
7. Phase 6 (Advanced Features) - Progressive enhancement
8. Phase 7 (Templates) - Developer productivity
9. Phase 8 (Documentation) - Polish
10. Phase 9 (Testing) - Quality assurance

**Dependencies:**
- Phase 2-9 depend on Phase 1 (function registry)
- Phase 5-6 depend on Phase 4 (PSI parser)
- Phase 3 can use Phase 1 immediately without waiting for Phase 4

## Reference Materials

### Resources Directory
- `resources/FileMakerCalcs_InNotepadPlusPlus.xml` - Notepad++ language definition with function list
- `resources/filemaker-vscode-bundle-master/` - VSCode extension with comprehensive function definitions
  - `syntaxes/FileMaker.tmLanguage` - TextMate grammar with 280+ functions
  - `snippets/filemaker.json` - Detailed function signatures with parameters

### Key Implementation Files
- `src/main/java/dev/fmcuttingboard/language/`
  - `filemaker-calculation.flex` - JFlex lexer definition
  - `FunctionMetadata.java` - Function metadata model
  - `FunctionParameter.java` - Function parameter model
  - `completion/FileMakerCalculationCompletionContributor.java` - Code completion
  - `hints/FileMakerCalculationParameterInfoHandler.java` - Parameter hints
  - `format/FileMakerCalculationFormattingModelBuilder.java` - Code formatting
  - `parser/FileMakerCalculationPsiParser.java` - PSI parser (to be replaced)

## Progress Tracking

- [x] Phase 1: Complete Function Knowledge Base
  - [x] 1.1 Consolidate Function Information
  - [x] 1.2 Build Function Metadata Registry
  - [x] 1.3 Update Lexer with Complete Function List
- [x] Phase 2: Enhanced Code Completion
- [x] Phase 3: Advanced Parameter Hints
- [x] Phase 4: Grammar-Based PSI Parser
- [x] Phase 5: Intelligent Code Formatting
- [ ] Phase 6: Advanced Language Features
- [ ] Phase 7: Live Templates & Snippets
- [ ] Phase 8: Documentation & Help
- [ ] Phase 9: Testing & Polish
