package io.a2a.spec;

import static io.a2a.spec.A2A.CANCEL_TASK_METHOD;
import static io.a2a.spec.A2A.GET_TASK_METHOD;
import static io.a2a.spec.A2A.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2A.SEND_MESSAGE_METHOD;
import static io.a2a.spec.A2A.SEND_STREAMING_MESSAGE_METHOD;
import static io.a2a.spec.A2A.SEND_TASK_RESUBSCRIPTION_METHOD;
import static io.a2a.spec.A2A.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2A.isValidMethodName;
import static io.a2a.util.Utils.OBJECT_MAPPER;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public abstract class JSONRPCRequestDeserializerBase<T> extends StdDeserializer<JSONRPCRequest<?>> {

    public JSONRPCRequestDeserializerBase() {
        this(null);
    }

    public JSONRPCRequestDeserializerBase(Class<?> vc) {
        super(vc);
    }

    @Override
    public JSONRPCRequest<?> deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException, JsonProcessingException {
        JsonNode treeNode = jsonParser.getCodec().readTree(jsonParser);
        String jsonrpc = getAndValidateJsonrpc(treeNode, jsonParser);
        String method = getAndValidateMethod(treeNode, jsonParser);
        Object id = getAndValidateId(treeNode, jsonParser);
        JsonNode paramsNode = treeNode.get("params");

        switch (method) {
            case GET_TASK_METHOD:
                return new GetTaskRequest(jsonrpc, id, method, getAndValidateParams(paramsNode, jsonParser, treeNode, TaskQueryParams.class));
            case CANCEL_TASK_METHOD:
                return new CancelTaskRequest(jsonrpc, id, method, getAndValidateParams(paramsNode, jsonParser, treeNode, TaskIdParams.class));
            case SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD:
                return new SetTaskPushNotificationConfigRequest(jsonrpc, id, method, getAndValidateParams(paramsNode, jsonParser, treeNode, TaskPushNotificationConfig.class));
            case GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD:
                return new GetTaskPushNotificationConfigRequest(jsonrpc, id, method, getAndValidateParams(paramsNode, jsonParser, treeNode, TaskIdParams.class));
            case SEND_MESSAGE_METHOD:
                return new SendMessageRequest(jsonrpc, id, method, getAndValidateParams(paramsNode, jsonParser, treeNode, MessageSendParams.class));
            case SEND_TASK_RESUBSCRIPTION_METHOD:
                return new TaskResubscriptionRequest(jsonrpc, id, method, getAndValidateParams(paramsNode, jsonParser, treeNode, TaskIdParams.class));
            case SEND_STREAMING_MESSAGE_METHOD:
                return new SendStreamingMessageRequest(jsonrpc, id, method, getAndValidateParams(paramsNode, jsonParser, treeNode, MessageSendParams.class));
            default:
                throw new MethodNotFoundJsonMappingException("Invalid method", getIdIfPossible(treeNode, jsonParser));
        }
    }

    protected <T> T getAndValidateParams(JsonNode paramsNode, JsonParser jsonParser, JsonNode node, Class<T> paramsType) throws JsonMappingException {
        if (paramsNode == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.treeToValue(paramsNode, paramsType);
        } catch (JsonProcessingException e) {
            throw new InvalidParamsJsonMappingException("Invalid params", e, getIdIfPossible(node, jsonParser));
        }
    }

    protected String getAndValidateJsonrpc(JsonNode treeNode, JsonParser jsonParser) throws JsonMappingException {
        JsonNode jsonrpcNode = treeNode.get("jsonrpc");
        if (jsonrpcNode == null || ! jsonrpcNode.asText().equals(A2A.JSONRPC_VERSION)) {
            throw new IdJsonMappingException("Invalid JSON-RPC protocol version", getIdIfPossible(treeNode, jsonParser));
        }
        return jsonrpcNode.asText();
    }

    protected String getAndValidateMethod(JsonNode treeNode, JsonParser jsonParser) throws JsonMappingException {
        JsonNode methodNode = treeNode.get("method");
        if (methodNode == null) {
            throw new IdJsonMappingException("Missing method", getIdIfPossible(treeNode, jsonParser));
        }
        String method = methodNode.asText();
        if (! isValidMethodName(method)) {
            throw new MethodNotFoundJsonMappingException("Invalid method", getIdIfPossible(treeNode, jsonParser));
        }
        return method;
    }

    protected Object getAndValidateId(JsonNode treeNode, JsonParser jsonParser) throws JsonProcessingException {
        JsonNode idNode = treeNode.get("id");
        Object id = null;
        if (idNode != null) {
            if (idNode.isTextual()) {
                id = OBJECT_MAPPER.treeToValue(idNode, String.class);
            } else if (idNode.isNumber()) {
                id = OBJECT_MAPPER.treeToValue(idNode, Integer.class);
            } else {
                throw new JsonMappingException(jsonParser, "Invalid id");
            }
        }
        return id;
    }

    protected Object getIdIfPossible(JsonNode treeNode, JsonParser jsonParser) {
        try {
            return getAndValidateId(treeNode, jsonParser);
        } catch (JsonProcessingException e) {
            // id can't be determined
            return null;
        }
    }
}
