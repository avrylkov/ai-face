package org.example;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.langchain4j.GigaChatChatModel;
import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.ModelName;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static chat.giga.model.Scope.GIGACHAT_API_PERS;

public class AppGigaClassification {
    private static Logger log = LogManager.getLogger(AppGigaClassification.class);

    enum Sentiment {
        POSITIVE, NEUTRAL, NEGATIVE
    }

    // Define the AI-powered Sentiment Analyzer interface
    interface SentimentAnalyzer {

        @UserMessage("Проанализируйте настроен {{it}}")
        Sentiment analyzeSentimentOf(String text);

        @UserMessage("Есть ли у {{it}} позитивный настрой?")
        boolean isPositive(String text);
    }
    public static void main(String[] args) {


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
                        //.responseFormat(jsonSchema)
                        .modelName(ModelName.GIGA_CHAT_2)
                        .build())
                .build();

        // Create an AI-powered Sentiment Analyzer instance
        SentimentAnalyzer sentimentAnalyzer = AiServices.create(SentimentAnalyzer.class, model);

        // Example Sentiment Analysis
        Sentiment sentiment = sentimentAnalyzer.analyzeSentimentOf("Я в восторге от этого продукта!");
        log.info(sentiment); // Expected Output: POSITIVE

        boolean positive = sentimentAnalyzer.isPositive("Это ужасный опыт.");
        log.info(positive); // Expected Output: false

    }

}
