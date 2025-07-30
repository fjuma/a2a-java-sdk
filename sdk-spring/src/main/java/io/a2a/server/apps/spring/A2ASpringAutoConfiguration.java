package io.a2a.server.apps.spring;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.QueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.tasks.InMemoryPushNotifier;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotifier;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentProvider;
import io.a2a.spec.AgentSkill;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-configuration for A2A (Agent2Agent) protocol.
 * Automatically configures the necessary beans for A2A server functionality.
 */
@Configuration
@EnableConfigurationProperties(A2AServerProperties.class)
public class A2ASpringAutoConfiguration {

    @Bean(name = "a2aServerSelfCard")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "a2a.server.enabled", havingValue = "true")
    public AgentCard agentCard(final A2AServerProperties a2aServerProperties) {
        AgentCard.Builder builder = new AgentCard.Builder()
                .name(a2aServerProperties.getName())
                .url(a2aServerProperties.getUrl())
                .version(a2aServerProperties.getVersion())
                .description(a2aServerProperties.getDescription());

        // Add provider information if exists
        if (a2aServerProperties.getProvider() != null) {
            builder.provider(new AgentProvider(a2aServerProperties.getProvider().getName(), a2aServerProperties.getProvider().getUrl()));
        }

        // Add documentation URL if exists
        if (a2aServerProperties.getDocumentationUrl() != null) {
            builder.documentationUrl(a2aServerProperties.getDocumentationUrl());
        }

        // Add capabilities if exists
        if (a2aServerProperties.getCapabilities() != null) {
            builder.capabilities(new AgentCapabilities.Builder()
                    .streaming(a2aServerProperties.getCapabilities().isStreaming())
                    .pushNotifications(a2aServerProperties.getCapabilities().isPushNotifications())
                    .stateTransitionHistory(a2aServerProperties.getCapabilities().isStateTransitionHistory())
                    .build());
        }

        // Add security requirements if exists
        if (a2aServerProperties.getSecurity() != null && !a2aServerProperties.getSecurity().isEmpty()) {
            builder.security(a2aServerProperties.getSecurity());
        }

        // Add default input modes if exists
        if (a2aServerProperties.getDefaultInputModes() != null && !a2aServerProperties.getDefaultInputModes().isEmpty()) {
            builder.defaultInputModes(a2aServerProperties.getDefaultInputModes());
        }

        // Add default output modes if exists
        if (a2aServerProperties.getDefaultOutputModes() != null && !a2aServerProperties.getDefaultOutputModes().isEmpty()) {
            builder.defaultOutputModes(a2aServerProperties.getDefaultOutputModes());
        }

        // Add skills list if exists
        if (a2aServerProperties.getSkills() != null && !a2aServerProperties.getSkills().isEmpty()) {
            builder.skills(a2aServerProperties.getSkills().stream()
                    .filter(skill -> skill != null && skill.getName() != null)
                    .map(skill -> new AgentSkill.Builder()
                            .id(skill.getName())
                            .name(skill.getName())
                            .description(skill.getDescription())
                            .tags(skill.getTags())
                            .examples(skill.getExamples())
                            .inputModes(skill.getInputModes())
                            .outputModes(skill.getOutputModes())
                            .build())
                    .collect(Collectors.toList()));
        }

        // Add authenticated extended card support
        builder.supportsAuthenticatedExtendedCard(a2aServerProperties.isSupportsAuthenticatedExtendedCard());

        return builder.build();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public Executor a2aExecutor() {
        return ForkJoinPool.commonPool();
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskStore taskStore() {
        return new InMemoryTaskStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public QueueManager queueManager() {
        return new InMemoryQueueManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public PushNotifier pushNotifier() {
        return new InMemoryPushNotifier();
    }

    /**
     * Configure RequestHandler Bean.
     * This Bean handles all A2A requests.
     */
    @Bean
    @ConditionalOnMissingBean
    public RequestHandler requestHandler(AgentExecutor agentExecutor, TaskStore taskStore, QueueManager queueManager, PushNotifier pushNotifier, Executor executor) {
        return new DefaultRequestHandler(agentExecutor, taskStore, queueManager, pushNotifier, executor);
    }

    @Bean
    @ConditionalOnMissingBean
    public JSONRPCHandler jsonrpcHandler(AgentCard agentCard, RequestHandler requestHandler) {
        return new JSONRPCHandler(agentCard, requestHandler);
    }
}
