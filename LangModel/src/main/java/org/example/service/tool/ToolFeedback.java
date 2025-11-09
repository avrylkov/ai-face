package org.example.service.tool;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class ToolFeedback {

    private final Consumer<String> toolConsumer;
    private final Runnable clearItem;

    public ToolFeedback(Consumer<String> toolConsumer,
                        Runnable clearItem) {
        this.toolConsumer = toolConsumer;
        this.clearItem = clearItem;
    }

    public void execute(String message) {
        toolConsumer.accept(message);
    }

    @Scheduled(initialDelay = 3000, fixedDelay = 10000)
    public void clearItem() {
        clearItem.run();
    }

}
