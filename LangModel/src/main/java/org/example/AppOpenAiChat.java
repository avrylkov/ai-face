package org.example;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AppOpenAiChat
{
    private static Logger log = LogManager.getLogger(AppOpenAiChat.class);

    public static void main( String[] args ) {

OpenAiChatModel model2 = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .build();

        String answer2 = model2.chat("Say 'Hello World'");
        log.info(answer2);
    }
}
