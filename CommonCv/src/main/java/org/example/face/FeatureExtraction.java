/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.example.face;

import ai.djl.MalformedModelException;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.translator.ImageFeatureExtractorFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import org.example.CommonProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class FeatureExtraction {

    private static final Logger log = LoggerFactory.getLogger(FeatureExtraction.class);

    private ZooModel<Image, float[]> model;

    public void init() {
        List<Float> mean =
                Arrays.asList(
                        127.5f / 255.0f,
                        127.5f / 255.0f,
                        127.5f / 255.0f,
                        128.0f / 255.0f,
                        128.0f / 255.0f,
                        128.0f / 255.0f);
        String normalize = mean.stream().map(Object::toString).collect(Collectors.joining(","));
        //String userHome = System.getProperty("user.home");
        Path path = Paths.get(CommonProperties.INSTANCE().getFeatureExtract());

        Criteria<Image, float[]> criteria =
                Criteria.builder()
                        .setTypes(Image.class, float[].class)
                        .optModelPath(path)
//                        .optModelUrls(
//                                "https://resources.djl.ai/test-models/pytorch/face_feature.zip")
                        .optModelName("face_feature") // specify model file prefix
                        .optArgument("normalize", normalize)
                        .optTranslatorFactory(new ImageFeatureExtractorFactory())
                        .optProgress(new ProgressBar())
                        .optEngine("PyTorch") // Use PyTorch engine
                        .build();
        try {
            model = criteria.loadModel();
        } catch (IOException | ModelException e ) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        log.info("Closing model");
        if (model != null) {
            model.close();
        }
    }

//    public float[] predict(Image img) {
//        Predictor<Image, float[]> predictor = model.newPredictor();
//        try {
//            return predictor.predict(img);
//        } catch (TranslateException e) {
//            throw new RuntimeException(e);
//        }
//    }

    public Predictor<Image, float[]> predictor() {
        return model.newPredictor();
    }

    public float calculSimilar(float[] feature1, float[] feature2) {
        float ret = 0.0f;
        float mod1 = 0.0f;
        float mod2 = 0.0f;
        int length = feature1.length;
        for (int i = 0; i < length; ++i) {
            ret += feature1[i] * feature2[i];
            mod1 += feature1[i] * feature1[i];
            mod2 += feature2[i] * feature2[i];
        }
        return (float) ((ret / Math.sqrt(mod1) / Math.sqrt(mod2) + 1) / 2.0f);
    }

    public ZooModel<Image, float[]> getModel() {
        return model;
    }
}
