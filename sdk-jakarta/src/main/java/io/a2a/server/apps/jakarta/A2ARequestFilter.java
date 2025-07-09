package io.a2a.server.apps.jakarta;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.TaskResubscriptionRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@PreMatching
public class A2ARequestFilter implements ContainerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(A2ARequestFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (requestContext.getMethod().equals("POST") && requestContext.hasEntity()) {
            try (InputStream entityInputStream = requestContext.getEntityStream()) {
                byte[] requestBodyBytes = entityInputStream.readAllBytes();
                String requestBody = new String(requestBodyBytes);
                // ensure the request is treated as a streaming request or a non-streaming request
                // based on the method in the request body
                if (isStreamingRequest(requestBody)) {
                    LOGGER.debug("Handling request as streaming: {}", requestBody);
                    putAcceptHeader(requestContext, MediaType.SERVER_SENT_EVENTS);
                } else if (isNonStreamingRequest(requestBody)) {
                    LOGGER.debug("Handling request as non-streaming: {}", requestBody);
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
        return requestBody.contains(SendStreamingMessageRequest.METHOD) ||
               requestBody.contains(TaskResubscriptionRequest.METHOD);
    }

    private static boolean isNonStreamingRequest(String requestBody) {
        return requestBody.contains(GetTaskRequest.METHOD) ||
                requestBody.contains(CancelTaskRequest.METHOD) ||
                requestBody.contains(SendMessageRequest.METHOD) ||
                requestBody.contains(SetTaskPushNotificationConfigRequest.METHOD) ||
                requestBody.contains(GetTaskPushNotificationConfigRequest.METHOD);
    }

    private static void putAcceptHeader(ContainerRequestContext requestContext, String mediaType) {
        requestContext.getHeaders().putSingle("Accept", mediaType);
    }

}