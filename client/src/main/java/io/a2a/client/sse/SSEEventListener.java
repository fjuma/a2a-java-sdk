package io.a2a.client.sse;

import static io.a2a.util.Utils.OBJECT_MAPPER;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.SendStreamingMessageResponse;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.TaskStatusUpdateEvent;

public class SSEEventListener {
    private static final Logger log = Logger.getLogger(SSEEventListener.class.getName());
    private final Consumer<StreamingEventKind> eventHandler;
    private final Consumer<JSONRPCError> errorHandler;
    private final Runnable failureHandler;

    public SSEEventListener(Consumer<StreamingEventKind> eventHandler, Consumer<JSONRPCError> errorHandler, Runnable failureHandler) {
        this.eventHandler = eventHandler;
        this.errorHandler = errorHandler;
        this.failureHandler = failureHandler;
    }

    public void onMessage(String message, Future<Void> completableFuture) {
        try {
            SendStreamingMessageResponse sendStreamingMessageResponse = OBJECT_MAPPER.readValue(message, SendStreamingMessageResponse.class);
            handleMessage(sendStreamingMessageResponse,completableFuture);
        } catch (JsonProcessingException e) {
            log.warning("Failed to parse JSON message: " + message);
        }
    }

    public void onMessage(JSONRPCResponse<?> response, Future<Void> completableFuture) {
        handleMessage(response,completableFuture);
    }

    public void onError(Throwable throwable, Future<Void> future) {
        failureHandler.run();
        future.cancel(true); // close SSE channel
    }

    private void handleMessage(JSONRPCResponse<?> response, Future<Void> future) {
        if (null != response.getError()) {
            errorHandler.accept(response.getError());
        } else if (null != response.getResult()) {
            // result can be a Task, Message, TaskStatusUpdateEvent, or TaskArtifactUpdateEvent
            StreamingEventKind event = (StreamingEventKind) response.getResult();
            eventHandler.accept(event);
            if (event instanceof TaskStatusUpdateEvent && ((TaskStatusUpdateEvent) event).isFinal()) {
                future.cancel(true); // close SSE channel
            }
        } else {
            throw new IllegalArgumentException("Unknown message type");
        }
    }
}
