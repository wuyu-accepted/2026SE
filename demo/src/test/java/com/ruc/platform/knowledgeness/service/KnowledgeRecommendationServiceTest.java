package com.ruc.platform.knowledgeness.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.knowledgeness.mapper.KnowledgeArticleMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeBehaviorEventMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeCategoryMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeRecommendationLogMapper;
import com.ruc.platform.knowledgeness.mapper.KnowledgeTemplateMapper;
import com.ruc.platform.knowledgeness.dto.KnowledgeArticleQueryDTO;
import com.ruc.platform.knowledgeness.entity.KnowledgeCategory;
import com.ruc.platform.knowledgeness.entity.KnowledgeTemplate;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleListItemVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeRecommendationVO;
import com.ruc.platform.party.entity.PartyStudentProgress;
import com.ruc.platform.party.mapper.PartyReminderMapper;
import com.ruc.platform.party.mapper.PartyStudentProgressMapper;
import com.ruc.platform.student.entity.StudentProfile;
import com.ruc.platform.student.mapper.StudentProfileMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeRecommendationServiceTest {

    @Test
    void shouldRecommendArticleForMatchingPartyStageAndGrade() {
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        KnowledgeTemplateMapper templateMapper = mock(KnowledgeTemplateMapper.class);
        StudentProfileMapper studentProfileMapper = mock(StudentProfileMapper.class);
        PartyStudentProgressMapper partyProgressMapper = mock(PartyStudentProgressMapper.class);
        KnowledgeBehaviorEventMapper behaviorEventMapper = mock(KnowledgeBehaviorEventMapper.class);
        KnowledgeRecommendationLogMapper recommendationLogMapper = mock(KnowledgeRecommendationLogMapper.class);
        PartyReminderMapper partyReminderMapper = mock(PartyReminderMapper.class);
        KnowledgeCategoryMapper categoryMapper = mock(KnowledgeCategoryMapper.class);
        KnowledgeServiceImpl service = new KnowledgeServiceImpl(
                articleMapper,
                templateMapper,
                studentProfileMapper,
                partyProgressMapper,
                behaviorEventMapper,
                recommendationLogMapper,
                partyReminderMapper,
                categoryMapper,
                new KnowledgeContentRenderer()
        );
        StudentProfile profile = new StudentProfile();
        profile.setUserId(1001L);
        profile.setGrade("2023");
        profile.setMajor("软件工程");
        profile.setPoliticalStatus("共青团员");
        PartyStudentProgress progress = new PartyStudentProgress();
        progress.setUserId(1001L);
        progress.setCurrentStageCode("activist");
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(20001L);
        article.setTitle("积极分子思想汇报流程");
        article.setSummary("说明积极分子如何提交思想汇报");
        article.setStatus(1);
        article.setContentType("process");
        article.setTags("党团,思想汇报");
        article.setTargetGrades("2023");
        article.setTargetPartyStages("activist");
        article.setPriority(2);
        article.setViewCount(10L);
        article.setPublishTime(LocalDateTime.now());
        when(studentProfileMapper.selectByUserId(1001L)).thenReturn(profile);
        when(partyProgressMapper.selectByUserId(1001L)).thenReturn(progress);
        when(articleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(article));
        when(templateMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        List<KnowledgeRecommendationVO> recommendations = service.listRecommendations(1001L, 5);

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).getScore()).isGreaterThanOrEqualTo(55);
        assertThat(recommendations.get(0).getRecommendReason()).contains("2023").contains("党团阶段");
    }

    @Test
    void shouldFallbackToHotArticlesWhenNoProfileSignalsMatch() {
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        KnowledgeTemplateMapper templateMapper = mock(KnowledgeTemplateMapper.class);
        StudentProfileMapper studentProfileMapper = mock(StudentProfileMapper.class);
        PartyStudentProgressMapper partyProgressMapper = mock(PartyStudentProgressMapper.class);
        KnowledgeBehaviorEventMapper behaviorEventMapper = mock(KnowledgeBehaviorEventMapper.class);
        KnowledgeRecommendationLogMapper recommendationLogMapper = mock(KnowledgeRecommendationLogMapper.class);
        PartyReminderMapper partyReminderMapper = mock(PartyReminderMapper.class);
        KnowledgeCategoryMapper categoryMapper = mock(KnowledgeCategoryMapper.class);
        KnowledgeServiceImpl service = new KnowledgeServiceImpl(
                articleMapper,
                templateMapper,
                studentProfileMapper,
                partyProgressMapper,
                behaviorEventMapper,
                recommendationLogMapper,
                partyReminderMapper,
                categoryMapper,
                new KnowledgeContentRenderer()
        );
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(20002L);
        article.setTitle("在校证明办理指南");
        article.setSummary("说明在校证明如何办理");
        article.setFileId(9301L);
        article.setStatus(1);
        article.setContentType("guide");
        article.setTags("证明,日常服务");
        article.setViewCount(500L);
        article.setPublishTime(LocalDateTime.now());
        when(studentProfileMapper.selectByUserId(1001L)).thenReturn(null);
        when(partyProgressMapper.selectByUserId(1001L)).thenReturn(null);
        when(articleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(article));
        when(templateMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        List<KnowledgeRecommendationVO> recommendations = service.listRecommendations(1001L, 5);

        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations.get(0).getRecommendReason()).contains("热门");
    }

    @Test
    void listArticlesShouldResolveCategoryName() {
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        KnowledgeTemplateMapper templateMapper = mock(KnowledgeTemplateMapper.class);
        StudentProfileMapper studentProfileMapper = mock(StudentProfileMapper.class);
        PartyStudentProgressMapper partyProgressMapper = mock(PartyStudentProgressMapper.class);
        KnowledgeBehaviorEventMapper behaviorEventMapper = mock(KnowledgeBehaviorEventMapper.class);
        KnowledgeRecommendationLogMapper recommendationLogMapper = mock(KnowledgeRecommendationLogMapper.class);
        PartyReminderMapper partyReminderMapper = mock(PartyReminderMapper.class);
        KnowledgeCategoryMapper categoryMapper = mock(KnowledgeCategoryMapper.class);
        KnowledgeServiceImpl service = new KnowledgeServiceImpl(
                articleMapper,
                templateMapper,
                studentProfileMapper,
                partyProgressMapper,
                behaviorEventMapper,
                recommendationLogMapper,
                partyReminderMapper,
                categoryMapper,
                new KnowledgeContentRenderer()
        );
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(20003L);
        article.setCategoryId(4L);
        article.setTitle("在校证明办理指南");
        article.setFileId(9301L);
        article.setStatus(1);
        Page<KnowledgeArticle> page = new Page<>(1, 10);
        page.setRecords(List.of(article));
        page.setTotal(1L);
        KnowledgeCategory category = new KnowledgeCategory();
        category.setId(4L);
        category.setName("日常服务");
        when(articleMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        when(categoryMapper.selectById(4L)).thenReturn(category);

        KnowledgeArticleQueryDTO queryDTO = new KnowledgeArticleQueryDTO();
        List<KnowledgeArticleListItemVO> records = service.listArticles(queryDTO).getRecords();

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getCategoryName()).isEqualTo("日常服务");
        assertThat(records.get(0).getFileId()).isEqualTo(9301L);
    }

    @Test
    void recentSearchKeywordShouldBoostMatchingExtractedTextRecommendation() {
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        KnowledgeTemplateMapper templateMapper = mock(KnowledgeTemplateMapper.class);
        StudentProfileMapper studentProfileMapper = mock(StudentProfileMapper.class);
        PartyStudentProgressMapper partyProgressMapper = mock(PartyStudentProgressMapper.class);
        KnowledgeBehaviorEventMapper behaviorEventMapper = mock(KnowledgeBehaviorEventMapper.class);
        KnowledgeRecommendationLogMapper recommendationLogMapper = mock(KnowledgeRecommendationLogMapper.class);
        PartyReminderMapper partyReminderMapper = mock(PartyReminderMapper.class);
        KnowledgeCategoryMapper categoryMapper = mock(KnowledgeCategoryMapper.class);
        KnowledgeServiceImpl service = new KnowledgeServiceImpl(
                articleMapper,
                templateMapper,
                studentProfileMapper,
                partyProgressMapper,
                behaviorEventMapper,
                recommendationLogMapper,
                partyReminderMapper,
                categoryMapper,
                new KnowledgeContentRenderer()
        );
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(30001L);
        article.setTitle("奖助政策附件");
        article.setSummary("查看附件");
        article.setStatus(1);
        article.setExtractedText("家庭经济困难认定 材料清单 奖助学金申请");
        article.setViewCount(0L);
        com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent event = new com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent();
        event.setKeyword("困难认定");
        when(articleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(article));
        when(templateMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(studentProfileMapper.selectByUserId(1001L)).thenReturn(null);
        when(partyProgressMapper.selectByUserId(1001L)).thenReturn(null);
        when(partyReminderMapper.selectPendingByUserId(1001L)).thenReturn(List.of());
        when(behaviorEventMapper.selectList(any(Wrapper.class))).thenReturn(List.of(event));

        List<KnowledgeRecommendationVO> recommendations = service.listRecommendations(1001L, 6);

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).getTargetId()).isEqualTo(30001L);
        assertThat(recommendations.get(0).getRecommendReason()).contains("最近搜索");
    }

    @Test
    void recordTemplateDownloadShouldIncrementDownloadCount() {
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        KnowledgeTemplateMapper templateMapper = mock(KnowledgeTemplateMapper.class);
        StudentProfileMapper studentProfileMapper = mock(StudentProfileMapper.class);
        PartyStudentProgressMapper partyProgressMapper = mock(PartyStudentProgressMapper.class);
        KnowledgeBehaviorEventMapper behaviorEventMapper = mock(KnowledgeBehaviorEventMapper.class);
        KnowledgeRecommendationLogMapper recommendationLogMapper = mock(KnowledgeRecommendationLogMapper.class);
        PartyReminderMapper partyReminderMapper = mock(PartyReminderMapper.class);
        KnowledgeCategoryMapper categoryMapper = mock(KnowledgeCategoryMapper.class);
        KnowledgeServiceImpl service = new KnowledgeServiceImpl(
                articleMapper,
                templateMapper,
                studentProfileMapper,
                partyProgressMapper,
                behaviorEventMapper,
                recommendationLogMapper,
                partyReminderMapper,
                categoryMapper,
                new KnowledgeContentRenderer()
        );
        KnowledgeTemplate template = new KnowledgeTemplate();
        template.setId(9101L);
        template.setFileId(9201L);
        template.setDownloadCount(3L);
        when(templateMapper.selectOne(any(Wrapper.class))).thenReturn(template);

        service.recordTemplateDownload(1001L, 9201L, "file-download");

        org.mockito.ArgumentCaptor<KnowledgeTemplate> captor = org.mockito.ArgumentCaptor.forClass(KnowledgeTemplate.class);
        verify(templateMapper).updateById(captor.capture());
        assertThat(captor.getValue().getDownloadCount()).isEqualTo(4L);
        verify(behaviorEventMapper).insert(any());
    }

    @Test
    void recordTemplateDownloadShouldRecordKnowledgeArticleFileBehaviorWhenTemplateMissing() {
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        KnowledgeTemplateMapper templateMapper = mock(KnowledgeTemplateMapper.class);
        StudentProfileMapper studentProfileMapper = mock(StudentProfileMapper.class);
        PartyStudentProgressMapper partyProgressMapper = mock(PartyStudentProgressMapper.class);
        KnowledgeBehaviorEventMapper behaviorEventMapper = mock(KnowledgeBehaviorEventMapper.class);
        KnowledgeRecommendationLogMapper recommendationLogMapper = mock(KnowledgeRecommendationLogMapper.class);
        PartyReminderMapper partyReminderMapper = mock(PartyReminderMapper.class);
        KnowledgeCategoryMapper categoryMapper = mock(KnowledgeCategoryMapper.class);
        KnowledgeServiceImpl service = new KnowledgeServiceImpl(
                articleMapper,
                templateMapper,
                studentProfileMapper,
                partyProgressMapper,
                behaviorEventMapper,
                recommendationLogMapper,
                partyReminderMapper,
                categoryMapper,
                new KnowledgeContentRenderer()
        );
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(20001L);
        article.setFileId(9301L);
        when(templateMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(articleMapper.selectOne(any(Wrapper.class))).thenReturn(article);

        service.recordTemplateDownload(1001L, 9301L, "file-download");

        org.mockito.ArgumentCaptor<com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent> captor = org.mockito.ArgumentCaptor.forClass(com.ruc.platform.knowledgeness.entity.KnowledgeBehaviorEvent.class);
        verify(behaviorEventMapper).insert(captor.capture());
        assertThat(captor.getValue().getTargetType()).isEqualTo("article");
        assertThat(captor.getValue().getTargetId()).isEqualTo(20001L);
    }

    @Test
    void getArticleDetailShouldRenderMarkdownPreview() {
        KnowledgeArticleMapper articleMapper = mock(KnowledgeArticleMapper.class);
        KnowledgeTemplateMapper templateMapper = mock(KnowledgeTemplateMapper.class);
        StudentProfileMapper studentProfileMapper = mock(StudentProfileMapper.class);
        PartyStudentProgressMapper partyProgressMapper = mock(PartyStudentProgressMapper.class);
        KnowledgeBehaviorEventMapper behaviorEventMapper = mock(KnowledgeBehaviorEventMapper.class);
        KnowledgeRecommendationLogMapper recommendationLogMapper = mock(KnowledgeRecommendationLogMapper.class);
        PartyReminderMapper partyReminderMapper = mock(PartyReminderMapper.class);
        KnowledgeCategoryMapper categoryMapper = mock(KnowledgeCategoryMapper.class);
        KnowledgeServiceImpl service = new KnowledgeServiceImpl(
                articleMapper,
                templateMapper,
                studentProfileMapper,
                partyProgressMapper,
                behaviorEventMapper,
                recommendationLogMapper,
                partyReminderMapper,
                categoryMapper,
                new KnowledgeContentRenderer()
        );
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(20005L);
        article.setTitle("Markdown 指南");
        article.setStatus(1);
        article.setContentMode("editor");
        article.setEditorType("markdown");
        article.setSourceContent("# 一级标题\n\n正文");
        article.setViewCount(0L);
        when(articleMapper.selectById(20005L)).thenReturn(article);

        var detail = service.getArticleDetail(20005L);

        assertThat(detail.getRenderedContent()).contains("<h1>一级标题</h1>").contains("<p>正文</p>");
    }
}
