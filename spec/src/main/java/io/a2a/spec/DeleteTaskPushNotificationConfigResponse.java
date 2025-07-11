package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A response for a delete task push notification config request.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DeleteTaskPushNotificationConfigResponse extends JSONRPCResponse<Void> {

    @JsonCreator
    public DeleteTaskPushNotificationConfigResponse(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                                    @JsonProperty("result") Void result,
                                                    @JsonProperty("error") JSONRPCError error) {
        super(jsonrpc, id, result, error);
    }

    public DeleteTaskPushNotificationConfigResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    public DeleteTaskPushNotificationConfigResponse(Object id) {
        this(null, id, null, null);
    }

}
