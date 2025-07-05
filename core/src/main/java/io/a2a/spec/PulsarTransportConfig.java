package io.a2a.spec;

public record PulsarTransportConfig(
    String serviceUrl,
    String requestTopic,
    String streamingTopic
) implements TransportConfig {
}
