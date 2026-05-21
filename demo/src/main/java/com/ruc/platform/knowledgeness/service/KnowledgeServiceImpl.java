package com.ruc.platform.knowledgeness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.knowledgeness.dto.KnowledgeArticleQueryDTO;
import com.ruc.platform.knowledgeness.dto.KnowledgeBehaviorDTO;
import com.ruc.platform.knowledgeness.dto.KnowledgeTemplateQueryDTO;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent;
import com.ruc.platform.knowledgeness.entity.KnowledgeCategory;
import com.ruc.platform.knowledgeness.entity.KnowledgeRecommendationLog;
import com.ruc.platform.knowledgeness.entity.KnowledgeTemplate;
import com.ruc.platform.knowledgeness.mapper.KnowledgeArticleMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeBehaviorEventMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeCategoryMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeRecommendationLogMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeTemplateMapper;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleDetailVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleListItemVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeRecommendationVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeTemplateVO;
import com.ruc.platform.party.entity.PartyReminder;
import com.ruc.platform.party.entity.PartyStudentProgress;
import com.ruc.platform.party.mapper.PartyReminderMapper;
import com.ruc.platform.party.mapper.PartyStudentProgressMapper;
import com.ruc.platform.student.entity.StudentProfile;
import com.ruc.platform.student.mapper.StudentProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final String STRATEGY_VERSION = "rule-v1";

    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeTemplateMapper templateMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final PartyStudentProgressMapper partyProgressMapper;
    private final KnowledgeBehaviorEventMapper behaviorEventMapper;
    private final KnowledgeRecommendationLogMapper recommendationLogMapper;
    private final PartyReminderMapper partyReminderMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final KnowledgeContentRenderer contentRenderer;
    private final KnowledgeLocalSearchService localSearchService;
    private final KnowledgeSemanticSearchService semanticSearchService;

    @Override
    public PageResult<KnowledgeArticleListItemVO> listArticles(KnowledgeArticleQueryDTO queryDTO) {
        Page<KnowledgeArticle> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<KnowledgeArticle> wrapper = buildArticleQueryWrapper(queryDTO);
        Page<KnowledgeArticle> articlePage = articleMapper.selectPage(page, wrapper);
        List<KnowledgeArticle> records = articlePage.getRecords();
        if (hasText(queryDTO.getKeyword())) {
            records = rankByLocalSearch(queryDTO.getKeyword(), records);
        }
        List<KnowledgeArticleListItemVO> list = records.stream()
                .map(this::convertToListItemVO)
                .collect(Collectors.toList());

        return PageResult.of(articlePage.getTotal(), articlePage.getCurrent(), articlePage.getSize(), list);
    }

    private LambdaQueryWrapper<KnowledgeArticle> buildArticleQueryWrapper(KnowledgeArticleQueryDTO queryDTO) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(queryDTO.getStatus() != null, KnowledgeArticle::getStatus, queryDTO.getStatus())
                .eq(queryDTO.getCategoryId() != null, KnowledgeArticle::getCategoryId, queryDTO.getCategoryId())
                .eq(hasText(queryDTO.getContentType()), KnowledgeArticle::getContentType, queryDTO.getContentType())
                .like(hasText(queryDTO.getTag()), KnowledgeArticle::getTags, queryDTO.getTag())
                .like(hasText(queryDTO.getScenarioCode()), KnowledgeArticle::getScenarioCodes, queryDTO.getScenarioCode());
        if (hasText(queryDTO.getKeyword())) {
            Set<String> keywords = semanticSearchService.expandKeywords(queryDTO.getKeyword());
            wrapper.and(w -> {
                boolean first = true;
                for (String keyword : keywords) {
                    if (!first) {
                        w.or();
                    }
                    w.like(KnowledgeArticle::getTitle, keyword)
                            .or()
                            .like(KnowledgeArticle::getSummary, keyword)
                            .or()
                            .like(KnowledgeArticle::getContent, keyword)
                            .or()
                            .like(KnowledgeArticle::getAnswer, keyword)
                            .or()
                            .like(KnowledgeArticle::getTags, keyword)
                            .or()
                            .like(KnowledgeArticle::getExtractedText, keyword);
                    first = false;
                }
            });
        }
        wrapper.and(w -> w.isNull(KnowledgeArticle::getEffectiveFrom).or().le(KnowledgeArticle::getEffectiveFrom, LocalDateTime.now()))
                .and(w -> w.isNull(KnowledgeArticle::getEffectiveTo).or().ge(KnowledgeArticle::getEffectiveTo, LocalDateTime.now()))
                .orderByDesc(KnowledgeArticle::getPriority)
                .orderByDesc(KnowledgeArticle::getPublishTime);
        return wrapper;
    }

    private List<KnowledgeArticle> rankByLocalSearch(String keyword, List<KnowledgeArticle> records) {
        List<Long> rankedIds = localSearchService.searchArticleIds(keyword, 100);
        if (rankedIds.isEmpty() || records.isEmpty()) {
            return records;
        }
        Map<Long, Integer> rankMap = new LinkedHashMap<>();
        for (int i = 0; i < rankedIds.size(); i++) {
            rankMap.put(rankedIds.get(i), i);
        }
        return records.stream()
                .sorted(Comparator.comparing(article -> rankMap.getOrDefault(article.getId(), Integer.MAX_VALUE)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeArticleDetailVO getArticleDetail(Long id) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null || Integer.valueOf(2).equals(article.getStatus())) {
            throw new BizException(ResultCode.NOT_FOUND, "知识条目不存在");
        }
        article.setViewCount(safeLong(article.getViewCount()) + 1);
        articleMapper.updateById(article);
        KnowledgeArticleDetailVO detail = convertToDetailVO(article);
        detail.setViewCount(article.getViewCount());
        return detail;
    }

    @Override
    public KnowledgeArticleDetailVO previewArticle(KnowledgeArticleDetailVO draft) {
        if (draft == null) {
            return null;
        }
        draft.setRenderedContent(contentRenderer.render(draft.getEditorType(), draft.getSourceContent()));
        return draft;
    }

    @Override
    public List<KnowledgeTemplateVO> listTemplates() {
        KnowledgeTemplateQueryDTO queryDTO = new KnowledgeTemplateQueryDTO();
        queryDTO.setStatus(1);
        return listTemplates(queryDTO);
    }

    @Override
    public List<KnowledgeTemplateVO> listTemplates(KnowledgeTemplateQueryDTO queryDTO) {
        LambdaQueryWrapper<KnowledgeTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(queryDTO.getStatus() != null, KnowledgeTemplate::getStatus, queryDTO.getStatus())
                .eq(hasText(queryDTO.getCategory()), KnowledgeTemplate::getCategory, queryDTO.getCategory())
                .like(hasText(queryDTO.getTag()), KnowledgeTemplate::getTags, queryDTO.getTag())
                .like(hasText(queryDTO.getScenarioCode()), KnowledgeTemplate::getScenarioCodes, queryDTO.getScenarioCode())
                .and(hasText(queryDTO.getKeyword()), w -> w
                        .like(KnowledgeTemplate::getName, queryDTO.getKeyword())
                        .or()
                        .like(KnowledgeTemplate::getDescription, queryDTO.getKeyword())
                        .or()
                        .like(KnowledgeTemplate::getTags, queryDTO.getKeyword()))
                .and(w -> w.isNull(KnowledgeTemplate::getEffectiveFrom).or().le(KnowledgeTemplate::getEffectiveFrom, LocalDateTime.now()))
                .and(w -> w.isNull(KnowledgeTemplate::getEffectiveTo).or().ge(KnowledgeTemplate::getEffectiveTo, LocalDateTime.now()))
                .orderByDesc(KnowledgeTemplate::getPriority)
                .orderByDesc(KnowledgeTemplate::getCreatedAt);
        return templateMapper.selectList(wrapper).stream()
                .map(this::convertToTemplateVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<KnowledgeRecommendationVO> listRecommendations(Long userId, Integer limit) {
        int size = limit == null || limit <= 0 ? 6 : Math.min(limit, 20);
        StudentProfile profile = studentProfileMapper.selectByUserId(userId);
        PartyStudentProgress progress = partyProgressMapper.selectByUserId(userId);
        List<PartyReminder> reminders = partyReminderMapper == null ? List.of() : partyReminderMapper.selectPendingByUserId(userId);
        Set<String> scenarioHints = collectScenarioHints(reminders);
        Set<String> recentKeywords = collectRecentSearchKeywords(userId);

        List<KnowledgeRecommendationVO> results = new ArrayList<>();
        for (KnowledgeArticle article : publishedArticles()) {
            ScoredReason scored = scoreArticle(article, profile, progress, scenarioHints, recentKeywords);
            if (scored.score > 0 || safeLong(article.getViewCount()) > 0) {
                KnowledgeRecommendationVO vo = toArticleRecommendation(article, scored);
                results.add(vo);
            }
        }
        for (KnowledgeTemplate template : enabledTemplates()) {
            ScoredReason scored = scoreTemplate(template, profile, progress, scenarioHints);
            if (scored.score > 0 || safeLong(template.getDownloadCount()) > 0) {
                KnowledgeRecommendationVO vo = toTemplateRecommendation(template, scored);
                results.add(vo);
            }
        }

        List<KnowledgeRecommendationVO> limited = results.stream()
                .sorted(Comparator.comparing(KnowledgeRecommendationVO::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(KnowledgeRecommendationVO::getPublishTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(size)
                .collect(Collectors.toList());
        limited.forEach(item -> saveRecommendationLog(userId, item));
        return limited;
    }

    @Override
    public void recordBehavior(Long userId, KnowledgeBehaviorDTO dto) {
        if (dto == null || !hasText(dto.getEventType())) {
            return;
        }
        KnowledgeBehaviorEvent event = new KnowledgeBehaviorEvent();
        event.setUserId(userId);
        event.setEventType(dto.getEventType());
        event.setTargetType(dto.getTargetType());
        event.setTargetId(dto.getTargetId());
        event.setKeyword(dto.getKeyword());
        event.setSourcePage(dto.getSourcePage());
        event.setFeatureSnapshot("{\"strategyVersion\":\"" + STRATEGY_VERSION + "\"}");
        event.setCreatedAt(LocalDateTime.now());
        behaviorEventMapper.insert(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordTemplateDownload(Long userId, Long fileId, String sourcePage) {
        if (fileId == null) {
            return;
        }
        LambdaQueryWrapper<KnowledgeTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeTemplate::getFileId, fileId).last("LIMIT 1");
        KnowledgeTemplate template = templateMapper.selectOne(wrapper);
        KnowledgeBehaviorDTO dto = new KnowledgeBehaviorDTO();
        dto.setEventType("download_template");
        dto.setSourcePage(sourcePage);
        if (template != null) {
            template.setDownloadCount(safeLong(template.getDownloadCount()) + 1);
            template.setUpdatedAt(LocalDateTime.now());
            templateMapper.updateById(template);
            dto.setTargetType("template");
            dto.setTargetId(template.getId());
            recordBehavior(userId, dto);
            return;
        }
        LambdaQueryWrapper<KnowledgeArticle> articleWrapper = new LambdaQueryWrapper<>();
        articleWrapper.eq(KnowledgeArticle::getFileId, fileId).last("LIMIT 1");
        KnowledgeArticle article = articleMapper.selectOne(articleWrapper);
        if (article == null) {
            return;
        }
        dto.setEventType("download_article_file");
        dto.setTargetType("article");
        dto.setTargetId(article.getId());
        recordBehavior(userId, dto);
    }

    private List<KnowledgeArticle> publishedArticles() {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeArticle::getStatus, 1)
                .and(w -> w.isNull(KnowledgeArticle::getEffectiveFrom).or().le(KnowledgeArticle::getEffectiveFrom, LocalDateTime.now()))
                .and(w -> w.isNull(KnowledgeArticle::getEffectiveTo).or().ge(KnowledgeArticle::getEffectiveTo, LocalDateTime.now()));
        return articleMapper.selectList(wrapper);
    }

    private List<KnowledgeTemplate> enabledTemplates() {
        LambdaQueryWrapper<KnowledgeTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeTemplate::getStatus, 1)
                .and(w -> w.isNull(KnowledgeTemplate::getEffectiveFrom).or().le(KnowledgeTemplate::getEffectiveFrom, LocalDateTime.now()))
                .and(w -> w.isNull(KnowledgeTemplate::getEffectiveTo).or().ge(KnowledgeTemplate::getEffectiveTo, LocalDateTime.now()));
        return templateMapper.selectList(wrapper);
    }

    private ScoredReason scoreArticle(KnowledgeArticle article, StudentProfile profile, PartyStudentProgress progress, Set<String> scenarioHints, Set<String> recentKeywords) {
        ScoredReason scored = new ScoredReason();
        addProfileScore(scored, profile, progress, article.getTargetGrades(), article.getTargetMajors(), article.getTargetPoliticalStatuses(), article.getTargetPartyStages());
        addScenarioScore(scored, article.getScenarioCodes(), scenarioHints);
        addRecentKeywordScore(scored, article, recentKeywords);
        addContentScore(scored, article.getPriority(), safeLong(article.getViewCount()), "热门文章");
        return scored.ensureFallback("热门文章");
    }

    private ScoredReason scoreTemplate(KnowledgeTemplate template, StudentProfile profile, PartyStudentProgress progress, Set<String> scenarioHints) {
        ScoredReason scored = new ScoredReason();
        addProfileScore(scored, profile, progress, template.getTargetGrades(), template.getTargetMajors(), template.getTargetPoliticalStatuses(), template.getTargetPartyStages());
        addScenarioScore(scored, template.getScenarioCodes(), scenarioHints);
        addContentScore(scored, template.getPriority(), safeLong(template.getDownloadCount()), "热门模板");
        return scored.ensureFallback("热门模板");
    }

    private void addProfileScore(ScoredReason scored, StudentProfile profile, PartyStudentProgress progress, String grades, String majors, String politicalStatuses, String partyStages) {
        if (profile != null) {
            if (matches(grades, profile.getGrade())) {
                scored.add(20, "适用于 " + profile.getGrade() + " 级学生");
            }
            if (matches(majors, profile.getMajor())) {
                scored.add(15, "与你的专业匹配");
            }
            if (matches(politicalStatuses, profile.getPoliticalStatus())) {
                scored.add(15, "与你的政治面貌匹配");
            }
        }
        if (progress != null && matches(partyStages, progress.getCurrentStageCode())) {
            scored.add(35, "与你当前党团阶段匹配");
        }
    }

    private void addScenarioScore(ScoredReason scored, String scenarioCodes, Set<String> scenarioHints) {
        if (!hasText(scenarioCodes) || scenarioHints.isEmpty()) {
            return;
        }
        for (String hint : scenarioHints) {
            if (matches(scenarioCodes, hint)) {
                scored.add(25, "与你近期待办场景相关");
                return;
            }
        }
    }

    private void addRecentKeywordScore(ScoredReason scored, KnowledgeArticle article, Set<String> recentKeywords) {
        if (recentKeywords.isEmpty()) {
            return;
        }
        String searchable = normalizeSearchableText(String.join(" ",
                nullToEmpty(article.getTitle()),
                nullToEmpty(article.getSummary()),
                nullToEmpty(article.getContent()),
                nullToEmpty(article.getAnswer()),
                nullToEmpty(article.getTags()),
                nullToEmpty(article.getExtractedText())));
        for (String keyword : recentKeywords) {
            if (hasText(keyword) && searchable.contains(normalizeSearchableText(keyword))) {
                scored.add(30, "与你最近搜索相关");
                return;
            }
        }
    }

    private void addContentScore(ScoredReason scored, Integer priority, long hotCount, String hotReason) {
        if (priority != null && priority > 0) {
            scored.add(priority * 5, "优先推荐内容");
        }
        if (hotCount > 0) {
            scored.add((int) Math.min(20, hotCount / 25 + 1), hotReason);
        }
    }

    private KnowledgeRecommendationVO toArticleRecommendation(KnowledgeArticle article, ScoredReason scored) {
        KnowledgeRecommendationVO vo = new KnowledgeRecommendationVO();
        vo.setTargetType("article");
        vo.setTargetId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContentType(article.getContentType());
        vo.setTags(article.getTags());
        vo.setFileId(article.getFileId());
        vo.setScore(scored.score);
        vo.setRecommendReason(scored.reasonText());
        vo.setPublishTime(article.getPublishTime());
        return vo;
    }

    private KnowledgeRecommendationVO toTemplateRecommendation(KnowledgeTemplate template, ScoredReason scored) {
        KnowledgeRecommendationVO vo = new KnowledgeRecommendationVO();
        vo.setTargetType("template");
        vo.setTargetId(template.getId());
        vo.setTitle(template.getName());
        vo.setSummary(template.getDescription());
        vo.setCategoryName(template.getCategory());
        vo.setTags(template.getTags());
        vo.setFileId(template.getFileId());
        vo.setFormat(template.getFormat());
        vo.setScore(scored.score);
        vo.setRecommendReason(scored.reasonText());
        vo.setPublishTime(template.getCreatedAt());
        return vo;
    }

    private void saveRecommendationLog(Long userId, KnowledgeRecommendationVO item) {
        KnowledgeRecommendationLog log = new KnowledgeRecommendationLog();
        log.setUserId(userId);
        log.setTargetType(item.getTargetType());
        log.setTargetId(item.getTargetId());
        log.setScore(item.getScore());
        log.setReason(item.getRecommendReason());
        log.setStrategyVersion(STRATEGY_VERSION);
        log.setFeatureSnapshot("{\"strategyVersion\":\"" + STRATEGY_VERSION + "\"}");
        log.setCreatedAt(LocalDateTime.now());
        recommendationLogMapper.insert(log);
    }

    private Set<String> collectRecentSearchKeywords(Long userId) {
        Set<String> keywords = new LinkedHashSet<>();
        if (userId == null || behaviorEventMapper == null) {
            return keywords;
        }
        LambdaQueryWrapper<KnowledgeBehaviorEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBehaviorEvent::getUserId, userId)
                .eq(KnowledgeBehaviorEvent::getEventType, "search")
                .isNotNull(KnowledgeBehaviorEvent::getKeyword)
                .orderByDesc(KnowledgeBehaviorEvent::getCreatedAt)
                .last("LIMIT 10");
        for (KnowledgeBehaviorEvent event : behaviorEventMapper.selectList(wrapper)) {
            if (hasText(event.getKeyword())) {
                keywords.add(event.getKeyword().trim());
            }
        }
        return keywords;
    }

    private Set<String> collectScenarioHints(List<PartyReminder> reminders) {
        Set<String> hints = new LinkedHashSet<>();
        if (reminders == null) {
            return hints;
        }
        for (PartyReminder reminder : reminders) {
            if (hasText(reminder.getReminderType())) {
                hints.add(reminder.getReminderType());
            }
        }
        return hints;
    }

    private KnowledgeArticleListItemVO convertToListItemVO(KnowledgeArticle article) {
        KnowledgeArticleListItemVO vo = new KnowledgeArticleListItemVO();
        BeanUtils.copyProperties(article, vo);
        vo.setCategoryName(resolveCategoryName(article.getCategoryId()));
        return vo;
    }

    private KnowledgeArticleDetailVO convertToDetailVO(KnowledgeArticle article) {
        KnowledgeArticleDetailVO vo = new KnowledgeArticleDetailVO();
        BeanUtils.copyProperties(article, vo);
        vo.setCategoryName(resolveCategoryName(article.getCategoryId()));
        vo.setKeywords(splitValues(article.getTags()));
        vo.setRenderedContent(contentRenderer.render(article.getEditorType(), article.getSourceContent()));
        return vo;
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null || categoryMapper == null) {
            return "";
        }
        KnowledgeCategory category = categoryMapper.selectById(categoryId);
        return category == null ? "" : category.getName();
    }

    private KnowledgeTemplateVO convertToTemplateVO(KnowledgeTemplate template) {
        KnowledgeTemplateVO vo = new KnowledgeTemplateVO();
        BeanUtils.copyProperties(template, vo);
        return vo;
    }

    private boolean matches(String csv, String value) {
        if (!hasText(csv) || !hasText(value)) {
            return false;
        }
        for (String item : csv.split("[,，]")) {
            if (value.trim().equals(item.trim())) {
                return true;
            }
        }
        return false;
    }

    private List<String> splitValues(String csv) {
        if (!hasText(csv)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String item : csv.split("[,，]")) {
            if (hasText(item)) {
                values.add(item.trim());
            }
        }
        return values;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String normalizeSearchableText(String value) {
        return nullToEmpty(value).toLowerCase().replaceAll("\\s+", "");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static class ScoredReason {
        private int score;
        private final List<String> reasons = new ArrayList<>();

        private void add(int delta, String reason) {
            score += delta;
            if (reason != null && !reason.isBlank() && !reasons.contains(reason)) {
                reasons.add(reason);
            }
        }

        private ScoredReason ensureFallback(String fallback) {
            if (reasons.isEmpty()) {
                reasons.add(fallback);
            }
            return this;
        }

        private String reasonText() {
            return String.join("；", reasons);
        }
    }
}
