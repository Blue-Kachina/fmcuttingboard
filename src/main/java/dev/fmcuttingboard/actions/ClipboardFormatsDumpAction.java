package dev.fmcuttingboard.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * Tools menu action that dumps Windows clipboard formats to the IDE log with [CB-DUMP] prefix.
 * Additionally, for FileMaker-related formats (Mac-XMSS/Mac-XMFD), this will extract raw bytes,
 * perform a quick encoding/newline/BOM analysis, and write a report to the project under
 * docs/FileMaker-Native-Clipboard-Analysis.md when a project is available. This supports Phase 1.2.
 */
public class ClipboardFormatsDumpAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ClipboardFormatsDumpAction.class);
    private static final String[] INTERESTING_NAMES = new String[] {"Mac-XMSS", "Mac-XMFD"};

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String os = System.getProperty("os.name", "");
        if (os == null || !os.toLowerCase().startsWith("windows")) {
            LOG.info("[CB-DUMP] Not a Windows OS; skipping clipboard dump");
            return;
        }
        try {
            // Ensure JNA is present
            Class.forName("com.sun.jna.Native");
        } catch (Throwable t) {
            LOG.info("[CB-DUMP] JNA not present; cannot dump clipboard formats: " + t.getClass().getSimpleName());
            return;
        }

        boolean opened = false;
        StringBuilder analysis = new StringBuilder();
        try {
            for (int i = 0; i < 5; i++) {
                opened = User32.INSTANCE.OpenClipboard(null);
                if (opened) break;
                try { Thread.sleep(50); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            if (!opened) {
                LOG.info("[CB-DUMP] OpenClipboard failed/busy");
                return;
            }
            int id = 0;
            int count = 0;
            while (true) {
                id = User32.INSTANCE.EnumClipboardFormats(id);
                if (id == 0) break;
                String name = getFormatName(id);
                // Attempt very light size probe via GlobalSize if we can get a handle
                Long size = null;
                try {
                    WinNT.HANDLE h = User32.INSTANCE.GetClipboardData(id);
                    if (h != null) {
                        size = Kernel32.INSTANCE.GlobalSize(h).longValue();
                    }
                } catch (Throwable ignore) {}
                LOG.info("[CB-DUMP] format id=" + id + (name == null ? "" : ", name='" + name + "'") + (size == null ? "" : ", size=" + size));
                count++;
                if (count >= 64) break; // avoid excessive logs

                // If this format looks like a FileMaker custom type, extract/analyze bytes
                if (name != null && isInteresting(name)) {
                    try {
                        byte[] bytes = readAllBytes(id);
                        if (bytes != null) {
                            String section = analyzeBytesSection(id, name, bytes);
                            LOG.info(section.replace("\n", " ")); // compact info into log line
                            analysis.append(section).append("\n\n");
                        }
                    } catch (Throwable t) {
                        LOG.info("[CB-ANALYZE] Failed reading/analyzing id=" + id + ", name='" + name + "': " + t.getClass().getSimpleName());
                    }
                }
            }
            if (count == 0) {
                LOG.info("[CB-DUMP] No clipboard formats enumerated");
            }
            // Write analysis report if we gathered anything
            if (analysis.length() > 0) {
                writeAnalysisReport(e, analysis.toString());
            }
        } catch (Throwable t) {
            LOG.info("[CB-DUMP] Dump failed: " + t.getClass().getSimpleName());
        } finally {
            if (opened) {
                try { User32.INSTANCE.CloseClipboard(); } catch (Throwable ignore) {}
            }
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

    interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);
        boolean OpenClipboard(WinDef.HWND hWndNewOwner);
        boolean CloseClipboard();
        int EnumClipboardFormats(int formatId);
        int GetClipboardFormatName(int formatId, char[] buffer, int cchMax);
        WinNT.HANDLE GetClipboardData(int uFormat);
    }

    interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);
        Pointer GlobalLock(WinNT.HANDLE hMem);
        boolean GlobalUnlock(WinNT.HANDLE hMem);
        com.sun.jna.platform.win32.BaseTSD.SIZE_T GlobalSize(WinNT.HANDLE hMem);
    }

    private static boolean isInteresting(String name) {
        for (String s : INTERESTING_NAMES) if (s.equalsIgnoreCase(name)) return true;
        return false;
    }

    private static byte[] readAllBytes(int formatId) {
        try {
            WinNT.HANDLE h = User32.INSTANCE.GetClipboardData(formatId);
            if (h == null) return null;
            Pointer p = Kernel32.INSTANCE.GlobalLock(h);
            if (p == null) return null;
            try {
                long size = 0L;
                try { size = Kernel32.INSTANCE.GlobalSize(h).longValue(); } catch (Throwable ignore) {}
                final long MAX = 10L * 1024 * 1024;
                if (size > 0 && size <= MAX) {
                    byte[] bytes = new byte[(int) size];
                    p.read(0, bytes, 0, (int) size);
                    return bytes;
                } else if (size == 0) {
                    int[] windows = new int[] { 4096, 32768, 262144 };
                    for (int w : windows) {
                        try {
                            byte[] probe = new byte[w];
                            p.read(0, probe, 0, w);
                            return probe;
                        } catch (Throwable ignore) {}
                    }
                    return null;
                } else {
                    return null;
                }
            } finally {
                try { Kernel32.INSTANCE.GlobalUnlock(h); } catch (Throwable ignore) {}
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static String analyzeBytesSection(int id, String name, byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("[CB-ANALYZE] id=").append(id).append(", name='").append(name).append("'\n");
        sb.append("  size=").append(bytes.length).append(" bytes\n");
        // BOM detection
        String bom = detectBom(bytes);
        sb.append("  BOM=").append(bom).append("\n");
        // Encoding guess
        String enc = guessEncoding(bytes, bom);
        sb.append("  encodingGuess=").append(enc).append("\n");
        // Newline style counts
        int cr = count(bytes, (byte) '\r');
        int lf = count(bytes, (byte) '\n');
        int crlf = countCrlf(bytes);
        sb.append("  newlines: CR=").append(cr).append(", LF=").append(lf).append(", CRLF=").append(crlf).append("\n");
        // Null terminator check (last byte(s))
        boolean endsNull = bytes.length > 0 && bytes[bytes.length - 1] == 0x00;
        sb.append("  endsWithNull=").append(endsNull).append("\n");
        // Hex preview and fmxml snippet preview
        sb.append("  hexPreview=").append(hexPreview(bytes, 64)).append("\n");
        String preview = safePreview(bytes, enc);
        if (preview != null) sb.append("  textPreview=").append(preview).append("\n");
        return sb.toString();
    }

    private static String detectBom(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xEF && (b[1] & 0xFF) == 0xBB && (b[2] & 0xFF) == 0xBF) return "UTF-8";
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFE) return "UTF-16LE";
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFE && (b[1] & 0xFF) == 0xFF) return "UTF-16BE";
        return "none";
    }

    private static String guessEncoding(byte[] b, String bom) {
        try {
            switch (bom) {
                case "UTF-8": return "UTF-8";
                case "UTF-16LE": return "UTF-16LE";
                case "UTF-16BE": return "UTF-16BE";
            }
            // Heuristic: if every other byte is 0x00, guess UTF-16LE "ASCII"
            int zeros = 0;
            for (int i = 1; i < Math.min(b.length, 256); i += 2) if (b[i] == 0x00) zeros++;
            if (zeros > 64) return "UTF-16LE?";
        } catch (Throwable ignore) {}
        return "unknown";
    }

    private static int count(byte[] b, byte value) {
        int c = 0; for (byte x : b) if (x == value) c++; return c;
    }

    private static int countCrlf(byte[] b) {
        int c = 0; for (int i = 0; i + 1 < b.length; i++) if (b[i] == '\r' && b[i+1] == '\n') c++; return c;
    }

    private static String hexPreview(byte[] b, int max) {
        int n = Math.min(b.length, max);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02X", b[i]));
        }
        if (b.length > n) sb.append(" …");
        return sb.toString();
    }

    private static String safePreview(byte[] bytes, String enc) {
        try {
            String charset = enc != null && enc.startsWith("UTF-16") ? enc : "UTF-8";
            String s = new String(bytes, charset);
            int idx = s.toLowerCase().indexOf("<fmxmlsnippet");
            if (idx >= 0) {
                int end = Math.min(s.length(), idx + 120);
                String sub = s.substring(idx, end).replaceAll("[\r\n]+", "\\n");
                return sub;
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void writeAnalysisReport(AnActionEvent e, String content) {
        java.nio.file.Path out;
        try {
            String base = e.getProject() != null ? e.getProject().getBasePath() : null;
            if (base != null) {
                out = java.nio.file.Paths.get(base, "docs", "FileMaker-Native-Clipboard-Analysis.md");
            } else {
                out = java.nio.file.Paths.get(System.getProperty("user.home"), "FileMaker-Native-Clipboard-Analysis.md");
            }
            java.nio.file.Files.createDirectories(out.getParent());
            String header = "# FileMaker Native Clipboard Analysis\n\n" +
                    "Generated: " + java.time.ZonedDateTime.now() + "\n\n";
            String body = "## Capture\n\n" + content + "\n";
            if (java.nio.file.Files.exists(out)) {
                java.nio.file.Files.writeString(out, "\n\n" + body, java.nio.charset.StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
            } else {
                java.nio.file.Files.writeString(out, header + body, java.nio.charset.StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE);
            }
            LOG.info("[CB-ANALYZE] Wrote analysis report to " + out);
        } catch (Throwable t) {
            LOG.info("[CB-ANALYZE] Failed to write analysis report: " + t.getClass().getSimpleName());
        }
    }
}
