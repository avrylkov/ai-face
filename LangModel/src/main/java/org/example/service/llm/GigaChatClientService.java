package org.example.service.llm;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.model.Scope;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class GigaChatClientService {

    public static final String API_KEY = "MmZhNTA2OTYtNzUzZC00NWY1LWFkMGItYmY0YjczZjI1MzBjOmM0ZmRhNjFlLWI5YmYtNDVmZS1iOGRmLWZhYzU3MThmYTUyNQ==";

    private GigaChatClient gigaChatClient;

    @PostConstruct
    public void initialize() {
        gigaChatClient = GigaChatClient.builder()
                .verifySslCerts(false)
                .authClient(AuthClient.builder()
                        .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .scope(Scope.GIGACHAT_API_PERS)
                                .authKey(API_KEY)
                                .build())
                        .build())
//                .logRequests(true)
//                .logResponses(true)
                .build();
    }

    public GigaChatClient getGigaChatClient() {
        return gigaChatClient;
    }
}
