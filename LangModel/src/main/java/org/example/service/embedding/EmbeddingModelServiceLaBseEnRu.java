package org.example.service.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import jakarta.annotation.PostConstruct;
import org.example.CommonProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("!giga")
public class EmbeddingModelServiceLaBseEnRu implements EmbeddingModelService {

    private EmbeddingModel embeddingModel;

    @Override
    public List<Embedding> embedAll(List<TextSegment> segments) {
        return embeddingModel.embedAll(segments).content();
    }

    @Override
    public Embedding embed(String text) {
        return embeddingModel.embed(text).content();
    }

    @Override
    public int dimension() {
        return embeddingModel.dimension();
    }

    @PostConstruct
    private void init() {
        PoolingMode poolingMode = PoolingMode.MEAN;
        String embeddingModelPath = CommonProperties.INSTANCE().getEmbeddingModel();
        String embeddingModelTokenizer = CommonProperties.INSTANCE().getEmbeddingModelTokenizer();
        embeddingModel = new OnnxEmbeddingModel(embeddingModelPath, embeddingModelTokenizer, poolingMode);
    }


}
