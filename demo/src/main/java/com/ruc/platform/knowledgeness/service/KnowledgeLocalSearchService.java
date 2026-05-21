package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
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
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
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
                document.add(new TextField("content", String.join(" ", safe(article.getContent()), safe(article.getAnswer()), safe(article.getTags()), safe(article.getSourceContent()), safe(article.getExtractedText())), Field.Store.NO));
                writer.updateDocument(new Term("id", String.valueOf(article.getId())), document);
            }
        } catch (Exception e) {
            log.warn("Lucene 索引写入失败，articleId: {}, error: {}", article.getId(), e.getMessage());
        }
    }

    public List<Long> searchArticleIds(String keyword, int limit) {
        List<Long> ids = new ArrayList<>();
        if (keyword == null || keyword.isBlank()) {
            return ids;
        }
        try {
            Path indexPath = indexPath();
            if (!Files.exists(indexPath)) {
                return ids;
            }
            try (FSDirectory directory = FSDirectory.open(indexPath); DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                MultiFieldQueryParser parser = new MultiFieldQueryParser(new String[]{"title", "summary", "content"}, new StandardAnalyzer());
                Query query = parser.parse(MultiFieldQueryParser.escape(keyword));
                TopDocs docs = searcher.search(query, Math.max(1, limit));
                for (ScoreDoc scoreDoc : docs.scoreDocs) {
                    Document doc = searcher.doc(scoreDoc.doc);
                    ids.add(Long.valueOf(doc.get("id")));
                }
            }
        } catch (Exception e) {
            log.warn("Lucene 检索失败，keyword: {}, error: {}", keyword, e.getMessage());
        }
        return ids;
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
}
