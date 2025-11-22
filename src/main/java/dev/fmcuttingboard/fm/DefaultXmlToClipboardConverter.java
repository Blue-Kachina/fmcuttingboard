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

        // Phase 3.3 update: support FIELDS, SCRIPTS, TABLES, CUSTOM FUNCTIONS, and VALUE LISTS; Layout-only remains unsupported for now
        boolean isFields = types.contains(ElementType.FIELDS);
        boolean isScripts = types.contains(ElementType.SCRIPTS);
        boolean isTables = types.contains(ElementType.TABLES);
        boolean isLayouts = types.contains(ElementType.LAYOUTS);
        boolean isCustomFunctions = types.contains(ElementType.CUSTOM_FUNCTIONS);
        boolean isValueLists = types.contains(ElementType.VALUE_LISTS);

        if (isLayouts && !isFields && !isScripts && !isTables && !isCustomFunctions && !isValueLists) {
            throw new ConversionException("Layout snippets are not supported yet.");
        }
        if (!isFields && !isScripts && !isTables && !isCustomFunctions && !isValueLists) {
            throw new ConversionException("Unsupported or unknown fmxmlsnippet type. Supported: Script/Steps, Fields, Tables, Custom Functions, Value Lists.");
        }

        // For supported types, returning the validated XML is sufficient for FileMaker paste
        return model.getRawXml();
    }
}
