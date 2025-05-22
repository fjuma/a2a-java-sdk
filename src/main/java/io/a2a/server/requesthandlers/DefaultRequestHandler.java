package io.a2a.server.requesthandlers;

import static io.a2a.util.AsyncUtils.convertingProcessor;
import static io.a2a.util.AsyncUtils.createTubeConfig;
import static io.a2a.util.AsyncUtils.processor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.Event;
import io.a2a.server.events.EventConsumer;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.QueueManager;
import io.a2a.server.events.TaskQueueExistsException;
import io.a2a.server.tasks.PushNotifier;
import io.a2a.server.tasks.ResultAggregator;
import io.a2a.server.tasks.TaskManager;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.EventType;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.StreamingEventType;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.UnsupportedOperationError;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultRequestHandler implements RequestHandler {

    private final AgentExecutor agentExecutor;
    private final TaskStore taskStore;
    private final QueueManager queueManager;
    private final PushNotifier pushNotifier;

    // TODO the value upstream is asyncio.Task. Trying a Runnable
    private final Map<String, Runnable> runningAgents = Collections.synchronizedMap(new HashMap<>());

    private final Executor executor = Executors.newSingleThreadExecutor();

    public DefaultRequestHandler(AgentExecutor agentExecutor, TaskStore taskStore, QueueManager queueManager, PushNotifier pushNotifier) {
        this.agentExecutor = agentExecutor;
        this.taskStore = taskStore;
        this.queueManager = queueManager;
        this.pushNotifier = pushNotifier;
    }

    @Override
    public Task onGetTask(TaskQueryParams params) throws JSONRPCError {
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }
        return task;
    }

    @Override
    public Task onCancelTask(TaskIdParams params) throws JSONRPCError {
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }
        TaskManager taskManager = new TaskManager(
                task.getId(),
                task.getContextId(),
                taskStore,
                null);

        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null);

        EventQueue queue = queueManager.tap(task.getId());
        if (queue == null) {
            queue = new EventQueue();
        }
        agentExecutor.cancel(
                new RequestContext(null, task.getId(), task.getContextId(), task, null),
                queue);

        // TODO need to cancel the asyncio.Task looked up from runningAgents

        EventConsumer consumer = new EventConsumer(queue);
        EventType type = resultAggregator.consumeAll(consumer);
        if (type instanceof Task tempTask) {
            return tempTask;
        }

        throw new InternalError("Agent did not return a valid response");
    }

    @Override
    public EventType onMessageSend(MessageSendParams params) throws JSONRPCError {
        TaskManager taskManager = new TaskManager(
                params.message().getTaskId(),
                params.message().getContextId(),
                taskStore,
                params.message());

        Task task = taskManager.getTask();
        if (task != null) {
            task = taskManager.updateWithMessage(params.message(), task);
            if (shouldAddPushInfo(params)) {
                pushNotifier.setInfo(task.getId(), params.configuration().pushNotification());
            }
        }
        RequestContext requestContext = new RequestContext(
                params,
                task == null ? null : task.getId(),
                task == null ? null : task.getContextId(),
                task,
                null);

        String taskId = requestContext.getTaskId();
        EventQueue queue = queueManager.createOrTap(taskId);
        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null);

        Runnable providerRunnable = new Runnable() {
            @Override
            public void run() {
                runEventStream(requestContext, queue);
            }
        };
        registerProducer(taskId, providerRunnable);

        EventConsumer consumer = new EventConsumer(queue);

        // TODO https://github.com/fjuma/a2a-java-sdk/issues/62 Add this callback

        EventType type = null;
        boolean interrupted = false;
        ResultAggregator.EventTypeAndInterrupt etai = resultAggregator.consumeAndBreakOnInterrupt(consumer);

        try {
            if (etai == null) {
                throw new InternalError();
            }
            interrupted = etai.interrupted();
        } finally {
            if (interrupted) {
                // TODO Make this async
                cleanupProducer(providerRunnable, taskId);
            } else {
                cleanupProducer(providerRunnable, taskId);
            }
        }

        return etai.eventType();
    }

    @Override
    public Flow.Publisher<StreamingEventType> onMessageSendStream(MessageSendParams params) throws JSONRPCError {
        TaskManager taskManager = new TaskManager(
                params.message().getTaskId(),
                params.message().getContextId(),
                taskStore,
                params.message());
        Task task = taskManager.getTask();

        if (task != null) {
            task = taskManager.updateWithMessage(params.message(), task);

            if (shouldAddPushInfo(params)) {
                pushNotifier.setInfo(task.getId(), params.configuration().pushNotification());
            }
        }

        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null);
        RequestContext requestContext = new RequestContext(
                params,
                task == null ? null : task.getId(),
                task == null ? null : task.getContextId(),
                task,
                null);

        AtomicReference<String> taskId = new AtomicReference<>(requestContext.getTaskId());
        EventQueue queue = queueManager.createOrTap(taskId.get());
        Runnable producerRunnable = new Runnable() {
            @Override
            public void run() {
                runEventStream(requestContext, queue);
            }
        };
        registerProducer(taskId.get(), producerRunnable);
        // TODO https://github.com/fjuma/a2a-java-sdk/issues/62 Add this callback

        try {
            EventConsumer consumer = new EventConsumer(queue);

            Flow.Publisher<Event> all = consumer.consumeAll();

            Flow.Publisher<Event> eventPublisher =
                    processor(createTubeConfig(), all, ((errorConsumer, event) -> {
                if (event instanceof Task createdTask && !taskId.get().equals(createdTask.getId())) {
                    try {
                        queueManager.add(createdTask.getId(), queue);
                        taskId.set(createdTask.getId());
                    } catch (TaskQueueExistsException e) {
                        // TODO Log
                    }
                    if (pushNotifier != null &&
                            params.configuration() != null &&
                            params.configuration().pushNotification() != null) {

                        pushNotifier.setInfo(
                                createdTask.getId(),
                                params.configuration().pushNotification());
                    }

                }
                if (pushNotifier != null && taskId.get() != null) {
                    EventType latest = resultAggregator.getCurrentResult();
                    if (latest instanceof Task latestTask) {
                        pushNotifier.sendNotification(latestTask);
                    }
                }

                return true;
            }));

            return convertingProcessor(createTubeConfig(), eventPublisher, event -> (StreamingEventType) event);
        } finally {
            cleanupProducer(producerRunnable, taskId.get());
        }
    }

    @Override
    public TaskPushNotificationConfig onSetTaskPushNotificationConfig(TaskPushNotificationConfig params) throws JSONRPCError {
        if (pushNotifier == null) {
            throw new UnsupportedOperationError();
        }
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        pushNotifier.setInfo(params.id(), params.pushNotificationConfig());

        return params;
    }

    @Override
    public TaskPushNotificationConfig onGetTaskPushNotificationConfig(TaskIdParams params) throws JSONRPCError {
        if (pushNotifier == null) {
            throw new UnsupportedOperationError();
        }
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        PushNotificationConfig pushNotificationConfig = pushNotifier.getInfo(params.id());
        if (pushNotificationConfig == null) {
            throw new InternalError();
        }

        return new TaskPushNotificationConfig(params.id(), pushNotificationConfig);
    }

    @Override
    public Flow.Publisher<Event> onResubscribeToTask(TaskIdParams params) throws JSONRPCError {
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        TaskManager taskManager = new TaskManager(task.getId(), task.getContextId(), taskStore, null);
        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null);
        EventQueue queue = queueManager.tap(task.getId());

        if (queue == null) {
            throw new TaskNotFoundError();
        }

        EventConsumer consumer = new EventConsumer(queue);

        return resultAggregator.consumeAndEmit(consumer);
    }

    private boolean shouldAddPushInfo(MessageSendParams params) {
        return pushNotifier != null && params.configuration() != null && params.configuration().pushNotification() != null;
    }

    private void runEventStream(RequestContext requestContext, EventQueue queue)  throws JSONRPCError {
        agentExecutor.execute(requestContext, queue);
    }

    private void registerProducer(String taskId, Runnable providerRunnable) {
        runningAgents.put(taskId, providerRunnable);
        executor.execute(providerRunnable);
    }

    private void cleanupProducer(Runnable producerRunnable, String taskId) {
        // TODO the Python implementation waits for the producerRunnable

        queueManager.close(taskId);
        runningAgents.remove(taskId);
    }

}
