// Port of jetbrains/.../fm/ElementType.java. Array order = Java enum declaration order, which is also the
// iteration order of Java's EnumSet; sets are always reported in this order.
export const ELEMENT_TYPES = [
  'FIELDS',
  'SCRIPTS',
  'TABLES',
  'LAYOUTS',
  'CUSTOM_FUNCTIONS',
  'VALUE_LISTS',
  'UNKNOWN',
] as const;
export type ElementType = (typeof ELEMENT_TYPES)[number];

/** Returns the members of `set` in declaration order (like iterating a Java EnumSet). */
export function inDeclarationOrder(set: ReadonlySet<ElementType>): ElementType[] {
  return ELEMENT_TYPES.filter((t) => set.has(t));
}
