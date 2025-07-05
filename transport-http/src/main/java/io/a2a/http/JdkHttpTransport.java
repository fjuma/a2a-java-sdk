package io.a2a.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.StreamingEventKind;
import io.a2a.transport.Transport;
import io.a2a.util.Utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class JdkHttpTransport implements Transport {

    private final HttpClient httpClient;

    public JdkHttpTransport() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public CompletableFuture<String> request(String url, String body) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return response.body();
                    } else {
                        throw new RuntimeException("Request failed with status code: " + response.statusCode() + " and body: " + response.body());
                    }
                });
    }

    @Override
    public void stream(String url, String body, Consumer<StreamingEventKind> onEvent, Consumer<JSONRPCError> onError, Runnable onComplete) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            response.body().forEach(line -> {
                                if (line.startsWith("data:")) {
                                    String data = line.substring(5).trim();
                                    if (data.isEmpty()) {
                                        return;
                                    }
                                    try {
                                        StreamingEventKind event = Utils.OBJECT_MAPPER.readValue(data, StreamingEventKind.class);
                                        onEvent.accept(event);
                                    } catch (JsonProcessingException e) {
                                        onError.accept(new JSONRPCError(-32700, "Parse error", e.getMessage()));
                                    }
                                }
                            });
                            onComplete.run();
                        } else {
                            onError.accept(new JSONRPCError(response.statusCode(), "Request failed", response.body().toString()));
                        }
                    }).exceptionally(ex -> {
                        onError.accept(new JSONRPCError(-32603, "Internal error", ex.getMessage()));
                        return null;
                    });
        } catch (Exception e) {
            onError.accept(new JSONRPCError(-32603, "Internal error", e.getMessage()));
        }
    }
}