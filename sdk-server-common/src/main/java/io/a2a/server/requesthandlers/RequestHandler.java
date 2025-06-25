package io.a2a.server.requesthandlers;

import java.util.concurrent.Flow;

import io.a2a.spec.EventKind;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;

public interface RequestHandler {
    Task onGetTask(TaskQueryParams params) throws JSONRPCError;

    Task onCancelTask(TaskIdParams params) throws JSONRPCError;

    EventKind onMessageSend(MessageSendParams params) throws JSONRPCError;

    Flow.Publisher<StreamingEventKind> onMessageSendStream(MessageSendParams params) throws JSONRPCError;

    TaskPushNotificationConfig onSetTaskPushNotificationConfig(TaskPushNotificationConfig params) throws JSONRPCError;

    TaskPushNotificationConfig onGetTaskPushNotificationConfig(TaskIdParams params) throws JSONRPCError;

    Flow.Publisher<StreamingEventKind> onResubscribeToTask(TaskIdParams params) throws JSONRPCError;
}
