package io.a2a.server.apps.jakarta;

import static io.a2a.spec.A2A.CANCEL_TASK_METHOD;
import static io.a2a.spec.A2A.GET_TASK_METHOD;
import static io.a2a.spec.A2A.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2A.SEND_MESSAGE_METHOD;
import static io.a2a.spec.A2A.SEND_STREAMING_MESSAGE_METHOD;
import static io.a2a.spec.A2A.SEND_TASK_RESUBSCRIPTION_METHOD;
import static io.a2a.spec.A2A.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

@Provider
@PreMatching
public class A2ARequestFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (requestContext.getMethod().equals("POST") && requestContext.hasEntity()) {
            try (InputStream entityInputStream = requestContext.getEntityStream()) {
                byte[] requestBodyBytes = entityInputStream.readAllBytes();
                String requestBody = new String(requestBodyBytes);
                // ensure the request is treated as a streaming request or a non-streaming request
                // based on the method in the request body
                if (isStreamingRequest(requestBody)) {
                    putAcceptHeader(requestContext, MediaType.SERVER_SENT_EVENTS);
                } else if (isNonStreamingRequest(requestBody)) {
                    putAcceptHeader(requestContext, MediaType.APPLICATION_JSON);
                }
                // reset the entity stream
                requestContext.setEntityStream(new ByteArrayInputStream(requestBodyBytes));
            } catch(IOException e){
                throw new RuntimeException("Unable to read the request body");
            }
        }
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

    private static void putAcceptHeader(ContainerRequestContext requestContext, String mediaType) {
        requestContext.getHeaders().putSingle("Accept", mediaType);
    }

}