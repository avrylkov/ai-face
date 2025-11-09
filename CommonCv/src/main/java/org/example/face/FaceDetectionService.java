package org.example.face;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import org.example.CommonProperties;
import org.example.opencv.OpenCVImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FaceDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FaceDetectionService.class);

    private ZooModel<Image, DetectedObjects> model = null;

    public ZooModel<Image, DetectedObjects> loadModel() {
        Criteria<Image, DetectedObjects> criteria;
        double confThresh = 0.85f;
        double nmsThresh = 0.45f;
        double[] variance = {0.1f, 0.2f};
        int topK = 5000;
        int[][] scales = {{16, 32}, {64, 128}, {256, 512}};
        int[] steps = {8, 16, 32};
        FaceDetectionTranslator translator =
                new FaceDetectionTranslator(confThresh, nmsThresh, variance, topK, scales, steps);

        //String userHome = System.getProperty("user.home");
        Path path = Paths.get(CommonProperties.INSTANCE().getFaceDetection());
        criteria =
                Criteria.builder()
                        .setTypes(Image.class, DetectedObjects.class)
                        .optModelPath(path)
                        //.optModelUrls("https://resources.djl.ai/test-models/pytorch/retinaface.zip")
                        // Load model from local file, e.g:
                        .optModelName("retinaface") // specify model file prefix
                        .optTranslator(translator)
                        .optProgress(new ProgressBar())
                        .optEngine("PyTorch") // Use PyTorch engine
                        .build();
        try {
            model = criteria.loadModel();
            return model;
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            throw new RuntimeException(e);
        }
    }

    public DetectedObjects predict(OpenCVImage img) {
        if (!model.getNDManager().isOpen()) {
            log.info("ND Manager closed");
            return null;
        }
        try (Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
            DetectedObjects detection = predictor.predict(img);
            int numberOfObjects = detection.getNumberOfObjects();
            log.debug("Detected {} objects", numberOfObjects);
            //
            if (numberOfObjects > 0) {
                img.drawBoundingRectangle(detection);
                if (log.isDebugEnabled()) {
                    detection2File(img, detection);
                }
            }
            return detection;
        } catch (Exception e) {
            log.error("Error predict", e);
        }
        return null;
    }

    public void close() {
        if (model != null) {
            model.close();
        }
    }

    public boolean isLoaded() {
        if (model != null) {
            return model.getNDManager().isOpen();
        }
        return false;
    }

    private void detection2File(OpenCVImage img, DetectedObjects detection) {
        List<DetectedObjects.DetectedObject> list = detection.items();
        for (int i = 0; i < list.size(); i++) {
            Rectangle rectangle = list.get(i).getBoundingBox().getBounds();
            OpenCVImage subImage = (OpenCVImage) img.getSubImage(rectangle);
            subImage.save(String.format("./face/face-detection_%s.jpg", i));
        }
    }

}
