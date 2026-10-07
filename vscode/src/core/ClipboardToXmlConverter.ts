// Port of jetbrains/.../fm/ClipboardToXmlConverter.java: clipboard text → fmxmlsnippet XML.
import { ConversionException } from './ConversionException';
import { DefaultFileMakerClipboardParser } from './DefaultFileMakerClipboardParser';
import { FmSnippet } from './FmSnippet';

export class ClipboardToXmlConverter {
  constructor(private readonly parser = new DefaultFileMakerClipboardParser()) {}

  /** @throws ConversionException if the text holds no fmxmlsnippet */
  convert(clipboardText: string | null | undefined): FmSnippet {
    let xml: string | null;
    try {
      xml = this.parser.normalizeToXmlText(clipboardText);
    } catch (ex) {
      throw new ConversionException('Unexpected error during conversion.', { cause: ex });
    }
    if (xml == null) {
      throw new ConversionException('Clipboard does not contain a recognizable FileMaker fmxmlsnippet.');
    }
    return new FmSnippet(xml, FmSnippet.detectTypes(xml));
  }

  convertToXml(clipboardText: string | null | undefined): string {
    return this.convert(clipboardText).xml;
  }
}
