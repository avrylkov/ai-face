package org.example;

import nu.pattern.OpenCV;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Feature2D;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.features2d.SIFT;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.opencv.features2d.DescriptorMatcher.BRUTEFORCE_HAMMING;
import static org.opencv.features2d.DescriptorMatcher.BRUTEFORCE_L1;
import static org.opencv.features2d.DescriptorMatcher.FLANNBASED;

public class OpenCvUtils {

    private static final Logger log = LogManager.getLogger(OpenCvUtils.class);

    private MatOfDMatch matOfDMatchEmpty;

    public OpenCvUtils() {
        OpenCV.loadLocally();
        matOfDMatchEmpty = new MatOfDMatch();
    }

    private CascadeClassifier cascadeClassifier = null;
    public Float kDist = 1.85f;
    public String detectorStr  = "ORB";


    private int countGoodCounts = 0;
    private int countAllCounts = 0;

    public static Mat loadImageFromFile(String imagePath) {
        return Imgcodecs.imread(imagePath);
    }

    public static String resourceFilePath(String fileName) {
        URL resourceFile = OpenCvUtils.class.getClassLoader().getResource(fileName);
        try {
            Path path = Paths.get(resourceFile.toURI());
            return path.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadCascadeClassifier() {
        if (Objects.nonNull(cascadeClassifier)) {
            return;
        }
        cascadeClassifier = new CascadeClassifier();
        cascadeClassifier.load(resourceFilePath("haarcascade_frontalface_alt.xml"));
    }

    public static Mat loadImageResourceFile(String fileName) {
        URL resourceFile1 = OpenCvUtils.class.getClassLoader().getResource(fileName);
        try {
            Path path1 = Paths.get(resourceFile1.toURI());
            return loadImageFromFile(path1.toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static Mat loadImageFile(String filePath) {
        return loadImageFromFile(filePath);
    }

    public MatOfRect detectFace(Mat inputImage) {
        MatOfRect facesDetected = new MatOfRect();
        //List<Mat> faces = new ArrayList<>();

//        int minFaceSize = Math.round(inputImage.rows() * 0.1f);
//        OpenCvUtils.cascadeClassifier.detectMultiScale(inputImage, facesDetected, 1.1, 3,
//                Objdetect.CASCADE_SCALE_IMAGE, new Size(minFaceSize, minFaceSize), new Size());
        cascadeClassifier.detectMultiScale(inputImage, facesDetected, 1.3);
        return facesDetected;
    }

    public static void saveImage(Mat imageMatrix, String targetPath) {
        Imgcodecs.imwrite(targetPath, imageMatrix, new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY , 100));
    }

    public static Mat encodeImage2Jpg(Mat inputImage) {
        MatOfByte buf = new MatOfByte();
        boolean status = Imgcodecs.imencode(".jpg", inputImage, buf,
                new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY , 100));
        if(status) {
            return Imgcodecs.imdecode(buf, Imgcodecs.IMREAD_UNCHANGED);
        } else {
            log.error("Encoding jpg image error");
        }
        return null;
    }

    public static Mat loadImage(String filePath) {
        return Imgcodecs.imread(filePath);
    }

    public Feature2D createDetector() {
        final Feature2D detector;
        if (detectorStr.equals("SIFT")) {
            detector = SIFT.create();
        } else {
            detector = ORB.create();
        }
        return detector;
    }

    public MatOfDMatch descriptorMatcher(Mat descriptorsImg1,
                                         Mat descriptorsImg2) {
        // Сравниваем дескрипторы
        MatOfDMatch matches = new MatOfDMatch();
        final DescriptorMatcher dm = DescriptorMatcher.create(detectorStr.equals("SIFT") ? FLANNBASED : BRUTEFORCE_HAMMING);
        //DescriptorMatcher dm = FlannBasedMatcher.create(BRUTEFORCE_HAMMING); // BRUTEFORCE_HAMMING

        dm.match(descriptorsImg1, descriptorsImg2, matches);
        // Вычисляем минимальное и максимальное значения
        List<DMatch> dMatches = matches.toList();
        float minDist = dMatches.stream()
                .map(d -> d.distance)
                .min(Float::compare)
                .orElse(Float.MIN_VALUE);
        // Находим лучшие совпадения
        List<DMatch> list_good = dMatches.stream()
                .filter(d -> d.distance <= minDist * kDist)
                .collect(Collectors.toList());
        countGoodCounts = list_good.size();
        countAllCounts = dMatches.size();
        MatOfDMatch matGood = new MatOfDMatch();
        matGood.fromList(list_good);
        return matGood;
    }

    public MatOfDMatch descriptorBFMatcher(Mat descriptorsImg1,
                                                  Mat descriptorsImg2) {
        final BFMatcher matcher;
        if (detectorStr.equals("SIFT")) {
            matcher = new BFMatcher();
        } else {
            matcher = new BFMatcher(BRUTEFORCE_HAMMING, true);
        }
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(descriptorsImg1, descriptorsImg2, matches);

        List<DMatch> dMatches = matches.toList();
        float minDist = dMatches.stream()
                .map(d -> d.distance)
                .min(Float::compare)
                .orElse(Float.MIN_VALUE);
        // Находим лучшие совпадения
        List<DMatch> list_good = dMatches.stream()
                .filter(d -> d.distance < minDist * kDist)
                .collect(Collectors.toList());
        countGoodCounts = list_good.size();
        countAllCounts = dMatches.size();
        MatOfDMatch matGood = new MatOfDMatch();
        matGood.fromList(list_good);

        return matGood;
    }

    public MatOfDMatch descriptorMatcherKnn(Mat descriptorsImg1, Mat descriptorsImg2) {
        final DescriptorMatcher dm = DescriptorMatcher.create(detectorStr.equals("SIFT") ? BRUTEFORCE_L1 : BRUTEFORCE_HAMMING);
        //DescriptorMatcher dm = FlannBasedMatcher.create(BRUTEFORCE_HAMMING);
        // match these two keypoints sets
        List<MatOfDMatch> matches = new ArrayList<>();
        dm.knnMatch(descriptorsImg1, descriptorsImg2, matches, 2);
        // ratio test
        List<DMatch> good_matches = matches.stream().map(MatOfDMatch::toArray)
                .filter(dMatches -> dMatches[0].distance < (dMatches[1].distance * 0.75))
                .collect(Collectors.toList())
                .stream().map(dMatches -> dMatches[0])
                .collect(Collectors.toList());
        countGoodCounts = good_matches.size();
        countAllCounts = matches.size();
        MatOfDMatch better_matches_mat = new MatOfDMatch();
        better_matches_mat.fromList(good_matches);
        return better_matches_mat;
    }

    public MatOfDMatch descriptorMatcherKnnHomography(MatOfKeyPoint imageKeyPoints1, MatOfKeyPoint imageKeyPoints2,
                                                             Mat descriptorsImg1, Mat descriptorsImg2) {
        final DescriptorMatcher dm = DescriptorMatcher.create(detectorStr.equals("SIFT") ? BRUTEFORCE_L1 : BRUTEFORCE_HAMMING);
        // match these two keypoints sets
        List<MatOfDMatch> matches = new ArrayList<>();
        dm.knnMatch(descriptorsImg1, descriptorsImg2, matches, 2);
        // ratio test
        List<DMatch> good_matches = matches.stream().map(MatOfDMatch::toArray)
                //.filter(dMatches -> dMatches[0].distance / dMatches[1].distance < 0.9)
                .filter(dMatches -> dMatches[0].distance < (dMatches[1].distance * 0.75))
                .collect(Collectors.toList())
                .stream().map(dMatches -> dMatches[0])
                .collect(Collectors.toList());

        // get keypoint coordinates of good matches to find homography and remove outliers using ransac
        List<Point> pts1 = new ArrayList<>();
        List<Point> pts2 = new ArrayList<>();
        if (good_matches.size() < 4) {
            return matOfDMatchEmpty;
        }
        for (int i = 0; i < good_matches.size(); i++) {
            pts1.add(imageKeyPoints1.toList().get(good_matches.get(i).queryIdx).pt);
            pts2.add(imageKeyPoints2.toList().get(good_matches.get(i).trainIdx).pt);
        }
        // outputMask contains zeros and ones indicating which matches are filtered
        Mat outputMask = new Mat();
        // Find homography - here just used to perform match filtering with RANSAC, but could be used to e.g. stitch images
        // the smaller the allowed reprojection error (here 15), the more matches are filtered
        MatOfPoint2f pts1Mat = new MatOfPoint2f();
        pts1Mat.fromList(pts1);
        MatOfPoint2f pts2Mat = new MatOfPoint2f();
        pts2Mat.fromList(pts2);
        Mat homog = Calib3d.findHomography(pts1Mat, pts2Mat, Calib3d.RANSAC, 15, outputMask, 2000, 0.995);
        if (homog.empty()) {
            return matOfDMatchEmpty;
        }

        LinkedList<DMatch> better_matches = new LinkedList<>();
        for (int i = 0; i < good_matches.size(); i++) {
            if (outputMask.get(i, 0)[0] != 0.0) {
                better_matches.add(good_matches.get(i));
            }
        }

        MatOfDMatch better_matches_mat = new MatOfDMatch();
        better_matches_mat.fromList(better_matches);
        countGoodCounts = better_matches.size();
        countAllCounts = matches.size();
        return better_matches_mat;
    }

    public static Mat descriptorMatcherDraw(Mat img1,
                                            Mat img2,
                                            MatOfKeyPoint kp1,
                                            MatOfKeyPoint kp2,
                                            MatOfDMatch matGood) {
        // Отрисовываем результат
        Scalar COLOR_BLACK = new Scalar(0, 0, 0);
        Mat outImg = new Mat(img1.rows() + img2.rows() + 10,
                img1.cols() + img2.cols() + 10,
                CvType.CV_8UC3, COLOR_BLACK);

        Features2d.drawMatches(img1, kp1, img2, kp2, matGood, outImg,
                new Scalar(255, 255, 255), Scalar.all(-1), new MatOfByte(),
                Features2d.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS);
        return outImg;
    }


    public static Mat imageNextToAnother(Mat img1, Mat img2) {

        Mat resultImages = new Mat(Math.max(img1.rows(), img2.rows()), img1.cols() + img2.cols(), img1.type());
        Mat roi1 = resultImages.submat(new Rect(0,0, img1.width(), img1.height()));
        img1.copyTo(roi1);

        Mat roi2 = resultImages.submat(new Rect(img1.cols(), 0, img2.width(), img2.height()));
        img2.copyTo(roi2);
        return resultImages;
    }

    public int getCountGoodCounts() {
        return countGoodCounts;
    }

    public int getCountAllCounts() {
        return countAllCounts;
    }

}
