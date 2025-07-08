package io.a2a.transport.http;

public interface A2AHttpResponse {
    int status();

    boolean success();

    String body();
}
