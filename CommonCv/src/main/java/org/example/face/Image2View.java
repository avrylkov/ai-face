package org.example.face;

import javafx.scene.image.ImageView;
import org.example.opencv.OpenCVImage;
import org.example.opencv.Utils;

public class Image2View {

    private final ImageView imageView;

    public void apply(OpenCVImage image) {
        javafx.scene.image.Image img2fx = Utils.img2fx(image);
        imageView.setImage(img2fx);
    }

    public Image2View(ImageView imageView) {
        this.imageView = imageView;
    }
}
