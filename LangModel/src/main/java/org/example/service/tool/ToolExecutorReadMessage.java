package org.example.service.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.service.tool.ToolExecutor;
import org.apache.commons.collections4.CollectionUtils;
import org.example.model.Message;
import org.example.service.GlobalContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static org.example.lang.model.ToolResult.toolResultOk;

@Service
public class ToolExecutorReadMessage {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutorReadMessage.class);

    public static final String ToolReadMessage = "read_new_message";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MessageService messageService;
    private ToolFeedback toolFeedback;

    public ToolExecutorReadMessage(MessageService messageService,
                                   ToolFeedback toolFeedback) {
        this.messageService = messageService;
        this.toolFeedback = toolFeedback;
    }

    private final ToolExecutor toolExecutor = (toolExecutionRequest, memoryId) -> {
        try {
            toolFeedback.execute("Tool Read Message");
            List<Message> messages = messageService.readMessages(GlobalContext.getCurrentFace().getId());
            if (CollectionUtils.isEmpty(messages)) {
                return objectMapper.writeValueAsString(toolResultOk("Нет новых сообщений"));
            }

            String message = "Мои новые сообщения: " + messages.stream().map(m ->
                            String.format("От кого: %s, Дата: %s, Текст: %s", m.getFrom(), m.getTime(), m.getMessage()))
                    .collect(Collectors.joining("\n"));
            log.info("ToolExecutorReadMessage {}", message);
            return objectMapper.writeValueAsString(toolResultOk(message));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    };

    public ToolExecutor getToolExecutor() {
        return toolExecutor;
    }
}
