package org.example.opencv;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Utils {

    public static Image img2fx(OpenCVImage image) {
        MatOfByte buf = new MatOfByte();
        if (!Imgcodecs.imencode(".jpg", image.getWrappedImage(), buf)) {
            throw new RuntimeException("Failed save image.");
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(buf.toArray());
        return new Image(inputStream);
    }

    public static OpenCVImage img2cv(Image image) {
        // Convert JavaFX Image to AWT BufferedImage
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        // Write the BufferedImage to the ByteArrayOutputStream in the specified format
        // Common formats include "png", "jpg", "gif", "bmp"
        try {
            ImageIO.write(bufferedImage, "jpg", bos);
            MatOfByte buf = new MatOfByte(bos.toByteArray());
            return new OpenCVImage(buf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void apply2view(ImageView imageView, OpenCVImage image) {
        javafx.scene.image.Image img2fx = img2fx(image);
        imageView.setImage(img2fx);
    }

    public static Image getImage(String resourceImageName) {
        return new Image(Utils.class.getClassLoader().getResource(resourceImageName).toExternalForm());
    }

//    public static int getIntFromColor(int Red, int Green, int Blue){
//        Red = (Red << 16) & 0x00FF0000; //Shift red 16-bits and mask out other stuff
//        Green = (Green << 8) & 0x0000FF00; //Shift Green 8-bits and mask out other stuff
//        Blue = Blue & 0x000000FF; //Mask out anything not blue.
//
//        return 0xFF000000 | Red | Green | Blue; //0xFF000000 for 100% Alpha. Bitwise OR everything together.
//    }


}
