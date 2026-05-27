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
import com.ruc.platform.studyanalysis.vo.StudyElectiveModuleStatusVO;
import com.ruc.platform.studyanalysis.vo.StudyMissingCoursesVO;
import com.ruc.platform.studyanalysis.vo.StudyModuleDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final StudyPlanCourseJsonCatalog courseJsonCatalog;

    @Override
    public List<StudyCourseRecord> listMyRecords(Long userId) {
        return studyCourseRecordMapper.selectByUserId(userId);
    }

    @Override
    public void addMyCourses(Long userId, List<StudyCourseUploadItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        StudentProfile profile = studentProfileMapper.selectByUserId(userId);
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "学生档案不存在");
        }
        String major = normalizeMajor(profile.getMajor());
        if (!courseJsonCatalog.isSupportedMajor(major)) {
            throw new BizException(ResultCode.PARAM_ERROR, "暂不支持该专业的学业分析");
        }

        for (StudyCourseUploadItemDTO item : items) {
            String courseName = clean(item.getCourseName());
            String category = normalizeCategory(item.getCategory());
            if (courseName.isEmpty()) {
                continue;
            }
            BigDecimal credits = resolveCredits(major, item.getCredits(), courseName);

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
                if (item.getCredits() != null && item.getCredits().compareTo(BigDecimal.ZERO) > 0) {
                    existing.setCredits(item.getCredits());
                } else if (existing.getCredits() == null || existing.getCredits().compareTo(BigDecimal.ZERO) <= 0) {
                    existing.setCredits(credits);
                }
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
            String category = defaultCategory;
            BigDecimal credits = null;
            if (parts.length >= 2) {
                BigDecimal p2 = parseCreditsOrNull(parts[1]);
                if (p2 != null) {
                    credits = p2;
                } else {
                    category = normalizeCategory(parts[1]);
                }
            }
            if (parts.length >= 3) {
                BigDecimal p3 = parseCreditsOrNull(parts[2]);
                if (p3 != null) {
                    credits = p3;
                }
            }
            if (courseName.isEmpty()) {
                skipped.add(raw);
                continue;
            }
            StudyCourseUploadItemDTO item = new StudyCourseUploadItemDTO();
            item.setCourseName(courseName);
            item.setCategory(category);
            item.setCredits(credits);
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
        if (!courseJsonCatalog.isSupportedMajor(major)) {
            throw new BizException(ResultCode.PARAM_ERROR, "暂不支持该专业的学业分析");
        }
        Map<String, BigDecimal> required = requiredCreditsByMajor(major);

        List<StudyCourseRecord> records = studyCourseRecordMapper.selectByUserId(userId);
        Map<String, BigDecimal> earned = new LinkedHashMap<>();
        List<String> unknown = new ArrayList<>();

        for (StudyCourseRecord record : records) {
            String category = normalizeCategory(record.getCategory());
            BigDecimal credits = resolveCredits(major, record.getCredits(), record.getCourseName());
            if (record.getCredits() == null || record.getCredits().compareTo(BigDecimal.ZERO) <= 0) {
                if (credits.compareTo(BigDecimal.ZERO) > 0) {
                    record.setCredits(credits);
                    studyCourseRecordMapper.updateById(record);
                }
            }
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
        result.setWarnings(buildWarnings(major, records, summaries, unknown));
        return result;
    }

    @Override
    public StudyMissingCoursesVO getMyMissingCourses(Long userId, String q, Integer limit) {
        StudentProfile profile = studentProfileMapper.selectByUserId(userId);
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "学生档案不存在");
        }
        String major = normalizeMajor(profile.getMajor());
        if (!courseJsonCatalog.isSupportedMajor(major)) {
            throw new BizException(ResultCode.PARAM_ERROR, "暂不支持该专业的学业分析");
        }

        List<StudyCourseRecord> records = studyCourseRecordMapper.selectByUserId(userId);
        Set<String> recordedNames = records.stream()
                .map(r -> clean(r.getCourseName()))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        Set<String> normalizedRecorded = new HashSet<>();
        for (String name : recordedNames) {
            normalizedRecorded.add(normalizeForCompare(name));
        }

        int sizeLimit = (limit == null || limit <= 0) ? 200 : Math.min(limit, 500);
        String keyword = clean(q);
        List<String> required = courseJsonCatalog.getRequiredCourses(major, "专业模块");
        List<String> requiredMissing = new ArrayList<>();
        for (String name : required) {
            if (requiredMissing.size() >= sizeLimit) {
                break;
            }
            if (normalizedRecorded.contains(normalizeForCompare(name))) {
                continue;
            }
            if (!keyword.isEmpty() && !name.contains(keyword)) {
                continue;
            }
            requiredMissing.add(name);
        }

        StudyMissingCoursesVO result = new StudyMissingCoursesVO();
        result.setRequiredMissingCourses(requiredMissing);
        result.setCatalogMissingCourses(List.of());
        return result;
    }

    @Override
    public StudyModuleDetailVO getMyModuleDetail(Long userId, String module, String electiveKey, Integer limit) {
        StudentProfile profile = studentProfileMapper.selectByUserId(userId);
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "学生档案不存在");
        }
        String major = normalizeMajor(profile.getMajor());
        if (!courseJsonCatalog.isSupportedMajor(major)) {
            throw new BizException(ResultCode.PARAM_ERROR, "暂不支持该专业的学业分析");
        }

        List<StudyCourseRecord> records = studyCourseRecordMapper.selectByUserId(userId);
        Set<String> normalizedRecorded = records.stream()
                .map(r -> normalizeForCompare(r.getCourseName()))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        int sizeLimit = (limit == null || limit <= 0) ? 200 : Math.min(limit, 800);
        String moduleName = normalizeModule(module);

        StudyModuleDetailVO vo = new StudyModuleDetailVO();
        vo.setModule(moduleName);
        vo.setElectiveSuggestions(List.of());
        vo.setSemesterPlans(List.of());

        if (electiveKey != null && !electiveKey.isBlank()) {
            StudyPlanCourseJsonCatalog.ElectiveModule m = courseJsonCatalog.getElectiveModule(major, electiveKey.trim());
            if (m == null) {
                vo.setMissingCourses(List.of());
                vo.setElectiveModules(List.of());
                vo.setElectiveSuggestions(List.of());
                vo.setSemesterPlans(List.of());
                return vo;
            }
            List<String> missing = new ArrayList<>();
            for (String course : m.getCourses()) {
                if (missing.size() >= sizeLimit) {
                    break;
                }
                if (!normalizedRecorded.contains(normalizeForCompare(course))) {
                    missing.add(course);
                }
            }
            vo.setMissingCourses(missing);
            vo.setElectiveModules(List.of());

            StudyModuleDetailVO.ElectiveSuggestionVO suggestion = new StudyModuleDetailVO.ElectiveSuggestionVO();
            suggestion.setKey(m.getKey());
            suggestion.setName(m.getName());
            suggestion.setReason("该选修大类未修 " + missing.size() + " 门，可优先从下方推荐或未修列表中选择");
            suggestion.setRecommendedCourses(missing.stream().limit(6).toList());
            vo.setElectiveSuggestions(List.of(suggestion));

            vo.setSemesterPlans(buildElectiveSemesterPlans(profile, major, missing));
            return vo;
        }

        List<String> required = courseJsonCatalog.getRequiredCourses(major, moduleName);
        if (!required.isEmpty()) {
            // ok
        } else if ("通识模块".equals(moduleName) || "创新训练与科学研究".equals(moduleName) || "素质拓展与发展指导".equals(moduleName) || "专业模块".equals(moduleName)) {
            throw new BizException(ResultCode.BIZ_ERROR, "课程库为空或解析失败，请检查 course.json");
        }

        List<String> missing = new ArrayList<>();
        for (String course : required) {
            if (missing.size() >= sizeLimit) {
                break;
            }
            if (!normalizedRecorded.contains(normalizeForCompare(course))) {
                missing.add(course);
            }
        }
        vo.setMissingCourses(missing);

        if ("专业模块".equals(moduleName)) {
            List<StudyElectiveModuleStatusVO> electiveStatuses = new ArrayList<>();
            for (StudyPlanCourseJsonCatalog.ElectiveModule m : courseJsonCatalog.listElectiveModules(major)) {
                int total = m.getCourses() == null ? 0 : m.getCourses().size();
                int taken = 0;
                List<String> preview = new ArrayList<>();
                int missingCount = 0;
                if (m.getCourses() != null) {
                    for (String course : m.getCourses()) {
                        if (normalizedRecorded.contains(normalizeForCompare(course))) {
                            taken++;
                        } else {
                            missingCount++;
                            if (preview.size() < 5) {
                                preview.add(course);
                            }
                        }
                    }
                }
                StudyElectiveModuleStatusVO status = new StudyElectiveModuleStatusVO();
                status.setKey(m.getKey());
                status.setName(m.getName());
                status.setTotalCourses(total);
                status.setTakenCourses(taken);
                status.setMissingCourses(missingCount);
                status.setMissingPreview(preview);
                electiveStatuses.add(status);
            }
            vo.setElectiveModules(electiveStatuses);

            List<StudyModuleDetailVO.ElectiveSuggestionVO> suggestions = buildElectiveSuggestions(major, electiveStatuses, normalizedRecorded);
            vo.setElectiveSuggestions(suggestions);

            List<StudyModuleDetailVO.SemesterPlanVO> semesterPlans = buildSemesterPlans(profile, major, null, suggestions, normalizedRecorded, sizeLimit);
            vo.setSemesterPlans(semesterPlans);
        } else {
            vo.setElectiveModules(List.of());
        }
        return vo;
    }

    private List<StudyModuleDetailVO.ElectiveSuggestionVO> buildElectiveSuggestions(String major,
                                                                                   List<StudyElectiveModuleStatusVO> electiveStatuses,
                                                                                   Set<String> normalizedRecorded) {
        if (electiveStatuses == null || electiveStatuses.isEmpty()) {
            return List.of();
        }
        List<StudyElectiveModuleStatusVO> candidates = electiveStatuses.stream()
                .filter(s -> s != null && s.getTotalCourses() != null && s.getTotalCourses() > 0)
                .filter(s -> s.getMissingCourses() != null && s.getMissingCourses() > 0)
                .sorted(Comparator.<StudyElectiveModuleStatusVO>comparingDouble(s -> {
                            double taken = s.getTakenCourses() == null ? 0 : s.getTakenCourses();
                            double total = s.getTotalCourses() == null ? 1 : s.getTotalCourses();
                            return -(taken / total);
                        })
                        .thenComparingInt(s -> s.getMissingCourses() == null ? Integer.MAX_VALUE : s.getMissingCourses())
                        .thenComparingInt(s -> s.getTotalCourses() == null ? 0 : -s.getTotalCourses()))
                .limit(3)
                .toList();

        List<StudyModuleDetailVO.ElectiveSuggestionVO> result = new ArrayList<>();
        for (StudyElectiveModuleStatusVO status : candidates) {
            StudyPlanCourseJsonCatalog.ElectiveModule module = courseJsonCatalog.getElectiveModule(major, status.getKey());
            if (module == null || module.getCourses() == null || module.getCourses().isEmpty()) {
                continue;
            }
            List<String> recommended = new ArrayList<>();
            for (String course : module.getCourses()) {
                if (recommended.size() >= 3) {
                    break;
                }
                if (!normalizedRecorded.contains(normalizeForCompare(course))) {
                    recommended.add(course);
                }
            }
            StudyModuleDetailVO.ElectiveSuggestionVO vo = new StudyModuleDetailVO.ElectiveSuggestionVO();
            vo.setKey(status.getKey());
            vo.setName(status.getName());
            vo.setReason("已修 " + safeInt(status.getTakenCourses()) + " / 共 " + safeInt(status.getTotalCourses()) + "，建议优先补齐剩余 " + safeInt(status.getMissingCourses()) + " 门");
            vo.setRecommendedCourses(recommended);
            result.add(vo);
        }
        return result;
    }

    private List<StudyModuleDetailVO.SemesterPlanVO> buildSemesterPlans(StudentProfile profile,
                                                                       String major,
                                                                       List<String> missingRequired,
                                                                       List<StudyModuleDetailVO.ElectiveSuggestionVO> electiveSuggestions,
                                                                       Set<String> normalizedRecorded,
                                                                       int sizeLimit) {
        int intakeYear = parseIntakeYear(profile == null ? null : profile.getGrade());
        List<String> orderedTermKeys = orderedTermKeys();

        Map<String, List<String>> requiredByTerm = new LinkedHashMap<>();
        Map<String, List<String>> electiveByTerm = new LinkedHashMap<>();
        for (String key : orderedTermKeys) {
            requiredByTerm.put(key, new ArrayList<>());
            electiveByTerm.put(key, new ArrayList<>());
        }
        List<String> unknownRequired = new ArrayList<>();

        if (missingRequired != null) {
            for (String course : missingRequired) {
                List<String> keys = courseJsonCatalog.getCourseTermKeys(major, course);
                String chosen = chooseEarliestTermKey(keys, orderedTermKeys);
                if (chosen == null) {
                    unknownRequired.add(course);
                    continue;
                }
                List<String> list = requiredByTerm.get(chosen);
                if (list != null && list.size() < 12 && !list.contains(course)) {
                    list.add(course);
                }
            }
        }

        List<String> electiveCandidates = new ArrayList<>();
        if (electiveSuggestions != null) {
            for (StudyModuleDetailVO.ElectiveSuggestionVO s : electiveSuggestions) {
                StudyPlanCourseJsonCatalog.ElectiveModule module = courseJsonCatalog.getElectiveModule(major, s.getKey());
                if (module == null || module.getCourses() == null) {
                    continue;
                }
                for (String course : module.getCourses()) {
                    if (electiveCandidates.size() >= Math.min(sizeLimit, 200)) {
                        break;
                    }
                    if (!normalizedRecorded.contains(normalizeForCompare(course)) && !electiveCandidates.contains(course)) {
                        electiveCandidates.add(course);
                    }
                }
            }
        }

        for (String course : electiveCandidates) {
            List<String> keys = courseJsonCatalog.getCourseTermKeys(major, course);
            String chosen = chooseEarliestTermKey(keys, orderedTermKeys);
            if (chosen == null) {
                chosen = chooseLeastLoadedTermKey(electiveByTerm, orderedTermKeys);
            }
            List<String> list = electiveByTerm.get(chosen);
            if (list != null && list.size() < 8 && !list.contains(course)) {
                list.add(course);
            }
        }

        List<StudyModuleDetailVO.SemesterPlanVO> plans = new ArrayList<>();
        for (String key : orderedTermKeys) {
            List<String> req = requiredByTerm.getOrDefault(key, List.of());
            List<String> ele = electiveByTerm.getOrDefault(key, List.of());
            if ((req == null || req.isEmpty()) && (ele == null || ele.isEmpty())) {
                continue;
            }
            StudyModuleDetailVO.SemesterPlanVO vo = new StudyModuleDetailVO.SemesterPlanVO();
            vo.setTermKey(key);
            vo.setTermLabel(termLabel(intakeYear, key));
            vo.setRequiredCourses(req == null ? List.of() : req);
            vo.setElectiveCourses(ele == null ? List.of() : ele);
            plans.add(vo);
        }

        if (!unknownRequired.isEmpty()) {
            StudyModuleDetailVO.SemesterPlanVO vo = new StudyModuleDetailVO.SemesterPlanVO();
            vo.setTermKey("unknown");
            vo.setTermLabel("未指定学期");
            vo.setRequiredCourses(unknownRequired);
            vo.setElectiveCourses(List.of());
            plans.add(vo);
        }

        return plans;
    }

    private List<StudyModuleDetailVO.SemesterPlanVO> buildElectiveSemesterPlans(StudentProfile profile, String major, List<String> missingElectives) {
        if (missingElectives == null || missingElectives.isEmpty()) {
            return List.of();
        }
        int intakeYear = parseIntakeYear(profile == null ? null : profile.getGrade());
        List<String> orderedTermKeys = orderedTermKeys();

        Map<String, List<String>> electiveByTerm = new LinkedHashMap<>();
        for (String key : orderedTermKeys) {
            electiveByTerm.put(key, new ArrayList<>());
        }
        List<String> unknown = new ArrayList<>();

        for (String course : missingElectives) {
            List<String> keys = courseJsonCatalog.getCourseTermKeys(major, course);
            String chosen = chooseEarliestTermKey(keys, orderedTermKeys);
            if (chosen == null) {
                unknown.add(course);
                continue;
            }
            List<String> list = electiveByTerm.get(chosen);
            if (list != null && list.size() < 10 && !list.contains(course)) {
                list.add(course);
            }
        }

        List<StudyModuleDetailVO.SemesterPlanVO> plans = new ArrayList<>();
        for (String key : orderedTermKeys) {
            List<String> ele = electiveByTerm.getOrDefault(key, List.of());
            if (ele == null || ele.isEmpty()) {
                continue;
            }
            StudyModuleDetailVO.SemesterPlanVO vo = new StudyModuleDetailVO.SemesterPlanVO();
            vo.setTermKey(key);
            vo.setTermLabel(termLabel(intakeYear, key));
            vo.setRequiredCourses(List.of());
            vo.setElectiveCourses(ele);
            plans.add(vo);
        }

        if (!unknown.isEmpty()) {
            StudyModuleDetailVO.SemesterPlanVO vo = new StudyModuleDetailVO.SemesterPlanVO();
            vo.setTermKey("unknown");
            vo.setTermLabel("未指定学期");
            vo.setRequiredCourses(List.of());
            vo.setElectiveCourses(unknown);
            plans.add(vo);
        }

        return plans;
    }

    private BigDecimal resolveCredits(String major, BigDecimal inputCredits, String courseName) {
        if (inputCredits != null && inputCredits.compareTo(BigDecimal.ZERO) > 0) {
            return inputCredits;
        }
        return courseJsonCatalog.findCredits(major, courseName).orElseGet(() -> courseJsonCatalog.findCreditsAny(courseName).orElse(BigDecimal.ZERO));
    }

    private BigDecimal parseCreditsOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String s = value.trim().replace("学分", "").trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            BigDecimal d = new BigDecimal(s);
            if (d.compareTo(BigDecimal.ZERO) <= 0 || d.compareTo(new BigDecimal("50")) > 0) {
                return null;
            }
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> buildWarnings(String major,
                                       List<StudyCourseRecord> records,
                                       List<StudyCategorySummaryVO> summaries,
                                       List<String> unknownCourses) {
        List<String> warnings = new ArrayList<>();
        if (unknownCourses != null && !unknownCourses.isEmpty()) {
            warnings.add("有 " + unknownCourses.size() + " 门课程未识别学分，可能影响统计");
        }
        if (summaries != null) {
            for (StudyCategorySummaryVO vo : summaries) {
                if (vo == null) {
                    continue;
                }
                BigDecimal remaining = vo.getRemainingCredits() == null ? BigDecimal.ZERO : vo.getRemainingCredits();
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    warnings.add(vo.getCategory() + " 仍需 " + remaining.stripTrailingZeros().toPlainString() + " 学分");
                }
            }
        }

        Set<String> normalizedRecorded = (records == null ? List.<StudyCourseRecord>of() : records).stream()
                .map(r -> normalizeForCompare(r.getCourseName()))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        for (String module : CATEGORIES) {
            List<String> required = courseJsonCatalog.getRequiredCourses(major, module);
            if (required.isEmpty()) {
                continue;
            }
            int missingCount = 0;
            for (String course : required) {
                if (!normalizedRecorded.contains(normalizeForCompare(course))) {
                    missingCount++;
                }
            }
            if (missingCount > 0) {
                warnings.add(module + " 必修未修 " + missingCount + " 门");
            }
        }
        return warnings;
    }

    private List<String> orderedTermKeys() {
        List<String> keys = new ArrayList<>();
        for (int y = 1; y <= 4; y++) {
            keys.add(y + "-1");
            keys.add(y + "-2");
        }
        return keys;
    }

    private String chooseEarliestTermKey(List<String> keys, List<String> ordered) {
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        String best = null;
        int bestIndex = Integer.MAX_VALUE;
        for (String k : keys) {
            int idx = ordered.indexOf(k);
            if (idx >= 0 && idx < bestIndex) {
                bestIndex = idx;
                best = k;
            }
        }
        return best;
    }

    private String chooseLeastLoadedTermKey(Map<String, List<String>> electiveByTerm, List<String> ordered) {
        String best = ordered.isEmpty() ? null : ordered.get(0);
        int bestSize = Integer.MAX_VALUE;
        for (String k : ordered) {
            List<String> list = electiveByTerm.get(k);
            int size = list == null ? 0 : list.size();
            if (size < bestSize) {
                bestSize = size;
                best = k;
            }
        }
        return best;
    }

    private String termLabel(int intakeYear, String termKey) {
        if (termKey == null || termKey.isBlank()) {
            return "";
        }
        String[] parts = termKey.split("-");
        if (parts.length != 2) {
            return termKey;
        }
        int y = safeParseInt(parts[0]);
        int t = safeParseInt(parts[1]);
        if (y <= 0 || t <= 0) {
            return termKey;
        }
        String termName = t == 1 ? "第一学期" : "第二学期";
        if (intakeYear > 0) {
            int start = intakeYear + (y - 1);
            int end = start + 1;
            return start + "-" + end + termName;
        }
        return "第" + y + "学年" + termName;
    }

    private int parseIntakeYear(String grade) {
        if (grade == null) {
            return -1;
        }
        String s = grade.trim();
        if (s.length() < 4) {
            return -1;
        }
        String prefix = s.substring(0, 4);
        int y = safeParseInt(prefix);
        if (y < 2000 || y > 2100) {
            return -1;
        }
        return y;
    }

    private int safeParseInt(String value) {
        if (value == null || value.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
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

    private String normalizeModule(String module) {
        String m = module == null ? "" : module.trim();
        if (m.contains("通识")) {
            return "通识模块";
        }
        if (m.contains("创新")) {
            return "创新训练与科学研究";
        }
        if (m.contains("素质") || m.contains("拓展")) {
            return "素质拓展与发展指导";
        }
        if (m.contains("专业")) {
            return "专业模块";
        }
        return m;
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
