package dev.fmcuttingboard.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import dev.fmcuttingboard.clipboard.ClipboardAccessException;
import dev.fmcuttingboard.clipboard.ClipboardService;
import dev.fmcuttingboard.clipboard.DefaultClipboardService;
import dev.fmcuttingboard.fm.ClipboardToXmlConverter;
import dev.fmcuttingboard.fm.ConversionException;
import dev.fmcuttingboard.util.Notifier;
import dev.fmcuttingboard.util.PreviewDialogs;
import dev.fmcuttingboard.util.UserNotifier;
import dev.fmcuttingboard.settings.FmCuttingBoardSettingsState;
import org.jetbrains.annotations.NotNull;

/**
 * Smart action that inspects the clipboard content and decides what to do:
 * - If clipboard contains FileMaker objects convertible to fmxmlsnippet, it will:
 *   a) Run the "New XML File From FM Clipboard" workflow to persist the XML, then
 *   b) Replace the clipboard with the XML equivalent (respecting preview setting).
 * - If clipboard text looks like a FileMaker calculation (not convertible to fmxmlsnippet),
 *   it will run the "Get FileMaker Calculation From Clipboard" workflow creating a .fmcalc file.
 * - Otherwise, it will show an error notification.
 */
public class GetFileMakerClipboardContentAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GetFileMakerClipboardContentAction.class);

    private final ClipboardService clipboardService;
    private final ClipboardToXmlConverter converter;
    private final UserNotifier notifier;

    public GetFileMakerClipboardContentAction() {
        this(new DefaultClipboardService(), new ClipboardToXmlConverter(), Notifier::notify);
    }

    // Visible for testing / DI
    public GetFileMakerClipboardContentAction(ClipboardService clipboardService,
                                              ClipboardToXmlConverter converter,
                                              UserNotifier notifier) {
        this.clipboardService = clipboardService;
        this.converter = converter;
        this.notifier = notifier;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        LOG.info("Invoke: GetFileMakerClipboardContentAction");

        final String clipboardText;
        try {
            clipboardText = clipboardService.readText().orElse("");
        } catch (ClipboardAccessException ex) {
            Notifier.notifyWithDetails(project, NotificationType.ERROR,
                    "Get FileMaker Clipboard Content",
                    "Could not read clipboard: " + safeMessage(ex), ex);
            return;
        }

        if (clipboardText.isBlank()) {
            notifier.notify(project, NotificationType.INFORMATION,
                    "Get FileMaker Clipboard Content",
                    "Clipboard is empty or has no text content.");
            return;
        }

        String xml;
        try {
            // Try to treat it as FM object(s) -> fmxmlsnippet
            xml = converter.convertToXml(clipboardText);

            // 1) Run the existing routine that saves XML to a new file
            try {
                new ReadClipboardIntoNewXmlFileAction().actionPerformed(e);
            } catch (Throwable t) {
                LOG.warn("Delegated file creation action failed (continuing to clipboard write)", t);
            }

            // 2) Optionally preview and then replace the clipboard with the XML
            boolean proceed = true;
            try {
                if (project != null) {
                    FmCuttingBoardSettingsState st = FmCuttingBoardSettingsState.getInstance(project);
                    if (st != null && st.isPreviewBeforeClipboardWrite()) {
                        proceed = PreviewDialogs.confirmWrite(project,
                                "Preview: Replace Clipboard With XML",
                                xml,
                                800);
                    }
                }
            } catch (Throwable t) {
                LOG.warn("Preview failed; proceeding with clipboard write", t);
            }
            if (!proceed) {
                notifier.notify(project, NotificationType.INFORMATION,
                        "Get FileMaker Clipboard Content",
                        "Canceled: Clipboard was not modified.");
                return;
            }

            try {
                clipboardService.writeText(xml);
            } catch (ClipboardAccessException ex) {
                Notifier.notifyWithDetails(project, NotificationType.ERROR,
                        "Get FileMaker Clipboard Content",
                        "Saved XML to file, but failed to write XML to clipboard: " + safeMessage(ex), ex);
                return;
            }

            notifier.notify(project, NotificationType.INFORMATION,
                    "Get FileMaker Clipboard Content",
                    "Success: Saved XML to file and replaced clipboard with XML.");
            return;
        } catch (ConversionException notFmXml) {
            // Not fmxmlsnippet. Treat as text calculation
            try {
                new GetFileMakerCalculationFromClipboardAction().actionPerformed(e);
            } catch (Throwable t) {
                LOG.warn("Delegated calculation action failed", t);
                notifier.notify(project, NotificationType.ERROR,
                        "Get FileMaker Clipboard Content",
                        "Failed to create .fmcalc file: " + safeMessage(t));
            }
            return;
        } catch (Throwable t) {
            notifier.notify(project, NotificationType.ERROR,
                    "Get FileMaker Clipboard Content",
                    "Unrecognized clipboard content: " + safeMessage(t));
        }
    }

    private static String safeMessage(Throwable t) {
        String msg = t.getMessage();
        return (msg == null || msg.isBlank()) ? t.getClass().getSimpleName() : msg;
    }
}
