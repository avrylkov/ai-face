package org.example.service.config;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import org.example.face.speech.SpeechFaceController;
import org.example.service.GlobalContext;
import org.example.service.tool.ToolFeedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;


import static org.example.face.speech.SpeechFaceController.FxmlBeanController;

@Configuration
public class FaceConfig {

    private static final Logger log = LoggerFactory.getLogger(FaceConfig.class.getName());

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    

    @Bean
    public Consumer<String> toolConsumer() {
        return message -> {
            if (!GlobalContext.getContext().containsBean(FxmlBeanController)) {
                return;
            }
            log.info("Получено сообщение от инструмента: {}", message);
            SpeechFaceController faceController = GlobalContext.getContext().getBean(FxmlBeanController, SpeechFaceController.class);
            // Отправка сообщения в JavaFX UI
            String currentTime = LocalTime.now().format(TIME_FORMATTER);
            Platform.runLater(() -> {
                //int size = faceController.toolFeedBack.getItems().size();
                faceController.toolFeedBack.getItems().add(0, String.format("[%s]: %s",  currentTime, message));
            });
        };
    }

    @Bean
    public Runnable clearItem() {
        return () -> {
            if (!GlobalContext.getContext().containsBean(FxmlBeanController)) {
                return;
            }
            SpeechFaceController faceController = GlobalContext.getContext().getBean(FxmlBeanController, SpeechFaceController.class);
            Platform.runLater(() -> {
                ObservableList<String> items = faceController.toolFeedBack.getItems();
                if (items.size() > 1) {
                    faceController.toolFeedBack.getItems().remove(items.size() - 1);
                }
            });
        };
    }

    @Bean
    public ToolFeedback toolFeedback(Consumer<String> toolConsumer,
                                     Runnable clearItem) {
        return new ToolFeedback(toolConsumer, clearItem);
    }

}