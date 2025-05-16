package io.a2a.client;

import static io.a2a.client.JsonMessages.SEND_MESSAGE_TEST_REQUEST;
import static io.a2a.client.JsonMessages.SEND_MESSAGE_TEST_RESPONSE;
import static io.a2a.client.JsonStreamingMessages.SEND_MESSAGE_STREAMING_TEST_REQUEST;
import static io.a2a.client.JsonStreamingMessages.SEND_MESSAGE_STREAMING_TEST_RESPONSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.ConnectionOptions;
import org.mockserver.model.JsonBody;

import io.a2a.client.sse.SSEEventListener;
import io.a2a.spec.Artifact;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Part;
import io.a2a.spec.StreamingEventType;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;

public class A2AClientStreamingTest {

    private ClientAndServer server;

    @BeforeEach
    public void setUp() {
        server = new ClientAndServer(4001);
    }

    @AfterEach
    public void tearDown() {
        server.stop();
    }

    @Test
    public void testSendStreamingMessageParams() {
        // The goal here is just to verify the correct parameters are being used
        // This is a unit test of the parameter construction, not the streaming itself
        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("test message")))
                .contextId("context-test")
                .messageId("message-test")
                .build();
        
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(false)
                .build();
        
        MessageSendParams params = new MessageSendParams.Builder()
                .id("test-id")
                .message(message)
                .configuration(configuration)
                .build();
        
        assertNotNull(params);
        assertEquals("test-id", params.id());
        assertEquals(message, params.message());
        assertEquals(configuration, params.configuration());
        assertEquals(Message.Role.USER, params.message().getRole());
        assertEquals("test message", ((TextPart) params.message().getParts().get(0)).getText());
    }

    @Test
    public void testA2AClientSendStreamingMessage() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/message/stream")
                                .withBody(JsonBody.json(SEND_MESSAGE_STREAMING_TEST_REQUEST, MatchType.STRICT))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeader("Content-Type", "text/event-stream")
                                .withBody(SEND_MESSAGE_STREAMING_TEST_RESPONSE)
                );

        A2AClient client = new A2AClient("http://localhost:4001");
        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("tell me some jokes")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(false)
                .build();
        MessageSendParams params = new MessageSendParams.Builder()
                .id("1234")
                .message(message)
                .configuration(configuration)
                .build();

        AtomicReference<StreamingEventType> receivedEvent = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Consumer<StreamingEventType> eventHandler = event -> {
            receivedEvent.set(event);
            latch.countDown();
        };
        Consumer<JSONRPCError> errorHandler = error -> {};
        Runnable failureHandler = () -> {};
        client.sendStreamingMessage("request-1234", params, eventHandler, errorHandler, failureHandler);

        boolean eventReceived = latch.await(10, TimeUnit.SECONDS);
        assertTrue(eventReceived);
        assertNotNull(receivedEvent.get());
    }
} 