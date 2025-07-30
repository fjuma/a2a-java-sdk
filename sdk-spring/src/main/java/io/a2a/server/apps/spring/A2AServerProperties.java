/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.a2a.server.apps.spring;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for A2A Server.
 * <p>
 * This class provides configuration options for setting up an A2A server, including agent
 * metadata, capabilities, and server settings.
 *
 * <p>
 * Properties can be configured in application.yml or application.properties: <pre>
 * a2a:
 *   server:
 *     enabled: true
 *     name: My A2A Agent
 *     description: A sample A2A agent
 *     version: 1.0.0
 *     url: <a href="https://my-agent.example.com">...</a>
 *     capabilities:
 *       streaming: true
 *       push-notifications: false
 *       state-transition-history: true
 * </pre>
 *
 */
@ConfigurationProperties(prefix = "a2a.server")
public class A2AServerProperties implements Serializable {

    @Serial
    private static final long serialVersionUID = -608274692651491547L;

    /**
     * Whether the A2A server is enabled.
     */
    private boolean enabled = false;

    /**
     * The unique agent id
     */
    private String id;

    /**
     * The name of the agent.
     */
    private String name;

    /**
     * A description of what the agent does.
     */
    private String description;

    /**
     * The version of the agent.
     */
    private String version;

    /**
     * The base URL where the agent can be reached.
     */
    private String url;

    /**
     * Information about the provider of the agent.
     */
    private Provider provider;

    /**
     * An optional URL pointing to the agent's documentation.
     */
    private String documentationUrl;

    /**
     * Agent capabilities configuration.
     */
    private Capabilities capabilities = new Capabilities();

    /**
     * Authentication details required to interact with the agent.
     */
    private Authentication authentication = new Authentication();

    /**
     * Security scheme details used for authenticating with this agent.
     */
    private Map<String, SecurityScheme> securitySchemes = new HashMap<>();

    /**
     * Security requirements for contacting the agent.
     */
    private List<Map<String, List<String>>> security = new ArrayList<>();

    /**
     * Default input modes supported by the agent (e.g., 'text', 'file', 'json').
     */
    private List<String> defaultInputModes = List.of("text");

    /**
     * Default output modes supported by the agent (e.g., 'text', 'file', 'json').
     */
    private List<String> defaultOutputModes = List.of("text");

    /**
     * List of specific skills offered by the agent.
     */
    private List<Skill> skills = new ArrayList<>();

    /**
     * Whether the agent supports authenticated extended card retrieval.
     */
    private boolean supportsAuthenticatedExtendedCard = false;

    /**
     * Returns whether the A2A server is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether the A2A server is enabled.
     *
     * @param enabled true to enable the server, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get agent id.
     * @return agent id
     */
    public String getId() {
        return id;
    }

    /**
     * Set agent id.
     * @param id agent id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the agent name.
     *
     * @return the agent name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the agent name.
     *
     * @param name the agent name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the agent description.
     *
     * @return the agent description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the agent description.
     *
     * @param description the agent description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the agent version.
     *
     * @return the agent version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the agent version.
     *
     * @param version the agent version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the agent URL.
     *
     * @return the agent URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the agent URL.
     *
     * @param url the agent URL to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    public void setDocumentationUrl(String documentationUrl) {
        this.documentationUrl = documentationUrl;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public Map<String, SecurityScheme> getSecuritySchemes() {
        return securitySchemes;
    }

    public void setSecuritySchemes(Map<String, SecurityScheme> securitySchemes) {
        this.securitySchemes = securitySchemes;
    }

    public List<Map<String, List<String>>> getSecurity() {
        return security;
    }

    public void setSecurity(List<Map<String, List<String>>> security) {
        this.security = security;
    }

    public List<String> getDefaultInputModes() {
        return defaultInputModes;
    }

    public void setDefaultInputModes(List<String> defaultInputModes) {
        this.defaultInputModes = defaultInputModes;
    }

    public List<String> getDefaultOutputModes() {
        return defaultOutputModes;
    }

    public void setDefaultOutputModes(List<String> defaultOutputModes) {
        this.defaultOutputModes = defaultOutputModes;
    }

    public List<Skill> getSkills() {
        return skills;
    }

    public void setSkills(List<Skill> skills) {
        this.skills = skills;
    }

    /**
     * Returns whether the agent supports authenticated extended card retrieval.
     *
     * @return true if authenticated extended card is supported, false otherwise
     */
    public boolean isSupportsAuthenticatedExtendedCard() {
        return supportsAuthenticatedExtendedCard;
    }

    /**
     * Sets whether the agent supports authenticated extended card retrieval.
     *
     * @param supportsAuthenticatedExtendedCard true to enable authenticated extended card support, false to disable
     */
    public void setSupportsAuthenticatedExtendedCard(boolean supportsAuthenticatedExtendedCard) {
        this.supportsAuthenticatedExtendedCard = supportsAuthenticatedExtendedCard;
    }

    /**
     * Returns the agent capabilities configuration.
     *
     * @return the capabilities configuration
     */
    public Capabilities getCapabilities() {
        return capabilities;
    }

    /**
     * Sets the agent capabilities configuration.
     *
     * @param capabilities the capabilities configuration to set
     */
    public void setCapabilities(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * Configuration for agent capabilities.
     * <p>
     * This class defines what features the agent supports, such as streaming responses,
     * push notifications, and state history.
     */
    public static class Capabilities implements Serializable {

        private static final long serialVersionUID = 2371695651871067858L;

        /**
         * Whether the agent supports streaming responses.
         */
        private boolean streaming = true;

        /**
         * Whether the agent supports push notifications.
         */
        private boolean pushNotifications = false;

        /**
         * Whether the agent maintains state transition history.
         */
        private boolean stateTransitionHistory = true;

        /**
         * Returns whether streaming is supported.
         *
         * @return true if streaming is supported, false otherwise
         */
        public boolean isStreaming() {
            return streaming;
        }

        /**
         * Sets whether streaming is supported.
         *
         * @param streaming true to enable streaming support, false to disable
         */
        public void setStreaming(boolean streaming) {
            this.streaming = streaming;
        }

        /**
         * Returns whether push notifications are supported.
         *
         * @return true if push notifications are supported, false otherwise
         */
        public boolean isPushNotifications() {
            return pushNotifications;
        }

        /**
         * Sets whether push notifications are supported.
         *
         * @param pushNotifications true to enable push notification support, false to
         *                          disable
         */
        public void setPushNotifications(boolean pushNotifications) {
            this.pushNotifications = pushNotifications;
        }

        /**
         * Returns whether state transition history is supported.
         *
         * @return true if state transition history is supported, false otherwise
         */
        public boolean isStateTransitionHistory() {
            return stateTransitionHistory;
        }

        /**
         * Sets whether state transition history is supported.
         *
         * @param stateTransitionHistory true to enable state transition history, false to
         *                               disable
         */
        public void setStateTransitionHistory(boolean stateTransitionHistory) {
            this.stateTransitionHistory = stateTransitionHistory;
        }

    }

    /**
     * Configuration for agent provider information.
     */
    public static class Provider implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * The name of the provider.
         */
        private String name;

        /**
         * The URL of the provider.
         */
        private String url;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    /**
     * Configuration for agent authentication.
     */
    public static class Authentication implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * The type of authentication.
         */
        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    /**
     * Configuration for security scheme.
     */
    public static class SecurityScheme implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * The type of security scheme.
         */
        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    /**
     * Configuration for agent skill.
     */
    public static class Skill implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * The name of the skill.
         */
        private String name;

        /**
         * The description of the skill.
         */
        private String description;

        /**
         * Set of tag words describing classes of capabilities for this specific skill.
         * Example: ["cooking", "customer support", "billing"]
         */
        private List<String> tags = new ArrayList<>();

        /**
         * The set of example scenarios that the skill can perform.
         * Will be used by the client as a hint to understand how the skill can be used.
         * Example: ["I need a recipe for bread"]
         */
        private List<String> examples;

        /**
         * The input modes supported by the skill.
         */
        private List<String> inputModes = List.of("text");

        /**
         * The output modes supported by the skill.
         */
        private List<String> outputModes = List.of("text");

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public List<String> getExamples() {
            return examples;
        }

        public void setExamples(List<String> examples) {
            this.examples = examples;
        }

        public List<String> getInputModes() {
            return inputModes;
        }

        public void setInputModes(List<String> inputModes) {
            this.inputModes = inputModes;
        }

        public List<String> getOutputModes() {
            return outputModes;
        }

        public void setOutputModes(List<String> outputModes) {
            this.outputModes = outputModes;
        }
    }

}
