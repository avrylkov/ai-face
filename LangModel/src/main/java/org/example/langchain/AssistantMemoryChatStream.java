package org.example.langchain;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import static org.example.service.tool.ToolExecutorReadMessage.ToolReadMessage;
import static org.example.service.tool.ToolExecutorSendMessage.ToolSendMessage;
import static org.example.service.tool.ToolExecutorWiki.ToolSearchWiki;

public interface AssistantMemoryChatStream {
    String Context = "контекст:";

    @SystemMessage(value =
            "Ты Агент, с тобой ведет диалог человек, используй правила описанные ниже: \n" +
            "Если не знаешь ответ на вопрос или в вопросе явно сказано искать в Wiki, то используй tool \"" + ToolSearchWiki + "\". Если tool вернул ответ, то используй информацию из его ответа и дай полный ответ, больше не вызывай ни какого tool после этого. \n" +
            "Если тебя просят отправить сообщение, то обязательно используй tool \"" + ToolSendMessage + "\", верни текст поля 'status' и 'text' в формате 'Кому:', 'Текст:', больше не вызывай ни какого tool, после вызова tool предложи позже прочитать новые сообщения. \n" +
            "Если тебя просят прочитать новые сообщения, то обязательно используй tool \"" + ToolReadMessage + "\", верни текст поля 'status' и 'text' в формате 'От кого:', 'Дата:', 'Текст:', после вызова tool пожелай приятного прочтения сообщений. \n" +
            """
            При передаче запроса в tool используй оригинальный текст и русский язык.
            Tool вызывай только один раз, после вызова одного Tool не вызывай другие.
            При ответе используй русский язык.
            Используй предоставленный
            """ + Context + "{{context}}"
    )
    TokenStream chatStreamWithTool(@MemoryId int memoryId,
                                   @UserMessage String userMessage,
                                   @V("context") String context);

    @SystemMessage(value =
            """
            При ответе используй русский язык.
            Используй предоставленный
            """ + Context + "{{context}}"
    )
    TokenStream chatStream(@MemoryId int memoryId,
                                   @UserMessage String userMessage,
                                   @V("context") String context);

}
