package dev.fmcuttingboard.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.application.ApplicationManager;
import dev.fmcuttingboard.clipboard.ClipboardAccessException;
import dev.fmcuttingboard.clipboard.ClipboardService;
import dev.fmcuttingboard.clipboard.DefaultClipboardService;
import dev.fmcuttingboard.fm.ClipboardToXmlConverter;
import dev.fmcuttingboard.fm.ConversionException;
import dev.fmcuttingboard.fs.ProjectFiles;
import dev.fmcuttingboard.util.Notifier;
import dev.fmcuttingboard.util.UserNotifier;
import dev.fmcuttingboard.util.Diagnostics;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Phase 4.3 — Action Implementation
 * Reads clipboard, attempts to parse FileMaker content, converts to XML,
 * and writes it into a new timestamped file inside .fmCuttingBoard.
 */
public class ReadClipboardIntoNewXmlFileAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ReadClipboardIntoNewXmlFileAction.class);
    private final ClipboardService clipboardService;
    private final ClipboardToXmlConverter converter;
    private final UserNotifier notifier;

    public ReadClipboardIntoNewXmlFileAction() {
        this(new DefaultClipboardService(), new ClipboardToXmlConverter(), Notifier::notify);
    }

    // Visible for testing / DI
    public ReadClipboardIntoNewXmlFileAction(ClipboardService clipboardService, ClipboardToXmlConverter converter) {
        this(clipboardService, converter, Notifier::notify);
    }

    // Visible for testing / DI
    public ReadClipboardIntoNewXmlFileAction(ClipboardService clipboardService,
                                             ClipboardToXmlConverter converter,
                                             UserNotifier notifier) {
        this.clipboardService = clipboardService;
        this.converter = converter;
        this.notifier = notifier;
    }
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        LOG.info("Invoke: ReadClipboardIntoNewXmlFileAction");

        // 1) Read clipboard text
        final String clipboardText;
        try {
            clipboardText = clipboardService.readText().orElse("");
        } catch (ClipboardAccessException ex) {
            LOG.warn("Clipboard read failed", ex);
            Notifier.notifyWithDetails(project, NotificationType.ERROR, "Read Clipboard Into New XML File",
                    "Could not read clipboard: " + safeMessage(ex), ex);
            return;
        }

        if (clipboardText.isBlank()) {
            LOG.info("Clipboard is empty or does not contain text.");
            notifier.notify(project, NotificationType.INFORMATION, "Read Clipboard Into New XML File",
                    "Clipboard is empty or contains no text to save.");
            return;
        }

        // 2) Convert using parser/converter
        final String xml;
        try {
            xml = converter.convertToXml(clipboardText);
        } catch (ConversionException ce) {
            LOG.info("Clipboard does not contain recognizable FileMaker content.");
            notifier.notify(project, NotificationType.WARNING, "Read Clipboard Into New XML File",
                    "Clipboard does not contain recognizable FileMaker content or fmxmlsnippet.");
            return;
        } catch (Throwable t) {
            LOG.warn("Unexpected error during conversion", t);
            Notifier.notifyWithDetails(project, NotificationType.ERROR, "Read Clipboard Into New XML File",
                    "Unexpected error during conversion: " + safeMessage(t), t);
            return;
        }

        // 3) Create timestamped file inside .fmCuttingBoard and write XML
        try {
            Path projectRoot = ProjectFiles.getProjectRoot(project);
            Path file = processIntoNewXmlFile(projectRoot, xml);
            String display = displayPath(projectRoot, file);

            // 3a) Refresh VFS for the new file and its parent directory
            if (project != null) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(file);
                        if (vFile != null) {
                            VirtualFile parent = vFile.getParent();
                            if (parent != null) {
                                // Ensure IDE reloads the directory contents from disk
                                VfsUtil.markDirtyAndRefresh(false, true, true, parent);
                            }
                            // Open the newly created file in the editor
                            FileEditorManager.getInstance(project).openFile(vFile, true);
                        } else {
                            // As a fallback, refresh parent dir by path
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
            notifier.notify(project, NotificationType.INFORMATION, "Read Clipboard Into New XML File",
                    "Success: Wrote XML to file: " + display);
        } catch (IllegalArgumentException | IOException ex) {
            LOG.warn("Failed to create/write XML file in projectRoot=" + safeProjectRoot(project), ex);
            notifier.notify(project, NotificationType.ERROR, "Read Clipboard Into New XML File",
                    "Failed to create/write XML file: " + safeMessage(ex));
            return;
        }
    }

    // Package-private for testing: writes provided xml to new timestamped file under projectRoot
    Path processIntoNewXmlFile(Path projectRoot, String xml) throws IOException {
        Path file = ProjectFiles.createTimestampedXmlFile(projectRoot);
        long startNs = System.nanoTime();
        Files.writeString(file, xml, StandardCharsets.UTF_8);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        int byteCount = xml.getBytes(StandardCharsets.UTF_8).length;
        int charCount = xml.length();
        LOG.info("Wrote XML to: " + file + " (bytes=" + byteCount + ", chars=" + charCount + ", took=" + elapsedMs + "ms)");
        Diagnostics.vInfo(LOG, "XML preview (first 120 chars): " + xml.substring(0, Math.min(120, xml.length())));
        return file;
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
            // fallthrough
        }
        return file.toString();
    }
}
