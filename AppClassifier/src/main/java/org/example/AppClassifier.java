package org.example;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootApplication
public class AppClassifier
{

    private static Logger log = LogManager.getLogger(AppClassifier.class);

    private ScheduledExecutorService scheduler;
    private final AtomicBoolean cameraReady = new AtomicBoolean(false);
    private final AtomicBoolean saveModel = new AtomicBoolean(false);
    private final OpenCvUtils openCvUtils = new OpenCvUtils();
    private final ClassifierService classifierService = new ClassifierService(openCvUtils);
    private VideoCapture videoCapture;

    //private final TaskExecutor exec = new SimpleAsyncTaskExecutor();

    public static void main( String[] args )
    {
        SpringApplication.run(AppClassifier.class, args);//.close();
    }

    @KafkaListener(topics = "topic1")
    public void listen(Map<String, String> command) {
        log.info("Received command: {}", command);
        if (command.get(Constant.Command.CMD).equals(Constant.Command.CAMERA_READY)) {
            cameraReady.set(Boolean.parseBoolean(command.get(Constant.Command.READY)));
        } else if (command.get(Constant.Command.CMD).equals(Constant.Command.START)) {
            startTasksClassifier();
        } else if (command.get(Constant.Command.CMD).equals(Constant.Command.STOP)) {
            schedulerShutdown();
        } else {
            log.error("Unknown command: {}", command.get(Constant.Command.CMD));
        }
    }

    @Bean
    public NewTopic topic() {
        return new NewTopic("topic1", 1, (short) 1);
    }

    @Bean
    @Profile("default") // Don't run from test(s)
    public ApplicationRunner runner() {
        return args -> {

            if (classifierService.loadModelFromJson()) {
                log.info("Load model from json");
            } else {
                log.error("Failed to load model from json");
                return;
            }
            openCvUtils.loadCascadeClassifier();
            videoCapture = new VideoCapture(0); // The number is the ID of the camera
            startTasksClassifier();
            //
            System.out.println("Hit Enter to terminate...");
            System.in.read();
        };
    }

    private void startTasksClassifier() {
        if (scheduler == null) {
            scheduler = new DynamicScheduledExecutorService(1);
        } else {
            return;
        }
        scheduler.schedule(this::setCameraReady, 5 , TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::scheduleSaveModel, 5, 3, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::startClassifier, 0, 200, TimeUnit.MILLISECONDS);
    }

    private void schedulerShutdown() {
        if (scheduler != null) {
            log.info("Shutting down...");
            scheduler.shutdownNow();
        }
        scheduler = null;
    }

    private void setCameraReady() {
        log.info("cameraReady");
        cameraReady.set(true);
    }

    private void startClassifier() {
        if (!cameraReady.get() || saveModel.get()) {
            log.info("camera is not ready or save model running");
            return;
        }
        log.info("Starting classifier");
        captureWithFaceDetection();
    }

    private void scheduleSaveModel() {
        if (!cameraReady.get()) {
            log.info("camera is not ready");
            return;
        }
        saveModel.set(true);
        log.info("Save model");
        classifierService.saveModelToJson();
        saveModel.set(false);
    }

    private void captureWithFaceDetection() {
        Mat inputImage = new Mat();
        if (videoCapture.isOpened()) {
            videoCapture.read(inputImage);
        }
        if (!cameraReady.get()) {
            return;
        }
        Mat imageGray = new Mat();
        Imgproc.cvtColor(inputImage, imageGray, Imgproc.COLOR_BGR2GRAY);

        MatOfRect matOfRect = openCvUtils.detectFace(imageGray);
        Rect[] facesArray = matOfRect.toArray();
        for (Rect face : facesArray) {
            Rect f = new Rect(face.x - 10, face.y - 10, face.width + 10, face.height + 10);
            final Mat faceRect = imageGray.submat(f);
            classifierService.process(faceRect);
            //Imgproc.rectangle(imageGray, f.tl(), f.br(), new Scalar(0, 0, 255), 2);
            break;
        }

    }


}
