package org.example.service;

import org.example.face.Face;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GlobalContext implements ApplicationContextAware {

    private static ApplicationContext context;
    private static Face currentFace;
    private static final List<Face> allFaces = new ArrayList<>();

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        GlobalContext.context = applicationContext;
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static void setCurrentFace(Face currentFace) {
        GlobalContext.currentFace = currentFace;
    }

    public static Face getCurrentFace() {
        return currentFace;
    }

    public static Face getFaceById(Integer faceId) {
        return allFaces.stream()
                .filter(face -> faceId.equals(face.getId()))
                .findFirst()
                .orElse(null);
    }

    public static List<Face> getAllFaces() {
        return allFaces;
    }

    public static int nextFaceId() {
        return allFaces.size() + 1;
    }

}
