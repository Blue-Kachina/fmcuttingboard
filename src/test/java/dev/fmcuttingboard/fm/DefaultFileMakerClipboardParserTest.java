package dev.fmcuttingboard.fm;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DefaultFileMakerClipboardParserTest {

    private final DefaultFileMakerClipboardParser parser = new DefaultFileMakerClipboardParser();

    @Test
    void detectsFmxmlsnippetPositive() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<fmxmlsnippet type=\"LayoutObjects\">\n" +
                "  <SomeNode/>\n" +
                "</fmxmlsnippet>";
        assertTrue(parser.isLikelyFileMakerContent(xml));
        Optional<String> norm = parser.normalizeToXmlText(xml);
        assertTrue(norm.isPresent());
        assertTrue(norm.get().startsWith("<fmxmlsnippet"));
        assertTrue(norm.get().endsWith("</fmxmlsnippet>"));
    }

    @Test
    void rejectsPlainText() {
        String text = "Just a plain sentence, nothing special.";
        assertFalse(parser.isLikelyFileMakerContent(text));
        assertTrue(parser.normalizeToXmlText(text).isEmpty());
    }

    @Test
    void extractsSnippetFromSurroundingNoise() {
        String payload = "Some header\n\n<fmxmlsnippet>\n  <x/>\n</fmxmlsnippet>\nTrailing logs";
        Optional<String> norm = parser.normalizeToXmlText(payload);
        assertTrue(norm.isPresent());
        assertEquals("<fmxmlsnippet>\n  <x/>\n</fmxmlsnippet>", norm.get());
        assertTrue(parser.isLikelyFileMakerContent(payload));
    }

    @Test
    void rejectsOtherXmlFormats() {
        String otherXml = "<?xml version=\"1.0\"?><root><child/></root>";
        assertFalse(parser.isLikelyFileMakerContent(otherXml));
        assertTrue(parser.normalizeToXmlText(otherXml).isEmpty());
    }
}
