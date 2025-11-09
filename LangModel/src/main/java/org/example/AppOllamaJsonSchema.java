package org.example;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.example.CommonUtils.resourceToUrl;

public class AppOllamaJsonSchema {
    private static Logger log = LoggerFactory.getLogger(AppOllamaJsonSchema.class);

    interface AssistantPrompt {

        @SystemMessage("""                
                Необходимо преобразовать входящий текст в JSON формат согласно указанной JSON Schema.
                Преобразуй текст в JSON в соответствии с данной JSON Schema.
                Если какое-либо поле отсутствует во входных данных, но в схеме для него указано значение по умолчанию (default), обязательно подставь это значение в итоговый JSON, иначе игнорируй это поле.
                Даты преобразовать согласно формату указанному в pattern регулярном выражении.
                Делай вывод только в JSON формате, без дополнительных текстовых строк.
                """)
        @UserMessage("Входящий текст {{message}}, JSON Schema {{schema}}.")
        String chat(@V("schema") String schema, @V("message") String userMessage);
    }

    public static void main(String[] args) {

        OllamaChatModel model = OllamaChatModel.builder()
                .modelName("gemma3:12b")
                .baseUrl("http://localhost:11434")
                .logRequests(true)
                .logResponses(true)
                .timeout(Duration.ofMinutes(2))
                .build();
        try {
            String schema2 = FileUtils.readFileToString(new File(resourceToUrl(AppOllamaJsonSchema.class, "json-schema-organization.json").toURI()), StandardCharsets.UTF_8);
            AssistantPrompt assistantPrompt = AiServices.create(AssistantPrompt.class, model);

            System.out.println(assistantPrompt.chat(schema2, """
                    Регистрация новой Организация с ИНН 1234567890 и КПП 123456789, название 'ООО Рога и копыта',
                    номер договора 6777а, от 1 июля 2025г, спец программа 7766,
                    торговая точка с названием 'Магазин 1', MCC 5411
                    """));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
