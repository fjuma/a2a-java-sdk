package io.a2a.transport;

import io.a2a.spec.JSONRPCError;
import io.a2a.spec.StreamingEventKind;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface Transport {

    /**
     * Sends a single request and expects a single response.
     * If the transport does not support this, it should return a failed future.
     */
    CompletableFuture<String> request(String url, String body);

    /**
     * Initiates a streaming request.
     * If the transport does not support this, it should throw an UnsupportedOperationException.
     */
    void stream(String url, String body,
                Consumer<StreamingEventKind> onEvent,
                Consumer<JSONRPCError> onError,
                Runnable onComplete);
}