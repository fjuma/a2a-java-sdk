package io.a2a.spec;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class JSONRPCVoidResponseSerializer extends StdSerializer<JSONRPCResponse<Void>> {

    private static final JSONRPCErrorSerializer JSON_RPC_ERROR_SERIALIZER = new JSONRPCErrorSerializer();

    public JSONRPCVoidResponseSerializer() {
        this(null);
    }

    public JSONRPCVoidResponseSerializer(Class<JSONRPCResponse<Void>> vc) {
        super(vc);
    }

    @Override
    public void serialize(JSONRPCResponse<Void> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("jsonrpc", value.getJsonrpc());
        gen.writeObjectField("id", value.getId());
        if (value.getError() != null) {
            gen.writeFieldName("error");
            JSON_RPC_ERROR_SERIALIZER.serialize(value.getError(), gen, provider);
        } else {
            gen.writeNullField("result");
        }
        gen.writeEndObject();
    }
}
