// Finds where the cursor is in a FileMaker calculation: inside a string or comment, and/or inside which function
// call and argument. A small scanner, not a parser: enough for completion and signature help.

export interface CallInfo {
  /** Function name as written */
  name: string;
  /** Zero-based argument index at the cursor (counts top-level ';' separators) */
  argIndex: number;
}

export interface CursorContext {
  inStringOrComment: boolean;
  /** Innermost enclosing named call, if any */
  call: CallInfo | undefined;
}

interface Frame {
  name: string | undefined; // undefined for grouping parens and [ ] binding lists
  argIndex: number;
}

const IDENT = /[\p{L}\p{N}_.]/u;

export function cursorContext(text: string, offset: number): CursorContext {
  const stack: Frame[] = [];
  let i = 0;
  const end = Math.min(offset, text.length);
  while (i < end) {
    const c = text[i];
    if (c === '"') {
      // String with \" escapes; unterminated means the cursor is inside it
      i++;
      while (i < text.length && text[i] !== '"') i += text[i] === '\\' ? 2 : 1;
      if (i >= end) return { inStringOrComment: true, call: innermostCall(stack) };
      i++;
    } else if (c === '/' && text[i + 1] === '/') {
      const eol = text.indexOf('\n', i);
      if (eol === -1 || eol >= end) return { inStringOrComment: true, call: innermostCall(stack) };
      i = eol + 1;
    } else if (c === '/' && text[i + 1] === '*') {
      const close = text.indexOf('*/', i + 2);
      if (close === -1 || close + 2 > end) return { inStringOrComment: true, call: innermostCall(stack) };
      i = close + 2;
    } else if (c === '(') {
      stack.push({ name: identifierBefore(text, i), argIndex: 0 });
      i++;
    } else if (c === '[') {
      stack.push({ name: undefined, argIndex: 0 });
      i++;
    } else if (c === ')' || c === ']') {
      stack.pop();
      i++;
    } else if (c === ';') {
      if (stack.length > 0) stack[stack.length - 1].argIndex++;
      i++;
    } else {
      i++;
    }
  }
  return { inStringOrComment: false, call: innermostCall(stack) };
}

/** The nearest named call, but only through grouping parens; a [ ] list belongs to its call's current argument. */
function innermostCall(stack: Frame[]): CallInfo | undefined {
  for (let k = stack.length - 1; k >= 0; k--) {
    const f = stack[k];
    if (f.name !== undefined) return { name: f.name, argIndex: f.argIndex };
  }
  return undefined;
}

function identifierBefore(text: string, parenIndex: number): string | undefined {
  let j = parenIndex - 1;
  while (j >= 0 && /\s/.test(text[j])) j--;
  const endIdx = j + 1;
  while (j >= 0 && IDENT.test(text[j])) j--;
  const name = text.substring(j + 1, endIdx);
  return /^[\p{L}_]/u.test(name) && text[j] !== '$' ? name : undefined;
}

/**
 * Maps an argument index onto the parameter to highlight. Arguments past a repeating parameter keep
 * pointing at it (e.g. Sum's "field..."), cycling through a repeating group (Case's test, result, test, ...),
 * and the last parameter absorbs any extra arguments. Mirrors FileMakerCalculationParameterInfoHandler.java.
 */
export function activeParameterIndex(parameters: readonly { repeating: boolean; group?: string }[], argIndex: number): number {
  for (let p = 0; p < parameters.length; p++) {
    if (p === argIndex) return p;
    if (parameters[p].repeating && argIndex > p) {
      let size = 1;
      const group = parameters[p].group;
      while (group && p + size < parameters.length && parameters[p + size].repeating && parameters[p + size].group === group) size++;
      return p + ((argIndex - p) % size);
    }
  }
  return Math.max(0, parameters.length - 1);
}
