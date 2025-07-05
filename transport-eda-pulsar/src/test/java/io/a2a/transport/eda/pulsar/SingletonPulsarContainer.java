package io.a2a.transport.eda.pulsar;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

public class SingletonPulsarContainer {

    private static final PulsarContainer pulsarContainer;

    static {
        pulsarContainer = new PulsarContainer(DockerImageName.parse("apachepulsar/pulsar:4.0.5"));
        pulsarContainer.start();
    }

    public static String getPulsarBrokerUrl() {
        return pulsarContainer.getPulsarBrokerUrl();
    }

    public static PulsarClient createPulsarClient() throws PulsarClientException {
        return PulsarClient.builder()
                .serviceUrl(pulsarContainer.getPulsarBrokerUrl())
                .build();
    }
}
