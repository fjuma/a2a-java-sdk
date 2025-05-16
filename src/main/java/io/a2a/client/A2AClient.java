package io.a2a.client;

import static io.a2a.spec.A2A.CANCEL_TASK_REQUEST;
import static io.a2a.spec.A2A.GET_TASK_PUSH_NOTIFICATION_REQUEST;
import static io.a2a.spec.A2A.GET_TASK_REQUEST;
import static io.a2a.spec.A2A.JSONRPC_VERSION;
import static io.a2a.spec.A2A.SEND_MESSAGE_REQUEST;
import static io.a2a.spec.A2A.SEND_STREAMING_MESSAGE_REQUEST;
import static io.a2a.spec.A2A.SET_TASK_PUSH_NOTIFICATION_REQUEST;
import static io.a2a.spec.A2A.getRequestEndpoint;
import static io.a2a.util.Assert.checkNotNullParam;
import static io.a2a.util.Utils.OBJECT_MAPPER;
import static io.a2a.util.Utils.unmarshalFrom;

import java.io.IOException;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import io.a2a.client.sse.SSEEventListener;
import io.a2a.spec.A2A;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.GetTaskPushNotificationRequest;
import io.a2a.spec.GetTaskPushNotificationResponse;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationRequest;
import io.a2a.spec.SetTaskPushNotificationResponse;
import io.a2a.spec.StreamingEventType;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.util.Assert;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSources;

/**
 * An A2A client.
 */
public class A2AClient {

    private static TypeReference<SendMessageResponse> SEND_MESSAGE_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static TypeReference<GetTaskResponse> GET_TASK_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static TypeReference<CancelTaskResponse> CANCEL_TASK_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static TypeReference<GetTaskPushNotificationResponse> GET_TASK_PUSH_NOTIFICATION_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static TypeReference<SetTaskPushNotificationResponse> SET_TASK_PUSH_NOTIFICATION_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
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
        this.httpClient = new OkHttpClient();
    }

    /**
     * Create a new A2AClient.
     *
     * @param agentUrl the URL for the A2A server this client will be communicating with
     */
    public A2AClient(String agentUrl) {
        checkNotNullParam("agentUrl", agentUrl);
        this.agentUrl = agentUrl;
        this.httpClient = new OkHttpClient();
    }

    /**
     * Get the agent card for the A2A server this client will be communicating with.
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
                .method(SEND_MESSAGE_REQUEST)
                .params(messageSendParams);

        if (requestId != null) {
            sendMessageRequestBuilder.id(requestId);
        }

        SendMessageRequest sendMessageRequest = sendMessageRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(SEND_MESSAGE_REQUEST, sendMessageRequest);
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
                .method(GET_TASK_REQUEST)
                .params(taskQueryParams);

        if (requestId != null) {
            getTaskRequestBuilder.id(requestId);
        }

        GetTaskRequest getTaskRequest = getTaskRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(GET_TASK_REQUEST, getTaskRequest);
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
                .method(CANCEL_TASK_REQUEST)
                .params(taskIdParams);

        if (requestId != null) {
            cancelTaskRequestBuilder.id(requestId);
        }

        CancelTaskRequest cancelTaskRequest = cancelTaskRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(CANCEL_TASK_REQUEST, cancelTaskRequest);
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
    public GetTaskPushNotificationResponse getTaskPushNotificationConfig(String id) throws A2AServerException {
        return getTaskPushNotificationConfig(null, new TaskIdParams(id));
    }

    /**
     * Get the push notification configuration for a task.
     *
     * @param taskIdParams the params for the task
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public GetTaskPushNotificationResponse getTaskPushNotificationConfig(TaskIdParams taskIdParams) throws A2AServerException {
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
    public GetTaskPushNotificationResponse getTaskPushNotificationConfig(String requestId, TaskIdParams taskIdParams) throws A2AServerException {
        GetTaskPushNotificationRequest.Builder getTaskPushNotificationRequestBuilder = new GetTaskPushNotificationRequest.Builder()
                .jsonrpc(JSONRPC_VERSION)
                .method(GET_TASK_PUSH_NOTIFICATION_REQUEST)
                .params(taskIdParams);

        if (requestId != null) {
            getTaskPushNotificationRequestBuilder.id(requestId);
        }

        GetTaskPushNotificationRequest getTaskPushNotificationRequest = getTaskPushNotificationRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(GET_TASK_PUSH_NOTIFICATION_REQUEST, getTaskPushNotificationRequest);
            return unmarshalResponse(httpResponseBody, GET_TASK_PUSH_NOTIFICATION_RESPONSE_REFERENCE);
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
    public SetTaskPushNotificationResponse setTaskPushNotificationConfig(String taskId,
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
    public SetTaskPushNotificationResponse setTaskPushNotificationConfig(String requestId, String taskId,
                                                                         PushNotificationConfig pushNotificationConfig) throws A2AServerException {
        SetTaskPushNotificationRequest.Builder setTaskPushNotificationRequestBuilder = new SetTaskPushNotificationRequest.Builder()
                .jsonrpc(JSONRPC_VERSION)
                .method(SET_TASK_PUSH_NOTIFICATION_REQUEST)
                .params(new TaskPushNotificationConfig(taskId, pushNotificationConfig));

        if (requestId != null) {
            setTaskPushNotificationRequestBuilder.id(requestId);
        }

        SetTaskPushNotificationRequest setTaskPushNotificationRequest = setTaskPushNotificationRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(SET_TASK_PUSH_NOTIFICATION_REQUEST, setTaskPushNotificationRequest);
            return unmarshalResponse(httpResponseBody, SET_TASK_PUSH_NOTIFICATION_RESPONSE_REFERENCE);
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to set task push notification config: " + e);
        }
    }

    /**
     * Send a streaming message to the remote agent.
     *
     * @param messageSendParams the parameters for the message to be sent
     * @param eventHandler a consumer that will be invoked for each event received from the remote agent
     * @param errorHandler a consumer that will be invoked if the remote agent returns an error
     * @param failureHandler a consumer that will be invoked if a failure occurs when processing events
     * @throws A2AServerException if sending the streaming message fails for any reason
     */
    public void sendStreamingMessage(MessageSendParams messageSendParams, Consumer<StreamingEventType> eventHandler,
                                     Consumer<JSONRPCError> errorHandler, Runnable failureHandler) throws A2AServerException {
        sendStreamingMessage(null, messageSendParams, eventHandler, errorHandler, failureHandler);
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
    public void sendStreamingMessage(String requestId, MessageSendParams messageSendParams, Consumer<StreamingEventType> eventHandler,
                                     Consumer<JSONRPCError> errorHandler, Runnable failureHandler) throws A2AServerException {
        checkNotNullParam("messageSendParams", messageSendParams);
        checkNotNullParam("eventHandler", eventHandler);
        checkNotNullParam("errorHandler", errorHandler);
        checkNotNullParam("failureHandler", failureHandler);

        SendStreamingMessageRequest.Builder sendStreamingMessageRequestBuilder = new SendStreamingMessageRequest.Builder()
                .jsonrpc(JSONRPC_VERSION)
                .method(SEND_STREAMING_MESSAGE_REQUEST)
                .params(messageSendParams);

        if (requestId != null) {
            sendStreamingMessageRequestBuilder.id(requestId);
        }

        SendStreamingMessageRequest sendStreamingMessageRequest = sendStreamingMessageRequestBuilder.build();
        SSEEventListener sseEventListener = new SSEEventListener.Builder()
                .eventHandler(eventHandler)
                .errorHandler(errorHandler)
                .failureHandler(failureHandler)
                .build();
        try {
            EventSources.createFactory(httpClient)
                    .newEventSource(createPostRequest(SEND_STREAMING_MESSAGE_REQUEST, sendStreamingMessageRequest,
                            true), sseEventListener);
        } catch (IOException e) {
            throw new A2AServerException("Failed to send streaming message request: " + e);
        }
    }

    private String sendPostRequest(String request, Object value) throws IOException, InterruptedException{
        return sendPostRequest(request, value, false);
    }


    private String sendPostRequest(String request, Object value, boolean addEventStreamHeader) throws IOException, InterruptedException{
        Request okRequest = createPostRequest(request, value, addEventStreamHeader);
        try (Response response = httpClient.newCall(okRequest).execute()) {
            if (! response.isSuccessful()) {
                throw new IOException("Request failed " + response.code());
            }
            return response.body().string();
        }

    }

    private Request createPostRequest(String request, Object value) throws IOException {
        return createPostRequest(request, value, false);
    }

    private Request createPostRequest(String request, Object value, boolean addEventStreamHeader) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(getRequestEndpoint(agentUrl, request))
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(OBJECT_MAPPER.writeValueAsString(value), JSON_MEDIA_TYPE));
        if (addEventStreamHeader) {
            builder.addHeader("Accept", "text/event-stream");
        }
        return builder.build();
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
