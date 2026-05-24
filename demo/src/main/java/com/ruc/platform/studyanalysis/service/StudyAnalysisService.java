package com.ruc.platform.studyanalysis.service;

import com.ruc.platform.studyanalysis.dto.StudyCourseUploadItemDTO;
import com.ruc.platform.studyanalysis.dto.StudyCourseBatchImportDTO;
import com.ruc.platform.studyanalysis.entity.StudyCourseRecord;
import com.ruc.platform.studyanalysis.vo.StudyAnalysisSummaryVO;
import com.ruc.platform.studyanalysis.vo.StudyCourseImportResultVO;
import com.ruc.platform.studyanalysis.vo.StudyMissingCoursesVO;
import com.ruc.platform.studyanalysis.vo.StudyModuleDetailVO;

import java.util.List;

public interface StudyAnalysisService {

    List<StudyCourseRecord> listMyRecords(Long userId);

    void addMyCourses(Long userId, List<StudyCourseUploadItemDTO> items);

    StudyCourseImportResultVO importMyCourses(Long userId, StudyCourseBatchImportDTO dto);

    void clearMyCourses(Long userId);

    StudyAnalysisSummaryVO getMySummary(Long userId);

    StudyMissingCoursesVO getMyMissingCourses(Long userId, String q, Integer limit);

    StudyModuleDetailVO getMyModuleDetail(Long userId, String module, String electiveKey, Integer limit);
}
