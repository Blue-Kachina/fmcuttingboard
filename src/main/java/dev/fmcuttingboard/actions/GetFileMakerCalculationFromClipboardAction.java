package dev.fmcuttingboard.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import dev.fmcuttingboard.clipboard.ClipboardAccessException;
import dev.fmcuttingboard.clipboard.ClipboardService;
import dev.fmcuttingboard.clipboard.DefaultClipboardService;
import dev.fmcuttingboard.fs.ProjectFiles;
import dev.fmcuttingboard.util.Notifier;
import dev.fmcuttingboard.util.UserNotifier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Create a new .fmcalc file from text on the clipboard and open it in the editor.
 * Steps:
 * 1) Ensure clipboard has text
 * 2) Create new .fmcalc file under configured base directory (defaults to .fmCuttingBoard)
 * 3) Populate with clipboard text
 * 4) Open for editing
 * 5) Reload directory contents from disk
 */
public class GetFileMakerCalculationFromClipboardAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GetFileMakerCalculationFromClipboardAction.class);

    private final ClipboardService clipboardService;
    private final UserNotifier notifier;

    public GetFileMakerCalculationFromClipboardAction() {
        this(new DefaultClipboardService(), Notifier::notify);
    }

    // Visible for testing / DI
    public GetFileMakerCalculationFromClipboardAction(ClipboardService clipboardService, UserNotifier notifier) {
        this.clipboardService = clipboardService;
        this.notifier = notifier;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        LOG.info("Invoke: GetFileMakerCalculationFromClipboardAction");

        final String text;
        try {
            text = clipboardService.readText().orElse("");
        } catch (ClipboardAccessException ex) {
            LOG.warn("Clipboard read failed", ex);
            Notifier.notifyWithDetails(project, NotificationType.ERROR,
                    "Get FileMaker Calculation From Clipboard",
                    "Could not read clipboard: " + safeMessage(ex), ex);
            return;
        }

        if (text.isBlank()) {
            notifier.notify(project, NotificationType.INFORMATION,
                    "Get FileMaker Calculation From Clipboard",
                    "Clipboard is empty or does not contain text to save.");
            return;
        }

        try {
            Path projectRoot = ProjectFiles.getProjectRoot(project);

            // Resolve base directory and filename pattern potentially from settings
            String baseDir = null;
            String pattern = null;
            try {
                if (project != null) {
                    dev.fmcuttingboard.settings.FmCuttingBoardSettingsState settings =
                            dev.fmcuttingboard.settings.FmCuttingBoardSettingsState.getInstance(project);
                    if (settings != null) {
                        baseDir = settings.getBaseDirName();
                        pattern = settings.getFileNamePattern();
                    }
                }
            } catch (Throwable ignore) {
                // default to .fmCuttingBoard
            }

            Path dir = ProjectFiles.ensureCustomBaseDir(projectRoot, baseDir).directory();
            Path file = createUniqueFmcalcFile(dir, pattern);

            Files.writeString(file, text, StandardCharsets.UTF_8);

            // Refresh VFS and open the newly created file
            if (project != null) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(file);
                        if (vFile != null) {
                            VirtualFile parent = vFile.getParent();
                            if (parent != null) {
                                VfsUtil.markDirtyAndRefresh(false, true, true, parent);
                            }
                            FileEditorManager.getInstance(project).openFile(vFile, true);
                        } else {
                            Path parentPath = file.getParent();
                            if (parentPath != null) {
                                VirtualFile parent = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(parentPath);
                                if (parent != null) {
                                    VfsUtil.markDirtyAndRefresh(false, true, true, parent);
                                }
                            }
                        }
                    } catch (Throwable t) {
                        LOG.warn("Post-create IDE refresh/open failed for file=" + file, t);
                    }
                });
            }

            notifier.notify(project, NotificationType.INFORMATION,
                    "Get FileMaker Calculation From Clipboard",
                    "Created: " + displayPath(projectRoot, file));
        } catch (IllegalArgumentException | IOException ex) {
            LOG.warn("Failed to create/write .fmcalc file in projectRoot=" + safeProjectRoot(project), ex);
            notifier.notify(project, NotificationType.ERROR,
                    "Get FileMaker Calculation From Clipboard",
                    "Failed to create/write .fmcalc file: " + safeMessage(ex));
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Hidden from menus per requirement; keep action registered for internal use/tests
        e.getPresentation().setVisible(false);
        e.getPresentation().setEnabled(false);
    }

    private static Path createUniqueFmcalcFile(Path dir, String fileNamePattern) throws IOException {
        String pattern = (fileNamePattern == null || fileNamePattern.isBlank())
                ? "{timestamp}"
                : fileNamePattern;
        String baseName = pattern.replace("{timestamp}", String.valueOf(System.currentTimeMillis()));
        if (!baseName.endsWith(".fmcalc")) {
            baseName = baseName + ".fmcalc";
        }
        Path candidate = dir.resolve(baseName);
        int attempt = 0;
        while (Files.exists(candidate)) {
            attempt++;
            int dot = baseName.lastIndexOf('.');
            String withSuffix = (dot > 0)
                    ? baseName.substring(0, dot) + "-" + attempt + baseName.substring(dot)
                    : baseName + "-" + attempt;
            candidate = dir.resolve(withSuffix);
            if (attempt > 1000) {
                throw new IOException("Unable to create a unique filename after 1000 attempts for baseName=" + baseName);
            }
        }
        return Files.createFile(candidate);
    }

    private static String safeMessage(Throwable t) {
        String msg = t.getMessage();
        return (msg == null || msg.isBlank()) ? t.getClass().getSimpleName() : msg;
    }

    private static String safeProjectRoot(Project project) {
        try {
            return ProjectFiles.getProjectRoot(project).toString();
        } catch (Throwable t) {
            return "<unknown>";
        }
    }

    private static String displayPath(Path projectRoot, Path file) {
        try {
            if (file.startsWith(projectRoot)) {
                return projectRoot.relativize(file).toString();
            }
        } catch (Throwable ignore) {
        }
        return file.toString();
    }
}
