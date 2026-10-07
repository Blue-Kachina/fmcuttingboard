// .fmcalc completion, hover and signature help: thin adapters from src/core/CalcAssist.ts to the vscode API.
import * as vscode from 'vscode';
import { completions, hoverMarkdown, signatureAt, type CompletionKind } from '../core/CalcAssist';
import { cursorContext } from '../core/CallContext';

export const FMCALC: vscode.DocumentSelector = { language: 'fmcalc' };

const KINDS: Record<CompletionKind, vscode.CompletionItemKind> = {
  function: vscode.CompletionItemKind.Function,
  getConstant: vscode.CompletionItemKind.Constant,
  constant: vscode.CompletionItemKind.EnumMember,
};

const WORD = /\$\$?[\p{L}_~][\p{L}\p{N}_.~]*|[\p{L}_][\p{L}\p{N}_]*/u;

export function registerFmcalcProviders(context: vscode.ExtensionContext): void {
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
