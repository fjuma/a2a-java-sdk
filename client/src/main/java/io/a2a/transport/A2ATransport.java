package io.a2a.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Event;
import io.a2a.spec.JSONRPCRequest;
import io.a2a.spec.JSONRPCResponse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface A2ATransport {

    AgentCard getAgentCard(String method, Map<String, String> authInfo) throws A2AClientError;

    void sendEvent(Event event, String method) throws IOException, InterruptedException;

    <T extends JSONRPCResponse<?>> T sendMessage(
            JSONRPCRequest<?> request, String operation, TypeReference<T> responseTypeRef) throws IOException, InterruptedException;

    <T extends JSONRPCResponse<?>> CompletableFuture<Void> sendMessageStreaming(
            JSONRPCRequest<?> request,
            String operation,
            TypeReference<T> responseTypeRef,
            Consumer<T> responseConsumer,
            Consumer<Throwable> errorConsumer,
            Runnable completeRunnable) throws IOException, InterruptedException;
}
