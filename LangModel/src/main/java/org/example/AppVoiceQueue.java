package org.example;

import org.apache.commons.io.FileUtils;
import org.example.whisper.WhisperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Scanner;

public class AppVoiceQueue {

    private static Logger log = LoggerFactory.getLogger(AppVoiceQueue.class);
    private final WhisperService wishperService = new WhisperService();
    private final VoiceQueue voiceQueue = new VoiceQueue();

    public static void main(String[] args) {
        new AppVoiceQueue().run();
    }

    public void run() {
        try {
            log.info("Starting audio...");
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            //
            wishperService.initialize();
            voiceQueue.initialize(this::readFileQueueToSpeech, new Runnable() {
                @Override
                public void run() {
                }
            });
            voiceQueue.start();
            //
            log.info("Stopping audio...");
            line = scanner.nextLine();
            wishperService.stop();
            voiceQueue.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private String readFileQueueToSpeech(URI uri) {
        try {
            String speech = wishperService.speechToText(uri);
            FileUtils.delete(new File(uri));
            return speech;
        } catch (IOException e) {
            log.error("Error delete file {}", uri, e);
        }
        return null;
    }

}
