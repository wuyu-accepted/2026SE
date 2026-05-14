package com.ruc.platform.party.service;

import com.ruc.platform.party.vo.PartyOverviewVO;
import com.ruc.platform.party.vo.PartyRecordVO;
import com.ruc.platform.party.vo.PartyReminderVO;
import com.ruc.platform.party.vo.PartyTrackerVO;
import com.ruc.platform.party.dto.PartyReportDTO;

import java.util.List;

/**
 * 党团服务接口
 */
public interface PartyService {

    /**
     * 获取本人党团概览
     * @param userId 用户ID
     * @return 党团概览
     */
    PartyOverviewVO getOverview(Long userId);

    /**
     * 获取本人党团记录
     * @param userId 用户ID
     * @return 党团记录列表
     */
    List<PartyRecordVO> getRecords(Long userId);

    /**
     * 获取本人党团提醒
     * @param userId 用户ID
     * @return 党团提醒列表
     */
    List<PartyReminderVO> getReminders(Long userId);

    /**
     * 提交思想汇报
     * @param userId 用户ID
     * @param reportDTO 汇报数据
     */
    void submitReport(Long userId, PartyReportDTO reportDTO);

    PartyTrackerVO getTracker(Long userId);
}
