package org.example.service.llm;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.example.langchain.AssistantChatService;
import org.example.service.tool.ToolProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!giga")
public class OllamaService extends LLMServiceBase implements LLMService {

    private static final Logger log = LoggerFactory.getLogger(OllamaService.class);
    private static final boolean isLogRequest = true;

    public OllamaService(ToolProcessor toolProcessor) {
        this.toolProcessor = toolProcessor;
    }

    public void initialize() {
        if (streamingModel == null) {
            streamingModel = OllamaStreamingChatModel.builder()
//                .modelName("PetrosStav/gemma3-tools:12b")
//                    .modelName("PetrosStav/gemma3-tools:4b")
                .modelName("mix_77/gemma3-qat-tools:4b")
                    .baseUrl("http://localhost:11434")
                    .logRequests(isLogRequest)
                    .logResponses(isLogRequest)
                    .build();
        }
        if (model == null) {
            model = OllamaChatModel.builder()
//                .modelName("gemma3:12b")
//                .modelName("PetrosStav/gemma3-tools:12b")
//                    .modelName("PetrosStav/gemma3-tools:4b")
                .modelName("mix_77/gemma3-qat-tools:4b")
                    .baseUrl("http://localhost:11434")
                    .logRequests(isLogRequest)
                    .logResponses(isLogRequest)
                    .build();
        }

    }

    @Override
    public AssistantChatService getAssistantChatService() {
        return super.getAssistantChatService();
    }


}
