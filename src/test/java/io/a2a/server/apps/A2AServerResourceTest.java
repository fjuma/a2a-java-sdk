package io.a2a.server.apps;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.wildfly.common.Assert.assertNotNull;
import static org.wildfly.common.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.AgentCard;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.GetTaskPushNotificationRequest;
import io.a2a.spec.GetTaskPushNotificationResponse;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Part;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SendStreamingMessageResponse;
import io.a2a.spec.SetTaskPushNotificationRequest;
import io.a2a.spec.SetTaskPushNotificationResponse;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskResubscriptionRequest;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@QuarkusTest
public class A2AServerResourceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    TaskStore taskStore;

    private static final Task MINIMAL_TASK = new Task.Builder()
            .id("task-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.SUBMITTED))
            .build();

    private static final Task CANCEL_TASK = new Task.Builder()
            .id("cancel-task-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.SUBMITTED))
            .build();

    private static final Task CANCEL_TASK_NOT_SUPPORTED = new Task.Builder()
            .id("cancel-task-not-supported-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.SUBMITTED))
            .build();

    private static final Task SEND_MESSAGE_NOT_SUPPORTED = new Task.Builder()
            .id("task-not-supported-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.SUBMITTED))
            .build();

    private static final Message MESSAGE = new Message.Builder()
            .messageId("111")
            .role(Message.Role.AGENT)
            .parts(new TextPart("test message"))
            .build();

    @Test
    public void testGetTaskSuccess() {
        taskStore.save(MINIMAL_TASK);
        try {
            GetTaskRequest request = new GetTaskRequest("1", new TaskQueryParams(MINIMAL_TASK.getId()));
            GetTaskResponse response = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(GetTaskResponse.class);
            assertEquals("1", response.getId());
            assertEquals("task-123", response.getResult().getId());
            assertEquals("session-xyz", response.getResult().getContextId());
            assertEquals(TaskState.SUBMITTED, response.getResult().getStatus().state());
            assertNull(response.getError());
        } catch (Exception e) {
        } finally {
            taskStore.delete(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testGetTaskNotFound() {
        assertTrue(taskStore.get("non-existent-task") == null);
        GetTaskRequest request = new GetTaskRequest("1", new TaskQueryParams("non-existent-task"));
        GetTaskResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(GetTaskResponse.class);
        assertEquals("1", response.getId());
        // this should be an instance of TaskNotFoundError, see https://github.com/fjuma/a2a-java-sdk/issues/23
        assertInstanceOf(JSONRPCError.class, response.getError());
        assertEquals(new TaskNotFoundError().getCode(), response.getError().getCode());
        assertNull(response.getResult());
    }

    @Test
    public void testCancelTaskSuccess() {
        taskStore.save(CANCEL_TASK);
        try {
            CancelTaskRequest request = new CancelTaskRequest("1", new TaskIdParams(CANCEL_TASK.getId()));
            CancelTaskResponse response = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(CancelTaskResponse.class);
            assertNull(response.getError());
            assertEquals(request.getId(), response.getId());
            Task task = response.getResult();
            assertEquals(CANCEL_TASK.getId(), task.getId());
            assertEquals(CANCEL_TASK.getContextId(), task.getContextId());
            assertEquals(TaskState.CANCELED, task.getStatus().state());
        } catch (Exception e) {
        } finally {
            taskStore.delete(CANCEL_TASK.getId());
        }
    }

    @Test
    public void testCancelTaskNotSupported() {
        taskStore.save(CANCEL_TASK_NOT_SUPPORTED);
        try {
            CancelTaskRequest request = new CancelTaskRequest("1", new TaskIdParams(CANCEL_TASK_NOT_SUPPORTED.getId()));
            CancelTaskResponse response = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(CancelTaskResponse.class);
            assertEquals(request.getId(), response.getId());
            assertNull(response.getResult());
            // this should be an instance of UnsupportedOperationError, see https://github.com/fjuma/a2a-java-sdk/issues/23
            assertInstanceOf(JSONRPCError.class, response.getError());
            assertEquals(new UnsupportedOperationError().getCode(), response.getError().getCode());
        } catch (Exception e) {
        } finally {
            taskStore.delete(CANCEL_TASK_NOT_SUPPORTED.getId());
        }
    }

    @Test
    public void testCancelTaskNotFound() {
        CancelTaskRequest request = new CancelTaskRequest("1", new TaskIdParams("non-existent-task"));
        CancelTaskResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(CancelTaskResponse.class);
        assertEquals(request.getId(), response.getId());
        assertNull(response.getResult());
        // this should be an instance of UnsupportedOperationError, see https://github.com/fjuma/a2a-java-sdk/issues/23
        assertInstanceOf(JSONRPCError.class, response.getError());
        assertEquals(new TaskNotFoundError().getCode(), response.getError().getCode());
    }

    @Test
    public void testSendMessageNewMessageSuccess() {
        assertTrue(taskStore.get(MINIMAL_TASK.getId()) == null);
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams("1", message, null, null));
        SendMessageResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(SendMessageResponse.class);
        assertNull(response.getError());
        Message messageResponse =  (Message) response.getResult();
        assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
        assertEquals(MESSAGE.getRole(), messageResponse.getRole());
        Part<?> part = messageResponse.getParts().get(0);
        assertEquals(Part.Type.TEXT, part.getType());
        assertEquals("test message", ((TextPart) part).getText());
    }

    @Test
    public void testSendMessageExistingTaskSuccess() {
        taskStore.save(MINIMAL_TASK);
        try {
            Message message = new Message.Builder(MESSAGE)
                    .taskId(MINIMAL_TASK.getId())
                    .contextId(MINIMAL_TASK.getContextId())
                    .build();
            SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams("1", message, null, null));
            SendMessageResponse response = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(SendMessageResponse.class);
            assertNull(response.getError());
            Message messageResponse = (Message) response.getResult();
            assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
            assertEquals(MESSAGE.getRole(), messageResponse.getRole());
            Part<?> part = messageResponse.getParts().get(0);
            assertEquals(Part.Type.TEXT, part.getType());
            assertEquals("test message", ((TextPart) part).getText());
        } catch (Exception e) {
        } finally {
            taskStore.delete(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testSendMessageStreamNewMessageSuccess() throws Exception {
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams("1", message, null, null));
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8081/");
        Response response = target.request(MediaType.SERVER_SENT_EVENTS).post(Entity.json(request));
        InputStream inputStream = response.readEntity(InputStream.class);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    SendStreamingMessageResponse sendStreamingMessageResponse = OBJECT_MAPPER.readValue(line.substring("data: ".length()).trim(), SendStreamingMessageResponse.class);
                    assertNull(sendStreamingMessageResponse.getError());
                    Message messageResponse =  (Message) sendStreamingMessageResponse.getResult();
                    assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
                    assertEquals(MESSAGE.getRole(), messageResponse.getRole());
                    Part<?> part = messageResponse.getParts().get(0);
                    assertEquals(Part.Type.TEXT, part.getType());
                    assertEquals("test message", ((TextPart) part).getText());
                }
            }
        }
    }

    @Test
    public void testSendMessageStreamExistingTaskSuccess() {
        taskStore.save(MINIMAL_TASK);
        try {
            Message message = new Message.Builder(MESSAGE)
                    .taskId(MINIMAL_TASK.getId())
                    .contextId(MINIMAL_TASK.getContextId())
                    .build();
            SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                    "1", new MessageSendParams("1", message, null, null));
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target("http://localhost:8081/");
            Response response = target.request(MediaType.SERVER_SENT_EVENTS).post(Entity.json(request));
            InputStream inputStream = response.readEntity(InputStream.class);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        SendStreamingMessageResponse sendStreamingMessageResponse = OBJECT_MAPPER.readValue(line.substring("data: ".length()).trim(), SendStreamingMessageResponse.class);
                        assertNull(sendStreamingMessageResponse.getError());
                        Message messageResponse = (Message) sendStreamingMessageResponse.getResult();
                        assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
                        assertEquals(MESSAGE.getRole(), messageResponse.getRole());
                        Part<?> part = messageResponse.getParts().get(0);
                        assertEquals(Part.Type.TEXT, part.getType());
                        assertEquals("test message", ((TextPart) part).getText());
                    }
                }
            }
        } catch (Exception e) {
        } finally {
            taskStore.delete(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testSetPushNotificationSuccess() {
        taskStore.save(MINIMAL_TASK);
        try {
            TaskPushNotificationConfig taskPushConfig =
                    new TaskPushNotificationConfig(
                            MINIMAL_TASK.getId(), new
                            PushNotificationConfig("http://example.com", null, null));
            SetTaskPushNotificationRequest request = new SetTaskPushNotificationRequest("1", taskPushConfig);
            SetTaskPushNotificationResponse response = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(SetTaskPushNotificationResponse.class);
            assertNull(response.getError());
            assertEquals(request.getId(), response.getId());
            TaskPushNotificationConfig config = response.getResult();
            assertEquals(MINIMAL_TASK.getId(), config.id());
            assertEquals("http://example.com", config.pushNotificationConfig().url());
        } catch (Exception e) {
        } finally {
            taskStore.delete(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testGetPushNotificationSuccess() {
        taskStore.save(MINIMAL_TASK);
        try {
            TaskPushNotificationConfig taskPushConfig =
                    new TaskPushNotificationConfig(
                            MINIMAL_TASK.getId(), new
                            PushNotificationConfig("http://example.com", null, null));

            SetTaskPushNotificationRequest setTaskPushNotificationRequest = new SetTaskPushNotificationRequest("1", taskPushConfig);
            SetTaskPushNotificationResponse setTaskPushNotificationResponse = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(setTaskPushNotificationRequest)
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(SetTaskPushNotificationResponse.class);
            assertNotNull(setTaskPushNotificationResponse);

            GetTaskPushNotificationRequest request =
                    new GetTaskPushNotificationRequest("111", new TaskIdParams(MINIMAL_TASK.getId()));
            GetTaskPushNotificationResponse response = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(GetTaskPushNotificationResponse.class);
            assertNull(response.getError());
            assertEquals(request.getId(), response.getId());
            TaskPushNotificationConfig config = response.getResult();
            assertEquals(MINIMAL_TASK.getId(), config.id());
            assertEquals("http://example.com", config.pushNotificationConfig().url());
        } catch (Exception e) {
        } finally {
            taskStore.delete(MINIMAL_TASK.getId());
        }
    }

    @Disabled
    @Test
    public void testResubscribeNoExistingTaskError() throws Exception {
        TaskResubscriptionRequest request = new TaskResubscriptionRequest("1", new TaskIdParams("non-existent-task"));
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8081/");
        Response response = target.request(MediaType.SERVER_SENT_EVENTS).post(Entity.json(request));
        InputStream inputStream = response.readEntity(InputStream.class);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    SendStreamingMessageResponse sendStreamingMessageResponse = OBJECT_MAPPER.readValue(line.substring("data: ".length()).trim(), SendStreamingMessageResponse.class);
                    assertEquals(request.getId(), sendStreamingMessageResponse.getId());
                    assertNull(sendStreamingMessageResponse.getResult());
                    // this should be an instance of TaskNotFoundError, see https://github.com/fjuma/a2a-java-sdk/issues/23
                    assertInstanceOf(JSONRPCError.class, sendStreamingMessageResponse.getError());
                    assertEquals(new TaskNotFoundError().getCode(), sendStreamingMessageResponse.getError().getCode());
                }
            }
        }
    }


    @Test
    public void testError() {
        Message message = new Message.Builder(MESSAGE)
                .taskId(SEND_MESSAGE_NOT_SUPPORTED.getId())
                .contextId(SEND_MESSAGE_NOT_SUPPORTED.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest(
                "1", new MessageSendParams("1", message, null, null));
        SendMessageResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(SendMessageResponse.class);
        assertEquals(request.getId(), response.getId());
        assertNull(response.getResult());
        // this should be an instance of UnsupportedOperationError, see https://github.com/fjuma/a2a-java-sdk/issues/23
        assertInstanceOf(JSONRPCError.class, response.getError());
        assertEquals(new UnsupportedOperationError().getCode(), response.getError().getCode());
    }

    @Test
    public void testGetAgentCard() {
        AgentCard agentCard = given()
                .contentType(MediaType.APPLICATION_JSON)
                .when()
                .get("/.well-known/agent.json")
                .then()
                .statusCode(200)
                .extract()
                .as(AgentCard.class);
        assertNotNull(agentCard);
        assertEquals("test-card", agentCard.name());
        assertEquals("A test agent card", agentCard.description());
        assertEquals("http://localhost:8081", agentCard.url());
        assertEquals("1.0", agentCard.version());
        assertEquals("http://example.com/docs", agentCard.documentationUrl());
        assertTrue(agentCard.capabilities().pushNotifications());
        assertTrue(agentCard.capabilities().streaming());
        assertTrue(agentCard.capabilities().stateTransitionHistory());
        assertTrue(agentCard.skills().isEmpty());
    }
}
