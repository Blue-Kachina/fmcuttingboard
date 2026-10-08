// Completion, hover and signature-help content for .fmcalc, independent of the vscode API (the extension maps these
// onto CompletionItem, Hover and SignatureHelp). Completion mirrors the JetBrains
// FileMakerCalculationCompletionContributor: same items, type text and inserted template.
import { constants, findConstant, findFunction, functions, getConstantNames, isGetConstant } from './CalcLanguage';
import { activeParameterIndex, cursorContext } from './CallContext';
import type { FunctionMetadata } from './FileMakerFunctionRegistry';

export type CompletionKind = 'function' | 'getConstant' | 'constant';

export interface CompletionEntry {
  label: string;
  kind: CompletionKind;
  /** JetBrains "type text", e.g. "Text → Text" */
  detail: string;
  /** JetBrains "tail text", e.g. "Left(text; count)" */
  description?: string;
  /** VS Code snippet syntax */
  insertSnippet: string;
  documentation?: string;
}

function escapeSnippet(s: string): string {
  return s.replace(/[$}\\]/g, '\\$&');
}

/** Name(${1:p1}; ${2:p2}), with [] and ... removed from placeholders, like the JetBrains insert template. */
export function functionSnippet(meta: FunctionMetadata): string {
  const params = meta.parameters.map((p, i) => {
    const placeholder = p.name.replace(/[[\]]/g, '').replace(/\.\.\./g, '');
    return `\${${i + 1}:${escapeSnippet(placeholder)}}`;
  });
  return `${escapeSnippet(meta.name)}(${params.join('; ')})`;
}

export function completions(text: string, offset: number): CompletionEntry[] {
  const ctx = cursorContext(text, offset);
  if (ctx.inStringOrComment) return [];

  const entries: CompletionEntry[] = functions().map((meta) => ({
    label: meta.name,
    kind: 'function',
    detail: `${meta.category}${meta.returnType ? ` → ${meta.returnType}` : ''}`,
    description: meta.getSimpleSignature(),
    insertSnippet: functionSnippet(meta),
    documentation: meta.description || undefined,
  }));

  if (ctx.call && ctx.call.name.toLowerCase() === 'get') {
    for (const name of getConstantNames()) {
      entries.push({ label: name, kind: 'getConstant', detail: 'Get() constant', insertSnippet: escapeSnippet(name) });
    }
  }

  for (const c of constants()) {
    entries.push({ label: c.name, kind: 'constant', detail: `${c.group} constant`, insertSnippet: escapeSnippet(c.name) });
  }
  return entries;
}

/** Markdown for hovering `word`; `followedByParen` distinguishes calls from same-named fields. */
export function hoverMarkdown(word: string, followedByParen: boolean, insideGetCall: boolean): string | undefined {
  if (insideGetCall && isGetConstant(word)) {
    return `**${word}**: \`Get ( ${word} )\` constant`;
  }
  const meta = followedByParen ? findFunction(word) : undefined;
  if (meta) {
    const lines = ['```fmcalc', meta.getSignature(), '```'];
    if (meta.description) lines.push('', escapeMarkdown(meta.description));
    lines.push('', `*${meta.category}* · returns *${meta.returnType}*`);
    if (meta.helpUrl) lines.push('', `[Open Claris help](${meta.helpUrl})`);
    return lines.join('\n');
  }
  const constant = findConstant(word);
  if (constant) {
    const value = constant.value == null ? '' : ` = \`${constant.value}\``;
    return `**${constant.name}**: ${constant.group} constant${value}`;
  }
  return undefined;
}

function escapeMarkdown(s: string): string {
  return s.replace(/[\\`*_{}[\]<>#|]/g, '\\$&');
}

export interface SignatureInfo {
  label: string;
  parameters: string[];
  activeParameter: number;
  documentation?: string;
}

/** Signature help for the call enclosing `offset`, if it is a known function. */
export function signatureAt(text: string, offset: number): SignatureInfo | undefined {
  const ctx = cursorContext(text, offset);
  if (ctx.inStringOrComment || !ctx.call) return undefined;
  const meta = findFunction(ctx.call.name);
  if (!meta) return undefined;
  return {
    label: meta.getSignature(),
    parameters: meta.parameters.map((p) => p.getDisplayText()),
    activeParameter: activeParameterIndex(meta.parameters, ctx.call.argIndex),
    documentation: meta.description || undefined,
  };
}
