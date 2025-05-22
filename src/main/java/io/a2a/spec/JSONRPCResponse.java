package io.a2a.spec;

import static io.a2a.spec.A2A.JSONRPC_VERSION;
import static io.a2a.util.Utils.defaultIfNull;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a JSONRPC response.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract sealed class JSONRPCResponse<T> implements JSONRPCMessage permits SendStreamingMessageResponse,
        GetTaskResponse, CancelTaskResponse, SetTaskPushNotificationResponse, GetTaskPushNotificationResponse,
        SendMessageResponse, JSONRPCErrorResponse {

    protected String jsonrpc;
    protected Object id;
    protected T result;
    protected JSONRPCError error;

    public JSONRPCResponse() {
    }

    public JSONRPCResponse(String jsonrpc, Object id, T result, JSONRPCError error) {
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.result = result;
        this.error = error;
    }

    public String getJsonrpc() {
        return this.jsonrpc;
    }

    public Object getId() {
        return this.id;
    }

    public T getResult() {
        return this.result;
    }

    public JSONRPCError getError() {
        return this.error;
    }
}
