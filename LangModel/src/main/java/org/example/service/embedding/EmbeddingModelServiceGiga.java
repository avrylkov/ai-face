package org.example.service.embedding;

import chat.giga.model.embedding.EmbeddingRequest;
import chat.giga.model.embedding.EmbeddingResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import org.example.service.llm.GigaChatClientService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile("giga")
public class EmbeddingModelServiceGiga implements EmbeddingModelService {

    private final GigaChatClientService gigaChatClientService;

    public EmbeddingModelServiceGiga(GigaChatClientService gigaChatClientService) {
        this.gigaChatClientService = gigaChatClientService;
    }

    @Override
    public List<Embedding> embedAll(List<TextSegment> segments) {
        return embeddings(segments.stream()
                .map(TextSegment::text)
                .collect(Collectors.toList()));
    }

    @Override
    public Embedding embed(String text) {
        return embeddings(List.of(text)).get(0);
    }

    @Override
    public int dimension() {
        return this.embed("test").dimension();
    }

    private List<Embedding> embeddings(List<String> text) {
        EmbeddingResponse embeddingsQuery = gigaChatClientService.getGigaChatClient().embeddings(EmbeddingRequest.builder()
                .model("EmbeddingsGigaR")
                .input(text)
                .build());
        return embeddingsQuery.data().stream()
                .map(e -> Embedding.from(e.embedding()))
                .toList();
    }

}
