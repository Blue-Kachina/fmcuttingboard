package dev.fmcuttingboard.actions;

import dev.fmcuttingboard.clipboard.ClipboardAccessException;
import dev.fmcuttingboard.clipboard.ClipboardService;
import dev.fmcuttingboard.fm.ConversionException;
import dev.fmcuttingboard.fm.DefaultXmlToClipboardConverter;
import dev.fmcuttingboard.util.UserNotifier;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PushClipboardIntoFileMakerActionTest {

    private static class CapturingClipboard implements ClipboardService {
        String lastText;
        @Override
        public Optional<String> readText() throws ClipboardAccessException { return Optional.empty(); }
        @Override
        public void writeText(String text) throws ClipboardAccessException { this.lastText = text; }
    }

    private static final UserNotifier NOOP_NOTIFIER = (p, t, title, content) -> {};

    @Test
    void processXmlToClipboard_writesPayloadForSupportedSnippet() throws Exception {
        String xml = """
                <fmxmlsnippet>
                  <FieldDefinition name=\"TestField\"/>
                </fmxmlsnippet>
                """;
        CapturingClipboard clipboard = new CapturingClipboard();
        PushClipboardIntoFileMakerAction action = new PushClipboardIntoFileMakerAction(
                clipboard, new DefaultXmlToClipboardConverter(), NOOP_NOTIFIER);

        String payload = action.processXmlToClipboard(xml);
        assertNotNull(payload);
        assertEquals(payload, clipboard.lastText, "Payload should be written to clipboard");
        assertTrue(payload.contains("<fmxmlsnippet"));
    }

    @Test
    void processXmlToClipboard_throwsForUnsupportedUnknownSnippet() {
        String xml = """
                <fmxmlsnippet>
                  <UnknownTag/>
                </fmxmlsnippet>
                """;
        PushClipboardIntoFileMakerAction action = new PushClipboardIntoFileMakerAction(
                new CapturingClipboard(), new DefaultXmlToClipboardConverter(), NOOP_NOTIFIER);

        assertThrows(ConversionException.class, () -> action.processXmlToClipboard(xml));
    }

    @Test
    void enablement_requiresProjectAndXmlExtension() {
        PushClipboardIntoFileMakerAction action = new PushClipboardIntoFileMakerAction(
                new CapturingClipboard(), new DefaultXmlToClipboardConverter(), NOOP_NOTIFIER);

        // No project -> disabled (regardless of extension)
        assertFalse(action.isEnabled(false, "xml"));

        // Simulate non-XML file by extension
        assertFalse(action.isEnabled(true, "txt"));

        // XML file -> enabled
        assertTrue(action.isEnabled(true, "xml"));
    }
}
