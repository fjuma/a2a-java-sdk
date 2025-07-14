package io.a2a.server.apps.quarkus;

import jakarta.inject.Inject;

import io.a2a.server.apps.common.AbstractA2AServerTest;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.tasks.TaskStore;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusA2AServerTest extends AbstractA2AServerTest {

    public QuarkusA2AServerTest() {
        super(8081);
    }

    @Override
    protected void setStreamingSubscribedRunnable(Runnable runnable) {
        A2AServerRoutes.setStreamingMultiSseSupportSubscribedRunnable(runnable);
    }
}
