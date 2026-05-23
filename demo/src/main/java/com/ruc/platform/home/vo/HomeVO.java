package com.ruc.platform.home.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class HomeVO {
    private List<Map<String, Object>> banners;

    private List<Map<String, String>> quickEntries;

    private List<Map<String, Object>> allServiceEntries;

    private List<Map<String, String>> serviceEntries;

    private TodoStatsVO todoStats;

    private List<LatestNoticeVO> latestNotices;

    private List<Map<String, String>> downloads;
}
