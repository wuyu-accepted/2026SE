package com.ruc.platform.admin.knowledge.service;

import com.ruc.platform.admin.knowledge.dto.KnowledgeArticleSaveDTO;
import com.ruc.platform.admin.knowledge.dto.KnowledgeCategorySaveDTO;
import com.ruc.platform.admin.knowledge.dto.KnowledgeTemplateSaveDTO;
import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.knowledgeness.dto.KnowledgeArticleQueryDTO;
import com.ruc.platform.knowledgeness.dto.KnowledgeTemplateQueryDTO;
import com.ruc.platform.knowledgeness.entity.KnowledgeCategory;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleDetailVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleListItemVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeTemplateVO;

import java.util.List;
import java.util.Map;

public interface AdminKnowledgeService {

    PageResult<KnowledgeArticleListItemVO> listArticles(KnowledgeArticleQueryDTO queryDTO);

    KnowledgeArticleDetailVO getArticle(Long id);

    Long createArticle(Long operatorId, KnowledgeArticleSaveDTO dto);

    KnowledgeArticleDetailVO previewArticle(KnowledgeArticleDetailVO draft);

    void updateArticle(Long operatorId, Long id, KnowledgeArticleSaveDTO dto);

    void updateArticleStatus(Long operatorId, Long id, Integer status);

    void deleteArticle(Long id);

    List<KnowledgeTemplateVO> listTemplates(KnowledgeTemplateQueryDTO queryDTO);

    Long createTemplate(Long operatorId, KnowledgeTemplateSaveDTO dto);

    void updateTemplate(Long operatorId, Long id, KnowledgeTemplateSaveDTO dto);

    void updateTemplateStatus(Long operatorId, Long id, Integer status);

    void deleteTemplate(Long id);

    List<KnowledgeCategory> listCategories();

    Long createCategory(KnowledgeCategorySaveDTO dto);

    void updateCategory(Long id, KnowledgeCategorySaveDTO dto);

    Map<String, Object> stats();
}
