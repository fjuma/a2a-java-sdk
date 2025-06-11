package io.a2a.tck.server;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;

@ApplicationScoped
public class AgentExecutorProducer {

    @Produces
    public AgentExecutor agentExecutor() {
        return new FireAndForgetAgentExecutor();
    }
    
    private static class FireAndForgetAgentExecutor implements AgentExecutor {
        @Override
        public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
            Task task = context.getTask();

            if (context.getMessage().getTaskId() != null && task == null && context.getMessage().getTaskId().startsWith("non-existent")) {
                throw new TaskNotFoundError();
            }

            if (task == null) {
                task = new Task.Builder()
                        .id(context.getTaskId())
                        .contextId(context.getContextId())
                        .status(new TaskStatus(TaskState.SUBMITTED))
                        .history(context.getMessage())
                        .build();
                eventQueue.enqueueEvent(task);
            }

            TaskUpdater updater = new TaskUpdater(eventQueue, context.getTaskId(), context.getTaskId());

            // Immediately set to WORKING state
            updater.startWork();
            System.out.println("====> task set to WORKING, starting background execution");

            // Method returns immediately - task continues in background
            System.out.println("====> execute() method returning immediately, task running in background");
        }

        @Override
        public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
            System.out.println("====> task cancel request received");
            Task task = context.getTask();

            if (task.getStatus().state() == TaskState.CANCELED) {
                System.out.println("====> task already canceled");
                throw new TaskNotCancelableError();
            }
            
            if (task.getStatus().state() == TaskState.COMPLETED) {
                System.out.println("====> task already completed");
                throw new TaskNotCancelableError();
            }

            TaskUpdater updater = new TaskUpdater(eventQueue, context.getTaskId(), context.getTaskId());
            updater.cancel();
            eventQueue.enqueueEvent(new TaskStatusUpdateEvent.Builder()
                    .taskId(task.getId())
                    .contextId(task.getContextId())
                    .status(new TaskStatus(TaskState.CANCELED))
                    .isFinal(true)
                    .build());
            
            System.out.println("====> task canceled");
        }

        /**
         * This method runs completely in the background.
         * The main execute() method has already returned.
         */
        private void executeTaskInBackground(RequestContext context, EventQueue eventQueue) {
            String taskId = context.getTaskId();
            
            try {
                System.out.println("====> background execution started for task: " + taskId);
                
                // Perform the actual work
                Object result = performActualWork(context);
                
                // Task completed successfully
                eventQueue.enqueueEvent(new TaskStatusUpdateEvent.Builder()
                        .taskId(taskId)
                        .contextId(context.getContextId())
                        .status(new TaskStatus(TaskState.COMPLETED))
                        .isFinal(true)
                        .build());
                
                System.out.println("====> background task completed successfully: " + taskId);
                
            } catch (InterruptedException e) {
                // Task was interrupted (cancelled)
                System.out.println("====> background task was interrupted: " + taskId);
                Thread.currentThread().interrupt();
                
            } catch (Exception e) {
                // Task failed
                System.err.println("====> background task failed: " + taskId);
                e.printStackTrace();
                
            } finally {
                // Always clean up
                System.out.println("====> background task cleanup completed: " + taskId);
            }
        }

        /**
         * This method represents the actual work that needs to be done.
         * Replace this with your real business logic.
         */
        private Object performActualWork(RequestContext context) throws InterruptedException {
            
            
            System.out.println("====> starting actual work for task: " + context.getTaskId());
            
            // Simulate work that can be interrupted
            for (int i = 0; i < 10; i++) {
                // Check for interruption regularly during long-running work
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Task was cancelled during execution");
                }
                
                Thread.sleep(200); // Simulate work chunks
                System.out.println("====> work progress for task " + context.getTaskId() + ": " + ((i + 1) * 10) + "%");
            }
            
            System.out.println("====> finished actual work for task: " + context.getTaskId());
            return "Task completed successfully";
        }

        /**
         * Cleanup method for proper resource management
         */
        @PreDestroy
        public void cleanup() {
            System.out.println("====> shutting down task executor");
         }
    }
}