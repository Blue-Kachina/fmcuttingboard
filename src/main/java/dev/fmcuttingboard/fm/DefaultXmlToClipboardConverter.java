package dev.fmcuttingboard.fm;

import java.util.EnumSet;

/**
 * Default implementation that validates fmxmlsnippet XML and returns a
 * normalized payload suitable to be placed on the clipboard for FileMaker.
 */
public class DefaultXmlToClipboardConverter implements XmlToClipboardConverter {

    private final FmXmlParser parser;

    public DefaultXmlToClipboardConverter() {
        this(new FmXmlParser());
    }

    public DefaultXmlToClipboardConverter(FmXmlParser parser) {
        this.parser = parser;
    }

    @Override
    public String convertToClipboardPayload(String fmxmlsnippetXml) throws ConversionException {
        ParsedSnippet model = parser.parse(fmxmlsnippetXml);
        EnumSet<ElementType> types = model.getElementTypes();

        // Phase 5.2 scope: support FIELDS and SCRIPTS; others are unsupported for now
        boolean isFields = types.contains(ElementType.FIELDS);
        boolean isScripts = types.contains(ElementType.SCRIPTS);
        boolean isLayouts = types.contains(ElementType.LAYOUTS);

        if (isLayouts && !isFields && !isScripts) {
            throw new ConversionException("Layout snippets are not supported yet.");
        }
        if (!isFields && !isScripts) {
            throw new ConversionException("Unsupported or unknown fmxmlsnippet type. Supported: Fields, Script Steps.");
        }

        // For supported types, returning the validated XML is sufficient for FileMaker paste
        return model.getRawXml();
    }
}
