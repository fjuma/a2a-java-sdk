package io.a2a.server.agentexecution;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.a2a.spec.A2AServerException;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;

public class RequestContext {

    private MessageSendParams params;
    private String taskId;
    private String contextId;
    private Task task;
    private List<Task> relatedTasks;

    public RequestContext(MessageSendParams params, String taskId, String contextId, Task task, List<Task> relatedTasks) throws A2AServerException {
        this.params = params;
        this.taskId = taskId;
        this.contextId = contextId;
        this.task = task;
        if (relatedTasks == null) {
            this.relatedTasks = new ArrayList<>();
        }

        // if the taskId and contextId were specified, they must match the params
        if (params != null) {
            if (taskId != null && params.message().getTaskId() != taskId) {
                throw new A2AServerException(new InvalidParamsError("bad task id"));
            } else {
                checkOrGenerateTaskId();
            }
            if (contextId != null && params.message().getContextId() != contextId) {
                throw new A2AServerException(new InvalidParamsError("bad context id"));
            } else {
                checkOrGenerateContextId();
            }
        }
    }

    public MessageSendParams getParams() {
        return params;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getContextId() {
        return contextId;
    }

    public Task getTask() {
        return task;
    }

    public List<Task> getRelatedTasks() {
        return relatedTasks;
    }

    public Message getMessage() {
        return params != null ? params.message() : null;
    }

    public MessageSendConfiguration getConfiguration() {
        return params != null ? params.configuration() : null;
    }

    public String getUserInput(String delimiter) {
        if (params == null) {
            return "";
        }
        if (delimiter == null) {
            delimiter = "\n";
        }
        return getMessageText(params.message(), delimiter);
    }

    public void attachRelatedTask(Task task) {
        relatedTasks.add(task);
    }

    private void checkOrGenerateTaskId() {
        if (params == null) {
            return;
        }
        if (taskId == null && params.message().getTaskId() == null) {
            params.message().setTaskId(UUID.randomUUID().toString());
        }
        if (params.message().getTaskId() != null) {
            this.taskId = params.message().getTaskId();
        }
    }

    private void checkOrGenerateContextId() {
        if (params == null) {
            return;
        }
        if (contextId == null && params.message().getContextId() == null) {
            params.message().setContextId(UUID.randomUUID().toString());
        }
        if (params.message().getContextId() != null) {
            this.taskId = params.message().getContextId();
        }
    }

    private String getMessageText(Message message, String delimiter) {
        List<String> textParts = getTextParts(message.getParts());
        return String.join(delimiter, textParts);
    }

    private List<String> getTextParts(List<Part<?>> parts) {
        return parts.stream()
                .filter(part -> part.getType() == Part.Type.TEXT)
                .map(part -> (TextPart) part)
                .map(TextPart::getText)
                .collect(Collectors.toList());
    }
}
