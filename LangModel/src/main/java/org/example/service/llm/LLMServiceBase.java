package org.example.service.llm;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.example.langchain.AssistantChatService;
import org.example.langchain.AssistantMemoryChatStream;
import org.example.langchain.PersistentChatMemoryStore;
import org.example.service.AssistantChat;
import org.example.service.tool.ToolProcessor;

public abstract class LLMServiceBase {

    public static final int MaxMessages = 5;

    protected StreamingChatModel streamingModel;
    protected ChatModel model;
    protected ToolProcessor toolProcessor;

    protected ChatMemoryProvider createChatMemoryStore() {
        PersistentChatMemoryStore chatMemoryStore = PersistentChatMemoryStore.getInstance();

        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(MaxMessages)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }

    protected AssistantChatService getAssistantChatService() {
        if (AssistantChatService.isInitialized()) {
            return AssistantChatService.getInstance();
        }
        AssistantMemoryChatStream memoryChatStreamTool = AiServices.builder(AssistantMemoryChatStream.class)
                .streamingChatModel(streamingModel)
                .chatMemoryProvider(createChatMemoryStore())
                .toolProvider(toolProcessor.getToolProvider())
                .build();
        //
        AssistantMemoryChatStream memoryChatStream = AiServices.builder(AssistantMemoryChatStream.class)
                .streamingChatModel(streamingModel)
                .chatMemoryProvider(createChatMemoryStore())
                .build();
        //
        AssistantChat assistantChat = AiServices.builder(AssistantChat.class)
                .chatModel(model)
                .build();
        //
        return AssistantChatService.INSTANCE(assistantChat, memoryChatStreamTool, memoryChatStream);
    }

}
