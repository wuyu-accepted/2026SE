package com.ruc.platform.knowledgeness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.knowledgeness.entity.KnowledgeIndexTask;
import com.ruc.platform.knowledgeness.mapper.KnowledgeArticleMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeIndexTaskMapper;
import com.ruc.platform.notice.entity.Notice;
import com.ruc.platform.notice.mapper.NoticeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class KnowledgeIndexingService {

    private final KnowledgeIndexTaskMapper taskMapper;
    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeFileTextExtractor fileTextExtractor;
    private final KnowledgeLocalSearchService localSearchService;
    private final KnowledgeSemanticSearchService semanticSearchService;
    private final NoticeMapper noticeMapper;
    private final KnowledgeIntelligenceProperties properties;

    @Autowired
    public KnowledgeIndexingService(KnowledgeIndexTaskMapper taskMapper,
                                    KnowledgeArticleMapper articleMapper,
                                    KnowledgeFileTextExtractor fileTextExtractor,
                                    KnowledgeLocalSearchService localSearchService,
                                    KnowledgeSemanticSearchService semanticSearchService,
                                    @Autowired(required = false) NoticeMapper noticeMapper,
                                    KnowledgeIntelligenceProperties properties) {
        this.taskMapper = taskMapper;
        this.articleMapper = articleMapper;
        this.fileTextExtractor = fileTextExtractor;
        this.localSearchService = localSearchService;
        this.semanticSearchService = semanticSearchService;
        this.noticeMapper = noticeMapper;
        this.properties = properties;
    }

    public KnowledgeIndexingService(KnowledgeIndexTaskMapper taskMapper,
                                    KnowledgeArticleMapper articleMapper,
                                    KnowledgeFileTextExtractor fileTextExtractor,
                                    KnowledgeLocalSearchService localSearchService,
                                    KnowledgeSemanticSearchService semanticSearchService) {
        this(taskMapper, articleMapper, fileTextExtractor, localSearchService, semanticSearchService, null, new KnowledgeIntelligenceProperties());
    }

    public void enqueueArticle(Long articleId, String triggerType) {
        if (articleId == null || !properties.getIndexing().isEnabled()) {
            return;
        }
        KnowledgeIndexTask task = new KnowledgeIndexTask();
        task.setArticleId(articleId);
        task.setStatus("pending");
        task.setTriggerType(triggerType == null ? "manual" : triggerType);
        task.setRetryCount(0);
        task.setOcrUsed(false);
        task.setTaskLog("任务已入队");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        KnowledgeArticle indexedArticle = new KnowledgeArticle();
        indexedArticle.setId(articleId);
        indexedArticle.setLastIndexTaskId(task.getId());
        articleMapper.updateById(indexedArticle);
    }

    public int rebuildAll() {
        List<KnowledgeArticle> articles = articleMapper.selectList(null);
        for (KnowledgeArticle article : articles) {
            enqueueArticle(article.getId(), "rebuild");
        }
        int noticeCount = indexPublishedNotices();
        return articles.size() + noticeCount;
    }

    private int indexPublishedNotices() {
        if (noticeMapper == null) {
            return 0;
        }
        List<Notice> notices = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 1));
        for (Notice notice : notices) {
            localSearchService.indexNotice(notice);
        }
        return notices.size();
    }

    public List<KnowledgeIndexTask> listTasks(Long articleId, String status, int limit) {
        LambdaQueryWrapper<KnowledgeIndexTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(articleId != null, KnowledgeIndexTask::getArticleId, articleId)
                .eq(status != null && !status.isBlank(), KnowledgeIndexTask::getStatus, status)
                .orderByDesc(KnowledgeIndexTask::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100)));
        return taskMapper.selectList(wrapper);
    }

    public void retryTask(Long taskId) {
        KnowledgeIndexTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus("pending");
        task.setNextRetryAt(LocalDateTime.now());
        task.setLastError(null);
        task.setTaskLog(appendLog(task.getTaskLog(), "管理员手动重试"));
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    @Scheduled(fixedDelayString = "${knowledge.intelligence.indexing.fixed-delay-ms:30000}")
    public void processScheduled() {
        if (!properties.getIndexing().isEnabled()) {
            return;
        }
        int batchSize = Math.max(1, properties.getIndexing().getBatchSize());
        for (int i = 0; i < batchSize; i++) {
            if (!processOnePendingTask()) {
                break;
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean processOnePendingTask() {
        KnowledgeIndexTask task = taskMapper.selectPendingOne(LocalDateTime.now());
        if (task == null) {
            return false;
        }
        LocalDateTime startedAt = LocalDateTime.now();
        markRunning(task);
        try {
            KnowledgeArticle article = articleMapper.selectById(task.getArticleId());
            if (article == null) {
                throw new IllegalStateException("知识条目不存在");
            }
            refreshExtractedText(article, task);
            articleMapper.updateById(article);
            localSearchService.indexArticle(article);
            semanticSearchService.upsertArticle(article);
            task.setStatus("success");
            task.setLastError(null);
            task.setTaskLog(appendLog(task.getTaskLog(), "解析、全文索引、语义索引完成"));
            task.setFinishedAt(LocalDateTime.now());
            task.setDurationMs(Duration.between(startedAt, task.getFinishedAt()).toMillis());
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.updateById(task);
        } catch (Exception e) {
            failTask(task, e, startedAt);
        }
        return true;
    }

    private void refreshExtractedText(KnowledgeArticle article, KnowledgeIndexTask task) {
        if (!"file".equals(article.getContentMode())) {
            article.setExtractedText(article.getSourceContent());
            article.setExtractStatus("editor");
            article.setExtractError(null);
            article.setExtractedAt(LocalDateTime.now());
            task.setTaskLog(appendLog(task.getTaskLog(), "在线编排内容已写入索引"));
            return;
        }
        String extracted = fileTextExtractor.extract(article.getFileId());
        boolean ocrLikelyUsed = extracted != null && !extracted.isBlank() && (article.getFileId() != null);
        article.setOcrText(extracted);
        if (article.getOcrCorrectedText() != null && !article.getOcrCorrectedText().isBlank()) {
            extracted = article.getOcrCorrectedText();
            article.setOcrStatus("corrected");
        } else if (extracted == null || extracted.isBlank()) {
            article.setOcrStatus("empty");
        } else {
            article.setOcrStatus("success");
        }
        article.setExtractedText(extracted);
        article.setExtractStatus(extracted == null || extracted.isBlank() ? "empty" : "success");
        article.setExtractError(extracted == null || extracted.isBlank() ? "未从文件中抽取到可检索文本，可配置本地 OCR 后重建索引" : null);
        article.setOcrError(article.getExtractError());
        article.setExtractedAt(LocalDateTime.now());
        task.setOcrUsed(ocrLikelyUsed);
        task.setTaskLog(appendLog(task.getTaskLog(), "文件解析完成，文本长度=" + (extracted == null ? 0 : extracted.length())));
    }

    private void markRunning(KnowledgeIndexTask task) {
        task.setStatus("running");
        task.setTaskLog(appendLog(task.getTaskLog(), "开始执行"));
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    private void failTask(KnowledgeIndexTask task, Exception e, LocalDateTime startedAt) {
        int retry = task.getRetryCount() == null ? 0 : task.getRetryCount();
        retry++;
        task.setRetryCount(retry);
        task.setLastError(e.getMessage());
        task.setStatus(retry >= properties.getIndexing().getMaxRetry() ? "failed" : "pending");
        task.setNextRetryAt(LocalDateTime.now().plusMinutes(Math.min(30, retry * 5L)));
        task.setTaskLog(appendLog(task.getTaskLog(), "执行失败：" + e.getMessage()));
        task.setDurationMs(Duration.between(startedAt, LocalDateTime.now()).toMillis());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        log.warn("知识库索引任务失败，taskId: {}, retry: {}, error: {}", task.getId(), retry, e.getMessage());
    }

    private String appendLog(String oldLog, String message) {
        String line = LocalDateTime.now() + " " + message;
        if (oldLog == null || oldLog.isBlank()) {
            return line;
        }
        String next = oldLog + "\n" + line;
        return next.length() > 4000 ? next.substring(next.length() - 4000) : next;
    }
}
