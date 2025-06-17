package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * An object for fetching the push notification configuration for a task.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeleteTaskPushNotificationConfigParams(String id, String pushNotificationConfigId, Map<String, Object> metadata) {

    public DeleteTaskPushNotificationConfigParams {
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("pushNotificationConfigId", pushNotificationConfigId);
    }

    public DeleteTaskPushNotificationConfigParams(String id) {
        this(id, null, null);
    }
}
