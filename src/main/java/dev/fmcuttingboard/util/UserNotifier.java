package dev.fmcuttingboard.util;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

/**
 * Abstraction for sending user notifications, to allow testing without IDE runtime.
 */
public interface UserNotifier {
    void notify(Project project, NotificationType type, String title, String content);
}
