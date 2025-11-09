package org.example;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class AppAudioFileService {

    private static final Logger log = LoggerFactory.getLogger(AppAudioFileService.class);

    public  static void main(String[] args) {
        try {
            byte[] audioBytes = FileUtils.readFileToByteArray(new File("./Recording.wav"));
            AudioService audioService = new AudioService();
            audioService.writeAudioMap(audioBytes, "./audioMap.csv");
            List<int[]> detectSilence = audioService.detectSilence(audioBytes);
            log.info("Detected silence {}", detectSilence);
            byte[] removeSilence = audioService.removeSilence(audioBytes, detectSilence);
            audioService.writeAudioData(removeSilence, "./no_silence.wav");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
}
