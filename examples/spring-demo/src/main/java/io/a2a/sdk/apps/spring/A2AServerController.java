package io.a2a.sdk.apps.spring;

import jakarta.annotation.Resource;
import java.util.concurrent.Flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.a2a.server.ExtendedAgentCard;
import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.spec.AgentCard;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.IdJsonMappingException;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidParamsJsonMappingException;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONErrorResponse;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.JSONRPCRequest;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.MethodNotFoundJsonMappingException;
import io.a2a.spec.NonStreamingJSONRPCRequest;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.StreamingJSONRPCRequest;
import io.a2a.spec.TaskResubscriptionRequest;
import io.a2a.spec.UnsupportedOperationError;
import io.a2a.util.Utils;
import reactor.core.publisher.Flux;

/**
 * Spring Boot REST controller for A2A (Agent2Agent) protocol endpoints.
 * Provides endpoints for JSON-RPC communication and agent card retrieval.
 */
@RestController
@RequestMapping("/")
public class A2AServerController {

    @Resource
    private JSONRPCHandler jsonRpcHandler;

    @Autowired(required = false)
    @ExtendedAgentCard
    private AgentCard extendedAgentCard;
    
    // Hook for testing to wait until streaming is subscribed
    private static volatile Runnable streamingIsSubscribedRunnable;

    /**
     * Handles incoming POST requests to the main A2A endpoint.
     * Dispatches the request to the appropriate JSON-RPC handler method.
     *
     * @param requestBody the JSON-RPC request body as string
     * @return the JSON-RPC response
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JSONRPCResponse<?>> handleNonStreamingRequests(@RequestBody String requestBody) {
        try {
            NonStreamingJSONRPCRequest<?> request = Utils.OBJECT_MAPPER.readValue(requestBody, NonStreamingJSONRPCRequest.class);
            JSONRPCResponse<?> response = processNonStreamingRequest(request);
            return ResponseEntity.ok(response);
        } catch (JsonProcessingException e) {
            JSONRPCErrorResponse error = handleError(e);
            return ResponseEntity.ok(error);
        } catch (Throwable t) {
            JSONRPCErrorResponse error = new JSONRPCErrorResponse(new io.a2a.spec.InternalError(t.getMessage()));
            return ResponseEntity.ok(error);
        }
    }

    /**
     * Handles incoming POST requests for streaming operations using Server-Sent Events (SSE).
     * Dispatches the request to the appropriate JSON-RPC handler method.
     *
     * @param requestBody the JSON-RPC request body as string
     * @return a Flux of ServerSentEvent containing the streaming responses
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<JSONRPCResponse<?>>> handleStreamingRequests(@RequestBody String requestBody) {
        try {
            StreamingJSONRPCRequest<?> request = Utils.OBJECT_MAPPER.readValue(requestBody, StreamingJSONRPCRequest.class);
            return processStreamingRequest(request);
        } catch (JsonProcessingException e) {
            JSONRPCErrorResponse error = handleError(e);
            return Flux.just(ServerSentEvent.<JSONRPCResponse<?>>builder()
                    .data(error)
                    .build());
        } catch (Throwable t) {
            JSONRPCErrorResponse error = new JSONRPCErrorResponse(new io.a2a.spec.InternalError(t.getMessage()));
            return Flux.just(ServerSentEvent.<JSONRPCResponse<?>>builder()
                    .data(error)
                    .build());
        }
    }

    /**
     * Handles incoming GET requests to the agent card endpoint.
     * Returns the agent card in JSON format.
     *
     * @return the agent card
     */
    @GetMapping(path = "/.well-known/agent.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public AgentCard getAgentCard() {
        return jsonRpcHandler.getAgentCard();
    }

    /**
     * Handles incoming GET requests to the authenticated extended agent card endpoint.
     * Returns the extended agent card in JSON format.
     *
     * @return the authenticated extended agent card
     */
    @GetMapping(path = "/agent/authenticatedExtendedCard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAuthenticatedExtendedAgentCard() {
        // TODO: Add authentication for this endpoint
        // https://github.com/a2aproject/a2a-java/issues/77
        if (!jsonRpcHandler.getAgentCard().supportsAuthenticatedExtendedCard()) {
            JSONErrorResponse errorResponse = new JSONErrorResponse("Extended agent card not supported or not enabled.");
            return ResponseEntity.status(404).body(errorResponse);
        }
        if (extendedAgentCard == null) {
            JSONErrorResponse errorResponse = new JSONErrorResponse("Authenticated extended agent card is supported but not configured on the server.");
            return ResponseEntity.status(404).body(errorResponse);
        }
        return ResponseEntity.ok(extendedAgentCard);
    }

    private JSONRPCResponse<?> processNonStreamingRequest(NonStreamingJSONRPCRequest<?> request) {
        if (request instanceof GetTaskRequest) {
            return jsonRpcHandler.onGetTask((GetTaskRequest) request);
        } else if (request instanceof CancelTaskRequest) {
            return jsonRpcHandler.onCancelTask((CancelTaskRequest) request);
        } else if (request instanceof SetTaskPushNotificationConfigRequest) {
            return jsonRpcHandler.setPushNotification((SetTaskPushNotificationConfigRequest) request);
        } else if (request instanceof GetTaskPushNotificationConfigRequest) {
            return jsonRpcHandler.getPushNotification((GetTaskPushNotificationConfigRequest) request);
        } else if (request instanceof SendMessageRequest) {
            return jsonRpcHandler.onMessageSend((SendMessageRequest) request);
        } else {
            return generateErrorResponse(request, new UnsupportedOperationError());
        }
    }

    private Flux<ServerSentEvent<JSONRPCResponse<?>>> processStreamingRequest(StreamingJSONRPCRequest<?> request) {
        Flow.Publisher<? extends JSONRPCResponse<?>> publisher;
        if (request instanceof SendStreamingMessageRequest) {
            publisher = jsonRpcHandler.onMessageSendStream((SendStreamingMessageRequest) request);
        } else if (request instanceof TaskResubscriptionRequest) {
            publisher = jsonRpcHandler.onResubscribeToTask((TaskResubscriptionRequest) request);
        } else {
            return Flux.just(ServerSentEvent.<JSONRPCResponse<?>>builder()
                    .data(generateErrorResponse(request, new UnsupportedOperationError()))
                    .build());
        }

        return Flux.create(sink -> {
            publisher.subscribe(new Flow.Subscriber<JSONRPCResponse<?>>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                    // Notify tests that we are subscribed
                    Runnable runnable = streamingIsSubscribedRunnable;
                    if (runnable != null) {
                        runnable.run();
                    }
                }

                @Override
                public void onNext(JSONRPCResponse<?> item) {
                    sink.next(ServerSentEvent.<JSONRPCResponse<?>>builder()
                            .data(item)
                            .build());
                }

                @Override
                public void onError(Throwable throwable) {
                    sink.error(throwable);
                }

                @Override
                public void onComplete() {
                    sink.complete();
                }
            });
        });
    }

    private JSONRPCErrorResponse handleError(JsonProcessingException exception) {
        Object id = null;
        JSONRPCError jsonRpcError = null;
        if (exception.getCause() instanceof JsonParseException) {
            jsonRpcError = new JSONParseError();
        } else if (exception instanceof com.fasterxml.jackson.core.io.JsonEOFException) {
            jsonRpcError = new JSONParseError(exception.getMessage());
        } else if (exception instanceof MethodNotFoundJsonMappingException err) {
            id = err.getId();
            jsonRpcError = new MethodNotFoundError();
        } else if (exception instanceof InvalidParamsJsonMappingException err) {
            id = err.getId();
            jsonRpcError = new InvalidParamsError();
        } else if (exception instanceof IdJsonMappingException err) {
            id = err.getId();
            jsonRpcError = new InvalidRequestError();
        } else {
            jsonRpcError = new InvalidRequestError();
        }
        return new JSONRPCErrorResponse(id, jsonRpcError);
    }

    private JSONRPCResponse<?> generateErrorResponse(JSONRPCRequest<?> request, JSONRPCError error) {
        return new JSONRPCErrorResponse(request.getId(), error);
    }
    
} 
