package io.a2a.sdk.pulsar;

import io.a2a.sdk.pulsar.client.A2APulsarClient;
import io.a2a.sdk.pulsar.server.A2APulsarServer;
import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.spec.*;
import org.apache.pulsar.client.api.PulsarClient;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class PulsarHelloWorldTest {

    @Test
    public void testPulsarHelloWorld() throws Exception {
        PulsarTransportConfig transportConfig = new PulsarTransportConfig(
                SingletonPulsarContainer.getPulsarBrokerUrl(),
                "a2a-hello-requests",
                "a2a-hello-streaming"
        );
        PulsarClient pulsarClient = SingletonPulsarContainer.createPulsarClient();
        AgentCard agentCard = new AgentCard.Builder()
                .name("Hello World Agent")
                .description("Pulsar-based hello world agent")
                .version("1.0")
                .skills(List.of())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .transport(transportConfig)
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .build())
                // ... 其他字段
                .build();

        // 3. 启动服务端
        JSONRPCHandler handler = createTestHandler(agentCard); // 复用现有的 AgentExecutorProducer 逻辑
        A2APulsarServer server = new A2APulsarServer(
                pulsarClient,
                "a2a-hello-requests",
                "a2a-hello-streaming",
                handler
        );

        // 4. 测试客户端
        A2APulsarClient client = new A2APulsarClient(agentCard);

        // 同步消息测试
        Message message = A2A.toUserMessage("Hello from Pulsar!");
        MessageSendParams params = new MessageSendParams.Builder()
                .message(message)
                .build();

        SendMessageResponse response = client.sendMessage(params);
        assertNotNull(response.getResult());

        // 流式消息测试
        List<StreamingEventKind> events = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        client.sendStreamingMessage(params,
                event -> {
                    events.add(event);
                    if (event instanceof TaskStatusUpdateEvent && ((TaskStatusUpdateEvent) event).isFinal()) {
                        latch.countDown();
                    }
                },
                error -> fail("Received error: " + error.getMessage()),
                () -> fail("Connection failed")
        );

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertFalse(events.isEmpty());

        // 清理
        client.close();
        server.close();
        pulsarClient.close();
    }

    private JSONRPCHandler createTestHandler(AgentCard agentCard) {
        return new JSONRPCHandler(agentCard, null) {
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
}
