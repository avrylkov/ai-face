package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.face.speech.SpeechFaceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;


import java.util.concurrent.TimeUnit;

import static org.example.CommonUtils.resourceToUrl;
import static org.example.face.speech.SpeechFaceController.FxmlBeanController;

@SpringBootApplication(scanBasePackages = "org.example.service")
@EnableFeignClients(basePackages = "org.example.client")
@EnableScheduling
@Configuration()
public class AppSpeechFaceFxLLM extends Application {

    private static final Logger log = LoggerFactory.getLogger(AppSpeechFaceFxLLM.class);
    private static String[] args;
    private ConfigurableApplicationContext applicationContext;
    private SpeechFaceController controller;

    @Override
    public void start(Stage stage) throws Exception {
        applicationContext = SpringApplication.run(AppSpeechFaceFxLLM.class, AppSpeechFaceFxLLM.args);
        //
        FXMLLoader fxmlLoader = new FXMLLoader(resourceToUrl(AppSpeechFaceFxLLM.class, "SpeechFace.fxml"));
        Parent root = fxmlLoader.load();
        controller = fxmlLoader.getController();
        //
        applicationContext.getBeanFactory().registerSingleton(FxmlBeanController, controller);
        //stage.setOnCloseRequest(e -> controller.shutdown());
        stage.setTitle("AI Face agent");
        stage.setScene(new Scene(root, 1000, 700));
        stage.show();
    }

    @Override
    public void init() throws Exception {
        //applicationContext = SpringApplication.run(AppSpeechFaceFxLLM.class, AppSpeechFaceFxLLM.args);
    }

    @Override
    public void stop() throws Exception {
        controller.stopSchedule();
        TimeUnit.MILLISECONDS.sleep(2000);
        applicationContext.close();
        controller.close();
    }

    public static void main(String[] args) {
        AppSpeechFaceFxLLM.args = args;
        Application.launch(AppSpeechFaceFxLLM.class, args);
        //SpringApplication.run(AppSpeechFaceFxLLM.class, args);
    }

}
