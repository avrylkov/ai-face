package org.example;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.langchain4j.GigaChatChatModel;
import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.ModelName;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.List;

import static chat.giga.model.Scope.GIGACHAT_API_PERS;

public class AppGigaRagEasy
{
    private static Logger log = LogManager.getLogger(AppGigaRagEasy.class);

    interface Assistant {

        String chat(String userMessage);
    }

    public static void main( String[] args ) {

        try {
            GigaChatChatModel model = GigaChatChatModel.builder()
                    .verifySslCerts(false)
                    .authClient(AuthClient.builder().withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                    .authKey("MmZhNTA2OTYtNzUzZC00NWY1LWFkMGItYmY0YjczZjI1MzBjOmM0ZmRhNjFlLWI5YmYtNDVmZS1iOGRmLWZhYzU3MThmYTUyNQ==")
                                    .scope(GIGACHAT_API_PERS)
                                    .build())
                            .build())
                    .logRequests(true)
                    .logResponses(true)
                    .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                            //.responseFormat(ResponseFormat.JSON)
                            .modelName(ModelName.GIGA_CHAT_2)
                            .build())
                    .build();

            PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:*.pdf");
            List<Document> documents = FileSystemDocumentLoader.loadDocuments("./documentation", pathMatcher);

            InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
            EmbeddingStoreIngestor.ingest(documents, embeddingStore);

            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatLanguageModel(model)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore))
                    .build();

            System.out.println(assistant.chat("Чем закочила старуха в сказке?"));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
