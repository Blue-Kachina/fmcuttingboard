// Port of jetbrains/.../clipboard/ClipboardFormats.java and SnippetType.java.
// All knowledge comes from shared/data/clipboard-formats.json (bundled by esbuild); change it there.
import formats from '../../../shared/data/clipboard-formats.json';

/** fmxmlsnippet content types; names match the ids in clipboard-formats.json and the Java enum. */
export const SNIPPET_TYPES = [
  'SCRIPT',
  'SCRIPT_STEPS',
  'FIELD_DEFINITION',
  'TABLE_DEFINITION',
  'CUSTOM_FUNCTION',
  'VALUE_LIST',
  'LAYOUT_OBJECTS',
  'UNKNOWN',
] as const;
export type SnippetType = (typeof SNIPPET_TYPES)[number];

export interface TypeRule {
  readonly type: SnippetType;
  readonly label: string;
  readonly detectIfContainsAny: readonly string[];
  readonly windowsFormat: string;
  readonly windowsAliases: readonly string[];
}

function toSnippetType(id: string): SnippetType {
  if (!(SNIPPET_TYPES as readonly string[]).includes(id) || id === 'UNKNOWN') {
    throw new Error(`clipboard-formats.json has an unknown snippet type id: ${id}`);
  }
  return id as SnippetType;
}

const RULES: readonly TypeRule[] = formats.snippetTypes.map((t) => ({
  type: toSnippetType(t.id),
  label: t.label,
  detectIfContainsAny: t.detectIfContainsAny,
  windowsFormat: t.windows.format,
  windowsAliases: t.windows.aliases,
}));

export function rules(): readonly TypeRule[] {
  return RULES;
}

/** First rule (in shared-data order) with a case-sensitive substring match wins. */
export function detectSnippetType(text: string | null | undefined): SnippetType {
  if (text == null || text.length === 0) return 'UNKNOWN';
  for (const rule of RULES) {
    for (const marker of rule.detectIfContainsAny) {
      if (text.includes(marker)) return rule.type;
    }
  }
  return 'UNKNOWN';
}

/** Primary Windows format name followed by any aliases; empty for UNKNOWN. */
export function windowsFormatNames(type: SnippetType): string[] {
  const rule = RULES.find((r) => r.type === type);
  return rule ? [rule.windowsFormat, ...rule.windowsAliases] : [];
}

/** Every FileMaker Windows format name (primaries and aliases), in shared-data order. */
export function allWindowsFormatNames(): string[] {
  const names = new Set<string>();
  for (const rule of RULES) {
    names.add(rule.windowsFormat);
    rule.windowsAliases.forEach((a) => names.add(a));
  }
  return [...names];
}

export function isFileMakerWindowsFormat(name: string | null | undefined): boolean {
  if (name == null) return false;
  const lower = name.toLowerCase();
  return allWindowsFormatNames().some((known) => known.toLowerCase() === lower);
}

export function maxCustomPayloadBytes(): number {
  return formats.windows.customFormatPayload.maxBytes;
}
