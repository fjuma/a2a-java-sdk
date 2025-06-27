package io.a2a.server.events;

import java.util.ArrayList;
import java.util.List;

public abstract class EnhancedRunnable implements Runnable {
    private volatile Throwable error;
    private final List<DoneCallback> doneCallbacks = new ArrayList<>();

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public void addDoneCallback(DoneCallback doneCallback) {
        synchronized (doneCallbacks) {
            doneCallbacks.add(doneCallback);
        }
    }

    public void invokeDoneCallbacks() {
        synchronized (doneCallbacks) {
            for (DoneCallback doneCallback : doneCallbacks) {
                doneCallback.done(this);
            }
        }
    }

    public interface DoneCallback {
        void done(EnhancedRunnable agentRunnable);
    }
}
