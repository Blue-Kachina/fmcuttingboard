// Port of jetbrains/.../fm/DefaultFileMakerClipboardParser.java: finds an fmxmlsnippet in clipboard text.
import { javaIsBlank, javaTrim } from './javaCompat';

const OPEN = '<fmxmlsnippet';
const CLOSE = '</fmxmlsnippet>';

// Fallback for moderate-size inputs only; mirrors Java's (?is)<fmxmlsnippet\b[\s\S]*?</fmxmlsnippet>
const FMXMLSNIPPET_PATTERN = /<fmxmlsnippet\b[\s\S]*?<\/fmxmlsnippet>/i;

/** Guards for performance; large clipboard payloads are not uncommon (~2 MB of characters). */
export const MAX_SCAN_CHARS = 2_000_000;

export class DefaultFileMakerClipboardParser {
  isLikelyFileMakerContent(clipboardText: string | null | undefined): boolean {
    if (clipboardText == null || javaIsBlank(clipboardText)) return false;
    return indexOfIgnoreCase(clipboardText, OPEN) >= 0;
  }

  /** The first `<fmxmlsnippet …>…</fmxmlsnippet>` block (tags matched case-insensitively), trimmed; or null. */
  normalizeToXmlText(clipboardText: string | null | undefined): string | null {
    if (clipboardText == null) return null;

    const openIdx = indexOfIgnoreCase(clipboardText, OPEN);
    if (openIdx >= 0) {
      const openEnd = clipboardText.indexOf('>', openIdx);
      if (openEnd > openIdx) {
        const closeIdx = indexOfIgnoreCase(clipboardText, CLOSE, openEnd + 1);
        if (closeIdx > openEnd) {
          return javaTrim(clipboardText.substring(openIdx, closeIdx + CLOSE.length));
        }
      }
    }

    if (clipboardText.length > MAX_SCAN_CHARS) return null;

    const m = FMXMLSNIPPET_PATTERN.exec(clipboardText);
    return m ? javaTrim(m[0]) : null;
  }
}

/** Case-insensitive indexOf (per-character lower-casing, like the Java version). */
export function indexOfIgnoreCase(text: string, needle: string, fromIndex = 0): number {
  const n = text.length;
  const m = needle.length;
  const from = Math.max(0, fromIndex);
  if (m === 0) return Math.min(from, n);
  const lowerNeedle = needle.toLowerCase();
  for (let i = from; i <= n - m; i++) {
    let j = 0;
    while (j < m && text.charAt(i + j).toLowerCase() === lowerNeedle.charAt(j)) j++;
    if (j === m) return i;
  }
  return -1;
}
