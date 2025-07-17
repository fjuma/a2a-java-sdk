package io.a2a.server.apps.quarkus;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.JsonNode;
import io.a2a.server.ExtendedAgentCard;
import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.server.util.async.Internal;
import io.a2a.spec.AgentCard;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.IdJsonMappingException;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidParamsJsonMappingException;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONErrorResponse;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.JSONRPCRequest;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
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
import io.quarkus.vertx.web.Body;
import io.quarkus.vertx.web.ReactiveRoutes;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RoutingExchange;
import io.smallrye.mutiny.Multi;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class A2AServerRoutes {

    @Inject
    JSONRPCHandler jsonRpcHandler;

    @Inject
    @ExtendedAgentCard
    Instance<AgentCard> extendedAgentCard;

    // Hook so testing can wait until the MultiSseSupport is subscribed.
    // Without this we get intermittent failures
    private static volatile Runnable streamingMultiSseSupportSubscribedRunnable;

    @Inject
    @Internal
    Executor executor;

    @Route(path = "/", methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void invokeJSONRPCHandler(@Body String body, RoutingContext rc) {
        boolean streaming = false;
        JSONRPCResponse<?> nonStreamingResponse = null;
        Multi<? extends JSONRPCResponse<?>> streamingResponse = null;
        JSONRPCErrorResponse error = null;

        try {
            if (isStreamingRequest(body)) {
                streaming = true;
                StreamingJSONRPCRequest<?> request = Utils.OBJECT_MAPPER.readValue(body, StreamingJSONRPCRequest.class);
                streamingResponse = processStreamingRequest(request);
            } else {
                NonStreamingJSONRPCRequest<?> request = Utils.OBJECT_MAPPER.readValue(body, NonStreamingJSONRPCRequest.class);
                nonStreamingResponse = processNonStreamingRequest(request);
            }
        } catch (JsonProcessingException e) {
            error = handleError(e);
        } catch (Throwable t) {
            error = new JSONRPCErrorResponse(new InternalError(t.getMessage()));
        } finally {
            if (error != null) {
                rc.response()
                        .setStatusCode(200)
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(Json.encodeToBuffer(error));
            } else if (streaming) {
                final Multi<? extends JSONRPCResponse<?>> finalStreamingResponse = streamingResponse;
                executor.execute(() -> {
                        MultiSseSupport.subscribeObject(
                                finalStreamingResponse.map(i -> (Object)i), rc);
                });

            } else {
                rc.response()
                        .setStatusCode(200)
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(Json.encodeToBuffer(nonStreamingResponse));
            }
        }
    }

    private JSONRPCErrorResponse handleError(JsonProcessingException exception) {
        Object id = null;
        JSONRPCError jsonRpcError = null;
        if (exception.getCause() instanceof JsonParseException) {
            jsonRpcError = new JSONParseError();
        } else if (exception instanceof JsonEOFException) {
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

    /**
    /**
     * Handles incoming GET requests to the agent card endpoint.
     * Returns the agent card in JSON format.
     *
     * @return the agent card
     */
    @Route(path = "/.well-known/agent.json", methods = Route.HttpMethod.GET, produces = APPLICATION_JSON)
    public AgentCard getAgentCard() {
        return jsonRpcHandler.getAgentCard();
    }

    /**
     * Handles incoming GET requests to the authenticated extended agent card endpoint.
     * Returns the agent card in JSON format.
     *
     */
    @Route(path = "/agent/authenticatedExtendedCard", methods = Route.HttpMethod.GET, produces = APPLICATION_JSON)
    public void getAuthenticatedExtendedAgentCard(RoutingExchange re) {
        // TODO need to add authentication for this endpoint
        // https://github.com/a2aproject/a2a-java/issues/77
        try {
            if (! jsonRpcHandler.getAgentCard().supportsAuthenticatedExtendedCard()) {
                JSONErrorResponse errorResponse = new JSONErrorResponse("Extended agent card not supported or not enabled.");
                re.response().setStatusCode(Response.Status.NOT_FOUND.getStatusCode())
                        .end(Utils.OBJECT_MAPPER.writeValueAsString(errorResponse));
                return;
            }
            if (! extendedAgentCard.isResolvable()) {
                JSONErrorResponse errorResponse = new JSONErrorResponse("Authenticated extended agent card is supported but not configured on the server.");
                re.response().setStatusCode(Response.Status.NOT_FOUND.getStatusCode())
                        .end(Utils.OBJECT_MAPPER.writeValueAsString(errorResponse));
                return;
            }

            re.response().end(Utils.OBJECT_MAPPER.writeValueAsString(extendedAgentCard.get()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private JSONRPCResponse<?> processNonStreamingRequest(NonStreamingJSONRPCRequest<?> request) {
        if (request instanceof GetTaskRequest) {
            return jsonRpcHandler.onGetTask((GetTaskRequest) request);
        } else if (request instanceof CancelTaskRequest) {
            return jsonRpcHandler.onCancelTask((CancelTaskRequest) request);
        } else if (request instanceof SetTaskPushNotificationConfigRequest) {
            return jsonRpcHandler.setPushNotificationConfig((SetTaskPushNotificationConfigRequest) request);
        } else if (request instanceof GetTaskPushNotificationConfigRequest) {
            return jsonRpcHandler.getPushNotificationConfig((GetTaskPushNotificationConfigRequest) request);
        } else if (request instanceof SendMessageRequest) {
            return jsonRpcHandler.onMessageSend((SendMessageRequest) request);
        } else if (request instanceof ListTaskPushNotificationConfigRequest) {
            return jsonRpcHandler.listPushNotificationConfig((ListTaskPushNotificationConfigRequest) request);
        } else if (request instanceof DeleteTaskPushNotificationConfigRequest) {
            return jsonRpcHandler.deletePushNotificationConfig((DeleteTaskPushNotificationConfigRequest) request);
        } else {
            return generateErrorResponse(request, new UnsupportedOperationError());
        }
    }

    private Multi<? extends JSONRPCResponse<?>> processStreamingRequest(JSONRPCRequest<?> request) {
        Flow.Publisher<? extends JSONRPCResponse<?>> publisher;
        if (request instanceof SendStreamingMessageRequest) {
            publisher = jsonRpcHandler.onMessageSendStream((SendStreamingMessageRequest) request);
        } else if (request instanceof TaskResubscriptionRequest) {
            publisher = jsonRpcHandler.onResubscribeToTask((TaskResubscriptionRequest) request);
        } else {
            return Multi.createFrom().item(generateErrorResponse(request, new UnsupportedOperationError()));
        }
        return Multi.createFrom().publisher(publisher);
    }

    private JSONRPCResponse<?> generateErrorResponse(JSONRPCRequest<?> request, JSONRPCError error) {
        return new JSONRPCErrorResponse(request.getId(), error);
    }

    private static boolean isStreamingRequest(String requestBody) {
        try {
            JsonNode node = Utils.OBJECT_MAPPER.readTree(requestBody);
            JsonNode method = node != null ? node.get("method") : null;
            return method != null && (SendStreamingMessageRequest.METHOD.equals(method.asText())
                    || TaskResubscriptionRequest.METHOD.equals(method.asText()));
        } catch (Exception e) {
            return false;
        }
    }

    static void setStreamingMultiSseSupportSubscribedRunnable(Runnable runnable) {
        streamingMultiSseSupportSubscribedRunnable = runnable;
    }

    // Port of import io.quarkus.vertx.web.runtime.MultiSseSupport, which is considered internal API
    private static class MultiSseSupport {

        private MultiSseSupport() {
            // Avoid direct instantiation.
        }

        private static void initialize(HttpServerResponse response) {
            if (response.bytesWritten() == 0) {
                MultiMap headers = response.headers();
                if (headers.get("content-type") == null) {
                    headers.set("content-type", "text/event-stream");
                }
                response.setChunked(true);
            }
        }

        private static void onWriteDone(Flow.Subscription subscription, AsyncResult<Void> ar, RoutingContext rc) {
            if (ar.failed()) {
                rc.fail(ar.cause());
            } else {
                subscription.request(1);
            }
        }

        public static void write(Multi<Buffer> multi, RoutingContext rc) {
            HttpServerResponse response = rc.response();
            multi.subscribe().withSubscriber(new Flow.Subscriber<Buffer>() {
                Flow.Subscription upstream;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.upstream = subscription;
                    this.upstream.request(1);

                    // Notify tests that we are subscribed
                    Runnable runnable = streamingMultiSseSupportSubscribedRunnable;
                    if (runnable != null) {
                        runnable.run();
                    }
                }

                @Override
                public void onNext(Buffer item) {
                    initialize(response);
                    response.write(item, new Handler<AsyncResult<Void>>() {
                        @Override
                        public void handle(AsyncResult<Void> ar) {
                            onWriteDone(upstream, ar, rc);
                        }
                    });
                }

                @Override
                public void onError(Throwable throwable) {
                    rc.fail(throwable);
                }

                @Override
                public void onComplete() {
                    endOfStream(response);
                }
            });
        }

        public static void subscribeObject(Multi<Object> multi, RoutingContext rc) {
            AtomicLong count = new AtomicLong();
            write(multi.map(new Function<Object, Buffer>() {
                @Override
                public Buffer apply(Object o) {
                    if (o instanceof ReactiveRoutes.ServerSentEvent) {
                        ReactiveRoutes.ServerSentEvent<?> ev = (ReactiveRoutes.ServerSentEvent<?>) o;
                        long id = ev.id() != -1 ? ev.id() : count.getAndIncrement();
                        String e = ev.event() == null ? "" : "event: " + ev.event() + "\n";
                        return Buffer.buffer(e + "data: " + Json.encodeToBuffer(ev.data()) + "\nid: " + id + "\n\n");
                    } else {
                        return Buffer.buffer("data: " + Json.encodeToBuffer(o) + "\nid: " + count.getAndIncrement() + "\n\n");
                    }
                }
            }), rc);
        }

        private static void endOfStream(HttpServerResponse response) {
            if (response.bytesWritten() == 0) { // No item
                MultiMap headers = response.headers();
                if (headers.get("content-type") == null) {
                    headers.set("content-type", "text/event-stream");
                }
            }
            response.end();
        }
    }

}

