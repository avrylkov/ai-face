package org.example;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;
import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.ChatMessageRole;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
import chat.giga.model.file.FileResponse;
import chat.giga.model.file.UploadFileRequest;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

import static org.example.CommonUtils.resourceCommonToUri;

public class AppGigaImage {

    private static Logger log = LogManager.getLogger(AppGigaImage.class);

    public static void main(String[] args) {

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

        byte[] bytes = null;
        try {
            bytes = FileUtils.readFileToByteArray(new File(resourceCommonToUri("face-disorder.jpg")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                .purpose("general")
                .mimeType("image/jpeg")
                .file(bytes)
                .fileName("Screenshot.jpg")
                .build();
        FileResponse fileResponse = client.uploadFile(uploadFileRequest);
        log.info(fileResponse.id());

        CompletionResponse completions = client.completions(CompletionRequest.builder()
                .model(ModelName.GIGA_CHAT_MAX_2)
                .message(ChatMessage.builder()
                        .content("Что нарисованно на картинке? Если есть лицо, то какие эмоции у него? ")
                        .role(ChatMessageRole.USER)
                        .attachment(fileResponse.id().toString())
                        .build())
                .build());

        completions.choices().forEach(completion -> log.info(completion.message().role() + ": " + completion.message().content()));

    }

}




