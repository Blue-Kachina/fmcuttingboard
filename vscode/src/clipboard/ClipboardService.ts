// Port of jetbrains/.../clipboard/ClipboardService.java and ClipboardAccessException.java.

export class ClipboardAccessException extends Error {
  constructor(message: string, options?: { cause?: unknown }) {
    super(message, options);
    this.name = 'ClipboardAccessException';
  }
}

export interface ClipboardService {
  /** Clipboard content as text ('' when there is none). FileMaker's native formats are decoded on Windows. */
  readText(): Promise<string>;
  /** Writes text; fmxmlsnippet content also gets FileMaker's native format on Windows. */
  writeText(text: string): Promise<void>;
}

/** Plain-text clipboard (vscode.env.clipboard in the extension). */
export interface TextClipboard {
  readText(): Promise<string>;
  writeText(text: string): Promise<void>;
}

export interface Logger {
  trace(message: string): void;
  debug(message: string): void;
  info(message: string): void;
  warn(message: string): void;
  error(message: string | Error): void;
}
