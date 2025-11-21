package dev.fmcuttingboard.clipboard;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ide.CopyPasteManager;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Default implementation backed by IntelliJ's CopyPasteManager.
 */
public class DefaultClipboardService implements ClipboardService {

    private static final Logger LOG = Logger.getInstance(DefaultClipboardService.class);

    private final CopyPasteManager manager; // may be null in non-IDE test environments
    private final NativeClipboardReader nativeReader; // may be null when not supported

    public DefaultClipboardService() {
        this(safeCopyPasteManager(), createDefaultNativeReader());
    }

    public DefaultClipboardService(CopyPasteManager manager) {
        this(manager, createDefaultNativeReader());
    }

    // Visible for tests / DI
    public DefaultClipboardService(CopyPasteManager manager, NativeClipboardReader nativeReader) {
        this.manager = manager;
        this.nativeReader = nativeReader;
    }

    @Override
    public Optional<String> readText() throws ClipboardAccessException {
        try {
            // Environment diagnostics (once per call; lightweight)
            try {
                String os = System.getProperty("os.name", "");
                String ver = System.getProperty("os.version", "");
                String arch = System.getProperty("os.arch", "");
                String java = System.getProperty("java.version", "");
                LOG.info("[CB] Env: os=" + os + " " + ver + ", arch=" + arch + ", java=" + java + ")");
            } catch (Throwable ignore) {
                // ignore env log failures
            }
            // 1) Fast path: IntelliJ's CopyPasteManager with plain String flavor
            if (manager != null) {
                boolean sfAvail = manager.areDataFlavorsAvailable(DataFlavor.stringFlavor);
                LOG.info("[CB] CPM stringFlavor available=" + sfAvail);
                if (sfAvail) {
                    String s = (String) manager.getContents(DataFlavor.stringFlavor);
                    if (s != null) {
                        int nul = (int) s.chars().filter(ch -> ch == 0).count();
                        boolean containsFmxml = s.toLowerCase().contains("<fmxmlsnippet");
                        LOG.info("[CB] CPM stringFlavor len=" + s.length() + ", nulCount=" + nul + ", containsFmxml=" + containsFmxml);
                        s = stripNulls(s);
                        if (s.isBlank()) {
                            LOG.info("[CB] CPM stringFlavor normalized to blank after NUL strip; continuing to probe CPM flavors");
                        } else {
                            return Optional.of(s);
                        }
                    } else {
                        LOG.info("[CB] CPM stringFlavor returned null; continuing to probe CPM flavors");
                    }
                }

                // 1b) Full-flavor probing via CopyPasteManager (no enumeration API in this platform version)
                try {
                    DataFlavor[] candidates = new DataFlavor[]{
                            DataFlavor.stringFlavor,
                            new DataFlavor("text/plain;class=java.lang.String"),
                            new DataFlavor("text/plain;charset=utf-16;class=java.io.InputStream"),
                            new DataFlavor("text/plain;charset=unicode;class=java.io.InputStream"),
                            new DataFlavor("text/html;class=java.lang.String"),
                            new DataFlavor("text/rtf;class=java.lang.String"),
                            new DataFlavor("text/xml;class=java.lang.String"),
                            new DataFlavor("application/xml;class=java.lang.String")
                    };

                    // Log which candidates are reported available
                    StringBuilder avail = new StringBuilder("[CB] CPM available candidate flavors: ");
                    boolean anyAvail = false;
                    for (int i = 0; i < candidates.length; i++) {
                        DataFlavor f = candidates[i];
                        boolean available;
                        try {
                            available = manager.areDataFlavorsAvailable(f);
                        } catch (Throwable t) {
                            available = false;
                        }
                        if (available) {
                            if (anyAvail) avail.append(", ");
                            avail.append('[').append(f.getMimeType()).append("]");
                            anyAvail = true;
                        }
                    }
                    LOG.info(anyAvail ? avail.toString() : "[CB] CPM no candidate flavors reported available");

                    for (DataFlavor flavor : candidates) {
                        boolean available;
                        try {
                            available = manager.areDataFlavorsAvailable(flavor);
                        } catch (Throwable t) {
                            available = false;
                        }
                        if (!available) continue;
                        try {
                            Object data = manager.getContents(flavor);
                            try {
                                LOG.info("[CB] CPM Flavor (candidate): " + flavor.getMimeType() + "; class=" + flavor.getRepresentationClass().getName() + "; isText=" + flavor.isFlavorTextType());
                            } catch (Throwable ignore) {
                                // logging only
                            }
                            if (data == null) continue;

                            if (data instanceof String) {
                                String s = stripNulls((String) data);
                                boolean containsFmxml = s.toLowerCase().contains("<fmxmlsnippet");
                                LOG.info("[CB] CPM as String len=" + s.length() + ", containsFmxml=" + containsFmxml);
                                if (!s.isBlank()) return Optional.of(s);
                            } else if (data instanceof Reader) {
                                String s = readAll((Reader) data);
                                s = stripNulls(s);
                                boolean containsFmxml = s != null && s.toLowerCase().contains("<fmxmlsnippet");
                                LOG.info("[CB] CPM as Reader decodedLen=" + (s == null ? -1 : s.length()) + ", containsFmxml=" + containsFmxml);
                                if (s != null && !s.isBlank()) return Optional.of(s);
                            } else if (data instanceof InputStream) {
                                byte[] bytes = readAllBytes((InputStream) data);
                                String s = decodeBytesWithBomHeuristics(bytes);
                                LOG.info("[CB] CPM as InputStream bytesLen=" + bytes.length + ", decodedLen=" + (s == null ? -1 : s.length()) + ", containsFmxml=" + (s != null && s.toLowerCase().contains("<fmxmlsnippet")));
                                if (s != null && !s.isBlank()) return Optional.of(s);
                                String extracted = extractFmxmlFromBytes(bytes);
                                LOG.info("[CB] CPM as InputStream extracted snippet len=" + (extracted == null ? -1 : extracted.length()));
                                if (extracted != null && !extracted.isBlank()) return Optional.of(extracted);
                            } else if (data instanceof byte[]) {
                                byte[] bytes = (byte[]) data;
                                String s = decodeBytesWithBomHeuristics(bytes);
                                LOG.info("[CB] CPM as byte[] bytesLen=" + bytes.length + ", decodedLen=" + (s == null ? -1 : s.length()) + ", containsFmxml=" + (s != null && s.toLowerCase().contains("<fmxmlsnippet")));
                                if (s != null && !s.isBlank()) return Optional.of(s);
                                String extracted = extractFmxmlFromBytes(bytes);
                                LOG.info("[CB] CPM as byte[] extracted snippet len=" + (extracted == null ? -1 : extracted.length()));
                                if (extracted != null && !extracted.isBlank()) return Optional.of(extracted);
                            }
                        } catch (Throwable cpmFlavorErr) {
                            LOG.info("[CB] CPM candidate flavor read failed: " + cpmFlavorErr.getClass().getSimpleName());
                        }
                    }
                } catch (Throwable cpmErr) {
                    LOG.info("[CB] CPM full-flavor probing failed (candidates): " + cpmErr.getClass().getSimpleName());
                }
            }

            // 2) Fallback path (PowerShell-inspired): enumerate all flavors from AWT clipboard
            //    and try to coerce to text using several strategies (String, Reader, InputStream, bytes).
            Clipboard sysClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (sysClipboard == null) {
                LOG.info("[CB] AWT early-exit: Toolkit.getSystemClipboard() returned null");
                Optional<String> nativeResultEarly = tryNativeClipboard();
                if (nativeResultEarly.isPresent()) return nativeResultEarly;
                return Optional.empty();
            }
            Transferable t = sysClipboard.getContents(null);
            if (t == null) {
                LOG.info("[CB] AWT early-exit: sysClipboard.getContents(null) returned null");
                Optional<String> nativeResultEarly = tryNativeClipboard();
                if (nativeResultEarly.isPresent()) return nativeResultEarly;
                return Optional.empty();
            }

            // Prefer text flavors first
            DataFlavor[] flavors = t.getTransferDataFlavors();
            if (flavors == null || flavors.length == 0) {
                LOG.info("[CB] AWT early-exit: getTransferDataFlavors() is null/empty");
                Optional<String> nativeResultEarly = tryNativeClipboard();
                if (nativeResultEarly.isPresent()) return nativeResultEarly;
                return Optional.empty();
            }

            // 2a) Try a few known text-like flavors explicitly (some platforms may not mark them as flavorTextType)
            String direct = trySpecificTextFlavors(t,
                    new String[]{
                            "text/plain;class=java.lang.String",
                            "text/plain;charset=utf-16;class=java.io.InputStream",
                            "text/plain;charset=unicode;class=java.io.InputStream",
                            "text/html;class=java.lang.String",
                            "text/rtf;class=java.lang.String",
                            "text/xml;class=java.lang.String",
                            "application/xml;class=java.lang.String"
                    });
            if (direct != null && !direct.isEmpty()) {
                return Optional.of(direct);
            }

            // 2b) Let AWT pick the best text flavor if available
            try {
                DataFlavor best = DataFlavor.selectBestTextFlavor(flavors);
                if (best != null) {
                    try (Reader rdr = best.getReaderForText(t)) {
                        if (rdr != null) {
                            String s = readAll(rdr);
                            if (s != null && !s.isEmpty()) {
                                return Optional.of(s);
                            }
                        }
                    } catch (UnsupportedFlavorException ignore) {
                        // fall through
                    }
                }
            } catch (Throwable ignore) {
                // continue with manual probing
            }

            for (DataFlavor flavor : flavors) {
                try {
                    try {
                        LOG.info("[CB] Flavor: " + flavor.getMimeType() + "; class=" + flavor.getRepresentationClass().getName() + "; isText=" + flavor.isFlavorTextType());
                    } catch (Throwable ignore) {
                        // logging only
                    }
                    // If it's declared as text, try the standard reader path
                    if (flavor.isFlavorTextType()) {
                        try {
                            Reader reader = flavor.getReaderForText(t);
                            if (reader != null) {
                                String s = readAll(reader);
                                if (s != null) {
                                    s = stripNulls(s);
                                    boolean containsFmxml = s.toLowerCase().contains("<fmxmlsnippet");
                                    LOG.info("[CB] ReaderForText decodedLen=" + s.length() + ", containsFmxml=" + containsFmxml);
                                }
                                if (s != null && !s.isBlank()) return Optional.of(s);
                            }
                        } catch (UnsupportedFlavorException ignore) {
                            // fall through to other attempts
                        }
                    }

                    Object data = t.getTransferData(flavor);
                    if (data == null) continue;

                    if (data instanceof String) {
                        String s = stripNulls((String) data);
                        try {
                            int nul = 0; // already stripped
                            boolean containsFmxml = s.toLowerCase().contains("<fmxmlsnippet");
                            LOG.info("[CB] Data as String len=" + s.length() + ", nulCount=" + nul + ", containsFmxml=" + containsFmxml);
                        } catch (Throwable ignore) {
                            // logging only
                        }
                        if (!s.isBlank()) return Optional.of(s);
                    } else if (data instanceof InputStream) {
                        byte[] bytes = readAllBytes((InputStream) data);
                        String s = decodeBytesWithBomHeuristics(bytes);
                        try {
                            LOG.info("[CB] Data as InputStream bytesLen=" + bytes.length + ", decodedLen=" + (s == null ? -1 : s.length()) +
                                    ", containsFmxml=" + (s != null && s.toLowerCase().contains("<fmxmlsnippet")));
                        } catch (Throwable ignore) {
                            // logging only
                        }
                        if (s != null && !s.isBlank()) return Optional.of(s);
                        // Raw fmxmlsnippet extraction as last resort for this flavor
                        String extracted = extractFmxmlFromBytes(bytes);
                        try {
                            LOG.info("[CB] Data as InputStream extracted snippet len=" + (extracted == null ? -1 : extracted.length()));
                        } catch (Throwable ignore) {
                            // logging only
                        }
                        if (extracted != null && !extracted.isBlank()) return Optional.of(extracted);
                    } else if (data instanceof byte[]) {
                        String s = decodeBytesWithBomHeuristics((byte[]) data);
                        try {
                            LOG.info("[CB] Data as byte[] bytesLen=" + ((byte[]) data).length + ", decodedLen=" + (s == null ? -1 : s.length()) +
                                    ", containsFmxml=" + (s != null && s.toLowerCase().contains("<fmxmlsnippet")));
                        } catch (Throwable ignore) {
                            // logging only
                        }
                        if (s != null && !s.isBlank()) return Optional.of(s);
                        String extracted = extractFmxmlFromBytes((byte[]) data);
                        try {
                            LOG.info("[CB] Data as byte[] extracted snippet len=" + (extracted == null ? -1 : extracted.length()));
                        } catch (Throwable ignore) {
                            // logging only
                        }
                        if (extracted != null && !extracted.isBlank()) return Optional.of(extracted);
                    }
                } catch (UnsupportedFlavorException ignored) {
                    // Try next flavor
                }
            }

            // Diagnostics: log discovered flavors to help troubleshoot
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("Clipboard had flavors but none yielded non-empty text. Flavors= ");
                for (int i = 0; i < flavors.length; i++) {
                    DataFlavor f = flavors[i];
                    if (i > 0) sb.append(", ");
                    sb.append('[')
                      .append(f.getMimeType())
                      .append("; class=")
                      .append(f.getRepresentationClass().getSimpleName())
                      .append(']');
                }
                LOG.info(sb.toString());
            } catch (Throwable ignore) {
                // ignore logging failures
            }

            // 3) Windows-native fallback via JNA (CF_UNICODETEXT / CF_TEXT)
            Optional<String> nativeResult = tryNativeClipboard();
            if (nativeResult.isPresent()) return nativeResult;

            return Optional.empty();
        } catch (IllegalStateException e) { // clipboard busy/locked
            throw new ClipboardAccessException("Clipboard is currently unavailable (locked).", e);
        } catch (Throwable t) {
            throw new ClipboardAccessException("Unexpected clipboard error while reading.", t);
        }
    }

    @Override
    public void writeText(String text) throws ClipboardAccessException {
        try {
            // Publish multiple text flavors to improve compatibility with apps like FileMaker on macOS
            // that may probe XML, UTF-16, or generic text representations. This mirrors the breadth
            // of formats we read and increases the chance that FileMaker recognizes fmxmlsnippet
            // as an object paste, not plain text.
            Transferable multi = new MultiFlavorTextTransferable(text == null ? "" : text);
            if (manager != null) {
                manager.setContents(multi);
            } else {
                Clipboard sysClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                if (sysClipboard == null) {
                    throw new ClipboardAccessException("System clipboard not available.");
                }
                sysClipboard.setContents(multi, null);
            }
        } catch (IllegalStateException e) { // clipboard busy/locked
            throw new ClipboardAccessException("Clipboard is currently unavailable (locked).", e);
        } catch (Throwable t) {
            throw new ClipboardAccessException("Unexpected clipboard error while writing.", t);
        }
    }

    /**
     * Transferable that exposes a wide range of text flavors (String, XML, UTF-16 streams, etc.)
     * to match the formats we probe on read, helping apps (incl. FileMaker on macOS) detect
     * fmxmlsnippet content as structured clipboard data.
     */
    @SuppressWarnings("ClassCanBeLocal")
    private static final class MultiFlavorTextTransferable implements Transferable {
        private final String text;
        private final DataFlavor[] flavors;

        MultiFlavorTextTransferable(String text) {
            this.text = text == null ? "" : text;
            // Build flavors array. Keep stringFlavor first as a fast path.
            DataFlavor[] tmp;
            try {
                tmp = new DataFlavor[] {
                        DataFlavor.stringFlavor,
                        new DataFlavor("text/plain;class=java.lang.String"),
                        new DataFlavor("text/xml;class=java.lang.String"),
                        new DataFlavor("application/xml;class=java.lang.String"),
                        new DataFlavor("text/html;class=java.lang.String"),
                        // UTF-16 variants exposed as streams are often preferred by macOS pasteboards
                        new DataFlavor("text/plain;charset=utf-16;class=java.io.InputStream"),
                        new DataFlavor("text/plain;charset=unicode;class=java.io.InputStream")
                };
            } catch (ClassNotFoundException e) {
                // Should not happen for core JRE classes; fall back to string-only
                tmp = new DataFlavor[] { DataFlavor.stringFlavor };
            }
            this.flavors = tmp;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            // Return a copy to be safe
            return flavors.clone();
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            for (DataFlavor f : flavors) {
                if (f.equals(flavor)) return true;
            }
            return false;
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            // String flavors
            if (flavor.equals(DataFlavor.stringFlavor)) return text;
            if ("java.lang.String".equals(flavor.getRepresentationClass().getName())) {
                return text;
            }

            // InputStream UTF-16 variants
            if (InputStream.class.equals(flavor.getRepresentationClass())) {
                String mime = flavor.getMimeType().toLowerCase();
                if (mime.contains("charset=utf-16") || mime.contains("charset=unicode")) {
                    // Provide BOM to be explicit; many macOS apps accept with or without BOM
                    byte[] bom = new byte[] {(byte)0xFE, (byte)0xFF};
                    byte[] body = text.getBytes(StandardCharsets.UTF_16BE);
                    byte[] all = new byte[bom.length + body.length];
                    System.arraycopy(bom, 0, all, 0, bom.length);
                    System.arraycopy(body, 0, all, bom.length, body.length);
                    return new ByteArrayInputStream(all);
                }
            }

            throw new UnsupportedFlavorException(flavor);
        }
    }

    private static String readAll(Reader reader) {
        try (BufferedReader br = new BufferedReader(reader)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = br.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] readAllBytes(InputStream in) {
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = input.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private static String decodeBytesWithBomHeuristics(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";

        // BOM detection
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
        if (bytes.length >= 2) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            if (b0 == 0xFE && b1 == 0xFF) {
                return stripNulls(new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE));
            }
            if (b0 == 0xFF && b1 == 0xFE) {
                return stripNulls(new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE));
            }
        }

        // Heuristic for UTF-16 without BOM (check zero distribution)
        int zerosEven = 0, zerosOdd = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                if ((i & 1) == 0) zerosEven++; else zerosOdd++;
            }
        }
        int threshold = Math.max(2, bytes.length / 10); // 10% zeros is a hint
        if (zerosOdd > zerosEven && zerosOdd >= threshold) {
            return stripNulls(new String(bytes, StandardCharsets.UTF_16BE));
        } else if (zerosEven > zerosOdd && zerosEven >= threshold) {
            return stripNulls(new String(bytes, StandardCharsets.UTF_16LE));
        }

        // Fallback to UTF-8 (as PS script does)
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        return stripNulls(utf8);
    }

    private static String stripNulls(String s) {
        if (s == null) return null;
        // Remove embedded NULs which can cause isBlank() to see effectively empty text
        return s.replace("\u0000", "");
    }

    /**
     * Attempts to locate an <fmxmlsnippet>…</fmxmlsnippet> block directly in the raw bytes in common encodings
     * (UTF-8/ASCII, UTF-16LE, UTF-16BE). If found, decodes and returns the snippet; otherwise returns null.
     */
    private static String extractFmxmlFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;

        // UTF-8 / ASCII search
        int startUtf8 = indexOf(bytes, ascii("<fmxmlsnippet"));
        if (startUtf8 >= 0) {
            int endUtf8 = lastIndexOf(bytes, ascii("</fmxmlsnippet>"));
            if (endUtf8 >= 0 && endUtf8 >= startUtf8) {
                int endPos = endUtf8 + ascii("</fmxmlsnippet>").length;
                return new String(bytes, startUtf8, endPos - startUtf8, StandardCharsets.UTF_8).trim();
            }
        }

        // UTF-16LE search
        byte[] startLe = utf16le("<fmxmlsnippet");
        byte[] endLe = utf16le("</fmxmlsnippet>");
        int start16le = indexOf(bytes, startLe);
        if (start16le >= 0) {
            int end16le = lastIndexOf(bytes, endLe);
            if (end16le >= 0 && end16le >= start16le) {
                int endPos = end16le + endLe.length;
                String s = new String(bytes, start16le, endPos - start16le, StandardCharsets.UTF_16LE);
                return stripNulls(s).trim();
            }
        }

        // UTF-16BE search
        byte[] startBe = utf16be("<fmxmlsnippet");
        byte[] endBe = utf16be("</fmxmlsnippet>");
        int start16be = indexOf(bytes, startBe);
        if (start16be >= 0) {
            int end16be = lastIndexOf(bytes, endBe);
            if (end16be >= 0 && end16be >= start16be) {
                int endPos = end16be + endBe.length;
                String s = new String(bytes, start16be, endPos - start16be, StandardCharsets.UTF_16BE);
                return stripNulls(s).trim();
            }
        }

        return null;
    }

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] utf16le(String s) {
        // Encode and drop potential BOM
        byte[] b = s.getBytes(StandardCharsets.UTF_16LE);
        return b;
    }

    private static byte[] utf16be(String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_16BE);
        return b;
    }

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

    private static CopyPasteManager safeCopyPasteManager() {
        try {
            return CopyPasteManager.getInstance();
        } catch (Throwable t) {
            // In unit tests or non-IDE contexts, ApplicationManager may be null; fall back to AWT clipboard only
            return null;
        }
    }

    private Optional<String> tryNativeClipboard() {
        try {
            if (nativeReader == null) return Optional.empty();
            String os = System.getProperty("os.name", "");
            boolean isWindows = os != null && os.toLowerCase().startsWith("windows");
            boolean isMac = os != null && (os.toLowerCase().startsWith("mac") || os.toLowerCase().contains("os x"));

            if (isWindows) {
                // Verify JNA classes are available in the runtime classloader to avoid NoClassDefFoundError
                try {
                    Class.forName("com.sun.jna.Native");
                    Class.forName("com.sun.jna.win32.W32APIOptions");
                } catch (Throwable jnaMissing) {
                    LOG.info("[CB] Native path: JNA classes not found in runtime: " + jnaMissing.getClass().getSimpleName());
                    return Optional.empty();
                }
                LOG.info("[CB] Native path: attempting Windows clipboard read (CF_UNICODETEXT/CF_TEXT)");
            } else if (isMac) {
                LOG.info("[CB] Native path: attempting macOS clipboard probe (public.utf16-plain-text, NSStringPboardType, XML/text)");
            } else {
                // Other OS not supported by native path
                return Optional.empty();
            }
            Optional<String> result = nativeReader.read();
            if (result.isPresent()) {
                String s = result.get();
                boolean containsFmxml = s.toLowerCase().contains("<fmxmlsnippet");
                LOG.info("[CB] Native path: success, len=" + s.length() + ", containsFmxml=" + containsFmxml);
            } else {
                LOG.info("[CB] Native path: no usable text returned");
            }
            return result;
        } catch (Throwable t) {
            LOG.info("[CB] Native path failed: " + t.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static NativeClipboardReader createDefaultNativeReader() {
        try {
            String os = System.getProperty("os.name", "");
            if (os != null && os.toLowerCase().startsWith("windows")) {
                return new WindowsClipboardReader();
            }
            if (os != null && (os.toLowerCase().startsWith("mac") || os.toLowerCase().contains("os x"))) {
                return new MacClipboardReader();
            }
        } catch (Throwable ignore) {
            // no-op
        }
        return null;
    }

    private static String trySpecificTextFlavors(Transferable t, String[] mimeTypes) {
        for (String mime : mimeTypes) {
            try {
                DataFlavor flavor = new DataFlavor(mime);
                if (!t.isDataFlavorSupported(flavor)) continue;
                Object data = t.getTransferData(flavor);
                if (data == null) continue;
                if (data instanceof String) {
                    String s = (String) data;
                    if (!s.isEmpty()) return s;
                } else if (data instanceof Reader) {
                    String s = readAll((Reader) data);
                    if (s != null && !s.isEmpty()) return s;
                } else if (data instanceof InputStream) {
                    byte[] bytes = readAllBytes((InputStream) data);
                    String s = decodeBytesWithBomHeuristics(bytes);
                    if (s != null && !s.isEmpty()) return s;
                } else if (data instanceof byte[]) {
                    String s = decodeBytesWithBomHeuristics((byte[]) data);
                    if (s != null && !s.isEmpty()) return s;
                }
            } catch (ClassNotFoundException | UnsupportedFlavorException | LinkageError ignored) {
                // Ignore and continue
            } catch (Throwable ignored) {
                // Continue trying others
            }
        }
        return null;
    }
}
