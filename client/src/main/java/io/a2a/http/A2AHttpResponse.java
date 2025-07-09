package io.a2a.http;

public interface A2AHttpResponse {
    int status();

    boolean success();

    String body();
}
