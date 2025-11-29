package org.example.service.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.service.tool.ToolExecutor;
import org.apache.commons.lang3.StringUtils;
import org.example.langchain.AssistantChatService;
import org.example.service.GlobalContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.example.lang.model.ToolResult.toolResultNegative;
import static org.example.lang.model.ToolResult.toolResultOk;

@Service
public class ToolExecutorSendMessage {

    public static final String ToolSendMessage = "send_message";
    public static final String ToolParameterSendMessageTo = "send_to";
    public static final String ToolParameterSendMessageText = "send_text";

    private static final Logger log = LoggerFactory.getLogger(ToolExecutorSendMessage.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MessageService messageService;
    private ToolFeedback toolFeedback;

    public ToolExecutorSendMessage(MessageService messageService,
                                   ToolFeedback toolFeedback) {
        this.messageService = messageService;
        this.toolFeedback = toolFeedback;
    }

    private final ToolExecutor toolExecutor = (toolExecutionRequest, memoryId) -> {
        AssistantChatService assistantChatService = AssistantChatService.getInstance();
        try {
            toolFeedback.execute("Tool Send Message");
            Map<String, Object> arguments = objectMapper.readValue(toolExecutionRequest.arguments(), Map.class);
            log.info("ToolExecutorSendMessage arguments {}", arguments);
            String to = (String) arguments.get(ToolParameterSendMessageTo);
            if (StringUtils.isEmpty(to)) {
                return objectMapper.writeValueAsString(toolResultNegative("Не понял кому отправить сообщение"));
            }
            String text = (String) arguments.get(ToolParameterSendMessageText);
            if (StringUtils.isEmpty(text)) {
                return objectMapper.writeValueAsString(toolResultNegative("Не понял какое сообщение отправить"));
            }

            String allFaces = allFacesToString();
            Integer toFaceId = assistantChatService.getFaceIdFromAllFaces(allFaces, to);
            if (toFaceId != null && !toFaceId.equals(GlobalContext.getCurrentFace().getId())) {
                String result = messageService.sendMessage(toFaceId, GlobalContext.getCurrentFace().getId(), text);
                if (result != null) {
                    return objectMapper.writeValueAsString(toolResultOk(result));
                } else {
                    return objectMapper.writeValueAsString(toolResultNegative("Сообщение не отправлено  " + to));
                }
            } else {
                return objectMapper.writeValueAsString(toolResultNegative("Не смог найти " + to));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    };

    public ToolExecutor getToolExecutor() {
        return toolExecutor;
    }

    private String allFacesToString() {
        return GlobalContext.getAllFaces()
                .stream()
                .filter(Objects::nonNull)
                .filter(f -> f.getPerson() != null)
                .map(g -> g.getPerson().getFullName() + ":" + g.getId())
                .collect(Collectors.joining("\n"));
    }

}
