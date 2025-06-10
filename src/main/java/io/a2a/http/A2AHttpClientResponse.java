package io.a2a.http;

public interface A2AHttpClientResponse {
    int status();

    boolean success();

    String body();
}
