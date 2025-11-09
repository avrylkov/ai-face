package org.example.service.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.service.tool.ToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.example.lang.model.ToolResult.toolResultNegative;
import static org.example.lang.model.ToolResult.toolResultOk;

@Service
public class ToolExecutorWiki {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutorWiki.class);

    private WikiService wikiService;
    private ToolFeedback toolFeedback;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static final String ToolParameterWikiSearchQuery = "Текст запроса";
    public static final String ToolSearchWiki = "Поиск информации в Wikipedia";

    public ToolExecutorWiki(WikiService wikiService,
                            ToolFeedback toolFeedback) {
        this.wikiService = wikiService;
        this.toolFeedback = toolFeedback;
    }

    private final ToolExecutor toolExecutor = (toolExecutionRequest, memoryId) -> {
        try {
            toolFeedback.execute("Tool Wiki");
            Map<String, Object> arguments = objectMapper.readValue(toolExecutionRequest.arguments(), Map.class);
            String text = wikiService.getWiki(String.valueOf(arguments.get(ToolParameterWikiSearchQuery)));
            final String jsonString;
            if (text == null) {
                jsonString = objectMapper.writeValueAsString(toolResultNegative(""));
            } else {
                jsonString = objectMapper.writeValueAsString(toolResultOk(text));
            }
            return jsonString;
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        return null;
    };

    public ToolExecutor getToolExecutor() {
        return toolExecutor;
    }

}
