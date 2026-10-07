// Small helpers that reproduce Java string semantics exactly, where JavaScript's built-ins differ.
// The JetBrains plugin is the reference implementation; parity is pinned by shared/fixtures.

/** Java's String.trim(): removes leading/trailing chars <= U+0020 (JS trim() also strips Unicode spaces/BOM). */
export function javaTrim(s: string): string {
  let start = 0;
  let end = s.length;
  while (start < end && s.charCodeAt(start) <= 0x20) start++;
  while (end > start && s.charCodeAt(end - 1) <= 0x20) end--;
  return s.substring(start, end);
}

// Characters for which Java's Character.isWhitespace() is true (no-break spaces are excluded, unlike JS \s)
const JAVA_BLANK = new RegExp("^[\\u0009-\\u000D\\u001C-\\u0020\\u1680\\u2000-\\u2006\\u2008-\\u200A\\u2028-\\u2029\\u205F\\u3000]*$");

/** Java's String.isBlank(), treating null as blank. */
export function javaIsBlank(s: string | null | undefined): boolean {
  return s == null || JAVA_BLANK.test(s);
}

/** Java's String.replace(CharSequence, CharSequence): replaces every literal occurrence. */
export function replaceAllLiteral(s: string, target: string, replacement: string): string {
  return s.split(target).join(replacement);
}
