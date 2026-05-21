package com.ruc.platform.admin.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruc.platform.admin.knowledge.dto.KnowledgeArticleSaveDTO;
import com.ruc.platform.admin.knowledge.dto.KnowledgeCategorySaveDTO;
import com.ruc.platform.admin.knowledge.dto.KnowledgeTemplateSaveDTO;
import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.knowledgeness.dto.KnowledgeArticleQueryDTO;
import com.ruc.platform.knowledgeness.dto.KnowledgeTemplateQueryDTO;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.knowledgeness.entity.KnowledgeCategory;
import com.ruc.platform.knowledgeness.entity.KnowledgeTemplate;
import com.ruc.platform.knowledgeness.mapper.KnowledgeArticleMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeBehaviorEventMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeCategoryMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeRecommendationLogMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeTemplateMapper;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleDetailVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleListItemVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeTemplateVO;
import com.ruc.platform.knowledgeness.service.KnowledgeContentRenderer;
import com.ruc.platform.knowledgeness.service.KnowledgeFileTextExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminKnowledgeServiceImpl implements AdminKnowledgeService {

    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeTemplateMapper templateMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final KnowledgeBehaviorEventMapper behaviorEventMapper;
    private final KnowledgeRecommendationLogMapper recommendationLogMapper;
    private final KnowledgeContentRenderer contentRenderer;
    private final KnowledgeFileTextExtractor fileTextExtractor;

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
        refreshExtractedText(article);
        article.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        article.setViewCount(0L);
        article.setCreatedBy(operatorId);
        article.setUpdatedBy(operatorId);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        if (Integer.valueOf(1).equals(article.getStatus())) {
            article.setPublishTime(LocalDateTime.now());
        }
        articleMapper.insert(article);
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
        copyArticle(dto, article);
        refreshExtractedText(article);
        article.setUpdatedBy(operatorId);
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);
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

    private void refreshExtractedText(KnowledgeArticle article) {
        if (article == null) {
            return;
        }
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
        article.setExtractError(extracted == null || extracted.isBlank() ? "未从文件中抽取到可检索文本" : null);
        article.setExtractedAt(LocalDateTime.now());
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
