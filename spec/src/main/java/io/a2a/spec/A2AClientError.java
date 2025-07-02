package io.a2a.spec;

public class A2AClientError extends Exception {
    public A2AClientError() {
    }

    public A2AClientError(String message) {
        super(message);
    }

    public A2AClientError(String message, Throwable cause) {
        super(message, cause);
    }
}
