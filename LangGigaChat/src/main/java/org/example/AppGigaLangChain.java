package org.example;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.langchain4j.GigaChatChatModel;
import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.ModelName;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static chat.giga.model.Scope.GIGACHAT_API_PERS;

public class AppGigaLangChain
{
    private static Logger log = LogManager.getLogger(AppGigaLangChain.class);

    interface Assistant {

        @SystemMessage("Отвечай, используя сленг.")
        String chat(String userMessage);
    }

    interface AssistantMemory {
        String chat(@MemoryId int memoryId, @dev.langchain4j.service.UserMessage String message);
        //String chat(String message);
    }

    public static void main( String[] args ) {

        try {
            GigaChatChatModel model = GigaChatChatModel.builder()
                    .verifySslCerts(false)
                    .authClient(AuthClient.builder().withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                    .authKey("MmZhNTA2OTYtNzUzZC00NWY1LWFkMGItYmY0YjczZjI1MzBjOmM0ZmRhNjFlLWI5YmYtNDVmZS1iOGRmLWZhYzU3MThmYTUyNQ==")
                                    .scope(GIGACHAT_API_PERS)
                                    .build())
                            .build())
                    .logRequests(true)
                    .logResponses(true)
                    .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                            //.responseFormat(ResponseFormat.JSON)
                            .modelName(ModelName.GIGA_CHAT_2)
                            .build())
                    .build();
            ChatResponse chatResponse = model.chat(ChatRequest.builder().messages(new UserMessage("как дела?"))
                    .build());
            System.out.println(chatResponse.aiMessage().text());
            //
            Assistant assistantWithSystemMessge = AiServices.create(Assistant.class, model);
            String chat = assistantWithSystemMessge.chat("Как дела?");
            System.out.println(chat);
            //
            ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
            AssistantMemory assistantWithChatMemory = AiServices.builder(AssistantMemory.class)
                    .chatLanguageModel(model)
                    //.chatMemory(chatMemory)
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .build();

            System.out.println(assistantWithChatMemory.chat(1,"Привет, мення завут Александр"));
            System.out.println(assistantWithChatMemory.chat(2,"Привет, мення завут Иван"));
            //
            System.out.println(assistantWithChatMemory.chat(1,"Ты помнишь как мое имя?"));
            System.out.println(assistantWithChatMemory.chat(2,"Ты помнишь как мое имя?"));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
