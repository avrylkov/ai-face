package org.example.model;

public class Search {

    public String title;
    public int pageid;
    public int size;
    public int wordcount;
    public String snippet;


    @Override
    public String toString() {
        return "Search{" +
                "title='" + title + '\'' +
                ", snippet='" + snippet + '\'' +
                '}';
    }
}
