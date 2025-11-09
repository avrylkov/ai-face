package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CommonProperties {

    private static CommonProperties instance;
    private String faceDetection;
    private String featureExtract;
    private String whisperModel;
    private String embeddingModel;
    private String embeddingModelTokenizer;
    private String voiceFolder;
    private boolean autoStart;
    private boolean usePauseDetect;

    private CommonProperties() {
        //
    }

    public static CommonProperties INSTANCE()
    {
        if (instance == null) {
            instance = new CommonProperties();
            try (InputStream input = CommonProperties.class.getClassLoader().getResourceAsStream("application.properties")) {
                if (input == null) {
                    throw new RuntimeException("unable to find application.properties");
                }
                Properties properties = new Properties();
                properties.load(input);
                //
                instance.voiceFolder = properties.getProperty("voice.folder");
                instance.faceDetection = properties.getProperty("face.detection");
                instance.featureExtract = properties.getProperty("feature.extract");
                instance.whisperModel = properties.getProperty("whisper.model");
                instance.embeddingModel = properties.getProperty("embedding.model");
                instance.embeddingModelTokenizer = properties.getProperty("embedding.model.tokenizer");
                instance.autoStart = Boolean.parseBoolean(properties.getProperty("auto.start"));
                instance.usePauseDetect = Boolean.parseBoolean(properties.getProperty("use.pause.detect"));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return instance;
    }

    public String getFaceDetection() {
        return faceDetection;
    }

    public String getFeatureExtract() {
        return featureExtract;
    }

    public String getWhisperModel() {
        return whisperModel;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public String getEmbeddingModelTokenizer() {
        return embeddingModelTokenizer;
    }

    public String getVoiceFolder() {
        return voiceFolder;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public boolean isUsePauseDetect() {
        return usePauseDetect;
    }
}
