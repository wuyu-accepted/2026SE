package com.ruc.platform.ai.service;

import com.ruc.platform.ai.vo.AiActionVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AiFeatureEntryService {

    private final List<Entry> entries = List.of(
            entry("partyProgress", "入党流程追踪", "查看个人入党阶段、待办和流程指引", "/pages/party-progress/party-progress", false, "入党", "党员", "党团", "流程", "阶段", "积极分子"),
            entry("partyReport", "思想汇报提交", "提交和查看思想汇报", "/pages/party-report/party-report", false, "思想汇报", "汇报", "提交思想", "党团材料"),
            entry("partyActivity", "党团活动申请", "提交党团活动申请", "/pages/party-activity/party-activity", false, "党团活动", "活动申请", "活动"),
            entry("certificate", "电子证明生成", "生成在校证明等常用电子证明", "/pages/e-certificate/e-certificate", false, "证明", "在校证明", "电子证明", "盖章"),
            entry("leave", "请假审批流程", "提交和查看请假申请", "/pages/leave-list/leave-list", false, "请假", "销假", "病假", "事假", "审批", "离校"),
            entry("policyKnowledge", "政策知识库", "查询政策资料和办事指南", "/pages/policy-knowledge/policy-knowledge", false, "政策", "制度", "规定", "指南"),
            entry("studyAnalysis", "学业分析与预警", "查看学业分析和预警信息", "/pages/study-analysis/study-analysis", false, "学业", "成绩", "预警", "分析"),
            entry("portrait", "学生画像", "查看个人成长画像", "/pages/student-portrait/student-portrait", false, "画像", "个人情况", "成长"),
            entry("knowledge", "知识库", "查询知识资料、模板和常见问题", "/pages/knowledge/knowledge", false, "知识库", "资料", "模板", "常见问题"),
            entry("notice", "通知", "查看学院通知和消息", "/pages/notice/notice", true, "通知", "消息", "公告"),
            entry("service", "服务", "查看全部办事服务入口", "/pages/service/service", true, "服务", "入口", "办事"),
            entry("profile", "个人中心", "查看个人信息和账号设置", "/pages/profile/profile", true, "个人中心", "我的", "账号")
    );

    public List<AiActionVO> match(String question, Set<String> roles, List<String> scenarioCodes, Integer limit) {
        String normalized = normalize(question);
        int max = limit == null || limit <= 0 ? 3 : limit;
        return entries.stream()
                .map(entry -> score(entry, normalized, scenarioCodes))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparing(ScoredEntry::score, Comparator.reverseOrder()))
                .limit(max)
                .map(scored -> toVO(scored.entry(), scored.score()))
                .toList();
    }

    private ScoredEntry score(Entry entry, String question, List<String> scenarioCodes) {
        int score = 0;
        for (String keyword : entry.keywords()) {
            if (question.contains(normalize(keyword))) {
                score += Math.max(5, keyword.length() * 5);
            }
        }
        if (scenarioCodes != null && scenarioCodes.stream().anyMatch(code -> entry.code().equalsIgnoreCase(code))) {
            score += 20;
        }
        return new ScoredEntry(entry, score);
    }

    private AiActionVO toVO(Entry entry, int score) {
        AiActionVO vo = new AiActionVO();
        vo.setCode(entry.code());
        vo.setTitle(entry.title());
        vo.setDescription(entry.description());
        vo.setPath(entry.path());
        vo.setTabPage(entry.tabPage());
        vo.setScore(score);
        return vo;
    }

    private Entry entry(String code, String title, String description, String path, boolean tabPage, String... keywords) {
        return new Entry(code, title, description, path, tabPage, List.of(keywords));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private record Entry(String code, String title, String description, String path, boolean tabPage, List<String> keywords) {
    }

    private record ScoredEntry(Entry entry, int score) {
    }
}
