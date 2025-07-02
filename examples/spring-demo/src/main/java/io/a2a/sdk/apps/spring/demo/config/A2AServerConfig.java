package io.a2a.sdk.apps.spring.demo.config;

import io.a2a.server.PublicAgentCard;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.A2A;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.JSONRPCError;

import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A2A server config
 */
@Configuration
public class A2AServerConfig {
    
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
            };
        };
    }
}
