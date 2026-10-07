#!/usr/bin/env node
// Regenerates shared/fixtures/golden/snippets.generated.json from shared/fixtures/snippets/**.
//
// This is an independent reference implementation of the encoding rules in
// shared/data/clipboard-formats.json, deliberately sharing no code with either plugin, so the
// expectations it writes are not just a plugin's own output played back.
// The expected snippet type comes from each fixture's folder name, not from the detection rules.
//
// Usage (from the repo root):  node shared/tools/generate-golden.mjs
// Then review the diff of the generated file before committing it.

import { createHash } from 'node:crypto';
import { readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const sharedDir = join(dirname(fileURLToPath(import.meta.url)), '..');
const fixturesDir = join(sharedDir, 'fixtures');
const snippetsDir = join(fixturesDir, 'snippets');
const formats = JSON.parse(readFileSync(join(sharedDir, 'data', 'clipboard-formats.json'), 'utf8'));

// Folder name → expected snippet type id
const FOLDER_TYPES = {
  'script': 'SCRIPT',
  'script-steps': 'SCRIPT_STEPS',
  'fields': 'FIELD_DEFINITION',
  'tables': 'TABLE_DEFINITION',
  'layout-objects': 'LAYOUT_OBJECTS',
  'custom-functions': 'CUSTOM_FUNCTION',
  'value-lists': 'VALUE_LIST',
};

const sha256 = (buf) => createHash('sha256').update(buf).digest('hex');
const hex = (buf) => buf.toString('hex').toUpperCase();

// Spec: windows.customFormatPayload — LF newlines, uint32 LE length prefix, UTF-8 without BOM, no terminator
function customFormatPayload(text) {
  const utf8 = Buffer.from(text.replace(/\r\n/g, '\n').replace(/\r/g, '\n'), 'utf8');
  const prefix = Buffer.alloc(4);
  prefix.writeUInt32LE(utf8.length, 0);
  return Buffer.concat([prefix, utf8]);
}

// Spec: windows.textFormat (CF_UNICODETEXT) — UTF-16LE without BOM plus a two-byte NUL terminator
function unicodeText(text) {
  return Buffer.concat([Buffer.from(text, 'utf16le'), Buffer.alloc(2)]);
}

function describe(buf) {
  return { length: buf.length, sha256: sha256(buf), headHex: hex(buf.subarray(0, 16)) };
}

const cases = [];
for (const folder of readdirSync(snippetsDir).sort()) {
  const type = FOLDER_TYPES[folder];
  if (!type) throw new Error(`Unknown fixture folder '${folder}'; add it to FOLDER_TYPES`);
  const rule = formats.snippetTypes.find((t) => t.id === type);
  for (const file of readdirSync(join(snippetsDir, folder)).filter((f) => f.endsWith('.xml')).sort()) {
    const path = join(snippetsDir, folder, file);
    const text = readFileSync(path, 'utf8');
    if (text.includes('\r')) throw new Error(`${path} contains CR characters; fixtures must use LF line endings`);
    const crlf = text.replace(/\n/g, '\r\n');
    cases.push({
      id: `${folder}/${file.replace(/\.xml$/, '')}`,
      fixture: relative(fixturesDir, path).split('\\').join('/'),
      expect: {
        snippetType: type,
        windowsFormats: [rule.windows.format, ...rule.windows.aliases],
        customFormatPayload: describe(customFormatPayload(text)),
        unicodeText: { lf: describe(unicodeText(text)), crlf: describe(unicodeText(crlf)) },
      },
    });
  }
}

const out = {
  $schema: '../../schemas/golden-snippets.schema.json',
  generatedBy: 'shared/tools/generate-golden.mjs (do not edit by hand)',
  cases,
};
const target = join(fixturesDir, 'golden', 'snippets.generated.json');
writeFileSync(target, JSON.stringify(out, null, 2) + '\n');
console.log(`Wrote ${cases.length} cases to ${relative(process.cwd(), target)}`);
