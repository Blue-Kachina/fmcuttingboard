package dev.fmcuttingboard.fm;

/**
 * Converts an fmxmlsnippet XML string into a payload suitable for placing on the
 * system clipboard so that FileMaker will accept it on paste.
 */
public interface XmlToClipboardConverter {
    /**
     * Validates and normalizes the input fmxmlsnippet XML and returns the text payload
     * to be put on the clipboard. Implementations may throw {@link ConversionException}
     * when the XML is malformed or when the snippet type is not supported.
     */
    String convertToClipboardPayload(String fmxmlsnippetXml) throws ConversionException;
}
