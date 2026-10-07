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

import static dev.fmcuttingboard.clipboard.FmClipboardCodec.decodeBytesWithBomHeuristics;
import static dev.fmcuttingboard.clipboard.FmClipboardCodec.extractFmxmlFromBytes;
import static dev.fmcuttingboard.clipboard.FmClipboardCodec.stripNulls;

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
        if (uni.isPresent() && !uni.get().isBlank()) return uni.map(FmClipboardCodec::stripNulls).map(String::trim).filter(s -> !s.isBlank());
        Optional<String> ansi = readFormat(CF_TEXT);
        if (ansi.isPresent() && !ansi.get().isBlank()) return ansi.map(FmClipboardCodec::stripNulls).map(String::trim).filter(s -> !s.isBlank());
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

            // First pass: find known FileMaker-specific formats by name (Mac-XMSS, Mac-XML2, …; see
            // shared/data/clipboard-formats.json) and try them immediately
            int id = 0;
            boolean any = false;
            while (true) {
                id = User32.INSTANCE.EnumClipboardFormats(id);
                if (id == 0) break;
                any = true;
                String name = getFormatName(id);
                if (ClipboardFormats.isFileMakerWindowsFormat(name)) {
                    LOG.info("[CB] Native path: probing known format id=" + id + ", name='" + name + "'");
                    Optional<String> result = tryReadFormatBytesAndExtract(id, name);
                    if (result.isPresent()) return result;
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
