// Tokenizes FileMaker calculations with the generated grammar, using VS Code's own TextMate engine.
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { beforeAll, describe, expect, it } from 'vitest';
import * as oniguruma from 'vscode-oniguruma';
import { INITIAL, Registry, parseRawGrammar, type IGrammar } from 'vscode-textmate';

const here = dirname(fileURLToPath(import.meta.url));
const grammarPath = join(here, '..', 'syntaxes', 'fmcalc.tmLanguage.json');
let grammar: IGrammar;

beforeAll(async () => {
  const wasm = readFileSync(createRequire(import.meta.url).resolve('vscode-oniguruma/release/onig.wasm'));
  await oniguruma.loadWASM(wasm.buffer.slice(wasm.byteOffset, wasm.byteOffset + wasm.byteLength));
  const registry = new Registry({
    onigLib: Promise.resolve({
      createOnigScanner: (patterns) => new oniguruma.OnigScanner(patterns),
      createOnigString: (s) => new oniguruma.OnigString(s),
    }),
    loadGrammar: async () => parseRawGrammar(readFileSync(grammarPath, 'utf8'), grammarPath),
  });
  grammar = (await registry.loadGrammar('source.fmcalc'))!;
});

/**
 * Tokens of one or more lines as [text, innermost scope] pairs: adjacent tokens with the same scope are merged,
 * text is trimmed, and whitespace-only tokens are dropped.
 */
function tokens(source: string): [string, string][] {
  const out: [string, string][] = [];
  let state = INITIAL;
  for (const line of source.split('\n')) {
    const r = grammar.tokenizeLine(line, state);
    let last: [string, string] | undefined;
    for (const t of r.tokens) {
      const text = line.substring(t.startIndex, t.endIndex);
      const scope = t.scopes[t.scopes.length - 1];
      if (last && last[1] === scope) last[0] += text;
      else out.push((last = [text, scope]));
    }
    state = r.ruleStack;
  }
  return out.map(([t, s]) => [t.trim(), s] as [string, string]).filter(([t]) => t.length > 0);
}

/** The innermost scope of the first token whose text is exactly `text`. */
function scopeOf(source: string, text: string): string | undefined {
  return tokens(source).find(([t]) => t === text)?.[1];
}

describe('fmcalc grammar', () => {
  it('scopes built-in functions case-insensitively, and other calls as functions', () => {
    expect(scopeOf('Substitute ( x ; "a" ; "b" )', 'Substitute')).toBe('support.function.builtin.fmcalc');
    expect(scopeOf('substitute( x ; "a" ; "b" )', 'substitute')).toBe('support.function.builtin.fmcalc');
    expect(scopeOf('MyCustomFunction ( 1 )', 'MyCustomFunction')).toBe('entity.name.function.fmcalc');
  });

  it('scopes Get ( Constant ) for any constant name', () => {
    const src = 'Get ( AccountName ) & Get(SomeFutureConstant)';
    expect(scopeOf(src, 'Get')).toBe('support.function.builtin.fmcalc');
    expect(scopeOf(src, 'AccountName')).toBe('support.constant.get.fmcalc');
    expect(scopeOf(src, 'SomeFutureConstant')).toBe('support.constant.get.fmcalc');
  });

  it('scopes strings with escapes and the paragraph mark', () => {
    expect(tokens('"say \\"hi\\"¶"')).toEqual([
      ['"say', 'string.quoted.double.fmcalc'],
      ['\\"', 'constant.character.escape.fmcalc'],
      ['hi', 'string.quoted.double.fmcalc'],
      ['\\"', 'constant.character.escape.fmcalc'],
      ['¶', 'constant.character.paragraph.fmcalc'],
      ['"', 'string.quoted.double.fmcalc'],
    ]);
    expect(scopeOf('a & ¶ & b', '¶')).toBe('constant.character.paragraph.fmcalc');
  });

  it('does not highlight keywords or functions inside strings and comments', () => {
    expect(tokens('"If ( and )"')).toEqual([['"If ( and )"', 'string.quoted.double.fmcalc']]);
    expect(tokens('// If ( x )')).toEqual([['// If ( x )', 'comment.line.double-slash.fmcalc']]);
    const block = tokens('/* Let (\n  x ) */ 1');
    expect(block.slice(0, 2).map(([, s]) => s)).toEqual(['comment.block.fmcalc', 'comment.block.fmcalc']);
    expect(block[2]).toEqual(['1', 'constant.numeric.fmcalc']);
  });

  it('scopes field references, variables and ${ } quoted names', () => {
    const src = 'Contacts::FirstName & $local & $$global & ${Table::If}';
    expect(scopeOf(src, 'Contacts')).toBe('entity.name.type.table.fmcalc');
    expect(scopeOf(src, '::')).toBe('punctuation.accessor.fmcalc');
    expect(scopeOf(src, 'FirstName')).toBe('variable.other.field.fmcalc');
    expect(scopeOf(src, '$local')).toBe('variable.other.local.fmcalc');
    expect(scopeOf(src, '$$global')).toBe('variable.other.global.fmcalc');
    expect(scopeOf(src, '${Table::If}')).toBe('variable.other.quoted.fmcalc');
  });

  it('scopes constants, word operators, symbol operators and numbers', () => {
    const src = 'JSONSetElement ( "{}" ; "k" ; True ; JSONBoolean ) and not x ≠ 1.5e3 or y <> -2';
    expect(scopeOf(src, 'True')).toBe('constant.language.fmcalc');
    expect(scopeOf(src, 'JSONBoolean')).toBe('constant.language.fmcalc');
    expect(scopeOf(src, 'and')).toBe('keyword.operator.logical.fmcalc');
    expect(scopeOf(src, 'not')).toBe('keyword.operator.logical.fmcalc');
    expect(scopeOf(src, '≠')).toBe('keyword.operator.comparison.fmcalc');
    expect(scopeOf(src, '<>')).toBe('keyword.operator.comparison.fmcalc');
    expect(scopeOf(src, '1.5e3')).toBe('constant.numeric.fmcalc');
    expect(scopeOf(src, ';')).toBe('punctuation.separator.arguments.fmcalc');
  });

  it('accepts Unicode names, like the JetBrains lexer', () => {
    const src = '顧客::名前 & $変数 & 自作関数 ( 1 )';
    expect(scopeOf(src, '顧客')).toBe('entity.name.type.table.fmcalc');
    expect(scopeOf(src, '名前')).toBe('variable.other.field.fmcalc');
    expect(scopeOf(src, '$変数')).toBe('variable.other.local.fmcalc');
    expect(scopeOf(src, '自作関数')).toBe('entity.name.function.fmcalc');
  });

  it('does not treat identifiers that merely contain a keyword as keywords', () => {
    expect(scopeOf('android + Trueness', 'android')).toBe('source.fmcalc');
    expect(scopeOf('android + Trueness', 'Trueness')).toBe('source.fmcalc');
    expect(tokens('android + Trueness').map(([t]) => t)).toEqual(['android', '+', 'Trueness']);
  });
});
