package com.ruc.platform.admin.knowledge.service;

import com.ruc.platform.admin.knowledge.dto.KnowledgeArticleSaveDTO;
import com.ruc.platform.admin.knowledge.dto.KnowledgeTemplateSaveDTO;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.knowledgeness.entity.KnowledgeTemplate;
import com.ruc.platform.knowledgeness.mapper.KnowledgeArticleMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeBehaviorEventMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeCategoryMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeRecommendationLogMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeTemplateMapper;
import com.ruc.platform.knowledgeness.service.KnowledgeContentRenderer;
import com.ruc.platform.knowledgeness.service.KnowledgeIndexingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminKnowledgeServiceImplTest {

    @Test
    void createArticleSetsCreatorAndUpdater() {
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        AdminKnowledgeServiceImpl service = new AdminKnowledgeServiceImpl(articleMapper, mock(KnowledgeTemplateMapper.class), mock(KnowledgeCategoryMapper.class), mock(KnowledgeBehaviorEventMapper.class), mock(KnowledgeRecommendationLogMapper.class), new KnowledgeContentRenderer(), mock(KnowledgeIndexingService.class));
        KnowledgeArticleSaveDTO dto = new KnowledgeArticleSaveDTO();
        dto.setTitle("奖助政策说明");
        dto.setSummary("说明奖助政策");
        dto.setFileId(9201L);
        dto.setContentType("policy");
        dto.setTags("奖助,政策");

        service.createArticle(88L, dto);

        ArgumentCaptor<KnowledgeArticle> captor = ArgumentCaptor.forClass(KnowledgeArticle.class);
        verify(articleMapper).insert(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(88L);
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(88L);
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
        assertThat(captor.getValue().getFileId()).isEqualTo(9201L);
    }

    @Test
    void createMarkdownArticleAllowsEditorModeWithoutFile() {
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        AdminKnowledgeServiceImpl service = new AdminKnowledgeServiceImpl(articleMapper, mock(KnowledgeTemplateMapper.class), mock(KnowledgeCategoryMapper.class), mock(KnowledgeBehaviorEventMapper.class), mock(KnowledgeRecommendationLogMapper.class), new KnowledgeContentRenderer(), mock(KnowledgeIndexingService.class));
        KnowledgeArticleSaveDTO dto = new KnowledgeArticleSaveDTO();
        dto.setTitle("Markdown 办事指南");
        dto.setSummary("在线编排指南");
        dto.setContentMode("editor");
        dto.setEditorType("markdown");
        dto.setSourceContent("# 标题\n\n![图](file:9201)");

        service.createArticle(88L, dto);

        ArgumentCaptor<KnowledgeArticle> captor = ArgumentCaptor.forClass(KnowledgeArticle.class);
        verify(articleMapper).insert(captor.capture());
        assertThat(captor.getValue().getContentMode()).isEqualTo("editor");
        assertThat(captor.getValue().getEditorType()).isEqualTo("markdown");
        assertThat(captor.getValue().getSourceContent()).contains("# 标题");
    }


    @Test
    void createFileArticleEnqueuesAsyncIndexing() {
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        KnowledgeIndexingService indexingService = mock(KnowledgeIndexingService.class);
        AdminKnowledgeServiceImpl service = new AdminKnowledgeServiceImpl(articleMapper, mock(KnowledgeTemplateMapper.class), mock(KnowledgeCategoryMapper.class), mock(KnowledgeBehaviorEventMapper.class), mock(KnowledgeRecommendationLogMapper.class), new KnowledgeContentRenderer(), indexingService);
        KnowledgeArticleSaveDTO dto = new KnowledgeArticleSaveDTO();
        dto.setTitle("奖助政策附件");
        dto.setSummary("请查看附件");
        dto.setContentMode("file");
        dto.setFileId(9201L);

        service.createArticle(88L, dto);

        ArgumentCaptor<KnowledgeArticle> captor = ArgumentCaptor.forClass(KnowledgeArticle.class);
        verify(articleMapper).insert(captor.capture());
        assertThat(captor.getValue().getExtractStatus()).isEqualTo("pending");
        verify(indexingService).enqueueArticle(captor.getValue().getId(), "save");
    }

    @Test
    void publishingArticleSetsPublishTimeAndUpdater() {
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(20001L);
        article.setStatus(0);
        when(articleMapper.selectById(20001L)).thenReturn(article);
        AdminKnowledgeServiceImpl service = new AdminKnowledgeServiceImpl(articleMapper, mock(KnowledgeTemplateMapper.class), mock(KnowledgeCategoryMapper.class), mock(KnowledgeBehaviorEventMapper.class), mock(KnowledgeRecommendationLogMapper.class), new KnowledgeContentRenderer(), mock(KnowledgeIndexingService.class));

        service.updateArticleStatus(99L, 20001L, 1);

        ArgumentCaptor<KnowledgeArticle> captor = ArgumentCaptor.forClass(KnowledgeArticle.class);
        verify(articleMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(99L);
        assertThat(captor.getValue().getPublishTime()).isNotNull();
    }

    @Test
    void createTemplatePreservesFileIdAndOperator() {
        KnowledgeTemplateMapper templateMapper = mock(KnowledgeTemplateMapper.class);
        AdminKnowledgeServiceImpl service = new AdminKnowledgeServiceImpl(mock(KnowledgeArticleMapper.class), templateMapper, mock(KnowledgeCategoryMapper.class), mock(KnowledgeBehaviorEventMapper.class), mock(KnowledgeRecommendationLogMapper.class), new KnowledgeContentRenderer(), mock(KnowledgeIndexingService.class));
        KnowledgeTemplateSaveDTO dto = new KnowledgeTemplateSaveDTO();
        dto.setName("在校证明模板");
        dto.setDescription("证明模板");
        dto.setCategory("日常服务");
        dto.setFileId(9201L);
        dto.setFormat("DOCX");

        service.createTemplate(77L, dto);

        ArgumentCaptor<KnowledgeTemplate> captor = ArgumentCaptor.forClass(KnowledgeTemplate.class);
        verify(templateMapper).insert(captor.capture());
        assertThat(captor.getValue().getFileId()).isEqualTo(9201L);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(77L);
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(77L);
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }

    @Test
    void statsShouldIncludeBehaviorAndRecommendationCounts() {
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        KnowledgeTemplateMapper templateMapper = mock(KnowledgeTemplateMapper.class);
        KnowledgeCategoryMapper categoryMapper = mock(KnowledgeCategoryMapper.class);
        KnowledgeBehaviorEventMapper behaviorEventMapper = mock(KnowledgeBehaviorEventMapper.class);
        KnowledgeRecommendationLogMapper recommendationLogMapper = mock(KnowledgeRecommendationLogMapper.class);
        when(articleMapper.selectCount(null)).thenReturn(2L);
        when(templateMapper.selectCount(null)).thenReturn(3L);
        when(categoryMapper.selectCount(null)).thenReturn(4L);
        when(behaviorEventMapper.selectCount(null)).thenReturn(5L);
        when(recommendationLogMapper.selectCount(null)).thenReturn(6L);
        AdminKnowledgeServiceImpl service = new AdminKnowledgeServiceImpl(articleMapper, templateMapper, categoryMapper, behaviorEventMapper, recommendationLogMapper, new KnowledgeContentRenderer(), mock(KnowledgeIndexingService.class));

        java.util.Map<String, Object> stats = service.stats();

        assertThat(stats).containsEntry("behaviorEventCount", 5L);
        assertThat(stats).containsEntry("recommendationLogCount", 6L);
    }
}
