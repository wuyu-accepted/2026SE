package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.knowledgeness.entity.KnowledgeCategory;
import com.ruc.platform.knowledgeness.mapper.KnowledgeCategoryMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeCategorySyncTest {

    @Test
    void listCategoriesShouldMirrorEnabledAdminCategories() {
        KnowledgeCategoryMapper categoryMapper = mock(KnowledgeCategoryMapper.class);
        KnowledgeCategory first = category(11L, "党团流程", 1);
        KnowledgeCategory second = category(12L, "奖助学金", 1);
        KnowledgeCategory disabled = category(13L, "已停用", 0);
        when(categoryMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(first, second, disabled));

        KnowledgeCategoryService service = new KnowledgeCategoryService(categoryMapper);

        List<KnowledgeCategory> categories = service.listEnabledCategories();

        assertThat(categories).extracting(KnowledgeCategory::getName).containsExactly("党团流程", "奖助学金");
        assertThat(categories).extracting(KnowledgeCategory::getId).containsExactly(11L, 12L);
    }

    private KnowledgeCategory category(Long id, String name, int status) {
        KnowledgeCategory category = new KnowledgeCategory();
        category.setId(id);
        category.setName(name);
        category.setStatus(status);
        return category;
    }
}
