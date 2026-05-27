package com.ruc.platform.studyanalysis.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class StudyPlanCourseJsonCatalog {

    private static final Pattern ELECTIVE_MODULE = Pattern.compile(".*限选模块\\s*(\\d{1,2}).*");

    private final ObjectMapper objectMapper;
    private final Path jsonPath;

    private volatile Catalog catalog;

    public StudyPlanCourseJsonCatalog(ObjectMapper objectMapper,
                                      @Value("${study.plan.course-json-path:course.json}") String jsonPath) {
        this.objectMapper = objectMapper;
        this.jsonPath = Path.of(jsonPath);
    }

    public boolean isSupportedMajor(String major) {
        String normalized = normalizeMajorKey(major);
        return ensureLoaded().coursesByMajor.containsKey(normalized);
    }

    public Optional<BigDecimal> findCredits(String major, String courseName) {
        if (courseName == null || courseName.isBlank()) {
            return Optional.empty();
        }
        String m = normalizeMajorKey(major);
        String key = normalizeCourseKey(courseName);
        List<CourseItem> items = ensureLoaded().coursesByMajor.get(m);
        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }
        for (CourseItem item : items) {
            if (key.equals(item.courseKey)) {
                return Optional.ofNullable(item.credits);
            }
        }
        return Optional.empty();
    }

    public Optional<String> findModule(String major, String courseName) {
        if (courseName == null || courseName.isBlank()) {
            return Optional.empty();
        }
        String m = normalizeMajorKey(major);
        String key = normalizeCourseKey(courseName);
        List<CourseItem> items = ensureLoaded().coursesByMajor.get(m);
        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }
        for (CourseItem item : items) {
            if (key.equals(item.courseKey)) {
                return Optional.of(moduleForCourseType(item.courseType));
            }
        }
        return Optional.empty();
    }

    public Optional<BigDecimal> findCreditsAny(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            return Optional.empty();
        }
        String key = normalizeCourseKey(courseName);
        BigDecimal credit = ensureLoaded().creditsByCourseKey.get(key);
        return credit == null ? Optional.empty() : Optional.of(credit);
    }

    public List<String> getRequiredCourses(String major, String moduleName) {
        String m = normalizeMajorKey(major);
        String module = normalizeModule(moduleName);
        Catalog c = ensureLoaded();
        List<CourseItem> items = c.coursesByMajor.get(m);
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (CourseItem item : items) {
            if (item.isElective) {
                continue;
            }
            if (belongsToModule(module, item.courseType)) {
                result.add(item.courseName);
            }
        }
        return result;
    }

    public Map<String, BigDecimal> getRequiredCreditsByModule(String major) {
        String m = normalizeMajorKey(major);
        Map<String, BigDecimal> credits = ensureLoaded().requiredCreditsByModuleByMajor.get(m);
        return credits == null ? Map.of() : credits;
    }

    public List<ElectiveModule> listElectiveModules(String major) {
        String m = normalizeMajorKey(major);
        Map<String, ElectiveModule> modules = ensureLoaded().electiveModulesByMajor.get(m);
        if (modules == null || modules.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(modules.values());
    }

    public ElectiveModule getElectiveModule(String major, String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String m = normalizeMajorKey(major);
        Map<String, ElectiveModule> modules = ensureLoaded().electiveModulesByMajor.get(m);
        if (modules == null || modules.isEmpty()) {
            return null;
        }
        return modules.get(key.trim());
    }

    public List<String> getCourseTermKeys(String major, String courseName) {
        if (courseName == null || courseName.isBlank()) {
            return List.of();
        }
        String m = normalizeMajorKey(major);
        String key = normalizeCourseKey(courseName);
        Map<String, List<String>> termKeysByCourse = ensureLoaded().termKeysByMajor.get(m);
        if (termKeysByCourse == null) {
            return List.of();
        }
        return termKeysByCourse.getOrDefault(key, List.of());
    }

    private boolean belongsToModule(String module, String courseType) {
        return module.equals(moduleForCourseType(courseType));
    }

    private String moduleForCourseType(String courseType) {
        String t = courseType == null ? "" : courseType.trim();
        if (t.contains("思想政治理论课")) {
            return "通识模块";
        }
        if (t.contains("科研") || t.contains("科研与实践")) {
            return "创新训练与科学研究";
        }
        if (t.contains("实践训练") || t.contains("素质") || t.contains("拓展")) {
            return "素质拓展与发展指导";
        }
        if (t.contains("专业核心课") || t.contains("部类共同课") || t.contains("部类基础课") || t.contains("个性化选修课")) {
            return "专业模块";
        }
        return "";
    }

    private Catalog ensureLoaded() {
        Catalog current = catalog;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (catalog != null) {
                return catalog;
            }
            catalog = load();
            return catalog;
        }
    }

    private Catalog load() {
        if (!Files.exists(jsonPath)) {
            log.warn("课程JSON不存在，无法加载培养方案课程库，jsonPath: {}", jsonPath.toAbsolutePath());
            return Catalog.empty();
        }

        Map<String, List<CourseRow>> raw;
        try {
            raw = objectMapper.readValue(jsonPath.toFile(), new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("课程JSON解析失败，jsonPath: {}", jsonPath.toAbsolutePath(), e);
            return Catalog.empty();
        }

        Map<String, List<CourseItem>> coursesByMajor = new LinkedHashMap<>();
        Map<String, BigDecimal> creditsByCourseKey = new LinkedHashMap<>();
        Map<String, Map<String, List<String>>> termKeysByMajor = new LinkedHashMap<>();
        Map<String, Map<String, ElectiveModule>> electiveModulesByMajor = new LinkedHashMap<>();
        Map<String, Map<String, BigDecimal>> requiredCreditsByModuleByMajor = new LinkedHashMap<>();

        for (Map.Entry<String, List<CourseRow>> entry : raw.entrySet()) {
            String majorKey = normalizeMajorKey(entry.getKey());
            List<CourseRow> rows = entry.getValue() == null ? List.of() : entry.getValue();
            List<CourseItem> items = new ArrayList<>();
            Map<String, List<String>> termsByCourse = new LinkedHashMap<>();
            Map<String, ElectiveModule> modules = new LinkedHashMap<>();
            Map<String, BigDecimal> requiredCreditsByModule = new LinkedHashMap<>();

            for (CourseRow row : rows) {
                String name = cleanCourseName(row.courseName);
                if (name.isEmpty()) {
                    continue;
                }
                BigDecimal credits = parseCredits(row.credits);
                String courseKey = normalizeCourseKey(name);
                String courseType = row.courseType == null ? "" : row.courseType.trim();

                CourseItem item = new CourseItem();
                item.courseName = name;
                item.courseKey = courseKey;
                item.credits = credits;
                item.courseType = courseType;
                item.termKeys = parseTermKeys(row.term);
                item.isElective = courseType.contains("个性化选修课");
                item.electiveModuleKey = extractElectiveModuleKey(courseType);
                items.add(item);

                if (!item.isElective && credits != null && credits.compareTo(BigDecimal.ZERO) > 0) {
                    String module = moduleForCourseType(courseType);
                    if (!module.isBlank()) {
                        requiredCreditsByModule.put(module, requiredCreditsByModule.getOrDefault(module, BigDecimal.ZERO).add(credits));
                    }
                }

                if (credits != null && credits.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal existing = creditsByCourseKey.get(courseKey);
                    if (existing == null || existing.compareTo(credits) < 0) {
                        creditsByCourseKey.put(courseKey, credits);
                    }
                }

                if (item.termKeys != null && !item.termKeys.isEmpty()) {
                    termsByCourse.putIfAbsent(courseKey, item.termKeys);
                } else {
                    termsByCourse.putIfAbsent(courseKey, List.of());
                }

                if (item.isElective && item.electiveModuleKey != null && !item.electiveModuleKey.isBlank()) {
                    ElectiveModule m = modules.computeIfAbsent(item.electiveModuleKey, k -> {
                        ElectiveModule em = new ElectiveModule();
                        em.setKey(k);
                        em.setName(k.replace("-", " "));
                        em.setCourses(new ArrayList<>());
                        return em;
                    });
                    if (!m.getCourses().contains(name)) {
                        m.getCourses().add(name);
                    }
                }
            }

            coursesByMajor.put(majorKey, items);
            termKeysByMajor.put(majorKey, termsByCourse);
            electiveModulesByMajor.put(majorKey, modules);
            requiredCreditsByModuleByMajor.put(majorKey, requiredCreditsByModule);
        }

        log.info("课程JSON加载完成，专业数: {}, jsonPath: {}", coursesByMajor.size(), jsonPath.toAbsolutePath());
        return Catalog.of(coursesByMajor, creditsByCourseKey, termKeysByMajor, electiveModulesByMajor, requiredCreditsByModuleByMajor);
    }

    private String extractElectiveModuleKey(String courseType) {
        if (courseType == null || courseType.isBlank()) {
            return null;
        }
        Matcher m = ELECTIVE_MODULE.matcher(courseType);
        if (!m.matches()) {
            return null;
        }
        String num = m.group(1);
        if (num == null || num.isBlank()) {
            return null;
        }
        return "计算机类-" + num.trim();
    }

    private BigDecimal parseCredits(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private List<String> parseTermKeys(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String s = value.trim().replaceAll("\\s+", "");
        List<String> result = new ArrayList<>();

        String[] parts = s.split("[,，/]");
        for (String p : parts) {
            if (p == null || p.isBlank()) {
                continue;
            }
            if (p.contains("-")) {
                String[] range = p.split("-");
                if (range.length == 2) {
                    int a = safeParseInt(range[0]);
                    int b = safeParseInt(range[1]);
                    if (a > 0 && b > 0 && a <= b) {
                        for (int i = a; i <= b; i++) {
                            addTermKey(result, i);
                        }
                    }
                }
                continue;
            }
            int term = safeParseInt(p);
            addTermKey(result, term);
        }
        return result;
    }

    private void addTermKey(List<String> result, int term) {
        if (term < 1 || term > 12) {
            return;
        }
        int year = (term + 1) / 2;
        int half = term % 2 == 1 ? 1 : 2;
        String key = year + "-" + half;
        if (!result.contains(key)) {
            result.add(key);
        }
    }

    private int safeParseInt(String value) {
        if (value == null || value.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private String normalizeMajorKey(String major) {
        String m = major == null ? "" : major.trim();
        if (m.equals("数据科学与大数据技术")) {
            return "数据科学与大数据技术(工学)";
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

    private String cleanCourseName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replace('（', '(')
                .replace('）', ')')
                .replaceAll("\\s+", " ");
    }

    private String normalizeCourseKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replace('（', '(')
                .replace('）', ')')
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    @Data
    private static class CourseRow {
        @JsonProperty("课程名称")
        private String courseName;

        @JsonProperty("学分")
        private String credits;

        @JsonProperty("课程代码")
        private String code;

        @JsonProperty("开课学期")
        private String term;

        @JsonProperty("课程类别")
        private String courseType;
    }

    private static class CourseItem {
        private String courseName;
        private String courseKey;
        private BigDecimal credits;
        private String courseType;
        private List<String> termKeys;
        private boolean isElective;
        private String electiveModuleKey;
    }

    @Data
    public static class ElectiveModule {
        private String key;
        private String name;
        private List<String> courses;
    }

    private static final class Catalog {
        private final Map<String, List<CourseItem>> coursesByMajor;
        private final Map<String, BigDecimal> creditsByCourseKey;
        private final Map<String, Map<String, List<String>>> termKeysByMajor;
        private final Map<String, Map<String, ElectiveModule>> electiveModulesByMajor;
        private final Map<String, Map<String, BigDecimal>> requiredCreditsByModuleByMajor;

        private Catalog(Map<String, List<CourseItem>> coursesByMajor,
                        Map<String, BigDecimal> creditsByCourseKey,
                        Map<String, Map<String, List<String>>> termKeysByMajor,
                        Map<String, Map<String, ElectiveModule>> electiveModulesByMajor,
                        Map<String, Map<String, BigDecimal>> requiredCreditsByModuleByMajor) {
            this.coursesByMajor = coursesByMajor;
            this.creditsByCourseKey = creditsByCourseKey;
            this.termKeysByMajor = termKeysByMajor;
            this.electiveModulesByMajor = electiveModulesByMajor;
            this.requiredCreditsByModuleByMajor = requiredCreditsByModuleByMajor;
        }

        private static Catalog of(Map<String, List<CourseItem>> coursesByMajor,
                                  Map<String, BigDecimal> creditsByCourseKey,
                                  Map<String, Map<String, List<String>>> termKeysByMajor,
                                  Map<String, Map<String, ElectiveModule>> electiveModulesByMajor,
                                  Map<String, Map<String, BigDecimal>> requiredCreditsByModuleByMajor) {
            return new Catalog(coursesByMajor, creditsByCourseKey, termKeysByMajor, electiveModulesByMajor, requiredCreditsByModuleByMajor);
        }

        private static Catalog empty() {
            return new Catalog(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }
    }
}
