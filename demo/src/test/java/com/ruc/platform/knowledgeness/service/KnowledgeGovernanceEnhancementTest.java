package com.ruc.platform.knowledgeness.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ruc.platform.knowledgeness.dto.KnowledgeBehaviorDTO;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent;
import com.ruc.platform.knowledgeness.mapper.KnowledgeArticleMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeBehaviorEventMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeCategoryMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeFavoriteMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeRecommendationLogMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeTemplateMapper;
import com.ruc.platform.knowledgeness.vo.KnowledgeRecommendationVO;
import com.ruc.platform.party.mapper.PartyReminderMapper;
import com.ruc.platform.party.mapper.PartyStudentProgressMapper;
import com.ruc.platform.student.mapper.StudentProfileMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeGovernanceEnhancementTest {

    @Test
    void favoriteAndSuccessFeedbackShouldBoostRecommendationReason() {
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        KnowledgeBehaviorEventMapper behaviorMapper = mock(KnowledgeBehaviorEventMapper.class);
        KnowledgeServiceImpl service = new KnowledgeServiceImpl(
                articleMapper,
                mock(KnowledgeTemplateMapper.class),
                mock(StudentProfileMapper.class),
                mock(PartyStudentProgressMapper.class),
                behaviorMapper,
                mock(KnowledgeRecommendationLogMapper.class),
                mock(PartyReminderMapper.class),
                mock(KnowledgeCategoryMapper.class),
                new KnowledgeContentRenderer(),
                mock(KnowledgeLocalSearchService.class),
                mock(KnowledgeSemanticSearchService.class),
                mock(KnowledgeFavoriteMapper.class)
        );
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(701L);
        article.setTitle("困难补助办理指南");
        article.setStatus(1);
        article.setViewCount(0L);
        when(articleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(article));
        when(behaviorMapper.selectList(any(Wrapper.class))).thenReturn(List.of(event("favorite", 701L), event("process_success", 701L)));

        List<KnowledgeRecommendationVO> recommendations = service.listRecommendations(1001L, 5);

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).getRecommendReason()).contains("收藏").contains("办理成功");
    }

    @Test
    void recordFavoriteShouldPersistFavoriteAndBehavior() {
        KnowledgeBehaviorEventMapper behaviorMapper = mock(KnowledgeBehaviorEventMapper.class);
        KnowledgeFavoriteMapper favoriteMapper = mock(KnowledgeFavoriteMapper.class);
        KnowledgeServiceImpl service = new KnowledgeServiceImpl(
                mock(KnowledgeArticleMapper.class),
                mock(KnowledgeTemplateMapper.class),
                mock(StudentProfileMapper.class),
                mock(PartyStudentProgressMapper.class),
                behaviorMapper,
                mock(KnowledgeRecommendationLogMapper.class),
                mock(PartyReminderMapper.class),
                mock(KnowledgeCategoryMapper.class),
                new KnowledgeContentRenderer(),
                mock(KnowledgeLocalSearchService.class),
                mock(KnowledgeSemanticSearchService.class),
                favoriteMapper
        );
        KnowledgeBehaviorDTO dto = new KnowledgeBehaviorDTO();
        dto.setEventType("favorite");
        dto.setTargetType("article");
        dto.setTargetId(702L);

        service.recordBehavior(1001L, dto);

        verify(favoriteMapper).insert(any());
        verify(behaviorMapper).insert(any());
    }

    private KnowledgeBehaviorEvent event(String type, Long targetId) {
        KnowledgeBehaviorEvent event = new KnowledgeBehaviorEvent();
        event.setEventType(type);
        event.setTargetType("article");
        event.setTargetId(targetId);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }
}
