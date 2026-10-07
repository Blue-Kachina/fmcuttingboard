// Port of the decision logic in jetbrains/.../clipboard/DefaultClipboardService.java.
//
// Read: plain text first; when there is none (FileMaker puts only its own formats on the clipboard), extract the
// fmxmlsnippet from FileMaker's native formats. Write: on Windows, fmxmlsnippet content is written as
// CF_UNICODETEXT plus the FileMaker format for its type (and aliases); anything else is plain text. If the native
// path fails, fall back to plain text, as the JetBrains plugin does.
import {
  allWindowsFormatNames,
  detectSnippetType,
  maxCustomPayloadBytes,
  windowsFormatNames,
} from '../core/ClipboardFormats';
import { encodeCustomFormatPayload, extractFmxmlFromBytes, stripNulls, utf16leNullTerminated } from '../core/FmClipboardCodec';
import { javaIsBlank } from '../core/javaCompat';
import { ClipboardAccessException, type ClipboardService, type Logger, type TextClipboard } from './ClipboardService';
import type { NativeClipboardBridge } from './PowerShellClipboardBridge';

export class DefaultClipboardService implements ClipboardService {
  /** @param bridge the Windows native bridge, or undefined on other platforms (plain text only) */
  constructor(
    private readonly text: TextClipboard,
    private readonly bridge: NativeClipboardBridge | undefined,
    private readonly log: Logger,
  ) {}

  async readText(): Promise<string> {
    if (this.bridge) {
      try {
        const result = await this.bridge.read(allWindowsFormatNames());
        if (result.text != null && !javaIsBlank(result.text)) {
          this.log.debug(`[CB] Read CF_UNICODETEXT (${result.text.length} chars)`);
          return stripNulls(result.text);
        }
        for (const f of result.formats) {
          const snippet = extractFmxmlFromBytes(f.bytes);
          if (snippet != null && !javaIsBlank(snippet)) {
            this.log.info(`[CB] fmxmlsnippet extracted from native format '${f.name}' (${f.bytes.length} bytes)`);
            return snippet;
          }
        }
        this.log.debug(`[CB] No text or FileMaker formats on the clipboard`);
        return '';
      } catch (ex) {
        this.log.warn(`[CB] Native clipboard read failed; falling back to plain text: ${messageOf(ex)}`);
      }
    }
    try {
      return stripNulls(await this.text.readText());
    } catch (ex) {
      throw new ClipboardAccessException(messageOf(ex), { cause: ex });
    }
  }

  async writeText(text: string): Promise<void> {
    if (this.bridge && (await this.tryNativeWrite(this.bridge, text))) return;
    try {
      await this.text.writeText(text);
    } catch (ex) {
      throw new ClipboardAccessException(messageOf(ex), { cause: ex });
    }
  }

  private async tryNativeWrite(bridge: NativeClipboardBridge, text: string): Promise<boolean> {
    const type = detectSnippetType(text);
    if (type === 'UNKNOWN') {
      this.log.info('[CB-DIAG] Native write skipped: unknown fmxmlsnippet type (falling back to text-only)');
      return false;
    }
    const payload = encodeCustomFormatPayload(text);
    const max = maxCustomPayloadBytes();
    if (payload.length > max || utf16leNullTerminated(text).length > max) {
      this.log.info('[CB] Native path: payload too large for native write; falling back');
      return false;
    }
    const names = windowsFormatNames(type);
    try {
      await bridge.write(text, names.map((name) => ({ name, bytes: payload })));
      this.log.info(`[CB-DIAG] Native write: detectedType=${type}, formats=${names.join('+')}, size=${payload.length}`);
      return true;
    } catch (ex) {
      this.log.warn(`[CB] Native clipboard write failed; falling back to plain text: ${messageOf(ex)}`);
      return false;
    }
  }
}

export function messageOf(ex: unknown): string {
  const msg = ex instanceof Error ? ex.message : String(ex);
  return msg == null || javaIsBlank(msg) ? (ex instanceof Error ? ex.name : 'Error') : msg;
}
