package org.example;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AudioService {

    private static String voiceFolder;

    private static final int silenceThreshold = 1000; // Adjust based on your needs
    private static final int silenceDurationMs = 1000; // Minimum duration of silence to detect
    private static final int delta = 10000;
    private static final int pauseLengthMs = 3000;
    private final Map<Integer, Integer> audioMap = new HashMap<>(); //<start, end

    private static final Logger log = LoggerFactory.getLogger(AudioService.class);
    private final ReentrantLock recordStopLock = new ReentrantLock();

    private ByteArrayOutputStream outputStream;

    private final AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
    ;
    private final int bytesPerSample = format.getFrameSize() / format.getChannels();
    private final int samplesPerSecond = (int) format.getSampleRate();

    private TargetDataLine line;
    private int bufferLengthInBytes;
    private boolean isRecording = false;
    private Runnable fileReady;

    public AudioService(Runnable fileReady) {
        this.fileReady = fileReady;
    }

    public void init() {
        File file = new File(CommonProperties.INSTANCE().getVoiceFolder());
        if (!file.exists()) {
            try {
                FileUtils.forceMkdir(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        voiceFolder = file.getAbsolutePath() + "/";
        line = getTargetDataLine(format);
        outputStream = new ByteArrayOutputStream();
        int frameSizeInBytes = format.getFrameSize();
        int bufferLengthInFrames = line.getBufferSize() / 8;
        bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
    }

    public static String getVoiceFolder() {
        return voiceFolder;
    }

    public void recordSplitStart() {
        if (!isRecording) {
            isRecording = true;
            VoiceRecorderSplitter voiceRecorderSplitter = new VoiceRecorderSplitter();
            voiceRecorderSplitter.start();
        }
    }

    public void recordSplitStop() {
        isRecording = false;
    }

    public void startLine() {
        init();
        VoiceRecorder recorder = new VoiceRecorder();
        recorder.start();
    }

    public void stopLine() {
        isRecording = false;
        recordStopLock.lock();
        try {
            byte[] audioBytes = outputStream.toByteArray();
            writeAudioData(audioBytes, "recorded-1.wav");
            //
            List<int[]> detectSilence = detectSilence(audioBytes);
            log.debug("Detected silence {}", detectSilence);
            byte[] removeSilence = removeSilence(audioBytes, detectSilence);
            writeAudioData(removeSilence, "removed.wav");
            line.close();
        } finally {
            recordStopLock.unlock();
        }
    }

    public void writeAudioMap(byte[] audioBytes, String fileName) {
        for (int i = 0; i < audioBytes.length - bytesPerSample; i += bytesPerSample) {
            int sample = 0;
            if (bytesPerSample == 1) {
                sample = audioBytes[i];
            } else if (bytesPerSample == 2) {
                sample = (audioBytes[i] & 0xFF) | (audioBytes[i + 1] << 8);
                sample = (short) sample;
            }

            int i_d = (int)((double) i / 100.0) * 100;
            int v_d = Math.abs((int)((double) sample / 100.0) * 100);
            audioMap.put(i_d, v_d);
        }
        String csvString = audioMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map((entry) -> entry.getKey() + ";" + entry.getValue() + "\n")
                .collect(Collectors.joining());
        try {
            FileUtils.writeStringToFile(new File(fileName), csvString, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<int[]> detectSilence(byte[] audioBytes) {
        List<int[]> silencePeriods = new ArrayList<>();
        int silenceStartIndex = -1;
        for (int i = 0; i < audioBytes.length - bytesPerSample; i += bytesPerSample) {
            int sample = 0;
            if (bytesPerSample == 1) {
                sample = audioBytes[i];
            } else if (bytesPerSample == 2) {
                sample = (audioBytes[i] & 0xFF) | (audioBytes[i + 1] << 8);
                sample = (short) sample;
            }

            if (Math.abs(sample) < silenceThreshold) {
                if (silenceStartIndex == -1) {
                    silenceStartIndex = i;
                }
            } else {
                if (silenceStartIndex != -1) {
                    int silenceLengthMs = getSilenceLengthMs(silenceStartIndex, i);
                    if (silenceLengthMs >= silenceDurationMs) {
                        silencePeriods.add(new int[]{silenceStartIndex, i});
                    }
                    silenceStartIndex = -1;
                }
            }
        }
        if (silenceStartIndex != -1 && silenceStartIndex < audioBytes.length - bytesPerSample) {
            int silenceLengthMs = getSilenceLengthMs(silenceStartIndex, audioBytes.length - bytesPerSample);
            if (silenceLengthMs >= silenceDurationMs) {
                silencePeriods.add(new int[]{silenceStartIndex, audioBytes.length - bytesPerSample});
                log.debug("There is silence at the end [{}, {}]", silenceStartIndex, audioBytes.length - bytesPerSample);
            }
        }
        return silencePeriods;
    }

    public byte[] removeSilence(byte[] audioBytes, List<int[]> silencePeriods) {
        byte[] outputAudio = new byte[0];
        for (int i = 0; i < silencePeriods.size(); i++) {
            if (i < silencePeriods.size() - 1) {
                int start = silencePeriods.get(i)[1] - delta;
                if (start < 0) {
                    start = silencePeriods.get(i)[1];
                }
                int end = silencePeriods.get(i + 1)[0] + delta;
                if (end > audioBytes.length) {
                    end = silencePeriods.get(i + 1)[0];
                }
                byte[] temp = new byte[outputAudio.length + (end - start)];
                System.arraycopy(outputAudio, 0, temp, 0, outputAudio.length);
                System.arraycopy(audioBytes, start, temp, outputAudio.length, (end - start));
                outputAudio = temp;
                log.debug("voice1 [{}, {}]", start, end);
            } else {
                if ((i + 1) % 2 == 0) {
                    int start = silencePeriods.get(i)[1] - delta;
                    if (start < 0 ) {
                        start = silencePeriods.get(i)[1];
                    }
                    byte[] temp = new byte[outputAudio.length + (audioBytes.length - start)];
                    System.arraycopy(outputAudio, 0, temp, 0, outputAudio.length);
                    System.arraycopy(audioBytes, start, temp, outputAudio.length, audioBytes.length - start);
                    outputAudio = temp;
                    log.debug("voice2 from [{}, {}]", start, audioBytes.length);
                }
            }
        }

        return outputAudio;
    }

    public void writeAudioData(byte[] audioBytes, String filename) {
        ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
        AudioInputStream removeSilenceAudioInputStream = new AudioInputStream(bais, format, audioBytes.length / format.getFrameSize());
        //
        File wavFile = new File(filename/*"recording.wav"*/);
        try {
            //log.debug("Saving recording to {}", wavFile.getAbsolutePath());
            AudioSystem.write(removeSilenceAudioInputStream, AudioFileFormat.Type.WAVE, wavFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TargetDataLine getTargetDataLine(AudioFormat format) {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        try {
            return (TargetDataLine) AudioSystem.getLine(info);
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }


    private int getSilenceLengthMs(int silenceStartId, int silenceEndId) {
        return getMs(silenceEndId) - getMs(silenceStartId);
    }

    private int getMs(int index) {
        return (index / bytesPerSample) * 1000 / samplesPerSecond;
    }


    class VoiceRecorderSplitter extends Thread {

        @Override
        public void run() {
            log.info("Начало обработки аудио...");
            Counter counter = new Counter();
            try {
                recordStopLock.lock();
                line.open(format, line.getBufferSize());
                line.start();
                byte[] buffer = new byte[bufferLengthInBytes];
                int bytesRead;
                while (isRecording) {
                    if ((bytesRead = line.read(buffer, 0, bufferLengthInBytes)) == -1) {
                        break;
                    }
                    outputStream.write(buffer, 0, bytesRead);
                    byte[] audioBytes = outputStream.toByteArray();
                    writePartAudio(audioBytes, counter);
                }
//                byte[] audioBytes = outputStream.toByteArray();
//                writePartAudio(audioBytes, counter);
                line.close();
                log.info("Остановка обработки аудио");
            } catch (LineUnavailableException e) {
                throw new RuntimeException(e);
            } finally {
                recordStopLock.unlock();
            }
        }
    }

    private void writePartAudio(byte[] audioBytes, Counter counter) {
        List<int[]> detectSilence = detectSilence(audioBytes);
        if (CollectionUtils.isEmpty(detectSilence)) {
            log.info("Пауза не обнаружена в аудио потоке: {}", meanAbsolute16LE(audioBytes));
        }
        log.debug("detectSilence: {}", detectSilence);
        for (int[] silencePeriod : detectSilence) {
            int lengthMs = getSilenceLengthMs(silencePeriod[0], silencePeriod[1]);
            if (lengthMs > pauseLengthMs) {
                log.info("detect pause ...");
                if (detectSilence.size() == 1 && detectSilence.get(0)[0] == 0) {
                    log.debug("Detected only silence {}", counter.count);
                    outputStream.reset();
                    break;
                }
                //byte[] removeSilence = removeSilence(audioBytes, detectSilence);
                String file = voiceFolder + "sound" + StringUtils.leftPad(String.valueOf(counter.count), 3, "0") + ".wav";
                //writeAudioData(removeSilence, file);
                writeAudioData(audioBytes, file);
                log.info("Записан файл {}", file);
                counter.count++;
                outputStream.reset();
                fileReady.run();
                break;
            }
        }
    }

    private double meanAbsolute16LE(byte[] audioBytes) {
        int samples = audioBytes.length / 2;
        if (samples == 0) return 0.0;
        double sumAbs = 0;
        for (int i = 0; i < samples; i++) {
            int lo = audioBytes[2 * i] & 0xFF;
            int hi = audioBytes[2 * i + 1];
            int sample = (hi << 8) | lo;
            sumAbs += Math.abs(sample);
        }
        return sumAbs / samples;
    }

    private static class Counter {
        public int count = 1;
    }

    private class VoiceRecorder extends Thread {

        @Override
        public void run() {
            log.debug("Starting voice recording...");
            try {
                recordStopLock.lock();
                line.open(format, line.getBufferSize());
                line.start();
                byte[] buffer = new byte[bufferLengthInBytes];
                int bytesRead;
                while (isRecording) {
                    if ((bytesRead = line.read(buffer, 0, bufferLengthInBytes)) == -1) {
                        break;
                    }
                    outputStream.write(buffer, 0, bytesRead);
                }
                line.close();
                log.debug("Finished voice recording.");
            } catch (LineUnavailableException e) {
                throw new RuntimeException(e);
            } finally {
                recordStopLock.unlock();
            }
        }
    }

}
