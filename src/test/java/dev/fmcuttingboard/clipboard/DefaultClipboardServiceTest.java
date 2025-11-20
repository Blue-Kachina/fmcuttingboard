package dev.fmcuttingboard.clipboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DefaultClipboardServiceTest {

    @Test
    void writeThenReadRoundTrip() throws Exception {
        DefaultClipboardService svc = new DefaultClipboardService();

        // Some CI environments may not allow clipboard access. If we fail to access,
        // skip this test rather than fail the pipeline.
        try {
            String payload = "Hello FMCuttingBoard";
            svc.writeText(payload);
            Optional<String> read = svc.readText();
            assumeTrue(read.isPresent(), "Clipboard text not present; environment may not allow clipboard access");
            assertEquals(payload, read.get());
        } catch (ClipboardAccessException ex) {
            assumeTrue(false, "Skipping due to clipboard access exception: " + ex.getMessage());
        }
    }

    @Test
    void writeNullBecomesEmptyString() throws Exception {
        DefaultClipboardService svc = new DefaultClipboardService();
        try {
            svc.writeText(null);
            Optional<String> read = svc.readText();
            assumeTrue(read.isPresent(), "Clipboard text not present; environment may not allow clipboard access");
            assertEquals("", read.get());
        } catch (ClipboardAccessException ex) {
            assumeTrue(false, "Skipping due to clipboard access exception: " + ex.getMessage());
        }
    }
}
