package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class KnowledgeSemanticSearchService {

    public void upsertArticle(KnowledgeArticle article) {
        // 本地开源路线第一阶段：语义能力由同义词/关键词扩展承担；后续可替换为本地 ONNX embedding。
    }

    public Set<String> expandKeywords(String keyword) {
        Set<String> keywords = new LinkedHashSet<>();
        if (keyword == null || keyword.isBlank()) {
            return keywords;
        }
        keywords.add(keyword.trim());
        if (keyword.contains("奖助")) {
            keywords.add("助学金");
            keywords.add("奖学金");
            keywords.add("困难认定");
        }
        if (keyword.contains("请假")) {
            keywords.add("销假");
            keywords.add("离校");
        }
        if (keyword.contains("证明")) {
            keywords.add("在校证明");
            keywords.add("学籍证明");
        }
        return keywords;
    }
}
