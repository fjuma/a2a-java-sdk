package io.a2a.server.apps;

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
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class A2AServerResource {

    @Inject
    JSONRPCHandler jsonRpcHandler;

    /**
     * Handles incoming POST requests to the main A2A endpoint. Dispatches the
     * request to the appropriate JSON-RPC handler method and returns the response.
     *
     * @param request the JSON-RPC request
     * @return the JSON-RPC response which may be an error response
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JSONRPCResponse<?> handleRequests(JSONRPCRequest<?> request) {
        if (request instanceof SendStreamingMessageRequest || request instanceof TaskResubscriptionRequest) {
            // TODO process streaming request
            return null;
        } else {
            return processNonStreamingRequest(request);
        }
    }

    @GET
    @Path("/.well-known/agent.json")
    @Produces(MediaType.APPLICATION_JSON)
    public AgentCard getAgentCard() {
        return jsonRpcHandler.getAgentCard();
    }

    private JSONRPCResponse<?> processNonStreamingRequest(JSONRPCRequest<?> request) {
        if (request instanceof GetTaskRequest) {
            return jsonRpcHandler.onGetTask((GetTaskRequest) request);
        } else if (request instanceof CancelTaskRequest) {
            return jsonRpcHandler.onCancelTask((CancelTaskRequest) request);
        } else if (request instanceof SetTaskPushNotificationRequest) {
            return jsonRpcHandler.setPushNotification((SetTaskPushNotificationRequest) request);
        } else if (request instanceof GetTaskPushNotificationRequest) {
            return jsonRpcHandler.getPushNotification((GetTaskPushNotificationRequest) request);
        } else if (request instanceof SendMessageRequest) {
            return jsonRpcHandler.onMessageSend((SendMessageRequest) request);
        } else {
            return generateErrorResponse(request, new UnsupportedOperationError());
        }
    }

    private JSONRPCResponse<?> generateErrorResponse(JSONRPCRequest<?> request, JSONRPCError error) {
        return new JSONRPCErrorResponse(request.getId(), error);
    }
}

