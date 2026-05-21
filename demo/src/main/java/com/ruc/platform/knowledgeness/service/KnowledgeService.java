package com.ruc.platform.knowledgeness.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.knowledgeness.dto.KnowledgeArticleQueryDTO;
import com.ruc.platform.knowledgeness.dto.KnowledgeBehaviorDTO;
import com.ruc.platform.knowledgeness.dto.KnowledgeTemplateQueryDTO;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleDetailVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleListItemVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeRecommendationVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeTemplateVO;

import java.util.List;

/**
 * 知识服务接口
 */
public interface KnowledgeService {

    /**
     * 分页查询知识条目列表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<KnowledgeArticleListItemVO> listArticles(KnowledgeArticleQueryDTO queryDTO);

    /**
     * 获取知识条目详情
     * @param id 条目ID
     * @return 条目详情
     */
    KnowledgeArticleDetailVO getArticleDetail(Long id);

    /**
     * 获取启用的模板列表
     * @return 模板列表
     */
    List<KnowledgeTemplateVO> listTemplates();

    List<KnowledgeTemplateVO> listTemplates(KnowledgeTemplateQueryDTO queryDTO);

    List<KnowledgeRecommendationVO> listRecommendations(Long userId, Integer limit);

    void recordBehavior(Long userId, KnowledgeBehaviorDTO dto);

    void recordTemplateDownload(Long userId, Long fileId, String sourcePage);

    KnowledgeArticleDetailVO previewArticle(KnowledgeArticleDetailVO draft);

    List<String> suggestKeywords(String keyword, Integer limit);
}
