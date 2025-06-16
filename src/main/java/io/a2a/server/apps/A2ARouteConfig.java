package io.a2a.server.apps;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

@ApplicationScoped
public class A2ARouteConfig {

    public void init(@Observes Router router) {
        router.route().handler(BodyHandler.create());
    }
}