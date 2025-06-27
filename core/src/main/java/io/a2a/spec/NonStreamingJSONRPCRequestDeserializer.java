package io.a2a.spec;

import static io.a2a.spec.A2A.CANCEL_TASK_METHOD;
import static io.a2a.spec.A2A.GET_TASK_METHOD;
import static io.a2a.spec.A2A.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2A.SEND_MESSAGE_METHOD;
import static io.a2a.spec.A2A.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

public class NonStreamingJSONRPCRequestDeserializer extends JSONRPCRequestDeserializerBase<NonStreamingJSONRPCRequest<?>> {

    public NonStreamingJSONRPCRequestDeserializer() {
        this(null);
    }

    public NonStreamingJSONRPCRequestDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public NonStreamingJSONRPCRequest<?> deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException, JsonProcessingException {
        JsonNode treeNode = jsonParser.getCodec().readTree(jsonParser);
        String jsonrpc = getAndValidateJsonrpc(treeNode, jsonParser);
        String method = getAndValidateMethod(treeNode, jsonParser);
        Object id = getAndValidateId(treeNode, jsonParser);
        JsonNode paramsNode = treeNode.get("params");

        switch (method) {
            case GET_TASK_METHOD:
                return new GetTaskRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, TaskQueryParams.class));
            case CANCEL_TASK_METHOD:
                return new CancelTaskRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, TaskIdParams.class));
            case SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD:
                return new SetTaskPushNotificationConfigRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, TaskPushNotificationConfig.class));
            case GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD:
                return new GetTaskPushNotificationConfigRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, TaskIdParams.class));
            case SEND_MESSAGE_METHOD:
                return new SendMessageRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, MessageSendParams.class));
            default:
                throw new MethodNotFoundJsonMappingException("Invalid method", getIdIfPossible(treeNode, jsonParser));
        }
    }
}
