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
import io.a2a.spec.AgentCard;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-configuration for A2A (Agent2Agent) protocol.
 * Automatically configures the necessary beans for A2A server functionality.
 */
@Configuration
@ComponentScan(basePackages = "io.a2a.server.apps.spring")
public class A2ASpringAutoConfiguration {

    /**
     * Provides an Executor bean for async operations if not already configured.
     * Uses ForkJoinPool.commonPool() as the default executor.
     *
     * @return the executor bean
     */
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
