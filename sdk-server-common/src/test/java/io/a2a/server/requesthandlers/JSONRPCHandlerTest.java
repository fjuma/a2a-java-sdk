package io.a2a.server.requesthandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import jakarta.enterprise.context.Dependent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.a2a.http.A2AHttpClient;
import io.a2a.http.A2AHttpResponse;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventConsumer;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.tasks.BasePushNotificationSender;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.ResultAggregator;
import io.a2a.server.tasks.TaskStore;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigResponse;
import io.a2a.spec.Event;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigResponse;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.ListTaskPushNotificationConfigResponse;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.PushNotificationNotSupportedError;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SendStreamingMessageResponse;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.SetTaskPushNotificationConfigResponse;
import io.a2a.spec.StreamingEventKind;
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
import io.quarkus.arc.profile.IfBuildProfile;
import mutiny.zero.ZeroPublisher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

public class JSONRPCHandlerTest {

    private static final AgentCard CARD = createAgentCard(true, true, true);

    private static final Task MINIMAL_TASK = new Task.Builder()
            .id("task-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.SUBMITTED))
            .build();

    private static final Message MESSAGE = new Message.Builder()
            .messageId("111")
            .role(Message.Role.AGENT)
            .parts(new TextPart("test message"))
            .build();

    AgentExecutor executor;
    TaskStore taskStore;
    RequestHandler requestHandler;
    AgentExecutorMethod agentExecutorExecute;
    AgentExecutorMethod agentExecutorCancel;
    private InMemoryQueueManager queueManager;
    private TestHttpClient httpClient;

    private final Executor internalExecutor = Executors.newCachedThreadPool();


    @BeforeEach
    public void init() {
        executor = new AgentExecutor() {
            @Override
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                if (agentExecutorExecute != null) {
                    agentExecutorExecute.invoke(context, eventQueue);
                }
            }

            @Override
            public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                if (agentExecutorCancel != null) {
                    agentExecutorCancel.invoke(context, eventQueue);
                }
            }
        };

        taskStore = new InMemoryTaskStore();
        queueManager = new InMemoryQueueManager();
        httpClient = new TestHttpClient();
        PushNotificationConfigStore pushConfigStore = new InMemoryPushNotificationConfigStore();
        PushNotificationSender pushSender = new BasePushNotificationSender(pushConfigStore, httpClient);

        requestHandler = new DefaultRequestHandler(executor, taskStore, queueManager, pushConfigStore, pushSender, internalExecutor);
    }

    @AfterEach
    public void cleanup() {
        agentExecutorExecute = null;
        agentExecutorCancel = null;
    }

    @Test
    public void testOnGetTaskSuccess() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        GetTaskRequest request = new GetTaskRequest("1", new TaskQueryParams(MINIMAL_TASK.getId()));
        GetTaskResponse response = handler.onGetTask(request);
        assertEquals(request.getId(), response.getId());
        assertSame(MINIMAL_TASK, response.getResult());
        assertNull(response.getError());
    }

    @Test
    public void testOnGetTaskNotFound() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        GetTaskRequest request = new GetTaskRequest("1", new TaskQueryParams(MINIMAL_TASK.getId()));
        GetTaskResponse response = handler.onGetTask(request);
        assertEquals(request.getId(), response.getId());
        assertInstanceOf(TaskNotFoundError.class, response.getError());
        assertNull(response.getResult());
    }

    @Test
    public void testOnCancelTaskSuccess() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        agentExecutorCancel = (context, eventQueue) -> {
            // We need to cancel the task or the EventConsumer never finds a 'final' event.
            // Looking at the Python implementation, they typically use AgentExecutors that
            // don't support cancellation. So my theory is the Agent updates the task to the CANCEL status
            Task task = context.getTask();
            TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
            taskUpdater.cancel();
        };

        CancelTaskRequest request = new CancelTaskRequest("111", new TaskIdParams(MINIMAL_TASK.getId()));
        CancelTaskResponse response = handler.onCancelTask(request);

        assertNull(response.getError());
        assertEquals(request.getId(), response.getId());
        Task task = response.getResult();
        assertEquals(MINIMAL_TASK.getId(), task.getId());
        assertEquals(MINIMAL_TASK.getContextId(), task.getContextId());
        assertEquals(TaskState.CANCELED, task.getStatus().state());
    }

    @Test
    public void testOnCancelTaskNotSupported() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        agentExecutorCancel = (context, eventQueue) -> {
            throw new UnsupportedOperationError();
        };

        CancelTaskRequest request = new CancelTaskRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        CancelTaskResponse response = handler.onCancelTask(request);
        assertEquals(request.getId(), response.getId());
        assertNull(response.getResult());
        assertInstanceOf(UnsupportedOperationError.class, response.getError());
    }

    @Test
    public void testOnCancelTaskNotFound() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        CancelTaskRequest request = new CancelTaskRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        CancelTaskResponse response = handler.onCancelTask(request);
        assertEquals(request.getId(), response.getId());
        assertNull(response.getResult());
        assertInstanceOf(TaskNotFoundError.class, response.getError());
    }

    @Test
    public void testOnMessageNewMessageSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request);
        assertNull(response.getError());
        // The Python implementation returns a Task here, but then again they are using hardcoded mocks and
        // bypassing the whole EventQueue.
        // If we were to send a Task in agentExecutorExecute EventConsumer.consumeAll() would not exit due to
        // the Task not having a 'final' state
        //
        // See testOnMessageNewMessageSuccessMocks() for a test more similar to the Python implementation
        assertSame(message, response.getResult());
    }

    @Test
    public void testOnMessageNewMessageSuccessMocks() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);

        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();

        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {Mockito.doReturn(ZeroPublisher.fromItems(MINIMAL_TASK)).when(mock).consumeAll();})){
            response = handler.onMessageSend(request);
        }
        assertNull(response.getError());
        assertSame(MINIMAL_TASK, response.getResult());
    }

    @Test
    public void testOnMessageNewMessageWithExistingTaskSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request);
        assertNull(response.getError());
        // The Python implementation returns a Task here, but then again they are using hardcoded mocks and
        // bypassing the whole EventQueue.
        // If we were to send a Task in agentExecutorExecute EventConsumer.consumeAll() would not exit due to
        // the Task not having a 'final' state
        //
        // See testOnMessageNewMessageWithExistingTaskSuccessMocks() for a test more similar to the Python implementation
        assertSame(message, response.getResult());
    }

    @Test
    public void testOnMessageNewMessageWithExistingTaskSuccessMocks() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromItems(MINIMAL_TASK)).when(mock).consumeAll();})){
            response = handler.onMessageSend(request);
        }
        assertNull(response.getError());
        assertSame(MINIMAL_TASK, response.getResult());

    }

    @Test
    public void testOnMessageError() {
        // See testMessageOnErrorMocks() for a test more similar to the Python implementation, using mocks for
        // EventConsumer.consumeAll()
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(new UnsupportedOperationError());
        };
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest(
                "1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request);
        assertInstanceOf(UnsupportedOperationError.class, response.getError());
        assertNull(response.getResult());
    }

    @Test
    public void testOnMessageErrorMocks() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest(
                "1", new MessageSendParams(message, null, null));
        SendMessageResponse response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromItems(new UnsupportedOperationError())).when(mock).consumeAll();})){
            response = handler.onMessageSend(request);
        }

        assertInstanceOf(UnsupportedOperationError.class, response.getError());
        assertNull(response.getResult());
    }

    @Test
    public void testOnMessageStreamNewMessageSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        Message message = new Message.Builder(MESSAGE)
            .taskId(MINIMAL_TASK.getId())
            .contextId(MINIMAL_TASK.getContextId())
            .build();

        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request);

        List<StreamingEventKind> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        response.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item.getResult());
                subscription.request(1);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                subscription.cancel();
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

        // The Python implementation has several events emitted since it uses mocks. Also, in the
        // implementation, a Message is considered a 'final' Event in EventConsumer.consumeAll()
        // so there would be no more Events.
        //
        // See testOnMessageStreamNewMessageSuccessMocks() for a test more similar to the Python implementation
        assertEquals(1, results.size());
        assertSame(message, results.get(0));
    }

    @Test
    public void testOnMessageStreamNewMessageSuccessMocks() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);

        // This is used to send events from a mock
        List<Event> events = List.of(
                MINIMAL_TASK,
                new TaskArtifactUpdateEvent.Builder()
                        .taskId(MINIMAL_TASK.getId())
                        .contextId(MINIMAL_TASK.getContextId())
                        .artifact(new Artifact.Builder()
                                .artifactId("art1")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                new TaskStatusUpdateEvent.Builder()
                        .taskId(MINIMAL_TASK.getId())
                        .contextId(MINIMAL_TASK.getContextId())
                        .status(new TaskStatus(TaskState.COMPLETED))
                        .build());

        Message message = new Message.Builder(MESSAGE)
            .taskId(MINIMAL_TASK.getId())
            .contextId(MINIMAL_TASK.getContextId())
            .build();

        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromIterable(events)).when(mock).consumeAll();})){
            response = handler.onMessageSendStream(request);
        }

        List<Event> results = new ArrayList<>();

        response.subscribe(new Flow.Subscriber<SendStreamingMessageResponse>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add((Event) item.getResult());
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });

        assertEquals(events, results);
    }

    @Test
    public void testOnMessageStreamNewMessageExistingTaskSuccess() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        Task task = new Task.Builder(MINIMAL_TASK)
                .history(new ArrayList<>())
                .build();
        taskStore.save(task);

        Message message = new Message.Builder(MESSAGE)
            .taskId(task.getId())
            .contextId(task.getContextId())
            .build();


        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request);

        // This Publisher never completes so we subscribe in a new thread.
        // I _think_ that is as expected, and testOnMessageStreamNewMessageSendPushNotificationSuccess seems
        // to confirm this
        final List<StreamingEventKind> results = new ArrayList<>();
        final AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        Executors.newSingleThreadExecutor().execute(() -> {
            response.subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscriptionRef.set(subscription);
                    subscription.request(1);
                }

                @Override
                public void onNext(SendStreamingMessageResponse item) {
                    results.add(item.getResult());
                    subscriptionRef.get().request(1);
                    latch.countDown();
                }

                @Override
                public void onError(Throwable throwable) {
                    subscriptionRef.get().cancel();
                }

                @Override
                public void onComplete() {
                    subscriptionRef.get().cancel();
                }
            });
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        subscriptionRef.get().cancel();
        // The Python implementation has several events emitted since it uses mocks.
        //
        // See testOnMessageStreamNewMessageExistingTaskSuccessMocks() for a test more similar to the Python implementation
        Task expected = new Task.Builder(task)
                .history(message)
                .build();
        assertEquals(1, results.size());
        StreamingEventKind receivedType = results.get(0);
        assertInstanceOf(Task.class, receivedType);
        Task received = (Task) receivedType;
        assertEquals(expected.getId(), received.getId());
        assertEquals(expected.getContextId(), received.getContextId());
        assertEquals(expected.getStatus(), received.getStatus());
        assertEquals(expected.getHistory(), received.getHistory());
    }

    @Test
    public void testOnMessageStreamNewMessageExistingTaskSuccessMocks() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);

        Task task = new Task.Builder(MINIMAL_TASK)
                .history(new ArrayList<>())
                .build();
        taskStore.save(task);

        // This is used to send events from a mock
        List<Event> events = List.of(
                new TaskArtifactUpdateEvent.Builder()
                        .taskId(task.getId())
                        .contextId(task.getContextId())
                        .artifact(new Artifact.Builder()
                                .artifactId("11")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                new TaskStatusUpdateEvent.Builder()
                        .taskId(task.getId())
                        .contextId(task.getContextId())
                        .status(new TaskStatus(TaskState.WORKING))
                        .build());

        Message message = new Message.Builder(MESSAGE)
            .taskId(task.getId())
            .contextId(task.getContextId())
            .build();

        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromIterable(events)).when(mock).consumeAll();})){
            response = handler.onMessageSendStream(request);
        }

        List<Event> results = new ArrayList<>();

        // Unlike testOnMessageStreamNewMessageExistingTaskSuccess() the ZeroPublisher.fromIterable()
        // used to mock the events completes once it has sent all the items. So no special thread
        // handling is needed.
        response.subscribe(new Flow.Subscriber<SendStreamingMessageResponse>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add((Event) item.getResult());
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });

        assertEquals(events, results);
    }


    @Test
    public void testSetPushNotificationConfigSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), new PushNotificationConfig.Builder().url("http://example.com").build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        SetTaskPushNotificationConfigResponse response = handler.setPushNotificationConfig(request);
        assertSame(taskPushConfig, response.getResult());
    }

    @Test
    public void testGetPushNotificationConfigSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };


        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), new PushNotificationConfig.Builder().url("http://example.com").build());

        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request);

        GetTaskPushNotificationConfigRequest getRequest =
                new GetTaskPushNotificationConfigRequest("111", new GetTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        GetTaskPushNotificationConfigResponse getResponse = handler.getPushNotificationConfig(getRequest);

        TaskPushNotificationConfig expectedConfig = new TaskPushNotificationConfig(MINIMAL_TASK.getId(),
                new PushNotificationConfig.Builder().id(MINIMAL_TASK.getId()).url("http://example.com").build());
        assertEquals(expectedConfig, getResponse.getResult());
    }

    @Test
    public void testOnMessageStreamNewMessageSendPushNotificationSuccess() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        List<Event> events = List.of(
                MINIMAL_TASK,
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
                        .build());


        agentExecutorExecute = (context, eventQueue) -> {
            // Hardcode the events to send here
            for (Event event : events) {
                eventQueue.enqueueEvent(event);
            }
        };


        TaskPushNotificationConfig config = new TaskPushNotificationConfig(
                MINIMAL_TASK.getId(),
                new PushNotificationConfig.Builder().url("http://example.com").build());
        SetTaskPushNotificationConfigRequest stpnRequest = new SetTaskPushNotificationConfigRequest("1", config);
        SetTaskPushNotificationConfigResponse stpnResponse = handler.setPushNotificationConfig(stpnRequest);
        assertNull(stpnResponse.getError());

        Message msg = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .build();
        SendStreamingMessageRequest request = new SendStreamingMessageRequest("1", new MessageSendParams(msg, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request);

        final List<StreamingEventKind> results = Collections.synchronizedList(new ArrayList<>());
        final AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(6);
        httpClient.latch = latch;

        Executors.newSingleThreadExecutor().execute(() -> {
            response.subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscriptionRef.set(subscription);
                    subscription.request(1);
                }

                @Override
                public void onNext(SendStreamingMessageResponse item) {
                    System.out.println("-> " + item.getResult());
                    results.add(item.getResult());
                    System.out.println(results);
                    subscriptionRef.get().request(1);
                    latch.countDown();
                }

                @Override
                public void onError(Throwable throwable) {
                    subscriptionRef.get().cancel();
                }

                @Override
                public void onComplete() {
                    subscriptionRef.get().cancel();
                }
            });
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        subscriptionRef.get().cancel();
        assertEquals(3, results.size());
        assertEquals(3, httpClient.tasks.size());

        Task curr = httpClient.tasks.get(0);
        assertEquals(MINIMAL_TASK.getId(), curr.getId());
        assertEquals(MINIMAL_TASK.getContextId(), curr.getContextId());
        assertEquals(MINIMAL_TASK.getStatus().state(), curr.getStatus().state());
        assertEquals(0, curr.getArtifacts() == null ? 0 : curr.getArtifacts().size());

        curr = httpClient.tasks.get(1);
        assertEquals(MINIMAL_TASK.getId(), curr.getId());
        assertEquals(MINIMAL_TASK.getContextId(), curr.getContextId());
        assertEquals(MINIMAL_TASK.getStatus().state(), curr.getStatus().state());
        assertEquals(1, curr.getArtifacts().size());
        assertEquals(1, curr.getArtifacts().get(0).parts().size());
        assertEquals("text", ((TextPart)curr.getArtifacts().get(0).parts().get(0)).getText());

        curr = httpClient.tasks.get(2);
        assertEquals(MINIMAL_TASK.getId(), curr.getId());
        assertEquals(MINIMAL_TASK.getContextId(), curr.getContextId());
        assertEquals(TaskState.COMPLETED, curr.getStatus().state());
        assertEquals(1, curr.getArtifacts().size());
        assertEquals(1, curr.getArtifacts().get(0).parts().size());
        assertEquals("text", ((TextPart)curr.getArtifacts().get(0).parts().get(0)).getText());
    }

    @Test
    public void testOnResubscribeExistingTaskSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        queueManager.createOrTap(MINIMAL_TASK.getId());

        agentExecutorExecute = (context, eventQueue) -> {
            // The only thing hitting the agent is the onMessageSend() and we should use the message
            eventQueue.enqueueEvent(context.getMessage());
            //eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskResubscriptionRequest request = new TaskResubscriptionRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onResubscribeToTask(request);

        // We need to send some events in order for those to end up in the queue
        Message message = new Message.Builder()
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .role(Message.Role.AGENT)
                .parts(new TextPart("text"))
                .build();
        SendMessageResponse smr =
                handler.onMessageSend(new SendMessageRequest("1", new MessageSendParams(message, null, null)));
        assertNull(smr.getError());


        List<StreamingEventKind> results = new ArrayList<>();

        response.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item.getResult());
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                subscription.cancel();
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

        // The Python implementation has several events emitted since it uses mocks.
        //
        // See testOnMessageStreamNewMessageExistingTaskSuccessMocks() for a test more similar to the Python implementation
        assertEquals(1, results.size());
    }


    @Test
    public void testOnResubscribeExistingTaskSuccessMocks() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        queueManager.createOrTap(MINIMAL_TASK.getId());

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
                        .status(new TaskStatus(TaskState.WORKING))
                        .build());

        TaskResubscriptionRequest request = new TaskResubscriptionRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        Flow.Publisher<SendStreamingMessageResponse> response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromIterable(events)).when(mock).consumeAll();})){
            response = handler.onResubscribeToTask(request);
        }

        List<StreamingEventKind> results = new ArrayList<>();

        // Unlike testOnResubscribeExistingTaskSuccess() the ZeroPublisher.fromIterable()
        // used to mock the events completes once it has sent all the items. So no special thread
        // handling is needed.
        response.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item.getResult());
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                subscription.cancel();
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

        // The Python implementation has several events emitted since it uses mocks.
        //
        // See testOnMessageStreamNewMessageExistingTaskSuccessMocks() for a test more similar to the Python implementation
        assertEquals(events, results);
    }

    @Test
    public void testOnResubscribeNoExistingTaskError() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);

        TaskResubscriptionRequest request = new TaskResubscriptionRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onResubscribeToTask(request);

        List<SendStreamingMessageResponse> results = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                subscription.cancel();
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

        assertEquals(1, results.size());
        assertNull(results.get(0).getResult());
        assertInstanceOf(TaskNotFoundError.class, results.get(0).getError());
    }

    @Test
    public void testStreamingNotSupportedError() {
        AgentCard card = createAgentCard(false, true, true);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler);

        SendStreamingMessageRequest request = new SendStreamingMessageRequest.Builder()
                .id("1")
                .params(new MessageSendParams.Builder()
                        .message(MESSAGE)
                        .build())
                .build();
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request);

        List<SendStreamingMessageResponse> results = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<SendStreamingMessageResponse>() {
            private Flow.Subscription subscription;
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                subscription.cancel();
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

        assertEquals(1, results.size());
        if (results.get(0).getError() != null && results.get(0).getError() instanceof InvalidRequestError ire) {
            assertEquals("Streaming is not supported by the agent", ire.getMessage());
        } else {
            fail("Expected a response containing an error");
        }
    }

    @Test
    public void testStreamingNotSupportedErrorOnResubscribeToTask() {
        // This test does not exist in the Python implementation
        AgentCard card = createAgentCard(false, true, true);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler);

        TaskResubscriptionRequest request = new TaskResubscriptionRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onResubscribeToTask(request);

        List<SendStreamingMessageResponse> results = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<SendStreamingMessageResponse>() {
            private Flow.Subscription subscription;
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                subscription.cancel();
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

        assertEquals(1, results.size());
        if (results.get(0).getError() != null && results.get(0).getError() instanceof InvalidRequestError ire) {
            assertEquals("Streaming is not supported by the agent", ire.getMessage());
        } else {
            fail("Expected a response containing an error");
        }
    }


    @Test
    public void testPushNotificationsNotSupportedError() {
        AgentCard card = createAgentCard(true, false, true);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler);
        taskStore.save(MINIMAL_TASK);

        TaskPushNotificationConfig config =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig.Builder()
                                .url("http://example.com")
                                .build());

        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest.Builder()
                .params(config)
                .build();
        SetTaskPushNotificationConfigResponse response = handler.setPushNotificationConfig(request);
        assertInstanceOf(PushNotificationNotSupportedError.class, response.getError());
    }

    @Test
    public void testOnGetPushNotificationNoPushNotifierConfig() {
        // Create request handler without a push notifier
        DefaultRequestHandler requestHandler =
                new DefaultRequestHandler(executor, taskStore, queueManager, null, null, internalExecutor);
        AgentCard card = createAgentCard(false, true, false);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler);

        taskStore.save(MINIMAL_TASK);

        GetTaskPushNotificationConfigRequest request =
                new GetTaskPushNotificationConfigRequest("id", new GetTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        GetTaskPushNotificationConfigResponse response = handler.getPushNotificationConfig(request);

        assertNotNull(response.getError());
        assertInstanceOf(UnsupportedOperationError.class, response.getError());
        assertEquals("This operation is not supported", response.getError().getMessage());
    }

    @Test
    public void testOnSetPushNotificationNoPushNotifierConfig() {
        // Create request handler without a push notifier
        DefaultRequestHandler requestHandler =
                new DefaultRequestHandler(executor, taskStore, queueManager, null, null, internalExecutor);
        AgentCard card = createAgentCard(false, true, false);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler);

        taskStore.save(MINIMAL_TASK);

                TaskPushNotificationConfig config =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig.Builder()
                                .url("http://example.com")
                                .build());

        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest.Builder()
                .params(config)
                .build();
        SetTaskPushNotificationConfigResponse response = handler.setPushNotificationConfig(request);

        assertInstanceOf(UnsupportedOperationError.class, response.getError());
        assertEquals("This operation is not supported", response.getError().getMessage());
    }

    @Test
    public void testOnMessageSendInternalError() {
        DefaultRequestHandler mocked = Mockito.mock(DefaultRequestHandler.class);
        Mockito.doThrow(new InternalError("Internal Error")).when(mocked).onMessageSend(Mockito.any(MessageSendParams.class));

        JSONRPCHandler handler = new JSONRPCHandler(CARD, mocked);

        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(MESSAGE, null, null));
        SendMessageResponse response = handler.onMessageSend(request);

        assertInstanceOf(InternalError.class, response.getError());
    }

    @Test
    public void testOnMessageStreamInternalError() {
        DefaultRequestHandler mocked = Mockito.mock(DefaultRequestHandler.class);
        Mockito.doThrow(new InternalError("Internal Error")).when(mocked).onMessageSendStream(Mockito.any(MessageSendParams.class));

        JSONRPCHandler handler = new JSONRPCHandler(CARD, mocked);

        SendStreamingMessageRequest request = new SendStreamingMessageRequest("1", new MessageSendParams(MESSAGE, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request);


        List<SendStreamingMessageResponse> results = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<SendStreamingMessageResponse>() {
            private Flow.Subscription subscription;
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                subscription.cancel();
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

        assertEquals(1, results.size());
        assertInstanceOf(InternalError.class, results.get(0).getError());
    }

    @Test
    @Disabled
    public void testDefaultRequestHandlerWithCustomComponents() {
        // Not much happening in the Python test beyond checking that the DefaultRequestHandler
        // constructor sets the fields as expected
    }

    @Test
    public void testOnMessageSendErrorHandling() {
        DefaultRequestHandler requestHandler =
                new DefaultRequestHandler(executor, taskStore, queueManager, null, null, internalExecutor);
        AgentCard card = createAgentCard(false, true, false);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler);

        taskStore.save(MINIMAL_TASK);

        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();

        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response;

        try (MockedConstruction<ResultAggregator> mocked = Mockito.mockConstruction(
                ResultAggregator.class,
                (mock, context) ->
                        Mockito.doThrow(
                                new UnsupportedOperationError())
                                .when(mock).consumeAndBreakOnInterrupt(Mockito.any(EventConsumer.class)))){
            response = handler.onMessageSend(request);
        }

        assertInstanceOf(UnsupportedOperationError.class, response.getError());

    }

    @Test
    public void testOnMessageSendTaskIdMismatch() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        agentExecutorExecute = ((context, eventQueue) -> {
            eventQueue.enqueueEvent(MINIMAL_TASK);
        });
        SendMessageRequest request = new SendMessageRequest("1",
                new MessageSendParams(MESSAGE, null, null));
        SendMessageResponse response = handler.onMessageSend(request);
        assertInstanceOf(InternalError.class, response.getError());

    }

    @Test
    public void testOnMessageStreamTaskIdMismatch() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        agentExecutorExecute = ((context, eventQueue) -> {
            eventQueue.enqueueEvent(MINIMAL_TASK);
        });

        SendStreamingMessageRequest request = new SendStreamingMessageRequest("1", new MessageSendParams(MESSAGE, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request);

        List<SendStreamingMessageResponse> results = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<SendStreamingMessageResponse>() {
            private Flow.Subscription subscription;
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                subscription.cancel();
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

        assertNull(error.get());
        assertEquals(1, results.size());
        assertInstanceOf(InternalError.class, results.get(0).getError());
    }

    @Test
    public void testListPushNotificationConfig() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.getId())
                        .build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request);

        ListTaskPushNotificationConfigRequest listRequest =
                new ListTaskPushNotificationConfigRequest("111", new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        ListTaskPushNotificationConfigResponse listResponse = handler.listPushNotificationConfig(listRequest);

        assertEquals("111", listResponse.getId());
        assertEquals(1, listResponse.getResult().size());
        assertEquals(taskPushConfig, listResponse.getResult().get(0));
    }

    @Test
    public void testListPushNotificationConfigNotSupported() {
        AgentCard card = createAgentCard(true, false, true);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.getId())
                        .build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request);

        ListTaskPushNotificationConfigRequest listRequest =
                new ListTaskPushNotificationConfigRequest("111", new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        ListTaskPushNotificationConfigResponse listResponse = handler.listPushNotificationConfig(listRequest);

        assertEquals("111", listResponse.getId());
        assertNull(listResponse.getResult());
        assertInstanceOf(PushNotificationNotSupportedError.class, listResponse.getError());
    }

    @Test
    public void testListPushNotificationConfigNoPushConfigStore() {
        DefaultRequestHandler requestHandler =
                new DefaultRequestHandler(executor, taskStore, queueManager, null, null, internalExecutor);
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        ListTaskPushNotificationConfigRequest listRequest =
                new ListTaskPushNotificationConfigRequest("111", new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        ListTaskPushNotificationConfigResponse listResponse = handler.listPushNotificationConfig(listRequest);

        assertEquals("111", listResponse.getId());
        assertNull(listResponse.getResult());
        assertInstanceOf(UnsupportedOperationError.class, listResponse.getError());
    }

    @Test
    public void testListPushNotificationConfigTaskNotFound() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        ListTaskPushNotificationConfigRequest listRequest =
                new ListTaskPushNotificationConfigRequest("111", new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        ListTaskPushNotificationConfigResponse listResponse = handler.listPushNotificationConfig(listRequest);

        assertEquals("111", listResponse.getId());
        assertNull(listResponse.getResult());
        assertInstanceOf(TaskNotFoundError.class, listResponse.getError());
    }

    @Test
    public void testDeletePushNotificationConfig() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.getId())
                        .build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request);

        DeleteTaskPushNotificationConfigRequest deleteRequest =
                new DeleteTaskPushNotificationConfigRequest("111", new DeleteTaskPushNotificationConfigParams(MINIMAL_TASK.getId(), MINIMAL_TASK.getId()));
        DeleteTaskPushNotificationConfigResponse deleteResponse = handler.deletePushNotificationConfig(deleteRequest);

        assertEquals("111", deleteResponse.getId());
        assertNull(deleteResponse.getError());
        assertNull(deleteResponse.getResult());
    }

    @Test
    public void testDeletePushNotificationConfigNotSupported() {
        AgentCard card = createAgentCard(true, false, true);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.getId())
                        .build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request);

        DeleteTaskPushNotificationConfigRequest deleteRequest =
                new DeleteTaskPushNotificationConfigRequest("111", new DeleteTaskPushNotificationConfigParams(MINIMAL_TASK.getId(), MINIMAL_TASK.getId()));
        DeleteTaskPushNotificationConfigResponse deleteResponse = handler.deletePushNotificationConfig(deleteRequest);

        assertEquals("111", deleteResponse.getId());
        assertNull(deleteResponse.getResult());
        assertInstanceOf(PushNotificationNotSupportedError.class, deleteResponse.getError());
    }

    @Test
    public void testDeletePushNotificationConfigNoPushConfigStore() {
        DefaultRequestHandler requestHandler =
                new DefaultRequestHandler(executor, taskStore, queueManager, null, null, internalExecutor);
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.getId())
                        .build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request);

        DeleteTaskPushNotificationConfigRequest deleteRequest =
                new DeleteTaskPushNotificationConfigRequest("111", new DeleteTaskPushNotificationConfigParams(MINIMAL_TASK.getId(), MINIMAL_TASK.getId()));
        DeleteTaskPushNotificationConfigResponse deleteResponse = handler.deletePushNotificationConfig(deleteRequest);

        assertEquals("111", deleteResponse.getId());
        assertNull(deleteResponse.getResult());
        assertInstanceOf(UnsupportedOperationError.class, deleteResponse.getError());
    }

    private static AgentCard createAgentCard(boolean streaming, boolean pushNotifications, boolean stateTransitionHistory) {
        return new AgentCard.Builder()
                .name("test-card")
                .description("A test agent card")
                .url("http://example.com")
                .version("1.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(streaming)
                        .pushNotifications(pushNotifications)
                        .stateTransitionHistory(stateTransitionHistory)
                        .build())
                .defaultInputModes(new ArrayList<>())
                .defaultOutputModes(new ArrayList<>())
                .skills(new ArrayList<>())
                .protocolVersion("0.2.5")
                .build();
    }

    private interface AgentExecutorMethod {
        void invoke(RequestContext context, EventQueue eventQueue) throws JSONRPCError;
    }

    @Dependent
    @IfBuildProfile("test")
    private static class TestHttpClient implements A2AHttpClient {
        final List<Task> tasks = Collections.synchronizedList(new ArrayList<>());
        volatile CountDownLatch latch;

        @Override
        public GetBuilder createGet() {
            return null;
        }

        @Override
        public PostBuilder createPost() {
            return new TestPostBuilder();
        }

        class TestPostBuilder implements A2AHttpClient.PostBuilder {
            private volatile String body;
            @Override
            public PostBuilder body(String body) {
                this.body = body;
                return this;
            }

            @Override
            public A2AHttpResponse post() throws IOException, InterruptedException {
                tasks.add(Utils.OBJECT_MAPPER.readValue(body, Task.TYPE_REFERENCE));
                try {
                    return new A2AHttpResponse() {
                        @Override
                        public int status() {
                            return 200;
                        }

                        @Override
                        public boolean success() {
                            return true;
                        }

                        @Override
                        public String body() {
                            return "";
                        }
                    };
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public CompletableFuture<Void> postAsyncSSE(Consumer<String> messageConsumer, Consumer<Throwable> errorConsumer, Runnable completeRunnable) throws IOException, InterruptedException {
                return null;
            }

            @Override
            public PostBuilder url(String s) {
                return this;
            }

            @Override
            public PostBuilder addHeader(String name, String value) {
                return this;
            }
        }
    }
}
