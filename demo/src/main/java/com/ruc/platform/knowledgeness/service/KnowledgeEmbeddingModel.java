package com.ruc.platform.knowledgeness.service;

import java.util.List;

@FunctionalInterface
public interface KnowledgeEmbeddingModel {
    double[] embed(String text);

    default List<double[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        return texts.stream().map(this::embed).toList();
    }

    default boolean available() {
        return true;
    }
}
