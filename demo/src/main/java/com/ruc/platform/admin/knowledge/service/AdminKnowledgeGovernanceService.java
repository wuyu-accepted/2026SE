package com.ruc.platform.admin.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticleVersion;
import com.ruc.platform.knowledgeness.mapper.KnowledgeArticleMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeArticleVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminKnowledgeGovernanceService {

    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeArticleVersionMapper versionMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public void enrichGovernanceFields(KnowledgeArticle article) {
        if (article == null) {
            return;
        }
        article.setDuplicateSignature(signature(article));
        article.setQualityScore(java.math.BigDecimal.valueOf(qualityScore(article)));
        if (article.getEffectiveTo() != null) {
            article.setExpireRemindAt(article.getEffectiveTo().minusDays(30));
        }
        validateReferences(article);
        validateScope(article);
    }

    public List<KnowledgeArticleVersion> listVersions(Long articleId) {
        LambdaQueryWrapper<KnowledgeArticleVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeArticleVersion::getArticleId, articleId)
                .orderByDesc(KnowledgeArticleVersion::getCreatedAt);
        return versionMapper.selectList(wrapper);
    }

    public List<KnowledgeArticle> findDuplicates(Long articleId) {
        KnowledgeArticle article = articleMapper.selectById(articleId);
        if (article == null || article.getDuplicateSignature() == null || article.getDuplicateSignature().isBlank()) {
            return List.of();
        }
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeArticle::getDuplicateSignature, article.getDuplicateSignature())
                .ne(KnowledgeArticle::getId, articleId);
        return articleMapper.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void snapshotArticle(KnowledgeArticle article, Long operatorId) {
        if (article == null || article.getId() == null || versionMapper == null) {
            return;
        }
        KnowledgeArticleVersion version = new KnowledgeArticleVersion();
        version.setArticleId(article.getId());
        version.setVersionNo(article.getVersionNo() == null ? 1 : article.getVersionNo());
        version.setSnapshotJson(toSnapshot(article));
        version.setCreatedBy(operatorId);
        version.setCreatedAt(LocalDateTime.now());
        versionMapper.insert(version);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollbackVersion(Long versionId, Long operatorId) {
        KnowledgeArticleVersion version = versionMapper.selectById(versionId);
        if (version == null) {
            throw new BizException(ResultCode.NOT_FOUND, "版本不存在");
        }
        KnowledgeArticle article = articleMapper.selectById(version.getArticleId());
        if (article == null) {
            throw new BizException(ResultCode.NOT_FOUND, "知识条目不存在");
        }
        Map<String, Object> snapshot = parseSnapshot(version.getSnapshotJson());
        article.setTitle((String) snapshot.getOrDefault("title", article.getTitle()));
        article.setSummary((String) snapshot.getOrDefault("summary", article.getSummary()));
        article.setContent((String) snapshot.getOrDefault("content", article.getContent()));
        article.setAnswer((String) snapshot.getOrDefault("answer", article.getAnswer()));
        article.setSource((String) snapshot.getOrDefault("source", article.getSource()));
        article.setTags((String) snapshot.getOrDefault("tags", article.getTags()));
        article.setSourceContent((String) snapshot.getOrDefault("sourceContent", article.getSourceContent()));
        article.setExtractedText((String) snapshot.getOrDefault("extractedText", article.getExtractedText()));
        article.setVersionNo((article.getVersionNo() == null ? version.getVersionNo() : article.getVersionNo()) + 1);
        article.setReviewStatus("pending");
        article.setUpdatedBy(operatorId);
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitReview(Long articleId, Long operatorId) {
        updateReviewStatus(articleId, operatorId, "pending", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveReview(Long articleId, Long operatorId) {
        updateReviewStatus(articleId, operatorId, "approved", 1);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectReview(Long articleId, Long operatorId) {
        updateReviewStatus(articleId, operatorId, "rejected", 0);
    }

    @Scheduled(cron = "${knowledge.governance.expire-cron:0 0 2 * * *}")
    @Transactional(rollbackFor = Exception.class)
    public int takeDownExpiredArticles() {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeArticle::getStatus, 1)
                .isNotNull(KnowledgeArticle::getEffectiveTo)
                .lt(KnowledgeArticle::getEffectiveTo, LocalDateTime.now());
        List<KnowledgeArticle> expired = articleMapper.selectList(wrapper);
        for (KnowledgeArticle article : expired) {
            article.setStatus(2);
            article.setUpdatedAt(LocalDateTime.now());
            articleMapper.updateById(article);
        }
        if (!expired.isEmpty()) {
            log.info("知识库过期自动下架 {} 条", expired.size());
        }
        return expired.size();
    }

    private String signature(KnowledgeArticle article) {
        String text = String.join("|",
                nullToEmpty(article.getTitle()),
                nullToEmpty(article.getSummary()),
                nullToEmpty(article.getSource()),
                nullToEmpty(article.getExtractedText()).substring(0, Math.min(200, nullToEmpty(article.getExtractedText()).length())));
        return org.springframework.util.DigestUtils.md5DigestAsHex(text.toLowerCase().replaceAll("\s+", "").getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private int qualityScore(KnowledgeArticle article) {
        int score = 30;
        if (hasText(article.getTitle())) score += 10;
        if (hasText(article.getSummary())) score += 10;
        if (hasText(article.getTags())) score += 10;
        if (hasText(article.getApplicableScope()) || hasText(article.getTargetGrades()) || hasText(article.getTargetMajors())) score += 10;
        if (hasText(article.getExtractedText()) || hasText(article.getSourceContent())) score += 20;
        if (article.getEffectiveTo() != null) score += 10;
        return Math.min(100, score);
    }

    private void validateReferences(KnowledgeArticle article) {
        if (!hasText(article.getReferenceArticleIds())) {
            return;
        }
        for (String idText : article.getReferenceArticleIds().split("[,，]")) {
            if (!hasText(idText)) {
                continue;
            }
            try {
                Long id = Long.valueOf(idText.trim());
                if (articleMapper.selectById(id) == null) {
                    throw new BizException(ResultCode.PARAM_ERROR, "引用知识不存在: " + id);
                }
            } catch (NumberFormatException e) {
                throw new BizException(ResultCode.PARAM_ERROR, "引用知识 ID 格式错误: " + idText);
            }
        }
    }

    private void validateScope(KnowledgeArticle article) {
        if (!hasText(article.getApplicableScope()) && !hasText(article.getTargetGrades()) && !hasText(article.getTargetMajors()) && !hasText(article.getTargetPoliticalStatuses()) && !hasText(article.getTargetPartyStages())) {
            article.setApplicableScope("全体学生");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void updateReviewStatus(Long articleId, Long operatorId, String reviewStatus, Integer status) {
        KnowledgeArticle article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BizException(ResultCode.NOT_FOUND, "知识条目不存在");
        }
        article.setReviewStatus(reviewStatus);
        if (status != null) {
            article.setStatus(status);
            if (status == 1 && article.getPublishTime() == null) {
                article.setPublishTime(LocalDateTime.now());
            }
        }
        article.setUpdatedBy(operatorId);
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);
    }

    private String toSnapshot(KnowledgeArticle article) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("title", article.getTitle());
            snapshot.put("summary", article.getSummary());
            snapshot.put("content", article.getContent());
            snapshot.put("answer", article.getAnswer());
            snapshot.put("source", article.getSource());
            snapshot.put("tags", article.getTags());
            snapshot.put("sourceContent", article.getSourceContent());
            snapshot.put("extractedText", article.getExtractedText());
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> parseSnapshot(String json) {
        try {
            return objectMapper.readValue(json == null ? "{}" : json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
