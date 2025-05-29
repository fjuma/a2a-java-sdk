package io.a2a.server.apps;

import java.util.concurrent.Flow;

import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.spec.AgentCard;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.GetTaskPushNotificationRequest;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.JSONRPCRequest;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationRequest;
import io.a2a.spec.TaskResubscriptionRequest;
import io.a2a.spec.UnsupportedOperationError;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

@Path("/")
public class A2AServerResource {

    @Inject
    JSONRPCHandler jsonRpcHandler;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.SERVER_SENT_EVENTS})
    public Response handleRequests(JSONRPCRequest<?> request, @Context SseEventSink sseEventSink, @Context Sse sse) {
        if (request instanceof SendStreamingMessageRequest || request instanceof TaskResubscriptionRequest) {
            return processStreamingRequest(request, sseEventSink, sse);
        }
        return processNonStreamingRequest(request);
    }

    /**
     * Handles incoming POST requests to the main A2A endpoint. Dispatches the
     * request to the appropriate JSON-RPC handler method and returns the response.
     *
     * @param request the JSON-RPC request
     * @return the JSON-RPC response which may be an error response
     */
    /*@POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleNonStreamingRequests(JSONRPCRequest<?> request) {
        if (request instanceof SendStreamingMessageRequest || request instanceof TaskResubscriptionRequest) {
            JSONRPCResponse<?> response = generateErrorResponse(request, new UnsupportedOperationError());
            return Response.ok(response).type(MediaType.APPLICATION_JSON).build();
        }
        return processNonStreamingRequest(request);
    }*/

    /**
     * Handles incoming POST requests to the main A2A endpoint that involve Server-Sent Events (SSE).
     * Dispatches the request to the appropriate JSON-RPC handler method and returns the response.
     */
    /*@POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Response handleStreamingRequests(JSONRPCRequest<?> request, @Context SseEventSink sseEventSink, @Context Sse sse) {
        if (!(request instanceof SendStreamingMessageRequest) && !(request instanceof TaskResubscriptionRequest)) {
            JSONRPCResponse<?> response = generateErrorResponse(request, new UnsupportedOperationError());
            return Response.ok(response).type(MediaType.APPLICATION_JSON).build();
        }
        return processStreamingRequest(request, sseEventSink, sse);
    }*/

    /**
     * Handles incoming GET requests to the agent card endpoint.
     * Returns the agent card in JSON format.
     *
     * @return the agent card
     */
    @GET
    @Path("/.well-known/agent.json")
    @Produces(MediaType.APPLICATION_JSON)
    public AgentCard getAgentCard() {
        return jsonRpcHandler.getAgentCard();
    }

    private Response processNonStreamingRequest(JSONRPCRequest<?> request) {
        JSONRPCResponse<?> response;
        if (request instanceof GetTaskRequest) {
            response = jsonRpcHandler.onGetTask((GetTaskRequest) request);
        } else if (request instanceof CancelTaskRequest) {
            response = jsonRpcHandler.onCancelTask((CancelTaskRequest) request);
        } else if (request instanceof SetTaskPushNotificationRequest) {
            response = jsonRpcHandler.setPushNotification((SetTaskPushNotificationRequest) request);
        } else if (request instanceof GetTaskPushNotificationRequest) {
            response = jsonRpcHandler.getPushNotification((GetTaskPushNotificationRequest) request);
        } else if (request instanceof SendMessageRequest) {
            response = jsonRpcHandler.onMessageSend((SendMessageRequest) request);
        } else {
            response = generateErrorResponse(request, new UnsupportedOperationError());
        }
        return Response.ok(response).type(MediaType.APPLICATION_JSON).build();
    }

    private Response processStreamingRequest(JSONRPCRequest<?> request, SseEventSink sseEventSink, Sse sse) {
        Flow.Publisher<? extends JSONRPCResponse<?>> publisher;
        if (request instanceof SendStreamingMessageRequest) {
            publisher = jsonRpcHandler.onMessageSendStream((SendStreamingMessageRequest) request);
            return handleStreamingResponse(publisher, sseEventSink, sse);
        } else if (request instanceof TaskResubscriptionRequest) {
            publisher = jsonRpcHandler.onResubscribeToTask((TaskResubscriptionRequest) request);
            return handleStreamingResponse(publisher, sseEventSink, sse);
        } else {
            JSONRPCResponse<?> response = generateErrorResponse(request, new UnsupportedOperationError());
            return Response.ok(response).type(MediaType.APPLICATION_JSON).build();
        }
    }

    private Response handleStreamingResponse(Flow.Publisher<? extends JSONRPCResponse<?>> publisher, SseEventSink sseEventSink, Sse sse) {
        publisher.subscribe(new Flow.Subscriber<JSONRPCResponse<?>>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(JSONRPCResponse<?> item) {
                sseEventSink.send(sse.newEventBuilder()
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(item)
                        .build());
            }

            @Override
            public void onError(Throwable throwable) {
                // TODO
                sseEventSink.close();
            }

            @Override
            public void onComplete() {
                sseEventSink.close();
            }
        });

        return Response.ok().type(MediaType.SERVER_SENT_EVENTS).build();
    }

    private JSONRPCResponse<?> generateErrorResponse(JSONRPCRequest<?> request, JSONRPCError error) {
        return new JSONRPCErrorResponse(request.getId(), error);
    }
}

