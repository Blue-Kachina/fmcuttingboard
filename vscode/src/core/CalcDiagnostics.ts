// Port of jetbrains/.../language/validation/FileMakerCalculationAnnotator.java and FunctionVariableScopes.java.
// Same checks, messages, ranges and order; pinned by shared/fixtures/golden/cases.json "diagnostics".
import { parse, findAll, type CalcNode, type ParsedCalc } from './CalcParser';
import { findByName, isComplete } from './FileMakerFunctionRegistry';

export type Severity = 'error' | 'warning' | 'weak_warning';

export interface CalcDiagnostic {
  severity: Severity;
  message: string;
  start: number;
  end: number;
}

export function diagnose(text: string): CalcDiagnostic[] {
  const parsed = parse(text);
  const out: CalcDiagnostic[] = [];
  // Like the annotator, the first lexical error is the only thing reported
  const lexical = lexicalError(parsed);
  if (lexical) return [lexical];
  validateFunctions(parsed, out);
  validateFunctionVariableScopes(parsed, out);
  return out;
}

function lexicalError({ text, tokens }: ParsedCalc): CalcDiagnostic | undefined {
  let round = 0;
  let square = 0;
  let curly = 0;
  const at = (start: number, message: string): CalcDiagnostic => ({ severity: 'error', message, start, end: start + 1 });
  for (const t of tokens) {
    if (t.type === 'LPAREN') round++;
    else if (t.type === 'LBRACKET') square++;
    else if (t.type === 'LBRACE') curly++;
    else if (t.type === 'RPAREN' && --round < 0) return at(t.start, 'Unmatched closing )');
    else if (t.type === 'RBRACKET' && --square < 0) return at(t.start, 'Unmatched closing ]');
    else if (t.type === 'RBRACE' && --curly < 0) return at(t.start, 'Unmatched closing }');
    else if (t.type === 'STRING' && !isTerminatedString(text, t.start, t.end)) {
      return at(t.start, 'Unterminated text constant (missing closing quotation mark)');
    } else if (t.type === 'BAD_CHARACTER' && text.charCodeAt(t.start) < 32) {
      return at(t.start, 'Invalid control character');
    }
  }
  return undefined;
}

/** A string token ends with a closing quote that is not escaped by an odd run of backslashes. */
export function isTerminatedString(text: string, start: number, end: number): boolean {
  if (end - start < 2 || text[end - 1] !== '"') return false;
  let backslashes = 0;
  for (let i = end - 2; i > start && text[i] === '\\'; i--) backslashes++;
  return backslashes % 2 === 0;
}

function functionName(parsed: ParsedCalc, call: CalcNode): string | undefined {
  const first = call.leaves[0];
  return first && first.start === call.start ? parsed.text.substring(first.start, first.end) : undefined;
}

function argumentsOf(call: CalcNode): CalcNode[] {
  const list = call.children.find((c) => c.type === 'ARG_LIST');
  return list ? list.children.filter((c) => c.type === 'ARGUMENT') : [];
}

function validateFunctions(parsed: ParsedCalc, out: CalcDiagnostic[]): void {
  for (const call of findAll(parsed.root, 'FUNCTION_CALL')) {
    const name = functionName(parsed, call);
    if (!name) continue;
    const meta = findByName(name);
    if (!meta) {
      // Only claim "unknown" when the function list is complete
      if (isComplete()) out.push({ severity: 'weak_warning', message: `Unknown function '${name}'`, start: call.start, end: call.end });
      continue;
    }
    const argCount = argumentsOf(call).length;
    let min = 0;
    let max = 0;
    let hasRepeating = false;
    for (const p of meta.parameters) {
      if (!p.optional && !p.repeating) min++;
      if (p.repeating) hasRepeating = true;
      else max++;
    }
    if (hasRepeating) max = Number.MAX_SAFE_INTEGER;
    if (argCount < min) {
      out.push({ severity: 'error', message: `Too few arguments for ${meta.name}: expected at least ${min}, got ${argCount}`, start: call.start, end: call.end });
    } else if (argCount > max) {
      const expected = hasRepeating ? `${min}+` : String(max);
      out.push({ severity: 'error', message: `Too many arguments for ${meta.name}: expected ${expected}, got ${argCount}`, start: call.start, end: call.end });
    }
  }
}

// ----- FunctionVariableScopes -----

interface Definition {
  name: string;
  fn: 'Let' | 'While';
  from: number;
  to: number; // inclusive, like TextRange.containsOffset
}

interface Binding {
  name: string;
  declaration: CalcNode;
  end: number;
}

function bindings(parsed: ParsedCalc, argument: CalcNode): Binding[] {
  const expr = argument.children[0];
  if (!expr) return [];
  const items = expr.type === 'BRACKET_LIST' ? expr.children : [expr];
  const out: Binding[] = [];
  for (const item of items) {
    if (item.type !== 'BINARY_EXPRESSION') continue;
    const left = item.children[0];
    const op = item.leaves.find((t) => t.type === 'OPERATOR');
    if (left?.type === 'IDENTIFIER_EXPRESSION' && op && parsed.text.substring(op.start, op.end) === '=') {
      out.push({ name: parsed.text.substring(left.start, left.end).toLowerCase(), declaration: left, end: item.end });
    }
  }
  return out;
}

function validateFunctionVariableScopes(parsed: ParsedCalc, out: CalcDiagnostic[]): void {
  const definitions: Definition[] = [];
  const declarations = new Set<CalcNode>();

  for (const call of findAll(parsed.root, 'FUNCTION_CALL')) {
    const fn = functionName(parsed, call)?.toLowerCase();
    const args = argumentsOf(call);
    if (args.length === 0) continue;
    if (fn === 'let') {
      for (const b of bindings(parsed, args[0])) {
        definitions.push({ name: b.name, fn: 'Let', from: b.end, to: call.end });
        declarations.add(b.declaration);
      }
    } else if (fn === 'while') {
      for (const b of bindings(parsed, args[0])) {
        definitions.push({ name: b.name, fn: 'While', from: b.end, to: call.end });
        declarations.add(b.declaration);
      }
      if (args.length > 2) {
        const conditionStart = args[1].start;
        for (const b of bindings(parsed, args[2])) {
          definitions.push({ name: b.name, fn: 'While', from: conditionStart, to: call.end });
          declarations.add(b.declaration);
        }
      }
    }
  }
  if (definitions.length === 0) return;

  for (const id of findAll(parsed.root, 'IDENTIFIER_EXPRESSION')) {
    if (declarations.has(id)) continue;
    const name = parsed.text.substring(id.start, id.end);
    if (name.length === 0 || name.startsWith('$')) continue;
    const key = name.toLowerCase();
    let first: Definition | undefined;
    let visible = false;
    for (const d of definitions) {
      if (d.name !== key) continue;
      first ??= d;
      if (id.start >= d.from && id.start <= d.to) {
        visible = true;
        break;
      }
    }
    if (first && !visible) {
      out.push({
        severity: 'weak_warning',
        message: `'${name}' is used outside the ${first.fn}() that defines it (here it refers to a field)`,
        start: id.start,
        end: id.end,
      });
    }
  }
}
