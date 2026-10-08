// Port of the JetBrains .fmcalc lexer: jetbrains/.../language/filemaker-calculation.flex (JFlex rules, same order,
// longest match wins, earlier rule wins ties) plus FileMakerCalculationLexerAdapter's identifier classification.
// Pinned by shared/fixtures/golden/cases.json "lexer". Block comments are one token here (JFlex emits one per
// character after "/*"); that is the only difference, and the golden harnesses merge them.
import { constants } from './CalcLanguage';
import { isJavaLetterOrDigit, isJavaWhitespace } from './javaCompat';

export type TokenType =
  | 'WHITE_SPACE' | 'BAD_CHARACTER' | 'IDENTIFIER' | 'NUMBER' | 'STRING' | 'LINE_COMMENT' | 'BLOCK_COMMENT'
  | 'OPERATOR' | 'LPAREN' | 'RPAREN' | 'LBRACKET' | 'RBRACKET' | 'LBRACE' | 'RBRACE' | 'KEYWORD_LOGICAL'
  | 'FIELD_REFERENCE' | 'QUOTED_NAME' | 'PARAGRAPH_MARK' | 'KEYWORD_FUNCTION' | 'GET_CONSTANT' | 'CONSTANT';

export interface Token {
  type: TokenType;
  start: number;
  end: number;
}

const NAME = '[\\p{L}_][\\p{L}\\p{Nd}_.]*';

// JFlex rule order matters for ties (e.g. "and" is KEYWORD_LOGICAL, not a name)
const RULES: [RegExp, TokenType][] = [
  [/[ \t\f\r\n]+/uy, 'WHITE_SPACE'],
  [/\/\/[^\n\r]*/uy, 'LINE_COMMENT'],
  [/\/\*[\s\S]*?(?:\*\/|$)/uy, 'BLOCK_COMMENT'],
  [/"(?:[^\\"]|\\[\s\S])*"?/uy, 'STRING'],
  [/(?:[0-9]+(?:\.[0-9]+)?(?:[eE][+-]?[0-9]+)?|\.[0-9]+(?:[eE][+-]?[0-9]+)?)/uy, 'NUMBER'],
  [/\$\{[^}]*\}?/uy, 'QUOTED_NAME'],
  [new RegExp(`${NAME}[ \\t]*::[ \\t]*${NAME}`, 'uy'), 'FIELD_REFERENCE'],
  [/(?:and|or|xor|not)/iuy, 'KEYWORD_LOGICAL'],
  [/\$\$?[\p{L}_~][\p{L}\p{Nd}_.~]*/uy, 'IDENTIFIER'],
  [/~[\p{L}\p{Nd}_.~]*/uy, 'IDENTIFIER'],
  [/¶/uy, 'PARAGRAPH_MARK'],
  [/(?:<=|>=|<>|≠|≤|≥|::)/uy, 'OPERATOR'],
  [/[+\-*/=^<>&;,]/uy, 'OPERATOR'],
  [/\(/uy, 'LPAREN'],
  [/\)/uy, 'RPAREN'],
  [/\[/uy, 'LBRACKET'],
  [/\]/uy, 'RBRACKET'],
  [/\{/uy, 'LBRACE'],
  [/\}/uy, 'RBRACE'],
  [new RegExp(NAME, 'uy'), 'IDENTIFIER'],
  [/[\s\S]/uy, 'BAD_CHARACTER'],
];

let constantNames: Set<string> | undefined;
function isConstantName(lower: string): boolean {
  constantNames ??= new Set(constants().map((c) => c.name.toLowerCase()));
  return constantNames.has(lower);
}

export function tokenize(text: string): Token[] {
  const tokens: Token[] = [];
  let pos = 0;
  while (pos < text.length) {
    let best: Token | undefined;
    for (const [re, type] of RULES) {
      re.lastIndex = pos;
      const m = re.exec(text);
      if (m && m[0].length > 0 && (!best || m[0].length > best.end - best.start)) {
        best = { type, start: pos, end: pos + m[0].length };
      }
    }
    // The last rule always matches one code point, so best is set
    const token = best!;
    if (token.type === 'IDENTIFIER') token.type = classify(text, token.start, token.end);
    tokens.push(token);
    pos = token.end;
  }
  return tokens;
}

/** FileMakerCalculationLexerAdapter.classify: calls, Get ( X ) constants and named constants. */
function classify(text: string, start: number, end: number): TokenType {
  if (text[start] === '$') return 'IDENTIFIER';

  let next = end;
  while (next < text.length && isJavaWhitespace(text[next])) next++;
  if (next < text.length && text[next] === '(') return 'KEYWORD_FUNCTION';

  if (isInsideGetCall(text, start)) return 'GET_CONSTANT';

  return isConstantName(text.substring(start, end).toLowerCase()) ? 'CONSTANT' : 'IDENTIFIER';
}

function isInsideGetCall(text: string, start: number): boolean {
  let i = start - 1;
  while (i >= 0 && isJavaWhitespace(text[i])) i--;
  if (i < 0 || text[i] !== '(') return false;
  i--;
  while (i >= 0 && isJavaWhitespace(text[i])) i--;
  const wordEnd = i + 1;
  while (i >= 0 && (isJavaLetterOrDigit(text[i]) || text[i] === '_' || text[i] === '.')) i--;
  return text.substring(i + 1, wordEnd).toLowerCase() === 'get';
}
