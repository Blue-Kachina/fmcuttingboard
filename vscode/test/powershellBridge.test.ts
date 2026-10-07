// End-to-end test of resources/fmclipboard.ps1 against the REAL Windows clipboard.
// Opt-in, because it overwrites the clipboard: set FMCB_CLIPBOARD_TESTS=1 (Windows only). Your clipboard text is
// saved first and restored afterwards; any non-text clipboard content (e.g. images) is lost.
import { readdirSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { DefaultClipboardService } from '../src/clipboard/DefaultClipboardService';
import { PowerShellClipboardBridge } from '../src/clipboard/PowerShellClipboardBridge';
import { windowsFormatNames, detectSnippetType } from '../src/core/ClipboardFormats';
import { encodeCustomFormatPayload, utf16leNullTerminated } from '../src/core/FmClipboardCodec';
import { silentLogger } from './fakes';

const here = dirname(fileURLToPath(import.meta.url));
const enabled = process.platform === 'win32' && process.env.FMCB_CLIPBOARD_TESTS === '1';
const bridge = new PowerShellClipboardBridge(join(here, '..', 'resources', 'fmclipboard.ps1'));
const snippetsDir = join(here, '..', '..', 'shared', 'fixtures', 'snippets');
const fixtures = readdirSync(snippetsDir).flatMap((folder) =>
  readdirSync(join(snippetsDir, folder)).map((file) => join(folder, file)),
);
const noTextClipboard = {
  readText: async () => { throw new Error('plain-text clipboard must not be used'); },
  writeText: async () => { throw new Error('plain-text clipboard must not be used'); },
};

describe.skipIf(!enabled)('PowerShell bridge on the real Windows clipboard', () => {
  let savedText: string | null = null;
  beforeAll(async () => { savedText = (await bridge.read([])).text; });
  afterAll(async () => { if (savedText != null) await bridge.write(savedText, []); });

  for (const fixture of fixtures) {
    it(`round-trips ${fixture} byte-for-byte`, async () => {
      const text = readFileSync(join(snippetsDir, fixture), 'utf8');
      const service = new DefaultClipboardService(noTextClipboard, bridge, silentLogger);
      await service.writeText(text);

      const dump = await bridge.dump();
      const names = windowsFormatNames(detectSnippetType(text));
      for (const name of names) {
        const f = dump.formats.find((x) => x.name === name);
        expect(f, `${name} on clipboard`).toBeDefined();
        expect(f!.bytes).toEqual(encodeCustomFormatPayload(text));
      }
      expect(dump.formats.find((x) => x.name === 'CF_UNICODETEXT')!.bytes).toEqual(utf16leNullTerminated(text));
      expect(await service.readText()).toBe(text);
    }, 30_000);
  }
});
