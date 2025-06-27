package io.a2a.spec;

import static io.a2a.spec.A2A.JSONRPC_VERSION;
import static io.a2a.spec.A2A.SEND_TASK_RESUBSCRIPTION_METHOD;
import static io.a2a.util.Utils.defaultIfNull;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * Used to resubscribe to a task.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TaskResubscriptionRequest extends StreamingJSONRPCRequest<TaskIdParams> {

    @JsonCreator
    public TaskResubscriptionRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                     @JsonProperty("method") String method, @JsonProperty("params") TaskIdParams params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(SEND_TASK_RESUBSCRIPTION_METHOD)) {
            throw new IllegalArgumentException("Invalid TaskResubscriptionRequest method");
        }
        Assert.checkNotNullParam("params", params);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.method = method;
        this.params = params;
    }

    public TaskResubscriptionRequest(Object id, TaskIdParams params) {
        this(null, id, SEND_TASK_RESUBSCRIPTION_METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method = SEND_TASK_RESUBSCRIPTION_METHOD;
        private TaskIdParams params;

        public TaskResubscriptionRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public TaskResubscriptionRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        public TaskResubscriptionRequest.Builder method(String method) {
            this.method = method;
            return this;
        }

        public TaskResubscriptionRequest.Builder params(TaskIdParams params) {
            this.params = params;
            return this;
        }

        public TaskResubscriptionRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new TaskResubscriptionRequest(jsonrpc, id, method, params);
        }
    }
}
