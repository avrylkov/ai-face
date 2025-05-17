package org.example;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

public class AppTest {

    private static Logger log = LogManager.getLogger(AppTest.class);

   @Test
    public void getUrlTest() throws IOException {

       Set<String> links = findLinks("https://docs.langchain4j.dev/tutorials/rag", 2);

       for (String link : links) {
           System.out.println(link);
       }
   }

    private static Set<String> findLinks(String url, int depth) throws IOException {

        Set<String> links = new HashSet<>();
        if (depth <= 0) {
            return links;
        }

        Document doc = Jsoup.connect(url)
                .data("query", "Java")
                .userAgent("Mozilla")
                .cookie("auth", "token")
                .timeout(3000)
                .get();

        List<Element> href = doc.select("a[href]").stream().filter(element -> element.attr("href").startsWith("https://")).toList();
        for (Element element : href) {
            links.add(element.attr("href"));
        }
        //
        Set<String> links1 = new HashSet<>(links);
        links1.forEach(link -> {
            try {
                links.addAll(findLinks(link, depth - 1));
            } catch (IOException e) {
                log.error("Error {}", link, e);
            }
        });
        return links;

    }

    @Test
    public void testEmbedding() {
        String pathToModel = "C:/Users/arylk/AppData/Roaming/Python/Python312/Scripts/cointegrated/LaBSE-en-ru_onnx/model.onnx";
        String pathToTokenizer = "C:/Users/arylk/AppData/Roaming/Python/Python312/Scripts/cointegrated/LaBSE-en-ru_onnx/tokenizer.json";
        PoolingMode poolingMode = PoolingMode.MEAN;
        EmbeddingModel embeddingModel = new OnnxEmbeddingModel(pathToModel, pathToTokenizer, poolingMode);

        TextDocumentParser documentParser = new TextDocumentParser();
        dev.langchain4j.data.document.Document document = loadDocument(CommonUtils.toPath("./document/Сказка.txt"), documentParser);
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> segments = splitter.split(document);
        Response<List<Embedding>> embedAll = embeddingModel.embedAll(segments);

        Response<Embedding> response = embeddingModel.embed("С чем осталась старуха в конце сказки");
        Embedding embeddingQuery = response.content();

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embedAll.content(), segments);

        //EmbeddingStoreIngestor.ingest(documents, embeddingStore);

        EmbeddingSearchResult<TextSegment> search = embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingQuery).build());
        search.matches().forEach(match -> System.out.println("---- \n" + match.score() + " " + match.embedded().text()));

    }

}
