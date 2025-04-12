package org.example;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.stage.Stage;
import javafx.geometry.Orientation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.Feature2D;

import java.util.List;

import static org.example.OpenCvUtils.descriptorMatcherDraw;
import static org.example.Utils.mat2Img;

public class AppOpenCvKeyPointFile extends Application {

    private static final Logger log = LogManager.getLogger(AppOpenCvKeyPointFile.class);

    private OpenCvUtils openCvUtils = new OpenCvUtils();
    private ImageView imageView;
    private TextField textFile1;
    private TextField textFile2;
    private Label label;
    private String matcherMethod = "DM";

    private static BorderStroke borderStroke;
    static {
        StrokeType strokeType     = StrokeType.INSIDE;
        StrokeLineJoin strokeLineJoin = StrokeLineJoin.MITER;
        StrokeLineCap strokeLineCap  = StrokeLineCap.BUTT;
        double         miterLimit     = 5;
        double         dashOffset     = 0;
        List<Double> dashArray      = null;
        BorderStrokeStyle borderStrokeStyle =
                new BorderStrokeStyle(
                        strokeType,
                        strokeLineJoin,
                        strokeLineCap,
                        miterLimit,
                        dashOffset,
                        dashArray
                );

        borderStroke = new BorderStroke(
                        Color.valueOf("000000"),
                        borderStrokeStyle,
                        new CornerRadii(0),
                        new BorderWidths(1)
                );

    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) {
        log.info("Start AppOpenCvKeyPointFile");
        //
        Button btnStop = new Button("Key points");
        btnStop.setOnAction(this::handleKeyPoints);
        textFile1 = new TextField("face1");
        textFile2 = new TextField("face2");
        Slider scaling = new Slider();
        scaling.setStyle("-fx-pref-width: 300px; -fx-pref-height: 10px;");
        scaling.setMax(2.0f);
        scaling.setMin(0.1f);
        scaling.setBlockIncrement(0.1f);
        scaling.setShowTickLabels(true);
        scaling.setShowTickMarks(true);
        scaling.setMajorTickUnit(0.5);
        scaling.setMinorTickCount(1);
        scaling.setValue(openCvUtils.kDist);
        scaling.valueProperty().addListener((observable, oldValue, newValue) -> handleScalingValueChange(oldValue, newValue));

        RadioButton radioBtnDm = new RadioButton("DM");
        radioBtnDm.setSelected(true);
        matcherMethod = radioBtnDm.getText();
        RadioButton radioBtnKnn = new RadioButton("KNN");
        RadioButton radioBtnKnnHm = new RadioButton("KNN-Hm");
        RadioButton radioBtnBf = new RadioButton("BF");

        ToggleGroup groupMatcher = new ToggleGroup();
        groupMatcher.selectedToggleProperty().addListener((observable, oldValue, newValue) -> handleSelectedMatcherChange(oldValue, newValue));
        // установка группы
        radioBtnDm.setToggleGroup(groupMatcher);
        radioBtnKnn.setToggleGroup(groupMatcher);
        radioBtnKnnHm.setToggleGroup(groupMatcher);
        radioBtnBf.setToggleGroup(groupMatcher);
        //
        RadioButton radioBtnSift = new RadioButton("SIFT");
        RadioButton radioBtnOrb = new RadioButton("ORB");
        radioBtnOrb.setSelected(true);
        openCvUtils.detectorStr = radioBtnOrb.getText();

        ToggleGroup groupDetector = new ToggleGroup();
        groupDetector.selectedToggleProperty().addListener((observable, oldValue, newValue) -> handleSelectedDetectorChange(oldValue, newValue));
        // установка группы
        radioBtnOrb.setToggleGroup(groupDetector);
        radioBtnSift.setToggleGroup(groupDetector);

        label = new Label();
        Button btnSwap = new Button("Swap");
        btnSwap.setOnAction(this::handleKeyPointsSwap);
        //
        FlowPane bottom = new FlowPane();
        //
        FlowPane bottom2 = new FlowPane();
        Border border = new Border(borderStroke);
        bottom2.setBorder(border);
        //
        Separator separator = new Separator(Orientation.VERTICAL);
        //
        FlowPane bottom1 = new FlowPane();
        bottom1.getChildren().add(textFile1);
        bottom1.getChildren().add(textFile2);
        bottom1.getChildren().add(btnSwap);
        bottom1.getChildren().add(scaling);
        bottom1.getChildren().add(btnStop);
        bottom1.getChildren().add(radioBtnDm);
        bottom1.getChildren().add(radioBtnBf);
        bottom1.getChildren().add(radioBtnKnn);
        bottom1.getChildren().add(radioBtnKnnHm);
        bottom1.getChildren().add(separator);
        bottom1.getChildren().add(label);
        bottom2.getChildren().add(radioBtnOrb);
        bottom2.getChildren().add(radioBtnSift);
        bottom2.setPadding(new Insets(15));
        //bottom.setSpacing(10);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        imageView = new ImageView();
        root.setCenter(imageView);
        bottom.getChildren().add(bottom1);
        bottom.getChildren().add(bottom2);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);

        stage.show();
    }

    private void handleKeyPointsSwap(ActionEvent actionEvent) {
        String text1 = textFile1.getText();
        textFile1.setText(textFile2.getText());
        textFile2.setText(text1);
        draw();
    }

    public static Image loadTwoImage() {
        Mat loadedImage1 = OpenCvUtils.loadImageResourceFile("faceRect.jpg");
        Mat loadedImage2 = OpenCvUtils.loadImageResourceFile("faceRect2.jpg");;
        Mat mat = OpenCvUtils.imageNextToAnother(loadedImage1, loadedImage2);
        return mat2Img(mat);
    }

    private void handleSelectedMatcherChange(Toggle oldValue, Toggle newValue) {
        if (imageView == null) {
            return;
        }
        RadioButton selectedBtn = (RadioButton) newValue;
        matcherMethod = selectedBtn.getText();
        draw();
    }

    private void handleSelectedDetectorChange(Toggle oldValue, Toggle newValue) {
        if (imageView == null) {
            return;
        }
        RadioButton selectedBtn = (RadioButton) newValue;
        openCvUtils.detectorStr = selectedBtn.getText();
        draw();
    }

    private void handleScalingValueChange(Number oldValue, Number newValue) {
        openCvUtils.kDist = newValue.floatValue();
        draw();
    }

    private void draw() {
        imageView.setImage(loadImage());
        label.setText(String.format("all=%s, good=%s", openCvUtils.getCountAllCounts(), openCvUtils.getCountGoodCounts()));
    }

    private void handleKeyPoints(ActionEvent actionEvent) {
        draw();
    }

    private Image loadImage() {
        Mat loadedImage1 = OpenCvUtils.loadImageFile(String.format("./%s.jpg", textFile1.getText()));
        MatOfKeyPoint imageKeyPoints1 = new MatOfKeyPoint();
        Mat imageDescriptors1 = new Mat();
        Feature2D detector = openCvUtils.createDetector();
        detector.detectAndCompute(loadedImage1, new Mat(), imageKeyPoints1, imageDescriptors1);
        //
        Mat loadedImage2 = OpenCvUtils.loadImageFile(String.format("./%s.jpg", textFile2.getText()));
        MatOfKeyPoint imageKeyPoints2 = new MatOfKeyPoint();
        Mat imageDescriptors2 = new Mat();
        detector.detectAndCompute(loadedImage2, new Mat(), imageKeyPoints2, imageDescriptors2);
        //
        final MatOfDMatch matOfDMatch;
        if (matcherMethod.equals("DM")) {
            matOfDMatch = openCvUtils.descriptorMatcher(imageDescriptors1, imageDescriptors2);
        } else if (matcherMethod.equals("KNN")) {
            matOfDMatch = openCvUtils.descriptorMatcherKnn(imageDescriptors1, imageDescriptors2);
        } else if (matcherMethod.equals("KNN-Hm")) {
            matOfDMatch = openCvUtils.descriptorMatcherKnnHomography(imageKeyPoints1, imageKeyPoints2, imageDescriptors1, imageDescriptors2);
        } else {
            matOfDMatch = openCvUtils.descriptorBFMatcher(imageDescriptors1, imageDescriptors2);
        }
        Mat mat = descriptorMatcherDraw(loadedImage1, loadedImage2, imageKeyPoints1, imageKeyPoints2, matOfDMatch);
        return mat2Img(mat);
    }

}
