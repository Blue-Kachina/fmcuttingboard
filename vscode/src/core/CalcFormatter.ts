// The .fmcalc formatter (Claris spacing, pretty-printed). The JetBrains plugin has a line-for-line Java port
// (jetbrains/.../language/format/FmCalcFormatter.java); both are pinned by shared/fixtures/golden/cases.json
// "formatting", and the rules are documented in docs/fmcalc-formatting.md. Change them together.
import { lexicalError } from './CalcDiagnostics';
import { parse, type CalcNode, type ParsedCalc } from './CalcParser';
import type { Token } from './CalcLexer';

export interface FormatOptions {
  /** One level of indentation: "\t" or a run of spaces */
  indentUnit: string;
  /** Columns one level occupies (tab size for "\t") */
  indentWidth: number;
  /** A call that fits within this many columns stays on one line */
  maxWidth: number;
  /** Leave the variables of the outermost Let ( [ … ] ; … ) flush with the Let */
  doNotIndentTopLetVariables: boolean;
}

export const DEFAULT_FORMAT_OPTIONS: FormatOptions = {
  indentUnit: '\t',
  indentWidth: 4,
  maxWidth: 120,
  doNotIndentTopLetVariables: true,
};

const isComment = (t: Token) => t.type === 'LINE_COMMENT' || t.type === 'BLOCK_COMMENT';

/** The formatted text, or null when the calculation is not well-formed enough to format safely. */
export function format(text: string, options: FormatOptions = DEFAULT_FORMAT_OPTIONS): string | null {
  const parsed = parse(text);
  if (lexicalError(parsed)) return null;
  if (!isWellFormed(parsed)) return null;
  const root = parsed.root.children[0];
  if (!root) return null;
  return new Formatter(parsed, options, root).run(text.endsWith('\n'));
}

/** Balanced brackets, one root expression, and no tokens the parser could not place. */
function isWellFormed(parsed: ParsedCalc): boolean {
  let depth = 0;
  for (const t of parsed.tokens) {
    if (t.type === 'LPAREN' || t.type === 'LBRACKET') depth++;
    else if (t.type === 'RPAREN' || t.type === 'RBRACKET') depth--;
    else if (t.type === 'BAD_CHARACTER' || t.type === 'LBRACE' || t.type === 'RBRACE') return false;
  }
  if (depth !== 0) return false;
  if (parsed.root.children.length !== 1 || parsed.root.leaves.length > 0) return false;
  return !hasStrayLeaves(parsed.root.children[0]);
}

/** Leaves the formatter would not know where to put (parser recovery skipped them). */
function hasStrayLeaves(node: CalcNode): boolean {
  const allowed: Record<string, ReadonlySet<string>> = {
    FUNCTION_CALL: new Set(['KEYWORD_FUNCTION', 'IDENTIFIER', 'LPAREN', 'RPAREN']),
    ARG_LIST: new Set(['OPERATOR']),
    ARGUMENT: new Set(),
    PAREN_EXPRESSION: new Set(['LPAREN', 'RPAREN']),
    BRACKET_LIST: new Set(['LBRACKET', 'RBRACKET', 'OPERATOR']),
    REPETITION_EXPRESSION: new Set(),
    BINARY_EXPRESSION: new Set(['OPERATOR', 'KEYWORD_LOGICAL']),
    UNARY_EXPRESSION: new Set(['OPERATOR', 'KEYWORD_LOGICAL']),
    IDENTIFIER_EXPRESSION: new Set(['IDENTIFIER', 'KEYWORD_FUNCTION']),
    LITERAL: new Set(['NUMBER', 'STRING', 'CONSTANT', 'GET_CONSTANT', 'FIELD_REFERENCE', 'QUOTED_NAME', 'PARAGRAPH_MARK']),
  };
  const ok = allowed[node.type];
  if (!ok || node.leaves.some((t) => !ok.has(t.type))) return true;
  if ((node.type === 'ARG_LIST' || node.type === 'BRACKET_LIST') && node.leaves.some((t) => t.type === 'OPERATOR' && t.end - t.start !== 1)) return true;
  return node.children.some(hasStrayLeaves);
}

class Formatter {
  /** Comments printed before a token (on their own lines) and after it (same line) */
  private readonly leading = new Map<number, Token[]>();
  private readonly trailing = new Map<number, Token[]>();
  private readonly endComments: Token[] = [];

  private lines: string[] = [];
  private line = '';
  private lineIndent = 0;
  private pendingNewline = false;

  constructor(
    private readonly parsed: ParsedCalc,
    private readonly options: FormatOptions,
    private readonly root: CalcNode,
    /** Measuring mode: lay everything out on one line */
    private readonly flat = false,
  ) {
    this.attachComments();
  }

  run(endsWithNewline: boolean): string {
    this.startLine(0);
    this.render(this.root, 0);
    for (const c of this.endComments) {
      this.newline(0);
      this.write(this.text(c));
    }
    this.flushLine();
    const out = this.lines.join('\n');
    return endsWithNewline ? out + '\n' : out;
  }

  // ----- comments -----

  private attachComments(): void {
    const tokens = this.parsed.tokens;
    let prevCode: Token | undefined;
    let newlineSincePrev = false;
    const pendingLeading: Token[] = [];
    for (const t of tokens) {
      if (t.type === 'WHITE_SPACE') {
        if (this.text(t).includes('\n')) newlineSincePrev = true;
        continue;
      }
      if (isComment(t)) {
        if (prevCode && !newlineSincePrev && pendingLeading.length === 0) {
          push(this.trailing, prevCode.start, t);
          if (t.type === 'LINE_COMMENT') newlineSincePrev = true;
        } else {
          pendingLeading.push(t);
        }
        continue;
      }
      if (pendingLeading.length > 0) {
        this.leading.set(t.start, [...pendingLeading]);
        pendingLeading.length = 0;
      }
      prevCode = t;
      newlineSincePrev = false;
    }
    this.endComments.push(...pendingLeading);
  }

  private hasComments(node: CalcNode): boolean {
    return this.parsed.tokens.some((t) => isComment(t) && t.start >= node.start && t.start < node.end);
  }

  // ----- writer -----

  private text(t: Token): string {
    return this.parsed.text.substring(t.start, t.end);
  }

  private startLine(level: number): void {
    this.line = this.options.indentUnit.repeat(level);
    this.lineIndent = level;
  }

  private flushLine(): void {
    this.lines.push(this.line.replace(/[ \t]+$/, ''));
  }

  private atLineStart(): boolean {
    return this.line.length === this.options.indentUnit.length * this.lineIndent;
  }

  private newline(level: number): void {
    this.flushLine();
    this.startLine(level);
    this.pendingNewline = false;
  }

  private column(): number {
    return this.lineIndent * this.options.indentWidth + (this.line.length - this.options.indentUnit.length * this.lineIndent);
  }

  private write(s: string): void {
    this.line += s;
  }

  private space(): void {
    if (!this.atLineStart() && !this.line.endsWith(' ')) this.write(' ');
  }

  /** Writes a token with its comments; `level` is used for any line break the comments need. */
  private token(t: Token, level: number): void {
    const before = this.leading.get(t.start) ?? [];
    for (const c of before) {
      if (!this.atLineStart()) this.newline(level);
      this.write(this.text(c));
      this.newline(level);
    }
    if (this.pendingNewline) this.newline(level);
    this.write(this.text(t));
    for (const c of this.trailing.get(t.start) ?? []) {
      this.write(' ' + this.text(c));
      if (c.type === 'LINE_COMMENT') this.pendingNewline = true;
    }
  }

  // ----- layout decisions -----

  /** Must this node span several lines regardless of width? */
  private forcesBreak(node: CalcNode): boolean {
    if (this.hasComments(node)) return true;
    const visit = (n: CalcNode): boolean => {
      if (n.type === 'FUNCTION_CALL') {
        const name = this.callName(n).toLowerCase();
        const args = this.args(n);
        if (name === 'while') return true;
        if (name === 'case' && args.length >= 4) return true;
        if (name === 'let' && this.letBindings(n).length >= 2) return true;
      }
      return n.children.some(visit);
    };
    return visit(node);
  }

  private fits(node: CalcNode): boolean {
    if (this.flat) return true;
    if (this.forcesBreak(node)) return false;
    const flat = new Formatter(this.parsed, this.options, node, true);
    flat.startLine(0);
    flat.render(node, 0);
    return this.column() + flat.line.length <= this.options.maxWidth;
  }

  // ----- structure helpers -----

  private callName(call: CalcNode): string {
    return this.text(call.leaves[0]);
  }

  private args(call: CalcNode): CalcNode[] {
    const list = call.children.find((c) => c.type === 'ARG_LIST');
    return list ? list.children : [];
  }

  private separators(container: CalcNode | undefined): Token[] {
    return container ? container.leaves.filter((t) => t.type === 'OPERATOR') : [];
  }

  /** The definitions of Let ( [ a = 1 ; b = 2 ] ; … ), or [] when the first argument is not a [ ] list. */
  private letBindings(call: CalcNode): CalcNode[] {
    const first = this.args(call)[0]?.children[0];
    return first?.type === 'BRACKET_LIST' ? first.children : [];
  }

  // ----- rendering -----

  private render(node: CalcNode, level: number): void {
    switch (node.type) {
      case 'ARGUMENT':
        if (node.children[0]) this.render(node.children[0], level);
        return;
      case 'LITERAL':
      case 'IDENTIFIER_EXPRESSION':
        this.token(node.leaves[0], level);
        return;
      case 'UNARY_EXPRESSION': {
        const op = node.leaves[0];
        this.token(op, level);
        if (op.type === 'KEYWORD_LOGICAL') this.space();
        this.render(node.children[0], level);
        return;
      }
      case 'BINARY_EXPRESSION':
        this.render(node.children[0], level);
        this.space();
        this.token(node.leaves[0], level);
        this.space();
        this.render(node.children[1], level);
        return;
      case 'PAREN_EXPRESSION':
        this.token(node.leaves[0], level);
        this.space();
        if (node.children[0]) this.render(node.children[0], level);
        this.space();
        this.token(node.leaves[1], level);
        return;
      case 'REPETITION_EXPRESSION':
        this.render(node.children[0], level);
        for (const list of node.children.slice(1)) this.renderCompactList(list, level);
        return;
      case 'BRACKET_LIST':
        this.renderList(node, level);
        return;
      case 'FUNCTION_CALL':
        this.renderCall(node, level);
        return;
      default:
        throw new Error(`Unexpected node ${node.type}`);
    }
  }

  /** Field[2]: no spaces */
  private renderCompactList(list: CalcNode, level: number): void {
    const [open, ...rest] = list.leaves;
    const close = rest.find((t) => t.type === 'RBRACKET')!;
    const seps = this.separators(list);
    this.token(open, level);
    list.children.forEach((item, i) => {
      this.render(item, level);
      if (seps[i]) {
        this.space();
        this.token(seps[i], level);
        this.space();
      }
    });
    this.token(close, level);
  }

  /** [ a ; b ] on one line if it fits, else one item per line. */
  private renderList(list: CalcNode, level: number, itemLevel = level + 1): void {
    const open = list.leaves[0];
    const close = list.leaves[list.leaves.length - 1];
    const seps = this.separators(list);
    this.token(open, level);
    if (list.children.length === 0) {
      this.token(close, level);
      return;
    }
    if (this.fits(list)) {
      list.children.forEach((item, i) => {
        this.space();
        this.render(item, level);
        if (seps[i]) {
          this.space();
          this.token(seps[i], level);
        }
      });
      this.space();
      this.token(close, level);
      return;
    }
    list.children.forEach((item, i) => {
      this.newline(itemLevel);
      this.render(item, itemLevel);
      if (seps[i]) {
        this.space();
        this.token(seps[i], itemLevel);
      }
    });
    this.newline(level);
    this.token(close, level);
  }

  private renderCall(call: CalcNode, level: number): void {
    const [nameToken, open] = call.leaves;
    const close = call.leaves.find((t, i) => i > 1 && t.type === 'RPAREN') ?? call.leaves[2];
    const args = this.args(call);
    const seps = this.separators(call.children.find((c) => c.type === 'ARG_LIST'));
    const name = this.callName(call).toLowerCase();

    this.token(nameToken, level);
    this.space();
    this.token(open, level);
    if (args.length === 0 || (args.length === 1 && !args[0].children[0])) {
      this.token(close, level);
      return;
    }

    if (name === 'let' && this.letBindings(call).length >= 2 && args.length === 2) {
      this.renderLet(call, args, seps, close, level);
    } else if (name === 'case' && args.length >= 2 && (args.length >= 4 || !this.fits(call))) {
      this.renderCase(args, seps, close, level);
    } else if (name !== 'while' && this.fits(call)) {
      args.forEach((arg, i) => {
        this.space();
        this.render(arg, level);
        if (seps[i]) {
          this.space();
          this.token(seps[i], level);
        }
      });
      this.space();
      this.token(close, level);
    } else {
      args.forEach((arg, i) => {
        this.newline(level + 1);
        this.render(arg, level + 1);
        if (seps[i]) {
          this.space();
          this.token(seps[i], level + 1);
        }
      });
      this.newline(level);
      this.token(close, level);
    }
  }

  /**
   * Let ( [
   *     a = 1 ;
   *     b = 2
   * ] ;
   *     calculation
   * )
   */
  private renderLet(call: CalcNode, args: CalcNode[], seps: Token[], close: Token, level: number): void {
    const list = args[0].children[0];
    const isTop = call === this.root;
    const itemLevel = isTop && this.options.doNotIndentTopLetVariables ? level : level + 1;
    this.space();
    const listOpen = list.leaves[0];
    const listClose = list.leaves[list.leaves.length - 1];
    const listSeps = this.separators(list);
    this.token(listOpen, level);
    list.children.forEach((item, i) => {
      this.newline(itemLevel);
      this.render(item, itemLevel);
      if (listSeps[i]) {
        this.space();
        this.token(listSeps[i], itemLevel);
      }
    });
    this.newline(level);
    this.token(listClose, level);
    this.space();
    this.token(seps[0], level);
    this.newline(level + 1);
    this.render(args[1], level + 1);
    this.newline(level);
    this.token(close, level);
  }

  /**
   * Case (
   *     test1 ; result1 ;
   *     test2 ; result2 ;
   *     default
   * )
   */
  private renderCase(args: CalcNode[], seps: Token[], close: Token, level: number): void {
    for (let i = 0; i < args.length; i++) {
      if (i % 2 === 0) this.newline(level + 1);
      else this.space();
      this.render(args[i], level + 1);
      if (seps[i]) {
        this.space();
        this.token(seps[i], level + 1);
      }
    }
    this.newline(level);
    this.token(close, level);
  }
}

function push(map: Map<number, Token[]>, key: number, t: Token): void {
  const list = map.get(key);
  if (list) list.push(t);
  else map.set(key, [t]);
}
