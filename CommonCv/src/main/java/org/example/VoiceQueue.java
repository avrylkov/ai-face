package org.example;

import org.apache.commons.io.FileUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.example.CommonUtils.toUri;

public class VoiceQueue {

    private ScheduledExecutorService scheduler = new DynamicScheduledExecutorService(1);

    private static final Logger log = LoggerFactory.getLogger(VoiceQueue.class);

    private AudioService audioService;
    private Function<URI, String> translate;
    private Runnable microphoneOff;

    public void initialize(Function<URI, String> translate, Runnable microphoneOff) {
        this.translate = translate;
        this.microphoneOff = microphoneOff;
        audioService = new AudioService();
        audioService.init();
        //audioService.recordSplitStart();
        scheduler.scheduleAtFixedRate(this::readFileQueueToSpeech, 1000, 100, TimeUnit.MILLISECONDS);
        log.info("Voice queue initialized");
    }

    public void stop() {
        log.info("Остановка записи голоса");
        if (audioService != null) {
            audioService.recordSplitStop();
        }
        scheduler.shutdown();
    }

    public void start() {
        if (scheduler.isShutdown()) {
            log.info("Начало записи голоса");
            scheduler = new DynamicScheduledExecutorService(1);
            scheduler.scheduleAtFixedRate(this::readFileQueueToSpeech, 1000, 100, TimeUnit.MILLISECONDS);
        }
        audioService.recordSplitStart();
    }

    private void readFileQueueToSpeech() {
        String[] extensions = {"wav"};
        Optional<String> firstFile = FileUtils.listFiles(new File(AudioService.getVoiceFolder()), extensions, false)
                .stream()
                .map(File::getName)
                .sorted()
                .toList()
                .stream()
                .findFirst();
        if (firstFile.isPresent()) {
            String file = firstFile.get();
            URI uri = toUri(AudioService.getVoiceFolder() + file);
            translate.apply(uri);
            microphoneOff.run();
        }
    }

}
