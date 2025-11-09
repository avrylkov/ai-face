package org.example.lang.model;

import dev.langchain4j.model.output.structured.Description;

@Description("Результат выполнения")
public record ToolResult(@Description("Статус") String status,
                         @Description("Результат") String text) {

    public static final String PositiveResult = "Ок";
    public static final String NegativeResult = "Отрицательный результат";

    public static ToolResult toolResultOk(String text) {
        return new ToolResult(PositiveResult, text);
    }

    public static ToolResult toolResultNegative(String text) {
        return new ToolResult(NegativeResult, text);
    }

}
