package dev.fmcuttingboard.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

/**
 * Helper for showing plugin notifications with a consistent group.
 */
public final class Notifier {
    private static final String GROUP_ID = "FMCuttingBoard";

    private Notifier() {}

    public static void notify(Project project, NotificationType type, String title, String content) {
        NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup(GROUP_ID);
        Notification notification = group.createNotification(title, content, type);
        notification.notify(project);
    }
}
