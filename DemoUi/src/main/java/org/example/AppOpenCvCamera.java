package org.example;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import static org.example.Utils.mat2Img;

public class AppOpenCvCamera extends Application {

    private static final Logger log = LogManager.getLogger(AppOpenCvCamera.class);

    private OpenCvUtils openCvUtils = new OpenCvUtils();
    private VideoCapture videoCapture;
    private static boolean isTakePhoto = false;
    private static int counter = 1;

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        log.info("Starting AppOpenCvCamera");
        //
        openCvUtils.loadCascadeClassifier();
        videoCapture = new VideoCapture(0); // The number is the ID of the camera
        //
        Button btnTakePhoto = new Button("Фото");
        btnTakePhoto.setOnAction(this::handleTakePhoto);
        Label label = new Label();

        FlowPane bottom = new FlowPane();
        bottom.getChildren().add(btnTakePhoto);
        bottom.getChildren().add(label);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        ImageView imageView = new ImageView();
        root.setCenter(imageView);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();
        new AnimationTimer() {
            @Override
            public void handle(long l) {
                label.setText(counter + "");
                imageView.setImage(getCaptureWithFaceDetection());
            }
        }.start();

    }

    private Image getCaptureWithFaceDetection() {
        Mat inputImage = new Mat();
        videoCapture.read(inputImage);
        Mat imageGray = new Mat();
        Imgproc.cvtColor(inputImage, imageGray, Imgproc.COLOR_BGR2GRAY);
        MatOfRect matOfRect = openCvUtils.detectFace(imageGray);
        Rect[] facesArray = matOfRect.toArray();
        for (Rect face : facesArray) {
            Rect f = new Rect(face.x-10, face.y-10, face.width+10, face.height+10);
            final Mat faceRect = imageGray.submat(f);
            Imgproc.rectangle(imageGray, f.tl(), f.br(), new Scalar(0, 0, 255), 3);
            if (isTakePhoto) {
                isTakePhoto = !isTakePhoto;
                OpenCvUtils.saveImage(faceRect, String.format("./face%s.jpg", counter));
                counter++;
            }
        }

        return mat2Img(imageGray);
    }

    private void handleTakePhoto(ActionEvent actionEvent) {
        isTakePhoto = !isTakePhoto;
    }

}
