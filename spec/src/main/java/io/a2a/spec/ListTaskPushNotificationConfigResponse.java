package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A response for a list task push notification config request.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ListTaskPushNotificationConfigResponse extends JSONRPCResponse<TaskPushNotificationConfig> {

    @JsonCreator
    public ListTaskPushNotificationConfigResponse(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                                  @JsonProperty("result") TaskPushNotificationConfig result,
                                                  @JsonProperty("error") JSONRPCError error) {
        super(jsonrpc, id, result, error);
    }

    public ListTaskPushNotificationConfigResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    public ListTaskPushNotificationConfigResponse(Object id, TaskPushNotificationConfig result) {
        this(null, id,  result, null);
    }

}
