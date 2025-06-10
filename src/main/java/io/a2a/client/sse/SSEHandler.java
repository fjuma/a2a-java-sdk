package io.a2a.client.sse;

import static io.a2a.util.Utils.OBJECT_MAPPER;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.TaskStatusUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSEHandler {
    private static final Logger log = LoggerFactory.getLogger(SSEEventListener.class);
    private final Consumer<StreamingEventKind> eventHandler;
    private final Consumer<JSONRPCError> errorHandler;
    private final Runnable failureHandler;

    public SSEHandler(Consumer<StreamingEventKind> eventHandler, Consumer<JSONRPCError> errorHandler, Runnable failureHandler) {
        this.eventHandler = eventHandler;
        this.errorHandler = errorHandler;
        this.failureHandler = failureHandler;
    }

    public void onMessage(String message, CompletableFuture<Void> completableFuture) {
        try {
            handleMessage(OBJECT_MAPPER.readTree(message),completableFuture);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON message: {}", message, e);
        }
    }

    public void onError(Throwable throwable, CompletableFuture<Void> completableFuture) {
        failureHandler.run();
        completableFuture.cancel(true); // close SSE channel
    }

    private void handleMessage(JsonNode jsonNode, CompletableFuture<Void> completableFuture) {
        try {
            if (jsonNode.has("error")) {
                JSONRPCError error = OBJECT_MAPPER.treeToValue(jsonNode.get("error"), JSONRPCError.class);
                errorHandler.accept(error);
            } else if (jsonNode.has("result")) {
                // result can be a Task, Message, TaskStatusUpdateEvent, or TaskArtifactUpdateEvent
                JsonNode result = jsonNode.path("result");
                StreamingEventKind event = OBJECT_MAPPER.treeToValue(result, StreamingEventKind.class);
                eventHandler.accept(event);
                if (event instanceof TaskStatusUpdateEvent && ((TaskStatusUpdateEvent) event).isFinal()) {
                    completableFuture.cancel(true); // close SSE channel
                }
            } else {
                throw new IllegalArgumentException("Unknown message type");
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
