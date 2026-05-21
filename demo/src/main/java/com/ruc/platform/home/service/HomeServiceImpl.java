package com.ruc.platform.home.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruc.platform.home.vo.LatestNoticeVO;
import com.ruc.platform.home.vo.HomeVO;
import com.ruc.platform.home.vo.TodoStatsVO;
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
import java.util.List;
import java.util.Map;

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

    @Override
    public HomeVO getHomeData(Long userId) {
        HomeVO homeVO = new HomeVO();

        Map<String, String> banner = new HashMap<>();
        banner.put("title", "欢迎使用学院服务平台");
        banner.put("subtitle", "便捷获取政策信息与党团服务");
        homeVO.setBanner(banner);

        List<Map<String, String>> quickEntries = new ArrayList<>();
        quickEntries.add(createEntry("knowledge", "知识库", "book"));
        quickEntries.add(createEntry("notice", "通知", "bell"));
        quickEntries.add(createEntry("service", "服务", "grid"));
        homeVO.setQuickEntries(quickEntries);

        List<Map<String, String>> serviceEntries = new ArrayList<>();
        serviceEntries.add(createEntry("leave", "请假申请", "file"));
        serviceEntries.add(createEntry("party", "党团事务", "flag"));
        serviceEntries.add(createEntry("template", "模板下载", "download"));
        serviceEntries.add(createEntry("studyAnalysis", "学业分析", "chart"));
        homeVO.setServiceEntries(serviceEntries);

        homeVO.setTodoStats(getTodoStats(userId));
        homeVO.setLatestNotices(getLatestNotices());
        homeVO.setDownloads(getDownloads());
        return homeVO;
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
