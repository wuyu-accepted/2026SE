package com.ruc.platform.studyanalysis.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class StudyPlanRequiredCourseCatalog {

    private static final List<String> SUPPORTED_MAJORS = List.of(
            "计算机科学与技术",
            "软件工程",
            "信息安全",
            "信息管理与信息系统",
            "数据科学与大数据技术"
    );

    private static final List<String> COMMON_REQUIRED_PROFESSIONAL = List.of(
            "高等数学Ⅰ",
            "高等数学Ⅱ",
            "高等代数Ⅰ",
            "高等代数Ⅱ",
            "普通物理 B",
            "程序设计"
    );

    private static final Pattern COURSE_ROW = Pattern.compile("^(.+?)\\s+([A-Z]{2,}[A-Z0-9/\\-]+)\\s+(\\d+(?:\\.\\d+)?)\\s+(.+)$");
    private static final Pattern TERM_KEY = Pattern.compile("(\\d)\\s*[-—–]\\s*(\\d)");

    private final Path pdfPath;
    private volatile Catalog catalog = null;

    public StudyPlanRequiredCourseCatalog(@Value("${study.plan.pdf-path:1 2024级理工大类培养方案-信息学院.pdf}") String pdfPath) {
        this.pdfPath = Path.of(pdfPath);
    }

    public List<String> getRequiredCourses(String major) {
        String normalized = normalizeMajor(major);
        Catalog c = ensureLoaded();
        List<String> required = c.requiredByMajor.getOrDefault(normalized, List.of());
        if (required.isEmpty()) {
            return List.of();
        }
        return required;
    }

    public List<String> getCourseTermKeys(String major, String courseName) {
        if (courseName == null || courseName.isBlank()) {
            return List.of();
        }
        String normalized = normalizeMajor(major);
        Catalog c = ensureLoaded();
        Map<String, List<String>> map = c.termKeysByMajor.get(normalized);
        if (map == null) {
            return List.of();
        }
        return map.getOrDefault(courseName.trim(), List.of());
    }

    public boolean isSupportedMajor(String major) {
        return SUPPORTED_MAJORS.contains(normalizeMajor(major));
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
        if (!Files.exists(pdfPath)) {
            log.warn("培养方案PDF不存在，无法解析必修课程清单，pdfPath: {}", pdfPath.toAbsolutePath());
            return Catalog.empty();
        }

        String text;
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            text = new PDFTextStripper().getText(document);
        } catch (IOException e) {
            log.warn("培养方案PDF解析失败，无法解析必修课程清单，pdfPath: {}", pdfPath.toAbsolutePath(), e);
            return Catalog.empty();
        }

        List<String> lines = preprocessLines(text);
        Map<String, List<String>> result = new LinkedHashMap<>();
        Map<String, Map<String, List<String>>> termKeysByMajor = new LinkedHashMap<>();
        for (String major : SUPPORTED_MAJORS) {
            result.put(major, new ArrayList<>());
            termKeysByMajor.put(major, new LinkedHashMap<>());
        }

        String currentMajor = null;
        String dsMode = "";
        String dsSub = "";
        String dsPrefix = "";

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isEmpty()) {
                continue;
            }

            String mergedMajor = mergeBrokenMajor(lines, i);
            if (mergedMajor != null) {
                currentMajor = mergedMajor;
                dsMode = "";
                dsSub = "";
                dsPrefix = "";
                i++;
                continue;
            }

            if (SUPPORTED_MAJORS.contains(line)) {
                currentMajor = line;
                dsMode = "";
                dsSub = "";
                dsPrefix = "";
                continue;
            }

            if (currentMajor == null) {
                continue;
            }

            if (currentMajor.equals("数据科学与大数据技术")) {
                if (line.contains("共同") && line.contains("模块")) {
                    dsMode = "common";
                    continue;
                }
                if (line.contains("工学") && line.contains("特色模块")) {
                    dsMode = "engineering";
                    continue;
                }
                if (line.contains("理学") && line.contains("特色模块")) {
                    dsMode = "science";
                    continue;
                }
                if (line.contains("数据科学类")) {
                    dsSub = "data";
                    continue;
                }
                if (line.contains("概率统计类")) {
                    dsSub = "prob";
                    continue;
                }
                if (line.contains("并行计算") && line.contains("大数")) {
                    dsSub = "parallel";
                    continue;
                }
                if (line.equals("A") || line.equals("B")) {
                    dsPrefix = line;
                    continue;
                }
            }

            Matcher matcher = COURSE_ROW.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            String courseName = cleanCourseName(matcher.group(1));
            if (courseName.isEmpty()) {
                continue;
            }
            String rest = matcher.group(4);
            List<String> termKeys = extractTermKeys(rest);

            if (currentMajor.equals("数据科学与大数据技术")) {
                if (!"common".equals(dsMode) && !"engineering".equals(dsMode)) {
                    continue;
                }
                String prefix = "";
                if (courseName.startsWith("A ")) {
                    prefix = "A";
                    courseName = cleanCourseName(courseName.substring(2));
                } else if (courseName.startsWith("B ")) {
                    prefix = "B";
                    courseName = cleanCourseName(courseName.substring(2));
                } else if (!dsPrefix.isEmpty()) {
                    prefix = dsPrefix;
                }
                dsPrefix = "";

                if ("prob".equals(dsSub) && !"B".equals(prefix)) {
                    continue;
                }
                if ("parallel".equals(dsSub) && !"A".equals(prefix)) {
                    continue;
                }
            }

            List<String> list = result.get(currentMajor);
            if (list != null && !list.contains(courseName)) {
                list.add(courseName);
            }
            Map<String, List<String>> termMap = termKeysByMajor.get(currentMajor);
            if (termMap != null) {
                List<String> existing = termMap.get(courseName);
                if ((existing == null || existing.isEmpty()) && !termKeys.isEmpty()) {
                    termMap.put(courseName, termKeys);
                } else if (existing == null) {
                    termMap.put(courseName, List.of());
                }
            }
        }

        for (String major : SUPPORTED_MAJORS) {
            List<String> list = result.get(major);
            if (list == null) {
                continue;
            }
            for (String common : COMMON_REQUIRED_PROFESSIONAL) {
                if (!list.contains(common)) {
                    list.add(0, common);
                }
            }
        }

        log.info("培养方案必修课程清单解析完成，专业数: {}, pdfPath: {}", result.size(), pdfPath.toAbsolutePath());
        return Catalog.of(result, termKeysByMajor);
    }

    private List<String> preprocessLines(String text) {
        String[] rawLines = (text == null ? "" : text).split("\\r?\\n");
        List<String> lines = new ArrayList<>(rawLines.length);
        for (String raw : rawLines) {
            String line = raw == null ? "" : raw.trim();
            if (!line.isEmpty()) {
                line = line.replaceAll("\\s+", " ");
            }
            lines.add(line);
        }

        List<String> merged = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isEmpty()) {
                merged.add(line);
                continue;
            }
            if (line.endsWith("/") && i + 1 < lines.size()) {
                merged.add(line + lines.get(i + 1));
                i++;
                continue;
            }
            if (line.endsWith("-") && i + 1 < lines.size() && lines.get(i + 1).matches("^[A-Z0-9].*")) {
                merged.add(line.substring(0, line.length() - 1) + lines.get(i + 1));
                i++;
                continue;
            }
            merged.add(line);
        }
        return merged;
    }

    private String mergeBrokenMajor(List<String> lines, int index) {
        String line = lines.get(index);
        if (line == null) {
            return null;
        }
        if (line.equals("数据科学与") && index + 1 < lines.size()) {
            String next = lines.get(index + 1);
            if (next != null && next.contains("大数据技术")) {
                return "数据科学与大数据技术";
            }
        }
        return null;
    }

    private String cleanCourseName(String value) {
        if (value == null) {
            return "";
        }
        String s = value.trim().replaceAll("\\s+", " ");
        s = s.replace("（", "(").replace("）", ")");
        return s;
    }

    private String normalizeMajor(String major) {
        String m = major == null ? "" : major.trim();
        if (m.isEmpty()) {
            return "";
        }
        for (String supported : SUPPORTED_MAJORS) {
            if (m.contains(supported)) {
                return supported;
            }
        }
        String compact = m.replaceAll("\\s+", "");
        if (compact.contains("数据科学与大数据技术")) {
            return "数据科学与大数据技术";
        }
        return m;
    }

    private List<String> extractTermKeys(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        Matcher m = TERM_KEY.matcher(value);
        List<String> keys = new ArrayList<>(2);
        while (m.find()) {
            int y = parseSmallInt(m.group(1));
            int t = parseSmallInt(m.group(2));
            if (y < 1 || y > 6) {
                continue;
            }
            if (t < 1 || t > 2) {
                continue;
            }
            String key = y + "-" + t;
            if (!keys.contains(key)) {
                keys.add(key);
            }
        }
        return keys;
    }

    private int parseSmallInt(String value) {
        if (value == null || value.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static final class Catalog {
        private final Map<String, List<String>> requiredByMajor;
        private final Map<String, Map<String, List<String>>> termKeysByMajor;

        private Catalog(Map<String, List<String>> requiredByMajor, Map<String, Map<String, List<String>>> termKeysByMajor) {
            this.requiredByMajor = requiredByMajor;
            this.termKeysByMajor = termKeysByMajor;
        }

        private static Catalog of(Map<String, List<String>> requiredByMajor, Map<String, Map<String, List<String>>> termKeysByMajor) {
            Map<String, List<String>> required = Collections.unmodifiableMap(requiredByMajor);
            Map<String, Map<String, List<String>>> terms = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, List<String>>> e : termKeysByMajor.entrySet()) {
                terms.put(e.getKey(), Collections.unmodifiableMap(e.getValue()));
            }
            terms = Collections.unmodifiableMap(terms);
            return new Catalog(required, terms);
        }

        private static Catalog empty() {
            return new Catalog(Map.of(), Map.of());
        }
    }
}
