/*package io.a2a.server.apps;

import static io.a2a.spec.A2A.CANCEL_TASK_METHOD;
import static io.a2a.spec.A2A.GET_TASK_METHOD;
import static io.a2a.spec.A2A.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2A.SEND_MESSAGE_METHOD;
import static io.a2a.spec.A2A.SEND_STREAMING_MESSAGE_METHOD;
import static io.a2a.spec.A2A.SEND_TASK_RESUBSCRIPTION_METHOD;
import static io.a2a.spec.A2A.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;

import jakarta.ws.rs.core.MediaType;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

public class A2ARouteFilter {

    @RouteFilter(100)
    void myFilter(RoutingContext routingContext) {
        HttpServerRequest request = routingContext.request();

        if (! request.method().name().equalsIgnoreCase("POST") || ! request.path().equals("/")) {
            routingContext.next();
            return;
        }

        routingContext.request().bodyHandler(buffer -> {
            try {
                String requestBody = buffer.toString();
                if (isStreamingRequest(requestBody)) {
                    putAcceptHeader(routingContext, MediaType.SERVER_SENT_EVENTS);
                } else if (isNonStreamingRequest(requestBody)) {
                    putAcceptHeader(routingContext, MediaType.APPLICATION_JSON);
                }
            } catch (Exception e) {
                throw new RuntimeException("Unable to read the request body");
            }
            routingContext.setBody(buffer);
            routingContext.next();
        });
        routingContext.next();
    }

    private static boolean isStreamingRequest(String requestBody) {
        return requestBody.contains(SEND_STREAMING_MESSAGE_METHOD) ||
                requestBody.contains(SEND_TASK_RESUBSCRIPTION_METHOD);
    }

    private static boolean isNonStreamingRequest(String requestBody) {
        return requestBody.contains(GET_TASK_METHOD) ||
                requestBody.contains(CANCEL_TASK_METHOD) ||
                requestBody.contains(SEND_MESSAGE_METHOD) ||
                requestBody.contains(SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD) ||
                requestBody.contains(GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
    }

    private static void putAcceptHeader(RoutingContext routingContext, String mediaType) {
        routingContext.request().headers().set("Accept", mediaType);
    }
}*/
