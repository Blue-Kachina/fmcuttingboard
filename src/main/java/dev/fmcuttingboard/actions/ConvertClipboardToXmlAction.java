package dev.fmcuttingboard.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import dev.fmcuttingboard.clipboard.ClipboardAccessException;
import dev.fmcuttingboard.clipboard.ClipboardService;
import dev.fmcuttingboard.clipboard.DefaultClipboardService;
import dev.fmcuttingboard.fm.ClipboardToXmlConverter;
import dev.fmcuttingboard.fm.ConversionException;
import org.jetbrains.annotations.NotNull;

/**
 * Phase 3.2 — Action Implementation
 * Integrates clipboard reader, detects/parses FileMaker content, converts to XML,
 * and replaces the clipboard content with the XML string. Provides user
 * notifications and logs outcomes.
 */
public class ConvertClipboardToXmlAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ConvertClipboardToXmlAction.class);
    private final ClipboardService clipboardService;
    private final ClipboardToXmlConverter converter;

    public ConvertClipboardToXmlAction() {
        this(new DefaultClipboardService(), new ClipboardToXmlConverter());
    }

    // Visible for testing / DI
    public ConvertClipboardToXmlAction(ClipboardService clipboardService, ClipboardToXmlConverter converter) {
        this.clipboardService = clipboardService;
        this.converter = converter;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        LOG.info("Invoke: ConvertClipboardToXmlAction");

        // 1) Read clipboard text
        final String clipboardText;
        try {
            clipboardText = clipboardService.readText().orElse("");
        } catch (ClipboardAccessException ex) {
            LOG.warn("Clipboard read failed", ex);
            notify(project, NotificationType.ERROR, "Convert FileMaker Clipboard To XML",
                    "Could not read clipboard: " + safeMessage(ex));
            return;
        }

        if (clipboardText.isBlank()) {
            LOG.info("Clipboard is empty or does not contain text.");
            notify(project, NotificationType.INFORMATION, "Convert FileMaker Clipboard To XML",
                    "Clipboard is empty or contains no text to convert.");
            return;
        }

        // 2) Convert using parser/converter
        final String xml;
        try {
            xml = converter.convertToXml(clipboardText);
        } catch (ConversionException ce) {
            LOG.info("Clipboard does not contain recognizable FileMaker content.");
            notify(project, NotificationType.WARNING, "Convert FileMaker Clipboard To XML",
                    "Clipboard does not contain recognizable FileMaker content or fmxmlsnippet.");
            return;
        } catch (Throwable t) {
            LOG.warn("Unexpected error during conversion", t);
            notify(project, NotificationType.ERROR, "Convert FileMaker Clipboard To XML",
                    "Unexpected error during conversion: " + safeMessage(t));
            return;
        }

        // 3) Write XML back to clipboard
        try {
            clipboardService.writeText(xml);
        } catch (ClipboardAccessException ex) {
            LOG.warn("Clipboard write failed", ex);
            notify(project, NotificationType.ERROR, "Convert FileMaker Clipboard To XML",
                    "Converted XML generated, but failed to write to clipboard: " + safeMessage(ex));
            return;
        }

        LOG.info("Conversion successful; XML placed on clipboard.");
        notify(project, NotificationType.INFORMATION, "Convert FileMaker Clipboard To XML",
                "Success: Converted FileMaker clipboard content to XML and placed it on the clipboard.");
    }

    private static void notify(Project project, NotificationType type, String title, String content) {
        NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup("FMCuttingBoard");
        Notification notification = group.createNotification(title, content, type);
        notification.notify(project);
    }

    private static String safeMessage(Throwable t) {
        String msg = t.getMessage();
        return (msg == null || msg.isBlank()) ? t.getClass().getSimpleName() : msg;
    }
}
