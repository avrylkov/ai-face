package org.example.face;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.repository.zoo.ZooModel;
import org.example.DynamicScheduledExecutorService;
import org.example.RepeatingTask;
import org.example.opencv.OpenCVImage;
import org.example.opencv.OpenCVImageFactory;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class VideoFace2Detection {

    private static final Logger log = LoggerFactory.getLogger(VideoFace2Detection.class);
    private final ScheduledExecutorService scheduler = new DynamicScheduledExecutorService(1);
    private final RepeatingTask repeatingLifeCycleTask = new RepeatingTask(this::runFacePredict, 3000,200, "Face Detection");

    private final ReentrantLock detectedObjectsLock = new ReentrantLock();
    private VideoCapture videoCapture;
    private OpenCVImageFactory openCVImageFactory;
    private final FaceDetectionService faceDetectionService = new FaceDetectionService();
    private Image2View image2View;
    private DetectedObjects detectedObjects;
    private OpenCVImage inputImage;


    public void start(Image2View image2View) {
        this.image2View = image2View;
        openCVImageFactory = new OpenCVImageFactory();
        videoCapture = new VideoCapture(0);
        if (!faceDetectionService.isLoaded()) {
            faceDetectionService.loadModel();
        }
        //
        Mat inputImageMat = new Mat();
        videoCapture.read(inputImageMat);
        //scheduler.scheduleAtFixedRate(this::runStartPredict, 3000, 200, TimeUnit.MILLISECONDS);
        repeatingLifeCycleTask.start();
    }

    public void stopSchedule() {
        log.info("stopSchedule VideoFace2Detection");
        scheduler.shutdown();
        repeatingLifeCycleTask.interrupt();
        if (videoCapture != null && videoCapture.isOpened()) {
            videoCapture.release();
        }
    }

    public void close() {
        try {
            log.info("Closing VideoFace2Detection");
            if (faceDetectionService.isLoaded()) {
                faceDetectionService.close();
            }
            if (videoCapture != null) {
                videoCapture.release();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Run cycle face predict
     */
    private void runFacePredict() throws InterruptedException {
        log.debug("startPredict VideoFace2Detection");
        if (!(videoCapture.isOpened() && faceDetectionService.isLoaded())) {
            log.info("Video Capture or Face Detection Model is not loaded");
            return;
        }
        Mat inputImageMat = new Mat();
        videoCapture.read(inputImageMat);
        OpenCVImage image = (OpenCVImage) openCVImageFactory.fromImage(inputImageMat);
        boolean triedLock = false;
        try {
            triedLock = detectedObjectsLock.tryLock(3, TimeUnit.SECONDS);
            if (triedLock) {
                //inputImage = (OpenCVImage) image.duplicate();
                inputImage = image;
                detectedObjects = faceDetectionService.predict(image);
                image2View.apply(image);
            }
        } finally {
            if (triedLock) {
                detectedObjectsLock.unlock();
            }
        }
    }

    public boolean setDetectedObjectsLock() throws InterruptedException {
        return detectedObjectsLock.tryLock(3, TimeUnit.SECONDS);
    }

    public void setDetectedObjectsUnlock() {
       detectedObjectsLock.unlock();
    }

    public DetectedObjects getDetectedObjects() {
        return detectedObjects;
    }

    public List<OpenCVImage> getFaces(DetectedObjects detection) {
        List<DetectedObjects.DetectedObject> list = detection.items();
        List<OpenCVImage> faces = new ArrayList<>();
        for (DetectedObjects.DetectedObject detectedObject : list) {
            Rectangle rectangle = detectedObject.getBoundingBox().getBounds();
            faces.add((OpenCVImage) inputImage.getSubImage(rectangle));
        }
        return faces;
    }

}
