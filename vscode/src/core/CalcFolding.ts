// Folding regions for .fmcalc, shared with the JetBrains FileMakerCalculationFoldingBuilder and pinned by
// shared/fixtures/golden/cases.json "folding":
// - the inside of multi-line Let / Case / If / While calls (any case, any spacing): "…" between the parentheses
// - the inside of multi-line [ … ] lists (e.g. Let variables)
// - multi-line block comments: "/*…*/"
import { findAll, parse } from './CalcParser';

export interface FoldRegion {
  start: number;
  end: number;
  placeholder: string;
  kind: 'region' | 'comment';
}

const FOLDING_CALLS = new Set(['let', 'case', 'if', 'while']);

export function foldRegions(text: string): FoldRegion[] {
  const parsed = parse(text);
  const out: FoldRegion[] = [];
  const multiLine = (start: number, end: number) => text.substring(start, end).includes('\n');

  for (const call of findAll(parsed.root, 'FUNCTION_CALL')) {
    const [name, open] = call.leaves;
    const close = call.leaves.find((t, i) => i > 1 && t.type === 'RPAREN');
    if (!open || open.type !== 'LPAREN' || !close) continue;
    if (!FOLDING_CALLS.has(text.substring(name.start, name.end).toLowerCase())) continue;
    if (multiLine(open.end, close.start)) out.push({ start: open.end, end: close.start, placeholder: '…', kind: 'region' });
  }
  for (const list of findAll(parsed.root, 'BRACKET_LIST')) {
    const open = list.leaves[0];
    const close = list.leaves[list.leaves.length - 1];
    if (open?.type !== 'LBRACKET' || close?.type !== 'RBRACKET' || open === close) continue;
    if (multiLine(open.end, close.start)) out.push({ start: open.end, end: close.start, placeholder: '…', kind: 'region' });
  }
  for (const t of parsed.tokens) {
    if (t.type === 'BLOCK_COMMENT' && multiLine(t.start, t.end)) {
      out.push({ start: t.start, end: t.end, placeholder: '/*…*/', kind: 'comment' });
    }
  }
  return out.sort((a, b) => a.start - b.start || b.end - a.end);
}
