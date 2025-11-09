package org.example.service.tool;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.example.client.WikiClient;
import org.example.model.RootSearch;
import org.example.service.embedding.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class WikiService {

    private static final Logger log = LoggerFactory.getLogger(WikiService.class);
    private static final int limit  = 2;

    private WikiClient wikiClient;

    private EmbeddingService embeddingService;

    public WikiService(WikiClient wikiClient,
                       EmbeddingService embeddingService) {
        this.wikiClient = wikiClient;
        this.embeddingService = embeddingService;
    }

    public String getWiki(String query) {
//        return """
//                Абсолютный минимум температуры на ней равен −38,1 °C, а абсолютный максимум +38,2 °C.
//                2010 год в Москве занял первое место по числу суточных рекордов максимальной температуры (28), однако из-за холодного января год не стал самым тёплым в истории
//                """;
        log.info("Wiki search query={}", query);
        ResponseEntity<RootSearch> search = wikiClient.search(query);
        if (search.getStatusCode().is2xxSuccessful()) {
            assert search.getBody() != null;
            if (!search.getBody().query.search.isEmpty()) {
                RootSearch searchBody = search.getBody();
                StringBuilder sbuilder = new StringBuilder();
                searchBody.query.search.stream().limit(limit).forEach(s -> {
                    ResponseEntity<Map<String, ?>> pageEntity = wikiClient.getPage(s.pageid);
                    if (pageEntity.getStatusCode().is2xxSuccessful()) {
                        Map pageQuery = (Map) pageEntity.getBody().get("query");
                        Map pages = (Map) pageQuery.get("pages");
                        Map page = (Map) pages.get(String.valueOf(s.pageid));
                        String text = page.get("extract").toString();
                        //
                        String embedding = getEmbedding(query, text);
                        sbuilder.append(embedding).append("\n");
                    }
                });
                return sbuilder.toString();
            }
        }
        return null;
    }

    public String getEmbedding(String query, String text) {
        if (text == null) {
            return null;
        }
        try (InputStream allDescriptionStream = new ByteArrayInputStream(text.getBytes())) {
            List<TextSegment> allTextSegments = embeddingService.documentSplitt(allDescriptionStream);
            log.info("Embedding split all text count={}", allTextSegments.size());
            EmbeddingStore<TextSegment> allTextSegmentEmbeddingStore = embeddingService.embeddingToStore(allTextSegments);
            Embedding embedQuery = embeddingService.embed(query);
            List<EmbeddingMatch<TextSegment>> embeddingMatches = embeddingService.queryEmbedding(embedQuery, allTextSegmentEmbeddingStore);
            String matchText = embeddingService.getTextSegment(embeddingMatches);
            log.info("""
                    Match Wiki embedding text
                    *******
                    {}
                    count={}
                    *********
                    """,
                    matchText, embeddingMatches.size());
            return matchText;
        } catch (Exception e) {
            log.error("Match Wiki embedding text error", e);
        }
        return null;
    }

}
