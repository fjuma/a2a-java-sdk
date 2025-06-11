package io.a2a.server.tasks;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;

public class TaskUpdater {
    private final EventQueue eventQueue;
    private final String taskId;
    private final String contextId;

    public TaskUpdater(EventQueue eventQueue, RequestContext context) {
        this.eventQueue = eventQueue;
        this.taskId = context.getTaskId();
        this.contextId = context.getContextId();
    }

    public void updateStatus(TaskState taskState) {
        updateStatus(taskState, null);
    }

    public void updateStatus(TaskState state, Message message) {
        updateStatus(state, message, false);
    }

    public void updateStatus(TaskState state, Message message, boolean isFinal) {

        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
                .taskId(taskId)
                .contextId(contextId)
                .isFinal(isFinal)
                .status(new TaskStatus(state, message, null))
                .build();
        eventQueue.enqueueEvent(event);
    }

    public void addArtifact(List<Part<?>> parts, String artifactId, String name, Map<String, Object> metadata) {
        if (artifactId == null) {
            artifactId = UUID.randomUUID().toString();
        }
        TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent.Builder()
                .taskId(taskId)
                .contextId(contextId)
                .artifact(
                        new Artifact.Builder()
                                .artifactId(artifactId)
                                .name(name)
                                .parts(parts)
                                .metadata(metadata)
                                .build()
                )
                .build();
        eventQueue.enqueueEvent(event);
    }

    public void complete() {
        complete(null);
    }

    public void complete(Message message) {
        updateStatus(TaskState.COMPLETED, message, true);
    }

    public void fail() {
        fail(null);
    }

    public void fail(Message message) {
        updateStatus(TaskState.FAILED, message, true);
    }

    public void submit() {
        submit(null);
    }

    public void submit(Message message) {
        updateStatus(TaskState.SUBMITTED, message);
    }

    public void startWork() {
        startWork(null);
    }

    public void startWork(Message message) {
        updateStatus(TaskState.WORKING, message);
    }

    public void cancel() {
        cancel(null);
    }

    public void cancel(Message message) {
        updateStatus(TaskState.CANCELED, message, true);
    }

    public Message newAgentMessage(List<Part<?>> parts, Map<String, Object> metadata) {
        return new Message.Builder()
                .role(Message.Role.AGENT)
                .taskId(taskId)
                .contextId(contextId)
                .messageId(UUID.randomUUID().toString())
                .metadata(metadata)
                .parts(parts)
                .build();
    }

}
