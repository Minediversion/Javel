package com.minediversion.javel;

import com.intellij.notification.*;
import com.intellij.openapi.project.Project;

public class JutgeNotificationHandler {
    public static void notifyError(Project project, String content){
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Jutge Notification")
                .createNotification(content, NotificationType.ERROR)
                .notify(project);
    }

    public static void notifyInfo(Project project, String content){
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Jutge Notification")
                .createNotification(content, NotificationType.INFORMATION)
                .notify(project);
    }
}
