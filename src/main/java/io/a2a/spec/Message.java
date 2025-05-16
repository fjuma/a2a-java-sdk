package io.a2a.spec;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import io.a2a.util.Assert;

/**
 * An A2A message.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message implements EventType, StreamingEventType {

    static final String MESSAGE = "message";
    private final Role role;
    private final List<Part<?>> parts;
    private final String messageId;
    private String contextId;
    private String taskId;
    private final Map<String, Object> metadata;
    private final String type;

    public Message(Role role, List<Part<?>> parts, String messageId, String contextId, String taskId,
                   Map<String, Object> metadata) {
        this(role, parts, messageId, contextId, taskId, metadata, MESSAGE);
    }

    @JsonCreator
    public Message(@JsonProperty("role") Role role, @JsonProperty("parts") List<Part<?>> parts,
                   @JsonProperty("messageId") String messageId, @JsonProperty("contextId") String contextId,
                   @JsonProperty("taskId") String taskId, @JsonProperty("metadata") Map<String, Object> metadata,
                   @JsonProperty("type") String type) {
        Assert.checkNotNullParam("parts", parts);
        this.role = role;
        this.parts = parts;
        this.messageId = messageId == null ? UUID.randomUUID().toString() : messageId;
        this.contextId = contextId;
        this.taskId = taskId;
        this.metadata = metadata;
        this.type = type;
    }

    public Role getRole() {
        return role;
    }

    public List<Part<?>> getParts() {
        return parts;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getContextId() {
        return contextId;
    }

    public String getTaskId() {
        return taskId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    @Override
    public String getType() {
        return type;
    }

    public enum Role {
        USER("user"),
        AGENT("agent");

        private String role;

        Role(String role) {
            this.role = role;
        }

        @JsonValue
        public String asString() {
            return this.role;
        }
    }

    public static class Builder {

        private Role role;
        private List<Part<?>> parts;
        private String messageId;
        private String contextId;
        private String taskId;
        private Map<String, Object> metadata;

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder parts(List<Part<?>> parts) {
            this.parts = parts;
            return this;
        }

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Message build() {
            return new Message(role, parts, messageId, contextId, taskId, metadata);
        }
    }
}
