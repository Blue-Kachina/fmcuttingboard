// .fmcalc completion, hover and signature help: thin adapters from src/core/CalcAssist.ts to the vscode API.
import * as vscode from 'vscode';
import { completions, hoverMarkdown, signatureAt, type CompletionKind } from '../core/CalcAssist';
import { diagnose, type Severity } from '../core/CalcDiagnostics';
import { foldRegions } from '../core/CalcFolding';
import { format } from '../core/CalcFormatter';
import { cursorContext } from '../core/CallContext';

export const FMCALC: vscode.DocumentSelector = { language: 'fmcalc' };

const KINDS: Record<CompletionKind, vscode.CompletionItemKind> = {
  function: vscode.CompletionItemKind.Function,
  getConstant: vscode.CompletionItemKind.Constant,
  constant: vscode.CompletionItemKind.EnumMember,
};

const WORD = /\$\$?[\p{L}_~][\p{L}\p{N}_.~]*|~[\p{L}\p{N}_.~]*|[\p{L}_][\p{L}\p{N}_]*/u;

// JetBrains severities → VS Code (a weak warning is subtler than a warning)
const SEVERITY: Record<Severity, vscode.DiagnosticSeverity> = {
  error: vscode.DiagnosticSeverity.Error,
  warning: vscode.DiagnosticSeverity.Warning,
  weak_warning: vscode.DiagnosticSeverity.Information,
};

/** Same checks as the JetBrains annotator (src/core/CalcDiagnostics.ts), updated as you type. */
function registerFmcalcDiagnostics(context: vscode.ExtensionContext): void {
  const collection = vscode.languages.createDiagnosticCollection('fmcalc');
  const pending = new Map<string, NodeJS.Timeout>();

  const refresh = (doc: vscode.TextDocument) => {
    if (doc.languageId !== 'fmcalc') return;
    collection.set(
      doc.uri,
      diagnose(doc.getText()).map((d) => {
        const diagnostic = new vscode.Diagnostic(
          new vscode.Range(doc.positionAt(d.start), doc.positionAt(d.end)),
          d.message,
          SEVERITY[d.severity],
        );
        diagnostic.source = 'FMCuttingBoard';
        return diagnostic;
      }),
    );
  };
  const refreshSoon = (doc: vscode.TextDocument) => {
    const key = doc.uri.toString();
    clearTimeout(pending.get(key));
    pending.set(key, setTimeout(() => {
      pending.delete(key);
      refresh(doc);
    }, 250));
  };

  vscode.workspace.textDocuments.forEach(refresh);
  context.subscriptions.push(
    collection,
    vscode.workspace.onDidOpenTextDocument(refresh),
    vscode.workspace.onDidChangeTextDocument((e) => refreshSoon(e.document)),
    vscode.workspace.onDidCloseTextDocument((doc) => {
      clearTimeout(pending.get(doc.uri.toString()));
      collection.delete(doc.uri);
    }),
    { dispose: () => pending.forEach((t) => clearTimeout(t)) },
  );
}

/** Format Document: the shared formatter (src/core/CalcFormatter.ts), with indentation from the editor's settings. */
function registerFmcalcFormatter(context: vscode.ExtensionContext): void {
  context.subscriptions.push(
    vscode.languages.registerDocumentFormattingEditProvider(FMCALC, {
      provideDocumentFormattingEdits(doc, editorOptions) {
        const config = vscode.workspace.getConfiguration('fmcuttingboard.format', doc.uri);
        const formatted = format(doc.getText(), {
          indentUnit: editorOptions.insertSpaces ? ' '.repeat(editorOptions.tabSize) : '\t',
          indentWidth: editorOptions.tabSize,
          maxWidth: config.get<number>('maxLineLength', 120),
          doNotIndentTopLetVariables: config.get<boolean>('doNotIndentTopLetVariables', true),
        });
        if (formatted === null) return []; // not well-formed: leave it alone (the problems are reported separately)
        const all = new vscode.Range(doc.positionAt(0), doc.positionAt(doc.getText().length));
        return formatted === doc.getText() ? [] : [vscode.TextEdit.replace(all, formatted)];
      },
    }),
  );
}

/** Folding regions shared with JetBrains (src/core/CalcFolding.ts); closing brackets stay visible. */
function registerFmcalcFolding(context: vscode.ExtensionContext): void {
  context.subscriptions.push(
    vscode.languages.registerFoldingRangeProvider(FMCALC, {
      provideFoldingRanges(doc) {
        const ranges: vscode.FoldingRange[] = [];
        for (const r of foldRegions(doc.getText())) {
          const start = doc.positionAt(r.start).line;
          const endPos = doc.positionAt(r.end);
          let end = endPos.line;
          // For brackets, keep the line with the closing bracket visible
          if (r.kind === 'region' && doc.lineAt(endPos.line).text.substring(0, endPos.character).trim() === '') end--;
          if (end > start) {
            ranges.push(new vscode.FoldingRange(start, end, r.kind === 'comment' ? vscode.FoldingRangeKind.Comment : undefined));
          }
        }
        return ranges;
      },
    }),
  );
}

export function registerFmcalcProviders(context: vscode.ExtensionContext): void {
  registerFmcalcDiagnostics(context);
  registerFmcalcFormatter(context);
  registerFmcalcFolding(context);
  context.subscriptions.push(
    vscode.languages.registerCompletionItemProvider(FMCALC, {
      provideCompletionItems(doc, position) {
        return completions(doc.getText(), doc.offsetAt(position)).map((e) => {
          const item = new vscode.CompletionItem({ label: e.label, detail: e.description ? `  ${e.description}` : undefined, description: e.detail }, KINDS[e.kind]);
          item.insertText = new vscode.SnippetString(e.insertSnippet);
          if (e.documentation) item.documentation = e.documentation;
          return item;
        });
      },
    }),

    vscode.languages.registerHoverProvider(FMCALC, {
      provideHover(doc, position) {
        const range = doc.getWordRangeAtPosition(position, WORD);
        if (!range) return undefined;
        const text = doc.getText();
        const after = text.substring(doc.offsetAt(range.end));
        const call = cursorContext(text, doc.offsetAt(range.start)).call;
        const md = hoverMarkdown(doc.getText(range), /^\s*\(/.test(after), call?.name.toLowerCase() === 'get');
        return md ? new vscode.Hover(new vscode.MarkdownString(md), range) : undefined;
      },
    }),

    vscode.languages.registerSignatureHelpProvider(
      FMCALC,
      {
        provideSignatureHelp(doc, position) {
          const sig = signatureAt(doc.getText(), doc.offsetAt(position));
          if (!sig) return undefined;
          const info = new vscode.SignatureInformation(sig.label, sig.documentation);
          // Parameter ranges as [start, end] offsets into the label, so repeated names resolve to the right one
          let from = sig.label.indexOf('(') + 1;
          info.parameters = sig.parameters.map((p) => {
            const start = sig.label.indexOf(p, from);
            from = start + p.length;
            return new vscode.ParameterInformation([start, start + p.length]);
          });
          const help = new vscode.SignatureHelp();
          help.signatures = [info];
          help.activeSignature = 0;
          help.activeParameter = sig.activeParameter;
          return help;
        },
      },
      { triggerCharacters: ['(', ';'], retriggerCharacters: [' '] },
    ),
  );
}
