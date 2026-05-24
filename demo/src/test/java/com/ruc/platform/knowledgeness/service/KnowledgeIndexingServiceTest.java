package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.knowledgeness.entity.KnowledgeIndexTask;
import com.ruc.platform.knowledgeness.mapper.KnowledgeArticleMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeIndexTaskMapper;
import com.ruc.platform.notice.entity.Notice;
import com.ruc.platform.notice.mapper.NoticeMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeIndexingServiceTest {

    @Test
    void enqueueArticleCreatesPendingTask() {
        KnowledgeIndexTaskMapper taskMapper = mock(KnowledgeIndexTaskMapper.class);
        KnowledgeIndexingService service = new KnowledgeIndexingService(taskMapper, mock(KnowledgeArticleMapper.class), mock(KnowledgeFileTextExtractor.class), mock(KnowledgeLocalSearchService.class), mock(KnowledgeSemanticSearchService.class));

        service.enqueueArticle(20001L, "manual");

        ArgumentCaptor<KnowledgeIndexTask> captor = ArgumentCaptor.forClass(KnowledgeIndexTask.class);
        verify(taskMapper).insert(captor.capture());
        assertThat(captor.getValue().getArticleId()).isEqualTo(20001L);
        assertThat(captor.getValue().getStatus()).isEqualTo("pending");
        assertThat(captor.getValue().getTriggerType()).isEqualTo("manual");
    }

    @Test
    void processOneTaskExtractsAndIndexesArticle() {
        KnowledgeIndexTaskMapper taskMapper = mock(KnowledgeIndexTaskMapper.class);
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        KnowledgeFileTextExtractor extractor = mock(KnowledgeFileTextExtractor.class);
        KnowledgeLocalSearchService searchService = mock(KnowledgeLocalSearchService.class);
        KnowledgeSemanticSearchService vectorService = mock(KnowledgeSemanticSearchService.class);
        KnowledgeIndexingService service = new KnowledgeIndexingService(taskMapper, articleMapper, extractor, searchService, vectorService);
        KnowledgeIndexTask task = new KnowledgeIndexTask();
        task.setId(1L);
        task.setArticleId(20001L);
        task.setRetryCount(0);
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(20001L);
        article.setContentMode("file");
        article.setFileId(9201L);
        when(taskMapper.selectPendingOne(any(LocalDateTime.class))).thenReturn(task);
        when(articleMapper.selectById(20001L)).thenReturn(article);
        when(extractor.extract(9201L)).thenReturn("扫描件 OCR 文字 奖助政策");

        service.processOnePendingTask();

        ArgumentCaptor<KnowledgeArticle> articleCaptor = ArgumentCaptor.forClass(KnowledgeArticle.class);
        verify(articleMapper).updateById(articleCaptor.capture());
        assertThat(articleCaptor.getValue().getExtractedText()).contains("奖助政策");
        assertThat(articleCaptor.getValue().getExtractStatus()).isEqualTo("success");
        verify(searchService).indexArticle(articleCaptor.getValue());
        verify(vectorService).upsertArticle(articleCaptor.getValue());
    }

    @Test
    void rebuildAllArticlesEnqueuesEveryArticle() {
        KnowledgeIndexTaskMapper taskMapper = mock(KnowledgeIndexTaskMapper.class);
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        KnowledgeLocalSearchService localSearchService = mock(KnowledgeLocalSearchService.class);
        NoticeMapper noticeMapper = mock(NoticeMapper.class);
        KnowledgeIndexingService service = new KnowledgeIndexingService(taskMapper, articleMapper, mock(KnowledgeFileTextExtractor.class), localSearchService, mock(KnowledgeSemanticSearchService.class), noticeMapper, new com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties());
        KnowledgeArticle first = new KnowledgeArticle();
        first.setId(1L);
        KnowledgeArticle second = new KnowledgeArticle();
        second.setId(2L);
        Notice notice = new Notice();
        notice.setId(3L);
        when(articleMapper.selectList(null)).thenReturn(List.of(first, second));
        when(noticeMapper.selectList(any())).thenReturn(List.of(notice));

        int count = service.rebuildAll();

        assertThat(count).isEqualTo(3);
        verify(taskMapper, org.mockito.Mockito.times(2)).insert(any(KnowledgeIndexTask.class));
        verify(localSearchService).indexNotice(notice);
    }
}
