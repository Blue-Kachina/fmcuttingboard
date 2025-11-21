package dev.fmcuttingboard.integration;

import dev.fmcuttingboard.actions.PushClipboardIntoFileMakerAction;
import dev.fmcuttingboard.actions.ReadClipboardIntoNewXmlFileAction;
import dev.fmcuttingboard.clipboard.ClipboardAccessException;
import dev.fmcuttingboard.clipboard.ClipboardService;
import dev.fmcuttingboard.fm.ClipboardToXmlConverter;
import dev.fmcuttingboard.fm.DefaultXmlToClipboardConverter;
import dev.fmcuttingboard.util.UserNotifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for workflow: Convert → Save XML → Push to Clipboard
 */
class WorkflowIntegrationTest {

    private static final UserNotifier NOOP = (p, t, title, content) -> {};

    private static class CapturingClipboard implements ClipboardService {
        String lastText;
        @Override
        public Optional<String> readText() throws ClipboardAccessException { return Optional.ofNullable(lastText); }
        @Override
        public void writeText(String text) throws ClipboardAccessException { this.lastText = text; }
    }

    @TempDir
    Path tmpDir;

    @Test
    void endToEnd_convert_save_push() throws Exception {
        // Start with a clipboard-like payload that contains an fmxmlsnippet with a script
        String clipboardPayload = "random header\n<fmxmlsnippet>\n  <Script name=\"DoWork\">\n    <Step id=\"1\"/>\n  </Script>\n</fmxmlsnippet>\nrandom footer";

        // Convert clipboard text to normalized XML
        ClipboardToXmlConverter toXml = new ClipboardToXmlConverter();
        String xml = toXml.convertToXml(clipboardPayload);
        assertTrue(xml.startsWith("<fmxmlsnippet"));

        // Save to timestamped file under .fmCuttingBoard
        ReadClipboardIntoNewXmlFileAction saveAction = new ReadClipboardIntoNewXmlFileAction(
                new CapturingClipboard(), new ClipboardToXmlConverter(), NOOP);
        Path writtenFile = saveAction.processIntoNewXmlFile(null, tmpDir, xml);
        assertTrue(Files.exists(writtenFile));
        assertEquals(tmpDir.resolve(".fmCuttingBoard"), writtenFile.getParent());
        String saved = Files.readString(writtenFile, StandardCharsets.UTF_8);
        assertEquals(xml, saved);

        // Push that XML into the clipboard (convert to FileMaker payload)
        CapturingClipboard capture = new CapturingClipboard();
        PushClipboardIntoFileMakerAction pushAction = new PushClipboardIntoFileMakerAction(
                capture, new DefaultXmlToClipboardConverter(), NOOP);
        String payload = pushAction.processXmlToClipboard(saved);

        assertNotNull(payload);
        assertEquals(payload, capture.lastText);
        assertTrue(payload.contains("<fmxmlsnippet"));
    }
}
