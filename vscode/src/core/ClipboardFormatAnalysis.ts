// Port of the [CB-ANALYZE] report in jetbrains/.../actions/ClipboardFormatsDumpAction.java (diagnostics only).

function detectBom(b: Uint8Array): string {
  if (b.length >= 3 && b[0] === 0xef && b[1] === 0xbb && b[2] === 0xbf) return 'UTF-8';
  if (b.length >= 2 && b[0] === 0xff && b[1] === 0xfe) return 'UTF-16LE';
  if (b.length >= 2 && b[0] === 0xfe && b[1] === 0xff) return 'UTF-16BE';
  return 'none';
}

function guessEncoding(b: Uint8Array, bom: string): string {
  if (bom !== 'none') return bom;
  let zeros = 0;
  for (let i = 1; i < Math.min(b.length, 256); i += 2) if (b[i] === 0) zeros++;
  return zeros > 64 ? 'UTF-16LE?' : 'unknown';
}

function count(b: Uint8Array, value: number): number {
  let c = 0;
  for (const x of b) if (x === value) c++;
  return c;
}

function countCrlf(b: Uint8Array): number {
  let c = 0;
  for (let i = 0; i + 1 < b.length; i++) if (b[i] === 0x0d && b[i + 1] === 0x0a) c++;
  return c;
}

function hexPreview(b: Uint8Array, max: number): string {
  const n = Math.min(b.length, max);
  const parts: string[] = [];
  for (let i = 0; i < n; i++) parts.push(b[i].toString(16).toUpperCase().padStart(2, '0'));
  return parts.join(' ') + (b.length > n ? ' …' : '');
}

function safePreview(b: Uint8Array, enc: string): string | null {
  const s = new TextDecoder(enc.startsWith('UTF-16') ? 'utf-16le' : 'utf-8').decode(b);
  const idx = s.toLowerCase().indexOf('<fmxmlsnippet');
  if (idx < 0) return null;
  return s.substring(idx, Math.min(s.length, idx + 120)).replace(/[\r\n]+/g, '\\n');
}

export function analyzeBytesSection(id: number | undefined, name: string, bytes: Uint8Array): string {
  const bom = detectBom(bytes);
  const enc = guessEncoding(bytes, bom);
  const lines = [
    `[CB-ANALYZE] id=${id ?? '?'}, name='${name}'`,
    `  size=${bytes.length} bytes`,
    `  BOM=${bom}`,
    `  encodingGuess=${enc}`,
    `  newlines: CR=${count(bytes, 0x0d)}, LF=${count(bytes, 0x0a)}, CRLF=${countCrlf(bytes)}`,
    `  endsWithNull=${bytes.length > 0 && bytes[bytes.length - 1] === 0}`,
    `  hexPreview=${hexPreview(bytes, 64)}`,
  ];
  const preview = safePreview(bytes, enc);
  if (preview != null) lines.push(`  textPreview=${preview}`);
  return lines.join('\n');
}
