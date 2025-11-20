package dev.fmcuttingboard.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Phase 1.2: Placeholder for pushing clipboard content into FileMaker.
 */
public class PushClipboardIntoFileMakerAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(PushClipboardIntoFileMakerAction.class);
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Placeholder: actual implementation will arrive in Phase 5.
        Project project = e.getProject();
        LOG.info("Invoke: PushClipboardIntoFileMakerAction");
        notifyNotImplemented(project, "Push Clipboard Into FileMaker");
    }

    private static void notifyNotImplemented(Project project, String actionName) {
        NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup("FMCuttingBoard");
        Notification notification = group.createNotification(
                actionName + " — Not implemented yet",
                "This action is a placeholder and will be implemented in a later phase.",
                NotificationType.INFORMATION
        );
        notification.notify(project);
    }
}
