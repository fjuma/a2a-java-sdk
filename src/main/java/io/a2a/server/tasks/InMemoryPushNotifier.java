package io.a2a.server.tasks;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.a2a.http.TempA2AHttpClient;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;

@ApplicationScoped
public class InMemoryPushNotifier implements PushNotifier {
    private final TempA2AHttpClient httpClient;
    private final Map<String, PushNotificationConfig> pushNotificationInfos = Collections.synchronizedMap(new HashMap<>());

    @Inject
    public InMemoryPushNotifier(TempA2AHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public void setInfo(String taskId, PushNotificationConfig notificationConfig) {
        pushNotificationInfos.put(taskId, notificationConfig);
    }

    @Override
    public PushNotificationConfig getInfo(String taskId) {
        return pushNotificationInfos.get(taskId);
    }

    @Override
    public void deleteInfo(String taskId) {
        pushNotificationInfos.remove(taskId);
    }

    @Override
    public void sendNotification(Task task) {
        PushNotificationConfig pushInfo = pushNotificationInfos.get(task.getId());
        if (pushInfo == null) {
            return;
        }
        String url = pushInfo.url();
        // TODO https://github.com/fjuma/a2a-java-sdk/issues/59 will have the real client
        httpClient.post(url, task);

    }
}
