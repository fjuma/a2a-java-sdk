package io.a2a.server.apps.quarkus;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.a2a.server.apps.common.TestUtilsBean;
import io.a2a.spec.Task;
import io.a2a.util.Utils;
import io.quarkus.vertx.web.Body;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class A2ATestRoutes {
    @Inject
    TestUtilsBean testUtilsBean;

    @Route(path = "/test/task", methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void saveTask(@Body String body, RoutingContext rc) {
        try {
            Task task = Utils.OBJECT_MAPPER.readValue(body, Task.class);
            testUtilsBean.saveTask(task);
            rc.response()
                .setStatusCode(200)
                .end();
    } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    @Route(path = "/test/task/:taskId", methods = {Route.HttpMethod.GET}, produces = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void getTask(@Param String taskId,  RoutingContext rc) {
        try {
            Task task = testUtilsBean.getTask(taskId);
            if (task == null) {
                rc.response()
                    .setStatusCode(404)
                    .end();
                return;
            }
            rc.response()
                    .setStatusCode(200)
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .end(Utils.OBJECT_MAPPER.writeValueAsString(task));

        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    @Route(path = "/test/task/:taskId", methods = {Route.HttpMethod.DELETE}, type = Route.HandlerType.BLOCKING)
    public void deleteTask(@Param String taskId, RoutingContext rc) {
        try {
            Task task = testUtilsBean.getTask(taskId);
            if (task == null) {
                rc.response()
                        .setStatusCode(404)
                        .end();
                return;
            }
            testUtilsBean.deleteTask(taskId);
            rc.response()
                    .setStatusCode(200)
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    private void errorResponse(Throwable t, RoutingContext rc) {
        rc.response()
                .setStatusCode(200)
                .putHeader(CONTENT_TYPE, TEXT_PLAIN)
                .end();
    }

}
