package org.example.service.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

public interface EmbeddingModelService {

    List<Embedding> embedAll(List<TextSegment> segments);

    Embedding embed(String text);

    int dimension();

}
