// Port of jetbrains/.../clipboard/FmClipboardCodec.java: pure byte-level encoding and decoding of FileMaker
// clipboard payloads. Rules: shared/data/clipboard-formats.json; behavior pinned by shared/fixtures/golden.
import { javaTrim, replaceAllLiteral } from './javaCompat';

const OPEN_TAG = '<fmxmlsnippet';
const CLOSE_TAG = '</fmxmlsnippet>';

// ignoreBOM: true keeps a U+FEFF in the output, as Java's decoders do; BOMs are handled explicitly below.
const UTF8 = new TextDecoder('utf-8', { ignoreBOM: true });
const UTF16LE = new TextDecoder('utf-16le', { ignoreBOM: true });

function decodeUtf8(bytes: Uint8Array): string {
  return UTF8.decode(bytes);
}

function decodeUtf16le(bytes: Uint8Array): string {
  return UTF16LE.decode(bytes);
}

/** UTF-16BE via byte swap, so it works without full ICU. */
function decodeUtf16be(bytes: Uint8Array): string {
  const swapped = new Uint8Array(bytes.length);
  for (let i = 0; i + 1 < bytes.length; i += 2) {
    swapped[i] = bytes[i + 1];
    swapped[i + 1] = bytes[i];
  }
  if (bytes.length % 2 === 1) swapped[bytes.length - 1] = bytes[bytes.length - 1];
  return decodeUtf16le(swapped);
}

function encodeUtf8(s: string): Uint8Array {
  return new TextEncoder().encode(s);
}

function encodeUtf16le(s: string): Uint8Array {
  const out = new Uint8Array(s.length * 2);
  for (let i = 0; i < s.length; i++) {
    const c = s.charCodeAt(i);
    out[i * 2] = c & 0xff;
    out[i * 2 + 1] = c >>> 8;
  }
  return out;
}

function encodeUtf16be(s: string): Uint8Array {
  const out = new Uint8Array(s.length * 2);
  for (let i = 0; i < s.length; i++) {
    const c = s.charCodeAt(i);
    out[i * 2] = c >>> 8;
    out[i * 2 + 1] = c & 0xff;
  }
  return out;
}

/** Converts CRLF and lone CR to LF. */
export function normalizeToLfNewlines(s: string | null | undefined): string {
  if (s == null || s.length === 0) return '';
  return replaceAllLiteral(replaceAllLiteral(s, '\r\n', '\n'), '\r', '\n');
}

/** The bytes for a FileMaker custom format (e.g. Mac-XMSS): newlines normalized, then length-prefixed UTF-8. */
export function encodeCustomFormatPayload(text: string | null | undefined): Uint8Array {
  return utf8LengthPrefixedNoBom(normalizeToLfNewlines(text));
}

/** [4-byte little-endian payload length] + [UTF-8 payload without BOM], no trailing NUL. */
export function utf8LengthPrefixedNoBom(s: string | null | undefined): Uint8Array {
  const payload = encodeUtf8(s ?? '');
  const out = new Uint8Array(4 + payload.length);
  new DataView(out.buffer).setUint32(0, payload.length, true);
  out.set(payload, 4);
  return out;
}

/** CF_UNICODETEXT bytes: UTF-16LE without BOM, followed by a two-byte NUL terminator. */
export function utf16leNullTerminated(s: string | null | undefined): Uint8Array {
  const data = encodeUtf16le(s ?? '');
  const out = new Uint8Array(data.length + 2);
  out.set(data, 0);
  return out;
}

/**
 * Decodes clipboard bytes of unknown encoding: honours a UTF-8/UTF-16 BOM, otherwise guesses UTF-16
 * from the distribution of zero bytes, otherwise UTF-8. Embedded NULs are removed.
 */
export function decodeBytesWithBomHeuristics(bytes: Uint8Array | null | undefined): string {
  if (bytes == null || bytes.length === 0) return '';

  if (bytes.length >= 3 && bytes[0] === 0xef && bytes[1] === 0xbb && bytes[2] === 0xbf) {
    return stripNulls(decodeUtf8(bytes.subarray(3)));
  }
  if (bytes.length >= 2) {
    if (bytes[0] === 0xfe && bytes[1] === 0xff) return stripNulls(decodeUtf16be(bytes.subarray(2)));
    if (bytes[0] === 0xff && bytes[1] === 0xfe) return stripNulls(decodeUtf16le(bytes.subarray(2)));
  }

  // No BOM: 10% or more zero bytes, concentrated on one parity, suggests UTF-16
  let zerosEven = 0;
  let zerosOdd = 0;
  for (let i = 0; i < bytes.length; i++) {
    if (bytes[i] === 0) {
      if ((i & 1) === 0) zerosEven++;
      else zerosOdd++;
    }
  }
  const threshold = Math.max(2, Math.floor(bytes.length / 10));
  if (zerosOdd > zerosEven && zerosOdd >= threshold) {
    return stripNulls(decodeUtf16le(bytes));
  } else if (zerosEven > zerosOdd && zerosEven >= threshold) {
    return stripNulls(decodeUtf16be(bytes));
  }

  return stripNulls(decodeUtf8(bytes));
}

/**
 * Locates `<fmxmlsnippet … </fmxmlsnippet>` (first opening tag through last closing tag) directly in raw
 * bytes, trying UTF-8 first, then UTF-16LE, then UTF-16BE. Returns the trimmed snippet, or null if none is
 * found. UTF-16 matches must start on an even byte offset, so one encoding is never mistaken for the other.
 */
export function extractFmxmlFromBytes(bytes: Uint8Array | null | undefined): string | null {
  if (bytes == null || bytes.length === 0) return null;

  const startUtf8 = encodeUtf8(OPEN_TAG);
  const endUtf8 = encodeUtf8(CLOSE_TAG);
  const start = indexOf(bytes, startUtf8, 1);
  if (start >= 0) {
    const end = lastIndexOf(bytes, endUtf8, 1);
    if (end >= start) {
      return javaTrim(decodeUtf8(bytes.subarray(start, end + endUtf8.length)));
    }
  }

  return extractUtf16(bytes, true) ?? extractUtf16(bytes, false);
}

function extractUtf16(bytes: Uint8Array, littleEndian: boolean): string | null {
  const encode = littleEndian ? encodeUtf16le : encodeUtf16be;
  const startPattern = encode(OPEN_TAG);
  const endPattern = encode(CLOSE_TAG);
  const start = indexOf(bytes, startPattern, 2);
  if (start < 0) return null;
  const end = lastIndexOf(bytes, endPattern, 2);
  if (end < start) return null;
  const slice = bytes.subarray(start, end + endPattern.length);
  return javaTrim(stripNulls(littleEndian ? decodeUtf16le(slice) : decodeUtf16be(slice)));
}

export function stripNulls(s: string | null | undefined): string {
  return s == null ? '' : replaceAllLiteral(s, '\u0000', '');
}

/** First match at an offset divisible by `alignment`, or -1. */
function indexOf(data: Uint8Array, pattern: Uint8Array, alignment: number): number {
  outer: for (let i = 0; i <= data.length - pattern.length; i += alignment) {
    for (let j = 0; j < pattern.length; j++) {
      if (data[i + j] !== pattern[j]) continue outer;
    }
    return i;
  }
  return -1;
}

/** Last match at an offset divisible by `alignment`, or -1. */
function lastIndexOf(data: Uint8Array, pattern: Uint8Array, alignment: number): number {
  let i = data.length - pattern.length;
  i -= i % alignment;
  outer: for (; i >= 0; i -= alignment) {
    for (let j = 0; j < pattern.length; j++) {
      if (data[i + j] !== pattern[j]) continue outer;
    }
    return i;
  }
  return -1;
}
