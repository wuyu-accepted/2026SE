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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiContextService {
    private static final Pattern WECHAT_ARTICLE_URL = Pattern.compile("https?://mp\\.weixin\\.qq\\.com/[^\\s，。；,;]+");

    private final KnowledgeService knowledgeService;
    private final KnowledgeLocalSearchService localSearchService;
    private final KnowledgeArticleMapper articleMapper;
    private final NoticeMapper noticeMapper;
    private final UserMessageMapper userMessageMapper;
    private final WechatArticleFetchService wechatArticleFetchService;

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
        Map<Long, String> wechatArticleExcerptCache = new LinkedHashMap<>();
        int rank = 0;
        for (KnowledgeLocalSearchService.SearchHit hit : localSearchService.search(question, safeLimit * 4)) {
            if ("knowledge".equals(hit.getSourceType())) {
                AiCitationVO citation = knowledgeCitation(hit);
                if (citation != null) {
                    merged.putIfAbsent("knowledge:" + citation.getId(), new ScoredCitation(citation, hit.getScore(), rank++));
                }
            } else if ("notice".equals(hit.getSourceType())) {
                AiCitationVO citation = visibleNoticeCitation(userId, hit, wechatArticleExcerptCache);
                if (citation != null) {
                    merged.putIfAbsent("notice:" + citation.getId(), new ScoredCitation(citation, hit.getScore(), rank++));
                }
            }
        }
        rank = supplementVisibleNoticeMatches(userId, question, safeLimit, merged, rank, wechatArticleExcerptCache);
        rank = supplementVisibleWechatArticleMatches(userId, question, safeLimit, merged, rank, wechatArticleExcerptCache);
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

    private int supplementVisibleNoticeMatches(Long userId, String question, int safeLimit, Map<String, ScoredCitation> merged, int rank,
                                               Map<Long, String> wechatArticleExcerptCache) {
        if (userMessageMapper == null || merged.size() >= safeLimit) {
            return rank;
        }
        int remaining = Math.max(1, safeLimit - merged.size());
        for (MessageDetailVO message : userMessageMapper.searchByKeyword(userId, question, remaining * 3)) {
            if (message == null || message.getNoticeId() == null) {
                continue;
            }
            String key = "notice:" + message.getNoticeId();
            if (merged.containsKey(key)) {
                continue;
            }
            AiCitationVO citation = visibleNoticeCitation(message, wechatArticleExcerptCache);
            if (citation != null) {
                merged.put(key, new ScoredCitation(citation, noticeRelevanceScore(message, question), rank++));
            }
            if (merged.size() >= safeLimit) {
                break;
            }
        }
        return rank;
    }

    private int supplementVisibleWechatArticleMatches(Long userId, String question, int safeLimit, Map<String, ScoredCitation> merged, int rank,
                                                      Map<Long, String> wechatArticleExcerptCache) {
        if (userMessageMapper == null || wechatArticleFetchService == null || merged.size() >= safeLimit) {
            return rank;
        }
        for (MessageDetailVO message : userMessageMapper.selectVisibleWechatArticleMessages(userId, Math.max(5, safeLimit * 3))) {
            if (message == null || message.getNoticeId() == null) {
                continue;
            }
            String key = "notice:" + message.getNoticeId();
            if (merged.containsKey(key)) {
                continue;
            }
            Notice notice = noticeMapper.selectById(message.getNoticeId());
            if (notice == null || !Integer.valueOf(1).equals(notice.getStatus())) {
                continue;
            }
            String articleText = wechatArticleExcerpt(notice, wechatArticleExcerptCache);
            if (articleText.isBlank() || noticeRelevanceScore(message, question) + fieldScore(articleText, normalize(question), 2D) <= 0D) {
                continue;
            }
            AiCitationVO citation = visibleNoticeCitation(message, wechatArticleExcerptCache);
            if (citation != null) {
                citation.setExcerpt(firstNonBlank(articleText, citation.getExcerpt()));
                merged.put(key, new ScoredCitation(citation, fieldScore(articleText, normalize(question), 2D), rank++));
            }
            if (merged.size() >= safeLimit) {
                break;
            }
        }
        return rank;
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

    private AiCitationVO visibleNoticeCitation(Long userId, KnowledgeLocalSearchService.SearchHit hit, Map<Long, String> wechatArticleExcerptCache) {
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
        String excerpt = firstNonBlank(hit.getHighlight());
        if (excerpt.isBlank()) {
            excerpt = wechatArticleExcerpt(notice, wechatArticleExcerptCache);
        }
        vo.setExcerpt(firstNonBlank(excerpt, notice.getContent(), notice.getSummary()));
        vo.setPath("/pages/notice-detail/notice-detail?id=" + notice.getId());
        return vo;
    }

    private AiCitationVO visibleNoticeCitation(MessageDetailVO message, Map<Long, String> wechatArticleExcerptCache) {
        Notice notice = noticeMapper.selectById(message.getNoticeId());
        if (notice == null || !Integer.valueOf(1).equals(notice.getStatus())) {
            return null;
        }
        AiCitationVO vo = new AiCitationVO();
        vo.setType("notice");
        vo.setId(notice.getId());
        vo.setTitle(firstNonBlank(notice.getTitle(), message.getTitle()));
        vo.setSummary(firstNonBlank(notice.getSummary(), message.getSummary()));
        String articleExcerpt = wechatArticleExcerpt(notice, wechatArticleExcerptCache);
        vo.setExcerpt(firstNonBlank(articleExcerpt, message.getContent(), notice.getContent(), notice.getSummary(), message.getSummary()));
        vo.setPath("/pages/notice-detail/notice-detail?id=" + notice.getId());
        return vo;
    }

    private String wechatArticleExcerpt(Notice notice, Map<Long, String> cache) {
        if (wechatArticleFetchService == null || notice == null) {
            return "";
        }
        if (cache != null && notice.getId() != null && cache.containsKey(notice.getId())) {
            return cache.get(notice.getId());
        }
        String url = firstWechatArticleUrl(notice.getContent());
        if (url.isBlank()) {
            if (cache != null && notice.getId() != null) {
                cache.put(notice.getId(), "");
            }
            return "";
        }
        try {
            WechatArticleFetchService.WechatArticleContent article = wechatArticleFetchService.fetch(url);
            if (article == null) {
                if (cache != null && notice.getId() != null) {
                    cache.put(notice.getId(), "");
                }
                return "";
            }
            String excerpt = firstNonBlank(article.content(), article.title());
            if (cache != null && notice.getId() != null) {
                cache.put(notice.getId(), excerpt);
            }
            return excerpt;
        } catch (Exception ignored) {
            if (cache != null && notice.getId() != null) {
                cache.put(notice.getId(), "");
            }
            return "";
        }
    }

    private String firstWechatArticleUrl(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        Matcher matcher = WECHAT_ARTICLE_URL.matcher(content);
        return matcher.find() ? matcher.group() : "";
    }

    private double noticeRelevanceScore(MessageDetailVO message, String question) {
        String normalizedQuestion = normalize(question);
        if (normalizedQuestion.isBlank()) {
            return 0D;
        }
        double score = 0D;
        score += fieldScore(message.getTitle(), normalizedQuestion, 6D);
        score += fieldScore(message.getSummary(), normalizedQuestion, 3D);
        score += fieldScore(message.getContent(), normalizedQuestion, 1D);
        return score;
    }

    private double fieldScore(String value, String normalizedQuestion, double weight) {
        String normalizedValue = normalize(value);
        if (normalizedValue.isBlank()) {
            return 0D;
        }
        if (normalizedValue.contains(normalizedQuestion)) {
            return weight * 3;
        }
        double score = 0D;
        for (String term : normalizedQuestion.split("[\\s,，;；]+")) {
            if (!term.isBlank() && normalizedValue.contains(term)) {
                score += weight;
            }
        }
        return score;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase().trim();
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
