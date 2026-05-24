package com.ruc.platform.home.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruc.platform.home.entity.HomeBanner;
import com.ruc.platform.home.mapper.HomeBannerMapper;
import com.ruc.platform.home.mapper.UserQuickEntryMapper;
import com.ruc.platform.home.vo.LatestNoticeVO;
import com.ruc.platform.home.vo.HomeVO;
import com.ruc.platform.home.vo.TodoStatsVO;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.knowledgeness.mapper.KnowledgeArticleMapper;
import com.ruc.platform.knowledgeness.entity.KnowledgeTemplate;
import com.ruc.platform.knowledgeness.mapper.KnowledgeTemplateMapper;
import com.ruc.platform.notice.entity.Notice;
import com.ruc.platform.notice.mapper.NoticeMapper;
import com.ruc.platform.auth.service.RoleAccessService;
import com.ruc.platform.notice.mapper.UserMessageMapper;
import com.ruc.platform.notice.service.NoticeFeedbackService;
import com.ruc.platform.party.entity.PartyReminder;
import com.ruc.platform.party.mapper.PartyReminderMapper;
import com.ruc.platform.party.mapper.PartyReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final UserMessageMapper userMessageMapper;
    private final PartyReminderMapper partyReminderMapper;
    private final PartyReportMapper partyReportMapper;
    private final NoticeMapper noticeMapper;
    private final KnowledgeTemplateMapper knowledgeTemplateMapper;
    private final NoticeFeedbackService noticeFeedbackService;
    private final RoleAccessService roleAccessService;
    private final HomeBannerMapper homeBannerMapper;
    private final UserQuickEntryMapper userQuickEntryMapper;
    private final KnowledgeArticleMapper knowledgeArticleMapper;

    private static final Map<String, Map<String, String>> ALL_SERVICE_MAP = new LinkedHashMap<>();
    static {
        put("partyProgress", "入党流程追踪", "🧭", "/pages/party-progress/party-progress");
        put("partyReport", "思想汇报提交", "📝", "/pages/party-report/party-report");
        put("partyActivity", "党团活动申请", "🗓️", "/pages/party-activity/party-activity");
        put("certificate", "电子证明生成", "🪪", "/pages/e-certificate/e-certificate");
        put("leave", "请假审批流程", "📝", "/pages/leave-list/leave-list");
        put("policyKnowledge", "政策知识库", "📖", "/pages/knowledge/knowledge");
        put("studyAnalysis", "学业分析与预警", "📈", "/pages/study-analysis/study-analysis");
        put("portrait", "学生画像", "🧑‍🎓", "/pages/student-portrait/student-portrait");
        put("honor", "奖励荣誉", "🏅", "/pages/honor/honor");
    }

    private static void put(String code, String name, String icon, String path) {
        Map<String, String> entry = new HashMap<>();
        entry.put("name", name);
        entry.put("icon", icon);
        entry.put("path", path);
        ALL_SERVICE_MAP.put(code, entry);
    }

    @Override
    public HomeVO getHomeData(Long userId) {
        HomeVO homeVO = new HomeVO();

        homeVO.setBanners(getBanners());

        homeVO.setQuickEntries(getUserQuickEntries(userId));

        homeVO.setAllServiceEntries(getAllServiceEntries());

        homeVO.setServiceEntries(getStaticServiceEntries());

        homeVO.setTodoStats(getTodoStats(userId));
        homeVO.setLatestNotices(getLatestNotices());
        homeVO.setDownloads(getDownloads());
        return homeVO;
    }

    private List<Map<String, Object>> getBanners() {
        List<HomeBanner> banners = homeBannerMapper.selectList(
                new LambdaQueryWrapper<HomeBanner>()
                        .eq(HomeBanner::getStatus, 1)
                        .orderByAsc(HomeBanner::getSortOrder)
        );
        if (banners != null && !banners.isEmpty()) {
            return banners.stream().map(b -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", b.getId());
                m.put("title", b.getTitle());
                m.put("subtitle", b.getSubtitle());
                m.put("imageUrl", b.getImageUrl());
                m.put("targetType", b.getTargetType());
                m.put("targetId", b.getTargetId());
                m.put("targetPath", b.getTargetPath());
                return m;
            }).collect(Collectors.toList());
        }

        List<KnowledgeArticle> articles = knowledgeArticleMapper.selectList(
                new LambdaQueryWrapper<KnowledgeArticle>()
                        .eq(KnowledgeArticle::getIsBanner, true)
                        .eq(KnowledgeArticle::getStatus, 1)
                        .orderByDesc(KnowledgeArticle::getPublishTime)
                        .last("LIMIT 5")
        );
        if (articles != null && !articles.isEmpty()) {
            return articles.stream().map(a -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", a.getId());
                m.put("title", a.getTitle());
                m.put("subtitle", a.getSummary());
                m.put("imageUrl", null);
                m.put("targetType", "knowledge");
                m.put("targetId", a.getId());
                m.put("targetPath", null);
                return m;
            }).collect(Collectors.toList());
        }

        List<Notice> notices = noticeMapper.selectList(
                new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getIsBanner, true)
                        .eq(Notice::getStatus, 1)
                        .orderByDesc(Notice::getPublishTime)
                        .last("LIMIT 3")
        );
        if (notices != null && !notices.isEmpty()) {
            return notices.stream().map(n -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", n.getId());
                m.put("title", n.getTitle());
                m.put("subtitle", n.getSummary());
                m.put("imageUrl", null);
                m.put("targetType", "notice");
                m.put("targetId", n.getId());
                m.put("targetPath", null);
                return m;
            }).collect(Collectors.toList());
        }

        List<Map<String, Object>> fallback = new ArrayList<>();
        Map<String, Object> fb1 = new HashMap<>();
        fb1.put("id", 0);
        fb1.put("title", "欢迎使用学院服务平台");
        fb1.put("subtitle", "便捷获取政策信息与党团服务");
        fb1.put("targetType", "none");
        fallback.add(fb1);
        Map<String, Object> fb2 = new HashMap<>();
        fb2.put("id", 1);
        fb2.put("title", "知识库全新上线");
        fb2.put("subtitle", "查询政策说明、办事指南与模板材料");
        fb2.put("targetType", "none");
        fallback.add(fb2);
        Map<String, Object> fb3 = new HashMap<>();
        fb3.put("id", 2);
        fb3.put("title", "党团事务一站式办理");
        fb3.put("subtitle", "入党流程追踪、思想汇报在线提交");
        fb3.put("targetType", "none");
        fallback.add(fb3);
        return fallback;
    }

    private List<Map<String, String>> getUserQuickEntries(Long userId) {
        List<com.ruc.platform.home.entity.UserQuickEntry> entries = userQuickEntryMapper.selectList(
                new LambdaQueryWrapper<com.ruc.platform.home.entity.UserQuickEntry>()
                        .eq(com.ruc.platform.home.entity.UserQuickEntry::getUserId, userId)
                        .orderByAsc(com.ruc.platform.home.entity.UserQuickEntry::getSortOrder)
        );
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        return entries.stream().map(e -> {
            Map<String, String> m = new HashMap<>();
            m.put("code", e.getEntryCode());
            m.put("name", e.getEntryName());
            m.put("icon", e.getEntryIcon());
            m.put("path", e.getEntryPath());
            return m;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getAllServiceEntries() {
        return ALL_SERVICE_MAP.entrySet().stream().map(e -> {
            Map<String, Object> m = new HashMap<>(e.getValue());
            m.put("code", e.getKey());
            return m;
        }).collect(Collectors.toList());
    }

    private List<Map<String, String>> getStaticServiceEntries() {
        List<Map<String, String>> list = new ArrayList<>();
        list.add(createEntry("leave", "请假申请", "file"));
        list.add(createEntry("party", "党团事务", "flag"));
        list.add(createEntry("template", "模板下载", "download"));
        list.add(createEntry("studyAnalysis", "学业分析", "chart"));
        return list;
    }

    private Map<String, String> createEntry(String code, String name, String icon) {
        Map<String, String> entry = new HashMap<>();
        entry.put("code", code);
        entry.put("name", name);
        entry.put("icon", icon);
        return entry;
    }

    private TodoStatsVO getTodoStats(Long userId) {
        TodoStatsVO stats = new TodoStatsVO();
        Long unreadCount = userMessageMapper.countUnreadByUserId(userId);
        List<PartyReminder> reminders = partyReminderMapper.selectPendingByUserId(userId);
        stats.setUnreadMessages(unreadCount == null ? 0 : unreadCount.intValue());
        stats.setUpcomingDeadlines(reminders.size());
        stats.setPendingReports(0);
        if (roleAccessService.hasAnyRole(userId, "cadre")) {
            stats.setPendingFeedbacks(noticeFeedbackService.countCadrePending(userId).intValue());
            stats.setPendingFeedbackRole("cadre");
        } else {
            stats.setPendingFeedbacks(0);
            stats.setPendingFeedbackRole("none");
        }
        return stats;
    }

    private List<LatestNoticeVO> getLatestNotices() {
        List<Notice> notices = noticeMapper.selectList(
                new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getStatus, 1)
                        .orderByDesc(Notice::getPublishTime)
                        .last("LIMIT 5")
        );
        if (notices == null || notices.isEmpty()) {
            return Collections.emptyList();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<LatestNoticeVO> result = new ArrayList<>(notices.size());
        for (Notice notice : notices) {
            LatestNoticeVO vo = new LatestNoticeVO();
            vo.setId(notice.getId());
            vo.setTitle(notice.getTitle());
            vo.setSummary(notice.getSummary());
            vo.setTag(notice.getTag());
            vo.setNoticeType(notice.getNoticeType());
            if (notice.getPublishTime() != null) {
                vo.setPublishDate(notice.getPublishTime().format(formatter));
                vo.setPublishTime(notice.getPublishTime());
            }
            result.add(vo);
        }
        return result;
    }

    private List<Map<String, String>> getDownloads() {
        List<KnowledgeTemplate> templates = knowledgeTemplateMapper.selectEnabledTemplates();
        if (templates == null || templates.isEmpty()) {
            return Collections.emptyList();
        }

        int limit = Math.min(templates.size(), 5);
        List<Map<String, String>> downloads = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            KnowledgeTemplate template = templates.get(i);
            Map<String, String> item = new HashMap<>();
            item.put("name", template.getName() == null ? "模板" : template.getName());
            item.put("description", template.getDescription() == null ? "" : template.getDescription());
            item.put("format", template.getFormat() == null ? "" : template.getFormat());
            downloads.add(item);
        }
        return downloads;
    }
}
