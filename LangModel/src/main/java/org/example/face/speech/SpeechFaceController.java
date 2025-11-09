package org.example.face.speech;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import org.example.CommonProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeechFaceController {

    public static final String FxmlBeanController = "fxmlController";

    private static final Logger log = LoggerFactory.getLogger(SpeechFaceController.class);

    public TextArea agentText;
    public TextArea faceText;
    public Label faceName;
    public TextField userText;
    public ImageView imageAgentBusy;
    public Button microphoneButton;
    public ListView<String> toolFeedBack;

    @FXML
    private ImageView imageView;
    @FXML
    private ImageView imageFace;

    private final DialogueCycle dialogueCycle = new DialogueCycle();

    public void initialize() {
        if (CommonProperties.INSTANCE().isAutoStart()) {
            log.info("initialize");
            dialogueCycle.init(this);
        }
    }

    public void onHandleChat(MouseEvent mouseEvent) {
        log.info("onClick");
        dialogueCycle.onHandleUserText(userText.getText());
    }

    public void onActionHandleChat(ActionEvent actionEvent) {
        dialogueCycle.onHandleUserText(userText.getText());
    }

    public void stopSchedule() {
        dialogueCycle.stopSchedule();
    }

    public void close() {
        dialogueCycle.close();
    }

    public ImageView getImageView() {
        return imageView;
    }

    public ImageView getImageFace() {
        return imageFace;
    }

    public TextArea getAgentText() {
        return agentText;
    }

    public TextArea getFaceText() {
        return faceText;
    }

    public Label getFaceName() {
        return faceName;
    }


    public void onHandleMicrophoneOn(MouseEvent mouseEvent) {
        dialogueCycle.onHandleMicrophone();
    }

    public void onStart(ActionEvent actionEvent) {
        log.info("initialize");
        dialogueCycle.init(this);
    }

}
