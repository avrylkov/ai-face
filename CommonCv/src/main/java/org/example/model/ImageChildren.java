package org.example.model;

public class ImageChildren {

    private String name;
    private int goodParentCounts;

    public ImageChildren() {
        //
    }

    public ImageChildren(String name, int goodParentCounts) {
        this.name = name;
        this.goodParentCounts = goodParentCounts;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGoodParentCounts(int goodParentCounts) {
        this.goodParentCounts = goodParentCounts;
    }

    public int getGoodParentCounts() {
        return goodParentCounts;
    }

    @Override
    public String toString() {
        return "ImageChildren{" +
                "goodParentCounts=" + goodParentCounts +
                ", name='" + name + '\'' +
                '}';
    }
}
