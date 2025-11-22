package dev.fmcuttingboard.clipboard;

import com.intellij.openapi.diagnostic.Logger;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.nio.charset.Charset;
import java.util.Optional;

/**
 * Windows-native clipboard reader using JNA with correct stdcall mappings.
 * Attempts CF_UNICODETEXT first, then CF_TEXT.
 */
class WindowsClipboardReader implements NativeClipboardReader {

    private static final Logger LOG = Logger.getInstance(WindowsClipboardReader.class);

    // Clipboard format constants
    private static final int CF_TEXT = 1;
    private static final int CF_UNICODETEXT = 13;

    // Retry settings for busy clipboard (per plan)
    private static final int OPEN_RETRIES = 5;
    private static final int OPEN_RETRY_DELAY_MS = 60;

    @Override
    public Optional<String> read() {
        // Try CF_UNICODETEXT then CF_TEXT
        Optional<String> uni = readFormat(CF_UNICODETEXT);
        if (uni.isPresent() && !uni.get().isBlank()) return uni.map(WindowsClipboardReader::stripNulls).map(String::trim).filter(s -> !s.isBlank());
        Optional<String> ansi = readFormat(CF_TEXT);
        if (ansi.isPresent() && !ansi.get().isBlank()) return ansi.map(WindowsClipboardReader::stripNulls).map(String::trim).filter(s -> !s.isBlank());
        // If neither simple text format helped, enumerate all formats and probe bytes for fmxmlsnippet
        Optional<String> fromFormats = enumerateAndProbeFormats();
        if (fromFormats.isPresent()) return fromFormats;
        return Optional.empty();
    }

    private Optional<String> readFormat(int format) {
        boolean opened = false;
        try {
            // Retry OpenClipboard if busy
            for (int i = 0; i < OPEN_RETRIES; i++) {
                opened = User32.INSTANCE.OpenClipboard(null);
                if (opened) break;
                try { Thread.sleep(OPEN_RETRY_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            if (!opened) {
                LOG.info("[CB] Native path: OpenClipboard failed/busy");
                return Optional.empty();
            }

            boolean available = false;
            try {
                available = User32.INSTANCE.IsClipboardFormatAvailable(format);
                LOG.info("[CB] Native path: IsClipboardFormatAvailable(" + format + ")=" + available);
            } catch (Throwable ignore) { }
            if (!available) return Optional.empty();

            WinNT.HANDLE hData = User32.INSTANCE.GetClipboardData(format);
            if (hData == null) {
                LOG.info("[CB] Native path: GetClipboardData(" + format + ") returned null despite availability");
                return Optional.empty();
            }

            Pointer ptr = Kernel32.INSTANCE.GlobalLock(hData);
            if (ptr == null) {
                return Optional.empty();
            }
            try {
                String s;
                if (format == CF_UNICODETEXT) {
                    // Read as wide string (null-terminated)
                    s = ptr.getWideString(0);
                } else {
                    // CF_TEXT: ANSI bytes to String with platform default charset
                    s = ptr.getString(0, Charset.defaultCharset().name());
                }
                if (s == null) return Optional.empty();
                s = stripNulls(s);
                return Optional.ofNullable(s);
            } finally {
                Kernel32.INSTANCE.GlobalUnlock(hData);
            }
        } catch (Throwable t) {
            LOG.info("[CB] Native path: readFormat(" + format + ") failed: " + t.getClass().getSimpleName());
            return Optional.empty();
        } finally {
            if (opened) {
                try {
                    User32.INSTANCE.CloseClipboard();
                } catch (Throwable ignore) {
                    // ignore
                }
            }
        }
    }

    private Optional<String> enumerateAndProbeFormats() {
        boolean opened = false;
        try {
            for (int i = 0; i < OPEN_RETRIES; i++) {
                opened = User32.INSTANCE.OpenClipboard(null);
                if (opened) break;
                try { Thread.sleep(OPEN_RETRY_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            if (!opened) {
                LOG.info("[CB] Native path: OpenClipboard failed/busy (enum)");
                return Optional.empty();
            }

            // First pass: find known FileMaker-specific formats by name and try them immediately
            // Known variants observed in the wild include:
            //  - Mac-XMSS (common)
            //  - Mac-XMFD (seen in user logs)
            final String[] interestingNames = new String[] { "Mac-XMSS", "Mac-XMFD" };
            int id = 0;
            boolean any = false;
            while (true) {
                id = User32.INSTANCE.EnumClipboardFormats(id);
                if (id == 0) break;
                any = true;
                String name = getFormatName(id);
                if (name != null) {
                    for (String target : interestingNames) {
                        if (name.equalsIgnoreCase(target)) {
                            LOG.info("[CB] Native path: probing known format id=" + id + ", name='" + name + "'");
                            Optional<String> result = tryReadFormatBytesAndExtract(id, name);
                            if (result.isPresent()) return result;
                        }
                    }
                }
            }

            // Second pass: generic enumeration and probing with limited logging
            int countLogged = 0;
            id = 0;
            while (true) {
                id = User32.INSTANCE.EnumClipboardFormats(id);
                if (id == 0) break;
                String name = getFormatName(id);
                if (countLogged < 32) {
                    LOG.info("[CB] Native path: format id=" + id + (name == null ? "" : ", name='" + name + "'"));
                    countLogged++;
                }
                Optional<String> result = tryReadFormatBytesAndExtract(id, name);
                if (result.isPresent()) return result;
            }
            if (!any) {
                LOG.info("[CB] Native path: no clipboard formats enumerated");
            }
            return Optional.empty();
        } catch (Throwable t) {
            LOG.info("[CB] Native path: enumeration failed: " + t.getClass().getSimpleName());
            return Optional.empty();
        } finally {
            if (opened) {
                try { User32.INSTANCE.CloseClipboard(); } catch (Throwable ignore) {}
            }
        }
    }

    private Optional<String> tryReadFormatBytesAndExtract(int id, String name) {
        // Try to fetch data for this format and search fmxmlsnippet
        WinNT.HANDLE hData = User32.INSTANCE.GetClipboardData(id);
        if (hData == null) return Optional.empty();
        Pointer ptr = Kernel32.INSTANCE.GlobalLock(hData);
        if (ptr == null) return Optional.empty();
        try {
            long size = 0L;
            try {
                size = Kernel32.INSTANCE.GlobalSize(hData).longValue();
            } catch (Throwable ignore) { }
            final long MAX = 10L * 1024 * 1024; // 10 MB cap
            byte[] bytes;
            if (size > 0 && size <= MAX) {
                int len = (int) size;
                bytes = new byte[len];
                ptr.read(0, bytes, 0, len);
            } else if (size == 0) {
                // Conservative fixed-window reads when size is unknown/zero
                int[] windows = new int[] { 512, 2048, 8192, 65536 };
                bytes = null;
                for (int w : windows) {
                    try {
                        byte[] probe = new byte[w];
                        ptr.read(0, probe, 0, w);
                        bytes = probe;
                        break;
                    } catch (Throwable ignore) {
                        // try next window size
                    }
                }
                if (bytes == null) return Optional.empty();
            } else {
                if (size > MAX) LOG.info("[CB] Native path: skipping format id=" + id + " size=" + size + " (>10MB)");
                return Optional.empty();
            }

            // Decode text heuristically
            String decoded = decodeBytesWithBomHeuristics(bytes);
            if (decoded != null && !decoded.isBlank()) {
                boolean contains = decoded.toLowerCase().contains("<fmxmlsnippet");
                if (contains) {
                    LOG.info("[CB] Native path: fmxmlsnippet detected in decoded text for id=" + id + (name == null ? "" : ", name='" + name + "'"));
                    return Optional.of(decoded);
                }
            }
            // Raw snippet extraction
            String snippet = extractFmxmlFromBytes(bytes);
            if (snippet != null && !snippet.isBlank()) {
                LOG.info("[CB] Native path: fmxmlsnippet extracted from format id=" + id + (name == null ? "" : ", name='" + name + "'"));
                return Optional.of(snippet);
            }
            return Optional.empty();
        } catch (Throwable ignore) {
            return Optional.empty();
        } finally {
            try { Kernel32.INSTANCE.GlobalUnlock(hData); } catch (Throwable ignore2) {}
        }
    }

    private String getFormatName(int id) {
        try {
            char[] buf = new char[128];
            int n = User32.INSTANCE.GetClipboardFormatName(id, buf, buf.length);
            if (n > 0) {
                return new String(buf, 0, n);
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    // Minimal decoding/scan helpers (mirrors DefaultClipboardService logic)
    private static String decodeBytesWithBomHeuristics(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return stripNulls(new String(bytes, 3, bytes.length - 3, java.nio.charset.StandardCharsets.UTF_8));
        }
        if (bytes.length >= 2) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            if (b0 == 0xFE && b1 == 0xFF) {
                return stripNulls(new String(bytes, 2, bytes.length - 2, java.nio.charset.StandardCharsets.UTF_16BE));
            }
            if (b0 == 0xFF && b1 == 0xFE) {
                return stripNulls(new String(bytes, 2, bytes.length - 2, java.nio.charset.StandardCharsets.UTF_16LE));
            }
        }
        int zerosEven = 0, zerosOdd = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                if ((i & 1) == 0) zerosEven++; else zerosOdd++;
            }
        }
        int threshold = Math.max(2, bytes.length / 10);
        if (zerosOdd > zerosEven && zerosOdd >= threshold) {
            return stripNulls(new String(bytes, java.nio.charset.StandardCharsets.UTF_16BE));
        } else if (zerosEven > zerosOdd && zerosEven >= threshold) {
            return stripNulls(new String(bytes, java.nio.charset.StandardCharsets.UTF_16LE));
        }
        return stripNulls(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String extractFmxmlFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        int startUtf8 = indexOf(bytes, ascii("<fmxmlsnippet"));
        if (startUtf8 >= 0) {
            int endUtf8 = lastIndexOf(bytes, ascii("</fmxmlsnippet>"));
            if (endUtf8 >= 0 && endUtf8 >= startUtf8) {
                int endPos = endUtf8 + ascii("</fmxmlsnippet>").length;
                return new String(bytes, startUtf8, endPos - startUtf8, java.nio.charset.StandardCharsets.UTF_8).trim();
            }
        }
        byte[] startLe = utf16le("<fmxmlsnippet");
        byte[] endLe = utf16le("</fmxmlsnippet>");
        int start16le = indexOf(bytes, startLe);
        if (start16le >= 0) {
            int end16le = lastIndexOf(bytes, endLe);
            if (end16le >= 0 && end16le >= start16le) {
                int endPos = end16le + endLe.length;
                String s = new String(bytes, start16le, endPos - start16le, java.nio.charset.StandardCharsets.UTF_16LE);
                return stripNulls(s).trim();
            }
        }
        byte[] startBe = utf16be("<fmxmlsnippet");
        byte[] endBe = utf16be("</fmxmlsnippet>");
        int start16be = indexOf(bytes, startBe);
        if (start16be >= 0) {
            int end16be = lastIndexOf(bytes, endBe);
            if (end16be >= 0 && end16be >= start16be) {
                int endPos = end16be + endBe.length;
                String s = new String(bytes, start16be, endPos - start16be, java.nio.charset.StandardCharsets.UTF_16BE);
                return stripNulls(s).trim();
            }
        }
        return null;
    }

    private static byte[] ascii(String s) { return s.getBytes(java.nio.charset.StandardCharsets.US_ASCII); }
    private static byte[] utf16le(String s) { return s.getBytes(java.nio.charset.StandardCharsets.UTF_16LE); }
    private static byte[] utf16be(String s) { return s.getBytes(java.nio.charset.StandardCharsets.UTF_16BE); }
    private static int indexOf(byte[] data, byte[] pattern) {
        if (pattern.length == 0) return 0;
        outer: for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) { if (data[i + j] != pattern[j]) continue outer; }
            return i;
        }
        return -1;
    }
    private static int lastIndexOf(byte[] data, byte[] pattern) {
        if (pattern.length == 0) return data.length;
        outer: for (int i = data.length - pattern.length; i >= 0; i--) {
            for (int j = 0; j < pattern.length; j++) { if (data[i + j] != pattern[j]) continue outer; }
            return i;
        }
        return -1;
    }

    private static String stripNulls(String s) {
        return s == null ? null : s.replace("\u0000", "");
    }

    /** User32 with stdcall and default W32 options. */
    interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

        boolean OpenClipboard(WinDef.HWND hWndNewOwner);
        boolean CloseClipboard();
        boolean IsClipboardFormatAvailable(int format);
        WinNT.HANDLE GetClipboardData(int uFormat);
        int EnumClipboardFormats(int formatId);
        int GetClipboardFormatName(int formatId, char[] buffer, int cchMax);
    }

    /** Kernel32 with stdcall and default W32 options. */
    interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);

        Pointer GlobalLock(WinNT.HANDLE hMem);
        boolean GlobalUnlock(WinNT.HANDLE hMem);
        com.sun.jna.platform.win32.BaseTSD.SIZE_T GlobalSize(WinNT.HANDLE hMem);
    }
}
