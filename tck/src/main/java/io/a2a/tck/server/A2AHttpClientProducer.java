package io.a2a.tck.server;

import io.a2a.http.A2AHttpClient;
import io.a2a.http.JdkA2AHttpClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class A2AHttpClientProducer {

    @Produces
    public A2AHttpClient httpClient() {
        return new JdkA2AHttpClient();
    }

}
