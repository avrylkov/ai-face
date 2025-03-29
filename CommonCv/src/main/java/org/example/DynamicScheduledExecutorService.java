package org.example;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DynamicScheduledExecutorService extends ScheduledThreadPoolExecutor {

    public DynamicScheduledExecutorService(int corePoolSize) {
        super(corePoolSize);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        setPoolSiz();
        return super.schedule(command, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@NotNull Runnable command, long initialDelay, long period, @NotNull TimeUnit unit) {
        setPoolSiz();
        return super.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    private void setPoolSiz() {
        if (getActiveCount() == getCorePoolSize()) {
            setCorePoolSize(getCorePoolSize() + 1);
        }
    }

}
