package io.a2a.server.apps.common;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import io.a2a.server.events.QueueManager;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.Event;
import io.a2a.spec.Task;
import io.quarkus.arc.profile.IfBuildProfile;

/**
 * Contains utilities to interact with the server side for the tests.
 * The intent for this bean is to be exposed via REST.
 *
 * <p>There is a Jakarta implementation in {@code TestUtilsRESTEndpoint} which shows the contract for how to
 * expose it via REST. If not using Jakarta REST you will need to provide an implementation that works in a similar
 * way to {@code TestUtilsRESTEndpoint}.</p>
 *
 */
@ApplicationScoped
public class TestUtilsBean {

    @Inject
    TaskStore taskStore;

    @Inject
    QueueManager queueManager;

    public void saveTask(Task task) {
        taskStore.save(task);
    }

    public Task getTask(String taskId) {
        return taskStore.get(taskId);
    }

    public void deleteTask(String taskId) {
        taskStore.delete(taskId);
    }

    public void ensureQueue(String taskId) {
        queueManager.createOrTap(taskId);
    }

    public void enqueueEvent(String taskId, Event event) {
        queueManager.get(taskId).enqueueEvent(event);
    }
}
