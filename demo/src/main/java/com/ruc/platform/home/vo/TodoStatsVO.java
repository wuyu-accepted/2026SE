package com.ruc.platform.home.vo;

import lombok.Data;

/**
 * 待办统计VO
 */
@Data
public class TodoStatsVO {

    /**
     * 待提交汇报数量
     */
    private Integer pendingReports;

    /**
     * 未读消息数量
     */
    private Integer unreadMessages;

    /**
     * 即将截止事项数量
     */
    private Integer upcomingDeadlines;

    /**
     * 待处理通知反馈数量
     */
    private Integer pendingFeedbacks;

    /**
     * 待处理反馈角色：cadre/counselor/none
     */
    private String pendingFeedbackRole;
}
