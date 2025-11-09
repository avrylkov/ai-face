package org.example;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import org.example.service.tool.AssistantWiki;

import java.util.List;

public class AppToolExecute {

    public static void main(String[] args) {
        OllamaChatModel model = OllamaChatModel.builder()
                .modelName("PetrosStav/gemma3-tools:4b")
                .baseUrl("http://localhost:11434")
                .logRequests(true)
                .logResponses(true)
                .build();

        // step 1: construct the tool specifications
        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(AssistantWiki.class);

        // step 2: first chat to analyze tools to use
//        UserMessage userMessage = UserMessage.from("что знаешь про город Краснознаменск Московской области? используй tool Search_Wikipedia");
        UserMessage userMessage = UserMessage.from("что знаешь про город Краснознаменск Московской области?");
        ChatRequest request = ChatRequest.builder()
                .messages(userMessage)
                .toolSpecifications(toolSpecifications)
                .build();
        ChatResponse chatResponse = model.chat(request);
        AiMessage aiMessage = chatResponse.aiMessage();

        // step 3: prepare executing the tool
        ToolExecutionRequest toolExecutionRequest = aiMessage.toolExecutionRequests().get(0);
        ToolExecutor defaultToolExecutor = new DefaultToolExecutor(
                new AssistantWiki(),
                AssistantWiki.class.getDeclaredMethods()[0]
        );
        // step 4: call the tool
        String executionResult = defaultToolExecutor.execute(toolExecutionRequest, 1);
        // step 5: merge all results in one chat
        ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.from(toolExecutionRequest, executionResult);
        ChatRequest requestWithToolOutput = ChatRequest.builder()
                .messages(List.of(userMessage, aiMessage, toolExecutionResultMessage))
                .toolSpecifications(toolSpecifications)
                .build();
        ChatResponse responseWithToolResult = model.chat(requestWithToolOutput);
        System.out.println(responseWithToolResult.aiMessage());
    }

}
