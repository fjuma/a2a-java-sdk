package io.a2a.server.apps.jakarta;

import jakarta.inject.Inject;

import io.a2a.server.apps.common.AbstractA2AServerTest;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.tasks.TaskStore;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class JakartaA2AServerTest extends AbstractA2AServerTest {
    @Inject
    TaskStore taskStore;

    @Inject
    InMemoryQueueManager queueManager;

    @Override
    protected TaskStore getTaskStore() {
        return taskStore;
    }

    @Override
    protected InMemoryQueueManager getQueueManager() {
        return queueManager;
    }

    @Override
    protected void setStreamingSubscribedRunnable(Runnable runnable) {
        A2AServerResource.setStreamingIsSubscribedRunnable(runnable);
    }
}
