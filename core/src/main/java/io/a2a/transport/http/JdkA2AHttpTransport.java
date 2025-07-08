package io.a2a.transport.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.a2a.spec.AgentCard;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientJSONError;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCRequest;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.Event;
import io.a2a.util.Utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

import static io.a2a.util.Utils.OBJECT_MAPPER;
import static io.a2a.util.Utils.unmarshalFrom;

public class JdkA2AHttpTransport implements A2AHttpTransport {
    private static final TypeReference<AgentCard> AGENT_CARD_TYPE_REFERENCE = new TypeReference<>() { };


    private final HttpClient httpClient;

    public JdkA2AHttpTransport() {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public AgentCard getAgentCard(String method, Map<String, String> authInfo) throws A2AClientError {
        GetBuilder builder = createGet()
                .url(method)
                .addHeader("Content-Type", "application/json");

        if (authInfo != null) {
            for (Map.Entry<String, String> entry : authInfo.entrySet()) {
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

    @Override
    public void sendEvent(Event event, String method) throws IOException, InterruptedException {
        String body = Utils.OBJECT_MAPPER.writeValueAsString(event);
        createPost().url(method).body(body).post();
    }

    @Override
    public <T extends JSONRPCResponse<?>> T sendMessage(
            JSONRPCRequest<?> request, String operation, TypeReference<T> responseTypeRef) throws IOException, InterruptedException {

        PostBuilder postBuilder = createPostBuilder(request, operation);
        A2AHttpResponse response = postBuilder.post();

        if (!response.success()) {
            throw new IOException("Request failed " + response.status());
        }

        return unmarshalResponse(response.body(), responseTypeRef);
    }

    @Override
    public <T extends JSONRPCResponse<?>> CompletableFuture<Void> sendMessageStreaming(
            JSONRPCRequest<?> request, String operation, TypeReference<T> responseTypeRef,
            Consumer<T> responseConsumer, Consumer<Throwable> errorConsumer, Runnable completeRunnable) throws IOException, InterruptedException {
        PostBuilder postBuilder = createPostBuilder(request, operation);

        return postBuilder.postAsyncSSE(message -> {
            try {
                T response = unmarshalResponse(message, responseTypeRef);
                responseConsumer.accept(response);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, errorConsumer, completeRunnable);
    }


    @Override
    public GetBuilder createGet() {
        return new JdkGetBuilder();
    }

    @Override
    public PostBuilder createPost() {
        return new JdkPostBuilder();
    }


    private PostBuilder createPostBuilder(JSONRPCRequest<?> request, String method) throws JsonProcessingException {
        return createPost()
                .url(method)
                .addHeader("Content-Type", "application/json")
                .body(OBJECT_MAPPER.writeValueAsString(request));
    }

    private <T extends JSONRPCResponse<?>> T unmarshalResponse(String response, TypeReference<T> typeReference)
            throws A2AServerException, JsonProcessingException {
        T value = unmarshalFrom(response, typeReference);
        JSONRPCError error = value.getError();
        if (error != null) {
            throw new A2AServerException(error.getMessage() + (error.getData() != null ? ": " + error.getData() : ""));
        }
        return value;
    }

    private abstract class JdkBuilder<T extends Builder<T>> implements Builder<T> {
        private String url;
        private final Map<String, String> headers = new HashMap<>();

        @Override
        public T url(String url) {
            this.url = url;
            return self();
        }

        @Override
        public T addHeader(String name, String value) {
            headers.put(name, value);
            return self();
        }

        @SuppressWarnings("unchecked")
        T self() {
            return (T) this;
        }

        protected HttpRequest.Builder createRequestBuilder() throws IOException {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url));
            for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
                builder.header(headerEntry.getKey(), headerEntry.getValue());
            }
            return builder;
        }

        protected CompletableFuture<Void> asyncRequest(
                HttpRequest request,
                Consumer<String> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable
        ) {
            Flow.Subscriber<String> subscriber = new Flow.Subscriber<String>() {
                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    subscription.request(1);
                }

                @Override
                public void onNext(String item) {
                    // SSE messages sometimes start with "data:". Strip that off
                    if (item != null && item.startsWith("data:")) {
                        item = item.substring(5).trim();
                        if (!item.isEmpty()) {
                            messageConsumer.accept(item);
                        }
                    }
                    subscription.request(1);
                }

                @Override
                public void onError(Throwable throwable) {
                    errorConsumer.accept(throwable);
                    subscription.cancel();
                }

                @Override
                public void onComplete() {
                    completeRunnable.run();
                    subscription.cancel();
                }
            };

            HttpResponse.BodyHandler<Void> bodyHandler = HttpResponse.BodyHandlers.fromLineSubscriber(subscriber);

            // Send the response async, and let the subscriber handle the lines.
            return httpClient.sendAsync(request, bodyHandler)
                    .thenAccept(response -> {
                        if (!JdkHttpResponse.success(response)) {
                            subscriber.onError(new IOException("Request failed " + response.statusCode()));
                        }
                    });
        }
    }

    private class JdkGetBuilder extends JdkBuilder<GetBuilder> implements A2AHttpTransport.GetBuilder {

        private HttpRequest.Builder createRequestBuilder(boolean SSE) throws IOException {
            HttpRequest.Builder builder = super.createRequestBuilder().GET();
            if (SSE) {
                builder.header("Accept", "text/event-stream");
            }
            return builder;
        }

        @Override
        public A2AHttpResponse get() throws IOException, InterruptedException {
            HttpRequest request = createRequestBuilder(false)
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new JdkHttpResponse(response);
        }

        @Override
        public CompletableFuture<Void> getAsyncSSE(
                Consumer<String> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) throws IOException, InterruptedException {
            HttpRequest request = createRequestBuilder(false)
                    .build();
            return super.asyncRequest(request, messageConsumer, errorConsumer, completeRunnable);
        }
    }

    private class JdkPostBuilder extends JdkBuilder<PostBuilder> implements A2AHttpTransport.PostBuilder {
        String body = "";

        @Override
        public PostBuilder body(String body) {
            this.body = body;
            return self();
        }

        private HttpRequest.Builder createRequestBuilder(boolean SSE) throws IOException {
            HttpRequest.Builder builder = super.createRequestBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            if (SSE) {
                builder.header("Accept", "text/event-stream");
            }
            return builder;
        }

        @Override
        public A2AHttpResponse post() throws IOException, InterruptedException {
            HttpRequest request = createRequestBuilder(false)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new JdkHttpResponse(response);
        }

        @Override
        public CompletableFuture<Void> postAsyncSSE(
                Consumer<String> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) throws IOException, InterruptedException {
            HttpRequest request = createRequestBuilder(false)
                    .build();
            return super.asyncRequest(request, messageConsumer, errorConsumer, completeRunnable);
        }
    }

    private record JdkHttpResponse(HttpResponse<String> response) implements A2AHttpResponse {

        @Override
        public int status() {
            return response.statusCode();
        }

        @Override
        public boolean success() {// Send the request and get the response
            return success(response);
        }

        static boolean success(HttpResponse<?> response) {
            return response.statusCode() >= 200 && response.statusCode() < 300;
        }

        @Override
        public String body() {
            return response.body();
        }
    }
}
