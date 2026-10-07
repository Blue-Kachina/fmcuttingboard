/// <reference types="mocha" />
// Runs inside a real VS Code instance (see .vscode-test.mjs), against the built extension.
import * as assert from 'node:assert/strict';
import * as vscode from 'vscode';

const STEPS = '<fmxmlsnippet type="FMObjectList"><Step enable="True" id="1" name="Beep"/></fmxmlsnippet>';
const COMMANDS = [
  'getFileMakerClipboardContent',
  'pushClipboardIntoFileMaker',
  'readClipboardIntoNewXmlFile',
  'getFileMakerCalculationFromClipboard',
  'convertClipboardToXml',
  'dumpClipboardFormats',
  'saveRawClipboardCapture',
].map((c) => `fmcuttingboard.${c}`);

const root = () => vscode.workspace.workspaceFolders![0].uri;
const decode = (b: Uint8Array) => new TextDecoder().decode(b);

async function listCuttingBoard(): Promise<string[]> {
  try {
    return (await vscode.workspace.fs.readDirectory(vscode.Uri.joinPath(root(), '.fmCuttingBoard'))).map(([n]) => n).sort();
  } catch {
    return [];
  }
}

describe('FMCuttingBoard extension', () => {
  // These tests use the real clipboard; keep the user's text and put it back afterwards
  let savedClipboard = '';
  before(async () => {
    savedClipboard = await vscode.env.clipboard.readText();
  });
  after(async () => {
    await vscode.env.clipboard.writeText(savedClipboard);
  });

  it('registers every command', async () => {
    const ext = vscode.extensions.all.find((e) => e.packageJSON.name === 'fmcuttingboard');
    assert.ok(ext, 'extension found');
    await ext.activate();
    const all = await vscode.commands.getCommands(true);
    for (const c of COMMANDS) assert.ok(all.includes(c), `${c} registered`);
  });

  it('shows the "Send To FileMaker Clipboard" CodeLens on fmxmlsnippet XML files only', async () => {
    const xml = vscode.Uri.joinPath(root(), 'snippet.xml');
    const other = vscode.Uri.joinPath(root(), 'other.xml');
    await vscode.workspace.fs.writeFile(xml, new TextEncoder().encode(STEPS));
    await vscode.workspace.fs.writeFile(other, new TextEncoder().encode('<root/>'));
    // The CodeLens command needs the documents loaded
    await vscode.workspace.openTextDocument(xml);
    await vscode.workspace.openTextDocument(other);

    const lenses = await vscode.commands.executeCommand<vscode.CodeLens[]>('vscode.executeCodeLensProvider', xml);
    const ours = lenses.filter((l) => l.command?.command === 'fmcuttingboard.pushClipboardIntoFileMaker');
    assert.equal(ours.length, 1);
    assert.equal(ours[0].command!.title, 'FileMaker XML Detected: Send To FileMaker Clipboard');

    const none = await vscode.commands.executeCommand<vscode.CodeLens[]>('vscode.executeCodeLensProvider', other);
    assert.equal(none.filter((l) => l.command?.command === 'fmcuttingboard.pushClipboardIntoFileMaker').length, 0);
  });

  it('Push puts the active editor XML (including unsaved edits) on the clipboard', async () => {
    const xml = vscode.Uri.joinPath(root(), 'push.xml');
    await vscode.workspace.fs.writeFile(xml, new TextEncoder().encode('<fmxmlsnippet><Step id="9"/></fmxmlsnippet>'));
    const editor = await vscode.window.showTextDocument(xml);
    await editor.edit((e) => e.replace(new vscode.Range(0, 0, editor.document.lineCount, 0), `\n${STEPS}\n`));
    assert.ok(editor.document.isDirty);

    await vscode.commands.executeCommand('fmcuttingboard.pushClipboardIntoFileMaker');
    assert.equal(await vscode.env.clipboard.readText(), STEPS);
    await vscode.commands.executeCommand('workbench.action.revertAndCloseActiveEditor');
  });

  it('Get saves an fmxmlsnippet into .fmCuttingBoard (with its .gitignore) and opens it', async () => {
    const before = await listCuttingBoard();
    await vscode.env.clipboard.writeText(`copied: ${STEPS}`);
    await vscode.commands.executeCommand('fmcuttingboard.getFileMakerClipboardContent');

    const created = (await listCuttingBoard()).filter((n) => !before.includes(n));
    const xmlFiles = created.filter((n) => n.endsWith('.xml'));
    assert.equal(xmlFiles.length, 1, `one new .xml file (got ${created.join(', ')})`);
    assert.match(xmlFiles[0], /^\d+\.xml$/);
    const dir = vscode.Uri.joinPath(root(), '.fmCuttingBoard');
    assert.equal(decode(await vscode.workspace.fs.readFile(vscode.Uri.joinPath(dir, xmlFiles[0]))), STEPS);
    assert.equal(decode(await vscode.workspace.fs.readFile(vscode.Uri.joinPath(dir, '.gitignore'))), '*\n');
    assert.equal(vscode.window.activeTextEditor?.document.uri.path.endsWith(xmlFiles[0]), true, 'opened in editor');
    assert.equal(await vscode.env.clipboard.readText(), STEPS, 'clipboard replaced with the XML');
  });

  it('Get saves other text as a .fmcalc file', async () => {
    const before = await listCuttingBoard();
    await vscode.env.clipboard.writeText('Let ( x = 1 ; x + 1 )');
    await vscode.commands.executeCommand('fmcuttingboard.getFileMakerClipboardContent');

    const created = (await listCuttingBoard()).filter((n) => !before.includes(n));
    assert.equal(created.length, 1);
    assert.match(created[0], /^\d+\.fmcalc$/);
  });
});
