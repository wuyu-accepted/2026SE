package com.ruc.platform.studyanalysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.student.entity.StudentProfile;
import com.ruc.platform.student.mapper.StudentProfileMapper;
import com.ruc.platform.studyanalysis.dto.StudyCourseBatchImportDTO;
import com.ruc.platform.studyanalysis.dto.StudyCourseUploadItemDTO;
import com.ruc.platform.studyanalysis.entity.StudyCourseRecord;
import com.ruc.platform.studyanalysis.mapper.StudyCourseRecordMapper;
import com.ruc.platform.studyanalysis.vo.StudyAnalysisSummaryVO;
import com.ruc.platform.studyanalysis.vo.StudyCategorySummaryVO;
import com.ruc.platform.studyanalysis.vo.StudyCourseImportResultVO;
import com.ruc.platform.studyanalysis.vo.StudyMissingCoursesVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyAnalysisServiceImpl implements StudyAnalysisService {

    private static final List<String> CATEGORIES = List.of(
            "通识模块",
            "专业模块",
            "创新训练与科学研究",
            "素质拓展与发展指导"
    );

    private final StudyCourseRecordMapper studyCourseRecordMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final StudyPlanCourseCreditCatalog creditCatalog;

    @Override
    public List<StudyCourseRecord> listMyRecords(Long userId) {
        return studyCourseRecordMapper.selectByUserId(userId);
    }

    @Override
    public void addMyCourses(Long userId, List<StudyCourseUploadItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (StudyCourseUploadItemDTO item : items) {
            String courseName = clean(item.getCourseName());
            String category = normalizeCategory(item.getCategory());
            if (courseName.isEmpty()) {
                continue;
            }
            BigDecimal credits = creditCatalog.findCredits(courseName).orElse(BigDecimal.ZERO);

            StudyCourseRecord existing = studyCourseRecordMapper.selectOne(new LambdaQueryWrapper<StudyCourseRecord>()
                    .eq(StudyCourseRecord::getUserId, userId)
                    .eq(StudyCourseRecord::getCourseName, courseName)
                    .eq(StudyCourseRecord::getCategory, category)
                    .last("LIMIT 1"));
            if (existing == null) {
                StudyCourseRecord record = new StudyCourseRecord();
                record.setUserId(userId);
                record.setCourseName(courseName);
                record.setCategory(category);
                record.setCredits(credits);
                studyCourseRecordMapper.insert(record);
            } else {
                existing.setCredits(credits);
                studyCourseRecordMapper.updateById(existing);
            }
        }
    }

    @Override
    public StudyCourseImportResultVO importMyCourses(Long userId, StudyCourseBatchImportDTO dto) {
        String text = dto == null ? "" : clean(dto.getText());
        String defaultCategory = dto == null ? "" : normalizeCategory(dto.getDefaultCategory());
        if (defaultCategory.isEmpty()) {
            defaultCategory = "专业模块";
        }
        if (text.isEmpty()) {
            StudyCourseImportResultVO empty = new StudyCourseImportResultVO();
            empty.setTotalLines(0);
            empty.setImportedCount(0);
            empty.setSkippedCount(0);
            empty.setSkippedLines(List.of());
            return empty;
        }

        String[] lines = text.split("\\r?\\n");
        List<StudyCourseUploadItemDTO> items = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("[,，\\t|｜]\\s*");
            String courseName = parts.length >= 1 ? clean(parts[0]) : "";
            String category = parts.length >= 2 ? normalizeCategory(parts[1]) : defaultCategory;
            if (courseName.isEmpty()) {
                skipped.add(raw);
                continue;
            }
            StudyCourseUploadItemDTO item = new StudyCourseUploadItemDTO();
            item.setCourseName(courseName);
            item.setCategory(category);
            items.add(item);
        }

        addMyCourses(userId, items);

        StudyCourseImportResultVO result = new StudyCourseImportResultVO();
        result.setTotalLines(lines.length);
        result.setImportedCount(items.size());
        result.setSkippedCount(skipped.size());
        result.setSkippedLines(skipped.size() > 10 ? skipped.subList(0, 10) : skipped);
        return result;
    }

    @Override
    public void clearMyCourses(Long userId) {
        studyCourseRecordMapper.delete(new LambdaQueryWrapper<StudyCourseRecord>().eq(StudyCourseRecord::getUserId, userId));
    }

    @Override
    public StudyAnalysisSummaryVO getMySummary(Long userId) {
        StudentProfile profile = studentProfileMapper.selectByUserId(userId);
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "学生档案不存在");
        }
        String major = normalizeMajor(profile.getMajor());
        Map<String, BigDecimal> required = requiredCreditsByMajor(major);

        List<StudyCourseRecord> records = studyCourseRecordMapper.selectByUserId(userId);
        Map<String, BigDecimal> earned = new LinkedHashMap<>();
        List<String> unknown = new ArrayList<>();

        for (StudyCourseRecord record : records) {
            String category = normalizeCategory(record.getCategory());
            BigDecimal credits = record.getCredits() == null ? BigDecimal.ZERO : record.getCredits();
            earned.put(category, earned.getOrDefault(category, BigDecimal.ZERO).add(credits));
            if (credits.compareTo(BigDecimal.ZERO) <= 0) {
                unknown.add(record.getCourseName());
            }
        }

        List<StudyCategorySummaryVO> summaries = new ArrayList<>();
        for (String category : CATEGORIES) {
            BigDecimal req = required.getOrDefault(category, BigDecimal.ZERO);
            BigDecimal got = earned.getOrDefault(category, BigDecimal.ZERO);
            BigDecimal remaining = req.subtract(got);
            if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                remaining = BigDecimal.ZERO;
            }
            StudyCategorySummaryVO vo = new StudyCategorySummaryVO();
            vo.setCategory(category);
            vo.setRequiredCredits(req);
            vo.setEarnedCredits(got);
            vo.setRemainingCredits(remaining);
            summaries.add(vo);
        }

        StudyAnalysisSummaryVO result = new StudyAnalysisSummaryVO();
        result.setMajor(major);
        result.setCategories(summaries);
        result.setUnknownCourses(unknown);
        return result;
    }

    @Override
    public StudyMissingCoursesVO getMyMissingCourses(Long userId, String q, Integer limit) {
        StudentProfile profile = studentProfileMapper.selectByUserId(userId);
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "学生档案不存在");
        }
        String major = normalizeMajor(profile.getMajor());

        List<StudyCourseRecord> records = studyCourseRecordMapper.selectByUserId(userId);
        Set<String> recordedNames = records.stream()
                .map(r -> clean(r.getCourseName()))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        Set<String> normalizedRecorded = new HashSet<>();
        for (String name : recordedNames) {
            normalizedRecorded.add(normalizeForCompare(name));
        }

        List<String> requiredChecklist = requiredCourseChecklistByMajor(major);
        List<String> requiredMissing = requiredChecklist.stream()
                .filter(name -> !normalizedRecorded.contains(normalizeForCompare(name)))
                .distinct()
                .collect(Collectors.toList());

        int sizeLimit = (limit == null || limit <= 0) ? 50 : Math.min(limit, 200);
        String keyword = clean(q);
        List<String> all = creditCatalog.listAllCourseNames();
        List<String> catalogMissing = new ArrayList<>();
        for (String name : all) {
            if (catalogMissing.size() >= sizeLimit) {
                break;
            }
            String normalized = normalizeForCompare(name);
            if (normalized.isEmpty() || normalizedRecorded.contains(normalized)) {
                continue;
            }
            if (!keyword.isEmpty() && !name.contains(keyword)) {
                continue;
            }
            if (!matchesMajorHint(major, name)) {
                continue;
            }
            catalogMissing.add(name);
        }

        StudyMissingCoursesVO result = new StudyMissingCoursesVO();
        result.setRequiredMissingCourses(requiredMissing);
        result.setCatalogMissingCourses(catalogMissing);
        return result;
    }

    private Map<String, BigDecimal> requiredCreditsByMajor(String major) {
        if (major.contains("计算机科学与技术")) {
            return Map.of(
                    "通识模块", bd("45"),
                    "专业模块", bd("90"),
                    "创新训练与科学研究", bd("10"),
                    "素质拓展与发展指导", bd("10")
            );
        }
        if (major.contains("软件工程")) {
            return Map.of(
                    "通识模块", bd("45"),
                    "专业模块", bd("91"),
                    "创新训练与科学研究", bd("10"),
                    "素质拓展与发展指导", bd("10")
            );
        }
        if (major.contains("信息安全")) {
            return Map.of(
                    "通识模块", bd("45"),
                    "专业模块", bd("90"),
                    "创新训练与科学研究", bd("10"),
                    "素质拓展与发展指导", bd("10")
            );
        }
        if (major.contains("信息管理与信息系统")) {
            return Map.of(
                    "通识模块", bd("45"),
                    "专业模块", bd("88"),
                    "创新训练与科学研究", bd("10"),
                    "素质拓展与发展指导", bd("10")
            );
        }
        if (major.contains("数据科学与大数据技术")) {
            return Map.of(
                    "通识模块", bd("45"),
                    "专业模块", bd("90"),
                    "创新训练与科学研究", bd("10"),
                    "素质拓展与发展指导", bd("10")
            );
        }
        throw new BizException(ResultCode.PARAM_ERROR, "暂不支持该专业的学业分析");
    }

    private String normalizeMajor(String major) {
        String m = major == null ? "" : major.trim();
        if (m.isEmpty()) {
            return "";
        }
        return m;
    }

    private List<String> requiredCourseChecklistByMajor(String major) {
        List<String> base = List.of(
                "高等数学Ⅰ",
                "高等数学Ⅱ",
                "高等代数Ⅰ",
                "高等代数Ⅱ",
                "普通物理 B",
                "程序设计"
        );
        if (major == null) {
            return base;
        }
        String m = major.trim();
        if (m.contains("数据科学与大数据技术")) {
            return List.of(
                    "高等数学Ⅰ",
                    "高等数学Ⅱ",
                    "高等代数Ⅰ",
                    "高等代数Ⅱ",
                    "普通物理 B",
                    "程序设计",
                    "概率论与数理统计"
            );
        }
        return base;
    }

    private boolean matchesMajorHint(String major, String courseName) {
        if (major == null || major.isBlank() || courseName == null || courseName.isBlank()) {
            return true;
        }
        String m = major.trim();
        if (m.contains("软件工程")) {
            return courseName.contains("软件") || courseName.contains("工程") || courseName.contains("程序") || courseName.contains("系统") || courseName.contains("测试");
        }
        if (m.contains("信息安全")) {
            return courseName.contains("安全") || courseName.contains("密码") || courseName.contains("系统") || courseName.contains("网络") || courseName.contains("计算机");
        }
        if (m.contains("信息管理与信息系统")) {
            return courseName.contains("管理") || courseName.contains("信息系统") || courseName.contains("商务") || courseName.contains("金融") || courseName.contains("数据");
        }
        if (m.contains("数据科学与大数据技术")) {
            return courseName.contains("数据") || courseName.contains("统计") || courseName.contains("概率") || courseName.contains("机器学习") || courseName.contains("大数据");
        }
        if (m.contains("计算机科学与技术")) {
            return courseName.contains("计算机") || courseName.contains("算法") || courseName.contains("系统") || courseName.contains("网络") || courseName.contains("程序") || courseName.contains("数据");
        }
        return true;
    }

    private String normalizeForCompare(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replace('（', '(')
                .replace('）', ')')
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeCategory(String category) {
        String c = clean(category);
        if (c.isEmpty()) {
            return "专业模块";
        }
        if (c.contains("通识")) {
            return "通识模块";
        }
        if (c.contains("创新")) {
            return "创新训练与科学研究";
        }
        if (c.contains("素质") || c.contains("拓展")) {
            return "素质拓展与发展指导";
        }
        if (c.contains("专业")) {
            return "专业模块";
        }
        return c;
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
