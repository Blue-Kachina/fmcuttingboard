package dev.fmcuttingboard.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import dev.fmcuttingboard.clipboard.ClipboardAccessException;
import dev.fmcuttingboard.clipboard.ClipboardService;
import dev.fmcuttingboard.clipboard.DefaultClipboardService;
import dev.fmcuttingboard.fm.ConversionException;
import dev.fmcuttingboard.fm.DefaultXmlToClipboardConverter;
import dev.fmcuttingboard.fm.XmlToClipboardConverter;
import dev.fmcuttingboard.util.Notifier;
import dev.fmcuttingboard.util.UserNotifier;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;

/**
 * Phase 5.3 — Action Implementation & Context Awareness
 *
 * Reads XML from the currently active editor (if available), validates/converts to
 * a FileMaker-compatible clipboard payload, and writes it to the system clipboard.
 * The action is only enabled when a project is open and an XML file is active.
 */
public class PushClipboardIntoFileMakerAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(PushClipboardIntoFileMakerAction.class);

    private final ClipboardService clipboardService;
    private final XmlToClipboardConverter converter;
    private final UserNotifier notifier;

    public PushClipboardIntoFileMakerAction() {
        this(new DefaultClipboardService(), new DefaultXmlToClipboardConverter(), Notifier::notify);
    }

    // Visible for testing / DI
    public PushClipboardIntoFileMakerAction(ClipboardService clipboardService,
                                            XmlToClipboardConverter converter,
                                            UserNotifier notifier) {
        this.clipboardService = clipboardService;
        this.converter = converter;
        this.notifier = notifier;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile vf = null;
        if (project != null) {
            vf = e.getData(CommonDataKeys.VIRTUAL_FILE);
            if (vf == null) {
                VirtualFile[] selected = FileEditorManager.getInstance(project).getSelectedFiles();
                if (selected.length > 0) {
                    vf = selected[0];
                }
            }
        }
        boolean enabled = isEnabled(project, vf);
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        LOG.info("Invoke: PushClipboardIntoFileMakerAction");

        VirtualFile vf = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (vf == null && project != null) {
            VirtualFile[] selected = FileEditorManager.getInstance(project).getSelectedFiles();
            if (selected.length > 0) {
                vf = selected[0];
            }
        }

        if (!isXmlFile(vf)) {
            LOG.info("Active editor is not an XML file or no file active.");
            notifier.notify(project, NotificationType.WARNING, "Push Clipboard Into FileMaker",
                    "Please focus an XML file to push into FileMaker.");
            return;
        }

        // 1) Read file content as XML
        final String xml;
        try {
            xml = VfsUtilCore.loadText(vf);
        } catch (Throwable t) {
            LOG.warn("Failed to read active XML file: " + safeName(vf), t);
            notifier.notify(project, NotificationType.ERROR, "Push Clipboard Into FileMaker",
                    "Failed to read the active XML file: " + safeMessage(t));
            return;
        }

        if (xml.isBlank()) {
            LOG.info("Active XML file is empty.");
            notifier.notify(project, NotificationType.INFORMATION, "Push Clipboard Into FileMaker",
                    "The active XML file is empty.");
            return;
        }

        // 2) Convert XML to FileMaker-compatible clipboard payload
        final String payload;
        try {
            payload = converter.convertToClipboardPayload(xml);
        } catch (ConversionException ce) {
            LOG.info("XML content is not a supported fmxmlsnippet.");
            notifier.notify(project, NotificationType.WARNING, "Push Clipboard Into FileMaker",
                    "The file does not contain a supported fmxmlsnippet.");
            return;
        } catch (Throwable t) {
            LOG.warn("Unexpected error during XML→clipboard conversion", t);
            notifier.notify(project, NotificationType.ERROR, "Push Clipboard Into FileMaker",
                    "Unexpected error during conversion: " + safeMessage(t));
            return;
        }

        // 3) Write payload to system clipboard
        try {
            clipboardService.writeText(payload);
        } catch (ClipboardAccessException ex) {
            LOG.warn("Clipboard write failed", ex);
            notifier.notify(project, NotificationType.ERROR, "Push Clipboard Into FileMaker",
                    "Converted payload ready, but failed to write to clipboard: " + safeMessage(ex));
            return;
        }

        int bytes = payload.getBytes(StandardCharsets.UTF_8).length;
        LOG.info("Push successful; payload written to clipboard (bytes=" + bytes + ")");
        notifier.notify(project, NotificationType.INFORMATION, "Push Clipboard Into FileMaker",
                "Success: Converted XML and placed FileMaker-compatible content on the clipboard.");
    }

    static boolean isXmlFile(VirtualFile vf) {
        if (vf == null || vf.isDirectory()) return false;
        String ext = vf.getExtension();
        return isXmlFileExtension(ext);
    }

    // Visible for testing: enablement predicate
    boolean isEnabled(Project project, VirtualFile vf) {
        return project != null && isXmlFile(vf);
    }

    // Overload for tests that don't require a Project instance
    boolean isEnabled(boolean hasProject, VirtualFile vf) {
        return hasProject && isXmlFile(vf);
    }

    // Overload for tests with only an extension available
    boolean isEnabled(boolean hasProject, String fileExtension) {
        return hasProject && isXmlFileExtension(fileExtension);
    }

    // Helper exposed for tests
    static boolean isXmlFileExtension(String ext) {
        return ext != null && ext.equalsIgnoreCase("xml");
    }

    // Package-private for tests: processes provided xml and writes to clipboard
    String processXmlToClipboard(String xml) throws ConversionException, ClipboardAccessException {
        String payload = converter.convertToClipboardPayload(xml);
        clipboardService.writeText(payload);
        return payload;
    }

    private static String safeMessage(Throwable t) {
        String msg = t.getMessage();
        return (msg == null || msg.isBlank()) ? t.getClass().getSimpleName() : msg;
    }

    private static String safeName(VirtualFile vf) {
        try {
            return vf == null ? "<no-file>" : vf.getPath();
        } catch (Throwable t) {
            return "<unavailable>";
        }
    }
}
