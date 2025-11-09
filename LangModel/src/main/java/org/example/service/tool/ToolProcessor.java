package org.example.service.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.example.langchain.AssistantChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static org.example.service.tool.ToolExecutorReadMessage.ToolReadMessage;
import static org.example.service.tool.ToolExecutorSendMessage.ToolParameterSendMessageText;
import static org.example.service.tool.ToolExecutorSendMessage.ToolParameterSendMessageTo;
import static org.example.service.tool.ToolExecutorSendMessage.ToolSendMessage;
import static org.example.service.tool.ToolExecutorWiki.ToolParameterWikiSearchQuery;
import static org.example.service.tool.ToolExecutorWiki.ToolSearchWiki;

@Service
public class ToolProcessor {

    private static final Logger log = LoggerFactory.getLogger(ToolProcessor.class);

    private ToolExecutorWiki toolExecutorWiki;
    private ToolExecutorSendMessage toolExecutorSendMessage;
    private ToolExecutorReadMessage  toolExecutorReadMessage;

    public ToolProcessor(ToolExecutorWiki toolExecutorWiki,
                         ToolExecutorSendMessage toolExecutorSendMessage,
                         ToolExecutorReadMessage toolExecutorReadMessage) {
        this.toolExecutorWiki = toolExecutorWiki;
        this.toolExecutorSendMessage = toolExecutorSendMessage;
        this.toolExecutorReadMessage = toolExecutorReadMessage;
    }

    private final ToolProvider toolProvider = (toolProviderRequest) -> {
        AssistantChatService assistantChatService = AssistantChatService.getInstance();
        String message = toolProviderRequest.userMessage().singleText();
        ToolProviderResult.Builder toolBuilder = ToolProviderResult.builder();
        if (assistantChatService.textContainsQuestion(message)) {
            log.info("Подготовка tool {}", ToolSearchWiki);
            ToolSpecification toolSpecification = ToolSpecification.builder()
                    .name(ToolSearchWiki)
                    //.description("Returns Wikipedia search result")
                    .parameters(JsonObjectSchema.builder()
                            .addStringProperty(ToolParameterWikiSearchQuery)
                            .build())
                    .build();
            toolBuilder.add(toolSpecification, toolExecutorWiki.getToolExecutor());
        }
        if (assistantChatService.textContainsSendMessage(message)) {
            log.info("Подготовка tool {}", ToolSendMessage);
            ToolSpecification toolSpecification = ToolSpecification.builder()
                    .name(ToolSendMessage)
                    //.description("Returns Wikipedia search result")
                    .parameters(JsonObjectSchema.builder()
                            .addStringProperty(ToolParameterSendMessageTo)
                            .addStringProperty(ToolParameterSendMessageText)
                            .build())
                    .build();

            toolBuilder.add(toolSpecification, toolExecutorSendMessage.getToolExecutor());
        }
        if (assistantChatService.textContainsReadMessage(message)) {
            log.info("Подготовка tool {}", ToolReadMessage);
            ToolSpecification toolSpecification = ToolSpecification.builder()
                    .name(ToolReadMessage)
                    .build();

             toolBuilder.add(toolSpecification, toolExecutorReadMessage.getToolExecutor());
        }
        ToolProviderResult toolProviderResult = toolBuilder.build();
        if (!toolProviderResult.tools().isEmpty()) {
            return toolProviderResult;
        }
        return null;
    };

    public ToolProvider getToolProvider() {
        return toolProvider;
    }

}
