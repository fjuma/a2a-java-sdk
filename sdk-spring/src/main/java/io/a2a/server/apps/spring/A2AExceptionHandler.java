package io.a2a.server.apps.spring;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.a2a.spec.IdJsonMappingException;
import io.a2a.spec.InvalidParamsJsonMappingException;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.MethodNotFoundJsonMappingException;

/**
 * Global exception handler for A2A Spring adapter.
 * Handles JSON parsing and mapping exceptions and converts them to appropriate JSON-RPC error responses.
 */
@ControllerAdvice
public class A2AExceptionHandler {

    /**
     * Handles JsonParseException and converts it to a JSON-RPC parse error response.
     *
     * @param exception the JsonParseException
     * @return ResponseEntity containing the JSON-RPC error response
     */
    @ExceptionHandler(JsonParseException.class)
    public ResponseEntity<JSONRPCErrorResponse> handleJsonParseException(JsonParseException exception) {
        // Parse error, not possible to determine the request id
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new JSONRPCErrorResponse(new JSONParseError()));
    }

    /**
     * Handles JsonMappingException and converts it to appropriate JSON-RPC error responses.
     *
     * @param exception the JsonMappingException
     * @return ResponseEntity containing the JSON-RPC error response
     */
    @ExceptionHandler(JsonMappingException.class)
    public ResponseEntity<JSONRPCErrorResponse> handleJsonMappingException(JsonMappingException exception) {
        if (exception.getCause() instanceof JsonParseException) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONRPCErrorResponse(new JSONParseError()));
        } else if (exception instanceof MethodNotFoundJsonMappingException) {
            Object id = ((MethodNotFoundJsonMappingException) exception).getId();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONRPCErrorResponse(id, new MethodNotFoundError()));
        } else if (exception instanceof InvalidParamsJsonMappingException) {
            Object id = ((InvalidParamsJsonMappingException) exception).getId();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONRPCErrorResponse(id, new InvalidRequestError()));
        } else if (exception instanceof IdJsonMappingException) {
            Object id = ((IdJsonMappingException) exception).getId();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONRPCErrorResponse(id, new InvalidRequestError()));
        }
        // Not possible to determine the request id
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new JSONRPCErrorResponse(new InvalidRequestError()));
    }
} 