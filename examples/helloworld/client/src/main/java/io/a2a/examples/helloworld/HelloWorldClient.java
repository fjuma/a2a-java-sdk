package io.a2a.examples.helloworld;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.client.A2AClient;
import io.a2a.http.A2ACardResolver;
import io.a2a.http.JdkHttpTransport;
import io.a2a.spec.*;

/**
 * A simple example of using the A2A Java SDK to communicate with an A2A server.
 * This example is equivalent to the Python example provided in the A2A Python SDK.
 */
public class HelloWorldClient {

    private static final String SERVER_URL = "http://localhost:9999";
    private static final String MESSAGE_TEXT = "how much is 10 USD in INR?";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        try {
            JdkHttpTransport transport = new JdkHttpTransport();
            A2ACardResolver resolver = new A2ACardResolver(transport, SERVER_URL);
            AgentCard finalAgentCard = null;
            AgentCard publicAgentCard = resolver.getAgentCard();
            System.out.println("Successfully fetched public agent card:");
            System.out.println(OBJECT_MAPPER.writeValueAsString(publicAgentCard));
            System.out.println("Using public agent card for client initialization (default).");
            finalAgentCard = publicAgentCard;

            if (publicAgentCard.supportsAuthenticatedExtendedCard()) {
                System.out.println("Public card supports authenticated extended card. Attempting to fetch from: " + SERVER_URL + "/agent/authenticatedExtendedCard");
                Map<String, String> authHeaders = new HashMap<>();
                authHeaders.put("Authorization", "Bearer dummy-token-for-extended-card");
                A2ACardResolver extendedResolver = new A2ACardResolver(transport, SERVER_URL, "/agent/authenticatedExtendedCard", authHeaders);
                AgentCard extendedAgentCard = extendedResolver.getAgentCard();
                System.out.println("Successfully fetched authenticated extended agent card:");
                System.out.println(OBJECT_MAPPER.writeValueAsString(extendedAgentCard));
                System.out.println("Using AUTHENTICATED EXTENDED agent card for client initialization.");
                finalAgentCard = extendedAgentCard;
            } else {
                System.out.println("Public card does not indicate support for an extended card. Using public card.");
            }

            A2AClient client = new A2AClient(finalAgentCard, transport);
            Message message = A2A.toUserMessage(MESSAGE_TEXT); // the message ID will be automatically generated for you
            MessageSendParams params = new MessageSendParams.Builder()
                .message(message)
                .build();
            SendMessageResponse response = client.sendMessage(params);
            System.out.println("Message sent with ID: " + response.getId());
            System.out.println("Response: " + response.toString());
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

} 