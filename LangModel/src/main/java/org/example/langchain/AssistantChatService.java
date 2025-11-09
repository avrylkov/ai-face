package org.example.langchain;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import org.apache.commons.lang3.StringUtils;
import org.example.face.Person;
import org.example.service.AssistantChat;
import org.example.service.GlobalContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.example.langchain.PersistentChatMemoryStore.Face_ID;

public class AssistantChatService {

    private static final Logger log = LoggerFactory.getLogger(AssistantChatService.class);

    private final AssistantChat assistantChat;
    private final AssistantMemoryChatStream assistantMemoryChatStreamTool;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy MMMM dd HH:mm", Locale.forLanguageTag("ru"));

    private static AssistantChatService  instance;
    private final AssistantMemoryChatStream assistantMemoryChatStream;

    public static AssistantChatService INSTANCE(AssistantChat assistantChat,
                                                AssistantMemoryChatStream assistantMemoryChatStreamTool,
                                                AssistantMemoryChatStream assistantMemoryChatStream) {
        if (instance == null) {
            instance = new AssistantChatService(assistantChat, assistantMemoryChatStreamTool, assistantMemoryChatStream);
        }
        return instance;
    };

    public static boolean isInitialized() {
        return instance != null;
    }

    public static AssistantChatService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AssistantChatService has not been initialized yet");
        }
        return instance;
    }

    private AssistantChatService(AssistantChat assistantChat,
                                AssistantMemoryChatStream assistantMemoryChatStreamTool,
                                 AssistantMemoryChatStream assistantMemoryChatStream) {
        this.assistantChat = assistantChat;
        this.assistantMemoryChatStreamTool = assistantMemoryChatStreamTool;
        this.assistantMemoryChatStream = assistantMemoryChatStream;
    }

    public void agentContinueDialogueStreaming(int memoryId, String useMessage, Consumer<String> response) {
        agentStreamingWithTool(memoryId, useMessage, response);
    }

    public void greetingOldFriendStreaming(int memoryId, String fullName, Consumer<String> response) {
        agentStreaming(memoryId, String.format("Ты уже был знаком с этим человеком, его зовут - %s, " +
                "поприветствуй его по имени, продолжи диалог используя предыдущие темы. Дата встречи %s",
                fullName, formatter.format(LocalDateTime.now())), response);
    }

    public Person agentDetectPersonInfo(String message) {
        String firstAndLastName = assistantChat.giveFirstAndLastName(message);
        if (StringUtils.contains( firstAndLastName, "true")) {
            return assistantChat.extractPersonFrom(message);
        }
        return null;
    }

    public String agentMeetingNewPersonWelcome() {
        return assistantChat.chat("Тебя зовут Агент. Перед тобой незнакомый человек, предложи ему приветствие и познакомься с ним. " +
                "Пусть он назовет свое имя и фамилию");
    }

    public String agentMultiplePersons() {
        return assistantChat.chat("Перед тобой несколько лиц, продолжить диалог ты можешь только с одним человеком. " +
                "Попроси остаться кого-то одного");
    }

    public void niceMeetYou(int memoryId, Person person, Consumer<String> response) {
        agentStreaming(memoryId, String.format( "Скажи что тебе было приятно познакомиться с человеком по имени %s, и фамилии %s. " +
                "Продолжи диалог и предложи любые темы для беседы", person.firstName(), person.lastName()),
                response);
    }

    public String IsNoOne() {
        return assistantChat.chat("Перед тобой никого нет, но тебе хочется общения с человеком, скажи что ты ждешь человека");
    }

    public String noUnderstandYourName(String message) {
        return assistantChat.chat(String.format("Человек представился как \"%s\", но тебе не понятно как его зовут, попроси его повторить как его зовут, т.е. свое имя и фамилию", message));
    }

    public String chat(String message) {
        return assistantChat.chat(message);
    }

    public boolean textContainsQuestion(String question) {
        String string = assistantChat.textContainsQuestion(question);
        return StringUtils.contains(string, "true");
    }

    public boolean textContainsSendMessage(String question) {
        String string = assistantChat.textContainsSendMessage(question);
        return StringUtils.contains(string, "true");
    }

    public boolean textContainsReadMessage(String question) {
        String string = assistantChat.textContainsReadMessage(question);
        return StringUtils.contains(string, "true");
    }

    public Integer getFaceIdFromAllFaces(String allFaces, String person) {
        String string = assistantChat.getFaceIdAllFaces(allFaces, person);
        string = StringUtils.trim(string);
        if (StringUtils.contains(string, Face_ID)) {
            return Integer.parseInt(StringUtils.substringAfter(string, "="));
        }
        return null;
    }

    private void agentStreamingWithTool(int memoryId, String useMessage, Consumer<String> response) {
        TokenStream tokenStream = assistantMemoryChatStreamTool.chatStreamWithTool(memoryId, useMessage, getUserContext());
        streaming(tokenStream, response);
    }

    private void agentStreaming(int memoryId, String useMessage, Consumer<String> response) {
        TokenStream tokenStream = assistantMemoryChatStream.chatStream(memoryId, useMessage, getUserContext());
        streaming(tokenStream, response);
    }

    private void streaming(TokenStream tokenStream, Consumer<String> response) {
        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        tokenStream.onPartialResponse(response)
                .onCompleteResponse(futureResponse::complete)
                .onError(futureResponse::completeExceptionally)
                .start();
        ChatResponse chatResponse;
        try {
            chatResponse = futureResponse.get(120, SECONDS);
            log.info("Chat response: {}", chatResponse);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private String getUserContext() {
        requireNonNull(GlobalContext.getCurrentFace(), "Лицо обязательно должно быть инициализировано");
        requireNonNull(GlobalContext.getCurrentFace().getPerson(), "Персона должна быть инициализирована");
        requireNonNull(GlobalContext.getCurrentFace().getMeetingTime(), "Время встречи должно быть инициализировано");
        //
        return String.format("Ты ведешь диалог с человеком по имени %s, и фамилии %s, дата знакомства - %s",
                GlobalContext.getCurrentFace().getPerson().firstName(),
                GlobalContext.getCurrentFace().getPerson().lastName(),
                formatter.format(GlobalContext.getCurrentFace().getMeetingTime()));

    }

}
