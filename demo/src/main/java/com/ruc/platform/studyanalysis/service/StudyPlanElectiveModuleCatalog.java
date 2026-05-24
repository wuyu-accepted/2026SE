package com.ruc.platform.studyanalysis.service;

import lombok.Data;
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
public class StudyPlanElectiveModuleCatalog {

    private static final Pattern MODULE_HEADER = Pattern.compile("^(计算机类)\\s*[-－—–]?\\s*(\\d{1,2})\\s*[:：]?\\s*(.+)$");
    private static final Pattern COURSE_ROW = Pattern.compile("^(.+?)\\s+([A-Z]{2,}[A-Z0-9/\\-]+)\\s+(\\d+(?:\\.\\d+)?)\\s+(.+)$");
    private static final Pattern TERM_KEY = Pattern.compile("(\\d)\\s*[-—–]\\s*(\\d)");

    private final Path pdfPath;
    private volatile Catalog catalog = null;

    public StudyPlanElectiveModuleCatalog(@Value("${study.plan.pdf-path:1 2024级理工大类培养方案-信息学院.pdf}") String pdfPath) {
        this.pdfPath = Path.of(pdfPath);
    }

    public List<ElectiveModule> listComputerElectiveModules() {
        return new ArrayList<>(ensureLoaded().modules.values());
    }

    public ElectiveModule getModule(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return ensureLoaded().modules.get(key.trim());
    }

    public List<String> getCourseTermKeys(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            return List.of();
        }
        Catalog c = ensureLoaded();
        return c.courseTermKeys.getOrDefault(normalizeForKey(courseName), List.of());
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
            log.warn("培养方案PDF不存在，无法解析个性化选修模块课程池，pdfPath: {}", pdfPath.toAbsolutePath());
            return Catalog.empty();
        }

        String text;
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            text = new PDFTextStripper().getText(document);
        } catch (IOException e) {
            log.warn("培养方案PDF解析失败，无法解析个性化选修模块课程池，pdfPath: {}", pdfPath.toAbsolutePath(), e);
            return Catalog.empty();
        }

        List<String> lines = preprocessLines(text);
        Map<String, ElectiveModule> result = new LinkedHashMap<>();
        Map<String, List<String>> courseTermKeys = new LinkedHashMap<>();

        ElectiveModule currentModule = null;
        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }
            Matcher header = MODULE_HEADER.matcher(line);
            if (header.matches()) {
                String group = header.group(1);
                String number = header.group(2);
                String name = clean(header.group(3));
                String key = group + "-" + number;
                currentModule = result.computeIfAbsent(key, k -> {
                    ElectiveModule m = new ElectiveModule();
                    m.setKey(k);
                    m.setName(group + "-" + number + " " + name);
                    m.setCourses(new ArrayList<>());
                    return m;
                });
                continue;
            }

            if (currentModule == null) {
                continue;
            }

            Matcher row = COURSE_ROW.matcher(line);
            if (row.matches()) {
                String courseName = cleanCourseName(row.group(1));
                if (!courseName.isEmpty() && !currentModule.getCourses().contains(courseName)) {
                    currentModule.getCourses().add(courseName);
                }
                List<String> keys = extractTermKeys(row.group(4));
                if (!courseName.isEmpty() && !keys.isEmpty()) {
                    String normalized = normalizeForKey(courseName);
                    List<String> existing = courseTermKeys.get(normalized);
                    if (existing == null || existing.isEmpty()) {
                        courseTermKeys.put(normalized, keys);
                    }
                } else if (!courseName.isEmpty()) {
                    courseTermKeys.putIfAbsent(normalizeForKey(courseName), List.of());
                }
            }
        }

        log.info("培养方案个性化选修模块课程池解析完成，模块数: {}", result.size());
        return Catalog.of(result, courseTermKeys);
    }

    private List<String> preprocessLines(String text) {
        String[] rawLines = (text == null ? "" : text).split("\\r?\\n");
        List<String> lines = new ArrayList<>(rawLines.length);
        for (String raw : rawLines) {
            String line = raw == null ? "" : raw.trim();
            if (!line.isEmpty()) {
                line = line.replaceAll("\\s+", " ");
            }
            if (line.startsWith("■")) {
                continue;
            }
            lines.add(line);
        }
        return lines;
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String cleanCourseName(String value) {
        if (value == null) {
            return "";
        }
        String s = value.trim().replaceAll("\\s+", " ");
        s = s.replace("（", "(").replace("）", ")");
        if (s.startsWith("A ")) {
            s = s.substring(2).trim();
        }
        if (s.startsWith("B ")) {
            s = s.substring(2).trim();
        }
        return s;
    }

    private String normalizeForKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replace('（', '(')
                .replace('）', ')')
                .replaceAll("\\s+", "")
                .toLowerCase();
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

    @Data
    public static class ElectiveModule {
        private String key;
        private String name;
        private List<String> courses;
    }

    private static final class Catalog {
        private final Map<String, ElectiveModule> modules;
        private final Map<String, List<String>> courseTermKeys;

        private Catalog(Map<String, ElectiveModule> modules, Map<String, List<String>> courseTermKeys) {
            this.modules = modules;
            this.courseTermKeys = courseTermKeys;
        }

        private static Catalog of(Map<String, ElectiveModule> modules, Map<String, List<String>> courseTermKeys) {
            return new Catalog(Collections.unmodifiableMap(modules), Collections.unmodifiableMap(courseTermKeys));
        }

        private static Catalog empty() {
            return new Catalog(Map.of(), Map.of());
        }
    }
}
