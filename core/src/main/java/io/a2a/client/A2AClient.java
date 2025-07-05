package io.a2a.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.a2a.spec.*;
import io.a2a.transport.Transport;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static io.a2a.spec.A2A.*;
import static io.a2a.util.Assert.checkNotNullParam;
import static io.a2a.util.Utils.OBJECT_MAPPER;
import static io.a2a.util.Utils.unmarshalFrom;

/**
 * An A2A client.
 */
public class A2AClient {

    private static final TypeReference<SendMessageResponse> SEND_MESSAGE_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<GetTaskResponse> GET_TASK_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<CancelTaskResponse> CANCEL_TASK_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<GetTaskPushNotificationConfigResponse> GET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<SetTaskPushNotificationConfigResponse> SET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = new TypeReference<>() {};
    private final Transport transport;
    private final String agentUrl;
    private AgentCard agentCard;


    /**
     * Create a new A2AClient.
     *
     * @param agentCard the agent card for the A2A server this client will be communicating with
     * @param transport the transport to use for communication
     */
    public A2AClient(AgentCard agentCard, Transport transport) {
        checkNotNullParam("agentCard", agentCard);
        checkNotNullParam("transport", transport);
        this.agentCard = agentCard;
        this.agentUrl = agentCard.url();
        this.transport = transport;
    }

    /**
     * Create a new A2AClient.
     *
     * @param agentUrl the URL for the A2A server this client will be communicating with
     * @param transport the transport to use for communication
     */
    public A2AClient(String agentUrl, Transport transport) {
        checkNotNullParam("agentUrl", agentUrl);
        checkNotNullParam("transport", transport);
        this.agentUrl = agentUrl;
        this.transport = transport;
    }

    /**
     * Send a message to the remote agent.
     *
     * @param messageSendParams the parameters for the message to be sent
     * @return the response, may contain a message or a task
     * @throws A2AServerException if sending the message fails for any reason
     */
    public SendMessageResponse sendMessage(MessageSendParams messageSendParams) throws A2AServerException {
        return sendMessage(null, messageSendParams);
    }

    /**
     * Send a message to the remote agent.
     *
     * @param requestId the request ID to use
     * @param messageSendParams the parameters for the message to be sent
     * @return the response, may contain a message or a task
     * @throws A2AServerException if sending the message fails for any reason
     */
    public SendMessageResponse sendMessage(String requestId, MessageSendParams messageSendParams) throws A2AServerException {
        SendMessageRequest.Builder sendMessageRequestBuilder = new SendMessageRequest.Builder()
                .jsonrpc(JSONRPC_VERSION)
                .method(SEND_MESSAGE_METHOD)
                .params(messageSendParams);

        if (requestId != null) {
            sendMessageRequestBuilder.id(requestId);
        }

        SendMessageRequest sendMessageRequest = sendMessageRequestBuilder.build();

        try {
            String responseBody = sendRequest(sendMessageRequest).get();
            return unmarshalResponse(responseBody, SEND_MESSAGE_RESPONSE_REFERENCE);
        } catch (Exception e) {
            throw new A2AServerException("Failed to send message: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieve a task from the A2A server. This method can be used to retrieve the generated
     * artifacts for a task.
     *
     * @param id the task ID
     * @return the response containing the task
     * @throws A2AServerException if retrieving the task fails for any reason
     */
    public GetTaskResponse getTask(String id) throws A2AServerException {
        return getTask(null, new TaskQueryParams(id));
    }

    /**
     * Retrieve a task from the A2A server. This method can be used to retrieve the generated
     * artifacts for a task.
     *
     * @param taskQueryParams the params for the task to be queried
     * @return the response containing the task
     * @throws A2AServerException if retrieving the task fails for any reason
     */
    public GetTaskResponse getTask(TaskQueryParams taskQueryParams) throws A2AServerException {
        return getTask(null, taskQueryParams);
    }

    /**
     * Retrieve the generated artifacts for a task.
     *
     * @param requestId the request ID to use
     * @param taskQueryParams the params for the task to be queried
     * @return the response containing the task
     * @throws A2AServerException if retrieving the task fails for any reason
     */
    public GetTaskResponse getTask(String requestId, TaskQueryParams taskQueryParams) throws A2AServerException {
        GetTaskRequest.Builder getTaskRequestBuilder = new GetTaskRequest.Builder()
                .jsonrpc(JSONRPC_VERSION)
                .method(GET_TASK_METHOD)
                .params(taskQueryParams);

        if (requestId != null) {
            getTaskRequestBuilder.id(requestId);
        }

        GetTaskRequest getTaskRequest = getTaskRequestBuilder.build();

        try {
            String responseBody = sendRequest(getTaskRequest).get();
            return unmarshalResponse(responseBody, GET_TASK_RESPONSE_REFERENCE);
        } catch (Exception e) {
            throw new A2AServerException("Failed to get task: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel a task that was previously submitted to the A2A server.
     *
     * @param id the task ID
     * @return the response indicating if the task was cancelled
     * @throws A2AServerException if cancelling the task fails for any reason
     */
    public CancelTaskResponse cancelTask(String id) throws A2AServerException {
        return cancelTask(null, new TaskIdParams(id));
    }

    /**
     * Cancel a task that was previously submitted to the A2A server.
     *
     * @param taskIdParams the params for the task to be cancelled
     * @return the response indicating if the task was cancelled
     * @throws A2AServerException if cancelling the task fails for any reason
     */
    public CancelTaskResponse cancelTask(TaskIdParams taskIdParams) throws A2AServerException {
        return cancelTask(null, taskIdParams);
    }

    /**
     * Cancel a task that was previously submitted to the A2A server.
     *
     * @param requestId the request ID to use
     * @param taskIdParams the params for the task to be cancelled
     * @return the response indicating if the task was cancelled
     * @throws A2AServerException if retrieving the task fails for any reason
     */
    public CancelTaskResponse cancelTask(String requestId, TaskIdParams taskIdParams) throws A2AServerException {
        CancelTaskRequest.Builder cancelTaskRequestBuilder = new CancelTaskRequest.Builder()
                .jsonrpc(JSONRPC_VERSION)
                .method(CANCEL_TASK_METHOD)
                .params(taskIdParams);

        if (requestId != null) {
            cancelTaskRequestBuilder.id(requestId);
        }

        CancelTaskRequest cancelTaskRequest = cancelTaskRequestBuilder.build();

        try {
            String responseBody = sendRequest(cancelTaskRequest).get();
            return unmarshalResponse(responseBody, CANCEL_TASK_RESPONSE_REFERENCE);
        } catch (Exception e) {
            throw new A2AServerException("Failed to cancel task: " + e.getMessage(), e);
        }
    }

    /**
     * Get the push notification configuration for a task.
     *
     * @param id the task ID
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public GetTaskPushNotificationConfigResponse getTaskPushNotificationConfig(String id) throws A2AServerException {
        return getTaskPushNotificationConfig(null, new TaskIdParams(id));
    }

    /**
     * Get the push notification configuration for a task.
     *
     * @param taskIdParams the params for the task
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public GetTaskPushNotificationConfigResponse getTaskPushNotificationConfig(TaskIdParams taskIdParams) throws A2AServerException {
        return getTaskPushNotificationConfig(null, taskIdParams);
    }

    /**
     * Get the push notification configuration for a task.
     *
     * @param requestId the request ID to use
     * @param taskIdParams the params for the task
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public GetTaskPushNotificationConfigResponse getTaskPushNotificationConfig(String requestId, TaskIdParams taskIdParams) throws A2AServerException {
        GetTaskPushNotificationConfigRequest.Builder getTaskPushNotificationRequestBuilder = new GetTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPC_VERSION)
                .method(GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD)
                .params(taskIdParams);

        if (requestId != null) {
            getTaskPushNotificationRequestBuilder.id(requestId);
        }

        GetTaskPushNotificationConfigRequest getTaskPushNotificationRequest = getTaskPushNotificationRequestBuilder.build();

        try {
            String responseBody = sendRequest(getTaskPushNotificationRequest).get();
            return unmarshalResponse(responseBody, GET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
        } catch (Exception e) {
            throw new A2AServerException("Failed to get task push notification config: " + e.getMessage(), e);
        }
    }

    /**
     * Set push notification configuration for a task.
     *
     * @param taskId the task ID
     * @param pushNotificationConfig the push notification configuration
     * @return the response indicating whether setting the task push notification configuration succeeded
     * @throws A2AServerException if setting the push notification configuration fails for any reason
     */
    public SetTaskPushNotificationConfigResponse setTaskPushNotificationConfig(String taskId,
                                                                               PushNotificationConfig pushNotificationConfig) throws A2AServerException {
        return setTaskPushNotificationConfig(null, taskId, pushNotificationConfig);
    }

    /**
     * Set push notification configuration for a task.
     *
     * @param requestId the request ID to use
     * @param taskId the task ID
     * @param pushNotificationConfig the push notification configuration
     * @return the response indicating whether setting the task push notification configuration succeeded
     * @throws A2AServerException if setting the push notification configuration fails for any reason
     */
    public SetTaskPushNotificationConfigResponse setTaskPushNotificationConfig(String requestId, String taskId,
                                                                               PushNotificationConfig pushNotificationConfig) throws A2AServerException {
        SetTaskPushNotificationConfigRequest.Builder setTaskPushNotificationRequestBuilder = new SetTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPC_VERSION)
                .method(SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD)
                .params(new TaskPushNotificationConfig(taskId, pushNotificationConfig));

        if (requestId != null) {
            setTaskPushNotificationRequestBuilder.id(requestId);
        }

        SetTaskPushNotificationConfigRequest setTaskPushNotificationRequest = setTaskPushNotificationRequestBuilder.build();

        try {
            String responseBody = sendRequest(setTaskPushNotificationRequest).get();
            return unmarshalResponse(responseBody, SET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
        } catch (Exception e) {
            throw new A2AServerException("Failed to set task push notification config: " + e.getMessage(), e);
        }
    }

    private CompletableFuture<String> sendRequest(Object value) throws JsonProcessingException {
        String requestBody = OBJECT_MAPPER.writeValueAsString(value);
        return transport.request(agentUrl, requestBody);
    }

    private <T extends JSONRPCResponse> T unmarshalResponse(String response, TypeReference<T> typeReference)
            throws A2AServerException, JsonProcessingException {
        T value = unmarshalFrom(response, typeReference);
        JSONRPCError error = value.getError();
        if (error != null) {
            throw new A2AServerException(error.getMessage() + (error.getData() != null ? ": " + error.getData() : ""));
        }
        return value;
    }

    public void sendStreamingMessage(MessageSendParams params, Consumer<StreamingEventKind> onEvent, Consumer<JSONRPCError> onError, Runnable onComplete) {
        try {
            String requestBody = OBJECT_MAPPER.writeValueAsString(new SendStreamingMessageRequest.Builder()
                    .jsonrpc(JSONRPC_VERSION)
                    .method(SEND_STREAMING_MESSAGE_METHOD)
                    .params(params)
                    .build());
            transport.stream(agentUrl, requestBody, onEvent, onError, onComplete);
        } catch (JsonProcessingException e) {
            onError.accept(new JSONRPCError(-32700, "Parse error", e.getMessage()));
        } catch (UnsupportedOperationException e) {
            onError.accept(new JSONRPCError(-32601, "Method not found", e.getMessage()));
        }
    }

    public void resubscribeToTask(TaskIdParams taskIdParams, Consumer<StreamingEventKind> onEvent, Consumer<JSONRPCError> onError, Runnable onComplete) {
        try {
            String requestBody = OBJECT_MAPPER.writeValueAsString(new TaskResubscriptionRequest.Builder()
                    .jsonrpc(JSONRPC_VERSION)
                    .method(SEND_TASK_RESUBSCRIPTION_METHOD)
                    .params(taskIdParams)
                    .build());
            transport.stream(agentUrl, requestBody, onEvent, onError, onComplete);
        } catch (JsonProcessingException e) {
            onError.accept(new JSONRPCError(-32700, "Parse error", e.getMessage()));
        } catch (UnsupportedOperationException e) {
            onError.accept(new JSONRPCError(-32601, "Method not found", e.getMessage()));
        }
    }
}