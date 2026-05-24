package com.ruc.platform.studyanalysis.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.common.api.Result;
import com.ruc.platform.studyanalysis.dto.StudyCourseBatchImportDTO;
import com.ruc.platform.studyanalysis.dto.StudyCourseUploadItemDTO;
import com.ruc.platform.studyanalysis.entity.StudyCourseRecord;
import com.ruc.platform.studyanalysis.service.StudyAnalysisService;
import com.ruc.platform.studyanalysis.vo.StudyAnalysisSummaryVO;
import com.ruc.platform.studyanalysis.vo.StudyCourseImportResultVO;
import com.ruc.platform.studyanalysis.vo.StudyMissingCoursesVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study-analysis/me")
@RequiredArgsConstructor
public class StudyAnalysisController {

    private final StudyAnalysisService studyAnalysisService;

    @GetMapping("/records")
    public Result<List<StudyCourseRecord>> listMyRecords() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(studyAnalysisService.listMyRecords(userId));
    }

    @PostMapping("/records")
    public Result<Void> addMyRecords(@Valid @RequestBody List<StudyCourseUploadItemDTO> items) {
        long userId = StpUtil.getLoginIdAsLong();
        studyAnalysisService.addMyCourses(userId, items);
        return Result.ok();
    }

    @PostMapping("/records/import")
    public Result<StudyCourseImportResultVO> importMyRecords(@RequestBody StudyCourseBatchImportDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(studyAnalysisService.importMyCourses(userId, dto));
    }

    @DeleteMapping("/records")
    public Result<Void> clearMyRecords() {
        long userId = StpUtil.getLoginIdAsLong();
        studyAnalysisService.clearMyCourses(userId);
        return Result.ok();
    }

    @GetMapping("/summary")
    public Result<StudyAnalysisSummaryVO> getSummary() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(studyAnalysisService.getMySummary(userId));
    }

    @GetMapping("/missing-courses")
    public Result<StudyMissingCoursesVO> getMissingCourses(@RequestParam(required = false) String q,
                                                           @RequestParam(required = false) Integer limit) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(studyAnalysisService.getMyMissingCourses(userId, q, limit));
    }

    @GetMapping("/module-detail")
    public Result<com.ruc.platform.studyanalysis.vo.StudyModuleDetailVO> getModuleDetail(@RequestParam String module,
                                                                                         @RequestParam(required = false) String electiveKey,
                                                                                         @RequestParam(required = false) Integer limit) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(studyAnalysisService.getMyModuleDetail(userId, module, electiveKey, limit));
    }
}
