package org.example;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.ImageChildren;
import org.example.model.ImageParent;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.Feature2D;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassifierService {

    public static final String MODEL_FILE = "model.json";
    private final OpenCvUtils openCvUtils;

    public ClassifierService(OpenCvUtils openCvUtils) {
        this.openCvUtils = openCvUtils;
    }

    private static final Logger log = LogManager.getLogger(ClassifierService.class);

    private List<ImageParent> imageParents = new ArrayList<>();
    private static final int MATCH_MINIMAL = 6;
    //public int prevGoodCounts = 0;
    //public int goodCounts = 0;
    //public String prevImageParent = "";

    public void process(Mat inputImage) {
        boolean isMatchParent = matchParent(inputImage);
        if (!isMatchParent) {
            String name = StringUtils.leftPad(String.valueOf(imageParents.size()), 5, '0');
            try {
                FileUtils.forceMkdir(new File("./" + name));
                Mat encodeImage = OpenCvUtils.encodeImage2Jpg(inputImage);
                ImageParent imageParent = new ImageParent(name, encodeImage);
                imageParents.add(imageParent);
                OpenCvUtils.saveImage(encodeImage, String.format("./%s/%s.jpg", imageParent.getName(), imageParent.getName()));
                log.info("Save parent {}", imageParent.getName());
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

    public boolean loadModelFromJson() {
        File file = new File("./" + MODEL_FILE);
        if (file.exists()) {
            try {
                String fileToString = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                deserializeModel(fileToString);
                log.info("Load deserialized Model");
            } catch (IOException e) {
                log.error(e);
                return false;
            }
        } else {
            log.info("Create new model");
        }
        return true;
    }

    public void saveModelToJson() {
        String serializeModel = serializeModel();
        log.info("Serialized Model");
        try {
            FileUtils.writeStringToFile(new File("./" + MODEL_FILE), serializeModel, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error(e);
        }
    }

    private void deserializeModel(String model) {
        ImageParent[] deserialize = JsonIterator.deserialize(model, ImageParent[].class);
        imageParents = new ArrayList<>(Arrays.asList(deserialize));
        for (ImageParent imageParent : imageParents) {
            imageParent.setImage(OpenCvUtils.loadImage("./" + imageParent.getName() + "/" + imageParent.getName() + ".jpg"));
        }
    }

    private String serializeModel() {
        return JsonStream.serialize(imageParents);
    }

    private boolean matchParent(Mat inputImage) {
        for (ImageParent imageParent : imageParents) {
            MatOfKeyPoint imageInputKeyPoints = new MatOfKeyPoint();
            Mat imageInputDescriptors = new Mat();
            Feature2D detector = openCvUtils.createDetector();

            Mat encodeInputImage = OpenCvUtils.encodeImage2Jpg(inputImage);
            detector.detectAndCompute(encodeInputImage, new Mat(), imageInputKeyPoints, imageInputDescriptors);
            //
            MatOfKeyPoint imageParentKeyPoints = new MatOfKeyPoint();
            Mat imageParentDescriptors = new Mat();
            detector.detectAndCompute(imageParent.getImage(), new Mat(), imageParentKeyPoints, imageParentDescriptors);
            //
            MatOfDMatch matOfDMatch = openCvUtils.descriptorMatcherKnnHomography(imageParentKeyPoints, imageInputKeyPoints, imageParentDescriptors, imageInputDescriptors);
            if (matOfDMatch.toArray().length >= MATCH_MINIMAL) {
                int goodCounts = matOfDMatch.toArray().length;
                boolean similarParentCounts = imageParent.isSimilarParentCounts(goodCounts);
                //int childAvgCount = imageParent.getChildAvgCount().intValue();
                //int deltaKeyPoints = Math.abs(goodCounts - childAvgCount);
                //float percentageDifference = ((float) deltaKeyPoints / goodCounts) * 100.0f;
                if (!similarParentCounts) {
                    ImageChildren imageChildren = new ImageChildren(imageParent.getName() + "_" + imageParent.getCounterInc(), goodCounts);
                    imageParent.getChildren().add(imageChildren);
                    OpenCvUtils.saveImage(encodeInputImage, String.format("./%s/%s.jpg", imageParent.getName(), imageChildren.getName()));
                    log.info(String.format("Save image %s, goodCounts=%s", imageChildren.getName(), goodCounts));
                    return true;
                } else {
                    log.info("Image similar, parent={}, goodCounts={}", imageParent.getName(), goodCounts);
                    return true;
                }
            } else {
                log.info("No match of parent={}, length={}", imageParent.getName(), matOfDMatch.toArray().length);
            }
        }
        return false;
    }

    private String getImageParentName(String imageName) {
        return StringUtils.substringBefore(imageName, "_");
    }

}
