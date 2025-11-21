package dev.fmcuttingboard.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

/**
 * Simple preview/confirmation helper used before writing to clipboard.
 */
public final class PreviewDialogs {
    private PreviewDialogs() {}

    /**
     * Shows a confirmation dialog with a (possibly truncated) preview of the content.
     * Returns true if the user confirmed proceeding.
     */
    public static boolean confirmWrite(@NotNull Project project,
                                       @NotNull String title,
                                       @NotNull String content,
                                       int previewLimit) {
        String preview;
        if (content.length() > previewLimit && previewLimit > 0) {
            preview = content.substring(0, previewLimit) + "\n… (truncated)";
        } else {
            preview = content;
        }
        String message = "About to write the following content to the clipboard:\n\n" + preview +
                "\n\nProceed?";
        int result = Messages.showYesNoDialog(project, message, title, Messages.getQuestionIcon());
        return result == Messages.YES;
    }
}
