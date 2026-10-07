// Mirrors jetbrains/.../language/FunctionRegistryBaselineTest.java: the displayed signatures must match the
// shared baseline (shared/fixtures/golden/function-signatures.txt), which the JetBrains plugin also checks.
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { findByName, getAll } from '../src/core/FileMakerFunctionRegistry';

const baselinePath = join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'shared', 'fixtures', 'golden', 'function-signatures.txt');

describe('FileMakerFunctionRegistry', () => {
  it('matches the shared signature baseline', () => {
    const expected = readFileSync(baselinePath, 'utf8').split('\n').filter((line) => line.length > 0);
    const actual = getAll().map(
      (m) => `${m.name} | ${m.category} | ${m.returnType} | ${m.getSignature()} | ${m.getSimpleSignature()} | ${m.description}`,
    );
    expect(actual).toEqual(expected);
  });

  it('looks names up case-insensitively', () => {
    expect(findByName('substitute')).toBeDefined();
    expect(findByName('GETVALUE')).toBeDefined();
    expect(findByName('NoSuchFunction')).toBeUndefined();
  });
});
