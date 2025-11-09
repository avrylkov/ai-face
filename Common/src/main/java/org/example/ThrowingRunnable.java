package org.example;

@FunctionalInterface
public interface ThrowingRunnable {

    void run() throws InterruptedException;

}
