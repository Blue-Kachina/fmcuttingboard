package dev.fmcuttingboard.ui;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;

/**
 * Shows a lightweight toolbar banner when an XML file contains a <fmxmlsnippet ...> marker.
 * Provides a "Push" button that triggers the existing PushClipboardIntoFileMaker action.
 */
public class FmXmlSnippetNotificationProvider implements EditorNotificationProvider {
    private static final String FMXML_TAG = "<fmxmlsnippet"; // case-insensitive contains check
    private static final String PUSH_ACTION_ID = "dev.fmcuttingboard.actions.PushClipboardIntoFileMaker";

    @Override
    public @Nullable Function<? super FileEditor, ? extends JComponent> collectNotificationData(@NotNull Project project,
                                                                                                  @NotNull VirtualFile file) {
        // Only for XML files
        FileType type = file.getFileType();
        if (!"XML".equalsIgnoreCase(type.getName())) {
            return null;
        }

        final String text;
        try {
            text = VfsUtilCore.loadText(file);
        } catch (Throwable t) {
            return null; // don't show on load issues
        }

        if (text == null) return null;
        if (!text.toLowerCase().contains(FMXML_TAG)) return null;

        return fileEditor -> {
            if (!(fileEditor instanceof TextEditor)) return null;

            EditorNotificationPanel panel = new EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Info);
            panel.setText("FileMaker XML Detected");

            panel.createActionLabel("Send To FileMaker Clipboard", () -> {
                // Delegate to existing action
                AnAction push = ActionManager.getInstance().getAction(PUSH_ACTION_ID);
                if (push == null) return;

                JComponent component = ((TextEditor) fileEditor).getEditor().getComponent();
                ActionManager.getInstance().tryToExecute(push, null, component, null, true);
            });

            return panel;
        };
    }
}
