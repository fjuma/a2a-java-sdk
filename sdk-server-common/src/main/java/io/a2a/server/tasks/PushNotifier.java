package io.a2a.server.tasks;

import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;

public interface PushNotifier {
    void setInfo(String taskId, PushNotificationConfig notificationConfig);

    PushNotificationConfig getInfo(String taskId);

    void deleteInfo(String taskId);

    void sendNotification(Task task);
}
