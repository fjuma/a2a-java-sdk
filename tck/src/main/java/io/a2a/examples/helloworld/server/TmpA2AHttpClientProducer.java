package io.a2a.examples.helloworld.server;

import io.a2a.http.TempA2AHttpClient;
import io.a2a.spec.Task;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class TmpA2AHttpClientProducer {

    @Produces
    public TempA2AHttpClient httpClient() {
        // TODO: This is just a temporary workaround while the InMemoryPushNotifier is still being worked on
        // https://github.com/fjuma/a2a-java-sdk/issues/80
        return new TempA2AHttpClient() {
            @Override
            public int post(String url, Task task) {
                return 200;
            }
        };
    }

}
