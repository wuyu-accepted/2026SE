package com.ruc.platform.admin.knowledge.service;

import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticleVersion;
import com.ruc.platform.knowledgeness.mapper.KnowledgeArticleMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeArticleVersionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeGovernanceAdminServiceTest {

    @Test
    void expiredPublishedArticleShouldBeTakenDown() {
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        AdminKnowledgeGovernanceService service = new AdminKnowledgeGovernanceService(articleMapper, mock(KnowledgeArticleVersionMapper.class));
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(801L);
        article.setStatus(1);
        article.setEffectiveTo(LocalDateTime.now().minusDays(1));
        when(articleMapper.selectList(any())).thenReturn(java.util.List.of(article));

        int count = service.takeDownExpiredArticles();

        ArgumentCaptor<KnowledgeArticle> captor = ArgumentCaptor.forClass(KnowledgeArticle.class);
        verify(articleMapper).updateById(captor.capture());
        assertThat(count).isEqualTo(1);
        assertThat(captor.getValue().getStatus()).isEqualTo(2);
    }

    @Test
    void rollbackVersionShouldRestoreSnapshotFields() {
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        KnowledgeArticleVersionMapper versionMapper = mock(KnowledgeArticleVersionMapper.class);
        AdminKnowledgeGovernanceService service = new AdminKnowledgeGovernanceService(articleMapper, versionMapper);
        KnowledgeArticleVersion version = new KnowledgeArticleVersion();
        version.setArticleId(802L);
        version.setVersionNo(2);
        version.setSnapshotJson("{\"title\":\"旧政策\",\"summary\":\"旧摘要\",\"content\":\"旧内容\"}");
        when(versionMapper.selectById(9001L)).thenReturn(version);
        when(articleMapper.selectById(802L)).thenReturn(new KnowledgeArticle());

        service.rollbackVersion(9001L, 5001L);

        ArgumentCaptor<KnowledgeArticle> captor = ArgumentCaptor.forClass(KnowledgeArticle.class);
        verify(articleMapper).updateById(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("旧政策");
        assertThat(captor.getValue().getVersionNo()).isEqualTo(3);
    }
    @Test
    void adminServiceShouldExposeVersionsDuplicatesAndExpireAction() {
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        KnowledgeArticleVersionMapper versionMapper = mock(KnowledgeArticleVersionMapper.class);
        AdminKnowledgeGovernanceService governanceService = new AdminKnowledgeGovernanceService(articleMapper, versionMapper);
        AdminKnowledgeServiceImpl service = new AdminKnowledgeServiceImpl(
                articleMapper,
                mock(com.ruc.platform.knowledgeness.mapper.KnowledgeTemplateMapper.class),
                mock(com.ruc.platform.knowledgeness.mapper.KnowledgeCategoryMapper.class),
                mock(com.ruc.platform.knowledgeness.mapper.KnowledgeBehaviorEventMapper.class),
                mock(com.ruc.platform.knowledgeness.mapper.KnowledgeRecommendationLogMapper.class),
                mock(com.ruc.platform.knowledgeness.mapper.KnowledgeSynonymGroupMapper.class),
                mock(com.ruc.platform.knowledgeness.mapper.KnowledgeRecommendWeightConfigMapper.class),
                new com.ruc.platform.knowledgeness.service.KnowledgeContentRenderer(),
                mock(com.ruc.platform.knowledgeness.service.KnowledgeIndexingService.class),
                governanceService
        );
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(803L);
        article.setDuplicateSignature("abc");
        KnowledgeArticleVersion version = new KnowledgeArticleVersion();
        version.setArticleId(803L);
        version.setVersionNo(1);
        when(articleMapper.selectById(803L)).thenReturn(article);
        when(versionMapper.selectList(any())).thenReturn(java.util.List.of(version));
        when(articleMapper.selectList(any())).thenReturn(java.util.List.of(article));

        assertThat(service.listVersions(803L)).hasSize(1);
        assertThat(service.findDuplicates(803L)).hasSize(1);
        assertThat(service.takeDownExpiredArticles()).isEqualTo(1);
    }

}
