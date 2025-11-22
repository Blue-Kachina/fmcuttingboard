package dev.fmcuttingboard.clipboard;

import com.intellij.openapi.diagnostic.Logger;
import dev.fmcuttingboard.util.Diagnostics;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Phase 2.2 — macOS Clipboard Writer (scaffold)
 *
 * Notes:
 * - On macOS, FileMaker appears to use custom pasteboard type names analogous to the Windows
 *   custom formats (e.g., "Mac-XMSS", "Mac-XMFD", etc.). Implementing true custom pasteboard
 *   writes requires invoking NSPasteboard APIs via JNI/JNA. Since we cannot validate on macOS
 *   in this environment, this class provides a best-effort text multi-flavor writer and a
 *   placeholder for future native integration.
 * - The method returns false when native path is not available, allowing callers to fall back
 *   to standard multi-flavor AWT/CopyPasteManager writes.
 */
final class MacClipboardWriter {
    private static final Logger LOG = Logger.getInstance(MacClipboardWriter.class);

    private MacClipboardWriter() {}

    /**
     * Attempt a native-like macOS pasteboard write. Currently returns false to indicate that
     * native integration is not active, but logs diagnostics and performs minor normalization
     * steps that are expected for fmxmlsnippet payloads.
     */
    static boolean write(String text) {
        // Guard: only attempt on macOS
        String os = System.getProperty("os.name", "");
        boolean isMac = os != null && (os.toLowerCase().startsWith("mac") || os.toLowerCase().contains("os x"));
        if (!isMac) return false;

        // Placeholder for future JNA/JNI NSPasteboard path. We emit diagnostics to make
        // behavior traceable in verbose logs and immediately return false to allow the
        // caller to use the generic multi-flavor AWT path.
        if (Diagnostics.isVerbose()) {
            int bytes = text == null ? 0 : text.getBytes(StandardCharsets.UTF_8).length;
            LOG.info("[CB-mac] Native pasteboard writer not active; falling back to AWT flavors. bytes=" + bytes);
        }

        // Example idea (not used currently): publish UTF-16 stream flavor directly via AWT
        // to increase compatibility. We intentionally do not claim success here, so the
        // caller still performs its broader multi-flavor write.
        try {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (cb != null && text != null && !text.isEmpty()) {
                DataFlavor utf16Stream = new DataFlavor("text/plain;charset=utf-16;class=java.io.InputStream");
                Transferable t = new Transferable() {
                    private final DataFlavor[] flavors = new DataFlavor[] { utf16Stream };
                    @Override public DataFlavor[] getTransferDataFlavors() { return flavors.clone(); }
                    @Override public boolean isDataFlavorSupported(DataFlavor flavor) { return utf16Stream.equals(flavor); }
                    @Override public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                        if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
                        // Provide BOM (BE) explicitly
                        byte[] bom = new byte[] {(byte)0xFE, (byte)0xFF};
                        byte[] body = text.getBytes(StandardCharsets.UTF_16BE);
                        byte[] all = new byte[bom.length + body.length];
                        System.arraycopy(bom, 0, all, 0, bom.length);
                        System.arraycopy(body, 0, all, bom.length, body.length);
                        return new ByteArrayInputStream(all);
                    }
                };
                // Do NOT set contents here to avoid taking ownership before the caller's broader write.
                // This block serves as documentation for a potential flavor-only enhancement.
            }
        } catch (Throwable ignore) {
            // diagnostics-only path
        }

        return false;
    }
}
