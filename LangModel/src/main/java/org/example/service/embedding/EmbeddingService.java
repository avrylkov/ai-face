package org.example.service.embedding;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private static final int embeddingSearchMaxResults = 4;
    private static final int maxSegmentSizeInChars = 1000;
    private static final int maxOverlapSizeInChars = 200;
    private static final String metaDataIndex = "index";
    private final EmbeddingModelService embeddingModelService;

    public EmbeddingService(EmbeddingModelService embeddingModelService) {
        this.embeddingModelService = embeddingModelService;
    }

    public List<TextSegment> documentSplitt(InputStream targetStream) {
        TextDocumentParser textDocumentParser = new TextDocumentParser();
        dev.langchain4j.data.document.Document document = textDocumentParser.parse(targetStream);

        DocumentSplitter splitter = DocumentSplitters.recursive(maxSegmentSizeInChars, maxOverlapSizeInChars);
        return splitter.split(document);
    }

    public EmbeddingStore<TextSegment> embeddingToStore(List<TextSegment> segments) {
        List<Embedding> embeddingsToStore = embeddingModelService.embedAll(segments);
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddingsToStore, segments);
        return embeddingStore;
    }

    public List<Embedding> embeddingSegments(List<TextSegment> segments) {
        return embeddingModelService.embedAll(segments);
    }

    public int dimension() {
        return embeddingModelService.dimension();
    }

    public Embedding embed(String text) {
        return embeddingModelService.embed(text);
    }

    public List<EmbeddingMatch<TextSegment>> queryEmbedding(Embedding embeddingQuery, EmbeddingStore<TextSegment> embeddingStore) {
        EmbeddingSearchResult<TextSegment> search = embeddingStore.search(EmbeddingSearchRequest.builder()
                .maxResults(embeddingSearchMaxResults)
                .queryEmbedding(embeddingQuery).build());
        return search.matches();
    }

    public String getTextSegment(List<EmbeddingMatch<TextSegment>> embeddingMatches) {
        List<TextSegment> matchTextSegments = embeddingMatches.stream()
                .map(EmbeddingMatch::embedded)
                .toList();
        return matchTextSegments.stream()
                .map(TextSegment::text)
                .collect(Collectors.joining(" "));
    }

//    public List<TextSegment> queryEmbeddingOverlap(List<TextSegment> allSegments, List<EmbeddingMatch<TextSegment>> embeddingMatches) {
//        List<TextSegment> resultSearch = new ArrayList<>();
//        List<TextSegment> matchTextSegments = embeddingMatches.stream().map(EmbeddingMatch::embedded).toList();
//        matchTextSegments.forEach(match -> {
//            int indexTextPrev = match.metadata().getInteger(metaDataIndex) - 1;
//            if (indexTextPrev >= 0) {
//                if (noneMatchIndex(matchTextSegments, indexTextPrev)) {
//                    resultSearch.add(allSegments.get(indexTextPrev));
//                }
//            }
//            int indexTextNext = match.metadata().getInteger(metaDataIndex) + 1;
//            if (indexTextNext < allSegments.size()) {
//                if (noneMatchIndex(matchTextSegments, indexTextNext)) {
//                    resultSearch.add(allSegments.get(indexTextNext));
//                }
//            }
//            resultSearch.add(match);
//        });
//        return resultSearch.stream().sorted(Comparator.comparingInt(value -> value.metadata().getInteger(metaDataIndex))).toList();
//    }

//    private boolean noneMatchIndex(List<TextSegment> matchTextSegments, Integer index) {
//        return matchTextSegments.stream().anyMatch(match1 -> !index.equals(match1.metadata().getInteger(metaDataIndex)));
//    }
}
