package io.a2a.server.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InMemoryQueueManager implements QueueManager {


    private final Map<String, EventQueue> queues = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void add(String taskId, EventQueue queue) {
        synchronized (queues) {
            if (queues.containsKey(taskId)) {
                throw new TaskQueueExistsException();
            }
            queues.put(taskId, queue);
        }
    }

    @Override
    public EventQueue get(String taskId) {
        return queues.get(taskId);
    }

    @Override
    public EventQueue tap(String taskId) {
        synchronized (taskId) {
            EventQueue queue = queues.get(taskId);
            if (queue == null) {
                return queue;
            }
            return queue.tap();
        }
    }

    @Override
    public void close(String taskId) {
        synchronized (queues) {
            EventQueue existing = queues.remove(taskId);
            if (existing == null) {
                throw new NoTaskQueueException();
            }
        }
    }

    @Override
    public EventQueue createOrTap(String taskId) {
        synchronized (queues) {
            EventQueue queue = queues.get(taskId);
            if (queue != null) {
                return queue.tap();
            }
            queue = new EventQueue();
            queues.put(taskId, queue);
            return queue;
        }
    }
}
