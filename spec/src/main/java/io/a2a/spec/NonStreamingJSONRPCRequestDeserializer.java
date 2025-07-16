package io.a2a.spec;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Custom Jackson deserializer for non-streaming JSON-RPC requests in the A2A protocol.
 * This deserializer handles the polymorphic deserialization of different non-streaming
 * request types based on the "method" field in the JSON-RPC request.
 *
 * <p>The deserializer supports the following non-streaming request methods:</p>
 * <ul>
 *   <li>{@code tasks/get} - deserializes to {@link GetTaskRequest}</li>
 *   <li>{@code tasks/cancel} - deserializes to {@link CancelTaskRequest}</li>
 *   <li>{@code tasks/pushNotificationConfig/set} - deserializes to {@link SetTaskPushNotificationConfigRequest}</li>
 *   <li>{@code tasks/pushNotificationConfig/get} - deserializes to {@link GetTaskPushNotificationConfigRequest}</li>
 *   <li>{@code message/send} - deserializes to {@link SendMessageRequest}</li>
 * </ul>
 *
 * <p>This deserializer extends {@link JSONRPCRequestDeserializerBase} to inherit common
 * JSON-RPC validation and parsing functionality. If an unrecognized method is encountered,
 * it throws a {@link MethodNotFoundJsonMappingException} with the request ID for proper
 * error response generation.</p>
 */
public class NonStreamingJSONRPCRequestDeserializer extends JSONRPCRequestDeserializerBase<NonStreamingJSONRPCRequest<?>> {

    /**
     * Default constructor for the deserializer.
     * Calls the parameterized constructor with null value class.
     */
    public NonStreamingJSONRPCRequestDeserializer() {
        this(null);
    }

    /**
     * Constructs a deserializer with the specified value class.
     *
     * @param vc the value class for deserialization (can be null)
     */
    public NonStreamingJSONRPCRequestDeserializer(Class<?> vc) {
        super(vc);
    }

    /**
     * Deserializes a JSON-RPC request into the appropriate non-streaming request type
     * based on the method field.
     *
     * @param jsonParser the JSON parser containing the request data
     * @param context the deserialization context
     * @return the deserialized non-streaming JSON-RPC request
     * @throws IOException if an I/O error occurs during parsing
     * @throws JsonProcessingException if the JSON content is malformed
     * @throws MethodNotFoundJsonMappingException if the method is not recognized
     */
    @Override
    public NonStreamingJSONRPCRequest<?> deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException, JsonProcessingException {
        JsonNode treeNode = jsonParser.getCodec().readTree(jsonParser);
        String jsonrpc = getAndValidateJsonrpc(treeNode, jsonParser);
        String method = getAndValidateMethod(treeNode, jsonParser);
        Object id = getAndValidateId(treeNode, jsonParser);
        JsonNode paramsNode = treeNode.get("params");

        switch (method) {
            case GetTaskRequest.METHOD:
                return new GetTaskRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, TaskQueryParams.class));
            case CancelTaskRequest.METHOD:
                return new CancelTaskRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, TaskIdParams.class));
            case SetTaskPushNotificationConfigRequest.METHOD:
                return new SetTaskPushNotificationConfigRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, TaskPushNotificationConfig.class));
            case GetTaskPushNotificationConfigRequest.METHOD:
                return new GetTaskPushNotificationConfigRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, TaskIdParams.class));
            case SendMessageRequest.METHOD:
                return new SendMessageRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, MessageSendParams.class));
            default:
                throw new MethodNotFoundJsonMappingException("Invalid method", getIdIfPossible(treeNode, jsonParser));
        }
    }
}
