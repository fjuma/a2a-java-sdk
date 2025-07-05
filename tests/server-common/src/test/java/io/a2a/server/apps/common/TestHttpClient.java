package io.a2a.server.apps.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;

import io.a2a.spec.Task;
import io.a2a.util.Utils;
import io.a2a.transport.Transport;

@Dependent
@Alternative
public class TestHttpClient implements Transport {
    final List<Task> tasks = Collections.synchronizedList(new ArrayList<>());
    volatile CountDownLatch latch;

    @Override
    public CompletableFuture<String> request(String url, String body) {
        try {
            tasks.add(Utils.OBJECT_MAPPER.readValue(body, Task.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        latch.countDown();
        return CompletableFuture.completedFuture("");
    }

    @Override
    public void stream(String url, String body, Consumer<io.a2a.spec.StreamingEventKind> onEvent, Consumer<io.a2a.spec.JSONRPCError> onError, Runnable onComplete) {
        // Not implemented for this test mock
    }
}
