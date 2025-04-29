package org.example;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.langchain4j.GigaChatChatModel;
import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.ModelName;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static chat.giga.model.Scope.GIGACHAT_API_PERS;

public class AppGigaTool
{
    private static Logger log = LogManager.getLogger(AppGigaTool.class);

    static class FunctionCallingService {
        record Transaction(String id) { }
        record Status(String name) { }

        private static final Map<Transaction, Status> DATASET = Map.of(
                new Transaction("001"), new Status("pending"),
                new Transaction("002"), new Status("approved"),
                new Transaction("003"), new Status("rejected"));

        @Tool("Получить статус платежной транзакции")
        public Status paymentStatus(@P("Идентификатор платежной транзакции") String transaction) {
            System.out.println();
            return DATASET.get(new Transaction(transaction));
        }
    }

    interface Assistant {
        @SystemMessage("Вы полезный помощник, который может ответить на вопросы о платежных транзакциях.")
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

            FunctionCallingService service = new FunctionCallingService();

            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatLanguageModel(model)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .tools(service)
                    .build();
            //
            String userMessage = "Ответьте на следующие вопросы: " +
                    "Каков статус моих платежных транзакций 002, 001, 003?\n" +
                    "Пожалуйста, укажите статус каждой транзакции";

            System.out.println(assistant.chat(userMessage));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
