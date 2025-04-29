package org.example;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.langchain4j.GigaChatChatModel;
import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.ModelName;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

import static chat.giga.model.Scope.GIGACHAT_API_PERS;
import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static org.example.LangGigaUtils.startConversationWith;

public class AppGigaRagNaive {

    public interface Assistant {

        String answer(String query);
    }

    interface AssistantPrompt {

        @SystemMessage("Используйте следующую информацию для ответа на вопрос: {{information}}")
        String chat(@V("information") String information, @UserMessage String userMessage);
    }



    /**
     * 1. Примите запрос пользователя как есть.
     * 2. Внедрите его, используя модель внедрения.
     * 3. Используйте вложение запроса для поиска в хранилище вложений (содержащем небольшие фрагменты ваших документов)
     * по X наиболее релевантным сегментам.
     * 4. Добавьте найденные сегменты к запросу пользователя.
     * 5. Отправьте объединенные входные данные (пользовательский запрос + сегменты) в LLM.
     * 6. Надеемся, что:
     * - Запрос пользователя хорошо сформулирован и содержит все необходимые детали для поиска.
     * - Найденные сегменты соответствуют запросу пользователя.
     */
    public static void main(String[] args) {
        // Давайте создадим помощника, который будет знать о нашем документе
        Assistant assistant = createAssistant("./document/Сказка о рыбаке и рыбке.pdf");

        // Теперь давайте начнем разговор
        startConversationWith(assistant);
    }

    private static Assistant createAssistant(String documentPath) {

        // Сначала давайте создадим модель чата, также известную как LLM, которая будет отвечать на наши запросы.
        GigaChatChatModel chatLanguageModel = GigaChatChatModel.builder()
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

        // Теперь давайте загрузим документ, который мы хотим использовать для RAG.
        //DocumentParser documentParser = new TextDocumentParser();
        DocumentParser documentParser = new ApachePdfBoxDocumentParser();
        Document document = loadDocument(CommonUtils.toPath(documentPath), documentParser);


        // Теперь нам нужно разбить этот документ на более мелкие сегменты, также известные как "куски".
        // Этот подход позволяет нам отправлять в LLM только соответствующие сегменты в ответ на запрос пользователя,
        //, а не весь документ целиком. Например, если пользователь спросит о правилах отмены,
        // мы определим и отправим только те сегменты, которые связаны с отменой.
        // Хорошей отправной точкой является использование рекурсивного разделителя документов, который сначала пытается
        // разделить на абзацы. Если абзац слишком велик, чтобы уместиться в одном сегменте,
        // разделитель рекурсивно разделит его на новые строки, затем на предложения и, наконец, на слова,
        // если необходимо, чтобы каждый фрагмент текста поместился в один сегмент.
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> segments = splitter.split(document);


        // Теперь нам нужно внедрить (также известное как "векторизация") эти сегменты.
        // Внедрение необходимо для выполнения поиска сходства.
        // В этом примере мы будем использовать локальную модель внедрения в процессе, но вы можете выбрать любую поддерживаемую модель.
        // В настоящее время Langchain4j поддерживает более 10 популярных поставщиков моделей встраивания.
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();


        // Далее мы сохраним эти вложения в хранилище вложений (также известном как "векторная база данных").
        // Это хранилище будет использоваться для поиска соответствующих сегментов при каждом взаимодействии с LLM.
        // Для простоты в этом примере используется хранилище для встраивания в память, но вы можете выбрать любое из поддерживаемых хранилищ.
        // В настоящее время Langchain4j поддерживает более 15 популярных хранилищ для встраивания.
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);

        // Средство поиска контента отвечает за поиск релевантного контента на основе запроса пользователя.
        // В настоящее время он способен извлекать текстовые сегменты, но будущие усовершенствования будут включать поддержку
        // дополнительные возможности, такие как изображения, аудио и многое другое.
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3) // при каждом взаимодействии мы будем извлекать 2 наиболее релевантных сегмента
                .minScore(0.5) // мы  хотим получить сегменты, хотя бы отчасти похожие на пользовательский запрос
                .build();

        // При желании мы можем использовать память чата, что позволит вести переписку с LLM в режиме реального времени
        // и позволит ему запоминать предыдущие взаимодействия.
        // В настоящее время LangChain4j предлагает две реализации памяти чата:
        // Память чата в окне сообщений и память чата в окне токенов.
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        // Последним шагом является создание нашего сервиса искусственного интеллекта,
        // настройка его для использования компонентов, которые мы создали выше.
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(chatLanguageModel)
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .build();
    }


}
