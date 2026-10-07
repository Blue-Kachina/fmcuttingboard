// The small set of host services the actions need. The extension implements these with the VS Code API
// (src/host/); tests use fakes. Keeping actions free of `vscode` lets their flows be unit-tested.
import type { ClipboardService, Logger } from '../clipboard/ClipboardService';

/** Mirrors the JetBrains plugin's settings (FmCuttingBoardSettingsState). */
export interface Settings {
  baseDirName: string;
  fileNamePattern: string;
  previewBeforeClipboardWrite: boolean;
  enableDiagnostics: boolean;
}

/** Mirrors the JetBrains Notifier: a title (logged; VS Code has no notification titles) and a message. */
export interface Notifier {
  info(title: string, message: string): void;
  warn(title: string, message: string): void;
  error(title: string, message: string, details?: unknown): void;
}

export interface Preview {
  /** Mirrors PreviewDialogs.confirmWrite: shows a (possibly truncated) preview; true to proceed. */
  confirmWrite(title: string, content: string, previewLimit: number): Promise<boolean>;
}

export interface CreatedFile {
  /** Path relative to the project root, as shown in notifications */
  displayPath: string;
}

export interface CuttingBoardFiles {
  /**
   * Creates `<project>/<baseDirName>/<name><extension>` (creating the folder and its `.gitignore` if needed),
   * writes `content`, and opens it in an editor.
   * @throws Error with a user-facing message when there is no project folder or the file cannot be written
   */
  createAndOpen(settings: Settings, extension: '.xml' | '.fmcalc', content: string): Promise<CreatedFile>;
}

/** The XML document a push acts on: the explorer selection or the active editor (unsaved edits included). */
export interface PushTarget {
  /** File extension without the dot, e.g. "xml"; undefined when nothing is selected or active */
  extension: string | undefined;
  readText(): Promise<string>;
}

export interface ActionContext {
  clipboard: ClipboardService;
  notifier: Notifier;
  preview: Preview;
  files: CuttingBoardFiles;
  settings(): Settings;
  log: Logger;
}

export const PREVIEW_LIMIT = 800;
