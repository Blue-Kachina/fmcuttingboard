// Port of jetbrains/.../clipboard/RawClipboardCapture.java: builds a raw clipboard capture (one .bin file per
// format plus capture.json) in the layout shared/fixtures/clipboard/ expects. Writing to disk is left to the caller.
import { detectSnippetType, isFileMakerWindowsFormat, type SnippetType } from './ClipboardFormats';
import { extractFmxmlFromBytes } from './FmClipboardCodec';

export interface FormatBytes {
  id: number;
  /** null for unregistered, non-standard formats */
  name: string | null;
  bytes: Uint8Array;
}

export interface CaptureFile {
  fileName: string;
  bytes: Uint8Array;
}

export interface Capture {
  folderName: string;
  files: CaptureFile[];
}

const STANDARD_FORMAT_NAMES: Record<number, string> = {
  1: 'CF_TEXT',
  2: 'CF_BITMAP',
  7: 'CF_OEMTEXT',
  8: 'CF_DIB',
  13: 'CF_UNICODETEXT',
  16: 'CF_LOCALE',
  17: 'CF_DIBV5',
};

export function displayName(f: FormatBytes): string {
  if (f.name != null && f.name.trim().length > 0) return f.name;
  return STANDARD_FORMAT_NAMES[f.id] ?? `format-${f.id}`;
}

export function sanitize(name: string): string {
  return name.replace(/[^A-Za-z0-9._-]/g, '_');
}

export function buildCapture(formats: readonly FormatBytes[], capturedAt: Date, source: string, os: string): Capture {
  const files: CaptureFile[] = [];
  const entries: object[] = [];
  const used = new Set<string>();
  let detected: SnippetType = 'UNKNOWN';

  for (const f of formats) {
    const name = displayName(f);
    const fileName = uniqueFileName(`${sanitize(name)}.bin`, used);
    files.push({ fileName, bytes: f.bytes });
    const fileMakerFormat = isFileMakerWindowsFormat(f.name);
    entries.push({ name, id: f.id, size: f.bytes.length, file: fileName, fileMakerFormat });
    if (fileMakerFormat && detected === 'UNKNOWN') {
      detected = detectSnippetType(extractFmxmlFromBytes(f.bytes));
    }
  }

  const captureJson = {
    capturedAt: capturedAt.toISOString(),
    source,
    os,
    fileMakerVersion: '',
    detectedSnippetType: detected,
    notes: 'Fill in fileMakerVersion and what was copied before adding this to shared/fixtures/clipboard/.',
    formats: entries,
  };
  files.push({ fileName: 'capture.json', bytes: new TextEncoder().encode(JSON.stringify(captureJson, null, 2) + '\n') });
  return { folderName: `capture-${capturedAt.getTime()}`, files };
}

function uniqueFileName(candidate: string, used: Set<string>): string {
  let name = candidate;
  let n = 2;
  while (used.has(name.toLowerCase())) name = candidate.replace(/\.bin$/, `-${n++}.bin`);
  used.add(name.toLowerCase());
  return name;
}
