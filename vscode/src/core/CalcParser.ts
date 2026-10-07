// Port of jetbrains/.../language/parser/FileMakerCalculationPsiParser.java, including its recovery quirks, so both
// IDEs see the same structure (argument counts depend on it). Mimics PsiBuilder: whitespace and comments are
// skipped, markers close to the last consumed token, and a later-closed marker with the same span is the parent.
// Pinned by shared/fixtures/golden/cases.json "diagnostics".
import language from '../../../shared/data/calc-language.json';
import { tokenize, type Token, type TokenType } from './CalcLexer';

export type NodeType =
  | 'FILE' | 'FUNCTION_CALL' | 'ARG_LIST' | 'ARGUMENT' | 'PAREN_EXPRESSION' | 'IDENTIFIER_EXPRESSION' | 'LITERAL'
  | 'BINARY_EXPRESSION' | 'UNARY_EXPRESSION' | 'BRACKET_LIST' | 'REPETITION_EXPRESSION';

export interface CalcNode {
  type: NodeType;
  start: number;
  end: number;
  /** Composite children (like PsiElement.getChildren()) */
  children: CalcNode[];
  /** Tokens directly inside this node, not inside a child */
  leaves: Token[];
}

export interface ParsedCalc {
  text: string;
  /** Every token, including whitespace and comments */
  tokens: Token[];
  root: CalcNode;
}

/** Binary precedence (higher binds tighter) from shared/data/calc-language.json, keyed by lowercase symbol. */
const BINARY_PRECEDENCE = new Map<string, number>();
for (const op of language.operators) {
  if (op.arity !== 2) continue;
  BINARY_PRECEDENCE.set(op.symbol.toLowerCase(), op.precedence);
  for (const alt of (op as { alternates?: string[] }).alternates ?? []) BINARY_PRECEDENCE.set(alt.toLowerCase(), op.precedence);
}

const SKIPPED: ReadonlySet<TokenType> = new Set(['WHITE_SPACE', 'LINE_COMMENT', 'BLOCK_COMMENT']);
const LITERALS: ReadonlySet<TokenType> = new Set([
  'NUMBER', 'STRING', 'CONSTANT', 'GET_CONSTANT', 'FIELD_REFERENCE', 'QUOTED_NAME', 'PARAGRAPH_MARK',
]);

interface Closed {
  type: NodeType;
  from: number; // index into significant tokens
  to: number; // exclusive
  order: number;
}

class Marker {
  constructor(private readonly b: Builder, readonly from: number) {}
  done(type: NodeType): void {
    this.b.closed.push({ type, from: this.from, to: this.b.pos, order: this.b.closed.length });
  }
  precede(): Marker {
    return new Marker(this.b, this.from);
  }
}

class Builder {
  pos = 0;
  readonly closed: Closed[] = [];
  constructor(readonly text: string, readonly tokens: Token[]) {}
  eof(): boolean { return this.pos >= this.tokens.length; }
  type(): TokenType | undefined { return this.tokens[this.pos]?.type; }
  tokenText(): string | undefined {
    const t = this.tokens[this.pos];
    return t ? this.text.substring(t.start, t.end) : undefined;
  }
  advance(): void { if (!this.eof()) this.pos++; }
  mark(): Marker { return new Marker(this, this.pos); }
}

export function parse(text: string): ParsedCalc {
  const all = tokenize(text);
  const significant = all.filter((t) => !SKIPPED.has(t.type));
  const b = new Builder(text, significant);
  parseExpression(b);
  while (!b.eof()) b.advance();
  return { text, tokens: all, root: buildTree(text, significant, b.closed) };
}

// ----- grammar (1:1 with FileMakerCalculationPsiParser) -----

function parseExpression(b: Builder): void {
  if (b.eof()) return;
  parseBinary(b, 0);
}

function parseArgumentList(b: Builder): void {
  const list = b.mark();
  if (b.type() === 'RPAREN') {
    list.done('ARG_LIST');
    return;
  }
  parseArgument(b);
  while (isSemicolon(b)) {
    b.advance();
    parseArgument(b);
  }
  list.done('ARG_LIST');
}

function parseArgument(b: Builder): void {
  const arg = b.mark();
  parseExpression(b);
  arg.done('ARGUMENT');
}

function parseBinary(b: Builder, minPrec: number): void {
  let left = b.mark();
  parseUnary(b);
  for (;;) {
    const prec = currentOperatorPrecedence(b);
    if (prec < minPrec) break;
    b.advance();
    parseBinary(b, prec + 1);
    left.done('BINARY_EXPRESSION');
    left = left.precede();
  }
}

function parseUnary(b: Builder): void {
  const isNot = b.type() === 'KEYWORD_LOGICAL' && b.tokenText()?.toLowerCase() === 'not';
  const isSign = b.type() === 'OPERATOR' && (b.tokenText() === '-' || b.tokenText() === '+');
  if (isNot || isSign) {
    const m = b.mark();
    b.advance();
    parseUnary(b);
    m.done('UNARY_EXPRESSION');
    return;
  }
  parsePostfix(b);
}

function parsePostfix(b: Builder): void {
  const isBracketList = b.type() === 'LBRACKET';
  const m = b.mark();
  parsePrimary(b);
  if (!isBracketList && b.type() === 'LBRACKET') {
    while (b.type() === 'LBRACKET') parseBracketList(b);
    m.done('REPETITION_EXPRESSION');
  }
}

function parseBracketList(b: Builder): void {
  const m = b.mark();
  b.advance(); // '['
  if (b.type() !== 'RBRACKET') {
    parseExpression(b);
    while (isSemicolon(b)) {
      b.advance();
      if (b.type() === 'RBRACKET') break;
      parseExpression(b);
    }
  }
  if (b.type() === 'RBRACKET') b.advance();
  m.done('BRACKET_LIST');
}

function parsePrimary(b: Builder): void {
  if (b.eof()) return;
  const t = b.type()!;
  if (t === 'IDENTIFIER' || t === 'KEYWORD_FUNCTION') {
    const m = b.mark();
    b.advance();
    if (b.type() === 'LPAREN') {
      b.advance();
      parseArgumentList(b);
      if (b.type() === 'RPAREN') b.advance();
      m.done('FUNCTION_CALL');
    } else {
      m.done('IDENTIFIER_EXPRESSION');
    }
    return;
  }
  if (t === 'LBRACKET') {
    parseBracketList(b);
    return;
  }
  if (t === 'LPAREN') {
    const m = b.mark();
    b.advance();
    parseExpression(b);
    if (b.type() === 'RPAREN') b.advance();
    m.done('PAREN_EXPRESSION');
    return;
  }
  if (LITERALS.has(t)) {
    const m = b.mark();
    b.advance();
    m.done('LITERAL');
    return;
  }
  b.advance(); // fallback: consume one token to prevent an infinite loop
}

function isSemicolon(b: Builder): boolean {
  return b.type() === 'OPERATOR' && b.tokenText() === ';';
}

function currentOperatorPrecedence(b: Builder): number {
  const t = b.type();
  if (t !== 'OPERATOR' && t !== 'KEYWORD_LOGICAL') return -1;
  return BINARY_PRECEDENCE.get(b.tokenText()!.toLowerCase()) ?? -1;
}

// ----- tree -----

function buildTree(text: string, tokens: Token[], closed: Closed[]): CalcNode {
  // Parents first: earlier start, then wider span, then closed later (PsiBuilder markers nest LIFO)
  const sorted = [...closed].sort((a, b) => a.from - b.from || b.to - a.to || b.order - a.order);
  const root: CalcNode & { from: number; to: number } = {
    type: 'FILE', start: 0, end: text.length, children: [], leaves: [], from: 0, to: tokens.length,
  };
  const stack: (CalcNode & { from: number; to: number })[] = [root];
  for (const c of sorted) {
    while (stack.length > 1 && !(c.from >= stack[stack.length - 1].from && c.to <= stack[stack.length - 1].to)) stack.pop();
    const empty = c.to <= c.from;
    const startOffset = empty ? (tokens[c.from]?.start ?? text.length) : tokens[c.from].start;
    const endOffset = empty ? startOffset : tokens[c.to - 1].end;
    const node = { type: c.type, start: startOffset, end: endOffset, children: [], leaves: [], from: c.from, to: c.to };
    stack[stack.length - 1].children.push(node);
    stack.push(node);
  }
  // Leaves: each token belongs to the deepest node covering it
  const assign = (node: CalcNode & { from: number; to: number }) => {
    let i = node.from;
    for (const child of node.children as (CalcNode & { from: number; to: number })[]) {
      for (; i < child.from; i++) node.leaves.push(tokens[i]);
      assign(child);
      i = Math.max(i, child.to);
    }
    for (; i < node.to; i++) node.leaves.push(tokens[i]);
  };
  assign(root);
  return root;
}

/** Pre-order traversal (like PsiTreeUtil.findChildrenOfType). */
export function findAll(node: CalcNode, type: NodeType): CalcNode[] {
  const out: CalcNode[] = [];
  const visit = (n: CalcNode) => {
    for (const c of n.children) {
      if (c.type === type) out.push(c);
      visit(c);
    }
  };
  visit(node);
  return out;
}
