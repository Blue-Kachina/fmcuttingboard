// Port of jetbrains/.../fm/FmSnippet.java: an extracted fmxmlsnippet plus a quick type heuristic.
import { ElementType, inDeclarationOrder } from './ElementType';
import { javaIsBlank } from './javaCompat';

export class FmSnippet {
  constructor(
    readonly xml: string,
    readonly elementTypes: readonly ElementType[],
  ) {}

  /** Very lightweight heuristic to infer common element groupings from the snippet. */
  static detectTypes(xml: string | null | undefined): ElementType[] {
    if (xml == null || javaIsBlank(xml)) return ['UNKNOWN'];
    const s = xml.toLowerCase();
    const set = new Set<ElementType>();
    if (s.includes('<field') || s.includes('<fielddefinition')) set.add('FIELDS');
    if (s.includes('<script') || s.includes('<step')) set.add('SCRIPTS');
    if (s.includes('<layout') || s.includes('layoutobjects') || s.includes('<object')) set.add('LAYOUTS');
    if (set.size === 0) set.add('UNKNOWN');
    return inDeclarationOrder(set);
  }
}
