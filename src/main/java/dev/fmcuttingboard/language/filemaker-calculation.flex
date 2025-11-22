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
%eof{ return; %eof}
%state COMMENT

WHITESPACE = [\ \t\f\r\n\v]+
ID_START = [A-Za-z_]
ID_PART = [A-Za-z0-9_]
DIGIT = [0-9]
INT = {DIGIT}+
DEC = {DIGIT}+\.({DIGIT}+)?|\.{DIGIT}+
EXP = ([eE][+-]?{DIGIT}+)
NUMBER = ({DEC}{EXP}?|{INT}{EXP}?)

LINE_COMMENT = "//"[^\n\r]*
BLOCK_COMMENT_START = "/\*"
BLOCK_COMMENT_END = "\*/"

DQ_STRING = \"([^\\\n\r\"]|\\.)*\"
SQ_STRING = '([^\\\n\r']|\\.)*'

%%

<YYINITIAL>{
  {WHITESPACE}                { return FileMakerCalculationTokenType.WHITE_SPACE; }

  // Comments
  {LINE_COMMENT}              { return FileMakerCalculationTokenType.LINE_COMMENT; }
  {BLOCK_COMMENT_START}       { yybegin(COMMENT); return FileMakerCalculationTokenType.BLOCK_COMMENT; }

  // Strings
  {DQ_STRING}                 { return FileMakerCalculationTokenType.STRING; }
  {SQ_STRING}                 { return FileMakerCalculationTokenType.STRING; }

  // Numbers
  {NUMBER}                    { return FileMakerCalculationTokenType.NUMBER; }

  // Keywords Group 1 - Control Flow
  "if"                        { return FileMakerCalculationTokenType.KEYWORD_CONTROL; }
  "case"                      { return FileMakerCalculationTokenType.KEYWORD_CONTROL; }

  // Keywords Group 2 - Logical Operators
  "and"                       { return FileMakerCalculationTokenType.KEYWORD_LOGICAL; }
  "or"                        { return FileMakerCalculationTokenType.KEYWORD_LOGICAL; }
  "not"                       { return FileMakerCalculationTokenType.KEYWORD_LOGICAL; }

  // Operators and punctuation
  [\+\-\*\/=\^<>&;,()\[\]{}] { return FileMakerCalculationTokenType.OPERATOR; }
  "≠"|"≤"|"≥"                 { return FileMakerCalculationTokenType.OPERATOR; }

  // Identifier
  {ID_START}{ID_PART}*        { return FileMakerCalculationTokenType.IDENTIFIER; }

  .                            { return TokenType.BAD_CHARACTER; }
}

<COMMENT>{
  {BLOCK_COMMENT_END}         { yybegin(YYINITIAL); return FileMakerCalculationTokenType.BLOCK_COMMENT; }
  [^]                         { return FileMakerCalculationTokenType.BLOCK_COMMENT; }
}
