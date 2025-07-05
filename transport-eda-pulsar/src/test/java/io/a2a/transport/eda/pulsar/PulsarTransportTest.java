package io.a2a.transport.eda.pulsar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.spec.A2A;
import io.a2a.spec.InternalError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.NonStreamingJSONRPCRequest;
import io.a2a.spec.PulsarTransportConfig;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SendStreamingMessageResponse;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.StreamingJSONRPCRequest;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.util.Utils;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.rpc.contrib.server.PulsarRpcServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class PulsarTransportTest {

    private static PulsarTransport transport;
    private static A2APulsarTestServer server;
    private static PulsarClient pulsarClient;

    private static final String REQUEST_TOPIC = "a2a-test-requests";
    private static final String STREAMING_TOPIC = "a2a-test-streaming";

    @BeforeAll
    public static void setup() throws Exception {
        pulsarClient = SingletonPulsarContainer.createPulsarClient();
        PulsarTransportConfig transportConfig = new PulsarTransportConfig(
                SingletonPulsarContainer.getPulsarBrokerUrl(),
                REQUEST_TOPIC,
                STREAMING_TOPIC
        );
        transport = new PulsarTransport(transportConfig);
        server = new A2APulsarTestServer(pulsarClient, REQUEST_TOPIC, STREAMING_TOPIC, createTestHandler());
    }

    @AfterAll
    public static void tearDown() throws Exception {
        transport.close();
        server.close();
        pulsarClient.close();
    }

    @Test
    public void testRequest() throws Exception {
        SendMessageRequest request = new SendMessageRequest.Builder()
                .jsonrpc(A2A.JSONRPC_VERSION)
                .method(A2A.SEND_MESSAGE_METHOD)
                .id("test-request-1")
                .params(new MessageSendParams.Builder().message(A2A.toUserMessage("hello")).build())
                .build();

        String requestJson = Utils.OBJECT_MAPPER.writeValueAsString(request);
        String responseJson = transport.request("", requestJson).get(5, TimeUnit.SECONDS);

        assertNotNull(responseJson);
        SendMessageResponse response = Utils.OBJECT_MAPPER.readValue(responseJson, SendMessageResponse.class);
        assertEquals("test-request-1", response.getId());
        assertNotNull(response.getResult());
    }

    @Test
    public void testStream() throws Exception {
        SendStreamingMessageRequest request = new SendStreamingMessageRequest.Builder()
                .jsonrpc(A2A.JSONRPC_VERSION)
                .method(A2A.SEND_STREAMING_MESSAGE_METHOD)
                .id("test-stream-1")
                .params(new MessageSendParams.Builder().message(A2A.toUserMessage("hello stream")).build())
                .build();

        String requestJson = Utils.OBJECT_MAPPER.writeValueAsString(request);
        List<StreamingEventKind> events = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        transport.stream("", requestJson,
                event -> {
                    events.add(event);
                    if (event instanceof TaskStatusUpdateEvent && ((TaskStatusUpdateEvent) event).isFinal()) {
                        latch.countDown();
                    }
                },
                error -> fail("Received error: " + error.getMessage()),
                () -> {
                }
        );

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertFalse(events.isEmpty());
    }

    private static JSONRPCHandler createTestHandler() {
        return new JSONRPCHandler(null, null) {
            @Override
            public SendMessageResponse onMessageSend(SendMessageRequest request) {
                return new SendMessageResponse(request.getId(), new Task.Builder()
                        .id("task-123")
                        .contextId("context-456")
                        .status(new TaskStatus(TaskState.SUBMITTED))
                        .build());
            }

            @Override
            public Flow.Publisher<SendStreamingMessageResponse> onMessageSendStream(SendStreamingMessageRequest request) {
                return subscriber -> {
                    subscriber.onSubscribe(new Flow.Subscription() {
                        @Override
                        public void request(long n) {
                            subscriber.onNext(new SendStreamingMessageResponse(request.getId(), new TaskStatusUpdateEvent.Builder()
                                    .taskId("task-123")
                                    .contextId("context-456")
                                    .status(new TaskStatus(TaskState.COMPLETED))
                                    .isFinal(true)
                                    .build()));
                            subscriber.onComplete();
                        }

                        @Override
                        public void cancel() {
                        }
                    });
                };
            }
        };
    }

    // Inner class for the test server
    private static class A2APulsarTestServer implements AutoCloseable {

        private final PulsarRpcServer<String, String> rpcServer;
        private final Consumer<String> streamingConsumer;
        private final PulsarClient pulsarClient;
        private final JSONRPCHandler jsonRpcHandler;
        private final ObjectMapper objectMapper = Utils.OBJECT_MAPPER;

        public A2APulsarTestServer(PulsarClient pulsarClient,
                                   String requestTopic,
                                   String streamingTopic,
                                   JSONRPCHandler handler) throws Exception {

            this.pulsarClient = pulsarClient;
            this.jsonRpcHandler = handler;

            Function<String, CompletableFuture<String>> requestFunction = this::handleSyncRequest;

            this.rpcServer = PulsarRpcServer.builder(Schema.STRING, Schema.STRING)
                    .requestTopic(requestTopic)
                    .requestSubscription("a2a-sync-handler")
                    .patternAutoDiscoveryInterval(Duration.ofSeconds(1))
                    .build(pulsarClient, requestFunction, this::rollbackRequest);

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
            if (request instanceof SendMessageRequest) {
                return jsonRpcHandler.onMessageSend((SendMessageRequest) request);
            }
            return new JSONRPCErrorResponse(request.getId(), new MethodNotFoundError());
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

                StreamingJSONRPCRequest<?> request = objectMapper.readValue(requestJson, StreamingJSONRPCRequest.class);

                Producer<String> replyProducer = pulsarClient.newProducer(Schema.STRING)
                        .topic(replyTopic)
                        .create();

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
                } else {
                    SendStreamingMessageResponse errorResponse = new SendStreamingMessageResponse(
                            requestId, new MethodNotFoundError());
                    String responseJson = objectMapper.writeValueAsString(errorResponse);
                    replyProducer.send(responseJson);
                    replyProducer.close();
                    return;
                }

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
}
