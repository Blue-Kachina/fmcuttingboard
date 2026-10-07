// Test doubles for the action ports and the clipboard.
import type { ClipboardService, Logger } from '../src/clipboard/ClipboardService';
import type { ActionContext, CreatedFile, CuttingBoardFiles, Notifier, Preview, PushTarget, Settings } from '../src/actions/ports';

export const silentLogger: Logger = { trace() {}, debug() {}, info() {}, warn() {}, error() {} };

export interface Notification {
  level: 'info' | 'warn' | 'error';
  title: string;
  message: string;
  hasDetails: boolean;
}

export class FakeNotifier implements Notifier {
  readonly all: Notification[] = [];
  info(title: string, message: string) { this.all.push({ level: 'info', title, message, hasDetails: false }); }
  warn(title: string, message: string) { this.all.push({ level: 'warn', title, message, hasDetails: false }); }
  error(title: string, message: string, details?: unknown) {
    this.all.push({ level: 'error', title, message, hasDetails: details !== undefined });
  }
  get last(): Notification | undefined { return this.all[this.all.length - 1]; }
}

export class FakeClipboard implements ClipboardService {
  writes: string[] = [];
  readError?: Error;
  writeError?: Error;
  constructor(public content = '') {}
  async readText() {
    if (this.readError) throw this.readError;
    return this.content;
  }
  async writeText(text: string) {
    if (this.writeError) throw this.writeError;
    this.writes.push(text);
    this.content = text;
  }
}

export class FakePreview implements Preview {
  shown: { title: string; content: string; limit: number }[] = [];
  constructor(public answer = true) {}
  async confirmWrite(title: string, content: string, previewLimit: number) {
    this.shown.push({ title, content, limit: previewLimit });
    return this.answer;
  }
}

export class FakeFiles implements CuttingBoardFiles {
  created: { extension: string; content: string; settings: Settings }[] = [];
  error?: Error;
  async createAndOpen(settings: Settings, extension: '.xml' | '.fmcalc', content: string): Promise<CreatedFile> {
    if (this.error) throw this.error;
    this.created.push({ extension, content, settings });
    return { displayPath: `.fmCuttingBoard/${this.created.length}${extension}` };
  }
}

export function defaultSettings(overrides: Partial<Settings> = {}): Settings {
  return { baseDirName: '.fmCuttingBoard', fileNamePattern: '{timestamp}', previewBeforeClipboardWrite: false, enableDiagnostics: false, ...overrides };
}

export function fakeContext(opts: { clipboard?: string; settings?: Partial<Settings>; previewAnswer?: boolean } = {}) {
  const clipboard = new FakeClipboard(opts.clipboard ?? '');
  const notifier = new FakeNotifier();
  const preview = new FakePreview(opts.previewAnswer ?? true);
  const files = new FakeFiles();
  const settings = defaultSettings(opts.settings);
  const ctx: ActionContext = { clipboard, notifier, preview, files, settings: () => settings, log: silentLogger };
  return { ctx, clipboard, notifier, preview, files };
}

export function target(extension: string | undefined, text: string): PushTarget {
  return { extension, readText: async () => text };
}
