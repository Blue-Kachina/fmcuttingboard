#!/usr/bin/env node
// Vendors the calculation catalogue exported by the sister repo fmscriptinventory into
// shared/data/fm-calc-catalogue.json (see docs/fm-calc-catalogue-contract.md).
//
// Usage (from the repo root):
//   node shared/tools/sync-calc-catalogue.mjs <path-to-export.json>
//   e.g. node shared/tools/sync-calc-catalogue.mjs ../../FMDev/fmscriptinventory/export/fm-calc-catalogue.json
//
// It validates the export against shared/schemas/fm-calc-catalogue.schema.json (via ajv-cli, like CI), checks the
// licensing rule (no copied help text), then writes a stably formatted copy. Review the diff before committing.
import { spawnSync } from 'node:child_process';
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const sharedDir = join(dirname(fileURLToPath(import.meta.url)), '..');
const repoRoot = join(sharedDir, '..');
const schema = join(sharedDir, 'schemas', 'fm-calc-catalogue.schema.json');
const target = join(sharedDir, 'data', 'fm-calc-catalogue.json');

const source = process.argv[2];
if (!source) {
  console.error('Usage: node shared/tools/sync-calc-catalogue.mjs <path-to-fm-calc-catalogue export>');
  process.exit(2);
}

// One command string (npx is a .cmd shim on Windows, so it needs a shell); paths are quoted
const ajv = spawnSync(
  `npx --yes ajv-cli@5.0.0 validate --spec=draft7 --errors=text -s "${schema}" -d "${resolve(source)}"`,
  { stdio: 'inherit', shell: true },
);
if (ajv.status !== 0) {
  console.error('Export does not match shared/schemas/fm-calc-catalogue.schema.json; nothing was copied.');
  process.exit(1);
}

const catalogue = JSON.parse(readFileSync(source, 'utf8'));
if (catalogue.generator.dirty) {
  console.error('Refusing to vendor an export made from uncommitted changes (generator.dirty = true).');
  process.exit(1);
}

// Licensing guard: summaries must be our own short words, never pasted help text
const long = [...catalogue.functions, ...catalogue.getConstants].filter((x) => (x.summary ?? '').length > 300);
if (long.length > 0) {
  console.error(`Summaries over 300 characters (copied help text?): ${long.map((x) => x.name).join(', ')}`);
  process.exit(1);
}

writeFileSync(target, JSON.stringify(catalogue, null, 2) + '\n');
console.log(
  `Vendored ${catalogue.functions.length} functions, ${catalogue.getConstants.length} Get() constants, ` +
    `${catalogue.constants.length} constants, ${catalogue.operators.length} operators ` +
    `(FileMaker ${catalogue.documentedFileMakerVersion}, ${catalogue.generator.repo}@${catalogue.generator.commit}) ` +
    `to ${relative(repoRoot, target)}`,
);
