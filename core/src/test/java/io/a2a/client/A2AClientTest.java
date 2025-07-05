package io.a2a.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.a2a.spec.*;
import io.a2a.transport.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockserver.integration.ClientAndServer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.a2a.client.JsonMessages.*;
import static io.a2a.util.Utils.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class A2AClientTest {

    @Mock
    private Transport transport;

    private A2AClient client;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        client = new A2AClient("http://localhost:4001", transport);
    }

    private <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, clazz);
    }

    @Test
    public void testA2AClientSendMessage() throws Exception {
        SendMessageRequest request = fromJson(SEND_MESSAGE_TEST_REQUEST, SendMessageRequest.class);
        when(transport.request(anyString(), anyString())).thenAnswer(invocation -> {
            String requestBody = invocation.getArgument(1);
            if (requestBody.equals(OBJECT_MAPPER.writeValueAsString(fromJson(SEND_MESSAGE_TEST_REQUEST, SendMessageRequest.class)))) {
                return CompletableFuture.completedFuture(SEND_MESSAGE_TEST_RESPONSE);
            } else if (requestBody.equals(OBJECT_MAPPER.writeValueAsString(fromJson(SEND_MESSAGE_TEST_REQUEST_WITH_MESSAGE_RESPONSE, SendMessageRequest.class)))) {
                return CompletableFuture.completedFuture(SEND_MESSAGE_TEST_RESPONSE_WITH_MESSAGE_RESPONSE);
            } else if (requestBody.equals(OBJECT_MAPPER.writeValueAsString(fromJson(SEND_MESSAGE_WITH_ERROR_TEST_REQUEST, SendMessageRequest.class)))) {
                return CompletableFuture.completedFuture(SEND_MESSAGE_ERROR_TEST_RESPONSE);
            } else if (requestBody.equals(OBJECT_MAPPER.writeValueAsString(fromJson(GET_TASK_TEST_REQUEST, GetTaskRequest.class)))) {
                return CompletableFuture.completedFuture(GET_TASK_TEST_RESPONSE);
            } else if (requestBody.equals(OBJECT_MAPPER.writeValueAsString(fromJson(CANCEL_TASK_TEST_REQUEST, CancelTaskRequest.class)))) {
                return CompletableFuture.completedFuture(CANCEL_TASK_TEST_RESPONSE);
            } else if (requestBody.equals(OBJECT_MAPPER.writeValueAsString(fromJson(GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST, GetTaskPushNotificationConfigRequest.class)))) {
                return CompletableFuture.completedFuture(GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE);
            } else if (requestBody.equals(OBJECT_MAPPER.writeValueAsString(fromJson(SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST, SetTaskPushNotificationConfigRequest.class)))) {
                return CompletableFuture.completedFuture(SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE);
            } else if (requestBody.equals(OBJECT_MAPPER.writeValueAsString(fromJson(SEND_MESSAGE_WITH_FILE_PART_TEST_REQUEST, SendMessageRequest.class)))) {
                return CompletableFuture.completedFuture(SEND_MESSAGE_WITH_FILE_PART_TEST_RESPONSE);
            } else if (requestBody.equals(OBJECT_MAPPER.writeValueAsString(fromJson(SEND_MESSAGE_WITH_DATA_PART_TEST_REQUEST, SendMessageRequest.class)))) {
                return CompletableFuture.completedFuture(SEND_MESSAGE_WITH_DATA_PART_TEST_RESPONSE);
            } else if (requestBody.equals(OBJECT_MAPPER.writeValueAsString(fromJson(SEND_MESSAGE_WITH_MIXED_PARTS_TEST_REQUEST, SendMessageRequest.class)))) {
                return CompletableFuture.completedFuture(SEND_MESSAGE_WITH_MIXED_PARTS_TEST_RESPONSE);
            }
            return CompletableFuture.completedFuture("");
        });

        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("tell me a joke")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = new MessageSendParams.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        SendMessageResponse response = client.sendMessage("request-1234", params);

        assertEquals("2.0", response.getJsonrpc());
        assertNotNull(response.getId());
        Object result = response.getResult();
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertNotNull(task.getContextId());
        assertEquals(TaskState.COMPLETED,task.getStatus().state());
        assertEquals(1, task.getArtifacts().size());
        Artifact artifact = task.getArtifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("joke", artifact.name());
        assertEquals(1, artifact.parts().size());
        Part<?> part = artifact.parts().get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart) part).getText());
        assertTrue(task.getMetadata().isEmpty());
    }

    @Test
    public void testA2AClientSendMessageWithMessageResponse() throws Exception {
        SendMessageRequest request = fromJson(SEND_MESSAGE_TEST_REQUEST_WITH_MESSAGE_RESPONSE, SendMessageRequest.class);
        when(transport.request(anyString(), eq(OBJECT_MAPPER.writeValueAsString(request))))
                .thenReturn(CompletableFuture.completedFuture(SEND_MESSAGE_TEST_RESPONSE_WITH_MESSAGE_RESPONSE));

        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("tell me a joke")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = new MessageSendParams.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        SendMessageResponse response = client.sendMessage("request-1234-with-message-response", params);

        assertEquals("2.0", response.getJsonrpc());
        assertNotNull(response.getId());
        Object result = response.getResult();
        assertInstanceOf(Message.class, result);
        Message agentMessage = (Message) result;
        assertEquals(Message.Role.AGENT, agentMessage.getRole());
        Part<?> part = agentMessage.getParts().get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart) part).getText());
        assertEquals("msg-456", agentMessage.getMessageId());
    }


    @Test
    public void testA2AClientSendMessageWithError() throws Exception {
        SendMessageRequest request = fromJson(SEND_MESSAGE_WITH_ERROR_TEST_REQUEST, SendMessageRequest.class);
        when(transport.request(anyString(), eq(OBJECT_MAPPER.writeValueAsString(request))))
                .thenReturn(CompletableFuture.completedFuture(SEND_MESSAGE_ERROR_TEST_RESPONSE));

        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("tell me a joke")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = new MessageSendParams.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        try {
            client.sendMessage("request-1234-with-error", params);
            fail(); // should not reach here
        } catch (A2AServerException e) {
            assertTrue(e.getMessage().contains("Invalid parameters: Hello world"));
        }
    }

    @Test
    public void testA2AClientGetTask() throws Exception {
        GetTaskRequest request = fromJson(GET_TASK_TEST_REQUEST, GetTaskRequest.class);
        when(transport.request(anyString(), eq(OBJECT_MAPPER.writeValueAsString(request))))
                .thenReturn(CompletableFuture.completedFuture(GET_TASK_TEST_RESPONSE));

        GetTaskResponse response = client.getTask("request-1234",
                new TaskQueryParams("de38c76d-d54c-436c-8b9f-4c2703648d64", 10));

        assertEquals("2.0", response.getJsonrpc());
        assertEquals(1, response.getId());
        Object result = response.getResult();
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertEquals("c295ea44-7543-4f78-b524-7a38915ad6e4", task.getContextId());
        assertEquals(TaskState.COMPLETED, task.getStatus().state());
        assertEquals(1, task.getArtifacts().size());
        Artifact artifact = task.getArtifacts().get(0);
        assertEquals(1, artifact.parts().size());
        assertEquals("artifact-1", artifact.artifactId());
        Part<?> part = artifact.parts().get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart) part).getText());
        assertTrue(task.getMetadata().isEmpty());
        List<Message> history = task.getHistory();
        assertNotNull(history);
        assertEquals(1, history.size());
        Message message = history.get(0);
        assertEquals(Message.Role.USER, message.getRole());
        List<Part<?>> parts = message.getParts();
        assertNotNull(parts);
        assertEquals(3, parts.size());
        part = parts.get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("tell me a joke", ((TextPart)part).getText());
        part = parts.get(1);
        assertEquals(Part.Kind.FILE, part.getKind());
        FileContent filePart = ((FilePart) part).getFile();
        assertEquals("file:///path/to/file.txt", ((FileWithUri) filePart).uri());
        assertEquals("text/plain", filePart.mimeType());
        part = parts.get(2);
        assertEquals(Part.Kind.FILE, part.getKind());
        filePart = ((FilePart) part).getFile();
        assertEquals("aGVsbG8=", ((FileWithBytes) filePart).bytes());
        assertEquals("hello.txt", filePart.name());
        assertTrue(task.getMetadata().isEmpty());
    }

    @Test
    public void testA2AClientCancelTask() throws Exception {
        CancelTaskRequest request = fromJson(CANCEL_TASK_TEST_REQUEST, CancelTaskRequest.class);
        when(transport.request(anyString(), eq(OBJECT_MAPPER.writeValueAsString(request))))
                .thenReturn(CompletableFuture.completedFuture(CANCEL_TASK_TEST_RESPONSE));

        CancelTaskResponse response = client.cancelTask("request-1234",
                new TaskIdParams("de38c76d-d54c-436c-8b9f-4c2703648d64", new HashMap<>()));

        assertEquals("2.0", response.getJsonrpc());
        assertEquals(1, response.getId());
        Object result = response.getResult();
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertEquals("c295ea44-7543-4f78-b524-7a38915ad6e4", task.getContextId());
        assertEquals(TaskState.CANCELED, task.getStatus().state());
        assertTrue(task.getMetadata().isEmpty());
    }

    @Test
    public void testA2AClientGetTaskPushNotificationConfig() throws Exception {
        GetTaskPushNotificationConfigRequest request = fromJson(GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST, GetTaskPushNotificationConfigRequest.class);
        when(transport.request(anyString(), eq(OBJECT_MAPPER.writeValueAsString(request))))
                .thenReturn(CompletableFuture.completedFuture(GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE));

        GetTaskPushNotificationConfigResponse response = client.getTaskPushNotificationConfig("1",
                new TaskIdParams("de38c76d-d54c-436c-8b9f-4c2703648d64", new HashMap<>()));
        assertEquals("2.0", response.getJsonrpc());
        assertEquals(1, response.getId());
        assertInstanceOf(TaskPushNotificationConfig.class, response.getResult());
        TaskPushNotificationConfig taskPushNotificationConfig = (TaskPushNotificationConfig) response.getResult();
        PushNotificationConfig pushNotificationConfig = taskPushNotificationConfig.pushNotificationConfig();
        assertNotNull(pushNotificationConfig);
        assertEquals("https://example.com/callback", pushNotificationConfig.url());
        PushNotificationAuthenticationInfo authenticationInfo = pushNotificationConfig.authentication();
        assertTrue(authenticationInfo.schemes().size() == 1);
        assertEquals("jwt", authenticationInfo.schemes().get(0));
    }

    @Test
    public void testA2AClientSetTaskPushNotificationConfig() throws Exception {
        SetTaskPushNotificationConfigRequest request = fromJson(SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST, SetTaskPushNotificationConfigRequest.class);
        when(transport.request(anyString(), eq(OBJECT_MAPPER.writeValueAsString(request))))
                .thenReturn(CompletableFuture.completedFuture(SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE));

        SetTaskPushNotificationConfigResponse response = client.setTaskPushNotificationConfig("1",
                "de38c76d-d54c-436c-8b9f-4c2703648d64",
                new PushNotificationConfig.Builder()
                        .url("https://example.com/callback")
                        .authenticationInfo(new PushNotificationAuthenticationInfo(Collections.singletonList("jwt"), null))
                        .build());
        assertEquals("2.0", response.getJsonrpc());
        assertEquals(1, response.getId());
        assertInstanceOf(TaskPushNotificationConfig.class, response.getResult());
        TaskPushNotificationConfig taskPushNotificationConfig = (TaskPushNotificationConfig) response.getResult();
        PushNotificationConfig pushNotificationConfig = taskPushNotificationConfig.pushNotificationConfig();
        assertNotNull(pushNotificationConfig);
        assertEquals("https://example.com/callback", pushNotificationConfig.url());
        PushNotificationAuthenticationInfo authenticationInfo = pushNotificationConfig.authentication();
        assertTrue(authenticationInfo.schemes().size() == 1);
        assertEquals("jwt", authenticationInfo.schemes().get(0));
    }

    @Test
    public void testA2AClientSendMessageWithFilePart() throws Exception {
        SendMessageRequest request = fromJson(SEND_MESSAGE_WITH_FILE_PART_TEST_REQUEST, SendMessageRequest.class);
        when(transport.request(anyString(), eq(OBJECT_MAPPER.writeValueAsString(request))))
                .thenReturn(CompletableFuture.completedFuture(SEND_MESSAGE_WITH_FILE_PART_TEST_RESPONSE));

        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(List.of(
                        new TextPart("analyze this image"),
                        new FilePart(new FileWithUri("image/jpeg", null, "file:///path/to/image.jpg"))
                ))
                .contextId("context-1234")
                .messageId("message-1234-with-file")
                .build();
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = new MessageSendParams.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        SendMessageResponse response = client.sendMessage("request-1234-with-file", params);

        assertEquals("2.0", response.getJsonrpc());
        assertNotNull(response.getId());
        Object result = response.getResult();
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertNotNull(task.getContextId());
        assertEquals(TaskState.COMPLETED, task.getStatus().state());
        assertEquals(1, task.getArtifacts().size());
        Artifact artifact = task.getArtifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("image-analysis", artifact.name());
        assertEquals(1, artifact.parts().size());
        Part<?> part = artifact.parts().get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("This is an image of a cat sitting on a windowsill.", ((TextPart) part).getText());
        assertTrue(task.getMetadata().isEmpty());
    }

    @Test
    public void testA2AClientSendMessageWithDataPart() throws Exception {
        SendMessageRequest request = fromJson(SEND_MESSAGE_WITH_DATA_PART_TEST_REQUEST, SendMessageRequest.class);
        when(transport.request(anyString(), eq(OBJECT_MAPPER.writeValueAsString(request))))
                .thenReturn(CompletableFuture.completedFuture(SEND_MESSAGE_WITH_DATA_PART_TEST_RESPONSE));

        Map<String, Object> data = new HashMap<>();
        data.put("temperature", 25.5);
        data.put("humidity", 60.2);
        data.put("location", "San Francisco");
        data.put("timestamp", "2024-01-15T10:30:00Z");

        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(List.of(
                        new TextPart("process this data"),
                        new DataPart(data)
                ))
                .contextId("context-1234")
                .messageId("message-1234-with-data")
                .build();
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = new MessageSendParams.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        SendMessageResponse response = client.sendMessage("request-1234-with-data", params);

        assertEquals("2.0", response.getJsonrpc());
        assertNotNull(response.getId());
        Object result = response.getResult();
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertNotNull(task.getContextId());
        assertEquals(TaskState.COMPLETED, task.getStatus().state());
        assertEquals(1, task.getArtifacts().size());
        Artifact artifact = task.getArtifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("data-analysis", artifact.name());
        assertEquals(1, artifact.parts().size());
        Part<?> part = artifact.parts().get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("Processed weather data: Temperature is 25.5°C, humidity is 60.2% in San Francisco.", ((TextPart) part).getText());
        assertTrue(task.getMetadata().isEmpty());
    }

    @Test
    public void testA2AClientSendMessageWithMixedParts() throws Exception {
        SendMessageRequest request = fromJson(SEND_MESSAGE_WITH_MIXED_PARTS_TEST_REQUEST, SendMessageRequest.class);
        when(transport.request(anyString(), eq(OBJECT_MAPPER.writeValueAsString(request))))
                .thenReturn(CompletableFuture.completedFuture(SEND_MESSAGE_WITH_MIXED_PARTS_TEST_RESPONSE));

        Map<String, Object> data = new HashMap<>();
        data.put("chartType", "bar");
        data.put("dataPoints", List.of(10, 20, 30, 40));
        data.put("labels", List.of("Q1", "Q2", "Q3", "Q4"));

        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(List.of(
                        new TextPart("analyze this data and image"),
                        new FilePart(new FileWithBytes("image/png", "chart.png", "aGVsbG8=")),
                        new DataPart(data)
                ))
                .contextId("context-1234")
                .messageId("message-1234-with-mixed")
                .build();
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = new MessageSendParams.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        SendMessageResponse response = client.sendMessage("request-1234-with-mixed", params);

        assertEquals("2.0", response.getJsonrpc());
        assertNotNull(response.getId());
        Object result = response.getResult();
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertNotNull(task.getContextId());
        assertEquals(TaskState.COMPLETED, task.getStatus().state());
        assertEquals(1, task.getArtifacts().size());
        Artifact artifact = task.getArtifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("mixed-analysis", artifact.name());
        assertEquals(1, artifact.parts().size());
        Part<?> part = artifact.parts().get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("Analyzed chart image and data: Bar chart showing quarterly data with values [10, 20, 30, 40].", ((TextPart) part).getText());
        assertTrue(task.getMetadata().isEmpty());
    }
}