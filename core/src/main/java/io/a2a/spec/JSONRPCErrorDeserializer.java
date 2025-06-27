package io.a2a.spec;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class JSONRPCErrorDeserializer extends StdDeserializer<JSONRPCError> {

    public JSONRPCErrorDeserializer() {
        this(null);
    }

    public JSONRPCErrorDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public JSONRPCError deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException, JsonProcessingException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        int code = node.get("code").asInt();
        String message = node.get("message").asText();
        JsonNode dataNode = node.get("data");
        Object data = dataNode != null ? jsonParser.getCodec().treeToValue(dataNode, Object.class) : null;
        return new JSONRPCError(code, message, data);
    }
}
