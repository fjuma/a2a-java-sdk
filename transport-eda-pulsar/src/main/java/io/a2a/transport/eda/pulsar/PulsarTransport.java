package io.a2a.transport.eda.pulsar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.PulsarTransportConfig;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.transport.Transport;
import io.a2a.util.Utils;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.rpc.contrib.client.PulsarRpcClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class PulsarTransport implements Transport, AutoCloseable {

    private final PulsarClient pulsarClient;
    private final PulsarRpcClient<String, String> rpcClient;
    private final String streamingTopic;
    private final ObjectMapper objectMapper = Utils.OBJECT_MAPPER;

    public PulsarTransport(PulsarTransportConfig config) throws A2AClientError {
        try {
            this.pulsarClient = PulsarClient.builder()
                    .serviceUrl(config.serviceUrl())
                    .build();

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

    @Override
    public CompletableFuture<String> request(String url, String body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String correlationId = UUID.randomUUID().toString();
                return rpcClient.request(correlationId, body, new HashMap<>());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void stream(String url, String body, Consumer<StreamingEventKind> onEvent, Consumer<JSONRPCError> onError,
                       Runnable onComplete) {
        String replyTopic = "a2a-streaming-replies-" + UUID.randomUUID();
        String requestId = UUID.randomUUID().toString();

        try {
            AtomicReference<org.apache.pulsar.client.api.Consumer<byte[]>> consumerRef = new AtomicReference<>();

            org.apache.pulsar.client.api.Consumer<byte[]> messageListener = pulsarClient.newConsumer(Schema.BYTES)
                    .topic(replyTopic)
                    .subscriptionName("exclusive-" + UUID.randomUUID())
                    .subscriptionType(SubscriptionType.Exclusive)
                    .messageListener((consumer, msg) -> {
                        try {
                            String messageJson = new String(msg.getData(), StandardCharsets.UTF_8);
                            JsonNode jsonNode = objectMapper.readTree(messageJson);
                            if (jsonNode.has("error")) {
                                JSONRPCError error = objectMapper.treeToValue(jsonNode.get("error"),
                                        JSONRPCError.class);
                                onError.accept(error);
                            } else if (jsonNode.has("result")) {
                                JsonNode result = jsonNode.path("result");
                                StreamingEventKind event = objectMapper.treeToValue(result, StreamingEventKind.class);
                                onEvent.accept(event);

                                if (event instanceof TaskStatusUpdateEvent
                                        && ((TaskStatusUpdateEvent) event).isFinal()) {
                                    consumer.close();
                                    onComplete.run();
                                }
                            }
                            consumer.acknowledge(msg);
                        } catch (Exception e) {
                            consumer.negativeAcknowledge(msg);
                            onError.accept(new JSONRPCError(-32603, "Internal error", e.getMessage()));
                        }
                    })
                    .subscribe();

            consumerRef.set(messageListener);

            Producer<String> producer = pulsarClient.newProducer(Schema.STRING)
                    .topic(streamingTopic)
                    .create();

            producer.newMessage()
                    .value(body)
                    .property("replyTopic", replyTopic)
                    .property("requestId", requestId)
                    .send();

            producer.close();

        } catch (Exception e) {
            onError.accept(new JSONRPCError(-32603, "Internal error", e.getMessage()));
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
