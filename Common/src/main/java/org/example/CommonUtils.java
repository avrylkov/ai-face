package org.example;

import org.apache.commons.io.FileUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommonUtils {

    public static Path resourceCommonToPath(String relativePath) {
        try {
            URL fileUrl = CommonUtils.class.getClassLoader().getResource(relativePath);
            return Paths.get(fileUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static URI resourceCommonToUri(String relativePath) {
        try {
            URL fileUrl = CommonUtils.class.getClassLoader().getResource(relativePath);
            return fileUrl.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static URL resourceToUrl(Class<?> clazz, String relativePath) {
        return clazz.getClassLoader().getResource(relativePath);
    }

    public static URI toUri(String relativePath) {
        return FileUtils.getFile(relativePath).toURI();
    }

}
