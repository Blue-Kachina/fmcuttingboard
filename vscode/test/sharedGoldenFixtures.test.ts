// Runs the shared golden fixtures (repo-root shared/fixtures/). Mirrors
// jetbrains/src/test/java/dev/fmcuttingboard/SharedGoldenFixturesTest.java; shared/fixtures/README.md defines
// what each section means. Both plugins must pass every case.
import { createHash } from 'node:crypto';
import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { detectSnippetType, isFileMakerWindowsFormat, windowsFormatNames } from '../src/core/ClipboardFormats';
import { ConversionException } from '../src/core/ConversionException';
import { DefaultFileMakerClipboardParser } from '../src/core/DefaultFileMakerClipboardParser';
import { DefaultXmlToClipboardConverter } from '../src/core/DefaultXmlToClipboardConverter';
import * as Codec from '../src/core/FmClipboardCodec';
import { FmSnippet } from '../src/core/FmSnippet';
import { FmXmlParser } from '../src/core/FmXmlParser';
import { resolveFileName, uniqueFileName } from '../src/core/ProjectFiles';

const fixturesRoot = join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'shared', 'fixtures');
const readJson = (rel: string) => JSON.parse(readFileSync(join(fixturesRoot, rel), 'utf8'));
const golden = readJson('golden/snippets.generated.json');
const cases = readJson('golden/cases.json');

interface BytesExpectation {
  length: number;
  sha256: string;
  headHex: string;
}

const sha256 = (b: Uint8Array) => createHash('sha256').update(b).digest('hex');
const headHex = (b: Uint8Array) => Buffer.from(b.subarray(0, 16)).toString('hex').toUpperCase();
const crlf = (lf: string) => lf.split('\n').join('\r\n');
const fromHex = (hex: string) => new Uint8Array(Buffer.from(hex, 'hex'));

function expectBytes(expected: BytesExpectation, actual: Uint8Array, what: string) {
  expect(actual.length, `${what} length`).toBe(expected.length);
  expect(headHex(actual), `${what} first bytes`).toBe(expected.headHex);
  expect(sha256(actual), `${what} sha256`).toBe(expected.sha256);
}

/** Text from the first `<fmxmlsnippet` through the end of the last `</fmxmlsnippet>`. */
function snippetOf(text: string): string {
  const start = text.indexOf('<fmxmlsnippet');
  const end = text.lastIndexOf('</fmxmlsnippet>') + '</fmxmlsnippet>'.length;
  return text.substring(start, end);
}

const concat = (...parts: Uint8Array[]) => new Uint8Array(Buffer.concat(parts));
const utf8 = (s: string) => new Uint8Array(Buffer.from(s, 'utf8'));
const utf16le = (s: string) => new Uint8Array(Buffer.from(s, 'utf16le'));
const utf16be = (s: string) => {
  const le = Buffer.from(s, 'utf16le');
  return new Uint8Array(le.swap16());
};

function buildVariant(variant: string, text: string): Uint8Array {
  switch (variant) {
    case 'utf8': return utf8(text);
    case 'utf8-bom': return concat(new Uint8Array([0xef, 0xbb, 0xbf]), utf8(text));
    case 'utf16le': return utf16le(text);
    case 'utf16le-bom': return concat(new Uint8Array([0xff, 0xfe]), utf16le(text));
    case 'utf16le-nul-terminated': return concat(utf16le(text), new Uint8Array(2));
    case 'utf16be': return utf16be(text);
    case 'utf16be-bom': return concat(new Uint8Array([0xfe, 0xff]), utf16be(text));
    case 'custom-format-payload': return Codec.encodeCustomFormatPayload(text);
    default: throw new Error(`Unknown byte variant in cases.json: ${variant}`);
  }
}

function inputBytes(c: { hex?: string; text?: string; encoding?: string }): Uint8Array {
  if (c.hex !== undefined) return fromHex(c.hex);
  switch (c.encoding) {
    case 'utf8': return utf8(c.text!);
    case 'utf16le': return utf16le(c.text!);
    case 'utf16be': return utf16be(c.text!);
    default: throw new Error(`Unknown encoding in cases.json: ${c.encoding}`);
  }
}

describe('snippet fixtures', () => {
  for (const c of golden.cases) {
    const text = readFileSync(join(fixturesRoot, c.fixture), 'utf8');
    const e = c.expect;

    describe(c.id, () => {
      it('detection and format names', () => {
        const type = detectSnippetType(text);
        expect(type).toBe(e.snippetType);
        expect(windowsFormatNames(type)).toEqual(e.windowsFormats);
      });

      it('custom format payload (LF and CRLF input)', () => {
        expectBytes(e.customFormatPayload, Codec.encodeCustomFormatPayload(text), 'LF');
        expectBytes(e.customFormatPayload, Codec.encodeCustomFormatPayload(crlf(text)), 'CRLF');
      });

      it('CF_UNICODETEXT bytes', () => {
        expectBytes(e.unicodeText.lf, Codec.utf16leNullTerminated(text), 'LF');
        expectBytes(e.unicodeText.crlf, Codec.utf16leNullTerminated(crlf(text)), 'CRLF');
      });

      for (const v of cases.byteVariants.variants) {
        for (const endings of ['lf', 'crlf'] as const) {
          it(`${v.id} (${endings})`, () => {
            const input = endings === 'lf' ? text : crlf(text);
            const bytes = buildVariant(v.id, input);
            if (v.decode === 'text') {
              expect(Codec.decodeBytesWithBomHeuristics(bytes), 'decode').toBe(input);
            }
            const expected =
              v.extract === 'snippet' ? snippetOf(input)
              : v.extract === 'lf-snippet' ? snippetOf(Codec.normalizeToLfNewlines(input))
              : (() => { throw new Error(`Unknown extract expectation: ${v.extract}`); })();
            expect(Codec.extractFmxmlFromBytes(bytes), 'extract').toBe(expected);
          });
        }
      }
    });
  }
});

describe('hand-written cases', () => {
  const parser = new DefaultFileMakerClipboardParser();

  for (const c of cases.detection) {
    it(`detection: ${c.id}`, () => expect(detectSnippetType(c.text)).toBe(c.expect));
  }
  for (const c of cases.normalizeToXmlText) {
    it(`normalizeToXmlText: ${c.id}`, () => expect(parser.normalizeToXmlText(c.input)).toBe(c.expect));
  }
  for (const c of cases.decodeBytesWithBomHeuristics) {
    it(`decode: ${c.id}`, () => expect(Codec.decodeBytesWithBomHeuristics(inputBytes(c))).toBe(c.expect));
  }
  for (const c of cases.extractFmxmlFromBytes) {
    it(`extract: ${c.id}`, () => expect(Codec.extractFmxmlFromBytes(inputBytes(c))).toBe(c.expect));
  }

  for (const c of cases.parseSnippet) {
    it(`parseSnippet: ${c.id}`, () => {
      if (c.error !== undefined) {
        expect(() => new FmXmlParser().parse(c.xml)).toThrow(ConversionException);
        expect(() => new FmXmlParser().parse(c.xml)).toThrow(new ConversionException(c.error));
        return;
      }
      const model = new FmXmlParser().parse(c.xml);
      expect({
        elementTypes: model.getElementTypes(),
        version: model.version,
        typeHint: model.typeHint,
        fieldNames: model.getFieldNames(),
        layoutNames: model.getLayoutNames(),
        scriptNames: model.getScriptNames(),
      }).toEqual(c.expect);

      const converter = new DefaultXmlToClipboardConverter();
      if (c.payloadError !== undefined) {
        expect(() => converter.convertToClipboardPayload(c.xml)).toThrow(new ConversionException(c.payloadError));
      } else {
        expect(converter.convertToClipboardPayload(c.xml)).toBe(c.payload);
      }
    });
  }

  for (const c of cases.fmSnippetDetectTypes) {
    it(`fmSnippetDetectTypes: ${c.id}`, () => expect(FmSnippet.detectTypes(c.xml)).toEqual(c.expect));
  }

  for (const c of cases.fileNaming) {
    it(`fileNaming: ${c.id}`, async () => {
      const base = resolveFileName(c.pattern, c.extension, c.nowMillis);
      expect(await uniqueFileName(base, (name) => c.existing.includes(name))).toBe(c.expect);
    });
  }
});

/** Raw captures from real FileMaker: our encoding must reproduce FileMaker's bytes exactly. */
describe('raw FileMaker captures', () => {
  const clipboardDir = join(fixturesRoot, 'clipboard');
  const FOLDER_TYPES: Record<string, string> = {
    'script': 'SCRIPT',
    'script-steps': 'SCRIPT_STEPS',
    'fields': 'FIELD_DEFINITION',
    'tables': 'TABLE_DEFINITION',
    'layout-objects': 'LAYOUT_OBJECTS',
    'custom-functions': 'CUSTOM_FUNCTION',
    'value-lists': 'VALUE_LIST',
  };

  const captures: string[] = [];
  const walk = (dir: string) => {
    for (const name of readdirSync(dir).sort()) {
      const p = join(dir, name);
      if (statSync(p).isDirectory()) walk(p);
      else if (name === 'capture.json') captures.push(p);
    }
  };
  if (existsSync(clipboardDir)) walk(clipboardDir);

  if (captures.length === 0) {
    it.skip('no raw captures yet (see shared/fixtures/README.md)', () => {});
  }

  for (const captureJson of captures) {
    const dir = dirname(captureJson);
    const rel = relative(clipboardDir, dir).split('\\').join('/');
    const typeFolder = rel.split('/')[0];
    const capture = JSON.parse(readFileSync(captureJson, 'utf8'));
    for (const f of capture.formats) {
      const bin = join(dir, f.file);
      if (!isFileMakerWindowsFormat(f.name) || !existsSync(bin)) continue;
      it(`${rel}: ${f.name}`, () => {
        const raw = new Uint8Array(readFileSync(bin));
        expect(raw.length, 'payload shorter than its length prefix').toBeGreaterThanOrEqual(4);
        const declared = new DataView(raw.buffer, raw.byteOffset, raw.byteLength).getUint32(0, true);
        expect(declared, 'length prefix').toBe(raw.length - 4);
        const xml = Codec.extractFmxmlFromBytes(raw);
        expect(xml, 'no fmxmlsnippet found').not.toBeNull();
        expect(detectSnippetType(xml), 'snippet type').toBe(FOLDER_TYPES[typeFolder]);
        const payload = new TextDecoder('utf-8', { ignoreBOM: true }).decode(raw.subarray(4));
        expect(Codec.encodeCustomFormatPayload(payload), "re-encoded bytes differ from FileMaker's").toEqual(raw);
      });
    }
  }
});
