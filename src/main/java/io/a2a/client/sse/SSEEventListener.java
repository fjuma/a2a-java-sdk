package io.a2a.client.sse;

import static io.a2a.util.Assert.checkNotNullParam;
import static io.a2a.util.Utils.OBJECT_MAPPER;

import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.TaskStatusUpdateEvent;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A listener for Server-Sent Events (SSE).
 */
public class SSEEventListener extends EventSourceListener {

    private static final Logger log = LoggerFactory.getLogger(SSEEventListener.class);
    private final boolean logEvents;
    private final Consumer<StreamingEventKind> eventHandler;
    private final Consumer<JSONRPCError> errorHandler;
    private final Runnable failureHandler;

    public SSEEventListener(Consumer<StreamingEventKind> eventHandler, Consumer<JSONRPCError> errorHandler,
                            Runnable failureHandler) {
        this(false, eventHandler, errorHandler, failureHandler);
    }

    public SSEEventListener(boolean logEvents, Consumer<StreamingEventKind> eventHandler,
                            Consumer<JSONRPCError> errorHandler, Runnable failureHandler) {
        checkNotNullParam("eventHandler", eventHandler);
        checkNotNullParam("errorHandler", errorHandler);
        checkNotNullParam("failureHandler", failureHandler);
        this.logEvents = logEvents;
        this.eventHandler = eventHandler;
        this.errorHandler = errorHandler;
        this.failureHandler = failureHandler;
    }

    @Override
    public void onOpen(EventSource eventSource, Response response) {
        if (logEvents) {
            log.debug("onOpen()");
        }
    }

    @Override
    public void onClosed(EventSource eventSource) {
        if (logEvents) {
            log.debug("onClosed()");
        }
    }

    @Override
    public void onEvent(EventSource eventSource, String id, String type, String data) {
        if (type.equals("message")) {
            if (logEvents) {
                log.debug("onEvent() {}", data);
            }
            try {
                handleMessage(OBJECT_MAPPER.readTree(data), eventSource);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse JSON message: {}", data, e);
            }
        }
    }

    @Override
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        if (logEvents) {
            log.debug("onFailure()", t);
        }
        failureHandler.run();
    }

    private void handleMessage(JsonNode jsonNode, EventSource eventSource) {
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
                    eventSource.cancel(); // close SSE channel
                }
            } else {
                throw new IllegalArgumentException("Unknown message type");
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Builder {
        private boolean logEvents;
        private Consumer<StreamingEventKind> eventHandler;
        private Consumer<JSONRPCError> errorHandler;
        private Runnable failureHandler;

        public Builder logEvents(boolean logEvents) {
            this.logEvents = logEvents;
            return this;
        }

        public Builder eventHandler(Consumer<StreamingEventKind> eventHandler) {
            this.eventHandler = eventHandler;
            return this;
        }

        public Builder errorHandler(Consumer<JSONRPCError> errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        public Builder failureHandler(Runnable failureHandler) {
            this.failureHandler = failureHandler;
            return this;
        }

        public SSEEventListener build() {
            return new SSEEventListener(logEvents, eventHandler, errorHandler, failureHandler);
        }
    }
}
