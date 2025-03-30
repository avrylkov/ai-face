package org.example;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.example.ClassifierService.MODEL_FILE;

public class AppClassifier extends Application {

    private static Logger log = LogManager.getLogger(AppClassifier.class);

    private final OpenCvUtils openCvUtils = new OpenCvUtils();
    private final ClassifierService classifierService = new ClassifierService(openCvUtils);
    private final ScheduledExecutorService scheduler = new DynamicScheduledExecutorService(1);
    private VideoCapture videoCapture;
    private AtomicBoolean cameraReady = new AtomicBoolean(false);
    private Mat blackRec;

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) {
        if (!classifierService.loadModelFromJson()) {
            log.error("Не удалось загрузить модель");
            return;
        }

        openCvUtils.loadCascadeClassifier();
        videoCapture = new VideoCapture(0); // The number is the ID of the camera
        blackRec = OpenCvUtils.loadImage("./black-rec.jpg");

        scheduler.schedule(() -> cameraReady.set(true), 5, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::scheduleSaveModel, 5, 3, TimeUnit.SECONDS);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        ImageView imageView = new ImageView();
        root.setCenter(imageView);

        Button btnTakePhoto = new Button("Стоп");
        btnTakePhoto.setOnAction(this::handleStopModel);
        FlowPane bottom = new FlowPane();
        bottom.getChildren().add(btnTakePhoto);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setOnCloseRequest(this::onCloseStage);
        stage.show();
        new AnimationTimer() {
            @Override
            public void handle(long l) {
                imageView.setImage(getCaptureWithFaceDetection());
            }
        }.start();

    }

    private void handleStopModel(ActionEvent actionEvent) {
        Button btn = (Button) actionEvent.getSource();
        if (cameraReady.get()) {
            cameraReady.set(false);
            btn.setText("Старт");
        } else {
            cameraReady.set(true);
            btn.setText("Стоп");
        }
    }

    private void scheduleSaveModel() {
        log.info("Scheduling save model...");
        saveModel();
    }

    private void onCloseStage(WindowEvent windowEvent) {
        log.info("Closing...");
        saveModel();
        cameraReady.set(false);
        scheduler.shutdown();
    }

    private void saveModel() {
        if (!cameraReady.get()) {
            log.info("Camera not ready");
            return;
        }
        ScheduledFuture<?> schedule = scheduler.schedule(() -> cameraReady.set(false), 0, TimeUnit.SECONDS);
        try {
            schedule.get();
            //classifierService.clearSameImage();
            classifierService.saveModelToJson();
            cameraReady.set(true);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Image getCaptureWithFaceDetection() {
        Mat inputImage = new Mat();
        if (videoCapture.isOpened()) {
            videoCapture.read(inputImage);
        } else {
            return Utils.mat2Img(blackRec);
        }
        Mat imageGray = new Mat();
        Imgproc.cvtColor(inputImage, imageGray, Imgproc.COLOR_BGR2GRAY);
        if (!cameraReady.get()) {
            return Utils.mat2Img(imageGray);
        }
        MatOfRect matOfRect = openCvUtils.detectFace(imageGray);
        Rect[] facesArray = matOfRect.toArray();
        for (Rect face : facesArray) {
            Rect f = new Rect(face.x - 10, face.y - 10, face.width + 10, face.height + 10);
            final Mat faceRect = imageGray.submat(f);
            classifierService.process(faceRect);
            Imgproc.rectangle(imageGray, f.tl(), f.br(), new Scalar(0, 0, 255), 2);
            break;
        }

        return Utils.mat2Img(imageGray);
    }

}
