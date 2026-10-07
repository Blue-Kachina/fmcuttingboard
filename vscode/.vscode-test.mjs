// Integration tests: run inside a real VS Code (downloaded into .vscode-test/) via @vscode/test-cli.
//   npm run test:integration
// Each run gets a fresh, empty temporary workspace folder so tests never write into the repo.
import { defineConfig } from '@vscode/test-cli';
import { mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

export default defineConfig({
  files: 'out/integration/**/*.test.js',
  // The oldest supported version (engines.vscode), so we never rely on newer APIs by accident
  version: '1.101.0',
  workspaceFolder: mkdtempSync(join(tmpdir(), 'fmcuttingboard-it-')),
  launchArgs: ['--disable-extensions'],
  mocha: { ui: 'bdd', timeout: 30_000 },
});
