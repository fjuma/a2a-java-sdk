package io.a2a.client;

import static io.a2a.grpc.A2AServiceGrpc.A2AServiceBlockingV2Stub;
import static io.a2a.grpc.A2AServiceGrpc.A2AServiceStub;
import static io.a2a.grpc.utils.ProtoUtils.FromProto;
import static io.a2a.grpc.utils.ProtoUtils.ToProto;
import static io.a2a.util.Assert.checkNotNullParam;

import java.util.function.Consumer;

import io.a2a.client.sse.SSEStreamObserver;
import io.a2a.grpc.A2AServiceGrpc;
import io.a2a.grpc.CancelTaskRequest;
import io.a2a.grpc.CreateTaskPushNotificationConfigRequest;
import io.a2a.grpc.GetTaskPushNotificationConfigRequest;
import io.a2a.grpc.GetTaskRequest;
import io.a2a.grpc.SendMessageRequest;
import io.a2a.grpc.SendMessageResponse;
import io.a2a.grpc.StreamResponse;
import io.a2a.grpc.utils.ProtoUtils;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

/**
 * A2A Client for interacting with an A2A agent via gRPC.
 */
public class A2AGrpcClient {

    private A2AServiceBlockingV2Stub blockingStub;
    private A2AServiceStub asyncStub;
    private AgentCard agentCard;

    /**
     * Create an A2A client for interacting with an A2A agent via gRPC.
     *
     * @param channel the gRPC channel
     * @param agentCard the agent card for the A2A server this client will be communicating with
     */
    public A2AGrpcClient(Channel channel, AgentCard agentCard) {
        checkNotNullParam("channel", channel);
        checkNotNullParam("agentCard", agentCard);
        this.asyncStub = A2AServiceGrpc.newStub(channel);
        this.blockingStub = A2AServiceGrpc.newBlockingV2Stub(channel);
        this.agentCard = agentCard;
    }

    /**
     * Send a message to the remote agent.
     *
     * @param messageSendParams the parameters for the message to be sent
     * @return the response, may be a message or a task
     * @throws A2AServerException if sending the message fails for any reason
     */
    public EventKind sendMessage(MessageSendParams messageSendParams) throws A2AServerException {
        SendMessageRequest request = createGrpcSendMessageRequestFromMessageSendParams(messageSendParams);
        try {
            SendMessageResponse response = blockingStub.sendMessage(request);
            if (response.hasMsg()) {
                return FromProto.message(response.getMsg());
            } else if (response.hasTask()) {
                return FromProto.task(response.getTask());
            } else {
                throw new A2AServerException("Server response did not contain a message or task");
            }
        } catch (StatusRuntimeException e) {
            throw new A2AServerException("Failed to send message: " + e, e);
        }
    }

    /**
     * Retrieves the current state and history of a specific task.
     *
     * @param taskQueryParams the params for the task to be queried
     * @return the task
     * @throws A2AServerException if retrieving the task fails for any reason
     */
    public Task getTask(TaskQueryParams taskQueryParams) throws A2AServerException {
        GetTaskRequest.Builder requestBuilder = GetTaskRequest.newBuilder();
        requestBuilder.setName("tasks/" + taskQueryParams.id());
        if (taskQueryParams.historyLength() != null) {
            requestBuilder.setHistoryLength(taskQueryParams.historyLength());
        }
        GetTaskRequest getTaskRequest = requestBuilder.build();
        try {
            return FromProto.task(blockingStub.getTask(getTaskRequest));
        } catch (StatusRuntimeException e) {
            throw new A2AServerException("Failed to get task: " + e, e);
        }
    }

    /**
     * Cancel a task that was previously submitted to the A2A server.
     *
     * @param taskIdParams the params for the task to be cancelled
     * @return the updated task
     * @throws A2AServerException if cancelling the task fails for any reason
     */
    public Task cancelTask(TaskIdParams taskIdParams) throws A2AServerException {
        CancelTaskRequest cancelTaskRequest = CancelTaskRequest.newBuilder()
                .setName("tasks/" + taskIdParams.id())
                .build();
        try {
            return FromProto.task(blockingStub.cancelTask(cancelTaskRequest));
        } catch (StatusRuntimeException e) {
            throw new A2AServerException("Failed to cancel task: " + e, e);
        }
    }

    /**
     * Set push notification configuration for a task.
     *
     * @param taskPushNotificationConfig the task push notification configuration
     * @return the task push notification config
     * @throws A2AServerException if setting the push notification configuration fails for any reason
     */
    public TaskPushNotificationConfig setTaskPushNotificationConfig(TaskPushNotificationConfig taskPushNotificationConfig) throws A2AServerException {
        String configId = taskPushNotificationConfig.pushNotificationConfig().id();
        CreateTaskPushNotificationConfigRequest request = CreateTaskPushNotificationConfigRequest.newBuilder()
                .setParent("tasks/" + taskPushNotificationConfig.taskId())
                .setConfig(ToProto.taskPushNotificationConfig(taskPushNotificationConfig))
                .setConfigId(configId == null ? "" : configId)
                .build();
        try {
            return FromProto.taskPushNotificationConfig(blockingStub.createTaskPushNotificationConfig(request));
        } catch (StatusRuntimeException e) {
            throw new A2AServerException("Failed to set the task push notification config: " + e, e);
        }
    }

    /**
     * Get the push notification configuration for a task.
     *
     * @param getTaskPushNotificationConfigParams the params for the task
     * @return the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public TaskPushNotificationConfig getTaskPushNotificationConfig(GetTaskPushNotificationConfigParams getTaskPushNotificationConfigParams) throws A2AServerException {
        GetTaskPushNotificationConfigRequest getTaskPushNotificationConfigRequest = GetTaskPushNotificationConfigRequest.newBuilder()
                .setName(getTaskPushNotificationConfigName(getTaskPushNotificationConfigParams))
                .build();
        try {
            return FromProto.taskPushNotificationConfig(blockingStub.getTaskPushNotificationConfig(getTaskPushNotificationConfigRequest));
        } catch (StatusRuntimeException e) {
            throw new A2AServerException("Failed to get the task push notification config: " + e, e);
        }
    }

    /**
     * Send a streaming message request to the remote agent.
     *
     * @param messageSendParams the parameters for the message to be sent
     * @param eventHandler a consumer that will be invoked for each event received from the remote agent
     * @param errorHandler a consumer that will be invoked if an error occurs
     * @throws A2AServerException if sending the streaming message fails for any reason
     */
    public void sendMessageStreaming(MessageSendParams messageSendParams, Consumer<StreamingEventKind> eventHandler,
                                     Consumer<Throwable> errorHandler) throws A2AServerException {
        SendMessageRequest request = createGrpcSendMessageRequestFromMessageSendParams(messageSendParams);
        StreamObserver<StreamResponse> streamObserver = new SSEStreamObserver(eventHandler, errorHandler);
        try {
            asyncStub.sendStreamingMessage(request, streamObserver);
        } catch (StatusRuntimeException e) {
            throw new A2AServerException("Failed to send streaming message: " + e, e);
        }
    }

    private SendMessageRequest createGrpcSendMessageRequestFromMessageSendParams(MessageSendParams messageSendParams) {
        SendMessageRequest.Builder builder = SendMessageRequest.newBuilder();
        builder.setRequest(ToProto.message(messageSendParams.message()));
        if (messageSendParams.configuration() != null) {
            builder.setConfiguration(ToProto.messageSendConfiguration(messageSendParams.configuration()));
        }
        if (messageSendParams.metadata() != null) {
            builder.setMetadata(ToProto.struct(messageSendParams.metadata()));
        }
        return builder.build();
    }

    private String getTaskPushNotificationConfigName(GetTaskPushNotificationConfigParams getTaskPushNotificationConfigParams) {
        StringBuilder name = new StringBuilder();
        name.append("tasks/");
        name.append(getTaskPushNotificationConfigParams.id());
        if (getTaskPushNotificationConfigParams.pushNotificationConfigId() != null) {
            name.append("/pushNotificationConfigs/");
            name.append(getTaskPushNotificationConfigParams.pushNotificationConfigId());
        }
        return name.toString();
    }
}
