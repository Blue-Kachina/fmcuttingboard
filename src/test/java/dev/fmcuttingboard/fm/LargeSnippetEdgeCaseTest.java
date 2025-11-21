package dev.fmcuttingboard.fm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LargeSnippetEdgeCaseTest {

    private static String largeFieldsSnippet(int count) {
        StringBuilder sb = new StringBuilder(128 + count * 40);
        sb.append("<fmxmlsnippet>\n");
        for (int i = 0; i < count; i++) {
            sb.append("  <FieldDefinition name=\"F").append(i).append("\"/>\n");
        }
        sb.append("</fmxmlsnippet>");
        return sb.toString();
    }

    @Test
    void parser_and_converters_handle_large_field_lists() throws Exception {
        String xml = largeFieldsSnippet(2000);

        // Parse
        FmXmlParser parser = new FmXmlParser();
        ParsedSnippet sn = parser.parse(xml);
        assertNotNull(sn);
        assertTrue(sn.getElementTypes().contains(ElementType.FIELDS));
        assertTrue(sn.getFieldNames().contains("F0"));
        assertTrue(sn.getFieldNames().contains("F1999"));

        // Convert to clipboard payload (should be essentially normalized XML)
        XmlToClipboardConverter toClipboard = new DefaultXmlToClipboardConverter(parser);
        String payload = toClipboard.convertToClipboardPayload(xml);
        assertNotNull(payload);
        assertTrue(payload.startsWith("<fmxmlsnippet"));

        // Simulate clipboard-like noise and convert back to XML via regex-based extractor
        String noisy = "Header noise\n" + xml + "\nFooter noise";
        ClipboardToXmlConverter fromClipboard = new ClipboardToXmlConverter();
        String normalized = fromClipboard.convertToXml(noisy);
        assertEquals(xml, normalized);
    }

    @Test
    void clipboard_parser_rejects_non_snippet_even_if_large() {
        StringBuilder sb = new StringBuilder();
        sb.append("<root>\n");
        for (int i = 0; i < 5000; i++) sb.append("  <n/>");
        sb.append("\n</root>");

        DefaultFileMakerClipboardParser p = new DefaultFileMakerClipboardParser();
        assertFalse(p.isLikelyFileMakerContent(sb.toString()));
        assertTrue(p.normalizeToXmlText(sb.toString()).isEmpty());
    }
}
