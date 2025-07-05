package io.a2a.sdk.pulsar.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.spec.*;
import io.a2a.spec.InternalError;
import io.a2a.util.Utils;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.rpc.contrib.server.PulsarRpcServer;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Function;

public class A2APulsarServer implements AutoCloseable {

    private final PulsarRpcServer<String, String> rpcServer;
    private final Consumer<String> streamingConsumer;
    private final PulsarClient pulsarClient;
    private final JSONRPCHandler jsonRpcHandler;
    private final ObjectMapper objectMapper = Utils.OBJECT_MAPPER;

    public A2APulsarServer(PulsarClient pulsarClient,
                           String requestTopic,
                           String streamingTopic,
                           JSONRPCHandler handler) throws Exception {

        this.pulsarClient = pulsarClient;
        this.jsonRpcHandler = handler;

        // 1. 初始化 RPC 服务器处理同步请求
        Function<String, CompletableFuture<String>> requestFunction = this::handleSyncRequest;

        this.rpcServer = PulsarRpcServer.builder(Schema.STRING, Schema.STRING)
                .requestTopic(requestTopic)
                .requestSubscription("a2a-sync-handler")
                .patternAutoDiscoveryInterval(Duration.ofSeconds(1))
                .build(pulsarClient, requestFunction, this::rollbackRequest);

        // 2. 初始化流式请求消费者
        this.streamingConsumer = pulsarClient.newConsumer(Schema.STRING)
                .topic(streamingTopic)
                .subscriptionName("a2a-streaming-handler")
                .subscriptionType(SubscriptionType.Shared)
                .messageListener(this::handleStreamingRequest)
                .subscribe();
    }

    private CompletableFuture<String> handleSyncRequest(String requestJson) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 复用现有的 JSONRPCHandler 逻辑
                NonStreamingJSONRPCRequest<?> request = objectMapper.readValue(requestJson, NonStreamingJSONRPCRequest.class);
                JSONRPCResponse<?> response = dispatchSyncRequest(request);
                return objectMapper.writeValueAsString(response);
            } catch (Exception e) {
                try {
                    JSONRPCErrorResponse errorResponse = new JSONRPCErrorResponse(new InternalError(e.getMessage()));
                    return objectMapper.writeValueAsString(errorResponse);
                } catch (JsonProcessingException jpe) {
                    throw new RuntimeException("Failed to serialize error response", jpe);
                }
            }
        });
    }

    private JSONRPCResponse<?> dispatchSyncRequest(NonStreamingJSONRPCRequest<?> request) {
        // 完全复用现有的 JSONRPCHandler 逻辑
        if (request instanceof SendMessageRequest) {
            return jsonRpcHandler.onMessageSend((SendMessageRequest) request);
        } else if (request instanceof GetTaskRequest) {
            return jsonRpcHandler.onGetTask((GetTaskRequest) request);
        } else if (request instanceof CancelTaskRequest) {
            return jsonRpcHandler.onCancelTask((CancelTaskRequest) request);
        } // ... 其他同步请求类型
        else {
            return new JSONRPCErrorResponse(request.getId(), new MethodNotFoundError());
        }
    }

    private void handleStreamingRequest(Consumer<String> consumer, Message<String> msg) {
        try {
            String requestJson = msg.getValue();
            String replyTopic = msg.getProperty("replyTopic");
            String requestId = msg.getProperty("requestId");

            if (replyTopic == null || requestId == null) {
                consumer.negativeAcknowledge(msg);
                return;
            }

            // 反序列化流式请求
            StreamingJSONRPCRequest<?> request = objectMapper.readValue(requestJson, StreamingJSONRPCRequest.class);

            // 创建回复 Producer
            Producer<String> replyProducer = pulsarClient.newProducer(Schema.STRING)
                    .topic(replyTopic)
                    .create();

            // 处理流式请求并发布事件
            handleStreamingResponse(request, replyProducer, requestId);

            consumer.acknowledge(msg);

        } catch (Exception e) {
            consumer.negativeAcknowledge(msg);
        }
    }

    private void handleStreamingResponse(StreamingJSONRPCRequest<?> request, Producer<String> replyProducer, String requestId) {
        try {
            Flow.Publisher<SendStreamingMessageResponse> publisher;

            if (request instanceof SendStreamingMessageRequest) {
                publisher = jsonRpcHandler.onMessageSendStream((SendStreamingMessageRequest) request);
            } else if (request instanceof TaskResubscriptionRequest) {
                publisher = jsonRpcHandler.onResubscribeToTask((TaskResubscriptionRequest) request);
            } else {
                // 发送错误响应
                SendStreamingMessageResponse errorResponse = new SendStreamingMessageResponse(
                        requestId, new MethodNotFoundError());
                String responseJson = objectMapper.writeValueAsString(errorResponse);
                replyProducer.send(responseJson);
                replyProducer.close();
                return;
            }

            // 订阅事件流并转发到 Pulsar
            publisher.subscribe(new Flow.Subscriber<SendStreamingMessageResponse>() {
                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    subscription.request(1);
                }

                @Override
                public void onNext(SendStreamingMessageResponse response) {
                    try {
                        String responseJson = objectMapper.writeValueAsString(response);
                        replyProducer.sendAsync(responseJson);
                        subscription.request(1);
                    } catch (Exception e) {
                        onError(e);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    try {
                        SendStreamingMessageResponse errorResponse = new SendStreamingMessageResponse(
                                requestId, new InternalError(throwable.getMessage()));
                        String responseJson = objectMapper.writeValueAsString(errorResponse);
                        replyProducer.send(responseJson);
                    } catch (Exception e) {
                        // Log error
                    } finally {
                        cleanup();
                    }
                }

                @Override
                public void onComplete() {
                    cleanup();
                }

                private void cleanup() {
                    try {
                        replyProducer.close();
                    } catch (Exception e) {
                        // Log error
                    }
                }
            });

        } catch (Exception e) {
            try {
                replyProducer.close();
            } catch (Exception ex) {
                // Log error
            }
        }
    }

    private void rollbackRequest(String correlationId, String request) {
        // 实现回滚逻辑，如果需要的话
    }

    @Override
    public void close() throws Exception {
        if (rpcServer != null) {
            rpcServer.close();
        }
        if (streamingConsumer != null) {
            streamingConsumer.close();
        }
    }
}
