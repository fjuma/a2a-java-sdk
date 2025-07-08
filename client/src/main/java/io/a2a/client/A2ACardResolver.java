package io.a2a.client;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import io.a2a.transport.A2ATransport;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientJSONError;
import io.a2a.spec.AgentCard;

public class A2ACardResolver {
    private final A2ATransport transport;
    private final String url;
    private final Map<String, String> authHeaders;

    static String DEFAULT_AGENT_CARD_PATH = "/.well-known/agent.json";

    static final TypeReference<AgentCard> AGENT_CARD_TYPE_REFERENCE = new TypeReference<>() {};
    /**
     * @param transport the transport to use
     * @param baseUrl the base URL for the agent whose agent card we want to retrieve
     */
    public A2ACardResolver(A2ATransport transport, String baseUrl) {
        this(transport, baseUrl, null, null);
    }

    /**
     * @param transport the transport to use
     * @param baseUrl the base URL for the agent whose agent card we want to retrieve
     * @param agentCardPath optional path to the agent card endpoint relative to the base
     *                         agent URL, defaults to ".well-known/agent.json"
     */
    public A2ACardResolver(A2ATransport transport, String baseUrl, String agentCardPath) {
        this(transport, baseUrl, agentCardPath, null);
    }

    /**
     * @param transport the transport to use
     * @param baseUrl the base URL for the agent whose agent card we want to retrieve
     * @param agentCardPath optional path to the agent card endpoint relative to the base
     *                         agent URL, defaults to ".well-known/agent.json"
     * @param authHeaders the HTTP authentication headers to use. May be {@code null}
     */
    public A2ACardResolver(A2ATransport transport, String baseUrl, String agentCardPath, Map<String, String> authHeaders) {
        this.transport = transport;
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        agentCardPath = agentCardPath == null || agentCardPath.isEmpty() ? DEFAULT_AGENT_CARD_PATH : agentCardPath;
        if (agentCardPath.startsWith("/")) {
            agentCardPath = agentCardPath.substring(1);
        }
        this.url = baseUrl + agentCardPath;
        this.authHeaders = authHeaders;
    }

    /**
     * Get the agent card for the configured A2A agent.
     *
     * @return the agent card
     * @throws A2AClientError If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError f the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public AgentCard getAgentCard() throws A2AClientError, A2AClientJSONError {
        return transport.getAgentCard(url, authHeaders);
    }


}
