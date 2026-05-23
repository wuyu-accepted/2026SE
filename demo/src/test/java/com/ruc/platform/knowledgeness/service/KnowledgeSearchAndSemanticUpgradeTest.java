package com.ruc.platform.knowledgeness.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.knowledgeness.entity.KnowledgeSynonymGroup;
import com.ruc.platform.knowledgeness.mapper.KnowledgeSynonymGroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeSearchAndSemanticUpgradeTest {

    @TempDir
    Path tempDir;

    @Test
    void luceneNgramSearchShouldReturnHighlightExplanationAndCorrection() {
        KnowledgeIntelligenceProperties properties = new KnowledgeIntelligenceProperties();
        properties.getSearch().setIndexPath(tempDir.resolve("lucene-ngram").toString());
        KnowledgeSynonymService synonymService = new KnowledgeSynonymService(null, properties);
        KnowledgeLocalSearchService searchService = new KnowledgeLocalSearchService(properties, synonymService);
        KnowledgeArticle article = article(101L, "家庭经济困难认定办理指南", "奖助学金申请材料清单");
        searchService.indexArticle(article);

        List<KnowledgeLocalSearchService.SearchHit> hits = searchService.search("家庭经济困哪认定", 5);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getArticleId()).isEqualTo(101L);
        assertThat(hits.get(0).getHighlight()).contains("<mark>");
        assertThat(hits.get(0).getScoreExplanation()).contains("Lucene n-gram");
        assertThat(hits.get(0).getCorrectedKeyword()).isEqualTo("家庭经济困难认定办理指南");
    }

    @Test
    void synonymServiceShouldLoadManagedTermsAndExpandBidirectionally() {
        KnowledgeSynonymGroupMapper mapper = mock(KnowledgeSynonymGroupMapper.class);
        KnowledgeSynonymGroup group = new KnowledgeSynonymGroup();
        group.setGroupName("困难资助");
        group.setTerms("困难补助,绿色通道,家庭经济困难认定");
        group.setStatus(1);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(group));
        KnowledgeSynonymService service = new KnowledgeSynonymService(mapper, new KnowledgeIntelligenceProperties());

        Set<String> expanded = service.expand("困难补助");

        assertThat(expanded).contains("困难补助", "绿色通道", "家庭经济困难认定");
    }

    @Test
    void semanticSearchShouldUsePluggableLocalEmbeddingModel() {
        KnowledgeIntelligenceProperties properties = new KnowledgeIntelligenceProperties();
        properties.getSemantic().setIndexPath(tempDir.resolve("vectors").toString());
        KnowledgeEmbeddingModel embeddingModel = text -> {
            double[] vector = new double[4];
            if (text.contains("困难") || text.contains("资助") || text.contains("助学金")) {
                vector[0] = 1D;
            } else {
                vector[1] = 1D;
            }
            return vector;
        };
        KnowledgeSemanticSearchService service = new KnowledgeSemanticSearchService(properties, new KnowledgeSynonymService(null, properties), embeddingModel);
        service.upsertArticle(article(201L, "奖助学金申请", "助学金材料与家庭经济情况核验"));

        List<Long> ids = service.searchArticleIds("困难学生资助怎么办", 5);

        assertThat(ids).containsExactly(201L);
    }

    @Test
    void onnxEmbeddingModelShouldStayOfflineAndReportUnavailableWithoutModel() {
        KnowledgeIntelligenceProperties properties = new KnowledgeIntelligenceProperties();
        properties.getSemantic().setOnnxModelPath(tempDir.resolve("missing.onnx").toString());
        KnowledgeOnnxEmbeddingModel model = new KnowledgeOnnxEmbeddingModel(properties);

        assertThat(model.available()).isFalse();
        assertThat(model.embed("困难资助")).isEmpty();
    }

    private KnowledgeArticle article(Long id, String title, String text) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(id);
        article.setTitle(title);
        article.setSummary(text);
        article.setExtractedText(text);
        return article;
    }
}
