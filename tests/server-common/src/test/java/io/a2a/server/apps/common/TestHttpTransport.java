package io.a2a.server.apps.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Event;
import io.a2a.spec.JSONRPCRequest;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.transport.http.A2AHttpTransport;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;

import io.a2a.transport.http.A2AHttpResponse;
import io.a2a.spec.Task;
import io.a2a.util.Utils;

@Dependent
@Alternative
public class TestHttpTransport implements A2AHttpTransport {
    final List<Task> tasks = Collections.synchronizedList(new ArrayList<>());
    volatile CountDownLatch latch;

    @Override
    public GetBuilder createGet() {
        return null;
    }

    @Override
    public PostBuilder createPost() {
        return new TestPostBuilder();
    }

    @Override
    public AgentCard getAgentCard(String method, Map<String, String> authInfo) throws A2AClientError {
        return null;
    }

    @Override
    public void sendEvent(Event event, String method) throws IOException, InterruptedException {

    }

    @Override
    public <T extends JSONRPCResponse<?>> T sendMessage(JSONRPCRequest<?> request, String operation, TypeReference<T> responseTypeRef) throws IOException, InterruptedException {
        return null;
    }

    @Override
    public <T extends JSONRPCResponse<?>> CompletableFuture<Void> sendMessageStreaming(JSONRPCRequest<?> request, String operation, TypeReference<T> responseTypeRef, Consumer<T> responseConsumer, Consumer<Throwable> errorConsumer, Runnable completeRunnable) throws IOException, InterruptedException {
        return null;
    }

    class TestPostBuilder implements PostBuilder {
        private volatile String body;
        @Override
        public PostBuilder body(String body) {
            this.body = body;
            return this;
        }

        @Override
        public A2AHttpResponse post() throws IOException, InterruptedException {
            tasks.add(Utils.OBJECT_MAPPER.readValue(body, Task.TYPE_REFERENCE));
            try {
                return new A2AHttpResponse() {
                    @Override
                    public int status() {
                        return 200;
                    }

                    @Override
                    public boolean success() {
                        return true;
                    }

                    @Override
                    public String body() {
                        return "";
                    }
                };
            } finally {
                latch.countDown();
            }
        }

        @Override
        public CompletableFuture<Void> postAsyncSSE(Consumer<String> messageConsumer, Consumer<Throwable> errorConsumer, Runnable completeRunnable) throws IOException, InterruptedException {
            return null;
        }

        @Override
        public PostBuilder url(String s) {
            return this;
        }

        @Override
        public PostBuilder addHeader(String name, String value) {
            return this;
        }
    }
}