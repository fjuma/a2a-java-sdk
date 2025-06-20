package io.a2a.client;

import static io.a2a.util.Utils.unmarshalFrom;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.a2a.http.A2AHttpClient;
import io.a2a.http.A2AHttpResponse;
import io.a2a.spec.A2A;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientJSONError;
import io.a2a.spec.AgentCard;

public class A2ACardResolver {
    private final A2AHttpClient httpClient;
    private final String url;
    private final Map<String, String> authHeaders;

    static String DEFAULT_AGENT_CARD_PATH = "/.well-known/agent.json";

    static final TypeReference<AgentCard> AGENT_CARD_TYPE_REFERENCE = new TypeReference<>() {};
    /**
     * @param httpClient the http client to use
     * @param baseUrl the base URL for the agent whose agent card we want to retrieve
     */
    public A2ACardResolver(A2AHttpClient httpClient, String baseUrl) {
        this(httpClient, baseUrl, null, null);
    }

    /**
     * @param httpClient the http client to use
     * @param baseUrl the base URL for the agent whose agent card we want to retrieve
     * @param agentCardPath optional path to the agent card endpoint relative to the base
     *                         agent URL, defaults to ".well-known/agent.json"
     */
    public A2ACardResolver(A2AHttpClient httpClient, String baseUrl, String agentCardPath) {
        this(httpClient, baseUrl, agentCardPath, null);
    }

    /**
     * @param httpClient the http client to use
     * @param baseUrl the base URL for the agent whose agent card we want to retrieve
     * @param agentCardPath optional path to the agent card endpoint relative to the base
     *                         agent URL, defaults to ".well-known/agent.json"
     * @param authHeaders the HTTP authentication headers to use. May be {@code null}
     */
    public A2ACardResolver(A2AHttpClient httpClient, String baseUrl, String agentCardPath, Map<String, String> authHeaders) {
        this.httpClient = httpClient;
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
        A2AHttpClient.GetBuilder builder = httpClient.createGet()
                .url(url)
                .addHeader("Content-Type", "application/json");

        if (authHeaders != null) {
            for (Map.Entry<String, String> entry : authHeaders.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        String body;
        try {
            A2AHttpResponse response = builder.get();
            if (!response.success()) {
                throw new A2AClientError("Failed to obtain agent card: " + response.status());
            }
            body = response.body();
        } catch (IOException | InterruptedException e) {
            throw new A2AClientError("Failed to obtain agent card", e);
        }

        try {
            return unmarshalFrom(body, AGENT_CARD_TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            throw new A2AClientJSONError("Could not unmarshal agent card response", e);
        }

    }


}
