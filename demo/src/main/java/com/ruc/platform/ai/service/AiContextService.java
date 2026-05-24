package com.ruc.platform.ai.service;

import com.ruc.platform.ai.vo.AiCitationVO;
import com.ruc.platform.knowledgeness.dto.KnowledgeArticleQueryDTO;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.knowledgeness.mapper.KnowledgeArticleMapper;
import com.ruc.platform.knowledgeness.service.KnowledgeService;
import com.ruc.platform.knowledgeness.service.KnowledgeLocalSearchService;
import com.ruc.platform.notice.entity.Notice;
import com.ruc.platform.notice.mapper.NoticeMapper;
import com.ruc.platform.notice.mapper.UserMessageMapper;
import com.ruc.platform.notice.vo.MessageDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiContextService {
    private final KnowledgeService knowledgeService;
    private final KnowledgeLocalSearchService localSearchService;
    private final KnowledgeArticleMapper articleMapper;
    private final NoticeMapper noticeMapper;
    private final UserMessageMapper userMessageMapper;

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
            vo.setExcerpt(firstNonBlank(item.getSearchHighlight(), item.getSummary()));
            vo.setPath("/pages/knowledge-detail/knowledge-detail?id=" + item.getId());
            return vo;
        }).toList();
    }

    public List<AiCitationVO> searchVisibleContent(Long userId, String question, Integer limit) {
        if (userId == null || question == null || question.isBlank()) {
            return List.of();
        }
        int safeLimit = limit == null || limit <= 0 ? 5 : Math.min(limit, 20);
        Map<String, ScoredCitation> merged = new LinkedHashMap<>();
        int rank = 0;
        for (KnowledgeLocalSearchService.SearchHit hit : localSearchService.search(question, safeLimit * 4)) {
            if ("knowledge".equals(hit.getSourceType())) {
                AiCitationVO citation = knowledgeCitation(hit);
                if (citation != null) {
                    merged.putIfAbsent("knowledge:" + citation.getId(), new ScoredCitation(citation, hit.getScore(), rank++));
                }
            } else if ("notice".equals(hit.getSourceType())) {
                AiCitationVO citation = visibleNoticeCitation(userId, hit);
                if (citation != null) {
                    merged.putIfAbsent("notice:" + citation.getId(), new ScoredCitation(citation, hit.getScore(), rank++));
                }
            }
        }
        if (merged.isEmpty()) {
            for (AiCitationVO citation : searchKnowledge(question, safeLimit)) {
                merged.putIfAbsent(citation.getType() + ":" + citation.getId(), new ScoredCitation(citation, 0D, rank++));
            }
        }
        return merged.values().stream()
                .sorted(Comparator.comparing(ScoredCitation::score, Comparator.reverseOrder())
                        .thenComparing(ScoredCitation::rank))
                .limit(safeLimit)
                .map(ScoredCitation::citation)
                .toList();
    }

    private AiCitationVO knowledgeCitation(KnowledgeLocalSearchService.SearchHit hit) {
        KnowledgeArticle article = articleMapper.selectById(hit.getSourceId());
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            return null;
        }
        AiCitationVO vo = new AiCitationVO();
        vo.setType("knowledge");
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setExcerpt(firstNonBlank(hit.getHighlight(), article.getSummary(), article.getContent(), article.getAnswer()));
        vo.setPath("/pages/knowledge-detail/knowledge-detail?id=" + article.getId());
        return vo;
    }

    private AiCitationVO visibleNoticeCitation(Long userId, KnowledgeLocalSearchService.SearchHit hit) {
        MessageDetailVO message = userMessageMapper.selectDetailByNoticeIdAndUserId(hit.getSourceId(), userId);
        if (message == null) {
            return null;
        }
        Notice notice = noticeMapper.selectById(hit.getSourceId());
        if (notice == null || !Integer.valueOf(1).equals(notice.getStatus())) {
            return null;
        }
        AiCitationVO vo = new AiCitationVO();
        vo.setType("notice");
        vo.setId(notice.getId());
        vo.setTitle(notice.getTitle());
        vo.setSummary(firstNonBlank(notice.getSummary(), message.getSummary()));
        vo.setExcerpt(firstNonBlank(hit.getHighlight(), notice.getContent(), notice.getSummary()));
        vo.setPath("/pages/notice-detail/notice-detail?id=" + notice.getId());
        return vo;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private record ScoredCitation(AiCitationVO citation, Double score, int rank) {
    }
}
