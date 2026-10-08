/*
 * JFlex lexer for the FileMaker calculation language.
 *
 * Deliberately generic: it knows FileMaker's syntax (strings, comments, operators, Table::Field, ${ }, ¶, variables),
 * but no function or constant names. FileMakerCalculationLexerAdapter classifies identifiers afterwards (calls,
 * Get ( X ) constants, named constants) from shared/data, mirroring the VS Code grammar
 * (vscode/scripts/generate-grammar.mjs), so both IDEs improve when the shared data grows.
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

NAME_START = [:letter:] | "_"
NAME_PART  = [:letter:] | [:digit:] | [_.]
NAME       = {NAME_START} {NAME_PART}*
WS         = [ \t\f\r\n]

%%

<YYINITIAL>{
  {WS}+                       { return FileMakerCalculationTokenType.WHITE_SPACE; }

  // Comments
  "//"[^\n\r]*                { return FileMakerCalculationTokenType.LINE_COMMENT; }
  // Block comments are one token each (an unterminated one runs to the end of the file); longest match picks the
  // terminated form whenever the comment is closed
  "/*" ( [^*] | "*"+ [^*/] )* "*"+ "/"
                              { return FileMakerCalculationTokenType.BLOCK_COMMENT; }
  "/*" ( [^*] | "*"+ [^*/] )* "*"*
                              { return FileMakerCalculationTokenType.BLOCK_COMMENT; }

  // Text constants: double quotes only, may span lines, \x escapes any character (\" \\ \¶).
  // An unterminated string runs to the end of the file (the annotator reports it).
  \"([^\\\"]|\\[^])*\"?      { return FileMakerCalculationTokenType.STRING; }

  // Numbers (integers, decimals, optional exponent)
  ([0-9]+(\.[0-9]+)?([eE][+-]?[0-9]+)?|\.[0-9]+([eE][+-]?[0-9]+)?)
                              { return FileMakerCalculationTokenType.NUMBER; }

  // ${ } quotes reserved names used as field or table names
  "${" [^}]* "}"?             { return FileMakerCalculationTokenType.QUOTED_NAME; }

  // Table::Field (one token, so "::" is never a bad character)
  {NAME} [ \t]* "::" [ \t]* {NAME}
                              { return FileMakerCalculationTokenType.FIELD_REFERENCE; }

  // Logical word operators, case-insensitive (Claris documents them as AND, OR, XOR, NOT)
  [aA][nN][dD] | [oO][rR] | [xX][oO][rR] | [nN][oO][tT]
                              { return FileMakerCalculationTokenType.KEYWORD_LOGICAL; }

  // Script variables ($local, $$global)
  ("$$"|"$") ({NAME_START}|"~") ({NAME_PART}|"~")*
                              { return FileMakerCalculationTokenType.IDENTIFIER; }

  // Paragraph mark (a return) outside strings
  "¶"                         { return FileMakerCalculationTokenType.PARAGRAPH_MARK; }

  // Operators (";" separates arguments; "," is kept so the comma-to-semicolon intention can find it)
  "<=" | ">=" | "<>" | "≠" | "≤" | "≥" | "::"
                              { return FileMakerCalculationTokenType.OPERATOR; }
  [\+\-\*\/=\^<>&;,]          { return FileMakerCalculationTokenType.OPERATOR; }

  // Braces and parentheses as distinct tokens (for brace matcher/folding)
  "("                         { return FileMakerCalculationTokenType.LPAREN; }
  ")"                         { return FileMakerCalculationTokenType.RPAREN; }
  "["                         { return FileMakerCalculationTokenType.LBRACKET; }
  "]"                         { return FileMakerCalculationTokenType.RBRACKET; }
  "{"                         { return FileMakerCalculationTokenType.LBRACE; }
  "}"                         { return FileMakerCalculationTokenType.RBRACE; }

  // Names: fields, Let variables, function names (classified by FileMakerCalculationLexerAdapter)
  {NAME}                      { return FileMakerCalculationTokenType.IDENTIFIER; }

  [^]                         { return TokenType.BAD_CHARACTER; }
}
