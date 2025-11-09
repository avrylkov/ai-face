package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class RepeatingTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(RepeatingTask.class);

    private ThrowingRunnable task;
    private long delay;
    private long initialDelay;
    private final String taskName;
    private Thread thread;
    //private boolean isInterrupted = false;

    public RepeatingTask(ThrowingRunnable task, long initialDelay, long delay, String taskName) {
        this.task = task;
        this.delay = delay;
        this.taskName = taskName;
        this.initialDelay = initialDelay;
    }

    @Override
    public void run() {
        log.debug("Запуск задачи: {}", taskName);
        if (initialDelay > 0 ) {
            try {
                TimeUnit.MILLISECONDS.sleep(initialDelay);
            }  catch (InterruptedException e) {
                log.warn("Задача была прервана: {}", taskName, e);
                return;
            }
        }
        while (!Thread.currentThread().isInterrupted() /*&& !isInterrupted*/) {
            try {
                task.run();
                TimeUnit.MILLISECONDS.sleep(delay);
            } catch (InterruptedException | RejectedExecutionException e) {
                log.warn("Задача была прервана: {}", taskName, e);
                // Обработка прерывания потока
                //Thread.currentThread().interrupt(); // Восстанавливаем флаг прерывания
                break;
            } catch (RuntimeException e) {
                log.error("Ошибка в задаче {}", taskName, e);
            }
        }
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public void interrupt() {
        if (thread != null) {
            thread.interrupt();
            //isInterrupted = true;
        }
    }

}
