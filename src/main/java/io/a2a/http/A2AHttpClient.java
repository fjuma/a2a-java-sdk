package io.a2a.http;

import java.io.IOException;

public interface A2AHttpClient {

    GetBuilder createGet();

    PostBuilder createPost();

    interface Builder<T extends Builder<T>> {
        T url(String s);
        T addHeader(String name, String value);
    }

    interface GetBuilder extends Builder<GetBuilder> {
        A2AHttpClientResponse get() throws IOException, InterruptedException;
    }

    interface PostBuilder extends Builder<PostBuilder> {
        PostBuilder body(String body);
        A2AHttpClientResponse post() throws IOException, InterruptedException;
    }
}
