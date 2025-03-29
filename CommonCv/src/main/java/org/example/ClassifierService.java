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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClassifierService {

    public static final String MODEL_FILE = "model.json";
    private final OpenCvUtils openCvUtils;

    public ClassifierService(OpenCvUtils openCvUtils) {
        this.openCvUtils = openCvUtils;
    }

    private static final Logger log = LogManager.getLogger(ClassifierService.class);

    private List<ImageParent> imageParents = new ArrayList<>();
    private static final int MATCH_MINIMAL = 7;
    //public int prevGoodCounts = 0;
    //public int goodCounts = 0;
    //public String prevImageParent = "";

    public void process(Mat inputImage) {
        boolean isMatchParent = matchParent(inputImage);
        if (!isMatchParent) {
            String name = StringUtils.leftPad(String.valueOf(imageParents.size()), 5, '0');
            ImageParent imageParent = new ImageParent(name, inputImage);
            try {
                FileUtils.forceMkdir(new File("./" + imageParent.getName()));
                imageParents.add(imageParent);
                OpenCvUtils.saveImage(inputImage, String.format("./%s/%s.jpg", imageParent.getName(), imageParent.getName()));
                log.info("Save parent {}", imageParent.getName());
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

    public String serializeModel() {
        return JsonStream.serialize(imageParents);
    }

    public void deserializeModel(String model) {
        ImageParent[] deserialize = JsonIterator.deserialize(model, ImageParent[].class);
        imageParents = new ArrayList<>(Arrays.asList(deserialize));
        for (ImageParent imageParent : imageParents) {
            imageParent.setImage(OpenCvUtils.loadImage("./" + imageParent.getName() + "/" + imageParent.getName() + ".jpg"));
        }
    }

    public void clearSameImage_() {
        imageParents.forEach(imageParent -> {
            List<ImageChildren> roundedParentCounts = imageParent.getChildren()
                    .stream()
                    .peek(s -> s.setGoodParentCounts((int) Math.round(s.getGoodParentCounts() / 50.0) * 50))
                    .collect(Collectors.toList());
            //
            List<Integer> uniqueParentCounts = roundedParentCounts
                    .stream()
                    .map(ImageChildren::getGoodParentCounts)
                    .distinct()
                    .collect(Collectors.toList());
            //
            List<ImageChildren> firstChildren = new ArrayList<>();
            uniqueParentCounts.forEach(count -> {
                roundedParentCounts
                        .stream()
                        .filter(f -> f.getGoodParentCounts() == count)
                        .findFirst()
                        .ifPresent(firstChildren::add);
            });
            //
            imageParent.getChildren().stream()
                    .filter(image -> image.getName().contains("_"))
                    .filter(image -> firstChildren.stream().noneMatch(firstImage -> firstImage.getName().equals(image.getName())))
                    .forEach(image -> {
                                String path = "./" + StringUtils.substringBefore(imageParent.getName(), ".") + "/" + image.getName() + ".jpg";
                                try {
                                    log.info("Delete image path={}, image = {}", path, image.toString());
                                    FileUtils.delete(new File(path));
                                } catch (IOException e) {
                                    log.error(e);
                                }
                            }
                    );

            imageParent.setChildren(firstChildren);
        });

    }

    private boolean matchParent(Mat inputImage) {
        for (ImageParent imageParent : imageParents) {
            MatOfKeyPoint imageInputKeyPoints = new MatOfKeyPoint();
            Mat imageInputDescriptors = new Mat();
            Feature2D detector = openCvUtils.createDetector();
            detector.detectAndCompute(inputImage, new Mat(), imageInputKeyPoints, imageInputDescriptors);
            //
            MatOfKeyPoint imageParentKeyPoints = new MatOfKeyPoint();
            Mat imageParentDescriptors = new Mat();
            detector.detectAndCompute(imageParent.getImage(), new Mat(), imageParentKeyPoints, imageParentDescriptors);
            //
            MatOfDMatch matOfDMatch = openCvUtils.descriptorMatcherKnnHomography(imageInputKeyPoints, imageParentKeyPoints, imageInputDescriptors, imageParentDescriptors);
            if (matOfDMatch.toArray().length > MATCH_MINIMAL) {
                int goodCounts = matOfDMatch.toArray().length;
                boolean similarParentCounts = imageParent.isSimilarParentCounts(goodCounts);
                //int childAvgCount = imageParent.getChildAvgCount().intValue();
                //int deltaKeyPoints = Math.abs(goodCounts - childAvgCount);
                //float percentageDifference = ((float) deltaKeyPoints / goodCounts) * 100.0f;
                if (!similarParentCounts) {
                    ImageChildren imageChildren = new ImageChildren(imageParent.getName() + "_" + imageParent.getCounterInc(), goodCounts);
                    imageParent.getChildren().add(imageChildren);
                    OpenCvUtils.saveImage(inputImage, String.format("./%s/%s.jpg", imageParent.getName(), imageChildren.getName()));
                    log.info(String.format("Save image %s, goodCounts=%s", imageChildren.getName(), goodCounts));
                    return true;
                } else {
                    log.info("Image similar, parent={}, goodCounts={}", imageParent.getName(), goodCounts);
                    return true;
                }
            } else {
                log.info("Match length={}", matOfDMatch.toArray().length);
            }
        }
        return false;
    }

    private String getImageParentName(String imageName) {
        return StringUtils.substringBefore(imageName, "_");
    }

}
