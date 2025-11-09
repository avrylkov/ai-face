package org.example.service.llm;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.langchain4j.GigaChatChatModel;
import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.langchain4j.GigaChatStreamingChatModel;
import chat.giga.model.ModelName;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import org.example.langchain.AssistantChatService;
import org.example.service.tool.ToolProcessor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import static chat.giga.model.Scope.GIGACHAT_API_PERS;
import static org.example.service.llm.GigaChatClientService.API_KEY;

@Service
@Profile("giga")
public class GigaService extends LLMServiceBase implements LLMService {

    public GigaService(ToolProcessor toolProcessor) {
        this.toolProcessor = toolProcessor;
    }

    public void initialize() {
        model = GigaChatChatModel.builder()
                .verifySslCerts(false)
                .authClient(AuthClient.builder().withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .authKey(API_KEY)
                                .scope(GIGACHAT_API_PERS)
                                .build())
                        .build())
                .logRequests(true)
                .logResponses(true)
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        //.responseFormat(ResponseFormat.JSON)
                        .modelName(ModelName.GIGA_CHAT_2)
                        .functionCall("auto")
                        .build())
                .build();
        //
        streamingModel = GigaChatStreamingChatModel.builder()
                .authClient(AuthClient.builder()
                        .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .scope(GIGACHAT_API_PERS)
                                .authKey(API_KEY)
                                .build())
                        .build())
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_2)
                        //.responseFormat(ResponseFormat.TEXT)
                        .responseFormat(JsonSchema.builder()
                                .rootElement(new JsonStringSchema())
                                .build())
                        .build())
                .verifySslCerts(false)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Override
    public AssistantChatService getAssistantChatService() {
        return super.getAssistantChatService();
    }


}

