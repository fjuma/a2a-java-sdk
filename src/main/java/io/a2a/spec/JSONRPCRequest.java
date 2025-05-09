package io.a2a.spec;

import static io.a2a.spec.A2A.JSONRPC_VERSION;
import static io.a2a.util.Utils.defaultIfNull;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * Represents a JSONRPC request.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public sealed class JSONRPCRequest implements JSONRPCMessage permits SendTaskRequest, SendTaskStreamingRequest,
        GetTaskRequest, CancelTaskRequest, SetTaskPushNotificationRequest, GetTaskPushNotificationRequest,
        TaskResubscriptionRequest {

    protected String jsonrpc;
    protected Object id;
    protected String method;
    protected Map<String, Object> params;

    public JSONRPCRequest() {
    }

    public JSONRPCRequest(String jsonrpc, Object id, String method, Map<String, Object> params) {
        Assert.checkNotNullParam("method", method);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id == null ? UUID.randomUUID().toString() : id;
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

    public Map<String, Object> getParams() {
        return this.params;
    }
}
