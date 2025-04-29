package org.example;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.langchain4j.GigaChatChatModel;
import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.ModelName;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static chat.giga.model.Scope.GIGACHAT_API_PERS;

public class AppGigaMessage
{
    private static Logger log = LogManager.getLogger(AppGigaMessage.class);

    interface Assistant {

        @SystemMessage("Отвечай, используя сленг.")
        String chat(String userMessage);
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

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
