package com.ruc.platform.studyanalysis.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class StudyPlanCourseCreditCatalog {

    private static final Pattern COURSE_LINE_PATTERN = Pattern.compile("([\\p{IsHan}A-Za-z0-9（）()·“”《》\\-]+)\\s+([A-Z]{2,}[A-Z0-9]{3,})\\s+(\\d+(?:\\.\\d+)?)\\s");

    private final Path pdfPath;
    private volatile Map<String, BigDecimal> creditsByCourseName = null;

    public StudyPlanCourseCreditCatalog(@Value("${study.plan.pdf-path:1 2024级理工大类培养方案-信息学院.pdf}") String pdfPath) {
        this.pdfPath = Path.of(pdfPath);
    }

    public Optional<BigDecimal> findCredits(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            return Optional.empty();
        }
        Map<String, BigDecimal> map = ensureLoaded();
        if (map.isEmpty()) {
            return Optional.empty();
        }
        String normalized = normalizeName(courseName);
        BigDecimal direct = map.get(normalized);
        if (direct != null) {
            return Optional.of(direct);
        }
        String compact = compactName(courseName);
        BigDecimal compactHit = map.get(compact);
        if (compactHit != null) {
            return Optional.of(compactHit);
        }
        return Optional.empty();
    }

    public List<String> listAllCourseNames() {
        Map<String, BigDecimal> map = ensureLoaded();
        if (map.isEmpty()) {
            return List.of();
        }
        return map.keySet().stream()
                .filter(key -> key != null && !key.isBlank())
                .filter(key -> key.contains(" ") || key.codePoints().anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private Map<String, BigDecimal> ensureLoaded() {
        Map<String, BigDecimal> current = creditsByCourseName;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (creditsByCourseName != null) {
                return creditsByCourseName;
            }
            creditsByCourseName = Collections.unmodifiableMap(loadCredits());
            return creditsByCourseName;
        }
    }

    private Map<String, BigDecimal> loadCredits() {
        if (!Files.exists(pdfPath)) {
            log.warn("培养方案PDF不存在，无法解析课程学分映射，pdfPath: {}", pdfPath.toAbsolutePath());
            return Map.of();
        }

        String text;
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            text = new PDFTextStripper().getText(document);
        } catch (IOException e) {
            log.warn("培养方案PDF解析失败，pdfPath: {}", pdfPath.toAbsolutePath(), e);
            return Map.of();
        }

        Map<String, BigDecimal> map = new HashMap<>();
        Matcher matcher = COURSE_LINE_PATTERN.matcher(text);
        while (matcher.find()) {
            String name = matcher.group(1);
            String creditsText = matcher.group(3);
            if (!isValidCourseName(name)) {
                continue;
            }
            BigDecimal credits;
            try {
                credits = new BigDecimal(creditsText);
            } catch (Exception ignore) {
                continue;
            }
            String normalized = normalizeName(name);
            map.putIfAbsent(normalized, credits);
            map.putIfAbsent(compactName(name), credits);
        }
        log.info("培养方案课程学分映射解析完成，课程数: {}", map.size());
        return map;
    }

    private boolean isValidCourseName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 60) {
            return false;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.contains("学时")) {
            return false;
        }
        if (trimmed.equals("II") || trimmed.equals("I")) {
            return false;
        }
        boolean hasHan = trimmed.codePoints().anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN);
        return hasHan;
    }

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim()
                .replace('（', '(')
                .replace('）', ')')
                .replaceAll("\\s+", " ");
    }

    private String compactName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim()
                .replace('（', '(')
                .replace('）', ')')
                .replaceAll("\\s+", "");
    }
}
