package dev.fmcuttingboard.clipboard;

import com.intellij.openapi.diagnostic.Logger;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

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

    private static String decodeBytesWithBomHeuristics(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        // UTF-8 BOM
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
        // UTF-16 LE BOM
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        }
        // UTF-16 BE BOM
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
        }
        // Heuristic: if many zero bytes, interpret as UTF-16
        int zero = 0;
        for (byte b : bytes) if (b == 0) zero++;
        if (zero > bytes.length / 4) {
            // try LE then BE
            String le = new String(bytes, StandardCharsets.UTF_16LE);
            String leStripped = stripNulls(le);
            if (leStripped != null && leStripped.length() >= 3) return leStripped;
            String be = new String(bytes, StandardCharsets.UTF_16BE);
            String beStripped = stripNulls(be);
            if (beStripped != null && beStripped.length() >= 3) return beStripped;
        }
        // fallback to default or UTF-8
        try {
            return new String(bytes, Charset.defaultCharset());
        } catch (Throwable ignore) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static String stripNulls(String s) {
        if (s == null) return null;
        // Remove embedded NULs that sometimes appear when decoding UTF-16 content as UTF-8
        return s.replace("\u0000", "");
    }

    private static String extractFmxmlFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        // Search for ASCII and UTF-16 variants of <fmxmlsnippet ... ></fmxmlsnippet>
        byte[] startAscii = ascii("<fmxmlsnippet");
        byte[] endAscii = ascii("</fmxmlsnippet>");
        int start = indexOf(bytes, startAscii);
        int end = (start >= 0) ? lastIndexOf(bytes, endAscii) : -1;
        if (start >= 0 && end >= 0 && end > start) {
            String s = new String(bytes, StandardCharsets.UTF_8);
            String sub = s.substring(start, end + endAscii.length);
            return stripNulls(sub);
        }
        // UTF-16 LE
        byte[] startLe = utf16le("<fmxmlsnippet");
        byte[] endLe = utf16le("</fmxmlsnippet>");
        start = indexOf(bytes, startLe);
        end = (start >= 0) ? lastIndexOf(bytes, endLe) : -1;
        if (start >= 0 && end >= 0 && end > start) {
            String s = new String(bytes, StandardCharsets.UTF_16LE);
            String sub = s.substring(start / 2, (end + endLe.length) / 2);
            return stripNulls(sub);
        }
        // UTF-16 BE
        byte[] startBe = utf16be("<fmxmlsnippet");
        byte[] endBe = utf16be("</fmxmlsnippet>");
        start = indexOf(bytes, startBe);
        end = (start >= 0) ? lastIndexOf(bytes, endBe) : -1;
        if (start >= 0 && end >= 0 && end > start) {
            String s = new String(bytes, StandardCharsets.UTF_16BE);
            String sub = s.substring(start / 2, (end + endBe.length) / 2);
            return stripNulls(sub);
        }
        return null;
    }

    private static byte[] ascii(String s) { return s.getBytes(StandardCharsets.US_ASCII); }
    private static byte[] utf16le(String s) { return s.getBytes(StandardCharsets.UTF_16LE); }
    private static byte[] utf16be(String s) { return s.getBytes(StandardCharsets.UTF_16BE); }

    private static int indexOf(byte[] data, byte[] pattern) {
        if (pattern.length == 0) return 0;
        outer:
        for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static int lastIndexOf(byte[] data, byte[] pattern) {
        if (pattern.length == 0) return data.length;
        outer:
        for (int i = data.length - pattern.length; i >= 0; i--) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
