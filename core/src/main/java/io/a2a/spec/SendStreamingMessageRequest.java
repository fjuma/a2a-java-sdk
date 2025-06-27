package io.a2a.spec;

import static io.a2a.spec.A2A.JSONRPC_VERSION;
import static io.a2a.spec.A2A.SEND_STREAMING_MESSAGE_METHOD;
import static io.a2a.util.Utils.defaultIfNull;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * Used to initiate a task with streaming.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SendStreamingMessageRequest extends StreamingJSONRPCRequest<MessageSendParams> {

    @JsonCreator
    public SendStreamingMessageRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                       @JsonProperty("method") String method, @JsonProperty("params") MessageSendParams params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(SEND_STREAMING_MESSAGE_METHOD)) {
            throw new IllegalArgumentException("Invalid SendStreamingMessageRequest method");
        }
        Assert.checkNotNullParam("params", params);
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public SendStreamingMessageRequest(Object id,  MessageSendParams params) {
        this(null, id, SEND_STREAMING_MESSAGE_METHOD, params);
    }

    public static class Builder {
            private String jsonrpc;
            private Object id;
            private String method = SEND_STREAMING_MESSAGE_METHOD;
            private MessageSendParams params;

            public Builder jsonrpc(String jsonrpc) {
                this.jsonrpc = jsonrpc;
                return this;
            }

            public Builder id(Object id) {
                this.id = id;
                return this;
            }

            public Builder method(String method) {
                this.method = method;
                return this;
            }

            public Builder params(MessageSendParams params) {
                this.params = params;
                return this;
            }

            public SendStreamingMessageRequest build() {
                if (id == null) {
                    id = UUID.randomUUID().toString();
                }
                return new SendStreamingMessageRequest(jsonrpc, id, method, params);
            }
        }
    }
