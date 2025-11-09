package org.example.service.llm;

import org.example.langchain.AssistantChatService;
import org.example.langchain.AssistantMemoryChatStream;

public interface LLMService {

    void initialize();

    AssistantChatService getAssistantChatService();

}
