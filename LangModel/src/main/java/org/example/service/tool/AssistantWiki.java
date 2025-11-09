package org.example.service.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.example.lang.model.ToolResult;
import org.example.service.GlobalContext;

import static org.example.service.tool.ToolExecutorWiki.ToolParameterWikiSearchQuery;
import static org.example.service.tool.ToolExecutorWiki.ToolSearchWiki;

public class AssistantWiki {

    @Tool(ToolSearchWiki)
    public ToolResult search(@ToolMemoryId int memoryId, @P(value = ToolParameterWikiSearchQuery) String query) {
        String wiki = GlobalContext.getContext().getBean(WikiService.class).getWiki(query);
        if (wiki == null) {
            return ToolResult.toolResultNegative("");
        }
        return ToolResult.toolResultOk(wiki);
    }

}
