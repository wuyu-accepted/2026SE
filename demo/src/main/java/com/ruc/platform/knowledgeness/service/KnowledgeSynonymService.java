package com.ruc.platform.knowledgeness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties;
import com.ruc.platform.knowledgeness.entity.KnowledgeSynonymGroup;
import com.ruc.platform.knowledgeness.mapper.KnowledgeSynonymGroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class KnowledgeSynonymService {

    private final KnowledgeSynonymGroupMapper synonymGroupMapper;
    private final KnowledgeIntelligenceProperties properties;

    public Set<String> expand(String keyword) {
        Set<String> expanded = new LinkedHashSet<>();
        if (keyword == null || keyword.isBlank()) {
            return expanded;
        }
        expanded.add(keyword.trim());
        for (List<String> group : synonymGroups()) {
            boolean matched = group.stream().anyMatch(term -> keyword.contains(term) || term.contains(keyword));
            if (matched) {
                expanded.addAll(group);
            }
        }
        return expanded;
    }

    public List<List<String>> synonymGroups() {
        List<List<String>> groups = new ArrayList<>();
        groups.addAll(defaultGroups());
        groups.addAll(loadManagedGroups());
        groups.addAll(loadFileGroups());
        return groups;
    }

    private List<List<String>> loadManagedGroups() {
        if (synonymGroupMapper == null) {
            return List.of();
        }
        LambdaQueryWrapper<KnowledgeSynonymGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeSynonymGroup::getStatus, 1);
        List<List<String>> groups = new ArrayList<>();
        for (KnowledgeSynonymGroup group : synonymGroupMapper.selectList(wrapper)) {
            List<String> terms = splitTerms(group.getTerms());
            if (!terms.isEmpty()) {
                groups.add(terms);
            }
        }
        return groups;
    }

    private List<List<String>> loadFileGroups() {
        String path = properties.getSemantic().getSynonymPath();
        if (path == null || path.isBlank()) {
            return List.of();
        }
        Path file = Path.of(path.replace("${user.home}", System.getProperty("user.home")));
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            List<List<String>> groups = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                List<String> terms = splitTerms(line);
                if (!terms.isEmpty()) {
                    groups.add(terms);
                }
            }
            return groups;
        } catch (IOException e) {
            return List.of();
        }
    }

    private List<List<String>> defaultGroups() {
        Map<String, List<String>> defaults = new LinkedHashMap<>();
        defaults.put("奖助学金", List.of("助学金", "奖学金", "补助", "资助", "困难认定", "家庭经济困难"));
        defaults.put("请假", List.of("离校", "销假", "返校", "外出报备"));
        defaults.put("证明", List.of("在校证明", "学籍证明", "户籍证明", "成绩证明"));
        defaults.put("就业", List.of("三方协议", "就业推荐表", "签约", "派遣"));
        defaults.put("党员", List.of("入党", "积极分子", "发展对象", "预备党员", "转正"));
        return defaults.entrySet().stream()
                .map(entry -> {
                    List<String> terms = new ArrayList<>();
                    terms.add(entry.getKey());
                    terms.addAll(entry.getValue());
                    return terms;
                })
                .toList();
    }

    private List<String> splitTerms(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> terms = new ArrayList<>();
        for (String term : value.split("[,，;；\\s]+")) {
            if (!term.isBlank()) {
                terms.add(term.trim());
            }
        }
        return terms;
    }
}
