package org.example;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.langchain4j.GigaChatChatModel;
import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.ModelName;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static chat.giga.model.Scope.GIGACHAT_API_PERS;
import static org.example.CommonUtils.resourceToUrl;

public class AppGigaJsonSchema {

    private static Logger log = LogManager.getLogger(AppGigaJsonSchema.class);

    interface AssistantPrompt {

        @SystemMessage("""
                Преобразуй текст в JSON в соответствии с данной JSON Schema.
                Если какое-либо поле отсутствует во входных данных, но в схеме для него указано значение по умолчанию (default), обязательно подставь это значение в итоговый JSON, иначе игнорируй это поле.
                Даты преобразовать согласно формату указанному в pattern регулярном выражении.
                Делай вывод только в JSON формате, без дополнительных текстовых строк.
                """)
        @UserMessage("""
                Входящий текст {{message}}, JSON Schema {{schema}}.
                Если какое-либо поле отсутствует во входных данных, но в схеме для него указано значение по умолчанию (default), обязательно подставь это значение в итоговый JSON, иначе игнорируй это поле.
                """)
        String chat(@V("schema") String schema, @V("message") String userMessage);
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
                        .modelName(ModelName.GIGA_CHAT_PRO_2)
                        .build())
                .build();

        try {
            String schema2 = FileUtils.readFileToString(new File(resourceToUrl(AppGigaJsonSchema.class, "json-schema-organization.json").toURI()), StandardCharsets.UTF_8);
            AssistantPrompt assistantPrompt = AiServices.create(AssistantPrompt.class, model);

            System.out.println(assistantPrompt.chat(schema2, """
                    Регистрация новой Организация с ИНН 1234567890 и КПП 123456789, название 'ООО Рога и копыта',
                    номер договора 6777а, от 1 июля 2025г, спец программа 7766,
                    торговая точка с названием 'Магазин 1', MCC 5411
                    """));
//        System.out.println(assistantPrompt.chat(schema2, """
//                Регистрация в Организацию с ИД=998877,
//                в договор с ИД=554433,
//                торговая точка с названием 'Магазин 2', MCC 5411"
//                """
//                ));

        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
