package org.example.face.speech;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.translate.TranslateException;
import javafx.application.Platform;
import org.example.DynamicScheduledExecutorService;
import org.example.RepeatingTask;
import org.example.face.Face;
import org.example.face.FeatureExtraction;
import org.example.face.Person;
import org.example.face.VideoFace2Detection;
import org.example.face.Voice2WishperService;
import org.example.opencv.OpenCVImage;
import org.example.opencv.Utils;
import org.example.langchain.AssistantChatService;
import org.example.service.llm.LLMService;
import org.example.service.GlobalContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class DialogueCycle {

    private enum LifeCycleSateEnum {
        MeetingNewPerson,
        ContinueDialogue,
        GreetingOldFriend,
        MultiplePersons,
        WaitingFace,
        Unknown
    }

    private static final Logger log = LoggerFactory.getLogger(DialogueCycle.class);

    private final ScheduledExecutorService scheduler = new DynamicScheduledExecutorService(1);

    private final VideoFace2Detection videoFace2Detection = new VideoFace2Detection();
    private final FeatureExtraction featureExtraction = new FeatureExtraction();
    private final Voice2WishperService voice2WishperService = new Voice2WishperService();
    private final RepeatingTask repeatingLifeCycleTask = new RepeatingTask(this::runDialogueCycle, 0,200, "lifeCycle");
    private AssistantChatService assistantChatService;
    private final AtomicReference<Face> currentFace = new AtomicReference<>();
    private SpeechFaceController speechFaceController;
    private LifeCycleSateEnum currentState = LifeCycleSateEnum.Unknown;
    private boolean isAgentThinks = false;
    private javafx.scene.image.Image imageThink = null;
    private final AtomicBoolean isPauseDetect =  new AtomicBoolean(false);
    private OpenCVImage tempCurrentFace = null;

    private static final float thresholdSimilar = 0.7f;

    private final Consumer<OpenCVImage> applyImage2View = (OpenCVImage image) -> Utils.apply2view(speechFaceController.getImageView(), image);

    public void init(SpeechFaceController speechFaceController) {
        if (repeatingLifeCycleTask.isStarted()) {
            return;
        }
        //
        LLMService llmService = GlobalContext.getContext().getBean(LLMService.class);
        this.speechFaceController = speechFaceController;
        imageThink = Utils.getImage("think.gif");
        featureExtraction.init();
        videoFace2Detection.start(applyImage2View);
        voice2WishperService.init(this::setUserMessage, this::microphoneOff);
        llmService.initialize();
        assistantChatService = llmService.getAssistantChatService();
        //
        //scheduler.scheduleAtFixedRate(this::runLifeCycle, 1000, 200, TimeUnit.MILLISECONDS);
        repeatingLifeCycleTask.start();
    }

    public void onHandleUserText(String text) {
        userMessage(text);
    }

    public void onHandleMicrophone() {
        voice2WishperService.microphoneOn();
        Platform.runLater(() ->speechFaceController.microphoneButton.setStyle("-fx-background-color: #ff0000"));
    }

    private void microphoneOff() {
        Platform.runLater(() ->speechFaceController.microphoneButton.setStyle("-fx-background-color: #00ff00"));
        voice2WishperService.microphoneOff();
    }

    public void stopSchedule() {
        log.info("остановка планировщика");
        repeatingLifeCycleTask.interrupt();
        scheduler.shutdown();
        videoFace2Detection.stopSchedule();
    }

    public void close() {
        try {
            log.info("закрытие цикла");
            videoFace2Detection.close();
            featureExtraction.close();
            voice2WishperService.stop();
        } catch (Exception e) {
            log.error("ошибка закрытия цикла", e);
            throw new RuntimeException(e);
        }
    }

    public void pauseDetect(boolean selected) {
        isPauseDetect.set(selected);
        if (isPauseDetect.get()) {
            videoFace2Detection.stopSchedule();
        } else {
            videoFace2Detection.start(applyImage2View);
        }
    }

    /*  -----------------------
     *   Dialogue Cycle
     * ----------------------
     */
    private void runDialogueCycle() throws InterruptedException {
        if (isPauseDetect.get()) {
            log.debug("обнаружение лиц приостановлено");
            return;
        }
        log.debug("запуск цикла");
        boolean faceObjectsLock = videoFace2Detection.setDetectedObjectsLock();
        try {
            if (!faceObjectsLock) {
                log.info("не удалось получить доступ к лицам");
                return;
            }
            //
            List<OpenCVImage> detectFaces = videoFace2Detection.getDetectFaces();
            if (!detectFaces.isEmpty()) {
                //List<OpenCVImage> facesImage = videoFace2Detection.getFacesNearest(faceDetect, 0.3);
                videoFace2Detection.setDetectedObjectsUnlock();
                faceObjectsLock = false;
                if (detectFaces.size() == 1) {
                    if (isSameCurrentFace()) {
                        continueCurrentFace();
                    } else if (lookingFamiliarFaceAndSetCurrent(detectFaces.get(0))) {
                        greetingOldFriend(currentFace.get());
                    } else {
                        startMeetingNewPerson();
                    }
                    setFaceImage(detectFaces.get(0));
                } else {
                    detectMultiplePersons();
                }
            } else {
                isNoOne();
            }
        } finally {
            if (faceObjectsLock) {
                videoFace2Detection.setDetectedObjectsUnlock();
            }
        }
    }

    private boolean knowCurrentPerson() {
        return  currentFace.get() != null && currentFace.get().getPerson() != null;
    }

    private void continueCurrentFace() {
        if (knowCurrentPerson()) {
            setCurrentState(LifeCycleSateEnum.ContinueDialogue);
            log.debug("лицо не изменилось и уже знакомое, продолжение диалога");
        } else {
            //setCurrentState(LifeCycleSate.MeetingNewPerson);
            log.debug("лицо не изменилось, но не знакомо, продолжение знакомства с новым человеком");
        }
    }

    private void greetingOldFriend(Face face) {
        if (face == null || face.getPerson() == null) {
           log.info("ошибка, человек не определен");
           return;
        }
        if (currentState != LifeCycleSateEnum.GreetingOldFriend) {
            setCurrentState(LifeCycleSateEnum.GreetingOldFriend);
            log.info("приветствие старого друга {}", face);
            clearMessages();
            face.setMeetingTime(LocalDateTime.now());
            Person person = face.getPerson();
            int memoryId = face.getId();
            runCommand(() -> {
                speechFaceController.getAgentText().appendText(getDelimiter());
                setAgentThink(true);
                try {
                    assistantChatService.greetingOldFriendStreaming(memoryId, person.getFullName(),
                            (text) -> speechFaceController.getAgentText().appendText(text));
                    speechFaceController.getAgentText().appendText(getDelimiter());
                } finally {
                    setAgentThink(false);
                    setCurrentState(LifeCycleSateEnum.ContinueDialogue);
                }
            });
        }
    }

    private void detectMultiplePersons() {
        if (currentState != LifeCycleSateEnum.MultiplePersons) {
            setCurrentState(LifeCycleSateEnum.MultiplePersons);
            clearMessages();
            faceReset();
            voice2WishperService.microphoneOff();
            runCommand(() -> {
                log.info("Обнаружено несколько лиц");
                setAgentThink(true);
                try {
                    TimeUnit.MILLISECONDS.sleep(300);
                    speechFaceController.getAgentText().appendText(assistantChatService.agentMultiplePersons() + getDelimiter());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    setAgentThink(false);
                }
            });
        }
    }

    private void startMeetingNewPerson() {
        if (currentState != LifeCycleSateEnum.MeetingNewPerson) {
            setCurrentState(LifeCycleSateEnum.MeetingNewPerson);
            faceReset();
            clearMessages();
            //setCurrentFace(new Face((OpenCVImage) facesImage.duplicate(), GlobalContext.nextFaceId()));
            agentWelcomeMeetingNewPerson();
        }
    }

    private void setFaceImageAndName(OpenCVImage faceImage, String faceName) {
        Platform.runLater(() -> {
            speechFaceController.getImageFace().setImage(faceImage == null ? null : Utils.img2fx(faceImage));
            speechFaceController.getFaceName().setText(faceName);
        });
    }

    private void clearMessages() {
        Platform.runLater(() -> {
            speechFaceController.getAgentText().clear();
            speechFaceController.getFaceText().clear();
        });
    }

    private void setUserMessage(String message) {
        Platform.runLater(() ->speechFaceController.userText.setText(message));
    }

    private void userMessage(String message) {
        if (isAgentThinks) {
            agentThinks();
            return;
        }

        switch (currentState) {
            case MeetingNewPerson:
                setMyName(message);
                break;
            case ContinueDialogue:
                continueDialogue(message);
                break;
            default:
                log.info("Неизвестный статус {}", currentState);
                break;
        }
    }

    private void setMyName(String message) {
        speechFaceController.getFaceText().appendText(message + getDelimiter());
        //
        runCommand(() -> {
            setAgentThink(true);
            try {
                Person person = assistantChatService.agentDetectPersonInfo(message);
                if (person != null && person.firstName() != null && person.lastName() != null) {
                    if (currentFace.get() == null /*&& currentFace.get().getPerson() == null*/) {
                        Face face = new Face(tempCurrentFace, GlobalContext.nextFaceId());
                        setCurrentFace(face);
                        face.setPerson(person, LocalDateTime.now());
                        log.info("новый человек {}", currentFace.get());
                        Platform.runLater(() -> {
                            speechFaceController.getFaceName().setText(person.firstName() + " " + person.lastName());
                        });
                        //
                        assistantChatService.niceMeetYou(currentFace.get().getId(), person,
                                (text) -> speechFaceController.getAgentText().appendText(text));
                        Platform.runLater(() -> speechFaceController.getAgentText().appendText(getDelimiter()));
                        GlobalContext.getAllFaces().add(currentFace.get());
                        //
                        log.info("Успешно добавлен новый человек {}", currentFace.get());
                        setCurrentState(LifeCycleSateEnum.ContinueDialogue);
                    } else {
                        log.info("Ошибка, человек не определен");
                    }
                } else {
                    noUnderstandYourName(message);
                }
            } finally {
                setAgentThink(false);
            }
        });
    }

    private void agentThinks() {
        speechFaceController.getAgentText().appendText("Подождите, я думаю..." + getDelimiter());
    }

    private void noUnderstandYourName(String message) {
        runCommand(() -> {
            log.info("не понял имя");
            setAgentThink(true);
            speechFaceController.getAgentText().appendText(assistantChatService.noUnderstandYourName(message) + getDelimiter());
            setAgentThink(false);
        });
    }

    private void isNoOne() throws InterruptedException {
        if (currentState != LifeCycleSateEnum.WaitingFace) {
            setCurrentState(LifeCycleSateEnum.WaitingFace);
            agentIsNoOneRepeat();
        }
    }

    private void agentIsNoOneRepeat() throws InterruptedException {
        int attempts = 0;
        for  (int i = 0; i < 3; i++) {
            TimeUnit.MILLISECONDS.sleep(100);
            boolean faceObjectsLock = videoFace2Detection.setDetectedObjectsLock();
            if (faceObjectsLock) {
                List<OpenCVImage> detectFaces = videoFace2Detection.getDetectFaces();
                if (!detectFaces.isEmpty()) {
                    attempts ++;
                } else {
                    attempts --;
                }
                videoFace2Detection.setDetectedObjectsUnlock();
            }
        }
        boolean isNoOne = attempts < 0;
        if (isNoOne) {
            log.info("никого не обнаружено");
            resetBeforeWaitingPerson();
            setAgentThink(true);
            speechFaceController.getAgentText().appendText(assistantChatService.IsNoOne() + getDelimiter());
            setAgentThink(false);
        } else {
            log.info("появилось лицо");
        }
    }

    private void agentWelcomeMeetingNewPerson() {
        log.info("начало знакомства c новым человеком");
        runCommand(() -> {
            setAgentThink(true);
            try {
                TimeUnit.MILLISECONDS.sleep(300);
                speechFaceController.getAgentText().appendText(assistantChatService.agentMeetingNewPersonWelcome() + getDelimiter());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                setAgentThink(false);
            }
        });
    }

    private void continueDialogue(String message) {
        if (currentFace.get() != null) {
            runCommand(() -> speechFaceController.getFaceText().appendText(message + getDelimiter()));
            runCommand(() -> {
                log.info("продолжение диалога {}", message);
                setAgentThink(true);
                try {
                    Consumer<String> response = (text) -> speechFaceController.getAgentText().appendText(text);
                    assistantChatService.agentContinueDialogueStreaming(currentFace.get().getId(), message, response);
                    speechFaceController.getAgentText().appendText(getDelimiter());
                } finally {
                    setAgentThink(false);
                }
            });
        } else {
            log.info("Ошибка, человек не определен");
        }
    }

    private boolean isSameCurrentFace() throws InterruptedException {
        if (currentFace.get() == null || currentFace.get().getImageFace() == null) {
            return false;
        }
        return isSameCurrentFaceRepeatTimes(currentFace.get().getImageFace());
    }

    private boolean isSameCurrentFaceRepeatTimes(OpenCVImage currentFace) throws InterruptedException {
        int attempts = 0;
        for (int i = 0; i < 3; i++) {
            TimeUnit.MILLISECONDS.sleep(100);
            boolean faceObjectsLock = videoFace2Detection.setDetectedObjectsLock();
            try {
                if (!faceObjectsLock) {
                    log.info("не удалось получить доступ к лицам");
                    continue;
                }
                List<OpenCVImage> detectFaces = videoFace2Detection.getDetectFaces();
                if (detectFaces.size() != 1) {
                    videoFace2Detection.setDetectedObjectsUnlock();
                    continue;
                }
                List<OpenCVImage> facesImage = videoFace2Detection.getDetectFaces();
                OpenCVImage cameraFace = facesImage.get(0);

                try (Predictor<Image, float[]> predictor1 = featureExtraction.predictor()) {
                    try (Predictor<Image, float[]> predictor2 = featureExtraction.predictor()) {
                        try {
                            float[] predictFace = predictor1.predict(cameraFace);
                            float[] predictInputFace = predictor2.predict(currentFace);
                            float similar = featureExtraction.calculSimilar(predictFace, predictInputFace);
                            if (similar >= thresholdSimilar) {
                                attempts++;
                                log.debug("лицо не изменилось {}, попытка {}", similar, attempts);
                                //return true;
                            } else {
                                attempts--;
                                log.info("другое лицо {}, попытка {}", similar, attempts);
                            }
                        } catch (TranslateException e) {
                            log.error("ошибка сравнения лиц", e);
                        }
                    }
                }
            } finally {
                if (faceObjectsLock) {
                    videoFace2Detection.setDetectedObjectsUnlock();
                }
            }
        }
        return attempts > 0;
    }

    private boolean lookingFamiliarFaceAndSetCurrent(OpenCVImage inputFace) {
        for (Face face : GlobalContext.getAllFaces()) {
            if (face.getPerson() == null) {
                continue;
            }
            OpenCVImage imageFace = face.getImageFace();
            try (Predictor<Image, float[]> predictor1 = featureExtraction.predictor()) {
                try (Predictor<Image, float[]> predictor2 = featureExtraction.predictor()) {
                    try {
                        float[] predictFace = predictor1.predict(imageFace);
                        float[] predictInputFace = predictor2.predict(inputFace);
                        float similar = featureExtraction.calculSimilar(predictFace, predictInputFace);
                        if (similar >= thresholdSimilar) {
                            setCurrentFace(face);
                            //setFaceImage(currentFace.get().getImageFace(), currentFace.get().getPerson().getFullName());
                            log.info("найден знакомый человек {}, {}", similar, face);
                            return true;
                        }
                    } catch (TranslateException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        long count = GlobalContext.getAllFaces().stream().filter(face -> face.getPerson() != null).count();
        log.debug("не найден знакомый человек, всего знакомых {}", count);
        return false;
    }

    private void resetBeforeWaitingPerson() {
        clearMessages();
        faceReset();
    }

    private void faceReset() {
        setCurrentFace(null);
    }

    private void setCurrentState(LifeCycleSateEnum state) {
        if (currentState != state) {
            log.info("setCurrentState, состояние прежнее {}, новое {}", currentState, state);
            currentState = state;
        }
    }

    private void setCurrentFace(Face face) {
        log.info("setCurrentFace, установка текущего лица, прежнее {}, новое {}", currentFace.get(), face);
        GlobalContext.setCurrentFace(face);
        currentFace.set(face);
        tempCurrentFace = face == null ? null : face.getImageFace();
        setFaceImageAndName(face == null ? null : face.getImageFace(),
                face == null || face.getPerson() == null ? "?": face.getPerson().getFullName());
    }

    private void setFaceImage(OpenCVImage image) {
        if (currentFace.get() != null && currentFace.get().getImageFace() != null) {
            return;
        }
        tempCurrentFace = image;
        Platform.runLater(() -> {
            speechFaceController.getImageFace().setImage(image == null ? null : Utils.img2fx(image));
        });
    }

    private void setAgentThink(boolean isThink) {
        if (isThink) {
            Platform.runLater(() -> speechFaceController.imageAgentBusy.setImage(imageThink));
        } else {
            Platform.runLater(() -> speechFaceController.imageAgentBusy.setImage(null));
        }
        isAgentThinks = isThink;
    }

    private void runCommand(Runnable command) {
        new Thread(command).start();
    }

    private String getDelimiter() {
        return  "\n-----------------------\n";
    }
}
