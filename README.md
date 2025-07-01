# A2A Java SDK

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

<!-- markdownlint-disable no-inline-html -->

<html>
   <h2 align="center">
   <img src="https://raw.githubusercontent.com/google-a2a/A2A/refs/heads/main/docs/assets/a2a-logo-black.svg" width="256" alt="A2A Logo"/>
   </h2>
   <h3 align="center">A Java library that helps run agentic applications as A2AServers following the <a href="https://google-a2a.github.io/A2A">Agent2Agent (A2A) Protocol</a>.</h3>
</html>

## Installation

You can build the A2A Java SDK using `mvn`:

```bash
mvn clean install
```

## Examples

You can find an example of how to use the A2A Java SDK [here](https://github.com/fjuma/a2a-samples/tree/java-sdk-example/samples/multi_language/python_and_java_multiagent/weather_agent).

More examples will be added soon.

## A2A Server

The A2A Java SDK provides a Java server implementation of the [Agent2Agent (A2A) Protocol](https://google-a2a.github.io/A2A). To run your agentic Java application as an A2A server, simply follow the steps below.

- [Add the A2A Java SDK Core Maven dependency to your project](#1-add-the-a2a-java-sdk-core-maven-dependency-to-your-project)
- [Add a class that creates an A2A Agent Card](#2-add-a-class-that-creates-an-a2a-agent-card)
- [Add a class that creates an A2A Agent Executor](#3-add-a-class-that-creates-an-a2a-agent-executor)
- [Add an A2A Java SDK Server Maven dependency to your project](#4-add-an-a2a-java-sdk-server-maven-dependency-to-your-project)

### 1. Add the A2A Java SDK Core Maven dependency to your project

> **Note**: The A2A Java SDK isn't available yet in Maven Central but will be soon. For now, be
> sure to check out the latest tag (you can see the tags [here](https://github.com/a2aproject/a2a-java/tags)), build from the tag, and reference that version below. For example, if the latest tag is `0.2.3`, you can use the following dependency.

```xml
<dependency>
    <groupId>io.a2a.sdk</groupId>
    <artifactId>a2a-java-sdk-core</artifactId>
    <version>0.2.3</version>
</dependency>
```

### 2. Add a class that creates an A2A Agent Card

```java
import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
...

@ApplicationScoped
public class WeatherAgentCardProducer {
    
    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return new AgentCard.Builder()
                .name("Weather Agent")
                .description("Helps with weather")
                .url("http://localhost:10001")
                .version("1.0.0")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .stateTransitionHistory(false)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill.Builder()
                        .id("weather_search")
                        .name("Search weather")
                        .description("Helps with weather in city, or states")
                        .tags(Collections.singletonList("weather"))
                        .examples(List.of("weather in LA, CA"))
                        .build()))
                .build();
    }
}
```

### 3. Add a class that creates an A2A Agent Executor

```java
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
...

@ApplicationScoped
public class WeatherAgentExecutorProducer {

    @Inject
    WeatherAgent weatherAgent;

    @Produces
    public AgentExecutor agentExecutor() {
        return new WeatherAgentExecutor(weatherAgent);
    }

    private static class WeatherAgentExecutor implements AgentExecutor {

        private final WeatherAgent weatherAgent;

        public WeatherAgentExecutor(WeatherAgent weatherAgent) {
            this.weatherAgent = weatherAgent;
        }

        @Override
        public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
            TaskUpdater updater = new TaskUpdater(context, eventQueue);

            // mark the task as submitted and start working on it
            if (context.getTask() == null) {
                updater.submit();
            }
            updater.startWork();

            // extract the text from the message
            String userMessage = extractTextFromMessage(context.getMessage());

            // call the weather agent with the user's message
            String response = weatherAgent.chat(userMessage);

            // create the response part
            TextPart responsePart = new TextPart(response, null);
            List<Part<?>> parts = List.of(responsePart);

            // add the response as an artifact and complete the task
            updater.addArtifact(parts, null, null, null);
            updater.complete();
        }

        @Override
        public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
            Task task = context.getTask();

            if (task.getStatus().state() == TaskState.CANCELED) {
                // task already cancelled
                throw new TaskNotCancelableError();
            }

            if (task.getStatus().state() == TaskState.COMPLETED) {
                // task already completed
                throw new TaskNotCancelableError();
            }

            // cancel the task
            TaskUpdater updater = new TaskUpdater(context, eventQueue);
            updater.cancel();
        }

        private String extractTextFromMessage(Message message) {
            StringBuilder textBuilder = new StringBuilder();
            if (message.getParts() != null) {
                for (Part part : message.getParts()) {
                    if (part instanceof TextPart textPart) {
                        textBuilder.append(textPart.getText());
                    }
                }
            }
            return textBuilder.toString();
        }
    }
}
```

### 4. Add an A2A Java SDK Server Maven dependency to your project

> **Note**: The A2A Java SDK isn't available yet in Maven Central but will be soon. For now, be
> sure to check out the latest tag (you can see the tags [here](https://github.com/a2aproject/a2a-java/tags)), build from the tag, and reference that version below. For example, if the latest tag is `0.2.3`, you can use the following dependency.

Adding a dependency on an A2A Java SDK Server will allow you to run your agentic Java application as an A2A server.

The A2A Java SDK provides two A2A server endpoint implementations, one based on Jakarta REST (`a2a-java-sdk-server-jakarta`) and one based on Quarkus Reactive Routes (`a2a-java-sdk-server-quarkus`). You can choose the one that best fits your application.

Add **one** of the following dependencies to your project:

```xml
<dependency>
    <groupId>io.a2a.sdk</groupId>
    <artifactId>a2a-java-sdk-server-jakarta</artifactId>
    <version>${io.a2a.sdk.version}</version>
</dependency>
```

OR

```xml
<dependency>
    <groupId>io.a2a.sdk</groupId>
    <artifactId>a2a-java-sdk-server-quarkus</artifactId>
    <version>${io.a2a.sdk.version}</version>
</dependency>
```

## A2A Client

The A2A Java SDK provides a Java client implementation of the [Agent2Agent (A2A) Protocol](https://google-a2a.github.io/A2A), allowing communication with A2A servers.

### Sample Usage

#### Create an A2A client

```java
// Create an A2AClient (the URL specified is the server agent's URL, be sure to replace it with the actual URL of the A2A server you want to connect to)
A2AClient client = new A2AClient("http://localhost:1234");
```

#### Send a message to the A2A server agent

```java
// Send a text message to the A2A server agent
Message message = A2A.toUserMessage("tell me a joke"); // the message ID will be automatically generated for you
MessageSendParams params = new MessageSendParams.Builder()
        .message(message)
        .build();
SendMessageResponse response = client.sendMessage(params);        
```

Note that `A2A#toUserMessage` will automatically generate a message ID for you when creating the `Message` 
if you don't specify it. You can also explicitly specify a message ID like this:

```java
Message message = A2A.toUserMessage("tell me a joke", "message-1234"); // messageId is message-1234
```

#### Get the current state of a task

```java
// Retrieve the task with id "task-1234"
GetTaskResponse response = client.getTask("task-1234");

// You can also specify the maximum number of items of history for the task
// to include in the response
GetTaskResponse response = client.getTask(new TaskQueryParams("task-1234", 10));
```

#### Cancel an ongoing task

```java
// Cancel the task we previously submitted with id "task-1234"
CancelTaskResponse response = client.cancelTask("task-1234");

// You can also specify additional properties using a map
Map<String, Object> metadata = ...        
CancelTaskResponse response = client.cancelTask(new TaskIdParams("task-1234", metadata));
```

#### Get the push notification configuration for a task

```java
// Get task push notification configuration
GetTaskPushNotificationConfigResponse response = client.getTaskPushNotificationConfig("task-1234");

// You can also specify additional properties using a map
Map<String, Object> metadata = ...
GetTaskPushNotificationConfigResponse response = client.getTaskPushNotificationConfig(new TaskIdParams("task-1234", metadata));
```

#### Set the push notification configuration for a task

```java
// Set task push notification configuration
PushNotificationConfig pushNotificationConfig = new PushNotificationConfig.Builder()
        .url("https://example.com/callback")
        .authenticationInfo(new AuthenticationInfo(Collections.singletonList("jwt"), null))
        .build();
SetTaskPushNotificationResponse response = client.setTaskPushNotificationConfig("task-1234", pushNotificationConfig);
```

#### Send a streaming message

```java
// Send a text message to the remote agent
Message message = A2A.toUserMessage("tell me some jokes"); // the message ID will be automatically generated for you
MessageSendParams params = new MessageSendParams.Builder()
        .message(message)
        .build();

// Create a handler that will be invoked for Task, Message, TaskStatusUpdateEvent, and TaskArtifactUpdateEvent
Consumer<StreamingEventKind> eventHandler = event -> {...};

// Create a handler that will be invoked if an error is received
Consumer<JSONRPCError> errorHandler = error -> {...};

// Create a handler that will be invoked in the event of a failure
Runnable failureHandler = () -> {...};

// Send the streaming message to the remote agent
client.sendStreamingMessage(params, eventHandler, errorHandler, failureHandler);
```

#### Resubscribe to a task

```java
// Create a handler that will be invoked for Task, Message, TaskStatusUpdateEvent, and TaskArtifactUpdateEvent
Consumer<StreamingEventKind> eventHandler = event -> {...};

// Create a handler that will be invoked if an error is received
Consumer<JSONRPCError> errorHandler = error -> {...};

// Create a handler that will be invoked in the event of a failure
Runnable failureHandler = () -> {...};

// Resubscribe to an ongoing task with id "task-1234"
TaskIdParams taskIdParams = new TaskIdParams("task-1234");
client.resubscribeToTask("request-1234", taskIdParams, eventHandler, errorHandler, failureHandler);
```

#### Retrieve details about the server agent that this client agent is communicating with
```java
AgentCard serverAgentCard = client.getAgentCard();
```

An agent card can also be retrieved using the `A2A#getAgentCard` method:
```java
// http://localhost:1234 is the base URL for the agent whose card we want to retrieve
AgentCard agentCard = A2A.getAgentCard("http://localhost:1234");
```

## Additional Examples

### Hello World Example

A complete example of an A2A client communicating with a Python A2A server is available in the [examples/helloworld](examples/helloworld) directory. This example demonstrates:

- Setting up and using the A2A Java client
- Sending regular and streaming messages
- Receiving and processing responses

The example includes detailed instructions on how to run both the Python server and the Java client using JBang. Check out the [example's README](examples/helloworld/client/src/main/java/io/a2a/examples/helloworld/README.md) for more information.

## License

This project is licensed under the terms of the [Apache 2.0 License](LICENSE).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.



