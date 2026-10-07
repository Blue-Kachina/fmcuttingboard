// Client for resources/fmclipboard.ps1, which reads and writes FileMaker's custom Windows clipboard formats
// through .NET (VS Code's clipboard API is text-only). The script is a byte pipe: every encoding decision is
// made in TypeScript by src/core/FmClipboardCodec.ts.
import { spawn } from 'node:child_process';

export interface RawFormat {
  name: string;
  id?: number;
  bytes: Uint8Array;
}

export interface BridgeReadResult {
  /** CF_UNICODETEXT content, or null when the clipboard has no text */
  text: string | null;
  formats: RawFormat[];
}

interface BridgeJson {
  ok: boolean;
  error?: string;
  text?: string | null;
  formats?: { name: string; id?: number; base64: string }[] | { name: string; id?: number; base64: string };
}

const TIMEOUT_MS = 15_000;

/** Native clipboard access for FileMaker formats (implemented with PowerShell on Windows; faked in tests). */
export interface NativeClipboardBridge {
  read(formatNames: readonly string[]): Promise<BridgeReadResult>;
  dump(): Promise<BridgeReadResult>;
  write(text: string, formats: readonly { name: string; bytes: Uint8Array }[]): Promise<void>;
}

export class PowerShellClipboardBridge implements NativeClipboardBridge {
  constructor(
    private readonly scriptPath: string,
    private readonly powershellExe = 'powershell.exe',
  ) {}

  /** CF_UNICODETEXT plus the listed formats (when present). */
  async read(formatNames: readonly string[]): Promise<BridgeReadResult> {
    return toReadResult(await this.run(['-Mode', 'read', '-FormatNames', formatNames.join(',')]));
  }

  /** Every clipboard format whose data is bytes, a stream or text (diagnostics). */
  async dump(): Promise<BridgeReadResult> {
    return toReadResult(await this.run(['-Mode', 'dump']));
  }

  /** Sets CF_UNICODETEXT and each format in one clipboard operation. */
  async write(text: string, formats: readonly { name: string; bytes: Uint8Array }[]): Promise<void> {
    const request = {
      text: Buffer.from(text, 'utf8').toString('base64'),
      formats: formats.map((f) => ({ name: f.name, base64: Buffer.from(f.bytes).toString('base64') })),
    };
    await this.run(['-Mode', 'write'], JSON.stringify(request));
  }

  private run(args: string[], stdin?: string): Promise<BridgeJson> {
    return new Promise((resolve, reject) => {
      const child = spawn(
        this.powershellExe,
        ['-NoProfile', '-NonInteractive', '-STA', '-ExecutionPolicy', 'Bypass', '-File', this.scriptPath, ...args],
        { windowsHide: true },
      );
      const stdout: Buffer[] = [];
      const stderr: Buffer[] = [];
      const timer = setTimeout(() => {
        child.kill();
        reject(new Error(`Clipboard helper timed out after ${TIMEOUT_MS / 1000}s`));
      }, TIMEOUT_MS);

      child.stdout.on('data', (d: Buffer) => stdout.push(d));
      child.stderr.on('data', (d: Buffer) => stderr.push(d));
      child.on('error', (err) => {
        clearTimeout(timer);
        reject(new Error(`Could not start PowerShell: ${err.message}`, { cause: err }));
      });
      child.on('close', (code) => {
        clearTimeout(timer);
        const out = Buffer.concat(stdout).toString('utf8').trim();
        let json: BridgeJson;
        try {
          json = JSON.parse(out) as BridgeJson;
        } catch {
          const err = Buffer.concat(stderr).toString('utf8').trim();
          reject(new Error(`Clipboard helper failed (exit ${code}): ${err || out || 'no output'}`));
          return;
        }
        if (!json.ok) reject(new Error(json.error ?? `Clipboard helper failed (exit ${code})`));
        else resolve(json);
      });

      child.stdin.end(stdin ?? '');
    });
  }
}

function toReadResult(json: BridgeJson): BridgeReadResult {
  // ConvertTo-Json in Windows PowerShell 5.1 emits a lone object (not an array) for one-element arrays
  const formats = json.formats == null ? [] : Array.isArray(json.formats) ? json.formats : [json.formats];
  return {
    text: json.text == null ? null : Buffer.from(json.text, 'base64').toString('utf8'),
    formats: formats.map((f) => ({ name: f.name, id: f.id, bytes: new Uint8Array(Buffer.from(f.base64, 'base64')) })),
  };
}
