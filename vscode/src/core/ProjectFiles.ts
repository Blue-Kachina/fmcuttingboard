// Port of the pure parts of jetbrains/.../fs/ProjectFiles.java. File system access is injected so this stays
// free of the vscode API (the extension supplies an implementation backed by vscode.workspace.fs).
import { javaIsBlank, replaceAllLiteral } from './javaCompat';

export const CUTTING_BOARD_DIR = '.fmCuttingBoard';
export const GITIGNORE = '.gitignore';
export const GITIGNORE_CONTENT = '*\n';

/**
 * Expands a settings file name pattern: `{timestamp}` becomes epoch millis, a blank pattern means
 * `{timestamp}`, and `extension` (e.g. ".xml") is appended unless the name already ends with it.
 */
export function resolveFileName(fileNamePattern: string | null | undefined, extension: string, nowMillis: number): string {
  const pattern = fileNamePattern == null || javaIsBlank(fileNamePattern) ? '{timestamp}' : fileNamePattern;
  const baseName = replaceAllLiteral(pattern, '{timestamp}', String(nowMillis));
  return baseName.endsWith(extension) ? baseName : baseName + extension;
}

/**
 * Returns `baseName` if it is free, otherwise the first free `name-N.ext` (N = 1, 2, …), with the suffix
 * inserted before the last dot.
 */
export async function uniqueFileName(baseName: string, exists: (name: string) => boolean | Promise<boolean>): Promise<string> {
  let candidate = baseName;
  let attempt = 0;
  while (await exists(candidate)) {
    attempt++;
    if (attempt > 1000) {
      throw new Error(`Unable to create a unique filename after 1000 attempts for baseName=${baseName}`);
    }
    const dot = baseName.lastIndexOf('.');
    candidate = dot > 0 ? `${baseName.substring(0, dot)}-${attempt}${baseName.substring(dot)}` : `${baseName}-${attempt}`;
  }
  return candidate;
}

/** The base directory name from settings, falling back to `.fmCuttingBoard`. */
export function baseDirNameOrDefault(baseDirName: string | null | undefined): string {
  return baseDirName == null || javaIsBlank(baseDirName) ? CUTTING_BOARD_DIR : baseDirName;
}
