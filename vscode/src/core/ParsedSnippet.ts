// Port of jetbrains/.../fm/ParsedSnippet.java: lightweight model of a parsed fmxmlsnippet.
import { ElementType, inDeclarationOrder } from './ElementType';
import { javaIsBlank } from './javaCompat';

export class ParsedSnippet {
  rawXml = '';
  version: string | null = null;
  typeHint: string | null = null;

  private readonly elementTypes = new Set<ElementType>();
  private readonly fieldNames: string[] = [];
  private readonly layoutNames: string[] = [];
  private readonly scriptNames: string[] = [];

  /** Element types in Java EnumSet order. */
  getElementTypes(): ElementType[] {
    return inDeclarationOrder(this.elementTypes);
  }

  addElementType(type: ElementType): void {
    this.elementTypes.add(type);
  }

  getFieldNames(): string[] {
    return [...this.fieldNames];
  }

  getLayoutNames(): string[] {
    return [...this.layoutNames];
  }

  getScriptNames(): string[] {
    return [...this.scriptNames];
  }

  addFieldName(name: string | null): void {
    if (name != null && !javaIsBlank(name)) {
      this.fieldNames.push(name);
      this.elementTypes.add('FIELDS');
    }
  }

  addLayoutName(name: string | null): void {
    if (name != null && !javaIsBlank(name)) {
      this.layoutNames.push(name);
      this.elementTypes.add('LAYOUTS');
    }
  }

  addScriptName(name: string | null): void {
    if (name != null && !javaIsBlank(name)) {
      this.scriptNames.push(name);
      this.elementTypes.add('SCRIPTS');
    }
  }
}
