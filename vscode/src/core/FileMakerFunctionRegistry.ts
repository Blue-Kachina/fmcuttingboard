// Port of jetbrains/.../language/FileMakerFunctionRegistry.java (+ FunctionMetadata, FunctionParameter).
// The data is shared/data/fm-calc-catalogue.json, vendored from fmscriptinventory by
// shared/tools/sync-calc-catalogue.mjs (docs/fm-calc-catalogue-contract.md). Never edit it by hand.
import data from '../../../shared/data/fm-calc-catalogue.json';

export class FunctionParameter {
  constructor(
    readonly name: string,
    readonly type: string,
    readonly optional = false,
    readonly repeating = false,
    /** Parameters sharing a group repeat together, e.g. Case's test/result pairs */
    readonly group?: string,
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
    readonly minArgs = 0,
    /** null = unlimited */
    readonly maxArgs: number | null = null,
    readonly helpUrl?: string,
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

interface CatalogueJson {
  categories: { key: string; label: string }[];
  functions: {
    name: string;
    category: string;
    returnType: string;
    minArgs: number;
    maxArgs: number | null;
    helpUrl: string;
    summary?: string;
    parameters: { name: string; type: string; optional: boolean; repeatable: boolean; group?: string }[];
  }[];
}

// Display names for the catalogue's data types. Must match FileMakerFunctionRegistry.java and
// shared/tools/generate-function-signatures.mjs, which writes the shared baseline.
const TYPE_LABELS: Record<string, string> = {
  text: 'Text',
  number: 'Number',
  date: 'Date',
  time: 'Time',
  timestamp: 'Timestamp',
  container: 'Container',
  boolean: 'Boolean',
  json: 'JSON',
  any: 'Any',
  expression: 'Expression',
  fieldReference: 'Field',
  variableBindings: 'Bindings',
};

export function typeLabel(type: string): string {
  return TYPE_LABELS[type] ?? type;
}

/** "Text functions" -> "Text" */
export function categoryLabel(label: string): string {
  return label.replace(/ functions$/i, '');
}

/** Converts the vendored catalogue into metadata objects, preserving its order (sorted by name). */
export function parse(root: CatalogueJson): FunctionMetadata[] {
  const categories = new Map(root.categories.map((c) => [c.key, categoryLabel(c.label)]));
  return root.functions.map(
    (f) =>
      new FunctionMetadata(
        f.name,
        f.parameters.map((p) => new FunctionParameter(p.name, typeLabel(p.type), p.optional, p.repeatable, p.group)),
        categories.get(f.category) ?? f.category,
        typeLabel(f.returnType),
        f.summary ?? '',
        f.minArgs,
        f.maxArgs,
        f.helpUrl,
      ),
  );
}

const BY_NAME = new Map<string, FunctionMetadata>();
for (const m of parse(data as unknown as CatalogueJson)) BY_NAME.set(m.name.toLowerCase(), m);

export function getAll(): FunctionMetadata[] {
  return [...BY_NAME.values()];
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
