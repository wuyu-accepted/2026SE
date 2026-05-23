package com.ruc.platform.admin.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruc.platform.admin.knowledge.dto.KnowledgeArticleSaveDTO;
import com.ruc.platform.admin.knowledge.dto.KnowledgeCategorySaveDTO;
import com.ruc.platform.admin.knowledge.dto.KnowledgeTemplateSaveDTO;
import com.ruc.platform.admin.knowledge.dto.KnowledgeRecommendWeightConfigDTO;
import com.ruc.platform.admin.knowledge.dto.KnowledgeSynonymSaveDTO;
import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.knowledgeness.dto.KnowledgeArticleQueryDTO;
import com.ruc.platform.knowledgeness.dto.KnowledgeTemplateQueryDTO;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticleVersion;
import com.ruc.platform.knowledgeness.entity.KnowledgeCategory;
import com.ruc.platform.knowledgeness.entity.KnowledgeIndexTask;
import com.ruc.platform.knowledgeness.entity.KnowledgeRecommendWeightConfig;
import com.ruc.platform.knowledgeness.entity.KnowledgeSynonymGroup;
import com.ruc.platform.knowledgeness.entity.KnowledgeTemplate;
import com.ruc.platform.knowledgeness.mapper.KnowledgeArticleMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeBehaviorEventMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeCategoryMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeRecommendationLogMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeRecommendWeightConfigMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeSynonymGroupMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeTemplateMapper;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleDetailVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleListItemVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeTemplateVO;
import com.ruc.platform.knowledgeness.service.KnowledgeContentRenderer;
import com.ruc.platform.knowledgeness.service.KnowledgeIndexingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminKnowledgeServiceImpl implements AdminKnowledgeService {

    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeTemplateMapper templateMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final KnowledgeBehaviorEventMapper behaviorEventMapper;
    private final KnowledgeRecommendationLogMapper recommendationLogMapper;
    private final KnowledgeSynonymGroupMapper synonymGroupMapper;
    private final KnowledgeRecommendWeightConfigMapper recommendWeightConfigMapper;
    private final KnowledgeContentRenderer contentRenderer;
    private final KnowledgeIndexingService indexingService;
    private final AdminKnowledgeGovernanceService governanceService;

    @Autowired
    public AdminKnowledgeServiceImpl(KnowledgeArticleMapper articleMapper,
                                     KnowledgeTemplateMapper templateMapper,
                                     KnowledgeCategoryMapper categoryMapper,
                                     KnowledgeBehaviorEventMapper behaviorEventMapper,
                                     KnowledgeRecommendationLogMapper recommendationLogMapper,
                                     KnowledgeSynonymGroupMapper synonymGroupMapper,
                                     KnowledgeRecommendWeightConfigMapper recommendWeightConfigMapper,
                                     KnowledgeContentRenderer contentRenderer,
                                     KnowledgeIndexingService indexingService,
                                     AdminKnowledgeGovernanceService governanceService) {
        this.articleMapper = articleMapper;
        this.templateMapper = templateMapper;
        this.categoryMapper = categoryMapper;
        this.behaviorEventMapper = behaviorEventMapper;
        this.recommendationLogMapper = recommendationLogMapper;
        this.synonymGroupMapper = synonymGroupMapper;
        this.recommendWeightConfigMapper = recommendWeightConfigMapper;
        this.contentRenderer = contentRenderer;
        this.indexingService = indexingService;
        this.governanceService = governanceService;
    }

    public AdminKnowledgeServiceImpl(KnowledgeArticleMapper articleMapper,
                                     KnowledgeTemplateMapper templateMapper,
                                     KnowledgeCategoryMapper categoryMapper,
                                     KnowledgeBehaviorEventMapper behaviorEventMapper,
                                     KnowledgeRecommendationLogMapper recommendationLogMapper,
                                     KnowledgeContentRenderer contentRenderer,
                                     KnowledgeIndexingService indexingService) {
        this.articleMapper = articleMapper;
        this.templateMapper = templateMapper;
        this.categoryMapper = categoryMapper;
        this.behaviorEventMapper = behaviorEventMapper;
        this.recommendationLogMapper = recommendationLogMapper;
        this.synonymGroupMapper = null;
        this.recommendWeightConfigMapper = null;
        this.contentRenderer = contentRenderer;
        this.indexingService = indexingService;
        this.governanceService = null;
    }

    @Override
    public PageResult<KnowledgeArticleListItemVO> listArticles(KnowledgeArticleQueryDTO queryDTO) {
        Page<KnowledgeArticle> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(queryDTO.getStatus() != null, KnowledgeArticle::getStatus, queryDTO.getStatus())
                .eq(queryDTO.getCategoryId() != null, KnowledgeArticle::getCategoryId, queryDTO.getCategoryId())
                .eq(hasText(queryDTO.getContentType()), KnowledgeArticle::getContentType, queryDTO.getContentType())
                .like(hasText(queryDTO.getTag()), KnowledgeArticle::getTags, queryDTO.getTag())
                .and(hasText(queryDTO.getKeyword()), w -> w
                        .like(KnowledgeArticle::getTitle, queryDTO.getKeyword())
                        .or()
                        .like(KnowledgeArticle::getSummary, queryDTO.getKeyword())
                        .or()
                        .like(KnowledgeArticle::getContent, queryDTO.getKeyword())
                        .or()
                        .like(KnowledgeArticle::getAnswer, queryDTO.getKeyword())
                        .or()
                        .like(KnowledgeArticle::getExtractedText, queryDTO.getKeyword()))
                .orderByDesc(KnowledgeArticle::getUpdatedAt);
        Page<KnowledgeArticle> result = articleMapper.selectPage(page, wrapper);
        List<KnowledgeArticleListItemVO> records = result.getRecords().stream().map(this::toArticleListItem).collect(Collectors.toList());
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public KnowledgeArticleDetailVO getArticle(Long id) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new BizException(ResultCode.NOT_FOUND, "知识条目不存在");
        }
        KnowledgeArticleDetailVO vo = new KnowledgeArticleDetailVO();
        BeanUtils.copyProperties(article, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createArticle(Long operatorId, KnowledgeArticleSaveDTO dto) {
        String mode = dto.getContentMode() == null || dto.getContentMode().isBlank() ? "file" : dto.getContentMode();
        if ("file".equals(mode) && dto.getFileId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "请先上传知识资料文件");
        }
        if ("editor".equals(mode) && (dto.getSourceContent() == null || dto.getSourceContent().isBlank())) {
            throw new BizException(ResultCode.PARAM_ERROR, "请填写在线编排内容");
        }
        KnowledgeArticle article = new KnowledgeArticle();
        copyArticle(dto, article);
        article.setVersionNo(1);
        if (governanceService != null) {
            governanceService.enrichGovernanceFields(article);
        }
        markIndexPending(article);
        article.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        article.setViewCount(0L);
        article.setCreatedBy(operatorId);
        article.setUpdatedBy(operatorId);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        if (Integer.valueOf(1).equals(article.getStatus())) {
            article.setPublishTime(LocalDateTime.now());
        }
        articleMapper.insert(article);
        indexingService.enqueueArticle(article.getId(), "save");
        return article.getId();
    }

    @Override
    public KnowledgeArticleDetailVO previewArticle(KnowledgeArticleDetailVO draft) {
        KnowledgeArticleDetailVO vo = new KnowledgeArticleDetailVO();
        BeanUtils.copyProperties(draft, vo);
        vo.setRenderedContent(contentRenderer.render(vo.getEditorType(), vo.getSourceContent()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(Long operatorId, Long id, KnowledgeArticleSaveDTO dto) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new BizException(ResultCode.NOT_FOUND, "知识条目不存在");
        }
        if (governanceService != null) {
            governanceService.snapshotArticle(article, operatorId);
        }
        copyArticle(dto, article);
        article.setVersionNo((article.getVersionNo() == null ? 1 : article.getVersionNo()) + 1);
        if (governanceService != null) {
            governanceService.enrichGovernanceFields(article);
        }
        markIndexPending(article);
        article.setUpdatedBy(operatorId);
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);
        indexingService.enqueueArticle(article.getId(), "save");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticleStatus(Long operatorId, Long id, Integer status) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new BizException(ResultCode.NOT_FOUND, "知识条目不存在");
        }
        article.setStatus(status);
        article.setUpdatedBy(operatorId);
        article.setUpdatedAt(LocalDateTime.now());
        if (Integer.valueOf(1).equals(status) && article.getPublishTime() == null) {
            article.setPublishTime(LocalDateTime.now());
        }
        articleMapper.updateById(article);
    }

    @Override
    public void deleteArticle(Long id) {
        articleMapper.deleteById(id);
    }

    @Override
    public List<KnowledgeTemplateVO> listTemplates(KnowledgeTemplateQueryDTO queryDTO) {
        LambdaQueryWrapper<KnowledgeTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(queryDTO.getStatus() != null, KnowledgeTemplate::getStatus, queryDTO.getStatus())
                .eq(hasText(queryDTO.getCategory()), KnowledgeTemplate::getCategory, queryDTO.getCategory())
                .like(hasText(queryDTO.getKeyword()), KnowledgeTemplate::getName, queryDTO.getKeyword())
                .orderByDesc(KnowledgeTemplate::getUpdatedAt);
        return templateMapper.selectList(wrapper).stream().map(this::toTemplateVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(Long operatorId, KnowledgeTemplateSaveDTO dto) {
        KnowledgeTemplate template = new KnowledgeTemplate();
        copyTemplate(dto, template);
        template.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        template.setDownloadCount(0L);
        template.setCreatedBy(operatorId);
        template.setUpdatedBy(operatorId);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.insert(template);
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(Long operatorId, Long id, KnowledgeTemplateSaveDTO dto) {
        KnowledgeTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BizException(ResultCode.NOT_FOUND, "模板不存在");
        }
        copyTemplate(dto, template);
        template.setUpdatedBy(operatorId);
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplateStatus(Long operatorId, Long id, Integer status) {
        KnowledgeTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BizException(ResultCode.NOT_FOUND, "模板不存在");
        }
        template.setStatus(status);
        template.setUpdatedBy(operatorId);
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    @Override
    public void deleteTemplate(Long id) {
        templateMapper.deleteById(id);
    }

    @Override
    public List<KnowledgeCategory> listCategories() {
        LambdaQueryWrapper<KnowledgeCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(KnowledgeCategory::getSortOrder).orderByAsc(KnowledgeCategory::getId);
        return categoryMapper.selectList(wrapper);
    }

    @Override
    public Long createCategory(KnowledgeCategorySaveDTO dto) {
        KnowledgeCategory category = new KnowledgeCategory();
        category.setName(dto.getName());
        category.setCode(dto.getCode());
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        category.setCreatedAt(LocalDateTime.now());
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    public void updateCategory(Long id, KnowledgeCategorySaveDTO dto) {
        KnowledgeCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ResultCode.NOT_FOUND, "分类不存在");
        }
        category.setName(dto.getName());
        category.setCode(dto.getCode());
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        categoryMapper.updateById(category);
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("articleCount", articleMapper.selectCount(null));
        stats.put("templateCount", templateMapper.selectCount(null));
        stats.put("categoryCount", categoryMapper.selectCount(null));
        stats.put("behaviorEventCount", behaviorEventMapper.selectCount(null));
        stats.put("recommendationLogCount", recommendationLogMapper.selectCount(null));
        return stats;
    }

    @Override
    public int rebuildKnowledgeIndex() {
        return indexingService.rebuildAll();
    }

    @Override
    public List<KnowledgeIndexTask> listIndexTasks(Long articleId, String status, Integer limit) {
        return indexingService.listTasks(articleId, status, limit == null ? 50 : limit);
    }

    @Override
    public void retryIndexTask(Long taskId) {
        indexingService.retryTask(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void correctOcrText(Long operatorId, Long articleId, String correctedText) {
        KnowledgeArticle article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BizException(ResultCode.NOT_FOUND, "知识条目不存在");
        }
        article.setOcrCorrectedText(correctedText);
        article.setExtractedText(correctedText);
        article.setOcrStatus("corrected");
        article.setExtractStatus("success");
        article.setExtractError(null);
        article.setOcrError(null);
        article.setOcrCorrectedBy(operatorId);
        article.setOcrCorrectedAt(LocalDateTime.now());
        article.setUpdatedBy(operatorId);
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);
        indexingService.enqueueArticle(articleId, "ocr-correct");
    }

    @Override
    public List<KnowledgeSynonymGroup> listSynonyms() {
        LambdaQueryWrapper<KnowledgeSynonymGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(KnowledgeSynonymGroup::getUpdatedAt);
        return synonymGroupMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveSynonym(Long operatorId, KnowledgeSynonymSaveDTO dto) {
        KnowledgeSynonymGroup group = new KnowledgeSynonymGroup();
        group.setGroupName(dto.getGroupName());
        group.setTerms(dto.getTerms());
        group.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        group.setCreatedBy(operatorId);
        group.setUpdatedBy(operatorId);
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        synonymGroupMapper.insert(group);
        return group.getId();
    }

    @Override
    public List<KnowledgeRecommendWeightConfig> listRecommendWeights() {
        LambdaQueryWrapper<KnowledgeRecommendWeightConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(KnowledgeRecommendWeightConfig::getUpdatedAt);
        return recommendWeightConfigMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveRecommendWeight(Long operatorId, KnowledgeRecommendWeightConfigDTO dto) {
        KnowledgeRecommendWeightConfig config = new KnowledgeRecommendWeightConfig();
        config.setConfigName(hasText(dto.getConfigName()) ? dto.getConfigName() : "默认推荐权重");
        config.setAbGroup(hasText(dto.getAbGroup()) ? dto.getAbGroup() : "default");
        config.setProfileWeight(defaultDecimal(dto.getProfileWeight()));
        config.setScenarioWeight(defaultDecimal(dto.getScenarioWeight()));
        config.setBehaviorWeight(defaultDecimal(dto.getBehaviorWeight()));
        config.setFavoriteWeight(defaultDecimal(dto.getFavoriteWeight()));
        config.setDownloadWeight(defaultDecimal(dto.getDownloadWeight()));
        config.setSuccessWeight(defaultDecimal(dto.getSuccessWeight()));
        config.setSimilarStudentWeight(defaultDecimal(dto.getSimilarStudentWeight()));
        config.setTimeDecayWeight(defaultDecimal(dto.getTimeDecayWeight()));
        config.setEnabled(dto.getEnabled() == null || dto.getEnabled());
        config.setCreatedBy(operatorId);
        config.setUpdatedBy(operatorId);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        recommendWeightConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public Map<String, Object> governanceStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("pendingReviewCount", articleMapper.selectCount(new LambdaQueryWrapper<KnowledgeArticle>().eq(KnowledgeArticle::getReviewStatus, "pending")));
        stats.put("expiringSoonCount", articleMapper.selectCount(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, 1)
                .le(KnowledgeArticle::getEffectiveTo, LocalDateTime.now().plusDays(30))
                .ge(KnowledgeArticle::getEffectiveTo, LocalDateTime.now())));
        stats.put("expiredPublishedCount", articleMapper.selectCount(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, 1)
                .lt(KnowledgeArticle::getEffectiveTo, LocalDateTime.now())));
        stats.put("lowQualityCount", articleMapper.selectCount(new LambdaQueryWrapper<KnowledgeArticle>().lt(KnowledgeArticle::getQualityScore, BigDecimal.valueOf(60))));
        stats.put("synonymGroupCount", synonymGroupMapper.selectCount(null));
        stats.put("recommendWeightConfigCount", recommendWeightConfigMapper.selectCount(null));
        return stats;
    }

    @Override
    public Map<String, Object> searchAnalytics() {
        Map<String, Object> result = new LinkedHashMap<>();
        LambdaQueryWrapper<com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent> searchWrapper = new LambdaQueryWrapper<>();
        searchWrapper.eq(com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent::getEventType, "search")
                .orderByDesc(com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent::getCreatedAt)
                .last("LIMIT 200");
        List<com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent> events = behaviorEventMapper.selectList(searchWrapper);
        Map<String, Long> keywordCounts = events.stream()
                .filter(event -> hasText(event.getKeyword()))
                .collect(Collectors.groupingBy(com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent::getKeyword, LinkedHashMap::new, Collectors.counting()));
        result.put("recentSearchCount", events.size());
        result.put("topKeywords", keywordCounts);
        result.put("clickCount", behaviorEventMapper.selectCount(new LambdaQueryWrapper<com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent>().eq(com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent::getEventType, "click")));
        result.put("favoriteCount", behaviorEventMapper.selectCount(new LambdaQueryWrapper<com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent>().eq(com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent::getEventType, "favorite")));
        result.put("successCount", behaviorEventMapper.selectCount(new LambdaQueryWrapper<com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent>().eq(com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent::getEventType, "process_success")));
        return result;
    }

    @Override
    public void submitReview(Long operatorId, Long articleId) {
        if (governanceService != null) {
            governanceService.submitReview(articleId, operatorId);
        }
    }

    @Override
    public void approveReview(Long operatorId, Long articleId) {
        if (governanceService != null) {
            governanceService.approveReview(articleId, operatorId);
        }
    }

    @Override
    public void rejectReview(Long operatorId, Long articleId) {
        if (governanceService != null) {
            governanceService.rejectReview(articleId, operatorId);
        }
    }

    @Override
    public void rollbackVersion(Long operatorId, Long versionId) {
        if (governanceService != null) {
            governanceService.rollbackVersion(versionId, operatorId);
        }
    }

    @Override
    public List<KnowledgeArticleVersion> listVersions(Long articleId) {
        return governanceService == null ? List.of() : governanceService.listVersions(articleId);
    }

    @Override
    public List<KnowledgeArticle> findDuplicates(Long articleId) {
        return governanceService == null ? List.of() : governanceService.findDuplicates(articleId);
    }

    @Override
    public int takeDownExpiredArticles() {
        return governanceService == null ? 0 : governanceService.takeDownExpiredArticles();
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
    }

    private void markIndexPending(KnowledgeArticle article) {
        article.setExtractStatus("pending");
        article.setExtractError(null);
        article.setExtractedAt(null);
    }

    private void copyArticle(KnowledgeArticleSaveDTO dto, KnowledgeArticle article) {
        BeanUtils.copyProperties(dto, article);
        if (!hasText(article.getContent())) {
            article.setContent(hasText(dto.getSummary()) ? dto.getSummary() : dto.getTitle());
        }
        if (!hasText(article.getContentMode())) {
            article.setContentMode("file");
        }
        if ("editor".equals(article.getContentMode()) && !hasText(article.getEditorType())) {
            article.setEditorType("markdown");
        }
        if (!hasText(article.getContentType())) {
            article.setContentType("guide");
        }
        if (article.getPriority() == null) {
            article.setPriority(0);
        }
    }

    private void copyTemplate(KnowledgeTemplateSaveDTO dto, KnowledgeTemplate template) {
        BeanUtils.copyProperties(dto, template);
        if (template.getPriority() == null) {
            template.setPriority(0);
        }
    }

    private KnowledgeArticleListItemVO toArticleListItem(KnowledgeArticle article) {
        KnowledgeArticleListItemVO vo = new KnowledgeArticleListItemVO();
        BeanUtils.copyProperties(article, vo);
        return vo;
    }

    private KnowledgeTemplateVO toTemplateVO(KnowledgeTemplate template) {
        KnowledgeTemplateVO vo = new KnowledgeTemplateVO();
        BeanUtils.copyProperties(template, vo);
        return vo;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
