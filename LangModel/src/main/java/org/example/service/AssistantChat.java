package org.example.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.example.face.Person;

public interface AssistantChat {

    Person extractPersonFrom(String text);

    @UserMessage("Надо убедится что в тексте \"{{message}}\" есть Имя и Фамилия? Надо ответить строго в следующем формате: одно из [true, false]")
    String giveFirstAndLastName(@V("message") String message);

    @UserMessage("Надо убедится что текст \"{{message}}\" содержит вопрос, что тебя пытаются спросит. Надо ответить строго в следующем формате: одно из [true, false]")
    String textContainsQuestion(@V("message") String message);

    @UserMessage("Надо убедится что в тексте \"{{message}}\" тебя просят отправить сообщение. Надо ответить строго в следующем формате: одно из [true, false]")
    String textContainsSendMessage(@V("message") String message);

    @UserMessage("Надо убедится что в тексте \"{{message}}\" тебя просят прочитать сообщение. Надо ответить строго в следующем формате: одно из [true, false]")
    String textContainsReadMessage(@V("message") String message);

    @SystemMessage("В ответе используй русский язык или тот что был в тексте")
    String chat(@UserMessage String userMessage);

    @UserMessage("Надо из текста \"{{allChatSystemContext}}\" вернуть ID для Имени и Фамилии \"{{person}}\". Вернуть в формате ID=N")
    String getFaceIdAllFaces(@V("allChatSystemContext") String allFaces, @V("person") String person);
}
