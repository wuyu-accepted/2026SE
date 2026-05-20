package com.ruc.platform.admin.party.service;

import com.ruc.platform.admin.party.dto.PartyProgressBatchImportDTO;
import com.ruc.platform.admin.party.vo.PartyProgressBatchImportResultVO;
import com.ruc.platform.admin.party.vo.PartyStageOptionVO;
import com.ruc.platform.admin.party.vo.PartyStepOptionVO;

import java.util.List;

public interface PartyAdminService {

    PartyProgressBatchImportResultVO batchImportProgress(PartyProgressBatchImportDTO dto);

    List<PartyStageOptionVO> listStageOptions();

    List<PartyStepOptionVO> listStepOptions(String stageCode);
}
