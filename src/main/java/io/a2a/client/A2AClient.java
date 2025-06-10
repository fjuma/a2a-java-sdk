package io.a2a.client;

import static io.a2a.spec.A2A.CANCEL_TASK_METHOD;
import static io.a2a.spec.A2A.GET_TASK_METHOD;
import static io.a2a.spec.A2A.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2A.JSONRPC_VERSION;
import static io.a2a.spec.A2A.SEND_MESSAGE_METHOD;
import static io.a2a.spec.A2A.SEND_STREAMING_MESSAGE_METHOD;
import static io.a2a.spec.A2A.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.util.Assert.checkNotNullParam;
import static io.a2a.util.Utils.OBJECT_MAPPER;
import static io.a2a.util.Utils.unmarshalFrom;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.a2a.client.sse.SSEHandler;
import io.a2a.http.A2AHttpClient;
import io.a2a.http.A2AHttpResponse;
import io.a2a.http.JdkA2AHttpClient;
import io.a2a.spec.A2A;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigResponse;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.SetTaskPushNotificationConfigResponse;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * An A2A client.
 */
public class A2AClient {

    private static final TypeReference<SendMessageResponse> SEND_MESSAGE_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<GetTaskResponse> GET_TASK_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<CancelTaskResponse> CANCEL_TASK_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<GetTaskPushNotificationConfigResponse> GET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<SetTaskPushNotificationConfigResponse> SET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient okHttpClient;
    private final A2AHttpClient httpClient;
    private final String agentUrl;
    private AgentCard agentCard;


    /**
     * Create a new A2AClient.
     *
     * @param agentCard the agent card for the A2A server this client will be communicating with
     */
    public A2AClient(AgentCard agentCard) {
        checkNotNullParam("agentCard", agentCard);
        this.agentCard = agentCard;
        this.agentUrl = agentCard.url();
        this.okHttpClient = new OkHttpClient();
        this.httpClient = new JdkA2AHttpClient();
    }

    /**
     * Create a new A2AClient.
     *
     * @param agentUrl the URL for the A2A server this client will be communicating with
     */
    public A2AClient(String agentUrl) {
        checkNotNullParam("agentUrl", agentUrl);
        this.agentUrl = agentUrl;
        this.okHttpClient = new OkHttpClient();
        this.httpClient = new JdkA2AHttpClient();
    }

    /**
     * Get the agent card for the A2A server this client will be communicating with from
     * the default public agent card endpoint.
     *
     * @return the agent card for the A2A server
     * @throws {@code A2AServerException} if the agent card for the A2A server cannot be obtained
     */
    public AgentCard getAgentCard() throws A2AServerException {
        if (this.agentCard == null) {
            this.agentCard = A2A.getAgentCard(this.httpClient, this.agentUrl);
        }
        return this.agentCard;
    }

    /**
     * Get the agent card for the A2A server this client will be communicating with.
     *
     * @param relativeCardPath the path to the agent card endpoint relative to the base URL of the A2A server
     * @param authHeaders the HTTP authentication headers to use
     * @return the agent card for the A2A server
     * @throws {@code A2AServerException} if the agent card for the A2A server cannot be obtained
     */
    public AgentCard getAgentCard(String relativeCardPath, Map<String, String> authHeaders) throws A2AServerException {
        if (this.agentCard == null) {
            this.agentCard = A2A.getAgentCard(this.httpClient, this.agentUrl, relativeCardPath, authHeaders);
        }
        return this.agentCard;
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
            String httpResponseBody = sendPostRequest(sendMessageRequest);
            return unmarshalResponse(httpResponseBody, SEND_MESSAGE_RESPONSE_REFERENCE);
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to send message: " + e);
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
            String httpResponseBody = sendPostRequest(getTaskRequest);
            return unmarshalResponse(httpResponseBody, GET_TASK_RESPONSE_REFERENCE);
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to get task: " + e);
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
            String httpResponseBody = sendPostRequest(cancelTaskRequest);
            return unmarshalResponse(httpResponseBody, CANCEL_TASK_RESPONSE_REFERENCE);
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to cancel task: " + e);
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
            String httpResponseBody = sendPostRequest(getTaskPushNotificationRequest);
            return unmarshalResponse(httpResponseBody, GET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to get task push notification config: " + e);
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
            String httpResponseBody = sendPostRequest(setTaskPushNotificationRequest);
            return unmarshalResponse(httpResponseBody, SET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to set task push notification config: " + e);
        }
    }

    /**
     * Send a streaming message to the remote agent.
     *
     * @param requestId the request ID to use
     * @param messageSendParams the parameters for the message to be sent
     * @param eventHandler a consumer that will be invoked for each event received from the remote agent
     * @param errorHandler a consumer that will be invoked if the remote agent returns an error
     * @param failureHandler a consumer that will be invoked if a failure occurs when processing events
     * @throws A2AServerException if sending the streaming message fails for any reason
     */
    public void sendStreamingMessage(String requestId, MessageSendParams messageSendParams, Consumer<StreamingEventKind> eventHandler,
                                       Consumer<JSONRPCError> errorHandler, Runnable failureHandler) throws A2AServerException {
        checkNotNullParam("messageSendParams", messageSendParams);
        checkNotNullParam("eventHandler", eventHandler);
        checkNotNullParam("errorHandler", errorHandler);
        checkNotNullParam("failureHandler", failureHandler);

        SendStreamingMessageRequest.Builder sendStreamingMessageRequestBuilder = new SendStreamingMessageRequest.Builder()
                .jsonrpc(JSONRPC_VERSION)
                .method(SEND_STREAMING_MESSAGE_METHOD)
                .params(messageSendParams);

        if (requestId != null) {
            sendStreamingMessageRequestBuilder.id(requestId);
        }

        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEHandler sseHandler = new SSEHandler(eventHandler, errorHandler, failureHandler);
        SendStreamingMessageRequest sendStreamingMessageRequest = sendStreamingMessageRequestBuilder.build();
        try {
            A2AHttpClient.PostBuilder builder = createPostBuilder(sendStreamingMessageRequest);
            ref.set(builder.postAsyncSSE(
                    msg -> sseHandler.onMessage(msg, ref.get()),
                    throwable -> sseHandler.onError(throwable, ref.get()),
                    () -> {
                        // We don't need to do anything special on completion
                    }));

        } catch (IOException e) {
            throw new A2AServerException("Failed to send streaming message request: " + e);
        } catch (InterruptedException e) {
            throw new A2AServerException("Send streaming message request timed out: " + e);
        }
    }

    private String sendPostRequest(Object value) throws IOException, InterruptedException {
        A2AHttpClient.PostBuilder builder = createPostBuilder(value);
        A2AHttpResponse response = builder.post();
        if (!response.success()) {
            throw new IOException("Request failed " + response.status());
        }
        return response.body();
    }

    private A2AHttpClient.PostBuilder createPostBuilder(Object value) throws JsonProcessingException {
        return httpClient.createPost()
                .url(agentUrl)
                .addHeader("Content-Type", "application/json")
                .body(OBJECT_MAPPER.writeValueAsString(value));

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
}
