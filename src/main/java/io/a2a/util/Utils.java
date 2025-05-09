package io.a2a.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Utils {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static <T> T unmarshalFrom(String data, TypeReference<T> typeRef) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(data, typeRef);
    }

    public static <T> T defaultIfNull(T value, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
