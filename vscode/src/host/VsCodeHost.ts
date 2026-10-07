// VS Code implementations of the action ports (src/actions/ports.ts).
import * as path from 'node:path';
import * as vscode from 'vscode';
import type { Logger, TextClipboard } from '../clipboard/ClipboardService';
import { messageOf } from '../clipboard/DefaultClipboardService';
import type { CreatedFile, CuttingBoardFiles, Notifier, Preview, PushTarget, Settings } from '../actions/ports';
import { baseDirNameOrDefault, GITIGNORE, GITIGNORE_CONTENT, resolveFileName, uniqueFileName } from '../core/ProjectFiles';

export const SECTION = 'fmcuttingboard';

/** The folder that plays the role of the JetBrains "project": the active file's workspace folder, else the first. */
export function projectRoot(): vscode.Uri | undefined {
  const active = vscode.window.activeTextEditor?.document.uri;
  const folder = active ? vscode.workspace.getWorkspaceFolder(active) : undefined;
  return (folder ?? vscode.workspace.workspaceFolders?.[0])?.uri;
}

export function readSettings(): Settings {
  const c = vscode.workspace.getConfiguration(SECTION, projectRoot());
  return {
    baseDirName: c.get<string>('baseDirName', '.fmCuttingBoard'),
    fileNamePattern: c.get<string>('fileNamePattern', '{timestamp}'),
    previewBeforeClipboardWrite: c.get<boolean>('previewBeforeClipboardWrite', false),
    enableDiagnostics: c.get<boolean>('enableDiagnostics', false),
  };
}

export class VsCodeLogger implements Logger {
  constructor(readonly channel: vscode.LogOutputChannel) {}
  trace(m: string) { this.channel.trace(m); }
  debug(m: string) { this.channel.debug(m); }
  info(m: string) { this.channel.info(m); }
  warn(m: string) { this.channel.warn(m); }
  error(m: string | Error) { this.channel.error(m); }
}

export class VsCodeNotifier implements Notifier {
  constructor(private readonly log: VsCodeLogger) {}

  info(title: string, message: string): void {
    this.log.info(`[${title}] ${message}`);
    void vscode.window.showInformationMessage(message);
  }

  warn(title: string, message: string): void {
    this.log.warn(`[${title}] ${message}`);
    void vscode.window.showWarningMessage(message);
  }

  error(title: string, message: string, details?: unknown): void {
    this.log.error(`[${title}] ${message}`);
    if (details === undefined) {
      void vscode.window.showErrorMessage(message);
      return;
    }
    void vscode.window.showErrorMessage(message, 'Show Details').then((choice) => {
      if (choice !== 'Show Details') return;
      this.log.error(details instanceof Error ? details : new Error(String(details)));
      this.log.channel.show(true);
    });
  }
}

export class VsCodePreview implements Preview {
  /** Mirrors PreviewDialogs.confirmWrite (a modal Yes/No with the content truncated to `previewLimit`). */
  async confirmWrite(title: string, content: string, previewLimit: number): Promise<boolean> {
    const preview =
      previewLimit > 0 && content.length > previewLimit ? `${content.substring(0, previewLimit)}\n… (truncated)` : content;
    const detail = `About to write the following content to the clipboard:\n\n${preview}\n\nProceed?`;
    const choice = await vscode.window.showInformationMessage(title, { modal: true, detail }, 'Yes');
    return choice === 'Yes';
  }
}

export class VsCodeTextClipboard implements TextClipboard {
  readText(): Promise<string> {
    return Promise.resolve(vscode.env.clipboard.readText());
  }
  writeText(text: string): Promise<void> {
    return Promise.resolve(vscode.env.clipboard.writeText(text));
  }
}

async function exists(uri: vscode.Uri): Promise<boolean> {
  try {
    await vscode.workspace.fs.stat(uri);
    return true;
  } catch {
    return false;
  }
}

function displayPath(root: vscode.Uri, file: vscode.Uri): string {
  // Local files: native separators, like the JetBrains plugin's Path.relativize
  if (root.scheme === 'file' && file.scheme === 'file') return path.relative(root.fsPath, file.fsPath);
  return vscode.workspace.asRelativePath(file, false);
}

/** Ensures the cutting board folder exists; writes its `.gitignore` only when the folder is newly created. */
export async function ensureBaseDir(settings: Settings): Promise<{ root: vscode.Uri; dir: vscode.Uri }> {
  const root = projectRoot();
  if (!root) throw new Error('No folder is open. Open a folder or workspace first.');
  const dir = vscode.Uri.joinPath(root, baseDirNameOrDefault(settings.baseDirName));
  if (!(await exists(dir))) {
    await vscode.workspace.fs.createDirectory(dir);
    await vscode.workspace.fs.writeFile(vscode.Uri.joinPath(dir, GITIGNORE), new TextEncoder().encode(GITIGNORE_CONTENT));
  }
  return { root, dir };
}

export class VsCodeCuttingBoardFiles implements CuttingBoardFiles {
  constructor(private readonly log: Logger) {}

  async createAndOpen(settings: Settings, extension: '.xml' | '.fmcalc', content: string): Promise<CreatedFile> {
    const { root, dir } = await ensureBaseDir(settings);
    const baseName = resolveFileName(settings.fileNamePattern, extension, Date.now());
    const name = await uniqueFileName(baseName, (n) => exists(vscode.Uri.joinPath(dir, n)));
    const file = vscode.Uri.joinPath(dir, name);
    const bytes = new TextEncoder().encode(content);
    await vscode.workspace.fs.writeFile(file, bytes);
    this.log.info(`Wrote ${extension} to: ${file.toString()} (bytes=${bytes.length}, chars=${content.length})`);
    try {
      await vscode.window.showTextDocument(file, { preview: false });
    } catch (ex) {
      this.log.warn(`Post-create open failed for file=${file.toString()}: ${messageOf(ex)}`);
    }
    return { displayPath: displayPath(root, file) };
  }
}

/** The file a push acts on: an explicit URI (explorer, editor title, CodeLens), else the active editor. */
export function pushTargetFor(arg: unknown): PushTarget {
  const uri = arg instanceof vscode.Uri ? arg : vscode.window.activeTextEditor?.document.uri;
  if (!uri) return { extension: undefined, readText: () => Promise.resolve('') };
  const base = path.posix.basename(uri.path);
  const dot = base.lastIndexOf('.');
  return {
    extension: dot >= 0 ? base.substring(dot + 1) : '',
    async readText() {
      // Prefer the in-memory document so unsaved edits are included
      const open = vscode.workspace.textDocuments.find((d) => d.uri.toString() === uri.toString());
      if (open) return open.getText();
      return new TextDecoder('utf-8').decode(await vscode.workspace.fs.readFile(uri));
    },
  };
}
