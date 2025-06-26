package io.a2a.server.apps.spring;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.apps.common.AgentExecutorProducer;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.QueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.server.tasks.InMemoryPushNotifier;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotifier;
import io.a2a.server.tasks.TaskStore;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.server.util.async.Internal;
import io.a2a.spec.A2A;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.UnsupportedOperationError;
import jakarta.enterprise.inject.Produces;
import java.util.concurrent.Executor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.util.List;
import java.util.Collections;

import io.a2a.server.PublicAgentCard;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentProvider;
import io.a2a.spec.EventKind;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.PushNotificationConfig;

/**
 * Example Spring Boot application demonstrating how to use the A2A Spring adapter.
 * This application provides a simple A2A server implementation.
 */
@SpringBootApplication
public class SpringA2AExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringA2AExampleApplication.class, args);
    }

    /**
     * Configure Agent Card Bean.
     * This Bean defines the basic information and capabilities of the A2A server.
     */
    @Bean
    @PublicAgentCard
    public AgentCard agentCard() {
        return new AgentCard.Builder()
            .name("Spring A2A Example Agent")
            .description("A simple A2A agent for Spring")
            .url("http://localhost:8080")
            .version("1.0.0")
            .documentationUrl("https://example.com/docs")
            .capabilities(new AgentCapabilities.Builder()
                .streaming(true)
                .pushNotifications(true)
                .stateTransitionHistory(true)
                .build())
            .defaultInputModes(Collections.singletonList("text"))
            .defaultOutputModes(Collections.singletonList("text"))
            .skills(Collections.emptyList())
            .build();
    }

    @Bean
    @Produces
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                eventQueue.enqueueEvent(context.getMessage() != null ? context.getMessage() : context.getTask());
                eventQueue.enqueueEvent(A2A.toAgentMessage("Hello World"));
            }

            @Override
            public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
                taskUpdater.cancel();
            }
        };
    }
} 
