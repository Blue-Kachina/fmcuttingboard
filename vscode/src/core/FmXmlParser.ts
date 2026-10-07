// Port of jetbrains/.../fm/FmXmlParser.java: validates fmxmlsnippet XML and maps it into a ParsedSnippet.
// Error messages are user-facing and must match the Java plugin (pinned by shared/fixtures/golden/cases.json).
import { DOMParser, type Element as XmlElement, type Document as XmlDocument } from '@xmldom/xmldom';
import { ConversionException } from './ConversionException';
import { ParsedSnippet } from './ParsedSnippet';
import { javaIsBlank, javaTrim } from './javaCompat';

const ELEMENT_NODE = 1;

export class FmXmlParser {
  /** @throws ConversionException when the XML is malformed or not an fmxmlsnippet */
  parse(xmlText: string | null | undefined): ParsedSnippet {
    if (xmlText == null || javaIsBlank(xmlText)) {
      throw new ConversionException('XML text is empty.');
    }

    const doc = toDocument(xmlText);
    const root = doc.documentElement;
    if (root == null) {
      throw new ConversionException('Failed to parse XML.');
    }
    if (root.tagName.toLowerCase() !== 'fmxmlsnippet') {
      throw new ConversionException('Root element is not <fmxmlsnippet>.');
    }

    const model = new ParsedSnippet();
    model.rawXml = javaTrim(xmlText);
    if (root.hasAttribute('version')) model.version = root.getAttribute('version');
    if (root.hasAttribute('type')) model.typeHint = root.getAttribute('type');

    walkElement(root, model);

    if (childElements(root).length === 0) {
      throw new ConversionException('<fmxmlsnippet> has no content elements.');
    }
    return model;
  }
}

// Characters XML 1.0 forbids anywhere in a document (Java's parser rejects them; xmldom doesn't)
const ILLEGAL_XML_CHARS = /[\u0000-\u0008\u000B\u000C\u000E-\u001F\uFFFE\uFFFF]|[\uD800-\uDBFF](?![\uDC00-\uDFFF])|(?<![\uD800-\uDBFF])[\uDC00-\uDFFF]/;
// Sections whose content is exempt from the '&' and ']]>' rules
const CDATA_COMMENTS_AND_PIS = /<!\[CDATA\[[\s\S]*?\]\]>|<!--[\s\S]*?-->|<\?[\s\S]*?\?>/g;
// '&' that does not start a well-formed entity or character reference
const BARE_AMPERSAND = /&(?!(?:[A-Za-z_:][A-Za-z0-9_.:-]*|#[0-9]+|#x[0-9A-Fa-f]+);)/;

/**
 * Well-formedness rules that xmldom does not enforce but Java's parser does. Without these, VS Code would
 * accept XML that the JetBrains plugin rejects (and that FileMaker may not paste).
 */
function assertStrictlyWellFormed(xml: string): void {
  if (ILLEGAL_XML_CHARS.test(xml)) throw new Error('Invalid XML character');
  const markupOnly = xml.replace(CDATA_COMMENTS_AND_PIS, '');
  if (BARE_AMPERSAND.test(markupOnly)) throw new Error("Unescaped '&'");
  if (markupOnly.includes(']]>')) throw new Error("']]>' is not allowed in content");
}

function toDocument(xml: string): XmlDocument {
  try {
    assertStrictlyWellFormed(xml);
    const parser = new DOMParser({
      onError: (_level: string, message: string) => {
        // Java's parser is strict: even xmldom's warnings (e.g. unquoted attributes) are failures
        throw new Error(message);
      },
    });
    const doc = parser.parseFromString(xml, 'text/xml');
    // Java disallows DOCTYPE declarations (protects against entity expansion attacks)
    if (doc.doctype != null) throw new Error('DOCTYPE is not allowed');
    return doc;
  } catch (ex) {
    throw new ConversionException('Failed to parse XML.', { cause: ex });
  }
}

function childElements(el: XmlElement): XmlElement[] {
  const out: XmlElement[] = [];
  for (let i = 0; i < el.childNodes.length; i++) {
    const n = el.childNodes[i];
    if (n.nodeType === ELEMENT_NODE) out.push(n as XmlElement);
  }
  return out;
}

function walkElement(el: XmlElement, model: ParsedSnippet): void {
  const lower = el.tagName.toLowerCase();
  if (lower === 'field' || lower === 'fielddefinition') {
    model.addFieldName(el.getAttribute('name'));
    // Even if name is missing, presence of a Field/FieldDefinition indicates a Fields snippet
    model.addElementType('FIELDS');
  } else if (lower === 'basetable') {
    model.addElementType('TABLES');
  } else if (
    lower === 'layout' ||
    lower === 'layoutobjectlist' ||
    lower === 'objectlist' ||
    lower === 'layoutobject' ||
    lower === 'object' ||
    lower === 'part'
  ) {
    model.addLayoutName(el.getAttribute('name'));
    model.addElementType('LAYOUTS');
  } else if (lower === 'script') {
    model.addScriptName(el.getAttribute('name'));
    model.addElementType('SCRIPTS');
  } else if (lower === 'step') {
    // Standalone Script Steps snippets may not include a <Script> wrapper
    model.addElementType('SCRIPTS');
  } else if (lower === 'customfunction') {
    model.addElementType('CUSTOM_FUNCTIONS');
  } else if (lower === 'valuelist') {
    model.addElementType('VALUE_LISTS');
  }

  for (const child of childElements(el)) {
    walkElement(child, model);
  }
}
