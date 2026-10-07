// The VS Code snippets must stay equivalent to the JetBrains live templates
// (jetbrains/src/main/resources/liveTemplates/filemaker_calculation.xml).
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const here = dirname(fileURLToPath(import.meta.url));
const xml = readFileSync(join(here, '..', '..', 'jetbrains', 'src', 'main', 'resources', 'liveTemplates', 'filemaker_calculation.xml'), 'utf8');
const snippets = JSON.parse(readFileSync(join(here, '..', 'snippets', 'fmcalc.json'), 'utf8')) as Record<
  string,
  { prefix: string; description: string; body: string[] }
>;

const decodeXml = (s: string) =>
  s.replace(/&#10;/g, '\n').replace(/&quot;/g, '"').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&');

/** JetBrains template text with $VAR$ placeholders replaced by their position (1, 2, …). */
function jetbrainsTemplates(): Map<string, { text: string; description: string }> {
  const out = new Map<string, { text: string; description: string }>();
  for (const m of xml.matchAll(/<template name="([^"]+)" value="([^"]*)" description="([^"]*)"/g)) {
    const order: string[] = [];
    const text = decodeXml(m[2]).replace(/\$([A-Z0-9_]+)\$/g, (_, v: string) => {
      if (!order.includes(v)) order.push(v);
      return `#${order.indexOf(v) + 1}`;
    });
    out.set(m[1], { text, description: decodeXml(m[3]) });
  }
  return out;
}

/** VS Code snippet body with ${n} / ${n:default} placeholders replaced by #n. */
function vscodeText(body: string[]): string {
  return body.join('\n').replace(/\$\{(\d+)(?::[^}]*)?\}/g, '#$1');
}

describe('snippets match JetBrains live templates', () => {
  const templates = jetbrainsTemplates();

  it('has the same set of templates', () => {
    expect(Object.values(snippets).map((s) => s.prefix).sort()).toEqual([...templates.keys()].sort());
  });

  for (const [name, template] of templates) {
    it(`"${name}" has the same text and description`, () => {
      const snippet = Object.values(snippets).find((s) => s.prefix === name)!;
      expect(vscodeText(snippet.body)).toBe(template.text);
      expect(snippet.description).toBe(template.description);
    });
  }
});
