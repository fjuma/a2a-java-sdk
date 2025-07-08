package io.a2a.server.tasks;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.a2a.transport.A2ATransport;
import io.a2a.transport.http.JdkA2AHttpTransport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;

@ApplicationScoped
public class InMemoryPushNotifier implements PushNotifier {
    private final A2ATransport transport;
    private final Map<String, PushNotificationConfig> pushNotificationInfos = Collections.synchronizedMap(new HashMap<>());

    @Inject
    public InMemoryPushNotifier() {
        this.transport = new JdkA2AHttpTransport();
    }

    public InMemoryPushNotifier(A2ATransport transport) {
        this.transport = transport;
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

        // TODO auth

        try {
            transport.sendEvent(task, url);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error pushing data to " + url + ": " + e.getMessage(), e);
        }

    }
}
