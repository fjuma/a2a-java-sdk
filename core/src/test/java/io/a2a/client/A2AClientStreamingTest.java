package io.a2a.client;

import io.a2a.spec.*;
import io.a2a.transport.Transport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

public class A2AClientStreamingTest {

    @Test
    public void testSendStreamingMessage() {
        Transport transport = mock(Transport.class);
        A2AClient client = new A2AClient("http://localhost:4001", transport);

        MessageSendParams params = new MessageSendParams.Builder()
                .message(new Message.Builder()
                        .role(Message.Role.USER)
                        .parts(new TextPart("test"))
                        .build())
                .build();

        List<StreamingEventKind> events = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Consumer<StreamingEventKind> onEvent = event -> {
            events.add(event);
            latch.countDown();
        };
        Consumer<JSONRPCError> onError = error -> {};
        Runnable onComplete = () -> {};

        client.sendStreamingMessage(params, onEvent, onError, onComplete);

        verify(transport, times(1)).stream(anyString(), anyString(), any(), any(), any());
    }
}
