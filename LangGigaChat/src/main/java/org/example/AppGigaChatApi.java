package org.example;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.http.client.HttpClientException;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;
import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.CompletionRequest;

public class AppGigaChatApi {

    public static void main( String[] args ) {
        GigaChatClient client = GigaChatClient.builder()
                .verifySslCerts(false)
                .authClient(AuthClient.builder()
                        .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .scope(Scope.GIGACHAT_API_PERS)
                                .clientId("2fa50696-753d-45f5-ad0b-bf4b73f2530c")
                                .clientSecret("c4fda61e-b9bf-45fe-b8df-fac5718fa525")
                                .build())
                        .build())
                .build();
        try {
            System.out.println(client.completions(CompletionRequest.builder()
                    .model(ModelName.GIGA_CHAT_2)
                    .message(ChatMessage.builder()
                            .content("Какие факторы влияют на стоимость страховки на дом?")
                            .role(ChatMessage.Role.USER)
                            .build())
                    .build()));
        } catch (HttpClientException ex) {
            System.out.println(ex.statusCode() + " " + ex.bodyAsString());
        }
    }

}
