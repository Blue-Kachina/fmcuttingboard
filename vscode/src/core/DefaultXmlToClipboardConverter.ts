// Port of jetbrains/.../fm/DefaultXmlToClipboardConverter.java: validates fmxmlsnippet XML before it is
// written to FileMaker's clipboard. The payload is the validated (trimmed) XML itself.
import { ConversionException } from './ConversionException';
import { FmXmlParser } from './FmXmlParser';

export class DefaultXmlToClipboardConverter {
  constructor(private readonly parser = new FmXmlParser()) {}

  /** @throws ConversionException if the XML is invalid or of no supported type */
  convertToClipboardPayload(fmxmlsnippetXml: string): string {
    const model = this.parser.parse(fmxmlsnippetXml);
    const types = model.getElementTypes();
    const supported = types.some((t) => t !== 'UNKNOWN');
    if (!supported) {
      throw new ConversionException(
        'Unsupported or unknown fmxmlsnippet type. Supported: Script/Steps, Scripts, Fields, Tables, Custom Functions, Value Lists, Layout Objects.',
      );
    }
    return model.rawXml;
  }
}
