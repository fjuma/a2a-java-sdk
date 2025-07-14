package io.a2a.server.apps.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import static org.wildfly.common.Assert.assertNotNull;
import static org.wildfly.common.Assert.assertTrue;

import io.a2a.client.A2AClient;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.GetTaskPushNotificationConfigResponse;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Part;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SetTaskPushNotificationConfigResponse;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;

public abstract class AbstractA2AServerTest {

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

    private final int serverPort;
    private A2AClient client;

    protected AbstractA2AServerTest(int serverPort) {
        this.serverPort = serverPort;
        this.client = new A2AClient("http://localhost:" + serverPort);
    }

    @Test
    public void testGetTaskSuccess() {
        getTaskStore().save(MINIMAL_TASK);
        try {
            GetTaskResponse response = client.getTask(MINIMAL_TASK.getId());
            assertEquals("task-123", response.getResult().getId());
            assertEquals("session-xyz", response.getResult().getContextId());
            assertEquals(TaskState.SUBMITTED, response.getResult().getStatus().state());
            assertNull(response.getError());
        } catch (A2AServerException e) {
            // Handle exception
        } finally {
            getTaskStore().delete(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testGetTaskNotFound() {
        assertTrue(getTaskStore().get("non-existent-task") == null);
        try {
            GetTaskResponse response = client.getTask("non-existent-task");
            // this should be an instance of TaskNotFoundError, see https://github.com/a2aproject/a2a-java/issues/23
            assertInstanceOf(JSONRPCError.class, response.getError());
            assertEquals(new TaskNotFoundError().getCode(), response.getError().getCode());
            assertNull(response.getResult());
        } catch (A2AServerException e) {
            // Handle exception
        }
    }

    @Test
    public void testCancelTaskSuccess() {
        getTaskStore().save(CANCEL_TASK);
        try {
            CancelTaskResponse response = client.cancelTask(CANCEL_TASK.getId());
            assertNull(response.getError());
            Task task = response.getResult();
            assertEquals(CANCEL_TASK.getId(), task.getId());
            assertEquals(CANCEL_TASK.getContextId(), task.getContextId());
            assertEquals(TaskState.CANCELED, task.getStatus().state());
        } catch (A2AServerException e) {
            // Handle exception
        } finally {
            getTaskStore().delete(CANCEL_TASK.getId());
        }
    }

    @Test
    public void testCancelTaskNotSupported() {
        getTaskStore().save(CANCEL_TASK_NOT_SUPPORTED);
        try {
            CancelTaskResponse response = client.cancelTask(CANCEL_TASK_NOT_SUPPORTED.getId());
            assertNull(response.getResult());
            // this should be an instance of UnsupportedOperationError, see https://github.com/a2aproject/a2a-java/issues/23
            assertInstanceOf(JSONRPCError.class, response.getError());
            assertEquals(new UnsupportedOperationError().getCode(), response.getError().getCode());
        } catch (A2AServerException e) {
            // Handle exception
        } finally {
            getTaskStore().delete(CANCEL_TASK_NOT_SUPPORTED.getId());
        }
    }

    @Test
    public void testCancelTaskNotFound() {
        try {
            CancelTaskResponse response = client.cancelTask("non-existent-task");
            assertNull(response.getResult());
            // this should be an instance of UnsupportedOperationError, see https://github.com/a2aproject/a2a-java/issues/23
            assertInstanceOf(JSONRPCError.class, response.getError());
            assertEquals(new TaskNotFoundError().getCode(), response.getError().getCode());
        } catch (A2AServerException e) {
            // Handle exception
        }
    }

    @Test
    public void testSendMessageNewMessageSuccess() {
        assertTrue(getTaskStore().get(MINIMAL_TASK.getId()) == null);
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        MessageSendParams messageSendParams = new MessageSendParams(message, null, null);
        
        try {
            SendMessageResponse response = client.sendMessage(messageSendParams);
            assertNull(response.getError());
            Message messageResponse = (Message) response.getResult();
            assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
            assertEquals(MESSAGE.getRole(), messageResponse.getRole());
            Part<?> part = messageResponse.getParts().get(0);
            assertEquals(Part.Kind.TEXT, part.getKind());
            assertEquals("test message", ((TextPart) part).getText());
        } catch (A2AServerException e) {
            // Handle exception
        }
    }

    @Test
    public void testSendMessageExistingTaskSuccess() {
        getTaskStore().save(MINIMAL_TASK);
        try {
            Message message = new Message.Builder(MESSAGE)
                    .taskId(MINIMAL_TASK.getId())
                    .contextId(MINIMAL_TASK.getContextId())
                    .build();
            MessageSendParams messageSendParams = new MessageSendParams(message, null, null);
            
            SendMessageResponse response = client.sendMessage(messageSendParams);
            assertNull(response.getError());
            Message messageResponse = (Message) response.getResult();
            assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
            assertEquals(MESSAGE.getRole(), messageResponse.getRole());
            Part<?> part = messageResponse.getParts().get(0);
            assertEquals(Part.Kind.TEXT, part.getKind());
            assertEquals("test message", ((TextPart) part).getText());
        } catch (A2AServerException e) {
            // Handle exception
        } finally {
            getTaskStore().delete(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testSetPushNotificationSuccess() {
        getTaskStore().save(MINIMAL_TASK);
        try {
            PushNotificationConfig pushNotificationConfig = new PushNotificationConfig.Builder().url("http://example.com").build();
            SetTaskPushNotificationConfigResponse response = client.setTaskPushNotificationConfig(MINIMAL_TASK.getId(), pushNotificationConfig);
            assertNull(response.getError());
            TaskPushNotificationConfig config = response.getResult();
            assertEquals(MINIMAL_TASK.getId(), config.taskId());
            assertEquals("http://example.com", config.pushNotificationConfig().url());
        } catch (A2AServerException e) {
            // Handle exception
        } finally {
            getTaskStore().delete(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testGetPushNotificationSuccess() {
        getTaskStore().save(MINIMAL_TASK);
        try {
            PushNotificationConfig pushNotificationConfig = new PushNotificationConfig.Builder().url("http://example.com").build();
            
            // First set the push notification config
            SetTaskPushNotificationConfigResponse setResponse = client.setTaskPushNotificationConfig(MINIMAL_TASK.getId(), pushNotificationConfig);
            assertNotNull(setResponse);

            // Then get the push notification config
            GetTaskPushNotificationConfigResponse response = client.getTaskPushNotificationConfig(MINIMAL_TASK.getId());
            assertNull(response.getError());
            TaskPushNotificationConfig config = response.getResult();
            assertEquals(MINIMAL_TASK.getId(), config.taskId());
            assertEquals("http://example.com", config.pushNotificationConfig().url());
        } catch (A2AServerException e) {
            // Handle exception
        } finally {
            getTaskStore().delete(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testError() {
        Message message = new Message.Builder(MESSAGE)
                .taskId(SEND_MESSAGE_NOT_SUPPORTED.getId())
                .contextId(SEND_MESSAGE_NOT_SUPPORTED.getContextId())
                .build();
        MessageSendParams messageSendParams = new MessageSendParams(message, null, null);
        
        try {
            SendMessageResponse response = client.sendMessage(messageSendParams);
            assertNull(response.getResult());
            // this should be an instance of UnsupportedOperationError, see https://github.com/a2aproject/a2a-java/issues/23
            assertInstanceOf(JSONRPCError.class, response.getError());
            assertEquals(new UnsupportedOperationError().getCode(), response.getError().getCode());
        } catch (A2AServerException e) {
            // Handle exception
        }
    }

    @Test
    public void testGetAgentCard() {
        try {
            AgentCard agentCard = client.getAgentCard();
            assertNotNull(agentCard);
            assertEquals("test-card", agentCard.name());
            assertEquals("A test agent card", agentCard.description());
            assertEquals("http://localhost:" + serverPort, agentCard.url());
            assertEquals("1.0", agentCard.version());
            assertEquals("http://example.com/docs", agentCard.documentationUrl());
            assertTrue(agentCard.capabilities().pushNotifications());
            assertTrue(agentCard.capabilities().streaming());
            assertTrue(agentCard.capabilities().stateTransitionHistory());
            assertTrue(agentCard.skills().isEmpty());
        } catch (A2AClientError e) {
            // Handle exception
        }
    }

    protected abstract TaskStore getTaskStore();

    protected abstract InMemoryQueueManager getQueueManager();

    protected abstract void setStreamingSubscribedRunnable(Runnable runnable);
}
