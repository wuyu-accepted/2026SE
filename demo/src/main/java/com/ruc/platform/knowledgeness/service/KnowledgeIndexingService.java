package com.ruc.platform.knowledgeness.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.knowledgeness.entity.KnowledgeIndexTask;
import com.ruc.platform.knowledgeness.mapper.KnowledgeArticleMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeIndexTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIndexingService {

    private static final int MAX_RETRY = 3;

    private final KnowledgeIndexTaskMapper taskMapper;
    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeFileTextExtractor fileTextExtractor;
    private final KnowledgeLocalSearchService localSearchService;
    private final KnowledgeSemanticSearchService semanticSearchService;

    public void enqueueArticle(Long articleId, String triggerType) {
        if (articleId == null) {
            return;
        }
        KnowledgeIndexTask task = new KnowledgeIndexTask();
        task.setArticleId(articleId);
        task.setStatus("pending");
        task.setTriggerType(triggerType == null ? "manual" : triggerType);
        task.setRetryCount(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
    }

    public int rebuildAll() {
        List<KnowledgeArticle> articles = articleMapper.selectList(null);
        for (KnowledgeArticle article : articles) {
            enqueueArticle(article.getId(), "rebuild");
        }
        return articles.size();
    }

    @Scheduled(fixedDelay = 30000)
    public void processScheduled() {
        processOnePendingTask();
    }

    @Transactional(rollbackFor = Exception.class)
    public void processOnePendingTask() {
        KnowledgeIndexTask task = taskMapper.selectPendingOne(LocalDateTime.now());
        if (task == null) {
            return;
        }
        markRunning(task);
        try {
            KnowledgeArticle article = articleMapper.selectById(task.getArticleId());
            if (article == null) {
                throw new IllegalStateException("知识条目不存在");
            }
            refreshExtractedText(article);
            articleMapper.updateById(article);
            localSearchService.indexArticle(article);
            semanticSearchService.upsertArticle(article);
            task.setStatus("success");
            task.setLastError(null);
            task.setFinishedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.updateById(task);
        } catch (Exception e) {
            failTask(task, e);
        }
    }

    private void refreshExtractedText(KnowledgeArticle article) {
        if (!"file".equals(article.getContentMode())) {
            article.setExtractedText(article.getSourceContent());
            article.setExtractStatus("editor");
            article.setExtractError(null);
            article.setExtractedAt(LocalDateTime.now());
            return;
        }
        String extracted = fileTextExtractor.extract(article.getFileId());
        article.setExtractedText(extracted);
        article.setExtractStatus(extracted == null || extracted.isBlank() ? "empty" : "success");
        article.setExtractError(extracted == null || extracted.isBlank() ? "未从文件中抽取到可检索文本，可配置本地 OCR 后重建索引" : null);
        article.setExtractedAt(LocalDateTime.now());
    }

    private void markRunning(KnowledgeIndexTask task) {
        task.setStatus("running");
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    private void failTask(KnowledgeIndexTask task, Exception e) {
        int retry = task.getRetryCount() == null ? 0 : task.getRetryCount();
        retry++;
        task.setRetryCount(retry);
        task.setLastError(e.getMessage());
        task.setStatus(retry >= MAX_RETRY ? "failed" : "pending");
        task.setNextRetryAt(LocalDateTime.now().plusMinutes(Math.min(30, retry * 5L)));
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        log.warn("知识库索引任务失败，taskId: {}, retry: {}, error: {}", task.getId(), retry, e.getMessage());
    }
}
