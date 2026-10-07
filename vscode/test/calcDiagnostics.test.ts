// Runs shared/fixtures/golden/cases.json "lexer" and "diagnostics" against the TypeScript port. The JetBrains plugin
// runs the same cases (SharedGoldenFixturesTest, SharedDiagnosticsGoldenTest), so both IDEs tokenize and report
// problems identically.
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { diagnose } from '../src/core/CalcDiagnostics';
import { tokenize } from '../src/core/CalcLexer';

const cases = JSON.parse(
  readFileSync(join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'shared', 'fixtures', 'golden', 'cases.json'), 'utf8'),
);

/** Non-whitespace tokens as TYPE:text, consecutive BLOCK_COMMENT tokens merged. */
function lexerTokens(calc: string): string[] {
  const out: string[] = [];
  let lastType = '';
  for (const t of tokenize(calc)) {
    if (t.type === 'WHITE_SPACE') continue;
    const text = calc.substring(t.start, t.end);
    if (t.type === 'BLOCK_COMMENT' && lastType === 'BLOCK_COMMENT') out[out.length - 1] += text;
    else out.push(`${t.type}:${text}`);
    lastType = t.type;
  }
  return out;
}

const describeDiagnostic = (d: { severity: string; start: number; end: number; message: string }) =>
  `${d.severity} ${d.start}-${d.end} ${d.message}`;

describe('golden lexer cases', () => {
  for (const c of cases.lexer) {
    it(c.id, () => expect(lexerTokens(c.calc)).toEqual(c.tokens));
  }
});

describe('golden diagnostics cases', () => {
  for (const c of cases.diagnostics) {
    it(c.id, () => {
      expect(diagnose(c.calc).map(describeDiagnostic).sort()).toEqual(c.expect.map(describeDiagnostic).sort());
    });
  }
});
