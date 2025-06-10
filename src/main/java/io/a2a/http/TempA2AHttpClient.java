package io.a2a.http;

import io.a2a.spec.Task;

public interface TempA2AHttpClient {
    int post(String url, Task task);
}
