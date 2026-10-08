// Runs shared/fixtures/golden/cases.json "formatting" against the TypeScript formatter. The JetBrains plugin runs the
// same cases (pure formatter and IDE Reformat Code), so both IDEs format calculations identically.
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { foldRegions } from '../src/core/CalcFolding';
import { format, type FormatOptions } from '../src/core/CalcFormatter';

const cases = JSON.parse(
  readFileSync(join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'shared', 'fixtures', 'golden', 'cases.json'), 'utf8'),
);

describe('golden formatting cases', () => {
  for (const c of cases.formatting) {
    const options: FormatOptions = { ...cases.formattingDefaults, ...(c.options ?? {}) };
    it(c.id, () => {
      expect(format(c.input, options)).toBe(c.expect);
      if (c.expect !== null) expect(format(c.expect, options), 'idempotent').toBe(c.expect);
    });
  }
});

describe('golden folding cases', () => {
  const describeRegion = (r: { start: number; end: number; placeholder: string }) => `${r.start}-${r.end} ${r.placeholder}`;
  for (const c of cases.folding) {
    it(c.id, () => {
      expect(foldRegions(c.calc).map(describeRegion).sort()).toEqual(c.expect.map(describeRegion).sort());
    });
  }
});
