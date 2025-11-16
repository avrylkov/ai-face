package org.example.face;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.example.opencv.OpenCVImage;
import org.example.opencv.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class AppRetinaFaceDetection extends Application {

    private static final Logger log = LoggerFactory.getLogger(AppRetinaFaceDetection.class);

    private final VideoFace2Detection videoFace2Detection = new VideoFace2Detection();

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        ImageView imageView = new ImageView();
        root.setCenter(imageView);

        Consumer<OpenCVImage> applyImage2View = (OpenCVImage image) -> {
            Utils.apply2view(imageView, image);
        };

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setOnCloseRequest(this::onCloseStage);

        videoFace2Detection.start(applyImage2View);

        stage.show();
    }

    private void onCloseStage(WindowEvent windowEvent) {
        videoFace2Detection.close();
    }


}
