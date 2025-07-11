package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Provides a declaration of the target url and the supported transport to interact with the agent.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentInterface(String transport, String url) {

    public AgentInterface {
        Assert.checkNotNullParam("transport", transport);
        Assert.checkNotNullParam("url", url);
    }
}
