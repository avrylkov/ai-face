package org.example.service.tool;

import org.example.face.Face;
import org.example.model.Message;
import org.example.service.GlobalContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    public String sendMessage(Integer toFaceId, Integer fromFaceId, String text) {
        Face toFace = GlobalContext.getFaceById(toFaceId);
        Face fromFace = GlobalContext.getFaceById(fromFaceId);
        if (toFace == null || fromFace == null) {
            return null;
        }
        toFace.getMessages().add(new Message(text, fromFace.getPerson().getFullName(), LocalDateTime.now(), false));
        return "Сообщение отправлено, Кому: " + toFace.getPerson().getFullName() + ", Текст: " + text;
    }

    public List<Message> readMessages(Integer faceId) {
        Face face = GlobalContext.getFaceById(faceId);
        if (face == null) {
            return Collections.emptyList();
        }
        return face.getMessages().stream().filter(m -> !m.isRead())
                .peek(m -> m.setRead(true))
                .collect(Collectors.toCollection(ArrayList::new));
    }

}
