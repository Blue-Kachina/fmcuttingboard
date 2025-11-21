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
 * Only active on Windows; no heavy reads, just IDs, names and light size when available.
 */
public class ClipboardFormatsDumpAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ClipboardFormatsDumpAction.class);

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
            }
            if (count == 0) {
                LOG.info("[CB-DUMP] No clipboard formats enumerated");
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
}
