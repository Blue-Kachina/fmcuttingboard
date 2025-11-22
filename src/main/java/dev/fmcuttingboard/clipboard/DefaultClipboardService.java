package dev.fmcuttingboard.clipboard;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ide.CopyPasteManager;
import dev.fmcuttingboard.util.Diagnostics;

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
            maybeDumpClipboardFormats("pre-read");
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

            Optional<String> res = Optional.empty();
            try {
                return res;
            } finally {
                maybeDumpClipboardFormats("post-read");
            }
        } catch (IllegalStateException e) { // clipboard busy/locked
            throw new ClipboardAccessException("Clipboard is currently unavailable (locked).", e);
        } catch (Throwable t) {
            throw new ClipboardAccessException("Unexpected clipboard error while reading.", t);
        }
    }

    @Override
    public void writeText(String text) throws ClipboardAccessException {
        try {
            maybeDumpClipboardFormats("pre-write");
            String toWrite = text == null ? "" : text;
            // Try Windows-native write if available to mirror FileMaker's custom formats.
            // For empty payloads, prefer the AWT/CPM path to preserve historical behavior and
            // avoid platform quirks observed in some environments.
            if (!toWrite.isEmpty() && tryWindowsNativeWrite(toWrite)) {
                maybeDumpClipboardFormats("post-write");
                return;
            }
            // Publish multiple text flavors to improve compatibility with apps like FileMaker on macOS
            // that may probe XML, UTF-16, or generic text representations. This mirrors the breadth
            // of formats we read and increases the chance that FileMaker recognizes fmxmlsnippet
            // as an object paste, not plain text.
            Transferable multi = new MultiFlavorTextTransferable(toWrite);
            if (manager != null) {
                manager.setContents(multi);
            } else {
                Clipboard sysClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                if (sysClipboard == null) {
                    throw new ClipboardAccessException("System clipboard not available.");
                }
                sysClipboard.setContents(multi, null);
            }
            maybeDumpClipboardFormats("post-write");
        } catch (IllegalStateException e) { // clipboard busy/locked
            throw new ClipboardAccessException("Clipboard is currently unavailable (locked).", e);
        } catch (Throwable t) {
            throw new ClipboardAccessException("Unexpected clipboard error while writing.", t);
        }
    }

    /**
     * Windows-native clipboard writer that sets CF_UNICODETEXT and FileMaker custom formats
     * ('Mac-XMSS' and 'Mac-XMFD') in a single OpenClipboard/EmptyClipboard session via JNA,
     * to better match FileMaker. Returns true if the native path succeeded; false to allow
     * fallback to AWT/CopyPasteManager.
     */
    private boolean tryWindowsNativeWrite(String text) {
        // PHASE 1.1 REVIEW NOTE (2025-11-22):
        // This method intentionally mirrors how FileMaker exposes clipboard data on Windows:
        // 1) Always publish CF_UNICODETEXT (format id 13) containing a UTF-16LE, NUL-terminated string.
        //    - No BOM is included for CF_UNICODETEXT (Windows convention for this format).
        //    - The terminator is a 16-bit 0x0000 (i.e., two trailing zero bytes).
        // 2) Additionally publish exactly one FileMaker custom format, depending on fmxmlsnippet type:
        //    - "Mac-XMSS" for Script Steps
        //    - "Mac-XMFD" for Field/Table definitions
        //    Payload details for custom formats:
        //    - Encoding: UTF-8 WITH BOM (EF BB BF)
        //    - Newlines: classic Mac CR (\r), so we normalize any LF/CRLF to CR
        //    - Terminator: a single trailing NUL byte (0x00)
        // The two formats are set within a single OpenClipboard/EmptyClipboard session.
        String os = System.getProperty("os.name", "");
        if (os == null || !os.toLowerCase().startsWith("windows")) return false;
        ensureJna();
        if (!jnaAvailable) return false;

        // Detect fmxmlsnippet content type for selecting correct FileMaker clipboard flavor
        final boolean isScript = text != null && text.contains("<Step");
        final boolean isField = text != null && (text.contains("<Field") || text.contains("<FieldDefinition"));
        if (!isScript && !isField) {
            // Unknown content — fall back to generic multi-flavor AWT writer
            Diagnostics.vInfo(LOG, "Native write skipped: unknown fmxmlsnippet type");
            return false;
        }

        // Guard extremely large payloads to avoid excessive allocations
        final byte[] utf16 = utf16leNullTerminated(text);
        // For FileMaker's custom Mac-* formats, normalize newlines to classic Mac CR ("\r").
        final String customPayload = normalizeToClassicMacNewlines(text);
        // FileMaker's custom Mac-* formats require UTF-8 with BOM and a trailing NUL terminator.
        final byte[] fmCustom = utf8BomNullTerminated(customPayload);

        // Diagnostics for Phase 1.1: verify BOMs and terminators at runtime when verbose logging is enabled.
        if (Diagnostics.isVerbose()) {
            // CF_UNICODETEXT: should NOT include BOM by spec; should end with two NUL bytes.
            boolean utf16HasBom = utf16.length >= 2 && (utf16[0] == (byte)0xFF && utf16[1] == (byte)0xFE);
            boolean utf16HasNullTerm = utf16.length >= 2 && (utf16[utf16.length - 1] == 0x00) && (utf16[utf16.length - 2] == 0x00);
            LOG.info("[CB-DIAG] CF_UNICODETEXT: hasBOM=" + utf16HasBom + " (expected=false), hasNullTerm=" + utf16HasNullTerm + ", size=" + utf16.length);

            // Custom UTF-8: must start with EF BB BF and end with single NUL.
            boolean customHasBom = fmCustom.length >= 3 && ((fmCustom[0] & 0xFF) == 0xEF) && ((fmCustom[1] & 0xFF) == 0xBB) && ((fmCustom[2] & 0xFF) == 0xBF);
            boolean customHasNullTerm = fmCustom.length >= 1 && (fmCustom[fmCustom.length - 1] == 0x00);
            // Also sample first/last few bytes to aid hex inspection
            StringBuilder head = new StringBuilder();
            for (int i = 0; i < Math.min(8, fmCustom.length); i++) head.append(String.format("%02X ", fmCustom[i] & 0xFF));
            StringBuilder tail = new StringBuilder();
            for (int i = Math.max(0, fmCustom.length - 4); i < fmCustom.length; i++) tail.append(String.format("%02X ", fmCustom[i] & 0xFF));
            LOG.info("[CB-DIAG] FM-CUSTOM: hasBOM=" + customHasBom + " (expected=true), hasNullTerm=" + customHasNullTerm + ", size=" + fmCustom.length + ", head=" + head + ", tail=" + tail);
        }
        final long MAX = 10L * 1024 * 1024; // 10 MB cap per format
        if (utf16.length > MAX || fmCustom.length > MAX) {
            LOG.info("[CB] Native path: payload too large for native write; falling back");
            return false;
        }

        boolean opened = false;
        try {
            for (int i = 0; i < 8; i++) {
                opened = User32.INSTANCE.OpenClipboard(null);
                if (opened) break;
                try { Thread.sleep(10); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            if (!opened) {
                LOG.info("[CB] Native path: OpenClipboard failed/busy (write)");
                return false;
            }

            // Take ownership and clear existing content first
            if (!User32.INSTANCE.EmptyClipboard()) {
                LOG.info("[CB] Native path: EmptyClipboard failed");
                return false;
            }

            // Register only the specific FileMaker format matching the content type
            int targetFormatId = 0;
            String targetFormatName = null;
            if (isScript) {
                targetFormatName = "Mac-XMSS"; // Script Steps
            } else if (isField) {
                targetFormatName = "Mac-XMFD"; // Fields/Tables
            }
            if (targetFormatName != null) {
                targetFormatId = User32.INSTANCE.RegisterClipboardFormat(targetFormatName);
            }
            if (targetFormatId == 0) {
                LOG.info("[CB] Native path: RegisterClipboardFormat failed for target FileMaker format (" + targetFormatName + ")");
                // We'll still proceed with CF_UNICODETEXT so paste as text works, but fail overall to allow fallback
            }

            boolean unicodeOk = false;
            boolean customOk = false;

            // Set CF_UNICODETEXT (13) — null-terminated UTF-16LE
            WinHandle hUtf16 = globalAllocAndWrite(utf16);
            if (hUtf16 == null) {
                LOG.info("[CB] Native path: GlobalAlloc failed for CF_UNICODETEXT");
                unicodeOk = false;
            } else {
                com.sun.jna.platform.win32.WinNT.HANDLE out = User32.INSTANCE.SetClipboardData(13, hUtf16.handle);
                if (out == null) {
                    // On failure, we still own the HGLOBAL and must free it
                    Kernel32.INSTANCE.GlobalFree(hUtf16.handle);
                    LOG.info("[CB] Native path: SetClipboardData(CF_UNICODETEXT) failed");
                    unicodeOk = false;
                } else {
                    unicodeOk = true;
                }
            }

            // Set the single target custom format if registered
            if (targetFormatId != 0) {
                WinHandle hCustom = globalAllocAndWrite(fmCustom);
                if (hCustom == null) {
                    LOG.info("[CB] Native path: GlobalAlloc failed for " + targetFormatName);
                } else {
                    com.sun.jna.platform.win32.WinNT.HANDLE out = User32.INSTANCE.SetClipboardData(targetFormatId, hCustom.handle);
                    if (out == null) {
                        Kernel32.INSTANCE.GlobalFree(hCustom.handle);
                        LOG.info("[CB] Native path: SetClipboardData(" + targetFormatName + ") failed");
                    } else {
                        customOk = true;
                    }
                }
            }

            long crc = 0L;
            try {
                java.util.zip.CRC32 c = new java.util.zip.CRC32();
                c.update(fmCustom);
                crc = c.getValue();
            } catch (Throwable ignore) {}
            Diagnostics.vInfo(LOG, "Native write: CF_UNICODETEXT=" + (unicodeOk ? "ok" : "fail")
                    + ", target=" + (targetFormatName == null ? "n/a" : targetFormatName) + "=" + (customOk ? "ok" : (targetFormatId == 0 ? "n/a" : "fail"))
                    + ", sizes: utf16=" + utf16.length + ", custom=" + fmCustom.length + ", custom.hasBom=true, custom.crc32=0x" + Long.toHexString(crc));

            // Consider it a success only if CF_UNICODETEXT and the target custom format were set
            return unicodeOk && customOk;
        } catch (Throwable t) {
            LOG.info("[CB] Native path: write failed: " + t.getClass().getSimpleName());
            return false;
        } finally {
            if (opened) {
                try { User32.INSTANCE.CloseClipboard(); } catch (Throwable ignore) {}
            }
        }
    }

    private static byte[] utf16leNullTerminated(String s) {
        byte[] data = (s == null ? "" : s).getBytes(StandardCharsets.UTF_16LE);
        byte[] out = new byte[data.length + 2];
        System.arraycopy(data, 0, out, 0, data.length);
        // last two bytes already zero
        return out;
    }

    private static byte[] utf8NullTerminated(String s) {
        byte[] data = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[data.length + 1];
        System.arraycopy(data, 0, out, 0, data.length);
        // last byte zero
        return out;
    }

    // UTF-8 with BOM (EF BB BF) and a trailing NUL terminator
    private static byte[] utf8BomNullTerminated(String s) {
        byte[] data = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[data.length + 1 /*NUL*/ + 3 /*BOM*/];
        // BOM
        out[0] = (byte)0xEF;
        out[1] = (byte)0xBB;
        out[2] = (byte)0xBF;
        // payload
        System.arraycopy(data, 0, out, 3, data.length);
        // trailing NUL is already 0 at out[out.length-1]
        return out;
    }

    // Normalize any mix of CRLF/CR/LF to classic Mac CR newlines for custom Mac-* formats.
    private static String normalizeToClassicMacNewlines(String s) {
        if (s == null || s.isEmpty()) return "";
        String tmp = s.replace("\r\n", "\n");
        tmp = tmp.replace("\r", "\n");
        return tmp.replace("\n", "\r");
    }

    // Small holder to track HGLOBAL while deciding whether we need to free
    private static final class WinHandle {
        final com.sun.jna.platform.win32.WinNT.HANDLE handle;
        WinHandle(com.sun.jna.platform.win32.WinNT.HANDLE h) { this.handle = h; }
    }

    private WinHandle globalAllocAndWrite(byte[] bytes) {
        // GMEM_MOVEABLE = 0x0002
        int GMEM_MOVEABLE = 0x0002;
        com.sun.jna.platform.win32.WinNT.HANDLE h = Kernel32.INSTANCE.GlobalAlloc(GMEM_MOVEABLE, new com.sun.jna.platform.win32.BaseTSD.SIZE_T(bytes.length));
        if (h == null) return null;
        com.sun.jna.Pointer p = Kernel32.INSTANCE.GlobalLock(h);
        if (p == null) {
            try { Kernel32.INSTANCE.GlobalFree(h); } catch (Throwable ignore) {}
            return null;
        }
        try {
            p.write(0, bytes, 0, bytes.length);
        } finally {
            try { Kernel32.INSTANCE.GlobalUnlock(h); } catch (Throwable ignore) {}
        }
        return new WinHandle(h);
    }

    // --- Diagnostics helpers: Windows clipboard formats dump (when verbose enabled) ---
    private static volatile boolean jnaChecked = false;
    private static volatile boolean jnaAvailable = false;

    private void maybeDumpClipboardFormats(String phase) {
        if (!Diagnostics.isVerbose()) return;
        String os = System.getProperty("os.name", "");
        if (os == null || !os.toLowerCase().startsWith("windows")) return;
        ensureJna();
        if (!jnaAvailable) {
            LOG.info("[CB-DIAG] (" + phase + ") JNA not available; skip formats dump");
            return;
        }
        boolean opened = false;
        try {
            for (int i = 0; i < 5; i++) {
                opened = User32.INSTANCE.OpenClipboard(null);
                if (opened) break;
                try { Thread.sleep(10); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            if (!opened) {
                LOG.info("[CB-DIAG] (" + phase + ") OpenClipboard failed/busy");
                return;
            }
            int id = 0;
            int count = 0;
            StringBuilder sb = new StringBuilder();
            sb.append("[CB-DIAG] (").append(phase).append(") formats: ");
            while (true) {
                id = User32.INSTANCE.EnumClipboardFormats(id);
                if (id == 0) break;
                String name = getFormatName(id);
                Long size = null;
                try {
                    com.sun.jna.platform.win32.WinNT.HANDLE h = User32.INSTANCE.GetClipboardData(id);
                    if (h != null) {
                        size = Kernel32.INSTANCE.GlobalSize(h).longValue();
                    }
                } catch (Throwable ignore) {}
                if (count > 0) sb.append(" | ");
                sb.append(id);
                if (name != null) sb.append(":").append(name);
                if (size != null) sb.append(" (size=").append(size).append(")");
                count++;
                if (count >= 64) { sb.append(" ..."); break; }
            }
            if (count == 0) sb.append("<none>");
            LOG.info(sb.toString());
        } catch (Throwable t) {
            LOG.info("[CB-DIAG] (" + phase + ") dump failed: " + t.getClass().getSimpleName());
        } finally {
            if (opened) {
                try { User32.INSTANCE.CloseClipboard(); } catch (Throwable ignore) {}
            }
        }
    }

    private void ensureJna() {
        if (jnaChecked) return;
        synchronized (DefaultClipboardService.class) {
            if (jnaChecked) return;
            try {
                Class.forName("com.sun.jna.Native");
                jnaAvailable = true;
            } catch (Throwable t) {
                jnaAvailable = false;
            }
            jnaChecked = true;
        }
    }

    private static String getFormatName(int id) {
        try {
            char[] buf = new char[128];
            int n = User32.INSTANCE.GetClipboardFormatName(id, buf, buf.length);
            if (n > 0) return new String(buf, 0, n);
        } catch (Throwable ignore) {}
        return null;
    }

    // Minimal JNA interfaces (duplicated locally to avoid public deps)
    private interface User32 extends com.sun.jna.win32.StdCallLibrary {
        User32 INSTANCE = com.sun.jna.Native.load("user32", User32.class, com.sun.jna.win32.W32APIOptions.DEFAULT_OPTIONS);
        boolean OpenClipboard(com.sun.jna.platform.win32.WinDef.HWND hWndNewOwner);
        boolean CloseClipboard();
        boolean EmptyClipboard();
        int EnumClipboardFormats(int formatId);
        int GetClipboardFormatName(int formatId, char[] buffer, int cchMax);
        int RegisterClipboardFormat(String lpString);
        com.sun.jna.platform.win32.WinNT.HANDLE SetClipboardData(int uFormat, com.sun.jna.platform.win32.WinNT.HANDLE hMem);
        com.sun.jna.platform.win32.WinNT.HANDLE GetClipboardData(int uFormat);
    }
    private interface Kernel32 extends com.sun.jna.win32.StdCallLibrary {
        Kernel32 INSTANCE = com.sun.jna.Native.load("kernel32", Kernel32.class, com.sun.jna.win32.W32APIOptions.DEFAULT_OPTIONS);
        com.sun.jna.Pointer GlobalLock(com.sun.jna.platform.win32.WinNT.HANDLE hMem);
        boolean GlobalUnlock(com.sun.jna.platform.win32.WinNT.HANDLE hMem);
        com.sun.jna.platform.win32.WinNT.HANDLE GlobalAlloc(int uFlags, com.sun.jna.platform.win32.BaseTSD.SIZE_T dwBytes);
        com.sun.jna.platform.win32.WinNT.HANDLE GlobalFree(com.sun.jna.platform.win32.WinNT.HANDLE hMem);
        com.sun.jna.platform.win32.BaseTSD.SIZE_T GlobalSize(com.sun.jna.platform.win32.WinNT.HANDLE hMem);
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
