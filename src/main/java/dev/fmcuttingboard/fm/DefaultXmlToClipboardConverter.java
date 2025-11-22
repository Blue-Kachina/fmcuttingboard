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

        // Phase 3.3/3.4 update: support FIELDS, SCRIPTS, TABLES, CUSTOM FUNCTIONS, VALUE LISTS, and LAYOUT OBJECTS
        boolean isFields = types.contains(ElementType.FIELDS);
        boolean isScripts = types.contains(ElementType.SCRIPTS);
        boolean isTables = types.contains(ElementType.TABLES);
        boolean isLayouts = types.contains(ElementType.LAYOUTS);
        boolean isCustomFunctions = types.contains(ElementType.CUSTOM_FUNCTIONS);
        boolean isValueLists = types.contains(ElementType.VALUE_LISTS);

        // Accept if any known type is present (including pure Layout Object snippets)
        if (!isFields && !isScripts && !isTables && !isCustomFunctions && !isValueLists && !isLayouts) {
            throw new ConversionException("Unsupported or unknown fmxmlsnippet type. Supported: Script/Steps, Scripts, Fields, Tables, Custom Functions, Value Lists, Layout Objects.");
        }

        // For supported types, returning the validated XML is sufficient for FileMaker paste
        return model.getRawXml();
    }
}
