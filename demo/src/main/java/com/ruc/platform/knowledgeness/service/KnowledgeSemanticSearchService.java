package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSemanticSearchService {

    private static final int VECTOR_DIMENSIONS = 384;

    private final KnowledgeIntelligenceProperties properties;

    public void upsertArticle(KnowledgeArticle article) {
        if (article == null || article.getId() == null || !properties.getSemantic().isEnabled()) {
            return;
        }
        try {
            Path vectorPath = vectorPath(article.getId());
            Files.createDirectories(vectorPath.getParent());
            VectorRecord record = VectorRecord.from(article.getId(), embed(articleText(article)));
            Files.writeString(vectorPath, record.serialize(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("知识库语义向量写入失败，articleId: {}, error: {}", article.getId(), e.getMessage());
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
        Set<String> keywords = new LinkedHashSet<>();
        if (keyword == null || keyword.isBlank()) {
            return keywords;
        }
        keywords.add(keyword.trim());
        for (Map.Entry<String, List<String>> entry : synonymGroups().entrySet()) {
            if (keyword.contains(entry.getKey()) || entry.getValue().stream().anyMatch(keyword::contains)) {
                keywords.add(entry.getKey());
                keywords.addAll(entry.getValue());
            }
        }
        return keywords;
    }

    private double[] embed(String text) {
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
        String normalized = normalizeText(text);
        if (normalized.isBlank()) {
            return tokens;
        }
        tokens.add(normalized);
        for (String part : normalized.split("[\\s,，;；。.!！?？、/]+")) {
            if (!part.isBlank()) {
                tokens.add(part);
                tokens.addAll(ngrams(part));
                tokens.addAll(expandKeywords(part));
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

    private Map<String, List<String>> synonymGroups() {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        groups.put("奖助学金", List.of("助学金", "奖学金", "补助", "资助", "困难认定", "家庭经济困难"));
        groups.put("请假", List.of("离校", "销假", "返校", "外出报备"));
        groups.put("证明", List.of("在校证明", "学籍证明", "户籍证明", "成绩证明"));
        groups.put("就业", List.of("三方协议", "就业推荐表", "签约", "派遣"));
        groups.put("党员", List.of("入党", "积极分子", "发展对象", "预备党员", "转正"));
        return groups;
    }

    private String articleText(KnowledgeArticle article) {
        return String.join(" ", safe(article.getTitle()), safe(article.getSummary()), safe(article.getContent()), safe(article.getAnswer()), safe(article.getTags()), safe(article.getSourceContent()), safe(article.getExtractedText()));
    }

    private double cosine(double[] left, double[] right) {
        double dot = 0;
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            dot += left[i] * right[i];
        }
        return dot;
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

    private String normalizeText(String value) {
        return safe(value).toLowerCase(Locale.ROOT).trim();
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
            double[] vector = new double[VECTOR_DIMENSIONS];
            for (int i = 0; i < Math.min(parts.length, vector.length); i++) {
                vector[i] = Double.parseDouble(parts[i]);
            }
            return new VectorRecord(id, vector);
        }
    }
}
