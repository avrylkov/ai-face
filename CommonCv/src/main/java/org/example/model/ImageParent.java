package org.example.model;

import com.jsoniter.annotation.JsonIgnore;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

public class ImageParent {

    private static final float K_SIMILAR_FLOAT = 20.0f;
    private static final int K_SIMILAR_INT = (int) K_SIMILAR_FLOAT;

    private String name;
    @JsonIgnore
    private Mat image;
    private int counter = 0;
    private List<ImageChildren> children = new ArrayList<>();

    public ImageParent() {
        //
    }

    public ImageParent(String name, Mat image) {
        this.name = name;
        this.image = image;
    }

    public List<ImageChildren> getChildren() {
        return children;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public Mat getImage() {
        return image;
    }

    public void setImage(Mat image) {
        this.image = image;
    }

    public void setChildren(List<ImageChildren> children) {
        this.children = children;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    @JsonIgnore
    public int getCounterInc() {
        int c = counter;
        counter++;
        return c;
    }

    @JsonIgnore
    public int getChildMaxCount() {
        return children.stream()
                .map(ImageChildren::getGoodParentCounts)
                .max(Integer::compare).orElse(0);
    }

    @JsonIgnore
    public Double getChildAvgCount() {
        return children.stream()
                .map(ImageChildren::getGoodParentCounts)
                .mapToInt(Integer::intValue)
                .summaryStatistics()
                .getAverage();
    }

    @JsonIgnore
    public boolean isSimilarParentCounts(int parentCounts) {
        final int inCount = Math.round(parentCounts / K_SIMILAR_FLOAT) * K_SIMILAR_INT;
        return children.stream()
                .map(imageChildren -> Math.round(imageChildren.getGoodParentCounts() / K_SIMILAR_FLOAT) * K_SIMILAR_INT)
                .anyMatch(count -> count == inCount);
    }

}
