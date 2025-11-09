package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class AwaitTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AwaitTask.class);

    private Runnable task;
    private long waitBefore;
    private final String taskName;
    private Thread thread;

    public AwaitTask(Runnable task, long waitBefore, String taskName) {
        this.task = task;
        this.waitBefore = waitBefore;
        this.taskName = taskName;
    }

    @Override
    public void run() {
        try {
            TimeUnit.MILLISECONDS.sleep(waitBefore);
            log.info("Запуск задачи: {}", taskName);
            task.run();
        } catch (InterruptedException e) {
            log.error("Задача была прервана: {}", taskName, e);
            // Обработка прерывания потока
            Thread.currentThread().interrupt(); // Восстанавливаем флаг прерывания
        } catch (RuntimeException e) {
            log.error("Ошибка в задаче {}", taskName, e);
        }
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public void interrupt() {
        if (thread != null) {
            thread.interrupt();
        }
    }

}
