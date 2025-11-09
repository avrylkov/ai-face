package org.example.whisper;

import ai.djl.translate.TranslateException;
import org.apache.commons.lang3.StringUtils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;

import static org.example.CommonUtils.resourceCommonToPath;

public class WhisperService {

    private static final String speechAfter = "notimestamps|>";
    private static final String speechBefore = "<|endoftext";
    private WhisperModel speechModel;
    private Logger log = LoggerFactory.getLogger(WhisperService.class);

    public void initialize() {
        try {
            speechModel = new WhisperModel();
            String speech = speechModel.speechToText(resourceCommonToPath("./Recording.wav"));
            log.info("Test speech {}", speech);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        log.info("Stopping model");
        if (speechModel != null) {
            speechModel.close();
        }
    }

    public String speechToText(URI uri) {
        String speech;
        try {
            speech = speechModel.speechToText(Paths.get(uri));
            log.info("Speech agent: {}", speech);
            String after = StringUtils.substringAfter(speech, speechAfter);
            return StringUtils.substringBefore(after, speechBefore);
        } catch (IOException | TranslateException e) {
            throw new RuntimeException(e);
        }
    }

}
