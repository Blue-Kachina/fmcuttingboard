// Port of jetbrains/.../language/FileMakerFunctionRegistry.java (+ FunctionMetadata, FunctionParameter).
// The data lives in shared/data/filemaker-functions.json; add or correct functions there.
import data from '../../../shared/data/filemaker-functions.json';

export class FunctionParameter {
  constructor(
    readonly name: string,
    readonly type: string,
    readonly optional = false,
    readonly repeating = false,
  ) {}

  /** e.g. "number", "[optional]", "field..." */
  getDisplayText(): string {
    let s = this.optional ? '[' : '';
    s += this.name;
    if (this.repeating) s += '...';
    if (this.optional) s += ']';
    return s;
  }
}

export class FunctionMetadata {
  constructor(
    readonly name: string,
    readonly parameters: readonly FunctionParameter[],
    readonly category: string,
    readonly returnType = 'Any',
    readonly description = '',
  ) {}

  /** e.g. "If(test; resultTrue; [resultFalse])" */
  getSignature(): string {
    return `${this.name}(${this.parameters.map((p) => p.getDisplayText()).join('; ')})`;
  }

  /** e.g. "If(test; resultTrue; resultFalse)" */
  getSimpleSignature(): string {
    return `${this.name}(${this.parameters.map((p) => p.name).join('; ')})`;
  }
}

interface FunctionJson {
  name: string;
  category: string;
  returnType: string;
  description?: string;
  parameters: { name: string; type: string; optional?: boolean; repeating?: boolean }[];
}

/** Converts the shared filemaker-functions.json document into metadata objects, preserving file order. */
export function parse(root: { functions: FunctionJson[] }): FunctionMetadata[] {
  return root.functions.map(
    (f) =>
      new FunctionMetadata(
        f.name,
        f.parameters.map((p) => new FunctionParameter(p.name, p.type, p.optional ?? false, p.repeating ?? false)),
        f.category,
        f.returnType,
        f.description ?? '',
      ),
  );
}

const BY_NAME = new Map<string, FunctionMetadata>();
for (const m of parse(data)) BY_NAME.set(m.name.toLowerCase(), m);

export function getAll(): FunctionMetadata[] {
  return [...BY_NAME.values()];
}

/** True only when the shared data lists every FileMaker function, so an unknown name is really unknown. */
export function isComplete(): boolean {
  return data.complete;
}

export function size(): number {
  return BY_NAME.size;
}

/** Case-insensitive lookup. */
export function findByName(name: string): FunctionMetadata | undefined {
  return BY_NAME.get(name.toLowerCase());
}

export function getByCategory(category: string): FunctionMetadata[] {
  return getAll().filter((m) => m.category === category);
}

export function getFunctionNames(): string[] {
  return getAll().map((m) => m.name);
}
