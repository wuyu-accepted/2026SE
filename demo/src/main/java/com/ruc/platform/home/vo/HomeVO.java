package com.ruc.platform.home.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 首页聚合数据VO
 */
@Data
public class HomeVO {

    /**
     * Banner信息
     */
    private Map<String, String> banner;

    /**
     * 快捷入口列表
     */
    private List<Map<String, String>> quickEntries;

    private List<Map<String, String>> serviceEntries;

    /**
     * 待办统计
     */
    private TodoStatsVO todoStats;

    /**
     * 最新通知列表
     */
    private List<LatestNoticeVO> latestNotices;

    /**
     * 常用下载列表
     */
    private List<Map<String, String>> downloads;
}
