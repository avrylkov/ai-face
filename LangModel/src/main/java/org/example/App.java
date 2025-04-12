package org.example;

//import dev.langchain4j.model.openai.OpenAiChatModel;
import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.langchain4j.GigaChatChatModel;
import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.ModelName;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static chat.giga.model.Scope.GIGACHAT_API_PERS;

public class App
{
    private static Logger log = LogManager.getLogger(App.class);

    public static void main( String[] args ) {

        try {
            GigaChatChatModel model = GigaChatChatModel.builder()
                    .authClient(AuthClient.builder().withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                    .clientId("2fa50696-753d-45f5-ad0b-bf4b73f2530c")
                                    .clientSecret("c4fda61e-b9bf-45fe-b8df-fac5718fa525")
                                    .scope(GIGACHAT_API_PERS)
                                    .build())
                            .build())
                    .logRequests(true)
                    .logResponses(true)
                    .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                            .modelName(ModelName.GIGA_CHAT_PRO)
                            .build())
                    .build();
//                    .authClient(AuthClient.builder()
//                            .withCertificatesAuth(new JdkHttpClientBuilder()
//                                    .httpClientBuilder(HttpClient.newBuilder())
//                                    .ssl(SSL.builder()
//                                            .truststorePassword("pass")
//                                            .trustStoreType("PKCS12")
//                                            .truststorePath("/Users/user/ssl/client_truststore.p12")
//                                            .keystorePassword("pass")
//                                            .keystoreType("PKCS12")
//                                            .keystorePath("/Users/user/ssl/client_keystore.p12")
//                                            .build())
//                                    .build())
//                            .build())
//                    .verifySslCerts(false)
//                    .logRequests(true)
//                    .logResponses(true)
//                    .apiUrl("host1")
//                    .build();
            ChatResponse chatResponse = model.chat(ChatRequest.builder().messages(new UserMessage("как дела?"))
                    .parameters(DefaultChatRequestParameters.builder().modelName(ModelName.GIGA_CHAT_PRO).build()).build());
            System.out.println(chatResponse.aiMessage().text());
        } catch (Exception ex) {
            ex.printStackTrace();
        }


//        OpenAiChatModel model2 = OpenAiChatModel.builder()
//                .baseUrl("http://langchain4j.dev/demo/openai/v1")
//                .apiKey("demo")
//                .modelName("gpt-4o-mini")
//                .build();
//
//        String answer2 = model2.generate("Say 'Hello World'");
//        log.info(answer2);
    }
}
