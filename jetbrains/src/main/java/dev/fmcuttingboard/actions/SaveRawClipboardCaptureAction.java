package dev.fmcuttingboard.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import dev.fmcuttingboard.clipboard.RawClipboardCapture;
import dev.fmcuttingboard.fs.ProjectFiles;
import dev.fmcuttingboard.settings.FmCuttingBoardSettingsState;
import dev.fmcuttingboard.util.Notifier;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostics (Windows only): saves the raw bytes of every current clipboard format into
 * {@code <base dir>/captures/capture-<timestamp>/}, for use as golden fixtures in
 * {@code shared/fixtures/clipboard/}. Visible only when diagnostics are enabled in settings.
 */
public class SaveRawClipboardCaptureAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(SaveRawClipboardCaptureAction.class);
    private static final int MAX_FORMATS = 64;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        String os = System.getProperty("os.name", "");
        if (!os.toLowerCase().startsWith("windows")) {
            Notifier.notify(project, NotificationType.WARNING, "FMCuttingBoard",
                    "Raw clipboard capture is only available on Windows.");
            return;
        }

        List<RawClipboardCapture.FormatBytes> formats = new ArrayList<>();
        boolean opened = false;
        try {
            for (int i = 0; i < 5 && !opened; i++) {
                opened = ClipboardFormatsDumpAction.User32.INSTANCE.OpenClipboard(null);
                if (!opened) Thread.sleep(50);
            }
            if (!opened) {
                Notifier.notify(project, NotificationType.WARNING, "FMCuttingBoard",
                        "The clipboard is busy. Try again in a moment.");
                return;
            }
            int id = 0;
            while (formats.size() < MAX_FORMATS) {
                id = ClipboardFormatsDumpAction.User32.INSTANCE.EnumClipboardFormats(id);
                if (id == 0) break;
                byte[] bytes = ClipboardFormatsDumpAction.readAllBytes(id);
                if (bytes != null) {
                    formats.add(new RawClipboardCapture.FormatBytes(id, ClipboardFormatsDumpAction.getFormatName(id), bytes));
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
        } catch (Throwable t) {
            Notifier.notifyWithDetails(project, NotificationType.ERROR, "FMCuttingBoard",
                    "Reading the clipboard failed.", t);
            return;
        } finally {
            if (opened) {
                try { ClipboardFormatsDumpAction.User32.INSTANCE.CloseClipboard(); } catch (Throwable ignore) {}
            }
        }

        if (formats.isEmpty()) {
            Notifier.notify(project, NotificationType.INFORMATION, "FMCuttingBoard", "The clipboard is empty.");
            return;
        }
        try {
            FmCuttingBoardSettingsState st = FmCuttingBoardSettingsState.getInstance(project);
            Path baseDir = ProjectFiles.ensureCustomBaseDir(ProjectFiles.getProjectRoot(project), st.getBaseDirName()).directory();
            Path dir = RawClipboardCapture.write(baseDir.resolve("captures"), formats, Instant.now(), "FMCuttingBoard for JetBrains");
            LOG.info("[CB-CAPTURE] Saved " + formats.size() + " clipboard formats to " + dir);
            Notifier.notify(project, NotificationType.INFORMATION, "FMCuttingBoard",
                    "Saved " + formats.size() + " clipboard formats to " + dir
                            + ". See shared/fixtures/README.md before adding it to the repo.");
        } catch (Throwable t) {
            Notifier.notifyWithDetails(project, NotificationType.ERROR, "FMCuttingBoard",
                    "Saving the clipboard capture failed.", t);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        boolean enabled = false;
        try {
            if (project != null) {
                FmCuttingBoardSettingsState st = FmCuttingBoardSettingsState.getInstance(project);
                enabled = st != null && st.isEnableDiagnostics();
            }
        } catch (Throwable ignore) {
            enabled = false;
        }
        e.getPresentation().setEnabledAndVisible(enabled);
    }
}
