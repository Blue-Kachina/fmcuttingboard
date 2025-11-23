/*
 * JFlex lexer for FileMaker Calculation language (Phase 2)
 */
package dev.fmcuttingboard.language;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

%%

%class _FileMakerCalculationLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%final
%eof{
  return;
%eof}
%state COMMENT

%%

<YYINITIAL>{
  [ \t\f\r\n]+            { return FileMakerCalculationTokenType.WHITE_SPACE; }

  // Comments
  "//"[^\n\r]*              { return FileMakerCalculationTokenType.LINE_COMMENT; }
  "/\*"                      { yybegin(COMMENT); return FileMakerCalculationTokenType.BLOCK_COMMENT; }

  // Strings
  [\"]([^\\\r\n\"]|\\.)*[\"] { return FileMakerCalculationTokenType.STRING; }
  [']([^\\\r\n\']|\\.)*[']     { return FileMakerCalculationTokenType.STRING; }

  // Numbers (integers, decimals with optional exponent)
  ([0-9]+(\.[0-9]+)?([eE][+-]?[0-9]+)?|\.[0-9]+([eE][+-]?[0-9]+)?)
                               { return FileMakerCalculationTokenType.NUMBER; }

  // Keywords Group 1 - Control Flow
  "if"                        { return FileMakerCalculationTokenType.KEYWORD_CONTROL; }
  "case"                      { return FileMakerCalculationTokenType.KEYWORD_CONTROL; }

  // Keywords Group 2 - Logical Operators
  "and"                       { return FileMakerCalculationTokenType.KEYWORD_LOGICAL; }
  "or"                        { return FileMakerCalculationTokenType.KEYWORD_LOGICAL; }
  "not"                       { return FileMakerCalculationTokenType.KEYWORD_LOGICAL; }
  "xor"                       { return FileMakerCalculationTokenType.KEYWORD_LOGICAL; }

  // Keywords Group 3 - Type Keywords
  "boolean"                   { return FileMakerCalculationTokenType.KEYWORD_TYPE; }
  "byte"                      { return FileMakerCalculationTokenType.KEYWORD_TYPE; }
  "char"                      { return FileMakerCalculationTokenType.KEYWORD_TYPE; }
  "class"                     { return FileMakerCalculationTokenType.KEYWORD_TYPE; }
  "double"                    { return FileMakerCalculationTokenType.KEYWORD_TYPE; }
  "float"                     { return FileMakerCalculationTokenType.KEYWORD_TYPE; }
  "int"                       { return FileMakerCalculationTokenType.KEYWORD_TYPE; }
  "interface"                 { return FileMakerCalculationTokenType.KEYWORD_TYPE; }
  "long"                      { return FileMakerCalculationTokenType.KEYWORD_TYPE; }
  "short"                     { return FileMakerCalculationTokenType.KEYWORD_TYPE; }
  "void"                      { return FileMakerCalculationTokenType.KEYWORD_TYPE; }

  // Keywords Group 4 - Functions (partial groups as per roadmap)
  // Mathematical functions
  "Abs"|"Acos"|"Asin"|"Atan"|"Ceiling"|"Cos"|"Degrees"|"Div"|"Exp"|"Floor"|"Int"|"Lg"|"Ln"|"Log"|"Max"|"Min"|"Mod"|"Pi"|"Radians"|"Round"|"Sign"|"Sin"|"Sqrt"|"Tan"|"Truncate"
                               { return FileMakerCalculationTokenType.KEYWORD_FUNCTION; }
  // Statistical functions
  "Average"|"Count"|"StDev"|"StDevP"|"Sum"|"Variance"|"VarianceP"
                               { return FileMakerCalculationTokenType.KEYWORD_FUNCTION; }
  // Text functions
  "Char"|"Code"|"Exact"|"Filter"|"FilterValues"|"Left"|"LeftValues"|"LeftWords"|"Length"|"Lower"|"Middle"|"MiddleValues"|"MiddleWords"|"Position"|"Proper"|"Quote"|"Replace"|"Right"|"RightValues"|"RightWords"|"Substitute"|"TextColor"|"TextColorRemove"|"TextFont"|"TextFontRemove"|"TextFormatRemove"|"TextSize"|"TextSizeRemove"|"TextStyleAdd"|"TextStyleRemove"|"Trim"|"TrimAll"|"Upper"|"WordCount"
                               { return FileMakerCalculationTokenType.KEYWORD_FUNCTION; }
  // Date/time functions
  "Date"|"Day"|"DayName"|"DayNameJ"|"DayOfWeek"|"DayOfYear"|"Hour"|"Minute"|"Month"|"MonthName"|"MonthNameJ"|"Seconds"|"Time"|"Timestamp"|"WeekOfYear"|"WeekOfYearFiscal"|"Year"|"YearName"
                               { return FileMakerCalculationTokenType.KEYWORD_FUNCTION; }
  // Conversion functions
  "GetAsBoolean"|"GetAsCSS"|"GetAsDate"|"GetAsNumber"|"GetAsSVG"|"GetAsText"|"GetAsTime"|"GetAsTimestamp"|"GetAsURLEncoded"
                               { return FileMakerCalculationTokenType.KEYWORD_FUNCTION; }
  // Generic Get() family — highlight core function name
  "Get"                        { return FileMakerCalculationTokenType.KEYWORD_FUNCTION; }
  /*
   * Note: The FileMaker platform defines 100+ Get() variants (from the Notepad++ XML Words4 list).
   * We purposefully highlight only the function identifier "Get" as a KEYWORD_FUNCTION so that
   * the argument token (e.g., AccountName) remains a normal IDENTIFIER token. This preserves
   * consistent operator/identifier highlighting across the language while still marking function calls.
   *
   * Authoritative list captured in resources/FileMakerCalcs_InNotepadPlusPlus.xml (Words4), including:
   *   Get(AccountExtendedPrivileges), Get(AccountName), Get(AccountPrivilegeSetName),
   *   Get(ActiveFieldContents), Get(ActiveFieldName), Get(ActiveFieldTableName),
   *   Get(ActiveLayoutObjectName), Get(ActiveModifierKeys), Get(ActivePortalRowNumber),
   *   Get(ActiveRepetitionNumber), Get(ActiveSelectionSize), Get(ActiveSelectionStart),
   *   Get(AllowAbortState), Get(AllowToolbarState), Get(ApplicationLanguage), Get(ApplicationVersion),
   *   Get(CalculationRepetitionNumber), Get(CurrentDate), Get(CurrentExtendedPrivileges),
   *   Get(CurrentHostTimestamp), Get(CurrentPrivilegeSetName), Get(CurrentTime), Get(CurrentTimestamp),
   *   Get(CustomMenuSetName), Get(DesktopPath), Get(DocumentsPath), Get(DocumentsPathListing),
   *   Get(ErrorCaptureState), Get(FileMakerPath), Get(FileName), Get(FilePath), Get(FileSize),
   *   Get(FoundCount), Get(HighContrastColor), Get(HighContrastState), Get(HostApplicationVersion),
   *   Get(HostIPAddress), Get(HostName), Get(LastError), Get(LastMessageChoice), Get(LastODBCError),
   *   Get(LayoutAccess), Get(LayoutCount), Get(LayoutName), Get(LayoutNumber), Get(LayoutTableName),
   *   Get(LayoutViewState), Get(MultiUserState), Get(NetworkProtocol), Get(PageNumber),
   *   Get(PreferencesPath), Get(PrinterName), Get(QuickFindText), Get(RecordAccess), Get(RecordID),
   *   Get(RecordModificationCount), Get(RecordNumber), Get(RecordOpenCount), Get(RecordOpenState),
   *   Get(RequestCount), Get(RequestOmitState), Get(ScreenDepth), Get(ScreenHeight), Get(ScreenWidth),
   *   Get(ScriptName), Get(ScriptParameter), Get(ScriptResult), Get(SortState), Get(StatusAreaState),
   *   Get(SystemDrive), Get(SystemIPAddress), Get(SystemLanguage), Get(SystemNICAddress),
   *   Get(SystemPlatform), Get(SystemVersion), Get(TemporaryPath), Get(TextRulerVisible),
   *   Get(TotalRecordCount), Get(TriggerKeystroke), Get(TriggerModifierKeys), Get(UserCount),
   *   Get(UserName), Get(UseSystemFormatsState), Get(WindowContentHeight), Get(WindowContentWidth),
   *   Get(WindowDesktopHeight), Get(WindowDesktopWidth), Get(WindowHeight), Get(WindowLeft),
   *   Get(WindowMode), Get(WindowName), Get(WindowTop), Get(WindowVisible), Get(WindowWidth),
   *   Get(WindowZoomLevel)
   */
   // FileMaker constants
   "True"|"true"|"False"|"false" { return FileMakerCalculationTokenType.KEYWORD_TYPE; }
   "JSONArray"|"JSONBoolean"|"JSONNull"|"JSONNumber"|"JSONObject"|"JSONRaw"|"JSONString"
                                { return FileMakerCalculationTokenType.KEYWORD_TYPE; }
   // Text style constants (subset)
   (?i)"Plain"|"Bold"|"Italic"|"Underline"|"HighlightYellow"|"Condense"|"Extend"|"Strikethrough"|"SmallCaps"|"Superscript"|"Subscript"|"Uppercase"|"Lowercase"|"Titlecase"|"WordUnderline"|"DoubleUnderline"|"AllStyles"
                                { return FileMakerCalculationTokenType.KEYWORD_TYPE; }
   // Layout object attribute constants (subset)
   (?i)"objectType"|"hasFocus"|"objectName"|"containsFocus"|"isFrontPanel"|"isActive"|"isObjectHidden"|"bounds"|"left"|"right"|"top"|"bottom"|"width"|"height"|"rotation"|"startPoint"|"endPoint"|"source"|"content"|"enclosingObject"|"containedObjects"
                                { return FileMakerCalculationTokenType.KEYWORD_TYPE; }

   // Script variables ($local, $$global)
   ("$$"|"$")[A-Za-z_][A-Za-z0-9_]* { return FileMakerCalculationTokenType.IDENTIFIER; }
  // Field/database functions
  "DatabaseNames"|"FieldBounds"|"FieldComment"|"FieldIDs"|"FieldNames"|"FieldRepetitions"|"FieldStyle"|"FieldType"|"GetField"|"GetFieldName"|"GetNthRecord"|"GetRepetition"|"GetSummary"|"GetValue"|"Lookup"|"LookupNext"
                               { return FileMakerCalculationTokenType.KEYWORD_FUNCTION; }
  // Japanese text functions
  "Hiragana"|"KanaHankaku"|"KanaZenkaku"|"KanjiNumeral"|"Katakana"|"NumToJText"|"RomanHankaku"|"RomanZenkaku"
                               { return FileMakerCalculationTokenType.KEYWORD_FUNCTION; }
  // Layout/window functions
  "GetLayoutObjectAttribute"|"LayoutIDs"|"LayoutNames"|"LayoutObjectNames"|"WindowNames"
                               { return FileMakerCalculationTokenType.KEYWORD_FUNCTION; }
  // List functions
  "List"|"ValueCount"|"ValueListIDs"|"ValueListItems"|"ValueListNames"
                               { return FileMakerCalculationTokenType.KEYWORD_FUNCTION; }
  // Script/relation functions
  "RelationInfo"|"ScriptIDs"|"ScriptNames"
                               { return FileMakerCalculationTokenType.KEYWORD_FUNCTION; }
  // Table functions
  "TableIDs"|"TableNames"
                               { return FileMakerCalculationTokenType.KEYWORD_FUNCTION; }
  // Financial functions
  "FV"|"NPV"|"PMT"|"PV"
                               { return FileMakerCalculationTokenType.KEYWORD_FUNCTION; }
  // Logical/special functions
  "Case"|"Choose"|"Evaluate"|"EvaluationError"|"If"|"IsEmpty"|"IsValid"|"IsValidExpression"|"Combination"|"Extend"|"External"|"Factorial"|"GetNextSerialValue"|"Last"|"Let"|"PatternCount"|"Random"|"RGB"|"Self"|"SerialIncrement"|"SetPrecision"
                               { return FileMakerCalculationTokenType.KEYWORD_FUNCTION; }

  // Operators
  "<="|">="|"<>"     { return FileMakerCalculationTokenType.OPERATOR; }
  [\+\-\*\/=\^<>&;,] { return FileMakerCalculationTokenType.OPERATOR; }
  // Braces and parentheses as distinct tokens (for brace matcher/folding)
  "(" { return FileMakerCalculationTokenType.LPAREN; }
  ")" { return FileMakerCalculationTokenType.RPAREN; }
  "[" { return FileMakerCalculationTokenType.LBRACKET; }
  "]" { return FileMakerCalculationTokenType.RBRACKET; }
  "{" { return FileMakerCalculationTokenType.LBRACE; }
  "}" { return FileMakerCalculationTokenType.RBRACE; }
  "≠"|"≤"|"≥"                 { return FileMakerCalculationTokenType.OPERATOR; }

  // Identifier
  [A-Za-z_][A-Za-z0-9_]*      { return FileMakerCalculationTokenType.IDENTIFIER; }

  .                            { return TokenType.BAD_CHARACTER; }
}

<COMMENT>{
  "\*/"                      { yybegin(YYINITIAL); return FileMakerCalculationTokenType.BLOCK_COMMENT; }
  [^]                         { return FileMakerCalculationTokenType.BLOCK_COMMENT; }
}
