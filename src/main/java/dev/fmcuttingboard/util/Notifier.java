package dev.fmcuttingboard.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.io.PrintWriter;
import java.io.StringWriter;

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

    /**
     * Notify with a standard action that allows the user to see additional diagnostic details.
     */
    public static void notifyWithDetails(Project project,
                                         NotificationType type,
                                         String title,
                                         String content,
                                         String details) {
        NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup(GROUP_ID);
        Notification notification = group.createNotification(title, content, type);
        if (details != null && !details.isBlank()) {
            notification.addAction(NotificationAction.createSimpleExpiring("Show Details", () ->
                    Messages.showInfoMessage(project, details, title + " — Details")));
        }
        notification.notify(project);
    }

    /**
     * Convenience overload that formats the Throwable's stack trace as details.
     */
    public static void notifyWithDetails(Project project,
                                         NotificationType type,
                                         String title,
                                         String content,
                                         Throwable t) {
        notifyWithDetails(project, type, title, content, stackTrace(t));
    }

    private static String stackTrace(Throwable t) {
        if (t == null) return "";
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.flush();
            return sw.toString();
        } catch (Throwable ignored) {
            String msg = t.getMessage();
            return (msg == null || msg.isBlank()) ? t.getClass().getName() : (t.getClass().getName() + ": " + msg);
        }
    }
}
