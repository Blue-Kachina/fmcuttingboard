package dev.fmcuttingboard.clipboard;

import com.intellij.openapi.diagnostic.Logger;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static dev.fmcuttingboard.clipboard.FmClipboardCodec.decodeBytesWithBomHeuristics;
import static dev.fmcuttingboard.clipboard.FmClipboardCodec.extractFmxmlFromBytes;
import static dev.fmcuttingboard.clipboard.FmClipboardCodec.stripNulls;

/**
 * macOS-oriented clipboard reader. Uses AWT Clipboard directly and probes a range of
 * text flavors commonly exposed on macOS, including UTF-16 and XML textual flavors.
 * Unlike WindowsClipboardReader, this does not rely on JNA.
 */
class MacClipboardReader implements NativeClipboardReader {

    private static final Logger LOG = Logger.getInstance(MacClipboardReader.class);

    @Override
    public Optional<String> read() {
        try {
            Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable t = systemClipboard.getContents(null);
            if (t == null) return Optional.empty();

            // Fast path: plain string
            if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    String s = (String) t.getTransferData(DataFlavor.stringFlavor);
                    s = stripNulls(s);
                    if (s != null && !s.isBlank()) return Optional.of(s);
                } catch (Throwable ignored) {}
            }

            // Probe likely macOS text variants
            String[] candidates = new String[] {
                    // Plain text as String
                    "text/plain;class=java.lang.String",
                    // UTF-16 or Unicode streams
                    "text/plain;charset=utf-16;class=java.io.InputStream",
                    "text/plain;charset=unicode;class=java.io.InputStream",
                    // HTML and XML as String
                    "text/html;class=java.lang.String",
                    "text/xml;class=java.lang.String",
                    "application/xml;class=java.lang.String",
                    // RTF sometimes carries XML-ish content on copy
                    "text/rtf;class=java.lang.String"
            };

            String s = trySpecificTextFlavors(t, candidates);
            if (s != null && !s.isBlank()) return Optional.of(s);

            // As a last step, attempt to iterate all flavors and try general decoding
            try {
                DataFlavor[] all = t.getTransferDataFlavors();
                if (all != null) {
                    for (DataFlavor f : all) {
                        try {
                            Object data = t.getTransferData(f);
                            if (data == null) continue;
                            if (data instanceof String) {
                                String str = stripNulls((String) data);
                                if (str != null && !str.isBlank()) return Optional.of(str);
                            } else if (data instanceof Reader) {
                                String str = readAll((Reader) data);
                                str = stripNulls(str);
                                if (str != null && !str.isBlank()) return Optional.of(str);
                            } else if (data instanceof InputStream) {
                                byte[] bytes = readAllBytes((InputStream) data);
                                String decoded = decodeBytesWithBomHeuristics(bytes);
                                if (decoded != null && !decoded.isBlank()) return Optional.of(decoded);
                                String extracted = extractFmxmlFromBytes(bytes);
                                if (extracted != null && !extracted.isBlank()) return Optional.of(extracted);
                            } else if (data instanceof byte[]) {
                                byte[] bytes = (byte[]) data;
                                String decoded = decodeBytesWithBomHeuristics(bytes);
                                if (decoded != null && !decoded.isBlank()) return Optional.of(decoded);
                                String extracted = extractFmxmlFromBytes(bytes);
                                if (extracted != null && !extracted.isBlank()) return Optional.of(extracted);
                            }
                        } catch (Throwable ignored) {
                            // Continue other flavors
                        }
                    }
                }
            } catch (Throwable ignored) {
                // ignore
            }

            return Optional.empty();
        } catch (Throwable t) {
            LOG.info("[CB] macOS native probe failed: " + t.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static String trySpecificTextFlavors(Transferable t, String[] mimeTypes) {
        for (String mime : mimeTypes) {
            try {
                DataFlavor flavor = new DataFlavor(mime);
                if (!t.isDataFlavorSupported(flavor)) continue;
                Object data = t.getTransferData(flavor);
                if (data == null) continue;
                if (data instanceof String) {
                    String s = stripNulls((String) data);
                    if (s != null && !s.isBlank()) return s;
                } else if (data instanceof Reader) {
                    String s = readAll((Reader) data);
                    s = stripNulls(s);
                    if (s != null && !s.isBlank()) return s;
                } else if (data instanceof InputStream) {
                    byte[] bytes = readAllBytes((InputStream) data);
                    String s = decodeBytesWithBomHeuristics(bytes);
                    if (s != null && !s.isBlank()) return s;
                    String extracted = extractFmxmlFromBytes(bytes);
                    if (extracted != null && !extracted.isBlank()) return extracted;
                } else if (data instanceof byte[]) {
                    String s = decodeBytesWithBomHeuristics((byte[]) data);
                    if (s != null && !s.isBlank()) return s;
                }
            } catch (Throwable ignored) {
                // continue
            }
        }
        return null;
    }

    private static String readAll(Reader reader) throws IOException {
        if (reader == null) return null;
        try (Reader r = reader) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        }
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        if (in == null) return new byte[0];
        try (InputStream i = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = i.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }
}
