package com.ruc.platform.knowledgeness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruc.platform.knowledgeness.entity.KnowledgeIndexTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface KnowledgeIndexTaskMapper extends BaseMapper<KnowledgeIndexTask> {

    @Select("""
            SELECT *
            FROM knowledge_index_task
            WHERE status = 'pending'
              AND (next_retry_at IS NULL OR next_retry_at <= #{now})
            ORDER BY created_at ASC
            LIMIT 1
            """)
    KnowledgeIndexTask selectPendingOne(@Param("now") LocalDateTime now);
}
