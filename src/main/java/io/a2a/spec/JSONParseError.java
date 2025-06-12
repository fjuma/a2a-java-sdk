package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JSONParseError extends JSONRPCError implements A2AError {

    public JSONParseError() {
        this(null, null, null);
    }

    public JSONParseError(String message) {
        this(null, message, null);
    }

    @JsonCreator
    public JSONParseError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, -32700),
                defaultIfNull(message, "Invalid JSON payload"),
                data);
    }
}
