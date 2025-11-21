package dev.fmcuttingboard.fm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClipboardToXmlConverterTest {

    private final ClipboardToXmlConverter converter = new ClipboardToXmlConverter();

    @Test
    void convertsValidSnippet() throws Exception {
        String payload = "Noise before\n<fmxmlsnippet type=\"LayoutObjects\">\n  <Layout name=\"L1\"/>\n</fmxmlsnippet>\nnoise after";
        FmSnippet snippet = converter.convert(payload);
        assertNotNull(snippet);
        assertTrue(snippet.getXml().startsWith("<fmxmlsnippet"));
        assertTrue(snippet.getXml().endsWith("</fmxmlsnippet>"));
        assertTrue(snippet.getElementTypes().contains(ElementType.LAYOUTS));
    }

    @Test
    void detectsFieldType() throws Exception {
        String payload = "<fmxmlsnippet>\n  <FieldDefinition name=\"Test\"/>\n</fmxmlsnippet>";
        FmSnippet snippet = converter.convert(payload);
        assertTrue(snippet.getElementTypes().contains(ElementType.FIELDS));
    }

    @Test
    void detectsScriptType() throws Exception {
        String payload = "<fmxmlsnippet>\n  <Script name=\"Do\">\n    <Step id=\"1\"/>\n  </Script>\n</fmxmlsnippet>";
        FmSnippet snippet = converter.convert(payload);
        assertTrue(snippet.getElementTypes().contains(ElementType.SCRIPTS));
    }

    @Test
    void failsWhenNoSnippet() {
        String payload = "<root><child/></root>";
        assertThrows(ConversionException.class, () -> converter.convert(payload));
    }

    @Test
    void failsWhenEmptyOrNull() {
        assertThrows(ConversionException.class, () -> converter.convert(""));
        assertThrows(ConversionException.class, () -> converter.convert(null));
    }
}
