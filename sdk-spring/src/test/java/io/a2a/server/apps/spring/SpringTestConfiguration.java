package io.a2a.server.apps.spring;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.Collections;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import io.a2a.server.PublicAgentCard;
import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.EventKind;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.MessageSendParams;

/**
 * Test configuration for Spring A2A server tests.
 * Provides mock implementations of required beans for testing.
 */
@TestConfiguration
public class SpringTestConfiguration {

    @Bean
    @Primary
    public Executor testExecutor() {
        return ForkJoinPool.commonPool();
    }

    @Bean
    @PublicAgentCard
    public AgentCard testAgentCard() {
        return new AgentCard.Builder()
            .name("Test Agent")
            .description("A test agent")
            .url("https://example.com")
            .version("1.0.0")
            .documentationUrl("https://example.com/docs")
            .capabilities(new AgentCapabilities.Builder()
                .streaming(false)
                .pushNotifications(false)
                .stateTransitionHistory(false)
                .build())
            .defaultInputModes(Collections.singletonList("text"))
            .defaultOutputModes(Collections.singletonList("text"))
            .skills(Collections.emptyList())
            .build();
    }

    @Bean
    public RequestHandler testRequestHandler() {
        return new RequestHandler() {
            @Override
            public EventKind onMessageSend(MessageSendParams params) {
                throw new TaskNotFoundError();
            }

            @Override
            public java.util.concurrent.Flow.Publisher<StreamingEventKind> onMessageSendStream(MessageSendParams params) {
                throw new TaskNotFoundError();
            }

            @Override
            public Task onCancelTask(TaskIdParams params) {
                throw new TaskNotFoundError();
            }

            @Override
            public java.util.concurrent.Flow.Publisher<StreamingEventKind> onResubscribeToTask(TaskIdParams params) {
                throw new TaskNotFoundError();
            }

            @Override
            public TaskPushNotificationConfig onGetTaskPushNotificationConfig(TaskIdParams params) {
                throw new TaskNotFoundError();
            }

            @Override
            public TaskPushNotificationConfig onSetTaskPushNotificationConfig(TaskPushNotificationConfig params) {
                throw new TaskNotFoundError();
            }

            @Override
            public Task onGetTask(TaskQueryParams params) {
                throw new TaskNotFoundError();
            }
        };
    }

    @Bean
    public JSONRPCHandler jsonRpcHandler(@PublicAgentCard AgentCard agentCard, RequestHandler requestHandler) {
        return new JSONRPCHandler(agentCard, requestHandler);
    }
} 
