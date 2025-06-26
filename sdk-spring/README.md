# A2A Java SDK - Spring Adapter

This module provides a Spring Boot adapter for the A2A (Agent2Agent) protocol, allowing easy integration of A2A server functionality in Spring Boot applications.

## Features

- Complete JSON-RPC support
- Server-Sent Events (SSE) streaming response support
- Auto-configuration and Bean registration
- Global exception handling
- Seamless integration with Spring Boot

## Dependencies

```xml
<dependency>
    <groupId>io.a2a.sdk</groupId>
    <artifactId>a2a-java-sdk-server-spring</artifactId>
    <version>0.2.4-SNAPSHOT</version>
</dependency>
```

## Quick Start

### 1. Add Dependencies

Add the dependency to your Spring Boot project's `pom.xml`:

```xml
<dependency>
    <groupId>io.a2a.sdk</groupId>
    <artifactId>a2a-java-sdk-server-spring</artifactId>
    <version>0.2.4-SNAPSHOT</version>
</dependency>
```

### 2. Configure Agent Card

Create a configuration class to provide the Agent Card:

```java
@Configuration
public class A2AConfiguration {
    
    @Bean
    @PublicAgentCard
    public AgentCard agentCard() {
        return new AgentCard.Builder()
            .name("My Agent")
            .description("My A2A Agent")
            .url("https://my-provider.com")
            .version("1.0.0")
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
}
```

### 3. Implement RequestHandler

Implement the `RequestHandler` interface to handle A2A requests:

```java
@Component
public class MyRequestHandler implements RequestHandler {
    
    @Override
    public EventKind onMessageSend(MessageSendParams params) {
        // Handle message send request
        return new Task.Builder()
            .id("task-id")
            .contextId("context-id")
            .status(new TaskStatus(TaskState.WORKING))
            .build();
    }
    
    @Override
    public Task onGetTask(TaskQueryParams params) {
        // Handle get task request
        return new Task.Builder()
            .id(params.id())
            .contextId("context-id")
            .status(new TaskStatus(TaskState.WORKING))
            .build();
    }
    
    // Implement other methods...
}
```

### 4. Start the Application

Start your Spring Boot application, and the A2A endpoints will be automatically available:

- `POST /` - JSON-RPC endpoint
- `GET /.well-known/agent.json` - Agent Card endpoint
- `GET /agent/authenticatedExtendedCard` - Extended Agent Card endpoint

## Endpoint Description

### JSON-RPC Endpoint

`POST /` - Handles all JSON-RPC requests

Supported methods:
- `a2a.getTask` - Get task
- `a2a.cancelTask` - Cancel task
- `a2a.sendMessage` - Send message
- `a2a.sendStreamingMessage` - Send streaming message
- `a2a.resubscribeToTask` - Resubscribe to task
- `a2a.setTaskPushNotificationConfig` - Set push notification configuration
- `a2a.getTaskPushNotificationConfig` - Get push notification configuration

### Agent Card Endpoint

`GET /.well-known/agent.json` - Returns Agent Card information

### Extended Agent Card Endpoint

`GET /agent/authenticatedExtendedCard` - Returns authenticated extended Agent Card information

## Streaming Responses

For streaming requests (such as `sendStreamingMessage`), the endpoint will return responses in Server-Sent Events (SSE) format.

## Exception Handling

The module provides a global exception handler that automatically converts JSON parsing errors to appropriate JSON-RPC error responses.

## Testing

Run tests:

```bash
mvn test
```

Tests include:
- Agent Card endpoint tests
- JSON-RPC request tests
- Error handling tests
- Streaming response tests

## Configuration Options

You can configure through Spring Boot's `application.properties` or `application.yml`:

```properties
# Custom port (optional)
server.port=8080

# Enable WebFlux (for SSE support)
spring.main.web-application-type=servlet
```

## Examples

See the `examples` directory for complete example applications.

## License

This project is licensed under the Apache License 2.0. 