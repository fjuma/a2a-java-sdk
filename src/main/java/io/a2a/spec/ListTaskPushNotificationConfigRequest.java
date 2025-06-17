package io.a2a.spec;

import static io.a2a.spec.A2A.JSONRPC_VERSION;
import static io.a2a.spec.A2A.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.util.Utils.defaultIfNull;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * A get task push notification request.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ListTaskPushNotificationConfigRequest extends NonStreamingJSONRPCRequest<ListTaskPushNotificationConfigParams> {

    @JsonCreator
    public ListTaskPushNotificationConfigRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                                 @JsonProperty("method") String method,
                                                 @JsonProperty("params") ListTaskPushNotificationConfigParams params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD)) {
            throw new IllegalArgumentException("Invalid ListTaskPushNotificationRequest method");
        }
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public ListTaskPushNotificationConfigRequest(String id, ListTaskPushNotificationConfigParams params) {
        this(null, id, LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method;
        private ListTaskPushNotificationConfigParams params;

        public ListTaskPushNotificationConfigRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public ListTaskPushNotificationConfigRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        public ListTaskPushNotificationConfigRequest.Builder method(String method) {
            this.method = method;
            return this;
        }

        public ListTaskPushNotificationConfigRequest.Builder params(ListTaskPushNotificationConfigParams params) {
            this.params = params;
            return this;
        }

        public ListTaskPushNotificationConfigRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new ListTaskPushNotificationConfigRequest(jsonrpc, id, method, params);
        }
    }
}
