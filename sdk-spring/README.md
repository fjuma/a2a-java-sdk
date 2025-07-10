# A2A Java SDK - Spring Adapter

This module provides a Spring Boot adapter for the A2A (Agent2Agent) protocol, allowing easy integration of A2A server functionality in Spring Boot applications.

## Features

- Complete JSON-RPC support
- Server-Sent Events (SSE) streaming response support
- Auto-configuration and Bean registration
- Configuration-based Agent Card setup
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

Add the following dependencies to your Spring Boot project's `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>io.a2a.sdk</groupId>
        <artifactId>a2a-java-sdk-server-spring</artifactId>
        <version>0.2.4-SNAPSHOT</version>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
</dependencies>
```

### 2. Configure Application Properties

Configure your A2A server in `application.yml`:

```yaml
server:
  port: 8089

spring:
  application:
    name: a2a-server

# A2A specific configuration
a2a:
  server:
    enabled: true
    id: "my-agent-id"
    name: "My A2A Agent"
    description: "A sample A2A agent implemented in Java"
    version: "1.0.0"
    url: "http://localhost:${server.port}/"
    provider:
      name: "My Company"
      url: "https://my-company.com"
    documentationUrl: "https://my-company.com/docs"
    capabilities:
      streaming: true
      pushNotifications: false
      stateTransitionHistory: true
    supportsAuthenticatedExtendedCard: true
    defaultInputModes:
      - "text"
    defaultOutputModes:
      - "text"
    skills:
      - name: "hello-world"
        description: "A simple hello world skill"
        tags:
          - "greeting"
          - "basic"
        examples:
          - "Say hello to me"
          - "Greet me"
        inputModes:
          - "text"
        outputModes:
          - "text"
```

### 3. Implement Agent Logic

Create a configuration class to provide the `AgentExecutor`:

```java
@Configuration
public class A2AServerConfig {

    @Bean
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                // Echo the incoming message/task
                eventQueue.enqueueEvent(context.getMessage() != null ? context.getMessage() : context.getTask());
                // Send a response
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
```

### 4. Create Spring Boot Application

Create a main application class:

```java
@SpringBootApplication
public class A2AServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(A2AServerApplication.class, args);
    }
}
```

### 5. Start the Application

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

The A2A Spring adapter supports comprehensive configuration through Spring Boot's `application.yml` or `application.properties`. Here are the available configuration options:

### Basic Configuration

```yaml
a2a:
  server:
    enabled: true                    # Enable/disable A2A server
    id: "my-agent-id"               # Unique agent identifier
    name: "My Agent"                # Agent display name
    description: "Agent description" # Agent description
    version: "1.0.0"                # Agent version
    url: "http://localhost:8080/"   # Agent base URL
```

### Provider Information

```yaml
a2a:
  server:
    provider:
      name: "My Company"
      url: "https://my-company.com"
    documentationUrl: "https://my-company.com/docs"
```

### Capabilities

```yaml
a2a:
  server:
    capabilities:
      streaming: true                 # Support streaming responses
      pushNotifications: false        # Support push notifications
      stateTransitionHistory: true    # Support state transition history
    supportsAuthenticatedExtendedCard: true  # Support authenticated extended card
```

### Input/Output Modes

```yaml
a2a:
  server:
    defaultInputModes:
      - "text"
      - "json"
    defaultOutputModes:
      - "text"
      - "json"
```

### Skills Configuration

```yaml
a2a:
  server:
    skills:
      - name: "skill-name"
        description: "Skill description"
        tags:
          - "tag1"
          - "tag2"
        examples:
          - "Example usage 1"
          - "Example usage 2"
        inputModes:
          - "text"
        outputModes:
          - "text"
```

### Server Configuration

```yaml
server:
  port: 8080                        # Custom port (optional)

spring:
  main:
    web-application-type: servlet     # Use servlet stack (required for SSE)
```

## Complete Example

Here's a complete example of a simple A2A agent:

### Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/example/
│   │       ├── A2AServerApplication.java
│   │       └── config/
│   │           └── A2AServerConfig.java
│   └── resources/
│       └── application.yml
└── pom.xml
```

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>my-a2a-agent</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.5</version>
        <relativePath/>
    </parent>
    
    <dependencies>
        <dependency>
            <groupId>io.a2a.sdk</groupId>
            <artifactId>a2a-java-sdk-server-spring</artifactId>
            <version>0.2.4-SNAPSHOT</version>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### A2AServerApplication.java

```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class A2AServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(A2AServerApplication.class, args);
    }
}
```

### A2AServerConfig.java

```java
package com.example.config;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.A2A;
import io.a2a.spec.JSONRPCError;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class A2AServerConfig {

    @Bean
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                // Echo the incoming message/task
                eventQueue.enqueueEvent(context.getMessage() != null ? context.getMessage() : context.getTask());
                // Send a response
                eventQueue.enqueueEvent(A2A.toAgentMessage("Hello from A2A Agent!"));
            }

            @Override
            public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
                taskUpdater.cancel();
            }
        };
    }
}
```

### application.yml

```yaml
server:
  port: 8089

spring:
  application:
    name: my-a2a-agent

a2a:
  server:
    enabled: true
    id: "my-agent-id"
    name: "My A2A Agent"
    description: "A sample A2A agent implemented in Java"
    version: "1.0.0"
    url: "http://localhost:${server.port}/"
    provider:
      name: "My Company"
      url: "https://my-company.com"
    documentationUrl: "https://my-company.com/docs"
    capabilities:
      streaming: true
      pushNotifications: false
      stateTransitionHistory: true
    supportsAuthenticatedExtendedCard: true
    defaultInputModes:
      - "text"
    defaultOutputModes:
      - "text"
    skills:
      - name: "hello-world"
        description: "A simple hello world skill"
        tags:
          - "greeting"
          - "basic"
        examples:
          - "Say hello to me"
          - "Greet me"
        inputModes:
          - "text"
        outputModes:
          - "text"
```

### Running the Application

1. Build the project: `mvn clean package`
2. Run the application: `java -jar target/my-a2a-agent-1.0.0.jar`
3. Test the endpoints:
   - Agent Card: `curl http://localhost:8089/.well-known/agent.json`
   - Send Message: `curl -X POST http://localhost:8089/ -H "Content-Type: application/json" -d '{"jsonrpc":"2.0","method":"a2a.sendMessage","params":{"message":{"content":"Hello"}},"id":1}'`

## Additional Examples

See the `examples/spring-helloworld` directory for a complete working example application.

## License

This project is licensed under the Apache License 2.0.
