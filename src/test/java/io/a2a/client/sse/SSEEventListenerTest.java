package io.a2a.client.sse;

import io.a2a.client.JsonStreamingMessages;
import io.a2a.spec.Artifact;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.StreamingEventType;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SSEEventListenerTest {

    @Test
    public void testOnEventWithTaskResult() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventType> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {},
                () -> {});

        // Parse the task event JSON
        String eventData = JsonStreamingMessages.STREAMING_TASK_EVENT.substring(
                JsonStreamingMessages.STREAMING_TASK_EVENT.indexOf("{"));
        
        // Call the onEvent method directly
        listener.onEvent(null, "event-id", "message", eventData);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof Task);
        Task task = (Task) receivedEvent.get();
        assertEquals("task-123", task.getId());
        assertEquals("context-456", task.getContextId());
        assertEquals(TaskState.WORKING, task.getStatus().state());
    }

    @Test
    public void testOnEventWithMessageResult() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventType> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {},
                () -> {});

        // Parse the message event JSON
        String eventData = JsonStreamingMessages.STREAMING_MESSAGE_EVENT.substring(
                JsonStreamingMessages.STREAMING_MESSAGE_EVENT.indexOf("{"));
        
        // Call onEvent method
        listener.onEvent(null, "event-id", "message", eventData);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof Message);
        Message message = (Message) receivedEvent.get();
        assertEquals(Message.Role.AGENT, message.getRole());
        assertEquals("msg-123", message.getMessageId());
        assertEquals("context-456", message.getContextId());
        assertEquals(1, message.getParts().size());
        assertTrue(message.getParts().get(0) instanceof TextPart);
        assertEquals("Hello, world!", ((TextPart) message.getParts().get(0)).getText());
    }

    @Test
    public void testOnEventWithTaskStatusUpdateEventEvent() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventType> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {},
                () -> {});

        // Parse the message event JSON
        String eventData = JsonStreamingMessages.STREAMING_STATUS_UPDATE_EVENT.substring(
                JsonStreamingMessages.STREAMING_STATUS_UPDATE_EVENT.indexOf("{"));

        // Call onEvent method
        listener.onEvent(null, "event-id", "message", eventData);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof TaskStatusUpdateEvent);
        TaskStatusUpdateEvent taskStatusUpdateEvent = (TaskStatusUpdateEvent) receivedEvent.get();
        assertEquals("1", taskStatusUpdateEvent.getTaskId());
        assertEquals("2", taskStatusUpdateEvent.getContextId());
        assertFalse(taskStatusUpdateEvent.isFinal());
        assertEquals(TaskState.SUBMITTED, taskStatusUpdateEvent.getStatus().state());
    }

    @Test
    public void testOnEventWithTaskArtifactUpdateEventEvent() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventType> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {},
                () -> {});

        // Parse the message event JSON
        String eventData = JsonStreamingMessages.STREAMING_ARTIFACT_UPDATE_EVENT.substring(
                JsonStreamingMessages.STREAMING_ARTIFACT_UPDATE_EVENT.indexOf("{"));

        // Call onEvent method
        listener.onEvent(null, "event-id", "message", eventData);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof TaskArtifactUpdateEvent);

        TaskArtifactUpdateEvent taskArtifactUpdateEvent = (TaskArtifactUpdateEvent) receivedEvent.get();
        assertEquals("1", taskArtifactUpdateEvent.getTaskId());
        assertEquals("2", taskArtifactUpdateEvent.getContextId());
        assertFalse(taskArtifactUpdateEvent.isAppend());
        assertTrue(taskArtifactUpdateEvent.isLastChunk());
        Artifact artifact = taskArtifactUpdateEvent.getArtifact();
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals(1, artifact.parts().size());
        assertEquals(Part.Type.TEXT, artifact.parts().get(0).getType());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart) artifact.parts().get(0)).getText());
    }

    @Test
    public void testOnEventWithError() throws Exception {
        // Set up event handler
        AtomicReference<JSONRPCError> receivedError = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> {},
                error -> receivedError.set(error),
                () -> {});

        // Parse the error event JSON
        String eventData = JsonStreamingMessages.STREAMING_ERROR_EVENT.substring(
                JsonStreamingMessages.STREAMING_ERROR_EVENT.indexOf("{"));
        
        // Call onEvent method
        listener.onEvent(null, "event-id", "message", eventData);

        // Verify the error was processed correctly
        assertNotNull(receivedError.get());
        assertEquals(-32602, receivedError.get().getCode());
        assertEquals("Invalid parameters", receivedError.get().getMessage());
        assertEquals("Missing required field", receivedError.get().getData());
    }

    @Test
    public void testOnFailure() {
        AtomicBoolean failureHandlerCalled = new AtomicBoolean(false);
        SSEEventListener listener = new SSEEventListener(
                event -> {},
                error -> {},
                () -> failureHandlerCalled.set(true));

        // Simulate a failure
        listener.onFailure(null, new RuntimeException("Test exception"), null);

        // Verify the failure handler was called
        assertTrue(failureHandlerCalled.get());
    }

    @Test
    public void testBuilderPattern() {
        List<StreamingEventType> receivedEvents = new ArrayList<>();
        List<JSONRPCError> receivedErrors = new ArrayList<>();
        AtomicBoolean failureHandlerCalled = new AtomicBoolean(false);

        SSEEventListener listener = new SSEEventListener.Builder()
                .logEvents(true)
                .eventHandler(receivedEvents::add)
                .errorHandler(receivedErrors::add)
                .failureHandler(() -> failureHandlerCalled.set(true))
                .build();

        // Verify listener is created with the correct properties
        assertNotNull(listener);
    }
} 