package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KnowledgeSemanticSearchService {

    private final KnowledgeIntelligenceProperties properties;
    private final KnowledgeSynonymService synonymService;
    private final KnowledgeEmbeddingModel embeddingModel;
    private final KnowledgeHashEmbeddingModel fallbackEmbeddingModel;
    private final KnowledgeVectorIndexService vectorIndexService;

    @Autowired
    public KnowledgeSemanticSearchService(KnowledgeIntelligenceProperties properties,
                                          KnowledgeSynonymService synonymService,
                                          KnowledgeOnnxEmbeddingModel onnxEmbeddingModel,
                                          KnowledgeHashEmbeddingModel fallbackEmbeddingModel,
                                          KnowledgeVectorIndexService vectorIndexService) {
        this.properties = properties;
        this.synonymService = synonymService;
        this.embeddingModel = onnxEmbeddingModel.available() ? onnxEmbeddingModel : fallbackEmbeddingModel;
        this.fallbackEmbeddingModel = fallbackEmbeddingModel;
        this.vectorIndexService = vectorIndexService;
    }

    public KnowledgeSemanticSearchService(KnowledgeIntelligenceProperties properties,
                                          KnowledgeSynonymService synonymService,
                                          KnowledgeEmbeddingModel embeddingModel) {
        this.properties = properties;
        this.synonymService = synonymService;
        this.embeddingModel = embeddingModel;
        this.fallbackEmbeddingModel = new KnowledgeHashEmbeddingModel(synonymService);
        this.vectorIndexService = new KnowledgeVectorIndexService(properties);
    }

    public KnowledgeSemanticSearchService(KnowledgeIntelligenceProperties properties) {
        this(properties, new KnowledgeSynonymService(null, properties), new KnowledgeHashEmbeddingModel(new KnowledgeSynonymService(null, properties)));
    }

    public KnowledgeSemanticSearchService() {
        this(new KnowledgeIntelligenceProperties(), new KnowledgeSynonymService(null, new KnowledgeIntelligenceProperties()), text -> new double[0]);
    }

    public void upsertArticle(KnowledgeArticle article) {
        if (article == null || article.getId() == null || !properties.getSemantic().isEnabled()) {
            return;
        }
        try {
            Path vectorPath = vectorPath(article.getId());
            Files.createDirectories(vectorPath.getParent());
            double[] vector = embed(articleText(article));
            VectorRecord record = VectorRecord.from(article.getId(), vector);
            Files.writeString(vectorPath, record.serialize(), StandardCharsets.UTF_8);
            vectorIndexService.upsert(article.getId(), vector);
        } catch (IOException e) {
            log.warn("知识库语义向量写入失败，articleId: {}, error: {}", article.getId(), e.getMessage());
        }
    }

    public void upsertArticles(List<KnowledgeArticle> articles) {
        if (articles == null || articles.isEmpty() || !properties.getSemantic().isEnabled()) {
            return;
        }
        List<String> texts = articles.stream().map(this::articleText).toList();
        List<double[]> vectors = embedBatch(texts);
        for (int i = 0; i < Math.min(articles.size(), vectors.size()); i++) {
            KnowledgeArticle article = articles.get(i);
            double[] vector = vectors.get(i);
            if (article.getId() == null || vector.length == 0) {
                continue;
            }
            try {
                Path vectorPath = vectorPath(article.getId());
                Files.createDirectories(vectorPath.getParent());
                Files.writeString(vectorPath, VectorRecord.from(article.getId(), vector).serialize(), StandardCharsets.UTF_8);
                vectorIndexService.upsert(article.getId(), vector);
            } catch (IOException e) {
                log.warn("知识库批量语义向量写入失败，articleId: {}, error: {}", article.getId(), e.getMessage());
            }
        }
    }

    public List<Long> searchArticleIds(String keyword, int limit) {
        return search(keyword, limit).stream().map(SemanticHit::getArticleId).collect(Collectors.toList());
    }

    public List<SemanticHit> search(String keyword, int limit) {
        if (keyword == null || keyword.isBlank() || !properties.getSemantic().isEnabled()) {
            return List.of();
        }
        try {
            Path dir = semanticPath();
            if (!Files.exists(dir)) {
                return List.of();
            }
            double[] queryVector = embed(keyword);
            List<KnowledgeVectorIndexService.VectorHit> indexedHits = vectorIndexService.search(queryVector, limit);
            if (!indexedHits.isEmpty()) {
                return indexedHits.stream()
                        .filter(hit -> hit.getScore() >= properties.getSemantic().getMinScore())
                        .map(hit -> new SemanticHit(hit.getArticleId(), hit.getScore(), "Lucene HNSW 本地向量相似度 " + String.format(Locale.ROOT, "%.3f", hit.getScore())))
                        .limit(Math.max(1, limit))
                        .collect(Collectors.toList());
            }
            List<SemanticHit> hits = new ArrayList<>();
            try (var paths = Files.list(dir)) {
                for (Path path : paths.filter(p -> p.getFileName().toString().endsWith(".vec")).toList()) {
                    VectorRecord record = VectorRecord.parse(Files.readString(path, StandardCharsets.UTF_8));
                    double score = cosine(queryVector, record.getVector());
                    if (score >= properties.getSemantic().getMinScore()) {
                        hits.add(new SemanticHit(record.getArticleId(), score, "本地语义向量相似度 " + String.format(Locale.ROOT, "%.3f", score)));
                    }
                }
            }
            return hits.stream()
                    .sorted(Comparator.comparing(SemanticHit::getScore, Comparator.reverseOrder()))
                    .limit(Math.max(1, limit))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("知识库语义检索失败，keyword: {}, error: {}", keyword, e.getMessage());
            return List.of();
        }
    }

    public Set<String> expandKeywords(String keyword) {
        return synonymService.expand(keyword);
    }

    private double[] embed(String text) {
        double[] vector = embeddingModel.embed(text);
        if (vector.length == 0 && fallbackEmbeddingModel != null) {
            vector = fallbackEmbeddingModel.embed(text);
        }
        return vector;
    }

    private List<double[]> embedBatch(List<String> texts) {
        List<double[]> vectors = embeddingModel.embedBatch(texts);
        if ((vectors.isEmpty() || vectors.stream().anyMatch(vector -> vector.length == 0)) && fallbackEmbeddingModel != null) {
            vectors = fallbackEmbeddingModel.embedBatch(texts);
        }
        return vectors;
    }

    private String articleText(KnowledgeArticle article) {
        return String.join(" ", safe(article.getTitle()), safe(article.getSummary()), safe(article.getContent()), safe(article.getAnswer()), safe(article.getTags()), safe(article.getSourceContent()), safe(article.getExtractedText()));
    }

    private double cosine(double[] left, double[] right) {
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private Path vectorPath(Long articleId) {
        return semanticPath().resolve(articleId + ".vec");
    }

    private Path semanticPath() {
        String configured = properties.getSemantic().getIndexPath();
        if (configured == null || configured.isBlank()) {
            configured = System.getProperty("user.home") + "/ruc-platform/vectors/knowledge";
        }
        return Path.of(configured.replace("${user.home}", System.getProperty("user.home")));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Data
    @AllArgsConstructor
    public static class SemanticHit {
        private Long articleId;
        private Double score;
        private String reason;
    }

    @Data
    @AllArgsConstructor
    private static class VectorRecord {
        private Long articleId;
        private double[] vector;

        static VectorRecord from(Long articleId, double[] vector) {
            return new VectorRecord(articleId, vector);
        }

        String serialize() {
            String values = java.util.Arrays.stream(vector)
                    .mapToObj(value -> String.format(Locale.ROOT, "%.8f", value))
                    .collect(Collectors.joining(","));
            return articleId + "\n" + values;
        }

        static VectorRecord parse(String text) {
            String[] lines = text.split("\n", 2);
            Long id = Long.valueOf(lines[0].trim());
            String[] parts = lines.length > 1 ? lines[1].split(",") : new String[0];
            double[] vector = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                vector[i] = Double.parseDouble(parts[i]);
            }
            return new VectorRecord(id, vector);
        }
    }
}
