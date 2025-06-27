package io.a2a.server.tasks;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.a2a.http.A2AHttpClient;
import io.a2a.http.JdkA2AHttpClient;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;
import io.a2a.util.Utils;

@ApplicationScoped
public class InMemoryPushNotifier implements PushNotifier {
    private final A2AHttpClient httpClient;
    private final Map<String, PushNotificationConfig> pushNotificationInfos = Collections.synchronizedMap(new HashMap<>());

    @Inject
    public InMemoryPushNotifier() {
        this.httpClient = new JdkA2AHttpClient();
    }

    public InMemoryPushNotifier(A2AHttpClient httpClient) {
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

        // TODO auth

        String body;
        try {
            body = Utils.OBJECT_MAPPER.writeValueAsString(task);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Error writing value as string: " + e.getMessage(), e);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException("Error writing value as string: " + throwable.getMessage(), throwable);
        }

        try {
            httpClient.createPost()
                    .url(url)
                    .body(body)
                    .post();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error pushing data to " + url + ": " + e.getMessage(), e);
        }

    }
}
