package io.a2a.spec;

import static io.a2a.spec.Message.MESSAGE;
import static io.a2a.spec.Task.TASK;
import static io.a2a.spec.TaskArtifactUpdateEvent.ARTIFACT_UPDATE;
import static io.a2a.spec.TaskStatusUpdateEvent.STATUS_UPDATE;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Task.class, name = TASK),
        @JsonSubTypes.Type(value = Message.class, name = MESSAGE)
})
public interface EventType {

    String getType();
}
