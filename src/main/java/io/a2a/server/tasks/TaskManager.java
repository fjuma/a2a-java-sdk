package io.a2a.server.tasks;

import static io.a2a.spec.TaskState.SUBMITTED;
import static io.a2a.util.Assert.checkNotNullParam;

import java.rmi.ServerError;
import java.util.ArrayList;
import java.util.List;

import io.a2a.spec.Artifact;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;

public class TaskManager {
    private volatile String taskId;
    private volatile String contextId;
    private final TaskStore taskStore;
    private final Message initialMessage;

    public TaskManager(String taskId, String contextId, TaskStore taskStore, Message initialMessage) {
        checkNotNullParam("taskStore", taskStore);
        this.taskId = taskId;
        this.contextId = contextId;
        this.taskStore = taskStore;
        this.initialMessage = initialMessage;
    }

    String getTaskId() {
        return taskId;
    }

    String getContextId() {
        return contextId;
    }

    public Task getTask() {
        if (taskId == null) {
            return null;
        }
        return taskStore.get(taskId);
    }

    public void saveTaskEvent(Task task) throws ServerError {
        checkIdsAndUpdateIfNecessary(task.getId(), task.getContextId());
        saveTask(task);
    }

    public void saveTaskEvent(TaskStatusUpdateEvent event) throws ServerError {
        checkIdsAndUpdateIfNecessary(event.getTaskId(), event.getContextId());
        Task task = ensureTask(event.getTaskId(), event.getContextId());


        Task.Builder builder = new Task.Builder(task)
                .status(event.getStatus());

        if (task.getStatus().message() != null) {
            List<Message> newHistory = task.getHistory() == null ? new ArrayList<>() : new ArrayList<>(task.getHistory());
            newHistory.add(task.getStatus().message());
            builder.history(newHistory);
        }

        task = builder.build();
        saveTask(task);
    }

    public void saveTaskEvent(TaskArtifactUpdateEvent event) throws ServerError {
        checkIdsAndUpdateIfNecessary(event.getTaskId(), event.getContextId());
        Task task = ensureTask(event.getTaskId(), event.getContextId());

        // Append artifacts
        List<Artifact> artifacts = task.getArtifacts() == null ? new ArrayList<>() : new ArrayList<>(task.getArtifacts());

        Artifact newArtifact = event.getArtifact();
        String artifactId = newArtifact.artifactId();
        boolean appendParts = event.isAppend() != null && event.isAppend();

        Artifact existingArtifact = null;
        int existingArtifactIndex = -1;

        for (int i = 0; i < artifacts.size(); i++) {
            Artifact curr = artifacts.get(i);
            if (curr.artifactId() != null && curr.artifactId().equals(artifactId)) {
                existingArtifact = curr;
                existingArtifactIndex = i;
                break;
            }
        }

        if (!appendParts) {
            // This represents the first chunk for this artifact index
            if (existingArtifactIndex >= 0) {
                // Replace the existing artifact entirely with the new artifact
                artifacts.set(existingArtifactIndex, newArtifact);
            } else {
                // Append the new artifact since no artifact with this id/index exists yet
                artifacts.add(newArtifact);
            }

        } else if (existingArtifact != null) {
            // Append new parts to the existing artifact's parts list
            // Do this to a copy

            List<Part<?>> parts = new ArrayList<>(existingArtifact.parts());
            parts.addAll(newArtifact.parts());
            Artifact updated = new Artifact.Builder(existingArtifact)
                    .parts(parts)
                    .build();
            artifacts.set(existingArtifactIndex, updated);
        } else {
            // We received a chunk to append, but we don't have an existing artifact.
            // We will ignore this chunk
        }

        task = new Task.Builder(task)
                .artifacts(artifacts)
                .build();

        saveTask(task);
    }

    private void checkIdsAndUpdateIfNecessary(String eventTaskId, String eventContextId) throws ServerError {
        if (taskId != null && !eventTaskId.equals(taskId)) {
            throw new ServerError(
                    "Invalid task id",
                    new InvalidParamsError(String.format("Task in event doesn't match TaskManager ")));
        }
        if (taskId == null) {
            taskId = eventTaskId;
        }
        if (contextId == null) {
            contextId = eventContextId;
        }
    }

    private Task ensureTask(String eventTaskId, String eventContextId) {
        Task task = taskStore.get(taskId);
        if (task == null) {
            task = createTask(eventTaskId, eventContextId);
            saveTask(task);
        }
        return task;
    }

    private Task createTask(String taskId, String contextId) {
        List<Message> history = initialMessage != null ? List.of(initialMessage) : null;
        return new Task.Builder()
                .id(taskId)
                .contextId(contextId)
                .status(new TaskStatus(SUBMITTED))
                .history(history)
                .build();
    }

    private void saveTask(Task task) {
        taskStore.save(task);
        if (taskId == null) {
            taskId = task.getId();
            contextId = task.getContextId();
        }
    }
}
