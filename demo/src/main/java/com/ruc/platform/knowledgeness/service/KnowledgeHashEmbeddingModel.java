package com.ruc.platform.knowledgeness.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class KnowledgeHashEmbeddingModel implements KnowledgeEmbeddingModel {

    private static final int VECTOR_DIMENSIONS = 384;
    private final KnowledgeSynonymService synonymService;

    public KnowledgeHashEmbeddingModel(KnowledgeSynonymService synonymService) {
        this.synonymService = synonymService;
    }

    @Override
    public double[] embed(String text) {
        double[] vector = new double[VECTOR_DIMENSIONS];
        for (String token : semanticTokens(text)) {
            int hash = Math.floorMod(token.hashCode(), VECTOR_DIMENSIONS);
            vector[hash] += token.length() <= 2 ? 1.0 : 1.6;
        }
        normalize(vector);
        return vector;
    }

    private Set<String> semanticTokens(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
        if (normalized.isBlank()) {
            return tokens;
        }
        tokens.add(normalized);
        for (String part : normalized.split("[\\s,，;；。.!！?？、/]+")) {
            if (!part.isBlank()) {
                tokens.add(part);
                tokens.addAll(ngrams(part));
                tokens.addAll(synonymService.expand(part));
            }
        }
        tokens.addAll(ngrams(normalized));
        return tokens;
    }

    private Set<String> ngrams(String value) {
        String text = value.replaceAll("\\s+", "");
        Set<String> grams = new LinkedHashSet<>();
        for (int size = 2; size <= 4; size++) {
            if (text.length() < size) {
                continue;
            }
            for (int i = 0; i <= text.length() - size; i++) {
                grams.add(text.substring(i, i + size));
            }
        }
        return grams;
    }

    private void normalize(double[] vector) {
        double sum = 0;
        for (double value : vector) {
            sum += value * value;
        }
        if (sum == 0) {
            return;
        }
        double length = Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / length;
        }
    }
}
