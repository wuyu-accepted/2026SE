package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class KnowledgeOnnxTokenizer {

    private final KnowledgeIntelligenceProperties properties;
    private Map<String, Long> vocab;

    public KnowledgeOnnxTokenizer(KnowledgeIntelligenceProperties properties) {
        this.properties = properties;
    }

    public TokenizedBatch tokenizeBatch(java.util.List<String> texts) {
        int batchSize = texts == null ? 0 : texts.size();
        int maxTokens = Math.max(8, properties.getSemantic().getMaxTokens());
        long[][] inputIds = new long[batchSize][maxTokens];
        long[][] attentionMask = new long[batchSize][maxTokens];
        for (int row = 0; row < batchSize; row++) {
            long[] ids = tokenize(texts.get(row), maxTokens);
            System.arraycopy(ids, 0, inputIds[row], 0, Math.min(ids.length, maxTokens));
            for (int i = 0; i < maxTokens && i < ids.length; i++) {
                attentionMask[row][i] = ids[i] == 0 ? 0L : 1L;
            }
        }
        return new TokenizedBatch(inputIds, attentionMask);
    }

    private long[] tokenize(String text, int maxTokens) {
        String normalized = text == null ? "" : text.trim();
        long[] ids = new long[maxTokens];
        ids[0] = properties.getSemantic().getClsTokenId();
        int index = 1;
        for (int offset = 0; offset < normalized.length() && index < maxTokens - 1; offset++) {
            String token = String.valueOf(normalized.charAt(offset));
            ids[index++] = tokenId(token);
        }
        ids[index] = properties.getSemantic().getSepTokenId();
        return ids;
    }

    private long tokenId(String token) {
        Map<String, Long> loaded = vocab();
        Long id = loaded.get(token);
        if (id != null) {
            return id;
        }
        return Math.floorMod(token.hashCode(), properties.getSemantic().getVocabHashSize()) + 1L;
    }

    private Map<String, Long> vocab() {
        if (vocab != null) {
            return vocab;
        }
        vocab = new LinkedHashMap<>();
        String path = properties.getSemantic().getTokenizerPath();
        if (path == null || path.isBlank()) {
            return vocab;
        }
        Path vocabPath = Path.of(path.replace("${user.home}", System.getProperty("user.home")));
        if (!Files.exists(vocabPath)) {
            return vocab;
        }
        try {
            int lineNo = 0;
            for (String line : Files.readAllLines(vocabPath, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    vocab.put(line.trim(), (long) lineNo);
                }
                lineNo++;
            }
        } catch (IOException ignored) {
            vocab = new LinkedHashMap<>();
        }
        return vocab;
    }

    @Data
    public static class TokenizedBatch {
        private final long[][] inputIds;
        private final long[][] attentionMask;
    }
}
