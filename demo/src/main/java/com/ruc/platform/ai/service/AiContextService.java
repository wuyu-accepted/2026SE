package com.ruc.platform.ai.service;

import com.ruc.platform.ai.vo.AiCitationVO;
import com.ruc.platform.knowledgeness.dto.KnowledgeArticleQueryDTO;
import com.ruc.platform.knowledgeness.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiContextService {
    private final KnowledgeService knowledgeService;

    public List<AiCitationVO> searchKnowledge(String question, Integer limit) {
        if (knowledgeService == null || question == null || question.isBlank()) {
            return List.of();
        }
        KnowledgeArticleQueryDTO query = new KnowledgeArticleQueryDTO();
        query.setKeyword(question);
        query.setStatus(1);
        query.setPageNum(1);
        query.setPageSize(limit == null || limit <= 0 ? 5 : limit);
        return knowledgeService.listArticles(query).getRecords().stream().map(item -> {
            AiCitationVO vo = new AiCitationVO();
            vo.setType("knowledge");
            vo.setId(item.getId());
            vo.setTitle(item.getTitle());
            vo.setSummary(item.getSummary());
            vo.setPath("/pages/knowledge-detail/knowledge-detail?id=" + item.getId());
            return vo;
        }).toList();
    }
}
