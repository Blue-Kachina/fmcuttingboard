#!/usr/bin/env node
// Regenerates shared/fixtures/golden/function-signatures.txt from shared/data/fm-calc-catalogue.json.
//
// One line per function, in catalogue order: name | category | returnType | signature | simple signature | summary.
// Both plugins' registry tests (FunctionRegistryBaselineTest.java, vscode/test/functionRegistry.test.ts) must
// produce exactly these lines. Like generate-golden.mjs, this shares no code with either plugin, so the
// baseline is not just a plugin's own output played back.
//
// Usage (from the repo root), after vendoring a new catalogue:
//   node shared/tools/generate-function-signatures.mjs           write the baseline; review its diff
//   node shared/tools/generate-function-signatures.mjs --check   fail if the baseline is stale (used in CI)
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const sharedDir = join(dirname(fileURLToPath(import.meta.url)), '..');
const catalogue = JSON.parse(readFileSync(join(sharedDir, 'data', 'fm-calc-catalogue.json'), 'utf8'));
const target = join(sharedDir, 'fixtures', 'golden', 'function-signatures.txt');

// Display names for the catalogue's data types (the plugins keep the same table)
const TYPE_LABELS = {
  text: 'Text', number: 'Number', date: 'Date', time: 'Time', timestamp: 'Timestamp', container: 'Container',
  boolean: 'Boolean', json: 'JSON', any: 'Any', expression: 'Expression', fieldReference: 'Field',
  variableBindings: 'Bindings',
};
const typeLabel = (t) => TYPE_LABELS[t] ?? t;
// "Text functions" -> "Text"
const categories = new Map(catalogue.categories.map((c) => [c.key, c.label.replace(/ functions$/i, '')]));

// [optional], repeating..., [optional repeating...]
const display = (p) => `${p.optional ? '[' : ''}${p.name}${p.repeatable ? '...' : ''}${p.optional ? ']' : ''}`;

const lines = catalogue.functions.map((f) =>
  [
    f.name,
    categories.get(f.category) ?? f.category,
    typeLabel(f.returnType),
    `${f.name}(${f.parameters.map(display).join('; ')})`,
    `${f.name}(${f.parameters.map((p) => p.name).join('; ')})`,
    f.summary ?? '',
  ].join(' | '),
);
const text = lines.join('\n') + '\n';

if (process.argv.includes('--check')) {
  let current = '';
  try {
    current = readFileSync(target, 'utf8');
  } catch {}
  if (current !== text) {
    console.error('shared/fixtures/golden/function-signatures.txt is stale. Run: node shared/tools/generate-function-signatures.mjs');
    process.exit(1);
  }
  console.log('Function signature baseline is up to date.');
} else {
  writeFileSync(target, text);
  console.log(`Wrote ${lines.length} functions to ${relative(process.cwd(), target)}`);
}
