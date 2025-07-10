package io.a2a.transport.http;

import io.a2a.transport.A2ATransport;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface A2AHttpTransport extends A2ATransport {

    GetBuilder createGet();

    PostBuilder createPost();

    interface Builder<T extends Builder<T>> {
        T url(String s);
        T addHeader(String name, String value);
    }

    interface GetBuilder extends Builder<GetBuilder> {
        A2AHttpResponse get() throws IOException, InterruptedException;
        CompletableFuture<Void> getAsyncSSE(
                Consumer<String> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) throws IOException, InterruptedException;
    }

    interface PostBuilder extends Builder<PostBuilder> {
        PostBuilder body(String body);
        A2AHttpResponse post() throws IOException, InterruptedException;
        CompletableFuture<Void> postAsyncSSE(
                Consumer<String> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) throws IOException, InterruptedException;
    }
}
