// DefaultClipboardService decisions (mirrors jetbrains/.../clipboard/DefaultClipboardService.java), with a fake bridge.
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import type { TextClipboard } from '../src/clipboard/ClipboardService';
import { DefaultClipboardService } from '../src/clipboard/DefaultClipboardService';
import type { BridgeReadResult, NativeClipboardBridge } from '../src/clipboard/PowerShellClipboardBridge';
import { encodeCustomFormatPayload } from '../src/core/FmClipboardCodec';
import { silentLogger } from './fakes';

const fixtures = join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'shared', 'fixtures');
const LAYOUT = readFileSync(join(fixtures, 'snippets', 'layout-objects', 'LayoutObjects.xml'), 'utf8');
const STEPS = '<fmxmlsnippet type="FMObjectList"><Step id="1"/></fmxmlsnippet>';

class FakeBridge implements NativeClipboardBridge {
  writes: { text: string; formats: { name: string; bytes: Uint8Array }[] }[] = [];
  fail = false;
  constructor(public readResult: BridgeReadResult = { text: null, formats: [] }) {}
  async read() {
    if (this.fail) throw new Error('powershell blocked');
    return this.readResult;
  }
  async dump() { return this.readResult; }
  async write(text: string, formats: readonly { name: string; bytes: Uint8Array }[]) {
    if (this.fail) throw new Error('powershell blocked');
    this.writes.push({ text, formats: [...formats] });
  }
}

class FakeText implements TextClipboard {
  writes: string[] = [];
  constructor(public content = '') {}
  async readText() { return this.content; }
  async writeText(t: string) { this.writes.push(t); }
}

describe('reading', () => {
  it('prefers plain text when present', async () => {
    const bridge = new FakeBridge({ text: 'If ( 1 ; 2 )', formats: [{ name: 'Mac-XMSS', bytes: encodeCustomFormatPayload(STEPS) }] });
    expect(await new DefaultClipboardService(new FakeText(), bridge, silentLogger).readText()).toBe('If ( 1 ; 2 )');
  });

  it("extracts the snippet from FileMaker's native format when there is no text", async () => {
    const bridge = new FakeBridge({ text: null, formats: [{ name: 'Mac-XMSS', bytes: encodeCustomFormatPayload(STEPS) }] });
    expect(await new DefaultClipboardService(new FakeText(), bridge, silentLogger).readText()).toBe(STEPS);
  });

  it('returns empty when the clipboard has neither', async () => {
    expect(await new DefaultClipboardService(new FakeText('ignored'), new FakeBridge(), silentLogger).readText()).toBe('');
  });

  it('falls back to plain text when the bridge fails', async () => {
    const bridge = new FakeBridge();
    bridge.fail = true;
    expect(await new DefaultClipboardService(new FakeText('fallback'), bridge, silentLogger).readText()).toBe('fallback');
  });

  it('uses plain text only without a bridge (macOS, Linux)', async () => {
    expect(await new DefaultClipboardService(new FakeText('a\u0000b'), undefined, silentLogger).readText()).toBe('ab');
  });
});

describe('writing', () => {
  it('writes the FileMaker format for the snippet type, plus aliases, with the shared encoding', async () => {
    const bridge = new FakeBridge();
    const text = new FakeText();
    await new DefaultClipboardService(text, bridge, silentLogger).writeText(LAYOUT);
    expect(bridge.writes).toHaveLength(1);
    expect(bridge.writes[0].text).toBe(LAYOUT);
    expect(bridge.writes[0].formats.map((f) => f.name)).toEqual(['Mac-XML2', 'Mac-XML']);
    for (const f of bridge.writes[0].formats) expect(f.bytes).toEqual(encodeCustomFormatPayload(LAYOUT));
    expect(text.writes).toEqual([]);
  });

  it('writes plain text for content that is not a known snippet type', async () => {
    const bridge = new FakeBridge();
    const text = new FakeText();
    await new DefaultClipboardService(text, bridge, silentLogger).writeText('<fmxmlsnippet><Theme/></fmxmlsnippet>');
    expect(bridge.writes).toEqual([]);
    expect(text.writes).toEqual(['<fmxmlsnippet><Theme/></fmxmlsnippet>']);
  });

  it('falls back to plain text when the native write fails (as the JetBrains plugin does)', async () => {
    const bridge = new FakeBridge();
    bridge.fail = true;
    const text = new FakeText();
    await new DefaultClipboardService(text, bridge, silentLogger).writeText(STEPS);
    expect(text.writes).toEqual([STEPS]);
  });
});
