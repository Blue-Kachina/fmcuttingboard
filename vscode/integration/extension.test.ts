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

  describe('.fmcalc language support', () => {
    let doc: vscode.TextDocument;
    const source = 'If ( Left ( $name ; 1 ) = "A" ; Get ( AccountName ) ; ';

    before(async () => {
      const uri = vscode.Uri.joinPath(root(), 'calc.fmcalc');
      await vscode.workspace.fs.writeFile(uri, new TextEncoder().encode(source));
      doc = await vscode.workspace.openTextDocument(uri);
      await vscode.window.showTextDocument(doc);
    });

    it('recognizes .fmcalc files', () => {
      assert.equal(doc.languageId, 'fmcalc');
    });

    it('completes functions with the JetBrains-style template', async () => {
      const list = await vscode.commands.executeCommand<vscode.CompletionList>(
        'vscode.executeCompletionItemProvider', doc.uri, doc.positionAt(source.length));
      const left = list.items.find((i) => (typeof i.label === 'string' ? i.label : i.label.label) === 'Left');
      assert.ok(left, 'Left offered');
      assert.equal((left.insertText as vscode.SnippetString).value, 'Left(${1:text}; ${2:numberOfCharacters})');
    });

    it('offers Get() constants inside Get ( … )', async () => {
      const offset = source.indexOf('AccountName');
      const list = await vscode.commands.executeCommand<vscode.CompletionList>(
        'vscode.executeCompletionItemProvider', doc.uri, doc.positionAt(offset));
      assert.ok(list.items.some((i) => (typeof i.label === 'string' ? i.label : i.label.label) === 'LastError'));
    });

    it('shows a hover with the signature', async () => {
      const hovers = await vscode.commands.executeCommand<vscode.Hover[]>(
        'vscode.executeHoverProvider', doc.uri, doc.positionAt(source.indexOf('Left') + 1));
      const text = hovers.flatMap((h) => h.contents.map((c) => (c as vscode.MarkdownString).value)).join('\n');
      assert.match(text, /Left\(text; numberOfCharacters\)/);
    });

    it('reports the same diagnostics as the JetBrains annotator', async () => {
      const uri = vscode.Uri.joinPath(root(), 'problems.fmcalc');
      const calc = 'Let ( [ total = Left ( x ) ] ; total ) + total';
      await vscode.workspace.fs.writeFile(uri, new TextEncoder().encode(calc));
      const problems = await vscode.workspace.openTextDocument(uri);
      let diagnostics: vscode.Diagnostic[] = [];
      for (let i = 0; i < 50 && diagnostics.length < 2; i++) {
        await new Promise((r) => setTimeout(r, 100));
        diagnostics = vscode.languages.getDiagnostics(problems.uri);
      }
      const summary = diagnostics
        .map((d) => `${vscode.DiagnosticSeverity[d.severity]} ${problems.offsetAt(d.range.start)} ${d.message}`)
        .sort();
      assert.deepEqual(summary, [
        'Error 16 Too few arguments for Left: expected at least 2, got 1',
        "Information 41 'total' is used outside the Let() that defines it (here it refers to a field)",
      ]);
    });

    it('formats the document (Claris spacing, top Let variables flush, tabs from the editor)', async () => {
      const uri = vscode.Uri.joinPath(root(), 'format.fmcalc');
      await vscode.workspace.fs.writeFile(uri, new TextEncoder().encode('Let([a=1;b=2];If(a>b;"x";"y"))'));
      const editor = await vscode.window.showTextDocument(uri);
      editor.options = { insertSpaces: false, tabSize: 4 };
      await vscode.commands.executeCommand('editor.action.formatDocument');
      // The editor applies its own line endings (CRLF for a new file on Windows)
      assert.equal(editor.document.getText().replace(/\r\n/g, '\n'), 'Let ( [\na = 1 ;\nb = 2\n] ;\n\tIf ( a > b ; "x" ; "y" )\n)');
      await vscode.commands.executeCommand('workbench.action.revertAndCloseActiveEditor');
    });

    it('folds multi-line Let/Case/If/While calls and lists, keeping closing brackets visible', async () => {
      const uri = vscode.Uri.joinPath(root(), 'fold.fmcalc');
      await vscode.workspace.fs.writeFile(uri, new TextEncoder().encode('Let ( [\na = 1 ;\nb = 2\n] ;\n\ta + b\n)'));
      const doc = await vscode.workspace.openTextDocument(uri);
      const ranges = await vscode.commands.executeCommand<vscode.FoldingRange[]>('vscode.executeFoldingRangeProvider', doc.uri);
      // VS Code keeps one fold per start line (the outer one), so the [ list on the Let line is not a separate fold
      assert.deepEqual(ranges.map((r) => [r.start, r.end]), [[0, 4]]);
    });

    it('shows signature help for the current argument', async () => {
      const help = await vscode.commands.executeCommand<vscode.SignatureHelp>(
        'vscode.executeSignatureHelpProvider', doc.uri, doc.positionAt(source.length));
      assert.equal(help.signatures[0].label, 'If(test; result1; [result2])');
      assert.equal(help.activeParameter, 2);
    });
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
