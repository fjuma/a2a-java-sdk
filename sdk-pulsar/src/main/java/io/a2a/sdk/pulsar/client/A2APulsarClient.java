package io.a2a.sdk.pulsar.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.spec.*;
import io.a2a.util.Utils;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.rpc.contrib.client.PulsarRpcClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class A2APulsarClient implements AutoCloseable {

    private final PulsarClient pulsarClient;
    private final PulsarRpcClient<String, String> rpcClient;
    private final String streamingTopic;
    private final ObjectMapper objectMapper = Utils.OBJECT_MAPPER;

    public A2APulsarClient(AgentCard agentCard) throws A2AClientError {
        if (!agentCard.supportsPulsarTransport()) {
            throw new A2AClientError("Agent card does not support Pulsar transport");
        }

        PulsarTransportConfig config = agentCard.getPulsarConfig();

        try {
            // 初始化 Pulsar 客户端
            this.pulsarClient = PulsarClient.builder()
                    .serviceUrl(config.serviceUrl())
                    .build();

            // 初始化 RPC 客户端（用于同步通信）
            this.rpcClient = PulsarRpcClient.builder(Schema.STRING, Schema.STRING)
                    .requestTopic(config.requestTopic())
                    .replyTopic(config.requestTopic() + "-reply")
                    .replySubscription("client-" + UUID.randomUUID())
                    .replyTimeout(Duration.ofSeconds(30))
                    .requestProducerConfig(new HashMap<>())
                    .patternAutoDiscoveryInterval(Duration.ofSeconds(1))
                    .build(pulsarClient);

            this.streamingTopic = config.streamingTopic();

        } catch (Exception e) {
            throw new A2AClientError("Failed to initialize Pulsar client", e);
        }
    }

    // 同步消息发送 - 复用现有的 SendMessageResponse 结构
    public SendMessageResponse sendMessage(MessageSendParams params) throws A2AServerException {
        return sendMessage(UUID.randomUUID().toString(), params);
    }

    public SendMessageResponse sendMessage(String requestId, MessageSendParams params) throws A2AServerException {
        SendMessageRequest request = new SendMessageRequest.Builder()
                .jsonrpc(A2A.JSONRPC_VERSION)
                .method(A2A.SEND_MESSAGE_METHOD)
                .id(requestId)
                .params(params)
                .build();

        try {
            String requestJson = objectMapper.writeValueAsString(request);
            String correlationId = UUID.randomUUID().toString();

            // 使用 pulsar-rpc 发送同步请求
            String responseJson = rpcClient.request(correlationId, requestJson, new HashMap<>());

            SendMessageResponse response = objectMapper.readValue(responseJson, SendMessageResponse.class);
            if (response.getError() != null) {
                throw new A2AServerException(response.getError().getMessage());
            }

            return response;
        } catch (Exception e) {
            throw new A2AServerException("Failed to send message via Pulsar", e);
        }
    }

    // 流式消息发送 - 复用 SSEEventListener 的逻辑
    public void sendStreamingMessage(MessageSendParams params,
                                     Consumer<StreamingEventKind> eventHandler,
                                     Consumer<JSONRPCError> errorHandler,
                                     Runnable failureHandler) throws A2AServerException {

        String requestId = UUID.randomUUID().toString();
        String replyTopic = "a2a-streaming-replies-" + UUID.randomUUID();

        try {
            // 1. 创建回复 Consumer - 复用 SSEEventListener 的处理逻辑
            AtomicReference<org.apache.pulsar.client.api.Consumer<byte[]>> consumerRef = new AtomicReference<>();

            org.apache.pulsar.client.api.Consumer<byte[]> messageListener = pulsarClient.newConsumer(Schema.BYTES)
                    .topic(replyTopic)
                    .subscriptionName("exclusive-" + UUID.randomUUID())
                    .subscriptionType(SubscriptionType.Exclusive)
                    .messageListener((consumer, msg) -> {
                        try {
                            String messageJson = new String(msg.getData(), StandardCharsets.UTF_8);

                            // 复用 SSEEventListener 的 handleMessage 逻辑
                            JsonNode jsonNode = objectMapper.readTree(messageJson);
                            if (jsonNode.has("error")) {
                                JSONRPCError error = objectMapper.treeToValue(jsonNode.get("error"), JSONRPCError.class);
                                errorHandler.accept(error);
                            } else if (jsonNode.has("result")) {
                                JsonNode result = jsonNode.path("result");
                                StreamingEventKind event = objectMapper.treeToValue(result, StreamingEventKind.class);
                                eventHandler.accept(event);

                                // 检查是否是最终事件
                                if (event instanceof TaskStatusUpdateEvent && ((TaskStatusUpdateEvent) event).isFinal()) {
                                    consumer.close(); // 关闭消费者
                                }
                            }

                            consumer.acknowledge(msg);
                        } catch (Exception e) {
                            consumer.negativeAcknowledge(msg);
                            failureHandler.run();
                        }
                    })
                    .subscribe();

            consumerRef.set(messageListener);

            // 2. 发送流式请求
            Producer<String> producer = pulsarClient.newProducer(Schema.STRING)
                    .topic(streamingTopic)
                    .create();

            SendStreamingMessageRequest request = new SendStreamingMessageRequest.Builder()
                    .jsonrpc(A2A.JSONRPC_VERSION)
                    .method(A2A.SEND_STREAMING_MESSAGE_METHOD)
                    .id(requestId)
                    .params(params)
                    .build();

            String requestJson = objectMapper.writeValueAsString(request);

            producer.newMessage()
                    .value(requestJson)
                    .property("replyTopic", replyTopic)
                    .property("requestId", requestId)
                    .send();

            producer.close();

        } catch (Exception e) {
            throw new A2AServerException("Failed to send streaming message via Pulsar", e);
        }
    }

    // 其他方法：getTask, cancelTask 等 - 用相同的 RPC 模式
    public GetTaskResponse getTask(TaskQueryParams params) throws A2AServerException {
        GetTaskRequest request = new GetTaskRequest.Builder()
                .jsonrpc(A2A.JSONRPC_VERSION)
                .method(A2A.GET_TASK_METHOD)
                .params(params).build();
        return sendRpcRequest(request, GetTaskResponse.class);
    }

    public CancelTaskResponse cancelTask(TaskIdParams params) throws A2AServerException {
        CancelTaskRequest request = new CancelTaskRequest.Builder()
                .jsonrpc(A2A.JSONRPC_VERSION)
                .method(A2A.CANCEL_TASK_METHOD)
                .params(params).build();
        return sendRpcRequest(request, CancelTaskResponse.class);
    }

    private <T extends JSONRPCResponse<?>> T sendRpcRequest(JSONRPCRequest<?> request, Class<T> responseClass) throws A2AServerException {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            String correlationId = UUID.randomUUID().toString();

            String responseJson = rpcClient.request(correlationId, requestJson, new HashMap<>());
            T response = objectMapper.readValue(responseJson, responseClass);

            if (response.getError() != null) {
                throw new A2AServerException(response.getError().getMessage());
            }

            return response;
        } catch (Exception e) {
            throw new A2AServerException("Failed to send RPC request via Pulsar", e);
        }
    }

    @Override
    public void close() throws Exception {
        if (rpcClient != null) {
            rpcClient.close();
        }
        if (pulsarClient != null) {
            pulsarClient.close();
        }
    }
}
