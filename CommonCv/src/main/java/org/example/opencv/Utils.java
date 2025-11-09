package org.example.opencv;

import javafx.scene.image.Image;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import java.io.ByteArrayInputStream;

public class Utils {

    public static Image img2fx(OpenCVImage image) {
        MatOfByte buf = new MatOfByte();
        if (!Imgcodecs.imencode(".jpg", image.getWrappedImage(), buf)) {
            throw new RuntimeException("Failed save image.");
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(buf.toArray());
        return new Image(inputStream);
    }

    public static Image getImage(String imageName) {
        return new Image(Utils.class.getClassLoader().getResource(imageName).toExternalForm());
    }

//    public static int getIntFromColor(int Red, int Green, int Blue){
//        Red = (Red << 16) & 0x00FF0000; //Shift red 16-bits and mask out other stuff
//        Green = (Green << 8) & 0x0000FF00; //Shift Green 8-bits and mask out other stuff
//        Blue = Blue & 0x000000FF; //Mask out anything not blue.
//
//        return 0xFF000000 | Red | Green | Blue; //0xFF000000 for 100% Alpha. Bitwise OR everything together.
//    }


}
