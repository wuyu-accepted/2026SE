package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Component;

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
@RequiredArgsConstructor
public class KnowledgeLocalSearchService {

    private final KnowledgeIntelligenceProperties properties;

    public void indexArticle(KnowledgeArticle article) {
        if (article == null || article.getId() == null) {
            return;
        }
        try {
            Path indexPath = indexPath();
            Files.createDirectories(indexPath);
            try (FSDirectory directory = FSDirectory.open(indexPath); IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(new StandardAnalyzer()))) {
                Document document = new Document();
                document.add(new StringField("id", String.valueOf(article.getId()), Field.Store.YES));
                document.add(new TextField("title", safe(article.getTitle()), Field.Store.YES));
                document.add(new TextField("summary", safe(article.getSummary()), Field.Store.YES));
                document.add(new TextField("content", searchableContent(article), Field.Store.YES));
                writer.updateDocument(new Term("id", String.valueOf(article.getId())), document);
            }
        } catch (Exception e) {
            log.warn("Lucene 索引写入失败，articleId: {}, error: {}", article.getId(), e.getMessage());
        }
    }

    public List<Long> searchArticleIds(String keyword, int limit) {
        return search(keyword, limit).stream().map(SearchHit::getArticleId).collect(Collectors.toList());
    }

    public List<SearchHit> search(String keyword, int limit) {
        List<SearchHit> hits = new ArrayList<>();
        if (keyword == null || keyword.isBlank()) {
            return hits;
        }
        try {
            Path indexPath = indexPath();
            if (!Files.exists(indexPath)) {
                return hits;
            }
            Set<String> queryTerms = terms(keyword);
            try (FSDirectory directory = FSDirectory.open(indexPath); DirectoryReader reader = DirectoryReader.open(directory)) {
                for (int i = 0; i < reader.maxDoc(); i++) {
                    Document document = reader.document(i);
                    SearchHit hit = score(document, queryTerms);
                    if (hit.getScore() > 0) {
                        hits.add(hit);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Lucene 检索失败，keyword: {}, error: {}", keyword, e.getMessage());
        }
        return hits.stream()
                .sorted(Comparator.comparing(SearchHit::getScore, Comparator.reverseOrder()))
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    public List<String> suggest(String keyword, int limit) {
        List<String> suggestions = new ArrayList<>();
        if (keyword == null || keyword.isBlank()) {
            return suggestions;
        }
        try {
            Path indexPath = indexPath();
            if (!Files.exists(indexPath)) {
                return suggestions;
            }
            String normalized = normalize(keyword);
            try (FSDirectory directory = FSDirectory.open(indexPath); DirectoryReader reader = DirectoryReader.open(directory)) {
                for (int i = 0; i < reader.maxDoc(); i++) {
                    Document document = reader.document(i);
                    String title = document.get("title");
                    if (title != null && (normalize(title).contains(normalized) || similarity(normalized, normalize(title)) >= 0.35)) {
                        suggestions.add(title);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Lucene 搜索建议失败，keyword: {}, error: {}", keyword, e.getMessage());
        }
        return suggestions.stream().distinct().limit(Math.max(1, limit)).collect(Collectors.toList());
    }

    private SearchHit score(Document document, Set<String> queryTerms) {
        long articleId = Long.parseLong(document.get("id"));
        String title = safe(document.get("title"));
        String summary = safe(document.get("summary"));
        String content = safe(document.get("content"));
        double score = 0;
        List<String> reasons = new ArrayList<>();
        int titleHits = countHits(title, queryTerms);
        int summaryHits = countHits(summary, queryTerms);
        int contentHits = countHits(content, queryTerms);
        if (titleHits > 0) {
            score += titleHits * 6;
            reasons.add("标题命中" + titleHits + "项");
        }
        if (summaryHits > 0) {
            score += summaryHits * 3;
            reasons.add("摘要命中" + summaryHits + "项");
        }
        if (contentHits > 0) {
            score += contentHits;
            reasons.add("正文命中" + contentHits + "项");
        }
        String source = !titleHitsEqualsZero(titleHits) ? title : (!summaryHitsEqualsZero(summaryHits) ? summary : content);
        return new SearchHit(articleId, score, highlight(source, queryTerms), String.join("；", reasons));
    }

    private boolean titleHitsEqualsZero(int hits) {
        return hits == 0;
    }

    private boolean summaryHitsEqualsZero(int hits) {
        return hits == 0;
    }

    private int countHits(String text, Set<String> queryTerms) {
        String normalized = normalize(text);
        int hits = 0;
        for (String term : queryTerms) {
            if (!term.isBlank() && normalized.contains(term)) {
                hits++;
            }
        }
        return hits;
    }

    private String highlight(String text, Set<String> queryTerms) {
        String result = safe(text);
        List<String> orderedTerms = queryTerms.stream()
                .filter(term -> term.length() >= 2)
                .sorted(Comparator.comparing(String::length).reversed())
                .toList();
        for (String term : orderedTerms) {
            result = result.replace(term, "<mark>" + term + "</mark>");
        }
        if (result.length() > 180) {
            result = result.substring(0, 180) + "...";
        }
        return result;
    }

    private Set<String> terms(String keyword) {
        Set<String> terms = new LinkedHashSet<>();
        String normalized = normalize(keyword);
        if (normalized.isBlank()) {
            return terms;
        }
        terms.add(normalized);
        for (String part : normalized.split("[\\s,，;；]+")) {
            if (!part.isBlank()) {
                terms.add(part);
                terms.addAll(ngrams(part));
            }
        }
        terms.addAll(ngrams(normalized));
        return terms;
    }

    private Set<String> ngrams(String value) {
        Set<String> grams = new LinkedHashSet<>();
        String text = normalize(value);
        for (int size = 2; size <= 3; size++) {
            if (text.length() < size) {
                continue;
            }
            for (int i = 0; i <= text.length() - size; i++) {
                grams.add(text.substring(i, i + size));
            }
        }
        return grams;
    }

    private double similarity(String left, String right) {
        Set<String> leftTerms = ngrams(left);
        Set<String> rightTerms = ngrams(right);
        if (leftTerms.isEmpty() || rightTerms.isEmpty()) {
            return 0;
        }
        long intersection = leftTerms.stream().filter(rightTerms::contains).count();
        long union = leftTerms.size() + rightTerms.size() - intersection;
        return union == 0 ? 0 : (double) intersection / union;
    }

    private String searchableContent(KnowledgeArticle article) {
        return String.join(" ", safe(article.getContent()), safe(article.getAnswer()), safe(article.getTags()), safe(article.getSourceContent()), safe(article.getExtractedText()));
    }

    private String normalize(String value) {
        return safe(value).toLowerCase(Locale.ROOT).replaceAll("\\s+", "").trim();
    }

    private Path indexPath() {
        String configured = properties.getSearch().getIndexPath();
        if (configured == null || configured.isBlank()) {
            configured = System.getProperty("user.home") + "/ruc-platform/lucene/knowledge";
        }
        return Path.of(configured.replace("${user.home}", System.getProperty("user.home")));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Data
    @AllArgsConstructor
    public static class SearchHit {
        private Long articleId;
        private Double score;
        private String highlight;
        private String scoreExplanation;
    }
}
