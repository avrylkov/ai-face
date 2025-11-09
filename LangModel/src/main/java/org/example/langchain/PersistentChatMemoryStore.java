package org.example.langchain;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class PersistentChatMemoryStore implements ChatMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(PersistentChatMemoryStore.class);

    private static PersistentChatMemoryStore instance;
    public static PersistentChatMemoryStore getInstance() {
        if (instance == null) {
            instance = new PersistentChatMemoryStore();
        }
        return instance;
    }

    public static final String Face_ID = "ID=";
    private final HashMap<Integer, List<ChatMessage>> chatMemoryMap = new HashMap<>();

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
       return chatMemoryMap.getOrDefault((Integer) memoryId, new ArrayList<>());
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        if (messages.get(0).type() != ChatMessageType.SYSTEM) {
            Optional<ChatMessage> first = messages.stream()
                    .filter(chatMessage -> chatMessage.type() == ChatMessageType.SYSTEM)
                    .findFirst();
            first.ifPresent(m -> {
                messages.remove(m);
                messages.add(0, m);
            });
        }
        //
        if (messages.stream().anyMatch(chatMessage ->
                chatMessage.type() == ChatMessageType.AI && ((AiMessage) chatMessage).hasToolExecutionRequests()) &&
                messages.stream().noneMatch(c -> c.type() != ChatMessageType.TOOL_EXECUTION_RESULT)) {
            log.info("hasToolExecutionRequests");
        }
        if (messages.stream().anyMatch(chatMessage ->
                chatMessage.type() == ChatMessageType.TOOL_EXECUTION_RESULT  &&
                messages.stream().noneMatch(c -> c.type() == ChatMessageType.AI && ((AiMessage) c).hasToolExecutionRequests()))) {
            log.info("TOOL_EXECUTION_RESULT");
        }

        chatMemoryMap.put((Integer) memoryId, messages);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        chatMemoryMap.remove((Integer)memoryId);
    }

}
