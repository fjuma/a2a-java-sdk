package io.a2a.server.apps.common;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.a2a.server.events.QueueManager;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.Event;
import io.a2a.spec.Task;

/**
 * Contains utilities to interact with the server side for the tests.
 */
@ApplicationScoped
public abstract class TestUtilsRESTEndpoint {

    @Inject
    TestUtilsBean testUtils;

    @POST
    @Path("task")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveTask(Task task) {
        testUtils.saveTask(task);
        return Response.ok().build();
    }

    @GET
    @Path("task/{taskId}")
    public Task getTask(@PathParam("taskId") String taskId) {
        return testUtils.getTask(taskId);
    }

    @DELETE
    @Path("task/{taskId}")
    public Response deleteTask(@PathParam("taskId") String taskId) {
        testUtils.deleteTask(taskId);
        return Response.ok().build();
    }

    @POST
    @Path("queue/ensure/{taskId}")
    public Response ensureQueue(@PathParam("taskId") String taskId) {
        testUtils.ensureQueue(taskId);
        return Response.ok().build();
    }

    @POST
    @Path("queue/enqueue/{taskId}")
    public Response enqueueEvent(@PathParam("taskId") String taskId, Event event) {
        testUtils.enqueueEvent(taskId, event);
        return Response.ok().build();
    }

}
