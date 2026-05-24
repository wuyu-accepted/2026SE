package com.ruc.platform.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruc.platform.ai.entity.AiMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {
    @Select("""
            SELECT *
            FROM ai_message
            WHERE conversation_id = #{conversationId}
              AND user_id = #{userId}
            ORDER BY created_at DESC
            LIMIT #{limit}
            """)
    List<AiMessage> selectRecentByConversation(@Param("conversationId") Long conversationId,
                                               @Param("userId") Long userId,
                                               @Param("limit") Integer limit);
}
