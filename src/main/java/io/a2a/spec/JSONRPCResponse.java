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
public sealed class JSONRPCResponse implements JSONRPCMessage permits SendTaskResponse, SendTaskStreamingResponse,
        GetTaskResponse, CancelTaskResponse, SetTaskPushNotificationResponse, GetTaskPushNotificationResponse {

    protected String jsonrpc;
    protected Object id;
    protected Object result;
    protected JSONRPCError error;

    public JSONRPCResponse() {
    }

    public JSONRPCResponse(String jsonrpc, Object id, Object result, JSONRPCError error) {
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

    public Object getResult() {
        return this.result;
    }

    public JSONRPCError getError() {
        return this.error;
    }
}
