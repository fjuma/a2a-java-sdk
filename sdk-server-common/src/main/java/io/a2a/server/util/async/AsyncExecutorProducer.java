package io.a2a.server.util.async;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
public class AsyncExecutorProducer {

    private ExecutorService executor;

    @PostConstruct
    public void init() {
        executor = Executors.newCachedThreadPool();
    }

    @PreDestroy
    public void close() {
        executor.shutdown();
    }

    @Produces
    @Internal
    public Executor produce() {
        return executor;
    }

}
