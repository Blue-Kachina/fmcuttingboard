import * as os from 'node:os';
import * as vscode from 'vscode';
import {
  convertClipboardToXml,
  getFileMakerCalculationFromClipboard,
  getFileMakerClipboardContent,
  pushClipboardIntoFileMaker,
  readClipboardIntoNewXmlFile,
} from './actions/actions';
import type { ActionContext } from './actions/ports';
import { DefaultClipboardService, messageOf } from './clipboard/DefaultClipboardService';
import { PowerShellClipboardBridge, type NativeClipboardBridge } from './clipboard/PowerShellClipboardBridge';
import { analyzeBytesSection } from './core/ClipboardFormatAnalysis';
import { registerFmcalcProviders } from './language/fmcalcProviders';
import { buildCapture } from './core/RawClipboardCapture';
import {
  ensureBaseDir,
  pushTargetFor,
  readSettings,
  SECTION,
  VsCodeCuttingBoardFiles,
  VsCodeLogger,
  VsCodeNotifier,
  VsCodePreview,
  VsCodeTextClipboard,
} from './host/VsCodeHost';

const FMXML_CONTEXT_KEY = 'fmcuttingboard.activeEditorIsFmxmlsnippet';

function isFmxmlsnippetXml(doc: vscode.TextDocument): boolean {
  return doc.uri.path.toLowerCase().endsWith('.xml') && doc.getText().toLowerCase().includes('<fmxmlsnippet');
}

export function activate(context: vscode.ExtensionContext): void {
  const channel = vscode.window.createOutputChannel('FMCuttingBoard', { log: true });
  const log = new VsCodeLogger(channel);
  const notifier = new VsCodeNotifier(log);

  // The extension runs on the UI side (extensionKind: ui), so this is the user's own machine
  const bridge =
    process.platform === 'win32'
      ? new PowerShellClipboardBridge(vscode.Uri.joinPath(context.extensionUri, 'resources', 'fmclipboard.ps1').fsPath)
      : undefined;

  const ctx: ActionContext = {
    clipboard: new DefaultClipboardService(new VsCodeTextClipboard(), bridge, log),
    notifier,
    preview: new VsCodePreview(),
    files: new VsCodeCuttingBoardFiles(log),
    settings: readSettings,
    log,
  };

  const command = (id: string, run: (...args: unknown[]) => Promise<void>) =>
    vscode.commands.registerCommand(`${SECTION}.${id}`, async (...args: unknown[]) => {
      try {
        await run(...args);
      } catch (ex) {
        notifier.error('FMCuttingBoard', `Unexpected error: ${messageOf(ex)}`, ex);
      }
    });

  context.subscriptions.push(
    channel,
    command('getFileMakerClipboardContent', () => getFileMakerClipboardContent(ctx)),
    command('pushClipboardIntoFileMaker', (arg) => pushClipboardIntoFileMaker(ctx, pushTargetFor(arg))),
    command('readClipboardIntoNewXmlFile', () => readClipboardIntoNewXmlFile(ctx)),
    command('getFileMakerCalculationFromClipboard', () => getFileMakerCalculationFromClipboard(ctx)),
    command('convertClipboardToXml', () => convertClipboardToXml(ctx)),
    command('dumpClipboardFormats', () => dumpClipboardFormats(bridge, log, notifier)),
    command('saveRawClipboardCapture', () => saveRawClipboardCapture(bridge, log, notifier)),
  );

  registerFmcalcProviders(context);

  // "FileMaker XML Detected" prompt (the JetBrains editor banner): a CodeLens plus an editor title button
  context.subscriptions.push(
    vscode.languages.registerCodeLensProvider(
      { pattern: '**/*.xml' },
      {
        provideCodeLenses(doc) {
          if (!isFmxmlsnippetXml(doc)) return [];
          return [
            new vscode.CodeLens(new vscode.Range(0, 0, 0, 0), {
              title: 'FileMaker XML Detected: Send To FileMaker Clipboard',
              command: `${SECTION}.pushClipboardIntoFileMaker`,
              arguments: [doc.uri],
            }),
          ];
        },
      },
    ),
  );

  const updateContextKey = () => {
    const doc = vscode.window.activeTextEditor?.document;
    void vscode.commands.executeCommand('setContext', FMXML_CONTEXT_KEY, doc != null && isFmxmlsnippetXml(doc));
  };
  updateContextKey();
  context.subscriptions.push(
    vscode.window.onDidChangeActiveTextEditor(updateContextKey),
    vscode.workspace.onDidChangeTextDocument((e) => {
      if (e.document === vscode.window.activeTextEditor?.document) updateContextKey();
    }),
  );
}

export function deactivate(): void {}

// ----- Diagnostics (visible when fmcuttingboard.enableDiagnostics is on) -----

async function dumpClipboardFormats(
  bridge: NativeClipboardBridge | undefined,
  log: VsCodeLogger,
  notifier: VsCodeNotifier,
): Promise<void> {
  if (!bridge) {
    log.info('[CB-DUMP] Not a Windows OS; skipping clipboard dump');
    notifier.warn('Dump Clipboard Formats', 'Dumping clipboard formats is only available on Windows.');
    return;
  }
  const result = await bridge.dump();
  if (result.formats.length === 0) log.info('[CB-DUMP] No clipboard formats enumerated');
  for (const f of result.formats) {
    log.info(`[CB-DUMP] format id=${f.id ?? '?'}, name='${f.name}', size=${f.bytes.length}`);
    log.info(analyzeBytesSection(f.id, f.name, f.bytes));
  }
  log.channel.show(true);
  notifier.info('Dump Clipboard Formats', `Logged ${result.formats.length} clipboard formats to the FMCuttingBoard output.`);
}

async function saveRawClipboardCapture(
  bridge: NativeClipboardBridge | undefined,
  log: VsCodeLogger,
  notifier: VsCodeNotifier,
): Promise<void> {
  const title = 'FMCuttingBoard';
  if (!bridge) {
    notifier.warn(title, 'Raw clipboard capture is only available on Windows.');
    return;
  }
  let formats;
  try {
    formats = (await bridge.dump()).formats;
  } catch (ex) {
    notifier.error(title, 'Reading the clipboard failed.', ex);
    return;
  }
  if (formats.length === 0) {
    notifier.info(title, 'The clipboard is empty.');
    return;
  }
  try {
    const capture = buildCapture(
      formats.map((f) => ({ id: f.id ?? 0, name: f.name, bytes: f.bytes })),
      new Date(),
      'FMCuttingBoard for VS Code',
      `${os.type()} ${os.release()}`,
    );
    const { dir } = await ensureBaseDir(readSettings());
    const target = vscode.Uri.joinPath(dir, 'captures', capture.folderName);
    await vscode.workspace.fs.createDirectory(target);
    for (const f of capture.files) await vscode.workspace.fs.writeFile(vscode.Uri.joinPath(target, f.fileName), f.bytes);
    const where = target.scheme === 'file' ? target.fsPath : target.toString();
    log.info(`[CB-CAPTURE] Saved ${formats.length} clipboard formats to ${where}`);
    notifier.info(title, `Saved ${formats.length} clipboard formats to ${where}. See shared/fixtures/README.md before adding it to the repo.`);
  } catch (ex) {
    notifier.error(title, 'Saving the clipboard capture failed.', ex);
  }
}
