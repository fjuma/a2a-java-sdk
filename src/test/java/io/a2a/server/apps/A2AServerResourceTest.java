package io.a2a.server.apps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.wildfly.common.Assert.assertNotNull;
import static org.wildfly.common.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.a2a.server.events.Event;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigResponse;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.Part;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SendStreamingMessageResponse;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.SetTaskPushNotificationConfigResponse;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskResubscriptionRequest;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;
import io.a2a.util.Utils;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class A2AServerResourceTest {

    @Inject
    TaskStore taskStore;

    @Inject
    InMemoryQueueManager queueManager;

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
        testGetTask();
    }

    private void testGetTask() {
        testGetTask(null);
    }

    private void testGetTask(String mediaType) {
        taskStore.save(MINIMAL_TASK);
        try {
            GetTaskRequest request = new GetTaskRequest("1", new TaskQueryParams(MINIMAL_TASK.getId()));
            RequestSpecification requestSpecification = RestAssured.given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request);
            if (mediaType != null) {
                requestSpecification = requestSpecification.accept(mediaType);
            }
            GetTaskResponse response = requestSpecification
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
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
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
        assertEquals(Part.Kind.TEXT, part.getKind());
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
            SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
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
            assertEquals(Part.Kind.TEXT, part.getKind());
            assertEquals("test message", ((TextPart) part).getText());
        } catch (Exception e) {
        } finally {
            taskStore.delete(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testSendMessageStreamNewMessageSuccess() throws Exception {
        testSendStreamingMessage();
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
                    "1", new MessageSendParams(message, null, null));
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target("http://localhost:8081/");
            Response response = target.request().post(Entity.json(request));
            InputStream inputStream = response.readEntity(InputStream.class);
            boolean dataRead = false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        SendStreamingMessageResponse sendStreamingMessageResponse = Utils.OBJECT_MAPPER.readValue(line.substring("data: ".length()).trim(), SendStreamingMessageResponse.class);
                        assertNull(sendStreamingMessageResponse.getError());
                        Message messageResponse = (Message) sendStreamingMessageResponse.getResult();
                        assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
                        assertEquals(MESSAGE.getRole(), messageResponse.getRole());
                        Part<?> part = messageResponse.getParts().get(0);
                        assertEquals(Part.Kind.TEXT, part.getKind());
                        assertEquals("test message", ((TextPart) part).getText());
                        dataRead = true;
                    }
                }
            }
            assertTrue(dataRead);
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
                            MINIMAL_TASK.getId(), new PushNotificationConfig.Builder().url("http://example.com").build());
            SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
            SetTaskPushNotificationConfigResponse response = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(SetTaskPushNotificationConfigResponse.class);
            assertNull(response.getError());
            assertEquals(request.getId(), response.getId());
            TaskPushNotificationConfig config = response.getResult();
            assertEquals(MINIMAL_TASK.getId(), config.taskId());
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
                            MINIMAL_TASK.getId(), new PushNotificationConfig.Builder().url("http://example.com").build());

            SetTaskPushNotificationConfigRequest setTaskPushNotificationRequest = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
            SetTaskPushNotificationConfigResponse setTaskPushNotificationResponse = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(setTaskPushNotificationRequest)
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(SetTaskPushNotificationConfigResponse.class);
            assertNotNull(setTaskPushNotificationResponse);

            GetTaskPushNotificationConfigRequest request =
                    new GetTaskPushNotificationConfigRequest("111", new TaskIdParams(MINIMAL_TASK.getId()));
            GetTaskPushNotificationConfigResponse response = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(GetTaskPushNotificationConfigResponse.class);
            assertNull(response.getError());
            assertEquals(request.getId(), response.getId());
            TaskPushNotificationConfig config = response.getResult();
            assertEquals(MINIMAL_TASK.getId(), config.taskId());
            assertEquals("http://example.com", config.pushNotificationConfig().url());
        } catch (Exception e) {
        } finally {
            taskStore.delete(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testResubscribeExistingTaskSuccess() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        taskStore.save(MINIMAL_TASK);

        try {
            // attempting to send a streaming message instead of explicitly calling queueManager#createOrTap
            // does not work because after the message is sent, the queue becomes null but task resubscription
            // requires the queue to still be active
            queueManager.createOrTap(MINIMAL_TASK.getId());

            CountDownLatch taskResubscriptionRequestSent = new CountDownLatch(1);
            CountDownLatch taskResubscriptionResponseReceived = new CountDownLatch(2);
            AtomicReference<SendStreamingMessageResponse> firstResponse = new AtomicReference<>();
            AtomicReference<SendStreamingMessageResponse> secondResponse = new AtomicReference<>();

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                // resubscribe to the task, requires the task and its queue to still be active
                TaskResubscriptionRequest taskResubscriptionRequest = new TaskResubscriptionRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
                Client client = ClientBuilder.newClient();
                WebTarget target = client.target("http://localhost:8081/");
                taskResubscriptionRequestSent.countDown();
                Response response = target.request().post(Entity.json(taskResubscriptionRequest));
                InputStream inputStream = response.readEntity(InputStream.class);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            SendStreamingMessageResponse sendStreamingMessageResponse = Utils.OBJECT_MAPPER.readValue(line.substring("data: ".length()).trim(), SendStreamingMessageResponse.class);
                            if (taskResubscriptionResponseReceived.getCount() == 2) {
                                firstResponse.set(sendStreamingMessageResponse);
                            } else {
                                secondResponse.set(sendStreamingMessageResponse);
                            }
                            taskResubscriptionResponseReceived.countDown();
                            if (taskResubscriptionResponseReceived.getCount() == 0) {
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                } finally {
                    response.close();
                    client.close();
                }
            }, executorService);

            try {
                taskResubscriptionRequestSent.await();
                // sleep to ensure that the events are sent after the client request is made
                Thread.sleep(1000);
                List<Event> events = List.of(
                        new TaskArtifactUpdateEvent.Builder()
                                .taskId(MINIMAL_TASK.getId())
                                .contextId(MINIMAL_TASK.getContextId())
                                .artifact(new Artifact.Builder()
                                        .artifactId("11")
                                        .parts(new TextPart("text"))
                                        .build())
                                .build(),
                        new TaskStatusUpdateEvent.Builder()
                                .taskId(MINIMAL_TASK.getId())
                                .contextId(MINIMAL_TASK.getContextId())
                                .status(new TaskStatus(TaskState.COMPLETED))
                                .isFinal(true)
                                .build());

                for (Event event : events) {
                    queueManager.get(MINIMAL_TASK.getId()).enqueueEvent(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // wait for the client to receive the responses
            taskResubscriptionResponseReceived.await();

            assertNotNull(firstResponse.get());
            SendStreamingMessageResponse sendStreamingMessageResponse = firstResponse.get();
            assertNull(sendStreamingMessageResponse.getError());
            TaskArtifactUpdateEvent taskArtifactUpdateEvent = (TaskArtifactUpdateEvent) sendStreamingMessageResponse.getResult();
            assertEquals(MINIMAL_TASK.getId(), taskArtifactUpdateEvent.getTaskId());
            assertEquals(MINIMAL_TASK.getContextId(), taskArtifactUpdateEvent.getContextId());
            Part<?> part = taskArtifactUpdateEvent.getArtifact().parts().get(0);
            assertEquals(Part.Kind.TEXT, part.getKind());
            assertEquals("text", ((TextPart) part).getText());

            assertNotNull(secondResponse.get());
            sendStreamingMessageResponse = secondResponse.get();
            assertNull(sendStreamingMessageResponse.getError());
            TaskStatusUpdateEvent taskStatusUpdateEvent = (TaskStatusUpdateEvent) sendStreamingMessageResponse.getResult();
            assertEquals(MINIMAL_TASK.getId(), taskStatusUpdateEvent.getTaskId());
            assertEquals(MINIMAL_TASK.getContextId(), taskStatusUpdateEvent.getContextId());
            assertEquals(TaskState.COMPLETED, taskStatusUpdateEvent.getStatus().state());
            assertNotNull(taskStatusUpdateEvent.getStatus().timestamp());
        } finally {
            taskStore.delete(MINIMAL_TASK.getId());
            executorService.shutdown();
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        }
    }

    @Test
    public void testResubscribeNoExistingTaskError() throws Exception {
        TaskResubscriptionRequest request = new TaskResubscriptionRequest("1", new TaskIdParams("non-existent-task"));
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8081/");
        Response response = target.request().post(Entity.json(request));
        InputStream inputStream = response.readEntity(InputStream.class);
        boolean dataRead = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    SendStreamingMessageResponse sendStreamingMessageResponse = Utils.OBJECT_MAPPER.readValue(line.substring("data: ".length()).trim(), SendStreamingMessageResponse.class);
                    assertEquals(request.getId(), sendStreamingMessageResponse.getId());
                    assertNull(sendStreamingMessageResponse.getResult());
                    // this should be an instance of TaskNotFoundError, see https://github.com/fjuma/a2a-java-sdk/issues/23
                    assertInstanceOf(JSONRPCError.class, sendStreamingMessageResponse.getError());
                    assertEquals(new TaskNotFoundError().getCode(), sendStreamingMessageResponse.getError().getCode());
                    dataRead = true;
                }
            }
        }
        assertTrue(dataRead);
    }
    
    @Test
    public void testError() {
        Message message = new Message.Builder(MESSAGE)
                .taskId(SEND_MESSAGE_NOT_SUPPORTED.getId())
                .contextId(SEND_MESSAGE_NOT_SUPPORTED.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest(
                "1", new MessageSendParams(message, null, null));
        SendMessageResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
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

    @Test
    public void testGetExtendAgentCardNotSupported() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .when()
                .get("/agent/authenticatedExtendedCard")
                .then()
                .statusCode(404)
                .body("error", equalTo("Extended agent card not supported or not enabled."));
    }

    @Test
    public void testMalformedJSONRPCRequest() {
        // missing closing bracket
        String malformedRequest = "{\"jsonrpc\": \"2.0\", \"method\": \"message/send\", \"params\": {\"foo\": \"bar\"}";
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(malformedRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new JSONParseError().getCode(), response.getError().getCode());
    }

    @Test
    public void testInvalidParamsJSONRPCRequest() {
        String invalidParamsRequest = """
            {"jsonrpc": "2.0", "method": "message/send", "params": "not_a_dict", "id": "1"}
            """;
        testInvalidParams(invalidParamsRequest);

        invalidParamsRequest = """
            {"jsonrpc": "2.0", "method": "message/send", "params": {"message": {"parts": "invalid"}}, "id": "1"}
            """;
        testInvalidParams(invalidParamsRequest);
    }

    private void testInvalidParams(String invalidParamsRequest) {
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidParamsRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new InvalidParamsError().getCode(), response.getError().getCode());
        assertEquals("1", response.getId());
    }

    @Test
    public void testInvalidJSONRPCRequestMissingJsonrpc() {
        String invalidRequest = """
            {
             "method": "message/send",
             "params": {}
            }
            """;
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new InvalidRequestError().getCode(), response.getError().getCode());
    }

    @Test
    public void testInvalidJSONRPCRequestMissingMethod() {
        String invalidRequest = """
            {"jsonrpc": "2.0", "params": {}}
            """;
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new InvalidRequestError().getCode(), response.getError().getCode());
    }

    @Test
    public void testInvalidJSONRPCRequestInvalidId() {
        String invalidRequest = """
            {"jsonrpc": "2.0", "method": "message/send", "params": {}, "id": {"bad": "type"}}
            """;
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new InvalidRequestError().getCode(), response.getError().getCode());
    }

    @Test
    public void testInvalidJSONRPCRequestNonExistentMethod() {
        String invalidRequest = """
            {"jsonrpc": "2.0", "method" : "nonexistent/method", "params": {}}
            """;
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new MethodNotFoundError().getCode(), response.getError().getCode());
    }

    @Test
    public void testNonStreamingMethodWithAcceptHeader() {
        testGetTask(MediaType.APPLICATION_JSON);
    }

    @Test
    public void testStreamingMethodWithAcceptHeader() throws Exception {
        testSendStreamingMessage(MediaType.SERVER_SENT_EVENTS);
    }

    private void testSendStreamingMessage() throws Exception {
        testSendStreamingMessage(null);
    }

    private void testSendStreamingMessage(String mediaType) throws Exception {
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8081/");
        Response response;
        if (mediaType != null) {
            response = target.request(mediaType).post(Entity.json(request));
        } else {
            response = target.request().post(Entity.json(request));
        }
        InputStream inputStream = response.readEntity(InputStream.class);
        boolean dataRead = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    SendStreamingMessageResponse sendStreamingMessageResponse = Utils.OBJECT_MAPPER.readValue(line.substring("data: ".length()).trim(), SendStreamingMessageResponse.class);
                    assertNull(sendStreamingMessageResponse.getError());
                    Message messageResponse =  (Message) sendStreamingMessageResponse.getResult();
                    assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
                    assertEquals(MESSAGE.getRole(), messageResponse.getRole());
                    Part<?> part = messageResponse.getParts().get(0);
                    assertEquals(Part.Kind.TEXT, part.getKind());
                    assertEquals("test message", ((TextPart) part).getText());
                    dataRead = true;
                }
            }
        }
        assertTrue(dataRead);
    }
}
