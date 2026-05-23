package com.ruc.platform.knowledgeness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleDetailVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeArticleMapper extends BaseMapper<KnowledgeArticle> {

    List<KnowledgeArticleListItemVO> selectArticlePage(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("status") Integer status
    );

    @Select("""
            SELECT
                a.id,
                a.category_id AS categoryId,
                c.name AS categoryName,
                a.status,
                a.title,
                a.summary,
                a.content,
                a.answer,
                a.source,
                a.publish_time AS publishTime,
                a.view_count AS viewCount
            FROM knowledge_article a
            LEFT JOIN knowledge_category c ON a.category_id = c.id
            WHERE a.id = #{id}
            """)
    KnowledgeArticleDetailVO selectArticleDetail(@Param("id") Long id);
}
