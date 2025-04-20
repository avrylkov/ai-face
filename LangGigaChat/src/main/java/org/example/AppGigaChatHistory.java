package org.example;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.http.client.HttpClientException;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;
import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;

public class AppGigaChatHistory {

    public static void main(String[] args) {
        GigaChatClient client = GigaChatClient.builder()
                .verifySslCerts(false)
                .authClient(AuthClient.builder()
                        .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .scope(Scope.GIGACHAT_API_PERS)
                                .authKey("MmZhNTA2OTYtNzUzZC00NWY1LWFkMGItYmY0YjczZjI1MzBjOmM0ZmRhNjFlLWI5YmYtNDVmZS1iOGRmLWZhYzU3MThmYTUyNQ==")
                                .build())
                        .build())
                .build();

        CompletionRequest.CompletionRequestBuilder completionRequestBuilder = CompletionRequest.builder()
                .model(ModelName.GIGA_CHAT_2)
                .temperature(0.3f)
                //.maxTokens(20)
                .message(ChatMessage.builder()
                        .content("Когда уже ИИ захватит этот мир?")
                        .role(ChatMessage.Role.USER)
                        .build())
                .message(ChatMessage.builder()
                        .content("Пока что это не является неизбежным событием. " +
                                "Несмотря на то, что искусственный интеллект (ИИ) развивается быстрыми темпами и может выполнять сложные задачи все более эффективно, " +
                                "он по-прежнему ограничен в своих возможностях и не может заменить полностью человека во многих областях. Кроме того, существуют этические " +
                                "и правовые вопросы, связанные с использованием ИИ, которые необходимо учитывать при его разработке и внедрении.")
                        .role(ChatMessage.Role.ASSISTANT).build());

        String sessionId = "8324244b-7133-4d30-a328-31d8466e5502";
        try {
            for (int i = 0; i < 4; i++) {
                CompletionRequest request = completionRequestBuilder.build();
                CompletionResponse response = client.completions(request, sessionId);
                System.out.println(response);

                response.choices().forEach(e -> completionRequestBuilder.message(e.message().ofAssistantMessage()));

                completionRequestBuilder.message(ChatMessage.builder()
                        .content("Думаешь, у нас еще есть шанс?")
                        .role(ChatMessage.Role.USER).build());
            }
        } catch (HttpClientException ex) {
            System.out.println(ex.statusCode() + " " + ex.bodyAsString());
        }
    }
}


