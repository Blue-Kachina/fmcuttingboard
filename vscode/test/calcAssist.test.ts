import { describe, expect, it } from 'vitest';
import { completions, functionSnippet, hoverMarkdown, signatureAt } from '../src/core/CalcAssist';
import { activeParameterIndex, cursorContext } from '../src/core/CallContext';
import { findFunction } from '../src/core/CalcLanguage';

/** Text before the `|` marker, and its offset. */
function at(marked: string): [string, number] {
  const offset = marked.indexOf('|');
  return [marked.replace('|', ''), offset];
}

describe('cursorContext', () => {
  it('finds the enclosing call and argument index', () => {
    expect(cursorContext(...at('Left ( "abc" ; |')).call).toEqual({ name: 'Left', argIndex: 1 });
    expect(cursorContext(...at('If ( Left ( x ; 1 ) = "a" ; |')).call).toEqual({ name: 'If', argIndex: 1 });
    expect(cursorContext(...at('Get ( |')).call).toEqual({ name: 'Get', argIndex: 0 });
  });

  it('ignores separators and parens inside strings and comments', () => {
    expect(cursorContext(...at('Left ( "a ; b ) c" ; /* ; ) */ |')).call).toEqual({ name: 'Left', argIndex: 1 });
  });

  it('keeps Let bindings in the first argument', () => {
    expect(cursorContext(...at('Let ( [ a = 1 ; b = 2 ; |')).call).toEqual({ name: 'Let', argIndex: 0 });
    expect(cursorContext(...at('Let ( [ a = 1 ; b = 2 ] ; |')).call).toEqual({ name: 'Let', argIndex: 1 });
  });

  it('looks through grouping parentheses', () => {
    expect(cursorContext(...at('Round ( ( a + |')).call).toEqual({ name: 'Round', argIndex: 0 });
  });

  it('reports strings and comments', () => {
    expect(cursorContext(...at('Left ( "ab|')).inStringOrComment).toBe(true);
    expect(cursorContext(...at('1 // note |')).inStringOrComment).toBe(true);
    expect(cursorContext(...at('/* open |')).inStringOrComment).toBe(true);
    expect(cursorContext(...at('"done" & |')).inStringOrComment).toBe(false);
  });

  it('does not treat variables as function names', () => {
    expect(cursorContext(...at('$x ( |')).call).toBeUndefined();
  });
});

describe('activeParameterIndex', () => {
  const sum = [{ repeating: false }, { repeating: true }];
  it('stays on a repeating parameter for extra arguments', () => {
    expect(activeParameterIndex(sum, 0)).toBe(0);
    expect(activeParameterIndex(sum, 1)).toBe(1);
    expect(activeParameterIndex(sum, 5)).toBe(1);
  });
  it('clamps to the last parameter', () => {
    expect(activeParameterIndex([{ repeating: false }, { repeating: false }], 4)).toBe(1);
  });
  it('cycles through a repeating group', () => {
    const jsonSet = [{ repeating: false }, ...['key', 'value', 'type'].map(() => ({ repeating: true, group: 'element' }))];
    expect([1, 2, 3, 4, 5, 6, 7].map((i) => activeParameterIndex(jsonSet, i))).toEqual([1, 2, 3, 1, 2, 3, 1]);
  });
});

describe('completions (mirrors the JetBrains completion contributor)', () => {
  it('inserts the same template as JetBrains: Name(p1; p2) without [] or ...', () => {
    expect(functionSnippet(findFunction('If')!)).toBe('If(${1:test}; ${2:result1}; ${3:result2})');
    expect(functionSnippet(findFunction('Sum')!)).toBe('Sum(${1:field})');
  });

  it('labels functions with category → return type and the simple signature', () => {
    const left = completions(...at('|')).find((c) => c.label === 'Left')!;
    expect(left).toMatchObject({ kind: 'function', detail: 'Text → Text', description: 'Left(text; numberOfCharacters)' });
  });

  it('offers Get() constants only inside Get ( … )', () => {
    expect(completions(...at('Get ( |')).some((c) => c.label === 'AccountName' && c.kind === 'getConstant')).toBe(true);
    expect(completions(...at('Left ( |')).some((c) => c.kind === 'getConstant')).toBe(false);
  });

  it('offers nothing inside strings or comments', () => {
    expect(completions(...at('"text |'))).toEqual([]);
    expect(completions(...at('// |'))).toEqual([]);
  });
});

describe('hover', () => {
  it('shows a function signature and category for calls only', () => {
    expect(hoverMarkdown('if', true, false)).toBe(
      '```fmcalc\nIf(test; result1; [result2])\n```\n\n*Logical* · returns *Any*\n\n' +
        '[Open Claris help](https://help.claris.com/en/pro-help/content/if-function.html)',
    );
    expect(hoverMarkdown('Left', false, false)).toBeUndefined();
  });

  it('describes Get constants and named constants', () => {
    expect(hoverMarkdown('AccountName', false, true)).toBe('**AccountName**: `Get ( AccountName )` constant');
    expect(hoverMarkdown('JSONBoolean', false, false)).toBe('**JSONBoolean**: json-type constant = `5`');
    expect(hoverMarkdown('Bold', false, false)).toBe('**Bold**: text-style constant');
  });
});

describe('signature help', () => {
  it('highlights the current parameter', () => {
    expect(signatureAt(...at('If ( a > 1 ; "x" ; |'))).toMatchObject({
      label: 'If(test; result1; [result2])',
      parameters: ['test', 'result1', '[result2]'],
      activeParameter: 2,
    });
  });

  it('is absent for unknown functions and outside calls', () => {
    expect(signatureAt(...at('MyCustomFunction ( |'))).toBeUndefined();
    expect(signatureAt(...at('1 + |'))).toBeUndefined();
  });
});

describe('Unicode names', () => {
  it('finds calls to functions with non-ASCII names', () => {
    expect(cursorContext('自作関数 ( 1 ; |'.replace('|', ''), '自作関数 ( 1 ; '.length).call).toEqual({ name: '自作関数', argIndex: 1 });
  });
});
