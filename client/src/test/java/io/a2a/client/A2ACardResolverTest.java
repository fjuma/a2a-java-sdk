package io.a2a.client;

import static io.a2a.client.A2ACardResolver.AGENT_CARD_TYPE_REFERENCE;
import static io.a2a.util.Utils.OBJECT_MAPPER;
import static io.a2a.util.Utils.unmarshalFrom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.a2a.transport.http.A2AHttpResponse;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientJSONError;
import io.a2a.spec.AgentCard;
import io.a2a.transport.http.JdkA2AHttpTransport;
import org.junit.jupiter.api.Test;

public class A2ACardResolverTest {
    @Test
    public void testConstructorStripsSlashes() throws Exception {
        TestHttpTransport transport = new TestHttpTransport();
        transport.body = JsonMessages.AGENT_CARD;

        A2ACardResolver resolver = new A2ACardResolver(transport, "http://example.com/");
        AgentCard card = resolver.getAgentCard();

        assertEquals("http://example.com" + A2ACardResolver.DEFAULT_AGENT_CARD_PATH, transport.url);


        resolver = new A2ACardResolver(transport, "http://example.com");
        card = resolver.getAgentCard();

        assertEquals("http://example.com" + A2ACardResolver.DEFAULT_AGENT_CARD_PATH, transport.url);

        resolver = new A2ACardResolver(transport, "http://example.com/", A2ACardResolver.DEFAULT_AGENT_CARD_PATH);
        card = resolver.getAgentCard();

        assertEquals("http://example.com" + A2ACardResolver.DEFAULT_AGENT_CARD_PATH, transport.url);

        resolver = new A2ACardResolver(transport, "http://example.com", A2ACardResolver.DEFAULT_AGENT_CARD_PATH);
        card = resolver.getAgentCard();

        assertEquals("http://example.com" + A2ACardResolver.DEFAULT_AGENT_CARD_PATH, transport.url);

        resolver = new A2ACardResolver(transport, "http://example.com/", A2ACardResolver.DEFAULT_AGENT_CARD_PATH.substring(0));
        card = resolver.getAgentCard();

        assertEquals("http://example.com" + A2ACardResolver.DEFAULT_AGENT_CARD_PATH, transport.url);

        resolver = new A2ACardResolver(transport, "http://example.com", A2ACardResolver.DEFAULT_AGENT_CARD_PATH.substring(0));
        card = resolver.getAgentCard();

        assertEquals("http://example.com" + A2ACardResolver.DEFAULT_AGENT_CARD_PATH, transport.url);
    }


    @Test
    public void testGetAgentCardSuccess() throws Exception {
        TestHttpTransport client = new TestHttpTransport();
        client.body = JsonMessages.AGENT_CARD;

        A2ACardResolver resolver = new A2ACardResolver(client, "http://example.com/");
        AgentCard card = resolver.getAgentCard();

        AgentCard expectedCard = unmarshalFrom(JsonMessages.AGENT_CARD, AGENT_CARD_TYPE_REFERENCE);
        String expected = OBJECT_MAPPER.writeValueAsString(expectedCard);

        String requestCardString = OBJECT_MAPPER.writeValueAsString(card);
        assertEquals(expected, requestCardString);
    }

    @Test
    public void testGetAgentCardJsonDecodeError() throws Exception {
        TestHttpTransport client = new TestHttpTransport();
        client.body = "X" + JsonMessages.AGENT_CARD;

        A2ACardResolver resolver = new A2ACardResolver(client, "http://example.com/");

        boolean success = false;
        try {
            AgentCard card = resolver.getAgentCard();
            success = true;
        } catch (A2AClientJSONError expected) {
        }
        assertFalse(success);
    }


    @Test
    public void testGetAgentCardRequestError() throws Exception {
        TestHttpTransport client = new TestHttpTransport();
        client.status = 503;

        A2ACardResolver resolver = new A2ACardResolver(client, "http://example.com/");

        String msg = null;
        try {
            AgentCard card = resolver.getAgentCard();
        } catch (A2AClientError expected) {
            msg = expected.getMessage();
        }
        assertTrue(msg.contains("503"));
    }

    private static class TestHttpTransport extends JdkA2AHttpTransport {
        int status = 200;
        String body;
        String url;

        @Override
        public GetBuilder createGet() {
            return new TestGetBuilder();
        }

        @Override
        public PostBuilder createPost() {
            return null;
        }

        class TestGetBuilder implements GetBuilder {

            @Override
            public A2AHttpResponse get() throws IOException, InterruptedException {
                return new A2AHttpResponse() {
                    @Override
                    public int status() {
                        return status;
                    }

                    @Override
                    public boolean success() {
                        return status == 200;
                    }

                    @Override
                    public String body() {
                        return body;
                    }
                };
            }

            @Override
            public CompletableFuture<Void> getAsyncSSE(Consumer<String> messageConsumer, Consumer<Throwable> errorConsumer, Runnable completeRunnable) throws IOException, InterruptedException {
                return null;
            }

            @Override
            public GetBuilder url(String s) {
                url = s;
                return this;
            }

            @Override
            public GetBuilder addHeader(String name, String value) {

                return this;
            }
        }
    }

}