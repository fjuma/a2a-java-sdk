package io.a2a.spec;

import static io.a2a.spec.A2A.CANCEL_TASK_METHOD;
import static io.a2a.spec.A2A.GET_TASK_METHOD;
import static io.a2a.spec.A2A.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2A.JSONRPC_VERSION;
import static io.a2a.spec.A2A.SEND_MESSAGE_METHOD;
import static io.a2a.spec.A2A.SEND_STREAMING_MESSAGE_METHOD;
import static io.a2a.spec.A2A.SEND_TASK_RESUBSCRIPTION_METHOD;
import static io.a2a.spec.A2A.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.a2a.util.Assert;

/**
 * Represents a JSONRPC request.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "method",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GetTaskRequest.class, name = GET_TASK_METHOD),
        @JsonSubTypes.Type(value = SendMessageRequest.class, name = SEND_MESSAGE_METHOD),
        @JsonSubTypes.Type(value = GetTaskPushNotificationConfigRequest.class, name = GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD),
        @JsonSubTypes.Type(value = CancelTaskRequest.class, name = CANCEL_TASK_METHOD),
        @JsonSubTypes.Type(value = SetTaskPushNotificationConfigRequest.class, name = SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD),
        @JsonSubTypes.Type(value = SendStreamingMessageRequest.class, name = SEND_STREAMING_MESSAGE_METHOD),
        @JsonSubTypes.Type(value = TaskResubscriptionRequest.class, name = SEND_TASK_RESUBSCRIPTION_METHOD)
})
@JsonDeserialize(using = JSONRPCRequestDeserializer.class)
public abstract sealed class JSONRPCRequest<T> implements JSONRPCMessage permits GetTaskRequest,
        CancelTaskRequest, SetTaskPushNotificationConfigRequest, GetTaskPushNotificationConfigRequest,
        TaskResubscriptionRequest, SendMessageRequest, SendStreamingMessageRequest {

    protected String jsonrpc;
    protected Object id;
    protected String method;
    protected T params;

    public JSONRPCRequest() {
    }

    public JSONRPCRequest(String jsonrpc, Object id, String method, T params) {
        Assert.checkNotNullParam("jsonrpc", jsonrpc);
        Assert.checkNotNullParam("method", method);
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    @Override
    public String getJsonrpc() {
        return this.jsonrpc;
    }

    @Override
    public Object getId() {
        return this.id;
    }

    public String getMethod() {
        return this.method;
    }

    public T getParams() {
        return this.params;
    }
}
