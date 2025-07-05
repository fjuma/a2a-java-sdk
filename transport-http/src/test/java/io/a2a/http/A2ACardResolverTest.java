package io.a2a.http;

import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientJSONError;
import io.a2a.spec.AgentCard;
import io.a2a.transport.Transport;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.a2a.http.A2ACardResolver.AGENT_CARD_TYPE_REFERENCE;
import static io.a2a.util.Utils.OBJECT_MAPPER;
import static io.a2a.util.Utils.unmarshalFrom;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class A2ACardResolverTest {
    @Test
    public void testConstructorStripsSlashes() throws Exception {
        Transport transport = mock(Transport.class);
        when(transport.request("http://example.com/.well-known/agent.json", "")).thenReturn(CompletableFuture.completedFuture(JsonMessages.AGENT_CARD));

        A2ACardResolver resolver = new A2ACardResolver(transport, "http://example.com/");
        AgentCard card = resolver.getAgentCard();
        assertNotNull(card);

        resolver = new A2ACardResolver(transport, "http://example.com");
        card = resolver.getAgentCard();
        assertNotNull(card);

        resolver = new A2ACardResolver(transport, "http://example.com/", "/.well-known/agent.json");
        card = resolver.getAgentCard();
        assertNotNull(card);

        resolver = new A2ACardResolver(transport, "http://example.com", "/.well-known/agent.json");
        card = resolver.getAgentCard();
        assertNotNull(card);

        resolver = new A2ACardResolver(transport, "http://example.com/", ".well-known/agent.json");
        card = resolver.getAgentCard();
        assertNotNull(card);

        resolver = new A2ACardResolver(transport, "http://example.com", ".well-known/agent.json");
        card = resolver.getAgentCard();
        assertNotNull(card);
    }


    @Test
    public void testGetAgentCardSuccess() throws Exception {
        Transport transport = mock(Transport.class);
        when(transport.request("http://example.com/.well-known/agent.json", "")).thenReturn(CompletableFuture.completedFuture(JsonMessages.AGENT_CARD));

        A2ACardResolver resolver = new A2ACardResolver(transport, "http://example.com/");
        AgentCard card = resolver.getAgentCard();

        AgentCard expectedCard = unmarshalFrom(JsonMessages.AGENT_CARD, AGENT_CARD_TYPE_REFERENCE);
        String expected = OBJECT_MAPPER.writeValueAsString(expectedCard);

        String requestCardString = OBJECT_MAPPER.writeValueAsString(card);
        assertEquals(expected, requestCardString);
    }

    @Test
    public void testGetAgentCardJsonDecodeError() {
        Transport transport = mock(Transport.class);
        when(transport.request("http://example.com/.well-known/agent.json", "")).thenReturn(CompletableFuture.completedFuture("X" + JsonMessages.AGENT_CARD));

        A2ACardResolver resolver = new A2ACardResolver(transport, "http://example.com/");

        assertThrows(A2AClientJSONError.class, resolver::getAgentCard);
    }


    @Test
    public void testGetAgentCardRequestError() {
        Transport transport = mock(Transport.class);
        when(transport.request("http://example.com/.well-known/agent.json", "")).thenReturn(CompletableFuture.failedFuture(new ExecutionException(new A2AClientError("Failed to obtain agent card"))));

        A2ACardResolver resolver = new A2ACardResolver(transport, "http://example.com/");

        assertThrows(A2AClientError.class, resolver::getAgentCard);
    }
}
