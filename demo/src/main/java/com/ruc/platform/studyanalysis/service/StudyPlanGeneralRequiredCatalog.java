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
public class StudyPlanGeneralRequiredCatalog {

    private static final Pattern COURSE_ROW = Pattern.compile("^(.+?)\\s+([A-Z]{2,}[A-Z0-9/\\-]+)\\s+(\\d+(?:\\.\\d+)?)\\s+(.+)$");

    private final Path pdfPath;
    private volatile Map<String, List<String>> requiredByModule = null;

    public StudyPlanGeneralRequiredCatalog(@Value("${study.plan.pdf-path:1 2024级理工大类培养方案-信息学院.pdf}") String pdfPath) {
        this.pdfPath = Path.of(pdfPath);
    }

    public List<String> getRequiredCourses(String moduleName) {
        Map<String, List<String>> map = ensureLoaded();
        return map.getOrDefault(normalizeModule(moduleName), List.of());
    }

    private Map<String, List<String>> ensureLoaded() {
        Map<String, List<String>> current = requiredByModule;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (requiredByModule != null) {
                return requiredByModule;
            }
            requiredByModule = Collections.unmodifiableMap(load());
            return requiredByModule;
        }
    }

    private Map<String, List<String>> load() {
        if (!Files.exists(pdfPath)) {
            log.warn("培养方案PDF不存在，无法解析通识/创新/素质模块必修清单，pdfPath: {}", pdfPath.toAbsolutePath());
            return Map.of();
        }

        String text;
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            text = new PDFTextStripper().getText(document);
        } catch (IOException e) {
            log.warn("培养方案PDF解析失败，无法解析通识/创新/素质模块必修清单，pdfPath: {}", pdfPath.toAbsolutePath(), e);
            return Map.of();
        }

        List<String> lines = preprocessLines(text);
        Map<String, List<String>> result = new LinkedHashMap<>();
        result.put("通识模块", new ArrayList<>());
        result.put("创新训练与科学研究", new ArrayList<>());
        result.put("素质拓展与发展指导", new ArrayList<>());

        boolean inIdeologyRequired = false;
        for (String line : lines) {
            if (line.contains("思想政治理论课")) {
                inIdeologyRequired = false;
                continue;
            }
            if (line.contains("必修模块") && !inIdeologyRequired) {
                inIdeologyRequired = true;
                continue;
            }
            if (line.contains("选修模块") && inIdeologyRequired) {
                inIdeologyRequired = false;
                continue;
            }

            if (inIdeologyRequired) {
                Matcher matcher = COURSE_ROW.matcher(line);
                if (matcher.matches()) {
                    String courseName = cleanCourseName(matcher.group(1));
                    addUnique(result.get("通识模块"), courseName);
                }
                continue;
            }

            if (line.contains("《综合设计》") || line.contains("综合设计")) {
                addUnique(result.get("创新训练与科学研究"), "综合设计");
            }
            if (line.contains("《学术调研与论文写作》") || line.contains("学术调研与论文写作")) {
                addUnique(result.get("创新训练与科学研究"), "学术调研与论文写作");
            }
            if (line.contains("毕业论文")) {
                addUnique(result.get("创新训练与科学研究"), "毕业论文");
            }

            if (line.equals("劳动教育") || line.contains("劳动教育")) {
                addUnique(result.get("素质拓展与发展指导"), "劳动教育");
            }
            if (line.equals("军事课") || line.contains("军事课")) {
                addUnique(result.get("素质拓展与发展指导"), "军事课");
            }
            if (line.contains("职业生涯规划")) {
                addUnique(result.get("素质拓展与发展指导"), "职业生涯规划");
            }
            if (line.contains("志愿服务")) {
                addUnique(result.get("素质拓展与发展指导"), "志愿服务");
            }
        }

        log.info("培养方案通识/创新/素质必修清单解析完成，通识: {}, 创新: {}, 素质: {}",
                result.get("通识模块").size(),
                result.get("创新训练与科学研究").size(),
                result.get("素质拓展与发展指导").size());
        return result;
    }

    private String normalizeModule(String moduleName) {
        if (moduleName == null) {
            return "";
        }
        String m = moduleName.trim();
        if (m.contains("通识")) {
            return "通识模块";
        }
        if (m.contains("创新")) {
            return "创新训练与科学研究";
        }
        if (m.contains("素质") || m.contains("拓展")) {
            return "素质拓展与发展指导";
        }
        return m;
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
        return lines;
    }

    private String cleanCourseName(String value) {
        if (value == null) {
            return "";
        }
        String s = value.trim().replaceAll("\\s+", " ");
        s = s.replace("（", "(").replace("）", ")");
        return s;
    }

    private void addUnique(List<String> list, String value) {
        if (list == null) {
            return;
        }
        String v = cleanCourseName(value);
        if (v.isEmpty()) {
            return;
        }
        if (!list.contains(v)) {
            list.add(v);
        }
    }
}
