package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeVectorIndexService {

    private static final String VECTOR_FIELD = "embedding";
    private final KnowledgeIntelligenceProperties properties;

    public void upsert(Long articleId, double[] vector) {
        if (articleId == null || vector == null || vector.length == 0 || !properties.getSemantic().isVectorIndexEnabled()) {
            return;
        }
        try {
            Path path = indexPath();
            Files.createDirectories(path);
            try (FSDirectory directory = FSDirectory.open(path); IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(new KeywordAnalyzer()))) {
                Document document = new Document();
                document.add(new StringField("id", String.valueOf(articleId), Field.Store.YES));
                document.add(new KnnFloatVectorField(VECTOR_FIELD, toFloat(vector), VectorSimilarityFunction.COSINE));
                writer.updateDocument(new Term("id", String.valueOf(articleId)), document);
            }
        } catch (Exception e) {
            log.warn("知识库 HNSW 向量索引写入失败，articleId: {}, error: {}", articleId, e.getMessage());
        }
    }

    public List<VectorHit> search(double[] vector, int limit) {
        List<VectorHit> hits = new ArrayList<>();
        if (vector == null || vector.length == 0 || !properties.getSemantic().isVectorIndexEnabled()) {
            return hits;
        }
        try {
            Path path = indexPath();
            if (!Files.exists(path)) {
                return hits;
            }
            try (FSDirectory directory = FSDirectory.open(path); DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                TopDocs docs = searcher.search(new KnnFloatVectorQuery(VECTOR_FIELD, toFloat(vector), Math.max(1, limit)), Math.max(1, limit));
                for (ScoreDoc scoreDoc : docs.scoreDocs) {
                    Document document = searcher.doc(scoreDoc.doc);
                    hits.add(new VectorHit(Long.valueOf(document.get("id")), (double) scoreDoc.score));
                }
            }
        } catch (Exception e) {
            log.warn("知识库 HNSW 向量检索失败: {}", e.getMessage());
        }
        return hits;
    }

    private Path indexPath() {
        String configured = properties.getSemantic().getVectorIndexPath();
        if (configured == null || configured.isBlank()) {
            String semanticPath = properties.getSemantic().getIndexPath();
            if (semanticPath != null && !semanticPath.isBlank()) {
                configured = Path.of(semanticPath.replace("${user.home}", System.getProperty("user.home"))).resolveSibling("knowledge-vectors").toString();
            } else {
                configured = System.getProperty("user.home") + "/ruc-platform/lucene/knowledge-vectors";
            }
        }
        return Path.of(configured.replace("${user.home}", System.getProperty("user.home")));
    }

    private float[] toFloat(double[] vector) {
        float[] result = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            result[i] = (float) vector[i];
        }
        return result;
    }

    @Data
    @AllArgsConstructor
    public static class VectorHit {
        private Long articleId;
        private Double score;
    }
}
