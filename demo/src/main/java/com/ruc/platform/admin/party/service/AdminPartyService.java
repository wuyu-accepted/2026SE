package com.ruc.platform.admin.party.service;

import com.ruc.platform.admin.party.dto.*;
import com.ruc.platform.admin.party.vo.*;
import com.ruc.platform.common.api.PageResult;

import java.util.List;
import java.util.Map;

public interface AdminPartyService {
    List<Map<String, Object>> listStages();
    List<Map<String, Object>> listSteps(String stageCode);
    PageResult<PartyStudentProgressVO> listStudentProgress(PartyStudentProgressQueryDTO query);
    PartyStudentProgressAdminVO getStudentProgressDetail(String studentNo, String realName);
    void updateStudentProgress(Long userId, UpdateProgressDTO dto);
    void batchImportProgress(BatchImportProgressDTO dto);
    PageResult<PartyReportVO> listReports(ReportReviewQueryDTO query);
    PartyReportVO getReportDetail(Long id);
    void approveReport(Long id, Long reviewerId, ReportReviewDTO dto);
    void rejectReport(Long id, Long reviewerId, ReportReviewDTO dto);
    PageResult<PartyActivityVO> listActivities(ActivityReviewQueryDTO query);
    PartyActivityVO getActivityDetail(Long id);
    void approveActivity(Long id, Long reviewerId, ActivityReviewDTO dto);
    void rejectActivity(Long id, Long reviewerId, ActivityReviewDTO dto);
}
