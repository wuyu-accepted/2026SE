package com.ruc.platform.admin.party.service;

import com.ruc.platform.admin.party.dto.PartyProgressBatchImportDTO;
import com.ruc.platform.admin.party.vo.*;

import java.util.List;

public interface AdminPartyService {

    List<PartyStageOptionVO> listStageOptions();

    List<PartyStepOptionVO> listStepOptions(String stageCode);

    PartyProgressBatchImportResultVO batchImportProgress(Long operatorUserId, PartyProgressBatchImportDTO dto);

    PartyStudentProgressAdminVO queryStudentProgress(String studentNo, String realName);

    List<PartyReportAdminListItemVO> listReports(Integer status);

    PartyReportAdminDetailVO getReportDetail(Long id);

    void approveReport(Long operatorUserId, Long id, String comment);

    void rejectReport(Long operatorUserId, Long id, String comment);

    List<PartyActivityAdminListItemVO> listActivities(Integer status);

    PartyActivityAdminDetailVO getActivityDetail(Long id);

    void approveActivity(Long operatorUserId, Long id, String comment);

    void rejectActivity(Long operatorUserId, Long id, String comment);
}
