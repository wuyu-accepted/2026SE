package com.ruc.platform.ai.service;

import com.ruc.platform.PlatformApplication;
import com.ruc.platform.ai.vo.AiCitationVO;
import com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.knowledgeness.mapper.KnowledgeArticleMapper;
import com.ruc.platform.knowledgeness.service.KnowledgeLocalSearchService;
import com.ruc.platform.notice.entity.Notice;
import com.ruc.platform.notice.entity.UserMessage;
import com.ruc.platform.notice.mapper.NoticeMapper;
import com.ruc.platform.notice.mapper.UserMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = PlatformApplication.class)
@ActiveProfiles("h2")
class AiContextServiceTest {

    @TempDir
    Path tempDir;

    @Autowired
    private AiContextService aiContextService;

    @Autowired
    private KnowledgeLocalSearchService localSearchService;

    @Autowired
    private KnowledgeIntelligenceProperties properties;

    @Autowired
    private KnowledgeArticleMapper articleMapper;

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private UserMessageMapper userMessageMapper;

    @BeforeEach
    void setUp() {
        properties.getSearch().setIndexPath(tempDir.resolve("unified-search").toString());
        userMessageMapper.deleteById(96101L);
        userMessageMapper.deleteById(96102L);
        userMessageMapper.deleteById(96103L);
        for (long id = 96200L; id < 96208L; id++) {
            userMessageMapper.deleteById(id);
        }
        noticeMapper.deleteById(95101L);
        noticeMapper.deleteById(95102L);
        noticeMapper.deleteById(95103L);
        for (long id = 95200L; id < 95208L; id++) {
            noticeMapper.deleteById(id);
        }
        articleMapper.deleteById(94101L);

        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(94101L);
        article.setTitle("奖学金申请指南");
        article.setSummary("说明奖学金评定材料");
        article.setContent("奖学金申请需要提交成绩单和综合测评证明。");
        article.setStatus(1);
        article.setPublishTime(LocalDateTime.now().minusDays(1));
        articleMapper.insert(article);
        localSearchService.indexArticle(article);

        insertNotice(95101L, "综合测评通知", "综合测评申诉入口开放到本周五。");
        insertNotice(95102L, "其他班级通知", "仅其他班级可见的奖学金答辩安排。");
        insertMessage(96101L, 1001L, 95101L);
        insertMessage(96102L, 1002L, 95102L);
        localSearchService.indexNotice(noticeMapper.selectById(95101L));
        localSearchService.indexNotice(noticeMapper.selectById(95102L));
    }

    @Test
    void searchVisibleContentReturnsKnowledgeAndOnlyUserVisibleNotices() {
        List<AiCitationVO> citations = aiContextService.searchVisibleContent(1001L, "奖学金 综合测评 申诉入口", 10);

        assertThat(citations).extracting(AiCitationVO::getType).contains("knowledge", "notice");
        assertThat(citations).anySatisfy(citation -> {
            assertThat(citation.getType()).isEqualTo("notice");
            assertThat(citation.getId()).isEqualTo(95101L);
            assertThat(citation.getPath()).isEqualTo("/pages/notice-detail/notice-detail?id=95101");
        });
        assertThat(citations)
                .filteredOn(citation -> "notice".equals(citation.getType()))
                .extracting(AiCitationVO::getId)
                .doesNotContain(95102L);
    }

    @Test
    void searchVisibleContentStillFindsVisibleNoticeWhenInvisibleHitsRankHigher() {
        for (long id = 95200L; id < 95208L; id++) {
            insertNotice(id, "奖学金答辩安排", "奖学金奖学金奖学金奖学金奖学金答辩安排，仅其他用户可见。");
            insertMessage(96200L + (id - 95200L), 1002L, id);
            localSearchService.indexNotice(noticeMapper.selectById(id));
        }
        insertNotice(95103L, "奖学金答辩安排", "奖学金奖学金奖学金答辩安排，当前用户可见。");
        insertMessage(96103L, 1001L, 95103L);
        localSearchService.indexNotice(noticeMapper.selectById(95103L));

        List<AiCitationVO> citations = aiContextService.searchVisibleContent(1001L, "奖学金答辩安排", 1);

        assertThat(citations)
                .filteredOn(citation -> "notice".equals(citation.getType()))
                .extracting(AiCitationVO::getId)
                .contains(95103L);
    }

    @Test
    void searchVisibleContentFallsBackToVisibleNoticeDatabaseSearchWhenLuceneCandidatesAreInvisible() {
        KnowledgeLocalSearchService localSearch = mock(KnowledgeLocalSearchService.class);
        when(localSearch.search("奖学金答辩安排", 4)).thenReturn(List.of(
                new KnowledgeLocalSearchService.SearchHit("notice", 95102L, 99D, "奖学金答辩安排", "Lucene", null)
        ));
        AiContextService service = new AiContextService(null, localSearch, articleMapper, noticeMapper, userMessageMapper);

        insertNotice(95103L, "奖学金答辩安排", "奖学金答辩安排，当前用户可见。");
        insertMessage(96103L, 1001L, 95103L);

        List<AiCitationVO> citations = service.searchVisibleContent(1001L, "奖学金答辩安排", 1);

        assertThat(citations)
                .filteredOn(citation -> "notice".equals(citation.getType()))
                .extracting(AiCitationVO::getId)
                .contains(95103L);
    }

    private void insertNotice(Long id, String title, String content) {
        Notice notice = new Notice();
        notice.setId(id);
        notice.setTitle(title);
        notice.setSummary("用于 AI 检索测试");
        notice.setContent(content);
        notice.setNoticeType("教学");
        notice.setTag("检索测试");
        notice.setStatus(1);
        notice.setPriority(1);
        notice.setPublishTime(LocalDateTime.now());
        notice.setCreatedAt(LocalDateTime.now());
        notice.setUpdatedAt(LocalDateTime.now());
        noticeMapper.insert(notice);
    }

    private void insertMessage(Long id, Long userId, Long noticeId) {
        UserMessage message = new UserMessage();
        message.setId(id);
        message.setUserId(userId);
        message.setNoticeId(noticeId);
        message.setTitle("投递通知");
        message.setSummary("用于 AI 检索测试");
        message.setReadStatus(0);
        message.setCreatedAt(LocalDateTime.now());
        userMessageMapper.insert(message);
    }
}
