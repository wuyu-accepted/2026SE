package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.notice.entity.Notice;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class KnowledgeLocalSearchService {

    private final KnowledgeIntelligenceProperties properties;
    private final KnowledgeSynonymService synonymService;

    @Autowired
    public KnowledgeLocalSearchService(KnowledgeIntelligenceProperties properties, KnowledgeSynonymService synonymService) {
        this.properties = properties;
        this.synonymService = synonymService;
    }

    public KnowledgeLocalSearchService(KnowledgeIntelligenceProperties properties) {
        this(properties, new KnowledgeSynonymService(null, properties));
    }

    public void indexArticle(KnowledgeArticle article) {
        if (article == null || article.getId() == null) {
            return;
        }
        indexSource("knowledge", article.getId(), safe(article.getTitle()), safe(article.getSummary()), searchableContent(article));
    }

    public void indexNotice(Notice notice) {
        if (notice == null || notice.getId() == null) {
            return;
        }
        indexSource("notice", notice.getId(), safe(notice.getTitle()), safe(notice.getSummary()), searchableNoticeContent(notice));
    }

    public void deleteSource(String sourceType, Long sourceId) {
        if (sourceType == null || sourceType.isBlank() || sourceId == null) {
            return;
        }
        try {
            Path indexPath = indexPath();
            if (!Files.exists(indexPath)) {
                return;
            }
            try (FSDirectory directory = FSDirectory.open(indexPath); IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer()))) {
                writer.deleteDocuments(new Term("id", sourceType + ":" + sourceId));
            }
        } catch (Exception e) {
            log.warn("Lucene 索引删除失败，source: {}:{}, error: {}", sourceType, sourceId, e.getMessage());
        }
    }

    private void indexSource(String sourceType, Long sourceId, String title, String summary, String content) {
        try {
            Path indexPath = indexPath();
            Files.createDirectories(indexPath);
            try (FSDirectory directory = FSDirectory.open(indexPath); IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer()))) {
                Document document = new Document();
                String sourceKey = sourceType + ":" + sourceId;
                document.add(new StringField("id", sourceKey, Field.Store.YES));
                document.add(new StringField("sourceType", sourceType, Field.Store.YES));
                document.add(new StringField("sourceId", String.valueOf(sourceId), Field.Store.YES));
                document.add(new TextField("title", safe(title), Field.Store.YES));
                document.add(new TextField("summary", safe(summary), Field.Store.YES));
                document.add(new TextField("content", safe(content), Field.Store.YES));
                writer.updateDocument(new Term("id", sourceKey), document);
            }
        } catch (Exception e) {
            log.warn("Lucene 索引写入失败，source: {}:{}, error: {}", sourceType, sourceId, e.getMessage());
        }
    }

    public List<Long> searchArticleIds(String keyword, int limit) {
        return search(keyword, limit).stream()
                .filter(hit -> "knowledge".equals(hit.getSourceType()))
                .map(SearchHit::getSourceId)
                .collect(Collectors.toList());
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
            Set<String> queryTerms = expandedTerms(keyword);
            String correctedKeyword = bestCorrection(keyword);
            try (FSDirectory directory = FSDirectory.open(indexPath); DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                Query query = buildQuery(queryTerms);
                TopDocs topDocs = searcher.search(query, Math.max(1, limit * 3));
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document document = searcher.doc(scoreDoc.doc);
                    SearchHit hit = score(document, queryTerms, scoreDoc.score, explain(searcher, query, scoreDoc.doc), correctedKeyword);
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
                    if (title != null && (normalize(title).contains(normalized) || similarity(normalized, normalize(title)) >= 0.25)) {
                        suggestions.add(title);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Lucene 搜索建议失败，keyword: {}, error: {}", keyword, e.getMessage());
        }
        return suggestions.stream().distinct().limit(Math.max(1, limit)).collect(Collectors.toList());
    }

    public String correct(String keyword) {
        return bestCorrection(keyword);
    }

    private Query buildQuery(Set<String> queryTerms) throws Exception {
        MultiFieldQueryParser parser = new MultiFieldQueryParser(new String[]{"title", "summary", "content"}, analyzer(), new java.util.HashMap<>() {{
            put("title", 6F);
            put("summary", 3F);
            put("content", 1F);
        }});
        parser.setDefaultOperator(MultiFieldQueryParser.Operator.OR);
        String query = queryTerms.stream()
                .map(MultiFieldQueryParser::escape)
                .collect(Collectors.joining(" "));
        return parser.parse(query.isBlank() ? "*" : query);
    }

    private SearchHit score(Document document, Set<String> queryTerms, float luceneScore, String luceneExplanation, String correctedKeyword) {
        String sourceType = safe(document.get("sourceType"));
        if (sourceType.isBlank()) {
            sourceType = "knowledge";
        }
        long sourceId = parseSourceId(document);
        String title = safe(document.get("title"));
        String summary = safe(document.get("summary"));
        String content = safe(document.get("content"));
        double score = luceneScore;
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
        reasons.add("Lucene n-gram score=" + String.format(Locale.ROOT, "%.3f", luceneScore));
        if (!luceneExplanation.isBlank()) {
            reasons.add(luceneExplanation);
        }
        String source = titleHits > 0 ? title : (summaryHits > 0 ? summary : content);
        return new SearchHit(sourceType, sourceId, score, highlight(source, queryTerms), String.join("；", reasons), correctedKeyword);
    }

    private long parseSourceId(Document document) {
        String sourceId = document.get("sourceId");
        if (sourceId != null && !sourceId.isBlank()) {
            return Long.parseLong(sourceId);
        }
        String legacyId = document.get("id");
        if (legacyId != null && legacyId.contains(":")) {
            return Long.parseLong(legacyId.substring(legacyId.indexOf(':') + 1));
        }
        return Long.parseLong(legacyId);
    }

    private String explain(IndexSearcher searcher, Query query, int docId) {
        try {
            Explanation explanation = searcher.explain(query, docId);
            return explanation == null ? "" : "相关性解释=" + String.format(Locale.ROOT, "%.3f", explanation.getValue().doubleValue());
        } catch (Exception e) {
            return "";
        }
    }

    private String bestCorrection(String keyword) {
        List<String> suggestions = suggest(keyword, 1);
        return suggestions.isEmpty() ? keyword : suggestions.get(0);
    }

    private int countHits(String text, Set<String> queryTerms) {
        String normalized = normalize(text);
        int hits = 0;
        for (String term : queryTerms) {
            if (!term.isBlank() && normalized.contains(normalize(term))) {
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
        if (!result.contains("<mark>")) {
            for (String gram : ngrams(String.join("", orderedTerms))) {
                if (result.contains(gram)) {
                    result = result.replace(gram, "<mark>" + gram + "</mark>");
                    break;
                }
            }
        }
        if (result.length() > 180) {
            result = result.substring(0, 180) + "...";
        }
        return result;
    }

    private Set<String> expandedTerms(String keyword) {
        Set<String> terms = new LinkedHashSet<>();
        for (String synonym : synonymService.expand(keyword)) {
            terms.add(synonym);
            terms.addAll(terms(synonym));
        }
        return terms;
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

    private String searchableNoticeContent(Notice notice) {
        return String.join(" ", safe(notice.getContent()), safe(notice.getNoticeType()), safe(notice.getTag()));
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

    private Analyzer analyzer() {
        return new KnowledgeNgramAnalyzer();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Data
    @AllArgsConstructor
    public static class SearchHit {
        private String sourceType;
        private Long sourceId;
        private Double score;
        private String highlight;
        private String scoreExplanation;
        private String correctedKeyword;

        public Long getArticleId() {
            return sourceId;
        }
    }
}
