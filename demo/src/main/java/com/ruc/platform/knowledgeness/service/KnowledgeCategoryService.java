package com.ruc.platform.knowledgeness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruc.platform.knowledgeness.entity.KnowledgeCategory;
import com.ruc.platform.knowledgeness.mapper.KnowledgeCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeCategoryService {

    private final KnowledgeCategoryMapper categoryMapper;

    public List<KnowledgeCategory> listEnabledCategories() {
        LambdaQueryWrapper<KnowledgeCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeCategory::getStatus, 1)
                .orderByAsc(KnowledgeCategory::getSortOrder)
                .orderByAsc(KnowledgeCategory::getId);
        return categoryMapper.selectList(wrapper).stream()
                .filter(category -> Integer.valueOf(1).equals(category.getStatus()))
                .collect(Collectors.toList());
    }
}
