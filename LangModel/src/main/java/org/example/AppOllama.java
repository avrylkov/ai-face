package org.example;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppOllama
{

    private static final Logger log = LoggerFactory.getLogger(AppOllama.class);

    interface Assistant {
        @SystemMessage("Предложи познакомится, поделится мыслями, впечатлениями")
        String chat(@UserMessage String userMessage);
    }

    public static void main( String[] args ) {

        OllamaChatModel model = OllamaChatModel.builder()
                .modelName("gemma3:12b")
                .baseUrl("http://localhost:11434")
                .logRequests(true)
                .logResponses(true)
                .build();

        Assistant assistant =  AiServices.create(Assistant.class, model);
        log.info(assistant.chat("Привет! Как дела?"));

        //log.info(model.generate("Привет! Как дела?"));
    }

}
