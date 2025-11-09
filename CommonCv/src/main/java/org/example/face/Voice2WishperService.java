package org.example.face;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FileUtils;
import org.example.VoiceQueue;
import org.example.whisper.WhisperService;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

public class Voice2WishperService {

    private static Logger log = LoggerFactory.getLogger(Voice2WishperService.class);

    private final WhisperService wishperService = new WhisperService();
    private final VoiceQueue voiceQueue = new VoiceQueue();
    private Consumer<String> messageConsumer;


    public void init(Consumer<String> messageConsumer, Runnable microphoneOff) {
        this.messageConsumer = messageConsumer;
        wishperService.initialize();
        voiceQueue.initialize(this::readFileQueueToSpeech, microphoneOff);
    }

    public void stop() {
        log.info("Voice2WishperService stop");
        wishperService.stop();
        voiceQueue.stop();
    }

    public void microphoneOn() {
        voiceQueue.start();
    }

    public void microphoneOff() {
        voiceQueue.stop();
    }

    private String readFileQueueToSpeech(URI uri) {
        try {
            String speech = wishperService.speechToText(uri);
            FileUtils.delete(new File(uri));
            messageConsumer.accept(speech);
            //String chat = ollamaService.chat(speech);
            //log.info("Speech agent: {}", chat);
            return speech;
        } catch (IOException e) {
            log.error("Error delete file {}", uri, e);
        }
        return null;
    }


}
